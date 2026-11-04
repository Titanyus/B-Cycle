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
        return TextStyle.builder().style(id).fontSize(16).textKey("name").isCaption(caption).priority(priority).build();
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
    @Test public void captionsRetainTheirPlaceAcrossArrivalOrderAndZoomButExpireWithTheirFeature() throws Exception {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());
        Tiles tiles=new Tiles();
        BCycleLabelPlacement placement=new BCycleLabelPlacement(view.map(),tiles,new ZoomLimiter(null,0,20,20));
        try {
            view.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));view.layout(0,0,800,600);
            MapPosition pos=new MapPosition();pos.x=.5;pos.y=.5;pos.setZoomLevel(16);view.map().viewport().setMapPosition(pos);
            TextStyle village=style("place-village",50,true);
            tiles.tile=tile(16,item("Zell",village,100,100));
            assertTrue(placement.updateLabels(new LabelTask()));Label retained=labels(placement);int firstAge=retained.active;
            tiles.tile=tile(16,item("Zell",village,100,100),item("Aachen",village,101,100));
            placement.updateLabels(new LabelTask());assertSame(retained,labels(placement));assertEquals("Zell",retained.label);
            pos.setZoomLevel(17);view.map().viewport().setMapPosition(pos);
            tiles.tile=tile(17,item("Zell",style("place-village-detail",50,true),200,200),item("Aachen",village,201,200));
            placement.updateLabels(new LabelTask());assertSame(retained,labels(placement));assertEquals(firstAge,retained.active);assertEquals(17,retained.tileZ);
            tiles.tile=tile(17,item("Ulm",style("place-city",30,true),200,200),item("Zell",village,200,200));
            placement.updateLabels(new LabelTask());assertEquals("Ulm",labels(placement).label);
            tiles.tile=tile(17);placement.updateLabels(new LabelTask());assertNull(labels(placement));
        } finally { placement.cleanup();view.disposeMap(); }
    }
    @Test public void roadImportancePrecedesAgeAndEqualRoadsKeepTheVisibleName() throws Exception {
        BicycleMapView view=new BicycleMapView(RuntimeEnvironment.getApplication());Tiles tiles=new Tiles();
        BCycleLabelPlacement placement=new BCycleLabelPlacement(view.map(),tiles,new ZoomLimiter(null,0,20,20));
        try {
            view.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));view.layout(0,0,800,600);
            MapPosition pos=new MapPosition();pos.x=.5;pos.y=.5;pos.setZoomLevel(16);view.map().viewport().setMapPosition(pos);
            TextStyle minor=style("minor",150,false),major=style("primary",120,false);
            tiles.tile=tile(16,item("Zeller Weg",minor,100,100));placement.updateLabels(new LabelTask());
            Label first=labels(placement);
            tiles.tile=tile(16,item("Zeller Weg",minor,100,100),item("Auweg",minor,100,100));placement.updateLabels(new LabelTask());assertSame(first,labels(placement));
            tiles.tile=tile(16,item("Bundesstraße",major,100,100),item("Auweg",minor,100,100),item("Zeller Weg",minor,100,100));placement.updateLabels(new LabelTask());assertEquals("Bundesstraße",labels(placement).label);
            tiles.tile=tile(16);placement.updateLabels(new LabelTask());assertNull(labels(placement));
        } finally { placement.cleanup();view.disposeMap(); }
    }
}
