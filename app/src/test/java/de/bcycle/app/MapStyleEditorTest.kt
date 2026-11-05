package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.view.View
import android.widget.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.*
import org.oscim.layers.tile.vector.labeling.BCycleLabelLayer
import org.oscim.theme.styles.*
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapStyleEditorTest {
    private val c get()=RuntimeEnvironment.getApplication()
    @Test fun exportedEditsRoundTripThroughProfilesAndBackupAndCreateActualNativeStyles() {
        val view=BicycleMapView(c)
        val prefs=c.getSharedPreferences("bike",0)
        val custom=MapStyleOverrides().update(MapStyleTarget.PRIMARY,MapObjectStyle(Color.RED,MapDash.DASH_DOT))
            .update(MapStyleTarget.FOREST,MapObjectStyle(Color.BLUE))
            .update(MapStyleTarget.TRACK,MapObjectStyle(Color.MAGENTA,widthPercent=40))
        try {
            custom.write(prefs)
            assertEquals(custom,MapStyleOverrides.parse(JSONObject(custom.export(prefs))))
            val bank=BikeUiConfigurationStore(prefs);bank.ensure(listOf(0,1));bank.save(0)
            MapStyleOverrides().write(prefs);bank.save(1);bank.apply(0);assertEquals(custom,MapStyleOverrides.read(prefs))
            val backup=SettingsBackup(prefs,c.getSharedPreferences("route-export",0),ThemeManager(prefs))
            val bytes=backup.export(setOf(BackupSection.MAPS));MapStyleOverrides().write(prefs)
            backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.MAPS)));assertEquals(custom,MapStyleOverrides.read(prefs))
            val theme=VtmBrightTheme.load(false,style=custom)
            try {
                val road=TagSet().apply { add(Tag("layer","transportation"));add(Tag("class","primary")) }
                val lines=theme.matchElement(GeometryBuffer.GeometryType.LINE,road,16).orEmpty().filterIsInstance<LineStyle>()
                assertTrue(lines.any { it.current().color==Color.RED && it.dashArray?.size==4 && it.texture!=null })
                val forest=TagSet().apply { add(Tag("layer","landcover"));add(Tag("class","wood")) }
                assertTrue(theme.matchElement(GeometryBuffer.GeometryType.POLY,forest,16).orEmpty().filterIsInstance<AreaStyle>().any { it.current().color==Color.argb(26,0,0,255) })
            } finally { theme.dispose() }
            val edited=VtmBrightTheme.load(false,style=custom)
            val baseline=VtmBrightTheme.load(false)
            try {
                for(zoom in listOf(15,16,18)) {
                    fun lines(t:org.oscim.theme.IRenderTheme,kind:String)=t.matchElement(GeometryBuffer.GeometryType.LINE,
                        TagSet().apply { add(Tag("layer","transportation"));add(Tag("class",kind)) },zoom).orEmpty().filterIsInstance<LineStyle>()
                    val original=lines(baseline,"track").single { it.style=="bcycle-track" }
                    val changed=lines(edited,"track").single { it.style=="bcycle-track" }
                    assertEquals(Color.MAGENTA,changed.current().color);assertEquals(original.width*.4f,changed.width,.001f)
                    assertTrue(original.width<lines(baseline,"minor").last().width)
                    assertNotNull(original.texture)
                    assertTrue(lines(edited,"minor").none { it.style=="bcycle-track" || it.current().color==Color.MAGENTA })
                }
            } finally { edited.dispose();baseline.dispose() }
            val invalid=custom.json();invalid.getJSONObject("objects").getJSONObject("PRIMARY").put("color","url(https://invalid)")
            assertThrows(IllegalArgumentException::class.java) { MapStyleOverrides.parse(invalid) }
            for(bad in listOf(0,301,50.5,"100")) {
                val invalidWidth=custom.json();invalidWidth.getJSONObject("objects").getJSONObject("TRACK").put("widthPercent",bad)
                assertThrows(IllegalArgumentException::class.java) { MapStyleOverrides.parse(invalidWidth) }
            }
            val invalidDash=MapStyleOverrides().json().put("objects",JSONObject().put("FOREST",JSONObject().put("lineStyle","DOTTED")))
            assertThrows(IllegalArgumentException::class.java) { MapStyleOverrides.parse(invalidDash) }
        } finally { view.disposeMap() }
    }
    @Test fun contoursAreBetweenGroundAndRoadsWithOnlyOnePassWhenDisabled() {
        val view=BicycleMapView(c);val map=view.map()
        val options=MapVectorLayers(MapVectorLayers().visible+MapVectorLayer.CONTOURS)
        val base=VtmBaseMap(c,map,MapSource.VECTOR,"",true,view::queueEvent,options)
        try {
            val all=map.layers();val ground=all.filterIsInstance<VtmStyledVectorLayer>().single { it.pass==VtmMapPass.GROUND }
            val foreground=all.filterIsInstance<VtmStyledVectorLayer>().single { it.pass==VtmMapPass.FOREGROUND }
            val contour=all.filterIsInstance<VtmContourLayer>().single();val labels=all.filterIsInstance<BCycleLabelLayer>().single()
            assertTrue(all.indexOf(ground)<all.indexOf(contour));assertTrue(all.indexOf(contour)<all.indexOf(foreground));assertTrue(all.indexOf(foreground)<all.indexOf(labels))
            assertEquals(Color.TRANSPARENT,foreground.theme.mapBackground)
            fun tags(layer:String,kind:String)=TagSet().apply { add(Tag("layer",layer));add(Tag("class",kind)) }
            for((layer,kind) in listOf("landcover" to "grass","water" to "lake")) {
                assertFalse(ground.theme.matchElement(GeometryBuffer.GeometryType.POLY,tags(layer,kind),16).isNullOrEmpty())
                assertTrue(foreground.theme.matchElement(GeometryBuffer.GeometryType.POLY,tags(layer,kind),16).isNullOrEmpty())
            }
            for((layer,kind,type) in listOf(Triple("transportation","primary",GeometryBuffer.GeometryType.LINE),Triple("building","",GeometryBuffer.GeometryType.POLY))) {
                assertTrue(ground.theme.matchElement(type,tags(layer,kind),16).isNullOrEmpty())
                assertFalse(foreground.theme.matchElement(type,tags(layer,kind),16).isNullOrEmpty())
            }
            base.onDetach();val single=VtmBaseMap(c,map,MapSource.VECTOR,"",true,view::queueEvent)
            try { assertEquals(1,map.layers().filterIsInstance<VtmStyledVectorLayer>().size);assertTrue(map.layers().none { it is VtmContourLayer }) }
            finally { single.onDetach() }
        } finally { base.onDetach();view.disposeMap() }
    }
    @Test fun onMapEditorChangesPalettePatternAndExportsTheSameSavedProfileWithoutResetOnOpen() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val activity=controller.get();val prefs=activity.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            var exported:String?=null;var changes=0
            val editor=MapStyleEditorView(activity,prefs,themes,{ changes++ },{ exported=it })
            activity.setContentView(ScrollView(activity).apply { addView(editor) })
            val target=editor.findViewWithTag<Spinner>("map_style_target")
            target.setSelection(MapStyleTarget.PRIMARY.ordinal);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertTrue(MapStyleOverrides.read(prefs).objects.isEmpty())
            val channels=(0..2).map { editor.findViewWithTag<BicycleSettingSlider>("map_style_rgb_$it") }
            channels.forEach { assertEquals(255,it.max) }
            channels[0].progress=17;channels[1].progress=92;channels[2].progress=203
            assertEquals(Color.rgb(17,92,203),MapStyleOverrides.read(prefs).objects[MapStyleTarget.PRIMARY]!!.color)
            assertEquals("#115CCB",editor.findViewWithTag<TextView>("map_style_rgb_hex").text.toString())
            target.setSelection(MapStyleTarget.FOREST.ordinal);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals("Existing forest transparency is shown",26,editor.findViewWithTag<BicycleSettingSlider>("map_style_rgb_3").progress)
            target.setSelection(MapStyleTarget.PRIMARY.ordinal);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(listOf(17,92,203),channels.map { it.progress })
            val beforeDrag=changes
            channels[0].progress=18
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(24))
            channels[1].progress=93
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(24))
            assertTrue("Colour preview runs during continuing slider changes, not just after a pause",changes>beforeDrag)
            channels[0].progress=17;channels[1].progress=92
            val dash=editor.findViewWithTag<Spinner>("map_style_dash");dash.setSelection(MapDash.DOTTED.ordinal)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200))
            assertEquals(MapDash.DOTTED,MapStyleOverrides.read(prefs).objects[MapStyleTarget.PRIMARY]!!.dash);assertTrue(changes>0)
            val width=editor.findViewWithTag<BicycleSettingSlider>("map_style_width")
            width.progress=55
            assertEquals(55,MapStyleOverrides.read(prefs).objects[MapStyleTarget.PRIMARY]!!.widthPercent)
            val alpha=editor.findViewWithTag<BicycleSettingSlider>("map_style_rgb_3")
            assertEquals(255,alpha.progress);alpha.progress=128
            assertEquals(128,MapStyleOverrides.read(prefs).objects[MapStyleTarget.PRIMARY]!!.alpha)
            assertEquals("#80115CCB",editor.findViewWithTag<TextView>("map_style_rgb_hex").text.toString())
            assertSame(target.parent,editor.findViewWithTag<View>("map_style_swatch").parent)
            val buttons=editor.findViewWithTag<LinearLayout>("map_style_actions")
            assertEquals(4,buttons.childCount);assertEquals(LinearLayout.HORIZONTAL,buttons.orientation)
            editor.findViewWithTag<View>("map_style_export").performClick()
            assertEquals(MapStyleOverrides.read(prefs),MapStyleOverrides.parse(JSONObject(exported!!)))
            val reopened=MapStyleEditorView(activity,prefs,themes,{}, {})
            assertEquals(MapStyleTarget.PRIMARY.ordinal,reopened.findViewWithTag<Spinner>("map_style_target").selectedItemPosition)
            assertEquals(55,reopened.findViewWithTag<BicycleSettingSlider>("map_style_width").progress)
            assertEquals(128,reopened.findViewWithTag<BicycleSettingSlider>("map_style_rgb_3").progress)
            editor.findViewWithTag<View>("map_style_reset_object").performClick();assertTrue(MapStyleOverrides.read(prefs).objects.isEmpty())
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun jsonExportUsesTheNativeFilePickerAndWritesTheCapturedStyle() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val activity=controller.get()
            val json=MapStyleOverrides().update(MapStyleTarget.RIVER,MapObjectStyle(Color.CYAN,MapDash.DASHED)).json().toString(2)
            MainActivity::class.java.getDeclaredMethod("exportMapStyle",String::class.java).apply { isAccessible=true }.invoke(activity,json)
            val request=Shadows.shadowOf(activity).nextStartedActivityForResult
            assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT,request.intent.action);assertEquals("application/json",request.intent.type);assertEquals(20,request.requestCode)
            val state=android.os.Bundle();controller.saveInstanceState(state);assertEquals(json,state.getString("pendingMapStyle"))
            val file=java.io.File(activity.cacheDir,"map-style-export.json")
            MainActivity::class.java.getDeclaredMethod("onActivityResult",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,android.content.Intent::class.java)
                .apply { isAccessible=true }.invoke(activity,20,Activity.RESULT_OK,android.content.Intent().setData(android.net.Uri.fromFile(file)))
            repeat(100) { if(!file.isFile || file.length()!=json.toByteArray().size.toLong()) Thread.sleep(10) }
            assertEquals(json,file.readText())
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun flatStickyHeadersUseSecondaryAcrossEveryPreset() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val activity=controller.get();val themes=ThemeManager(activity.getSharedPreferences("bike",0));themes.setFlatDesign(true)
            for(preset in themes.presets) {
                themes.activate(preset.id)
                val header=BicycleSectionHeader(activity,themes,"Test");activity.setContentView(header)
                header.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(100,View.MeasureSpec.EXACTLY));header.layout(0,0,400,100)
                val image=Bitmap.createBitmap(400,100,Bitmap.Config.ARGB_8888);header.draw(Canvas(image));val secondary=themes.active.tokens.secondary
                assertEquals(secondary,image.getPixel(10,50));image.recycle()
            }
        } finally { controller.pause().stop().destroy() }
    }
}
