package de.bcycle.app

import android.graphics.Color
import android.os.Looper
import android.view.View
import android.widget.Spinner
import android.widget.Switch
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.*
import org.oscim.layers.tile.vector.VectorTileLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.theme.ThemeLoader
import org.oscim.theme.styles.LineStyle
import org.oscim.tiling.*
import org.oscim.tiling.source.mvt.TileDecoder
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class VtmVectorMapTest {
    @Test fun brightUsesItsPaletteWithFlatBuildingsAndCameraAlwaysLooksDown() {
        val c=RuntimeEnvironment.getApplication();val map=BicycleMapView(c)
        try {
            val theme=ThemeLoader.load(VtmCycleTheme(false))
            assertEquals(Color.rgb(248,244,240),theme.mapBackground)
            val water=TagSet().apply { add(Tag("layer","water"));add(Tag("class","lake")) }
            assertTrue(theme.matchElement(GeometryBuffer.GeometryType.POLY,water,16).orEmpty()
                .filterIsInstance<org.oscim.theme.styles.AreaStyle>().any { it.color==Color.rgb(174,207,226) })
            // OpenMapTiles groups forest/meadow/crops by class, not their OSM subclass.
            val ruralAreas=listOf(
                Triple("wood","forest",Color.rgb(169,207,145)),
                Triple("grass","meadow",Color.rgb(209,231,181)),
                Triple("farmland","farmland",Color.rgb(232,229,184)),
                Triple("farmland","orchard",Color.rgb(232,229,184)),
                Triple("scrub","scrub",Color.rgb(196,214,173)),
                Triple("wetland","marsh",Color.rgb(198,222,208))
            )
            for((kind,subclass,color) in ruralAreas) for(zoom in listOf(10,14,19)) {
                val tags=TagSet().apply { add(Tag("layer","landcover"));add(Tag("class",kind));add(Tag("subclass",subclass)) }
                assertTrue("opaque $kind/$subclass at zoom $zoom",theme.matchElement(GeometryBuffer.GeometryType.POLY,tags,zoom).orEmpty()
                    .filterIsInstance<org.oscim.theme.styles.AreaStyle>().any { it.color==color && Color.alpha(it.color)==255 })
            }
            val motorway=TagSet().apply { add(Tag("layer","transportation"));add(Tag("class","motorway")) }
            assertTrue(theme.matchElement(GeometryBuffer.GeometryType.LINE,motorway,16).orEmpty()
                .filterIsInstance<LineStyle>().any { it.color==Color.rgb(255,204,136) })
            val building=TagSet().apply { add(Tag("layer","building"));add(Tag("height","40"));add(Tag("min_height","0")) }
            for(zoom in listOf(13,16,19)) {
                val styles=theme.matchElement(GeometryBuffer.GeometryType.POLY,building,zoom).orEmpty()
                assertTrue("flat footprint at zoom $zoom",styles.any { it is org.oscim.theme.styles.AreaStyle })
                assertFalse("no extrusion even with height at zoom $zoom",styles.any { it is org.oscim.theme.styles.ExtrusionStyle })
            }
            assertFalse(map.map().eventLayer.tiltEnabled())
            map.setCamera(map.camera.apply { tilt=55f;roll=20f;bearing=45f })
            assertEquals(0f,map.camera.tilt,0f);assertEquals(0f,map.camera.roll,0f)
            assertEquals("heading rotation remains available",45f,map.bearing,.01f)
            map.map().viewport().setTilt(55f)
            assertEquals("native camera also cannot tilt",0f,map.camera.tilt,0f)
            theme.dispose()
        } finally { map.disposeMap() }
    }
    @Test fun realOpenFreeMapTileDecodesAndCyclewaysAreAnOptionalStyle() {
        val map=BicycleMapView(RuntimeEnvironment.getApplication())
        try {
            val normal=ThemeLoader.load(VtmCycleTheme(false))
            val cycling=ThemeLoader.load(VtmCycleTheme(true))
            var features=0;var named=0;var styled=0;var rural=0
            val sink=object:ITileDataSink {
                override fun process(element:MapElement) {
                    features++
                    if(element.tags.getValue("layer")=="landcover" && element.tags.getValue("class") in setOf("wood","grass","farmland","scrub","wetland")) {
                        rural++
                        assertTrue("real rural polygon ${element.tags}",normal.matchElement(element.type,element.tags,14).orEmpty()
                            .filterIsInstance<org.oscim.theme.styles.AreaStyle>().any { Color.alpha(it.color)==255 })
                    }
                    if(element.tags.getValue("name")!=null) named++
                    if(!normal.matchElement(element.type,element.tags,14).isNullOrEmpty()) styled++
                }
                override fun setTileImage(bitmap:org.oscim.backend.canvas.Bitmap) { fail("MVT must remain vector geometry") }
                override fun completed(result:QueryResult) {}
            }
            javaClass.getResourceAsStream("/maps/openfreemap-14-8663-5675.pbf")!!.use {
                assertTrue(TileDecoder("de").decode(Tile(8663,5675,14),sink,it))
            }
            assertTrue("real rural polygons",rural>0)
            assertTrue("real geometry",features>50);assertTrue("real labels",named>0);assertTrue("schema matches theme",styled>50)
            fun highlighted(theme:org.oscim.theme.IRenderTheme,subclass:String,bicycle:String?=null):Boolean {
                val tags=TagSet().apply { add(Tag("layer","transportation"));add(Tag("class","path"));add(Tag("subclass",subclass));bicycle?.let { add(Tag("bicycle",it)) } }
                return theme.matchElement(GeometryBuffer.GeometryType.LINE,tags,16).orEmpty().filterIsInstance<LineStyle>().any { it.color==Color.rgb(36,120,196) }
            }
            assertFalse(highlighted(normal,"cycleway"));assertTrue(highlighted(cycling,"cycleway"))
            assertTrue(highlighted(cycling,"path","designated"));assertFalse(highlighted(cycling,"footway"))
            normal.dispose();cycling.dispose()
        } finally { map.disposeMap() }
    }
    @Test fun vectorAndRasterSwitchKeepRouteLayerAndPersistBikeSettings() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val runtime=RideRuntime.get(activity)
            val panel=testDescendants(activity.window.decorView).filterIsInstance<MapPanel>().single()
            val routes=panel.mapView.map().layers().filterIsInstance<VtmRouteLayer>().single()
            MainActivity::class.java.getDeclaredMethod("mapSettings").apply { isAccessible=true }.invoke(activity)
            shadowOf(Looper.getMainLooper()).idle()
            val root=ShadowAlertDialog.getLatestAlertDialog().window!!.decorView
            val spinner=testDescendants(root).filterIsInstance<Spinner>().single()
            spinner.setSelection(MapSource.VECTOR.ordinal);shadowOf(Looper.getMainLooper()).idle()
            assertEquals("VECTOR",runtime.preferences.getString("mapSource",null))
            val toggle=root.findViewWithTag<Switch>(VtmBaseMap.cyclewaysKey)
            assertEquals(View.VISIBLE,toggle.visibility);toggle.isChecked=false;shadowOf(Looper.getMainLooper()).idle()
            assertFalse(runtime.preferences.getBoolean(VtmBaseMap.cyclewaysKey,true))
            val layers=panel.mapView.map().layers()
            assertTrue(layers.indexOf(layers.filterIsInstance<VectorTileLayer>().single())<layers.indexOf(routes))
            assertTrue(layers.indexOf(layers.filterIsInstance<LabelLayer>().single())<layers.indexOf(routes))
            panel.pause();assertFalse(layers.filterIsInstance<VectorTileLayer>().single().isEnabled)
            panel.resume();assertTrue(layers.filterIsInstance<VectorTileLayer>().single().isEnabled)
            val bike=runtime.preferences;val route=activity.getSharedPreferences("route-export",0)
            val backup=SettingsBackup(bike,route,ThemeManager(bike))
            val bytes=backup.export(setOf(BackupSection.MAPS))
            assertEquals(false,BikeUiConfigurationScope.capture(bike).maps[VtmBaseMap.cyclewaysKey])
            bike.edit().putString("mapSource","OSM").putBoolean(VtmBaseMap.cyclewaysKey,true).commit()
            backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.MAPS)))
            assertEquals("VECTOR",bike.getString("mapSource",null));assertFalse(bike.getBoolean(VtmBaseMap.cyclewaysKey,true))
            spinner.setSelection(MapSource.OSM.ordinal);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.GONE,toggle.visibility)
            assertTrue(layers.none { it is VectorTileLayer || it is LabelLayer })
            assertTrue(layers.indexOf(layers.filterIsInstance<VtmRasterLayer>().single())<layers.indexOf(routes))
            assertSame(routes,layers.filterIsInstance<VtmRouteLayer>().single())
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun datedTileDirectoryIsDiscoveredOnceAndValidated() {
        val template="https://tiles.openfreemap.org/planet/20261025_080012_pt/{z}/{x}/{y}.pbf"
        var requests=0
        val http=OkHttpClient.Builder().addInterceptor { chain ->
            requests++;assertEquals("/planet",chain.request().url.encodedPath)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""{"tiles":["$template"]}""".toResponseBody()).build()
        }.build()
        assertEquals(template,OpenFreeMapTiles.template(http));assertEquals(template,OpenFreeMapTiles.template(http));assertEquals(1,requests)
        assertThrows(IllegalArgumentException::class.java) { OpenFreeMapTiles.validate(template.replace("https:","http:")) }
        assertThrows(IllegalArgumentException::class.java) { OpenFreeMapTiles.validate(template.replace("tiles.openfreemap.org","example.org")) }
    }
}
