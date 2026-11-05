package org.oscim.layers.tile.vector.labeling;

import android.view.View;
import de.bcycle.app.BicycleMapView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileRenderer;
import org.oscim.layers.tile.TileSet;
import org.oscim.layers.tile.ZoomLimiter;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.bucket.TextItem;
import org.oscim.theme.styles.TextStyle;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StableLabelPlacementTest {
    static class Tiles extends TileRenderer {
        MapTile tile;
        @Override public Integer getVisibleTiles(TileSet out, boolean replace) {
            out.tiles = new MapTile[]{tile}; out.cnt = 1; return (int)tile.zoomLevel;
        }
        @Override public void releaseTiles(TileSet tiles) { tiles.cnt=0; }
        @Override public void render(GLViewport view) {}
    }
    private static TextStyle style(String id, int priority, boolean caption) {
        return TextStyle.builder().style(id).fontSize(16).textKey("name").isCaption(caption).priority(priority*100).build();
    }
    private static TextItem item(String text, TextStyle style, float x, float y) {
        TextItem item=new TextItem().set(x,y,text,style);
        item.length=240; item.x1=x-120;item.x2=x+120;item.y1=y;item.y2=y;
        return item;
    }
    private static MapTile tile(int zoom, TextItem... items) {
        MapTile tile=new MapTile(1 << (zoom-1),1 << (zoom-1),zoom);
        tile.setState(MapTile.State.LOADING);tile.setState(MapTile.State.NEW_DATA);tile.setState(MapTile.State.READY);
        LabelTileData data=new LabelTileData();
        for(TextItem item:items) data.labels.push(item);
        tile.addData(LabelLayer.LABEL_DATA,data);
        return tile;
    }
    private static Label labels(BCycleLabelPlacement placement) throws Exception {
        java.lang.reflect.Field field=BCycleLabelPlacement.class.getDeclaredField("mLabels");field.setAccessible(true);
        return (Label)field.get(placement);
    }
    @Test public void repeatedRoadNumbersUseTheSameSpacingOnHorizontalVerticalAndDiagonalRoads() throws Exception {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());
        Tiles tiles=new Tiles();
        BCycleLabelPlacement placement=new BCycleLabelPlacement(view.map(),tiles,new ZoomLimiter(null,0,20,20));
        try {
            view.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));view.layout(0,0,1000,1000);
            MapPosition pos=new MapPosition();pos.x=.5;pos.y=.5;pos.setZoomLevel(16);view.map().viewport().setMapPosition(pos);
            TextStyle shield=style("highway-shield",120,true);
            TextStyle otherShield=style("highway-shield-us-other",120,true);
            tiles.tile=tile(16,item("B16",shield,30,20),item("B 16",otherShield,30,130),item("B16",shield,30,360));
            placement.updateLabels(new BCycleLabelTask());int count=0;
            for(Label label=labels(placement);label!=null;label=(Label)label.next) count++;
            assertEquals("Near vertical refs coalesce across style/spacing variants; distant repeat remains",2,count);
            float repeat=BCycleLabelPolicy.roadRepeatDistance(shield,shield,30,30);
            assertTrue(repeat>=220*org.oscim.backend.CanvasAdapter.getScale());
            assertEquals("A8",BCycleLabelPolicy.roadIdentity("A 8"));
            assertFalse("Water/place labels have their own policy",BCycleLabelPolicy.majorRoad(style("waterway-name",1,false)));
        } finally { placement.cleanup();view.disposeMap(); }
    }
    @Test public void roadRefsFillAvailableGapsRegularlyIndependentOfInputOrderAndMapRotation() throws Exception {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());
        try {
            view.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));view.layout(0,0,1000,1000);
            TextStyle road=style("highway-shield",120,true);
            java.util.List<Float> previous=null;
            for(boolean reverse:new boolean[]{false,true}) {
                Tiles tiles=new Tiles();BCycleLabelPlacement placement=new BCycleLabelPlacement(view.map(),tiles,new ZoomLimiter(null,0,20,20));
                try {
                    MapPosition pos=new MapPosition();pos.x=.5;pos.y=.5;pos.setZoomLevel(16);pos.bearing=reverse ? 90 : 0;view.map().viewport().setMapPosition(pos);
                    float[] positions={10,30,60,100,140,230,450,670};TextItem[] items=new TextItem[positions.length];
                    for(int n=0;n<positions.length;n++) { int index=reverse ? positions.length-1-n : n;items[n]=item(index%2==0 ? "B16" : "B 16",road,30,positions[index]); }
                    tiles.tile=tile(16,items);placement.updateLabels(new BCycleLabelTask());
                    java.util.List<Float> selected=new java.util.ArrayList<>();for(Label label=labels(placement);label!=null;label=(Label)label.next) selected.add(label.y);
                    java.util.Collections.sort(selected);
                    assertEquals(java.util.Arrays.asList(10f,230f,450f,670f),selected);
                    if(previous!=null) assertEquals(previous,selected);previous=selected;
                } finally { placement.cleanup(); }
            }
        } finally { view.disposeMap(); }
    }
    @Test public void repeatedRoadAndRiverLabelsInsideOneOldWorldCellRemainSeparateAcrossArrivalOrder() {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());
        try {
            for(boolean river:new boolean[]{false,true}) {
            TextStyle road=style(river ? "waterway-name" : "highway-shield-primary",river ? 1 : 120,false);
            String name=river ? "Donau" : "B16";
            Label first=new Label();first.clone(item(name,road,100,100));first.x=100;first.y=100;first.x1=0;first.x2=200;first.y1=first.y2=100;
            Label second=new Label();second.clone(item(name,road,250,100));second.x=250;second.y=100;second.x1=350;second.x2=150;second.y1=second.y2=100;
            first.next=second;
            BCycleLabelFades fades=new BCycleLabelFades();
            BCycleLabelTask task=new BCycleLabelTask();task.pos.x=.5;task.pos.y=.5;task.pos.setZoomLevel(16);
            fades.build(task,first,800,600);
            assertEquals("Two independently placed labels must not share one fade/geometry state",2,task.textLayer.labels.next==null ? 1 : 2);
            task.textLayer.clearLabels();
            first.next=null;second.next=first;
            fades.build(task,second,800,600);
            int count=0;boolean left=false,right=false;
            for(TextItem label=task.textLayer.labels;label!=null;label=label.next) {
                count++;
                if(Math.abs(label.x-100)<.01) { left=true;assertTrue(label.x2>label.x1); }
                if(Math.abs(label.x-250)<.01) { right=true;assertTrue(label.x2<label.x1); }
            }
            assertEquals(2,count);assertTrue(left && right);task.textLayer.clearLabels();
            }
            TextStyle village=style("place-village",50,true);
            assertTrue("Villages appear earlier",BCycleLabelPolicy.eligible(village,11.5,false));
            assertFalse(BCycleLabelPolicy.eligible(village,11.4,true));
            assertEquals(BCycleLabelPolicy.eligible(village,11.5,true),BCycleLabelPolicy.eligible(village,11.5,false));
        } finally { view.disposeMap(); }
    }
    @Test public void captionsRetainTheirPlaceAcrossArrivalOrderAndZoomButExpireWithTheirFeature() throws Exception {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());
        Tiles tiles=new Tiles();
        BCycleLabelPlacement placement=new BCycleLabelPlacement(view.map(),tiles,new ZoomLimiter(null,0,20,20));
        try {
            view.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));view.layout(0,0,800,600);
            MapPosition pos=new MapPosition();pos.x=.5;pos.y=.5;pos.setZoomLevel(16);view.map().viewport().setMapPosition(pos);
            TextStyle village=style("place-village",50,true);
            tiles.tile=tile(16,item("Zell",village,100,100));
            assertTrue(placement.updateLabels(new BCycleLabelTask()));Label retained=labels(placement);int firstAge=retained.active;
            tiles.tile=tile(16,item("Zell",village,100,100),item("Aachen",village,101,100));
            placement.updateLabels(new BCycleLabelTask());assertSame(retained,labels(placement));assertEquals("Zell",retained.label);
            org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(300));
            tiles.tile=tile(16,item("Aachen",village,101,100));placement.updateLabels(new BCycleLabelTask());
            org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(50));
            tiles.tile=tile(16,item("Zell",village,100,100),item("Aachen",village,101,100));placement.updateLabels(new BCycleLabelTask());
            assertEquals("Zell",labels(placement).label);assertEquals(firstAge,labels(placement).active);retained=labels(placement);
            pos.setZoomLevel(17);view.map().viewport().setMapPosition(pos);
            tiles.tile=tile(17,item("Zell",style("place-village-detail",50,true),200,200),item("Aachen",village,201,200));
            placement.updateLabels(new BCycleLabelTask());assertSame(retained,labels(placement));assertEquals(firstAge,retained.active);assertEquals(17,retained.tileZ);
            tiles.tile=tile(17,item("Ulm",style("place-city",30,true),200,200),item("Zell",village,200,200));
            placement.updateLabels(new BCycleLabelTask());assertEquals("Ulm",labels(placement).label);
            tiles.tile=tile(17);placement.updateLabels(new BCycleLabelTask());assertNull(labels(placement));
        } finally { placement.cleanup();view.disposeMap(); }
    }
    @Test public void longPlaceNamesWrapAtWordsAndShareOneAtlasCellWithMatchingCollisionSize() throws Exception {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());Tiles tiles=new Tiles();
        BCycleLabelPlacement placement=new BCycleLabelPlacement(view.map(),tiles,new ZoomLimiter(null,0,20,20));
        BCycleLabelTask task=new BCycleLabelTask();
        try {
            TextStyle town=style("place-town",40,true);
            org.oscim.renderer.bucket.BCycleCaptionLayout kirchheim=org.oscim.renderer.bucket.BCycleCaptionLayout.get(town,"Kirchheim unter Teck");
            assertEquals("Kirchheim",kirchheim.first);assertEquals("unter Teck",kirchheim.second);
            org.oscim.renderer.bucket.BCycleCaptionLayout dillingen=org.oscim.renderer.bucket.BCycleCaptionLayout.get(town,"Dillingen an der Donau");
            assertEquals("Dillingen",dillingen.first);assertEquals("an der Donau",dillingen.second);
            assertNull(org.oscim.renderer.bucket.BCycleCaptionLayout.get(town,"Herbrechtingen").second);
            assertNull(org.oscim.renderer.bucket.BCycleCaptionLayout.get(town,"Donaueschingenhausen").second);
            assertNull(org.oscim.renderer.bucket.BCycleCaptionLayout.get(town,"Bad Urach").second);
            assertNull(org.oscim.renderer.bucket.BCycleCaptionLayout.get(style("highway-name",120,false),"Dillingen an der Donau").second);
            assertTrue(dillingen.width<town.paint.measureText("Dillingen an der Donau"));
            assertTrue(dillingen.height>town.fontHeight*2);
            view.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));view.layout(0,0,800,600);
            MapPosition pos=new MapPosition();pos.x=.5;pos.y=.5;pos.setZoomLevel(16);view.map().viewport().setMapPosition(pos);
            tiles.tile=tile(16,item("Dillingen an der Donau",town,100,100));assertTrue(placement.updateLabels(task));
            assertEquals("Dillingen an der Donau",labels(placement).label);
            assertEquals(dillingen.width,labels(placement).width,.001f);
            java.lang.reflect.Field count=org.oscim.renderer.bucket.RenderBucket.class.getDeclaredField("numVertices");count.setAccessible(true);
            assertEquals("Both lines share one atlas rectangle/fade",4,count.getInt(task.textLayer));
            org.oscim.backend.canvas.Bitmap atlas=task.textLayer.textures.bitmap;
            int[] pixels=atlas.getPixels();int upper=0,lower=0;
            for(int y=0;y<(int)dillingen.height;y++) for(int x=0;x<(int)dillingen.width+8;x++) {
                if(android.graphics.Color.alpha(pixels[y*atlas.getWidth()+x])>64) {
                    if(y<town.fontHeight) upper++;else lower++;
                }
            }
            assertTrue("First line has glyph pixels",upper>10);assertTrue("Second line has glyph pixels",lower>10);
        } finally { task.textLayer.clear();placement.cleanup();view.disposeMap(); }
    }
    @Test public void roadShieldAndNumberShareOneAtlasCellAndOneFadeInsteadOfASeparatePlate() throws Exception {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());Tiles tiles=new Tiles();
        BCycleLabelPlacement placement=new BCycleLabelPlacement(view.map(),tiles,new ZoomLimiter(null,0,20,20));
        android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(40,24,android.graphics.Bitmap.Config.ARGB_8888);
        image.eraseColor(android.graphics.Color.WHITE);
        org.oscim.android.canvas.AndroidBitmap shield=new org.oscim.android.canvas.AndroidBitmap(image);
        BCycleLabelTask task=new BCycleLabelTask();
        try {
            view.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));view.layout(0,0,800,600);
            MapPosition pos=new MapPosition();pos.x=.5;pos.y=.5;pos.setZoomLevel(16);view.map().viewport().setMapPosition(pos);
            TextStyle text=TextStyle.builder().style("highway-shield").fontSize(14).color(android.graphics.Color.BLACK).textKey("ref").isCaption(true).priority(12000).bitmap(shield).build();
            tiles.tile=tile(16,item("B 12",text,100,100));assertTrue(placement.updateLabels(task));
            java.lang.reflect.Field count=org.oscim.renderer.bucket.RenderBucket.class.getDeclaredField("numVertices");count.setAccessible(true);
            assertEquals("No separately blinking background",0,count.getInt(task.symbolLayer));
            assertEquals(4,count.getInt(task.textLayer));
            org.oscim.backend.canvas.Bitmap atlas=task.textLayer.textures.bitmap;
            int[] pixels=atlas.getPixels();int white=0,ink=0;
            for(int y=0;y<24;y++) for(int x=0;x<40;x++) {
                int pixel=pixels[y*atlas.getWidth()+x];
                if(android.graphics.Color.alpha(pixel)>200 && android.graphics.Color.red(pixel)>240) white++;
                if(android.graphics.Color.alpha(pixel)>200 && android.graphics.Color.red(pixel)<128) ink++;
            }
            assertTrue("Shield rasterized into glyph atlas",white>100);
            assertTrue("Actual road number rasterized on the shield",ink>10);
            tiles.tile=tile(16);placement.updateLabels(new BCycleLabelTask());assertNull(labels(placement));
        } finally { task.textLayer.clear();placement.cleanup();view.disposeMap();shield.recycle(); }
    }

    @Test public void cohortsAndFadeReversalsAreContinuousAndTileRanksCannotEvictAnIncumbent() {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());
        try {
            TextStyle town=style("place-town",40,true),village=style("place-village",50,true);
            assertTrue(BCycleLabelPolicy.eligible(town,9.5,false));
            assertFalse(BCycleLabelPolicy.eligible(town,9.4,false));
            assertTrue(BCycleLabelPolicy.eligible(village,11.5,false));
            for(double zoom:new double[]{8,9,9.5,10,11,11.5,12,13.5,14.5}) {
                assertEquals(BCycleLabelPolicy.eligible(town,zoom,true),BCycleLabelPolicy.eligible(town,zoom,false));
                assertEquals(BCycleLabelPolicy.eligible(village,zoom,true),BCycleLabelPolicy.eligible(village,zoom,false));
            }
            assertTrue(BCycleLabelPolicy.horizontalGap(village)>village.fontHeight);
            Label visible=new Label(),incoming=new Label();
            visible.text=TextStyle.builder().set(town).priority(4099).build();visible.active=1;visible.label="Herbrechtingen";
            incoming.text=TextStyle.builder().set(town).priority(4001).build();incoming.active=2;incoming.label="Hürben";
            assertFalse(BCycleLabelPlacement.wins(incoming,visible));
            TextStyle water=TextStyle.builder().set(town).style("water-name").priority(10).build();
            assertTrue(BCycleLabelPolicy.importance(town)<BCycleLabelPolicy.importance(water));
            assertTrue(BCycleLabelPolicy.importance(style("primary",120,false))<BCycleLabelPolicy.importance(water));
            float half=BCycleLabelFades.value(0,1,1000,240,1120);
            assertEquals(.5f,half,.001f);
            assertEquals(half,BCycleLabelFades.value(half,0,1120,240,1120),.001f);
            assertEquals(0,BCycleLabelFades.value(half,0,1120,240,1360),.001f);
        } finally { view.disposeMap(); }
    }

    @Test public void roadImportancePrecedesAgeAndEqualRoadsKeepTheVisibleName() throws Exception {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());Tiles tiles=new Tiles();
        BCycleLabelPlacement placement=new BCycleLabelPlacement(view.map(),tiles,new ZoomLimiter(null,0,20,20));
        try {
            view.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));view.layout(0,0,800,600);
            MapPosition pos=new MapPosition();pos.x=.5;pos.y=.5;pos.setZoomLevel(16);view.map().viewport().setMapPosition(pos);
            TextStyle minor=style("minor",150,false),major=style("primary",120,false);
            tiles.tile=tile(16,item("Zeller Weg",minor,100,100));placement.updateLabels(new BCycleLabelTask());
            Label first=labels(placement);
            tiles.tile=tile(16,item("Zeller Weg",minor,100,100),item("Auweg",minor,100,100));placement.updateLabels(new BCycleLabelTask());assertSame(first,labels(placement));
            tiles.tile=tile(16,item("Bundesstraße",major,100,100),item("Auweg",minor,100,100),item("Zeller Weg",minor,100,100));placement.updateLabels(new BCycleLabelTask());assertEquals("Bundesstraße",labels(placement).label);
            tiles.tile=tile(16);placement.updateLabels(new BCycleLabelTask());assertNull(labels(placement));
        } finally { placement.cleanup();view.disposeMap(); }
    }
}
