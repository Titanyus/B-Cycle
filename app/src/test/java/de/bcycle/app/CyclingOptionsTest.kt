package de.bcycle.app

import android.app.Activity
import android.graphics.Color
import android.widget.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.*
import org.oscim.layers.tile.MapTile
import org.oscim.layers.tile.vector.labeling.BCycleLabelLayer
import org.oscim.theme.styles.LineStyle
import org.oscim.tiling.*
import org.robolectric.*
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class CyclingOptionsTest {
    @Test fun independentTrackStyleControlsExportProfileAndNativeRoutesPreserveOtherTracks() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible();val c=host.get()
        val prefs=c.getSharedPreferences("bike",0);val themes=ThemeManager(prefs);val map=BicycleMapView(c)
        try {
            val before=MapRouteStyles.read(prefs,MapLineKind.SAVED)
            MapStyleOverrides().update(MapStyleTarget.CYCLE,MapObjectStyle(Color.YELLOW,alpha=128,widthPercent=150)).write(prefs)
            assertEquals(Color.YELLOW,MapRouteStyles.read(prefs,MapLineKind.CYCLEWAYS).color)
            val ui=MapLineStyleView(c,prefs,themes,DialogNavigation(),kinds=listOf(MapLineKind.CYCLEWAYS,MapLineKind.CYCLE_ROUTES),heading=null) {}
            c.setContentView(ui)
            val ways=ui.findViewWithTag<Switch>(VtmBaseMap.cyclewaysKey);val routes=ui.findViewWithTag<Switch>(MapVectorLayer.CYCLE_ROUTES.key)
            assertTrue(ways.isChecked);assertFalse(routes.isChecked);routes.performClick();ways.performClick()
            ui.findViewWithTag<EditText>("map_line_hex_cycleRoutes").setText("#12A4CF")
            assertEquals(Color.rgb(18,164,207),MapRouteStyles.read(prefs,MapLineKind.CYCLE_ROUTES).color)
            assertEquals(Color.YELLOW,MapRouteStyles.read(prefs,MapLineKind.CYCLEWAYS).color);assertEquals(before,MapRouteStyles.read(prefs,MapLineKind.SAVED))
            val effective=MapCyclingStyles.effective(prefs)
            val exported=JSONObject(MapStyleOverrides.read(prefs).export(prefs));MapStyleOverrides.parse(exported)
            assertTrue(exported.getJSONObject("visibility").getBoolean("CYCLE_ROUTES"));assertFalse(exported.getBoolean("cycleways"))
            assertEquals(MapRouteStyles.read(prefs,MapLineKind.CYCLE_ROUTES).color,exported.getJSONObject("cyclingStyles").getJSONObject("cycleRoutes").getInt("color"))
            val bank=BikeUiConfigurationStore(prefs);bank.ensure(listOf(0,1));bank.save(0)
            prefs.edit().putString(MapRouteStyles.key,"{}").putBoolean(MapVectorLayer.CYCLE_ROUTES.key,false).apply();bank.save(1);bank.apply(0)
            assertTrue(MapVectorLayers.read(prefs).contains(MapVectorLayer.CYCLE_ROUTES));assertEquals(effective,MapCyclingStyles.effective(prefs))
            val base=VtmBaseMap(c,map.map(),MapSource.VECTOR,"",false,map::queueEvent,MapVectorLayers.read(prefs),effective)
            try {
                val all=map.map().layers();val layer=all.filterIsInstance<VtmCyclingRoutesLayer>().single()
                assertTrue(all.indexOf(layer)<all.indexOf(all.filterIsInstance<BCycleLabelLayer>().single()))
                val line=layer.theme.matchElement(GeometryBuffer.GeometryType.LINE,TagSet(),12).orEmpty().filterIsInstance<LineStyle>().single()
                assertEquals(MapRouteStyles.read(prefs,MapLineKind.CYCLE_ROUTES).argb,line.current().color)
                assertEquals(0,layer.theme.mapBackground)
                val overview=layer.theme.matchElement(GeometryBuffer.GeometryType.LINE,TagSet(),7).orEmpty().filterIsInstance<LineStyle>().single()
                assertTrue(overview.width<line.width)
                val backup=SettingsBackup(prefs,c.getSharedPreferences("route-export",0),themes)
                val bytes=backup.export(setOf(BackupSection.MAPS));prefs.edit().putString(MapRouteStyles.key,"{}").apply()
                backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.MAPS)))
                assertEquals(effective,MapCyclingStyles.effective(prefs))
            } finally { base.onDetach() }
            assertTrue(map.map().layers().none { it is VtmCyclingRoutesLayer })
        } finally { map.disposeMap();host.pause().stop().destroy() }
    }
    @Test fun overviewFetchesClippedVectorSegmentsWithoutDetailTileFanoutAndStopsAfterDispose() {
        val urls=ArrayList<String>()
        val data="""{"features":[{"geometry":{"type":"MultiLineString","coordinates":[[[0,0],[1000,-1000]],[[1000,-1000],[2000,-2000]]]}},{"geometry":{"type":"Point","coordinates":[0,0]}}]}"""
        val http=OkHttpClient.Builder().addInterceptor { chain ->
            urls.add(chain.request().url.toString())
            val body=if(chain.request().url.encodedPath.endsWith("by_area")) """{"results":[{"id":123},{"id":456}]}""" else data
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body.toResponseBody()).build()
        }.build()
        val source=CyclingRoutesSource(http).dataSource
        val map=BicycleMapView(RuntimeEnvironment.getApplication())
        try {
            val tile=MapTile(128,128,8);var count=0;var result:QueryResult?=null
            val sink=object:ITileDataSink {
                override fun process(element:MapElement) { assertTrue(element.isLine);assertEquals(4,element.pointNextPos);count++ }
                override fun setTileImage(bitmap:org.oscim.backend.canvas.Bitmap) { fail("Must stay vector") }
                override fun completed(value:QueryResult) { result=value }
            }
            source.query(tile,sink);assertEquals(QueryResult.SUCCESS,result);assertEquals(2,count);assertEquals(2,urls.size)
            assertTrue(urls.last().contains("relations=123,456"))
            source.query(tile,sink);assertEquals("Second query uses bounded geometry cache",2,urls.size)
            source.dispose();source.query(tile,sink);assertEquals(QueryResult.FAILED,result);assertEquals(2,urls.size)
        } finally { source.dispose();http.dispatcher.executorService.shutdown();map.disposeMap() }
    }
}
