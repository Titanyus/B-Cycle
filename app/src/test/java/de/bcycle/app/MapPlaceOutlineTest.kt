package de.bcycle.app

import android.graphics.Color
import android.os.Looper
import android.view.View
import android.widget.Spinner
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
class MapPlaceOutlineTest {
    @Test fun urbanAndNaturalLanduseCanBeHiddenIndependently() {
        fun tags(layer:String,kind:String)=TagSet().apply { add(Tag("layer",layer));add(Tag("class",kind)) }
        val withoutNature=MapVectorLayers(MapVectorLayers().visible-MapVectorLayer.NATURE)
        val withoutUrban=MapVectorLayers(MapVectorLayers().visible-MapVectorLayer.URBAN)
        for(kind in listOf("residential","commercial","industrial","school","hospital","military")) {
            assertTrue(withoutNature.accepts(tags("landuse",kind)));assertFalse(withoutUrban.accepts(tags("landuse",kind)))
        }
        for(pair in listOf("landcover" to "wood","landcover" to "grass","landuse" to "farmland","park" to "park")) {
            assertFalse(withoutNature.accepts(tags(pair.first,pair.second)));assertTrue(withoutUrban.accepts(tags(pair.first,pair.second)))
        }
    }
    @Test fun nativePlaceFillAndHaloAreIndependentAndSurviveSavedStyle() {
        val c=RuntimeEnvironment.getApplication();val view=BicycleMapView(c)
        val prefs=c.getSharedPreferences("bike",0)
        val style=MapStyleOverrides().update(MapStyleTarget.PLACE_TEXT,MapObjectStyle(Color.RED,outline=MapOutlineStyle(Color.BLUE,128,150)))
        style.write(prefs);prefs.edit().putBoolean(MapVectorLayer.URBAN.key,false).apply()
        val saved=MapStyleLibrary.save(prefs,"Labels",null);MapStyleOverrides().write(prefs);MapStyleLibrary.load(prefs,saved.id)
        assertEquals(style,MapStyleOverrides.read(prefs));assertFalse(prefs.getBoolean(MapVectorLayer.URBAN.key,true))
        assertFalse(style.canUpdateColorsFrom(MapStyleOverrides()))
        assertEquals(style,MapStyleOverrides.parse(JSONObject(style.export(prefs))))
        val theme=VtmBrightTheme.load(false,style=style)
        try {
            for(kind in listOf("city","town","village","hamlet")) {
                val tags=TagSet().apply { add(Tag("layer","place"));add(Tag("class",kind));add(Tag("rank","1")) }
                val text=theme.matchElement(GeometryBuffer.GeometryType.POINT,tags,17).orEmpty().filterIsInstance<TextStyle>().first()
                assertEquals(Color.RED,text.paint.color);assertEquals(Color.argb(128,0,0,255),text.stroke.color)
                assertEquals(3.6f*org.oscim.backend.CanvasAdapter.getScale(),text.stroke.strokeWidth,.001f)
            }
        } finally { theme.dispose();view.disposeMap() }
    }
    @Test fun editorShowsTwoColumnsAndUpdatesPlaceHaloWidth() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0)
            val editor=MapStyleEditorView(a,prefs,ThemeManager(prefs),{},{});a.setContentView(editor)
            val targets=MapStyleTarget.entries.filterNot { it==MapStyleTarget.CYCLE || it==MapStyleTarget.CYCLE_ROUTES }
            editor.findViewWithTag<Spinner>("map_style_target").setSelection(targets.indexOf(MapStyleTarget.PLACE_TEXT))
            editor.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.AT_MOST));editor.layout(0,0,320,editor.measuredHeight)
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE,editor.findViewWithTag<View>("map_style_outline_column").visibility)
            val width=editor.findViewWithTag<BicycleSettingSlider>("map_style_outline_width");width.progress=180
            assertEquals(180,MapStyleOverrides.read(prefs).objects.getValue(MapStyleTarget.PLACE_TEXT).outline!!.widthPercent)
        } finally { host.pause().stop().destroy() }
    }
}
