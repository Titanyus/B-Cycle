package de.bcycle.app

import android.app.Activity
import android.view.View
import android.widget.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.*
import org.oscim.theme.styles.TextStyle
import org.robolectric.*
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class MapPlaceTextEditorTest {
    @Test fun fourClassesKeepIndependentWeightsAndSizesAcrossEditorProfileBackupAndNativeAtlasPaints() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup().visible()
        val c=controller.get();val prefs=c.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
        val map=BicycleMapView(c)
        try {
            var changed=0
            val editor=MapStyleEditorView(c,prefs,themes,{ changed++ },{})
            c.setContentView(ScrollView(c).apply { addView(editor) })
            val frame=editor.findViewWithTag<BicycleFrameDropdown>("map_place_typography")
            assertFalse(frame.expanded);frame.setExpanded(true)
            val types=editor.findViewWithTag<Spinner>("map_place_class")
            val weights=editor.findViewWithTag<Spinner>("map_place_weight")
            val sizes=editor.findViewWithTag<BicycleSettingSlider>("map_place_size")
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(MapPlaceClass.entries.size,types.count);assertTrue(MapStyleOverrides.read(prefs).placeLabels.isEmpty())
            for(type in MapPlaceClass.entries.take(4)) {
                types.setSelection(type.ordinal);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                assertEquals(type.defaultWeight/100-1,weights.selectedItemPosition);assertEquals(type.defaultSize,sizes.progress)
                weights.setSelection(type.ordinal+1);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                sizes.progress=70+type.ordinal*10
            }
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200))
            assertTrue(changed>0)
            val saved=MapStyleOverrides.read(prefs)
            assertEquals(4,saved.placeLabels.size);assertFalse(saved.canUpdateColorsFrom(MapStyleOverrides()))
            assertEquals(saved,MapStyleOverrides.parse(JSONObject(saved.export(prefs))))
            val bank=BikeUiConfigurationStore(prefs);bank.ensure(listOf(0,1));bank.save(0)
            MapStyleOverrides().write(prefs);bank.save(1);bank.apply(0);assertEquals(saved,MapStyleOverrides.read(prefs))
            val backup=SettingsBackup(prefs,c.getSharedPreferences("route-export",0),themes)
            val bytes=backup.export(setOf(BackupSection.MAPS));MapStyleOverrides().write(prefs)
            backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.MAPS)));assertEquals(saved,MapStyleOverrides.read(prefs))
            val baseline=VtmBrightTheme.load(false);val custom=VtmBrightTheme.load(false,style=saved)
            try {
                for(type in MapPlaceClass.entries.take(4)) {
                    val tags=TagSet().apply { add(Tag("layer","place"));add(Tag("class",type.key));add(Tag("rank","1")) }
                    fun text(theme:org.oscim.theme.IRenderTheme)=theme.matchElement(GeometryBuffer.GeometryType.POINT,tags,17).orEmpty().filterIsInstance<TextStyle>().first()
                    val before=text(baseline);val after=text(custom);val value=saved.placeLabels.getValue(type)
                    assertEquals(before.fontSize*value.sizePercent/type.defaultSize,after.fontSize,.001f)
                    val field=after.paint.javaClass.getDeclaredField("mPaint").apply { isAccessible=true }
                    val paint=field.get(after.paint) as android.graphics.Paint
                    assertEquals(value.weight,paint.typeface.weight)
                    assertEquals(after.paint.fontHeight,after.fontHeight,.001f)
                    assertEquals(before.priority,after.priority)
                }
            } finally { baseline.dispose();custom.dispose() }
            val reopened=MapStyleEditorView(c,prefs,themes,{}, {})
            reopened.findViewWithTag<Spinner>("map_place_class").setSelection(MapPlaceClass.VILLAGE.ordinal)
            c.setContentView(reopened);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(90,reopened.findViewWithTag<BicycleSettingSlider>("map_place_size").progress)
            reopened.findViewWithTag<View>("map_style_reset_all").performClick()
            assertTrue(MapStyleOverrides.read(prefs).placeLabels.isEmpty())
            val invalid=saved.json();invalid.getJSONObject("placeLabels").getJSONObject("CITY").put("weight",950)
            assertThrows(IllegalArgumentException::class.java) { MapStyleOverrides.parse(invalid) }
        } finally { map.disposeMap();controller.pause().stop().destroy() }
    }
}
