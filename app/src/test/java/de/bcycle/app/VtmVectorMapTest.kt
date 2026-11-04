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
import org.oscim.layers.tile.vector.labeling.BCycleLabelLayer as LabelLayer
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
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class VtmVectorMapTest {
    @Test fun brightUsesItsPaletteWithFlatBuildingsAndCameraAlwaysLooksDown() {
        val c=RuntimeEnvironment.getApplication();val map=BicycleMapView(c)
        try {
            val theme=VtmBrightTheme.load(false)
            assertEquals(Color.rgb(248,244,240),theme.mapBackground)
            for(layer in listOf("poi","aerodrome_label")) for(kind in listOf("fuel","restaurant","railway","aerodrome")) {
                val poi=TagSet().apply { add(Tag("layer",layer));add(Tag("class",kind));add(Tag("rank","1"));add(Tag("name","POI")) }
                assertTrue(theme.matchElement(GeometryBuffer.GeometryType.POINT,poi,19).isNullOrEmpty())
            }
            for(level in listOf(8,9,10)) assertTrue(theme.matchElement(GeometryBuffer.GeometryType.LINE,
                TagSet().apply { add(Tag("layer","boundary"));add(Tag("admin_level",level.toString())) },16).isNullOrEmpty())
            assertFalse(theme.matchElement(GeometryBuffer.GeometryType.LINE,
                TagSet().apply { add(Tag("layer","boundary"));add(Tag("admin_level","4")) },16).isNullOrEmpty())
            val village=TagSet().apply { add(Tag("layer","place"));add(Tag("class","village"));add(Tag("name","Billenhausen")) }
            assertFalse(theme.matchElement(GeometryBuffer.GeometryType.POINT,village,16).isNullOrEmpty())
            fun priority(layer:String,kind:String)=theme.matchElement(if(layer=="transportation_name") GeometryBuffer.GeometryType.LINE else GeometryBuffer.GeometryType.POINT,
                TagSet().apply { add(Tag("layer",layer));add(Tag("class",kind));add(Tag("rank","1")) },19).orEmpty()
                .filterIsInstance<org.oscim.theme.styles.TextStyle>().minOf { it.priority }
            assertTrue(priority("place","city")<priority("place","town"))
            assertTrue(priority("place","town")<priority("place","village"))
            assertTrue(priority("transportation_name","primary")<priority("transportation_name","minor"))
            assertTrue(priority("transportation_name","minor")<priority("transportation_name","service"))
            val water=TagSet().apply { add(Tag("layer","water"));add(Tag("class","lake")) }
            assertTrue(theme.matchElement(GeometryBuffer.GeometryType.POLY,water,16).orEmpty()
                .filterIsInstance<org.oscim.theme.styles.AreaStyle>().any { it.color==Color.rgb(191,217,242) })
            val ruralAreas=listOf(
                Triple("wood","forest",Color.argb(26,102,170,68)),
                Triple("grass","meadow",Color.rgb(216,232,200)),
                Triple("grass","scrub",Color.rgb(216,232,200)),
                Triple("sand","sand",Color.rgb(245,238,188))
            )
            for((kind,subclass,color) in ruralAreas) for(zoom in listOf(10,14,19)) {
                val tags=TagSet().apply { add(Tag("layer","landcover"));add(Tag("class",kind));add(Tag("subclass",subclass)) }
                assertTrue("source color $kind/$subclass at zoom $zoom",theme.matchElement(GeometryBuffer.GeometryType.POLY,tags,zoom).orEmpty()
                    .filterIsInstance<org.oscim.theme.styles.AreaStyle>().any { it.color==color })
            }
            val farmland=TagSet().apply { add(Tag("layer","landcover"));add(Tag("class","farmland")) }
            assertTrue("reference has no farmland paint",theme.matchElement(GeometryBuffer.GeometryType.POLY,farmland,16).isNullOrEmpty())
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
            val normal=VtmBrightTheme.load(false)
            val cycling=VtmBrightTheme.load(true)
            var features=0;var named=0;var styled=0;var rural=0
            val sink=object:ITileDataSink {
                override fun process(element:MapElement) {
                    features++
                    if(element.tags.getValue("layer")=="landcover") {
                        val kind=element.tags.getValue("class")
                        val areas=normal.matchElement(element.type,element.tags,14).orEmpty().filterIsInstance<org.oscim.theme.styles.AreaStyle>()
                        if(kind=="wood" || kind=="grass") {
                            rural++
                            val expected=if(kind=="wood") Color.argb(26,102,170,68) else Color.rgb(216,232,200)
                            assertTrue("source class $kind/${element.tags.getValue("subclass")}",areas.any { it.color==expected })
                        } else if(kind=="farmland") assertTrue("no invented farmland style",areas.isEmpty())
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
    @Test fun brightRoadHierarchyDashesFiltersAndSchemaAliasesFollowTheReference() {
        val map=BicycleMapView(RuntimeEnvironment.getApplication())
        val theme=VtmBrightTheme.load(false)
        try {
            fun lines(kind:String,zoom:Int=16,vararg extra:Pair<String,String>):List<LineStyle> {
                val tags=TagSet().apply { add(Tag("layer","transportation"));add(Tag("class",kind));extra.forEach { add(Tag(it.first,it.second)) } }
                return theme.matchElement(GeometryBuffer.GeometryType.LINE,tags,zoom).orEmpty().filterIsInstance<LineStyle>()
            }
            val road=lines("motorway");val path=lines("path");val track=lines("track")
            assertEquals(listOf("highway-motorway-casing","highway-motorway"),road.map { it.style })
            val roadFill=road.last();val pathFill=path.single()
            val progress=(Math.pow(1.2,8.0)-1)/(Math.pow(1.2,13.0)-1)
            assertEquals("source exponential width at MapLibre zoom 15 / VTM 16",((.5+17.5*progress)/2).toFloat(),roadFill.width,.001f)
            assertTrue("paths significantly thinner than major roads",pathFill.width < roadFill.width/2)
            assertEquals("source path width at MapLibre zoom 15",.6f,pathFill.width,.001f)
            assertEquals("dash color",Color.rgb(204,187,170),pathFill.color)
            assertNotNull("actual native dash texture",pathFill.texture);assertFalse(pathFill.randomOffset)
            assertTrue("fixed widths avoid VTM's latitude multiplier",road.all { it.fixed } && pathFill.fixed)
            assertEquals(listOf("highway-minor-casing","highway-minor"),track.map { it.style })
            assertTrue("track narrower than motorway at source zoom 15",track.last().width<roadFill.width)
            val tunnel=lines("motorway",16,"brunnel" to "tunnel")
            assertEquals(listOf("tunnel-motorway-casing","tunnel-motorway"),tunnel.map { it.style })
            assertNotNull(tunnel.first().texture)
            assertEquals(listOf("bridge-trunk-primary-casing","bridge-trunk-primary"),lines("primary",16,"brunnel" to "bridge").map { it.style })
            assertEquals(listOf("highway-motorway-link-casing","highway-motorway-link"),lines("motorway",16,"ramp" to "1").map { it.style })
            assertEquals(listOf("tunnel-minor-casing","tunnel-minor"),lines("minor",16,"brunnel" to "tunnel").map { it.style })
            assertTrue("source path remains visible at low zoom",lines("path",10).isNotEmpty())
            assertTrue("one-way sprites available",theme.matchElement(GeometryBuffer.GeometryType.LINE,TagSet().apply {
                add(Tag("layer","transportation"));add(Tag("class","minor"));add(Tag("oneway","-1"))
            },16).orEmpty().any { it is org.oscim.theme.styles.SymbolStyle && it.bitmap!=null })
            val numeric=BrightFilter.compile(org.json.JSONArray("[\"all\",[\">=\",\"rank\",15],[\"<=\",\"rank\",24],[\"any\",[\"!has\",\"level\"],[\"==\",\"level\",0]]]"))
            assertTrue(numeric(GeometryBuffer.GeometryType.POINT,TagSet().apply { add(Tag("rank","20")) }))
            assertFalse(numeric(GeometryBuffer.GeometryType.POINT,TagSet().apply { add(Tag("rank","25")) }))
            assertFalse(numeric(GeometryBuffer.GeometryType.POINT,TagSet().apply { add(Tag("rank","20"));add(Tag("level","1")) }))
            assertTrue(numeric(GeometryBuffer.GeometryType.POINT,TagSet().apply { add(Tag("rank","20"));add(Tag("level","0.0")) }))
        } finally { theme.dispose();map.disposeMap() }
    }
    @Test fun cyclewaysRemainThinInTheOverviewAndUseTheExistingEditorAndVisibilitySwitch() {
        val view=BicycleMapView(RuntimeEnvironment.getApplication())
        val theme=VtmBrightTheme.load(true)
        val off=VtmBrightTheme.load(false)
        val custom=VtmBrightTheme.load(true,style=MapStyleOverrides().update(MapStyleTarget.CYCLE,MapObjectStyle(Color.RED,MapDash.DASHED)))
        try {
            val tags=TagSet().apply { add(Tag("layer","transportation"));add(Tag("class","path"));add(Tag("subclass","cycleway")) }
            fun highlight(zoom:Int)=theme.matchElement(GeometryBuffer.GeometryType.LINE,tags,zoom).orEmpty().filterIsInstance<LineStyle>().singleOrNull { it.style=="bcycle-cycleway" }
            assertNull(highlight(8))
            assertEquals(.6f,highlight(9)!!.width,.001f);assertEquals(.9f,highlight(11)!!.width,.001f)
            assertEquals(1.4f,highlight(12)!!.width,.001f);assertEquals(2.5f,highlight(13)!!.width,.001f)
            assertTrue(off.matchElement(GeometryBuffer.GeometryType.LINE,tags,9).orEmpty().none { it is LineStyle && it.style=="bcycle-cycleway" })
            assertTrue(custom.matchElement(GeometryBuffer.GeometryType.LINE,tags,9).orEmpty().filterIsInstance<LineStyle>().any { it.style=="bcycle-cycleway" && it.color==Color.RED && it.texture!=null })
            val designated=TagSet().apply { add(Tag("layer","transportation"));add(Tag("class","track"));add(Tag("subclass","track"));add(Tag("bicycle","designated")) }
            assertTrue(theme.matchElement(GeometryBuffer.GeometryType.LINE,designated,9).orEmpty().filterIsInstance<LineStyle>().any { it.style=="bcycle-cycleway" })
        } finally { theme.dispose();off.dispose();custom.dispose();view.disposeMap() }
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
            val options=root.findViewWithTag<BicycleFrameDropdown>("map_vector_layers")
            assertEquals(View.VISIBLE,options.visibility)
            options.setExpanded(true)
            val pathSwitch=root.findViewWithTag<Switch>(MapVectorLayer.PATHS.key)
            pathSwitch.isChecked=false;shadowOf(Looper.getMainLooper()).idle()
            assertFalse(MapVectorLayer.PATHS in MapVectorLayers.read(runtime.preferences))
            val quick=panel.findViewWithTag<android.widget.Button>("map_layers_button")
            assertEquals(View.VISIBLE,quick.visibility);quick.performClick()
            val popup=MapPanel::class.java.getDeclaredField("layersPopup").apply { isAccessible=true }.get(panel) as MapLayersPopup
            assertTrue(popup.isShowing)
            val liveMap=panel.mapView
            popup.contentView.findViewWithTag<Switch>("map_quick_${MapVectorLayer.PATHS.key}").isChecked=true
            assertTrue(MapVectorLayer.PATHS in MapVectorLayers.read(runtime.preferences))
            assertSame("Layer toggles retain the live GL host",liveMap,panel.mapView)
            assertTrue("Dropdown stays open for comparing the map",popup.isShowing);popup.dismiss()
            assertNull("Editor is no longer nested in layer switches",popup.contentView.findViewWithTag<View>("map_style_editor"))
            val editorButton=panel.findViewWithTag<android.widget.Button>("map_style_editor_button")
            assertEquals(View.VISIBLE,editorButton.visibility);editorButton.performClick()
            val editorPopup=MapPanel::class.java.getDeclaredField("stylePopup").apply { isAccessible=true }.get(panel) as MapStyleEditorPopup
            assertTrue(editorPopup.isShowing)
            assertNotNull(editorPopup.contentView.findViewWithTag<View>("map_style_rgb_0"))
            assertSame(liveMap,panel.mapView);editorPopup.dismiss()


            val contourSwitch=root.findViewWithTag<Switch>(MapVectorLayer.CONTOURS.key)
            val layers=panel.mapView.map().layers()
            assertTrue(layers.none { it is VtmContourLayer })
            contourSwitch.isChecked=true;shadowOf(Looper.getMainLooper()).idle()
            val contour=layers.filterIsInstance<VtmContourLayer>().single()
            assertTrue(layers.indexOf(contour)<layers.indexOf(layers.filterIsInstance<LabelLayer>().single()))
            assertTrue(layers.indexOf(contour)<layers.indexOf(routes))
            contourSwitch.isChecked=false;shadowOf(Looper.getMainLooper()).idle()
            assertTrue(layers.none { it is VtmContourLayer })
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
            assertEquals(View.GONE,toggle.visibility);assertEquals(View.GONE,options.visibility);assertEquals(View.GONE,quick.visibility);assertEquals(View.GONE,editorButton.visibility)
            assertTrue(layers.none { it is VectorTileLayer || it is LabelLayer })
            assertTrue(layers.indexOf(layers.filterIsInstance<VtmRasterLayer>().single())<layers.indexOf(routes))
            assertSame(routes,layers.filterIsInstance<VtmRouteLayer>().single())
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun brightBackgroundReachesGlAndIsRestoredAfterAnotherMapIsOpened() {
        val c=RuntimeEnvironment.getApplication();val view=BicycleMapView(c)
        val base=VtmBaseMap(c,view.map(),MapSource.VECTOR,"",false,view::queueEvent)
        fun rendererClear():FloatArray = org.oscim.renderer.MapRenderer::class.java.getDeclaredField("mClearColor")
            .apply { isAccessible=true }.get(null) as FloatArray
        val expected=floatArrayOf(248/255f,244/255f,240/255f,1f)
        try {
            assertArrayEquals("Bright background must reach actual GL clear color, not just parsed theme",expected,rendererClear(),.00001f)
            base.resume();base.pause()
            val other=BicycleMapView(c)
            try {
                assertArrayEquals("new VTM renderer resets its shared clear color",floatArrayOf(.8f,.8f,.8f,1f),rendererClear(),.00001f)
                base.resume()
                assertArrayEquals("returning dashboard restores reference background",expected,rendererClear(),.00001f)
            } finally { other.disposeMap() }
        } finally { base.onDetach();view.disposeMap() }
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
