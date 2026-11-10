package de.bcycle.app

import android.graphics.Color
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.oscim.core.*
import org.oscim.theme.styles.TextStyle
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class MapControlAccessTest {
    private val c get()=RuntimeEnvironment.getApplication()
    @Test fun roadAndHouseTextHaveIndependentNativeFillHaloAndVisibility() {
        val view=BicycleMapView(c)
        val base=c.assets.open("styles/bright-vtm.xml").bufferedReader().use { it.readText() }
        assertEquals(Color.WHITE,MapStyleXml.defaultColor(base,MapStyleTarget.HOUSE_TEXT,true))
        assertEquals(Color.rgb(85,85,85),MapStyleXml.defaultColor(base,MapStyleTarget.HOUSE_TEXT))
        val style=MapStyleOverrides().update(MapStyleTarget.ROAD_TEXT,MapObjectStyle(Color.RED,outline=MapOutlineStyle(Color.GREEN,128,150)))
            .update(MapStyleTarget.HOUSE_TEXT,MapObjectStyle(Color.BLUE,outline=MapOutlineStyle(Color.YELLOW,200,200)))
        val options=MapVectorLayers(MapVectorLayers().visible+MapVectorLayer.HOUSE_NUMBERS)
        val theme=VtmBrightTheme.load(false,options,style)
        try {
            fun tags(layer:String)=TagSet().apply { add(Tag("layer",layer));add(Tag("class","primary"));add(Tag("name","Main Street"));add(Tag("housenumber","12a")) }
            val road=theme.matchElement(GeometryBuffer.GeometryType.LINE,tags("transportation_name"),18).orEmpty().filterIsInstance<TextStyle>().first()
            assertEquals(Color.RED,road.paint.color);assertEquals(Color.argb(128,0,255,0),road.stroke.color)
            val house=theme.matchElement(GeometryBuffer.GeometryType.POINT,tags("housenumber"),18).orEmpty().filterIsInstance<TextStyle>().single()
            assertEquals(Color.BLUE,house.paint.color);assertEquals(Color.argb(200,255,255,0),house.stroke.color)
            assertEquals(2.4f*org.oscim.backend.CanvasAdapter.getScale(),house.stroke.strokeWidth,.001f)
            assertFalse(MapVectorLayers().accepts(tags("housenumber")))
            assertTrue(options.accepts(tags("housenumber")))
            assertEquals(style,MapStyleOverrides.parse(style.json()))
        } finally { theme.dispose();view.disposeMap() }
    }
    @Test fun mapEditorsRequireTwoSecondHoldAndDisappearOnEnteringSettings() {
        c.getSharedPreferences("bike",0).edit().putString("mapSource","VECTOR").commit()
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible();val a=host.get()
        try {
            val panel=MainActivity::class.java.getDeclaredField("mapPanel").apply { isAccessible=true }.get(a) as MapPanel
            panel.revealControls()
            val source=panel.findViewWithTag<View>("map_source_button")
            val layers=panel.findViewWithTag<View>("map_layers_button");val editor=panel.findViewWithTag<View>("map_style_editor_button")
            assertEquals(View.VISIBLE,source.visibility);assertEquals(View.GONE,layers.visibility);assertEquals(View.GONE,editor.visibility)
            fun touch(action:Int) { val event=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),action,10f,10f,0);source.dispatchTouchEvent(event);event.recycle() }
            touch(MotionEvent.ACTION_DOWN);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));touch(MotionEvent.ACTION_CANCEL)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertEquals(View.GONE,editor.visibility)
            touch(MotionEvent.ACTION_DOWN);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1999));assertEquals(View.GONE,editor.visibility)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));assertEquals(View.VISIBLE,editor.visibility);assertEquals(View.VISIBLE,layers.visibility)
            touch(MotionEvent.ACTION_UP)
            MainActivity::class.java.getDeclaredMethod("showMenu",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,false)
            Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(View.GONE,editor.visibility);assertEquals(View.GONE,layers.visibility)
            MainActivity::class.java.getDeclaredMethod("mapSettings").apply { isAccessible=true }.invoke(a)
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertNull(ShadowAlertDialog.getLatestAlertDialog().window!!.decorView.findViewWithTag<View>("map_vector_layers"))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun sourcePopupSwitchesAndStyleDialogManagementUseRealVisibleControls() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        val a=host.get();val runtime=RideRuntime.get(a);val prefs=runtime.preferences
        try {
            val anchor=android.widget.Button(a);a.setContentView(anchor)
            val popup=MapSourcePopup(anchor,runtime,{}, {})
            a.setContentView(popup.contentView)
            val paths=popup.contentView.findViewWithTag<android.widget.Switch>("map_source_${VtmBaseMap.cyclewaysKey}")
            val routes=popup.contentView.findViewWithTag<android.widget.Switch>("map_source_${MapVectorLayer.CYCLE_ROUTES.key}")
            paths.performClick();routes.performClick()
            assertFalse(prefs.getBoolean(VtmBaseMap.cyclewaysKey,true));assertTrue(prefs.getBoolean(MapVectorLayer.CYCLE_ROUTES.key,false))
            val first=MapStyleLibrary.save(prefs,"A",null);val second=MapStyleLibrary.save(prefs,"B",null)
            MapStyleSaveDialog.show(a,prefs,runtime.themes) { true };Shadows.shadowOf(Looper.getMainLooper()).idle()
            var dialog=ShadowAlertDialog.getLatestAlertDialog()
            val move=dialog.window!!.decorView.findViewWithTag<View>("map_style_move_up");assertTrue(move.isShown);move.performClick()
            Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(listOf(second.id,first.id),MapStyleLibrary.read(prefs).map { it.id })
            dialog=ShadowAlertDialog.getLatestAlertDialog()
            val selected=dialog.window!!.decorView.findViewWithTag<android.widget.Spinner>("map_style_save_choice")
            assertEquals("B",selected.selectedItem.toString())
            val delete=dialog.window!!.decorView.findViewWithTag<View>("map_style_delete");assertTrue(delete.isShown);delete.performClick()
            Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(listOf(first.id),MapStyleLibrary.read(prefs).map { it.id })
            ShadowAlertDialog.getLatestAlertDialog().dismiss()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun orderingAndDeletionKeepWorkingCopyAndUseSameOrderInMapPicker() {
        val prefs=c.getSharedPreferences("bike",0);val style=MapStyleOverrides().update(MapStyleTarget.FOREST,MapObjectStyle(Color.BLUE));style.write(prefs)
        val a=MapStyleLibrary.save(prefs,"A",null);val b=MapStyleLibrary.save(prefs,"B",null);val bright=MapStyleLibrary.save(prefs,"Bright",null)
        MapStyleLibrary.move(prefs,bright.id,-1)
        assertEquals(listOf(a.id,bright.id,b.id),MapStyleLibrary.read(prefs).map { it.id })
        val picker=MapSourcePicker(c,ThemeManager(prefs),prefs);picker.select(MapSource.VECTOR)
        assertEquals(8,picker.selectedItemPosition);assertEquals("Bright",picker.adapter.getItem(8));assertEquals("A",picker.adapter.getItem(7))
        MapStyleLibrary.delete(prefs,bright.id);assertFalse(prefs.contains(MapStyleLibrary.ACTIVE));assertEquals(style,MapStyleOverrides.read(prefs))
        assertEquals(listOf(a.id,b.id),MapStyleLibrary.read(prefs).map { it.id })
    }
}
