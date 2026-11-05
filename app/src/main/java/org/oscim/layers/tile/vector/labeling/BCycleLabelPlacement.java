/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2019 devemux86
 * Copyright 2018 Gustl22
 * Copyright 2021 blakfast
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
// B|CYCLE adaptation (2026-11-04), derived from Mapsforge VTM 0.25.0.
// Renamed local labeling classes; modifications and upstream references:
// docs/vtm-label-stability.md. Original LGPL notice retained above.
package org.oscim.layers.tile.vector.labeling;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileRenderer;
import org.oscim.layers.tile.TileSet;
import org.oscim.layers.tile.ZoomLimiter;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.renderer.bucket.SymbolBucket;
import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.renderer.bucket.BCycleCaptionLayout;
import org.oscim.theme.styles.TextStyle;
import org.oscim.utils.FastMath;
import org.oscim.utils.Parameters;
import org.oscim.utils.geom.OBB2D;

import java.util.HashSet;
import java.util.Set;

import static org.oscim.layers.tile.MapTile.State.NEW_DATA;
import static org.oscim.layers.tile.MapTile.State.READY;

public class BCycleLabelPlacement {
    static final boolean dbg = false;

    public static final LabelTileData getLabels(MapTile tile) {
        return (LabelTileData) tile.getData(LabelLayer.LABEL_DATA);
    }

    private static final float DISTANCE_COEFFICIENT = 3;
    private static final float MIN_WAY_DIST = 3;

    /**
     * thread local pool of for unused label items
     */
    private final LabelPool mPool = new LabelPool();

    private final TileSet mTileSet = new TileSet();
    private final TileRenderer mTileRenderer;
    private final Map mMap;
    private final ZoomLimiter mZoomLimiter;

    /**
     * list of current labels
     */
    private Label mLabels;
    private final BCycleLabelFades fades=new BCycleLabelFades();
    private double currentZoom;

    private float mSquareRadius;

    /**
     * incremented each update, to prioritize labels
     * that became visible ealier.
     */
    private int mRelabelCnt;

    /* Zoom level of current tiles (initial value doesn't matter) */
    private Integer mZoom = Viewport.MIN_ZOOM_LEVEL;

    public BCycleLabelPlacement(Map map, TileRenderer tileRenderer, ZoomLimiter zoomLimiter) {
        mMap = map;
        mTileRenderer = tileRenderer;
        mZoomLimiter = zoomLimiter;
    }

    /**
     * remove Label l from mLabels and return l.next
     */
    private Label removeLabel(Label l) {
        Label ret = (Label) l.next;
        mLabels = (Label) mPool.release(mLabels, l);
        return ret;
    }

    public void addLabel(Label l) {
        l.next = mLabels;
        mLabels = l;
    }

    /** Lower style priority wins; ties retain the earlier visible label. */
    static boolean wins(Label candidate, Label visible) {
        if (BCycleLabelPolicy.importance(candidate.text) != BCycleLabelPolicy.importance(visible.text))
            return BCycleLabelPolicy.importance(candidate.text) < BCycleLabelPolicy.importance(visible.text);
        if (candidate.active != visible.active)
            return candidate.active < visible.active;
        if(BCycleLabelPolicy.majorRoad(candidate.text) && BCycleLabelPolicy.majorRoad(visible.text)
                && BCycleLabelPolicy.roadIdentity(candidate.label).equals(BCycleLabelPolicy.roadIdentity(visible.label))) return false;
        // Stable result even when newly loaded tiles arrive in a different order.
        int name = candidate.label.compareTo(visible.label);
        if (name != 0) return name < 0;
        if (candidate.x != visible.x) return candidate.x < visible.x;
        return candidate.y < visible.y;
    }

    private static boolean conflicts(Label a, Label b) {
        if (a.bbox.overlaps(b.bbox)) return true;
        // Road refs/names need a rotation-independent screen-space repeat distance.
        // Shields and way text may carry the same ref with different style IDs/spaces.
        if(BCycleLabelPolicy.majorRoad(a.text) && BCycleLabelPolicy.majorRoad(b.text)
                && BCycleLabelPolicy.roadIdentity(a.label).equals(BCycleLabelPolicy.roadIdentity(b.label))) {
            float dx=a.x-b.x,dy=a.y-b.y;
            float distance=BCycleLabelPolicy.roadRepeatDistance(a.text,b.text,a.width,b.width);
            return dx*dx+dy*dy<distance*distance;
        }
        // Preserve VTM's local duplicate suppression across tile/style instances.
        return a.label.equals(b.label) && java.util.Objects.equals(a.text.style, b.text.style)
                && Math.abs(a.x-b.x) < (a.width+b.width)/2 + 100
                && Math.abs(a.y-b.y) < (BCycleCaptionLayout.height(a)+BCycleCaptionLayout.height(b))/2 + 30;
    }

    private byte checkOverlap(Label candidate) {
        // Decide against every incumbent before removing any: a candidate blocked
        // by a stronger label must not erase weaker labels on its way through.
        for (Label visible = mLabels; visible != null; visible = (Label) visible.next) {
            if (!conflicts(candidate, visible)) continue;
            if (!wins(candidate, visible)) return 1;
        }
        for (Label visible = mLabels; visible != null;) {
            if (conflicts(candidate, visible)) visible = removeLabel(visible);
            else visible = (Label) visible.next;
        }
        return 0;
    }

    private static final class Caption {
        final MapTile tile;
        final TextItem item;
        final float x, y;
        final String roadKey;
        Caption(MapTile tile, TextItem item, float x, float y) {
            this.tile = tile; this.item = item; this.x = x; this.y = y;
            this.roadKey=BCycleLabelPolicy.majorRoad(item.text) ? BCycleLabelPolicy.roadIdentity(item.label) : null;
        }
    }

    private static String captionKey(TextStyle style, String name) {
        String family = style.style != null && style.style.startsWith("place-") ? "place" : style.style;
        return family + "\u0000" + name;
    }

    /** Rebind retained captions to currently available features, never stale tile/style data. */
    private Caption currentCaption(Label label, java.util.Map<String, java.util.List<Caption>> captions,
                                   double tileX, double tileY, double positionScale, int zoom, int maxx) {
        java.util.List<Caption> matches = captions.get(captionKey(label.text, label.label));
        if (matches == null) return null;
        int diff = label.tileZ - zoom;
        if (diff < -1 || diff > 1) return null;
        float div = FastMath.pow(diff);
        float scale = (float) (positionScale / (1 << label.tileZ));
        float x = (flipLongitude((float) (label.tileX * Tile.SIZE - tileX * div), maxx)
                + label.item.x) * scale;
        float y = (float) ((label.tileY * Tile.SIZE - tileY * div + label.item.y) * scale);
        Caption best = null;
        float distance = 64 * 64;
        for (Caption match : matches) {
            float dx = match.x - x, dy = match.y - y;
            float next = dx * dx + dy * dy;
            if (next < distance) { best = match; distance = next; }
        }
        return best;
    }

    private boolean isVisible(float x, float y) {
        // rough filter
        float dist = x * x + y * y;
        if (dist > mSquareRadius)
            return false;

        return true;
    }

    private boolean wayIsVisible(Label ti) {
        // rough filter
        float dist = ti.x * ti.x + ti.y * ti.y;
        if (dist < mSquareRadius)
            return true;

        dist = ti.x1 * ti.x1 + ti.y1 * ti.y1;
        if (dist < mSquareRadius)
            return true;

        dist = ti.x2 * ti.x2 + ti.y2 * ti.y2;
        if (dist < mSquareRadius)
            return true;

        return false;
    }

    private Label getLabel() {
        Label l = (Label) mPool.get();
        l.active = Integer.MAX_VALUE;

        return l;
    }

    private static float flipLongitude(float dx, int max) {
        // flip around date-line
        if (dx > max)
            dx = dx - max * 2;
        else if (dx < -max)
            dx = dx + max * 2;

        return dx;
    }

    private void placeLabelFrom(Label l, TextItem ti) {
        // set line endpoints relative to view to be able to
        // check intersections with label from other tiles
        float w = (ti.x2 - ti.x1) / 2f;
        float h = (ti.y2 - ti.y1) / 2f;

        l.x1 = l.x - w;
        l.y1 = l.y - h;
        l.x2 = l.x + w;
        l.y2 = l.y + h;
    }

    private Label addWayLabel(MapTile tile,TextItem item,Label label,float x,float y,double scale) {
        if(!dbg && item.width>item.length*scale) return label;
        if(label==null) label=getLabel();
        label.clone(item);label.active=mRelabelCnt;label.x=x;label.y=y;placeLabelFrom(label,item);
        if(!wayIsVisible(label)) return label;
        if(label.bbox==null) label.bbox=new OBB2D(label.x,label.y,label.x1,label.y1,label.width+MIN_WAY_DIST,label.text.fontHeight+MIN_WAY_DIST);
        else label.bbox.set(label.x,label.y,label.x1,label.y1,label.width+MIN_WAY_DIST,label.text.fontHeight+MIN_WAY_DIST);
        byte overlaps=checkOverlap(label);
        if(dbg) Debug.addDebugBox(label,item,overlaps,false,(float)scale);
        if(overlaps!=0) return label;
        addLabel(label);label.item=TextItem.copy(item);label.tileX=tile.tileX;label.tileY=tile.tileY;label.tileZ=tile.zoomLevel;
        return null;
    }
    private Label addWayLabels(MapTile tile,Label label,float dx,float dy,double scale) {
        LabelTileData data=getLabels(tile);
        if(data!=null) for(TextItem item:data.labels) {
            if(item.text.caption || BCycleLabelPolicy.majorRoad(item.text)) continue;
            label=addWayLabel(tile,item,label,(float)((dx+item.x)*scale),(float)((dy+item.y)*scale),scale);
        }
        return label;
    }
    private final java.util.Map<String,String> roadNames=new java.util.HashMap<>();
    private String roadKey(String name) { return roadNames.computeIfAbsent(name,BCycleLabelPolicy::roadIdentity); }
    private double roadGap(Caption candidate) {
        double nearest=Double.POSITIVE_INFINITY;
        for(Label label=mLabels;label!=null;label=(Label)label.next) {
            if(!BCycleLabelPolicy.majorRoad(label.text) || !candidate.roadKey.equals(roadKey(label.label))) continue;
            double dx=candidate.x-label.x,dy=candidate.y-label.y;nearest=Math.min(nearest,dx*dx+dy*dy);
        }
        // Start an unrepresented road near the viewport centre; then fill its largest gaps.
        return Double.isInfinite(nearest) ? -(candidate.x*candidate.x+candidate.y*candidate.y) : nearest;
    }
    private Label addMajorRoadLabels(java.util.List<Caption> candidates,Label scratch,float cos,float sin,double scale) {
        candidates.sort(java.util.Comparator.comparingInt((Caption c)->BCycleLabelPolicy.importance(c.item.text))
                .thenComparing(c->c.roadKey).thenComparingDouble(c->c.x).thenComparingDouble(c->c.y)
                .thenComparing(c->c.item.text.style));
        while(!candidates.isEmpty()) {
            int best=0;int importance=BCycleLabelPolicy.importance(candidates.get(0).item.text);
            double score=roadGap(candidates.get(0));
            for(int i=1;i<candidates.size();i++) {
                Caption candidate=candidates.get(i);
                if(BCycleLabelPolicy.importance(candidate.item.text)!=importance) break;
                double next=roadGap(candidate);
                if(next>score) { best=i;score=next; }
            }
            Caption candidate=candidates.remove(best);
            scratch=candidate.item.text.caption ? addNodeLabel(candidate,scratch,cos,sin) :
                    addWayLabel(candidate.tile,candidate.item,scratch,candidate.x,candidate.y,scale);
        }
        return scratch;
    }

    private Label addNodeLabel(Caption candidate,Label label,float cos,float sin) {
        TextItem item=candidate.item;
        if(!BCycleLabelPolicy.eligible(item.text,currentZoom,false) || !isVisible(candidate.x,candidate.y)) return label;
        if(label==null) label=getLabel();
        label.clone(item);
        double factor=1.0/(1 << candidate.tile.zoomLevel);
        label.active=fades.visibleAge(item,(candidate.tile.tileX+item.x/Tile.SIZE)*factor,
                (candidate.tile.tileY+item.y/Tile.SIZE)*factor,Tile.SIZE*Math.pow(2,currentZoom),mRelabelCnt);
        label.x=candidate.x;label.y=candidate.y;
        if(label.bbox==null) label.bbox=new OBB2D();
        label.bbox.setNormalized(label.x,label.y,cos,-sin,
                label.width+BCycleLabelPolicy.horizontalGap(label.text),
                BCycleCaptionLayout.height(label)+BCycleLabelPolicy.verticalGap(label.text),label.text.dy);
        if(checkOverlap(label)!=0) return label;
        addLabel(label);label.item=TextItem.copy(item);
        label.tileX=candidate.tile.tileX;label.tileY=candidate.tile.tileY;label.tileZ=candidate.tile.zoomLevel;
        return null;
    }

    boolean updateLabels(BCycleLabelTask work) {

        /* get current tiles */
        int lastZoom = mZoom;
        mZoom = mTileRenderer.getVisibleTiles(mTileSet, true);
        boolean changedTiles = mZoom != null;

        // set to last zoom in case of tiles didn't change
        if (!changedTiles)
            mZoom = lastZoom;

        if (mTileSet.cnt == 0) {
            return false;
        }

        MapPosition pos = work.pos;
        boolean changedPos = mMap.viewport().getMapPosition(pos);

        /* do not loop! */
        if (!changedTiles && !changedPos)
            return false;

        if (mZoom < mZoomLimiter.getMinZoom() || mZoom > mZoomLimiter.getMaxZoom())
            return false;

        mRelabelCnt++;
        currentZoom=Math.log(pos.scale)/Math.log(2);

        int cnt = mTileSet.cnt;
        MapTile[] tiles;
        int zoom;
        if (mZoom > mZoomLimiter.getZoomLimit()) {
            // render from zoom limit tiles (avoid duplicates and null)
            Set<MapTile> hashTiles = new HashSet<>();
            for (int i = 0; i < cnt; i++) {
                MapTile t = mZoomLimiter.getTile(mTileSet.tiles[i]);
                if (t == null)
                    continue;
                hashTiles.add(t);
            }

            cnt = hashTiles.size();
            tiles = hashTiles.toArray(new MapTile[cnt]);
            zoom = mZoomLimiter.getZoomLimit();
        } else {
            tiles = mTileSet.tiles;
            zoom = mZoom;
        }

        /* estimation for visible area to be labeled */
        if (Parameters.DISTANT_LABELS) {
            int mw = mMap.getWidth();
            int mh = mMap.getHeight();
            float k = pos.tilt / mMap.viewport().getMaxTilt() * DISTANCE_COEFFICIENT;
            if (k < 0.5)
                k = 0.5f;
            mSquareRadius = (mw * mw + mh * mh) * k;
        } else {
            int mw = (mMap.getWidth() + Tile.SIZE) / 2;
            int mh = (mMap.getHeight() + Tile.SIZE) / 2;
            mSquareRadius = mw * mw + mh * mh;
        }

        /* scale of tiles zoom-level relative to current position */
        double scale = pos.scale / (1 << zoom);

        double angle = Math.toRadians(pos.bearing);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        int maxx = Tile.SIZE << (zoom - 1);

        // FIXME ???
        SymbolBucket sl = work.symbolLayer;
        sl.clearItems();

        double tileX = (pos.x * (Tile.SIZE << zoom));
        double tileY = (pos.y * (Tile.SIZE << zoom));

        java.util.Map<String, java.util.List<Caption>> captions = new java.util.HashMap<>();
        java.util.Set<String> currentWays = new java.util.HashSet<>();
        java.util.List<Caption> majorRoads=new java.util.ArrayList<>();roadNames.clear();
        for (int i = 0; i < cnt; i++) {
            MapTile tile = tiles[i];
            if (!tile.state(READY | NEW_DATA)) continue;
            LabelTileData data = getLabels(tile);
            if (data == null) continue;
            float dx = flipLongitude((float) (tile.tileX * Tile.SIZE - tileX), maxx);
            float dy = (float) (tile.tileY * Tile.SIZE - tileY);
            for (TextItem item : data.labels) {
                float x=(float)((dx+item.x)*scale),y=(float)((dy+item.y)*scale);
                if(BCycleLabelPolicy.majorRoad(item.text) && isVisible(x,y)) majorRoads.add(new Caption(tile,item,x,y));
                if (!item.text.caption) {
                    currentWays.add(captionKey(item.text, item.label));
                    continue;
                }
                item.width=BCycleCaptionLayout.width(item);
                Caption caption = new Caption(tile, item, (float) ((dx + item.x) * scale),
                        (float) ((dy + item.y) * scale));
                captions.computeIfAbsent(captionKey(item.text, item.label), key -> new java.util.ArrayList<>()).add(caption);
            }
        }

        /* put current label to previous label */
        Label prevLabels = mLabels;

        /* new labels */
        mLabels = null;
        Label l = null;

        /* add currently active labels first */
        for (l = prevLabels; l != null; ) {

            if (l.text.caption) {
                Caption match = currentCaption(l, captions, tileX, tileY, pos.scale, zoom, maxx);
                if (match == null || !isVisible(match.x, match.y) || !BCycleLabelPolicy.eligible(match.item.text,currentZoom,true)) {
                    l = mPool.releaseAndGetNext(l);
                    continue;
                }
                l.clone(match.item);
                TextItem.pool.release(l.item);
                l.item = TextItem.copy(match.item);
                l.x = match.x; l.y = match.y;
                l.tileX = match.tile.tileX; l.tileY = match.tile.tileY; l.tileZ = match.tile.zoomLevel;
                l.bbox.setNormalized(l.x, l.y, cos, -sin, l.width + BCycleLabelPolicy.horizontalGap(l.text),
                        BCycleCaptionLayout.height(l) + BCycleLabelPolicy.verticalGap(l.text), l.text.dy);
                if (checkOverlap(l) == 0) {
                    Label retained = l; l = (Label) l.next;
                    retained.next = null; addLabel(retained);
                } else l = mPool.releaseAndGetNext(l);
                continue;
            }

            if (!currentWays.contains(captionKey(l.text, l.label))) {
                l = mPool.releaseAndGetNext(l);
                continue;
            }
            int diff = l.tileZ - zoom;
            if (diff > 1 || diff < -1) {
                l = mPool.releaseAndGetNext(l);
                continue;
            }

            float div = FastMath.pow(diff);
            float sscale = (float) (pos.scale / (1 << l.tileZ));

            // plus 10 to rather keep label and avoid flickering
            if (l.width > (l.length + 10) * sscale) {
                l = mPool.releaseAndGetNext(l);
                continue;
            }

            float dx = (float) (l.tileX * Tile.SIZE - tileX * div);
            float dy = (float) (l.tileY * Tile.SIZE - tileY * div);

            dx = flipLongitude(dx, maxx);
            l.x = (float) ((dx + l.item.x) * sscale);
            l.y = (float) ((dy + l.item.y) * sscale);
            placeLabelFrom(l, l.item);

            if (!wayIsVisible(l)) {
                l = mPool.releaseAndGetNext(l);
                continue;
            }

            l.bbox.set(l.x, l.y, l.x1, l.y1,
                    l.width + MIN_WAY_DIST,
                    l.text.fontHeight + MIN_WAY_DIST);

            byte overlaps = checkOverlap(l);

            if (dbg)
                Debug.addDebugBox(l, l.item, overlaps, true, sscale);

            if (overlaps == 0) {
                Label ll = l;
                l = (Label) l.next;

                ll.next = null;
                addLabel(ll);
                continue;
            }
            l = mPool.releaseAndGetNext(l);
        }

        /* add way labels */
        for (int i = 0; i < cnt; i++) {
            MapTile t = tiles[i];
            if (!t.state(READY | NEW_DATA))
                continue;

            float dx = (float) (t.tileX * Tile.SIZE - tileX);
            float dy = (float) (t.tileY * Tile.SIZE - tileY);
            dx = flipLongitude(dx, maxx);

            l = addWayLabels(t, l, dx, dy, scale);
        }

        // Choose new captions as one viewport-wide cohort, not in tile arrival order.
        java.util.List<Caption> ordered=new java.util.ArrayList<>();
        for(java.util.List<Caption> group:captions.values()) ordered.addAll(group);
        ordered.sort(java.util.Comparator.comparingInt((Caption c)->BCycleLabelPolicy.importance(c.item.text))
                .thenComparingInt(c->c.item.text.priority)
                .thenComparing(c->c.item.label).thenComparingDouble(c->c.x).thenComparingDouble(c->c.y));
        for(Caption candidate:ordered) if(!BCycleLabelPolicy.majorRoad(candidate.item.text)) l=addNodeLabel(candidate,l,cos,sin);
        l=addMajorRoadLabels(majorRoads,l,cos,sin,scale);

        for (Label ti = mLabels; ti != null; ti = (Label) ti.next) {
            /* add caption symbols */
            if (ti.text.caption) {
                if (!org.oscim.renderer.bucket.BCycleTextBucket.inlineShield(ti.text) && (ti.text.bitmap != null || ti.text.texture != null)) {
                    SymbolItem s = SymbolItem.pool.get();
                    if (ti.text.bitmap != null)
                        s.bitmap = ti.text.bitmap;
                    else
                        s.texRegion = ti.text.texture;
                    s.x = ti.x;
                    s.y = ti.y;
                    s.billboard = true;
                    sl.addSymbol(s);
                }
                continue;
            }

            /* flip way label orientation */
            if (cos * (ti.x2 - ti.x1) - sin * (ti.y2 - ti.y1) < 0) {
                float tmp = ti.x1;
                ti.x1 = ti.x2;
                ti.x2 = tmp;

                tmp = ti.y1;
                ti.y1 = ti.y2;
                ti.y2 = tmp;
            }
        }

        /* add symbol items */
        for (int i = 0; i < cnt; i++) {
            MapTile t = tiles[i];
            if (!t.state(READY | NEW_DATA))
                continue;

            float dx = (float) (t.tileX * Tile.SIZE - tileX);
            float dy = (float) (t.tileY * Tile.SIZE - tileY);
            dx = flipLongitude(dx, maxx);

            LabelTileData ld = getLabels(t);
            if (ld == null)
                continue;

            for (SymbolItem ti : ld.symbols) {
                if (ti.bitmap == null && ti.texRegion == null)
                    continue;

                int x = (int) ((dx + ti.x) * scale);
                int y = (int) ((dy + ti.y) * scale);

                if (!isVisible(x, y))
                    continue;

                SymbolItem s = SymbolItem.pool.get();
                if (ti.bitmap != null)
                    s.bitmap = ti.bitmap;
                else
                    s.texRegion = ti.texRegion;
                s.x = x;
                s.y = y;
                s.billboard = ti.billboard;
                s.rotation = ti.rotation;
                sl.addSymbol(s);
            }
        }

        /* temporary used Label */
        l = (Label) mPool.release(l);

        /* draw text to bitmaps and create vertices */
        fades.build(work,mLabels,mMap.getWidth(),mMap.getHeight());
        work.textLayer.prepare();
        work.textLayer.prepared();

        /* remove tile locks */
        mTileRenderer.releaseTiles(mTileSet);

        return true;
    }

    public void cleanup() {
        fades.clear();
        mLabels = (Label) mPool.releaseAll(mLabels);
        mTileSet.releaseTiles();
    }

    /**
     * group labels by string and type
     */
    protected Label groupLabels(Label labels) {
        for (Label cur = labels; cur != null; cur = (Label) cur.next) {
            /* keep pointer to previous for removal */
            Label p = cur;
            TextStyle t = cur.text;
            float w = cur.width;

            /* iterate through following */
            for (Label l = (Label) cur.next; l != null; l = (Label) l.next) {

                if (w != l.width || t != l.text || !cur.label.equals(l.label)) {
                    p = l;
                    continue;
                } else if (cur.next == l) {
                    l.label = cur.label;
                    p = l;
                    continue;
                }
                l.label = cur.label;

                /* insert l after cur */
                Label tmp = (Label) cur.next;
                cur.next = l;

                /* continue outer loop at l */
                cur = l;

                /* remove l from previous place */
                p.next = l.next;
                l.next = tmp;

                /* continue from previous */
                l = p;
            }
        }
        return labels;
    }
}
