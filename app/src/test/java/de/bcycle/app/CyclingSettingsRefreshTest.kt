package de.bcycle.app

import android.os.Looper
import android.widget.SeekBar
import android.widget.Switch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CyclingSettingsRefreshTest {
    @Test fun cyclingWidthAndSwitchSaveImmediatelyButRebuildOnlyAfterReturningToDashboard() {
        val context=RuntimeEnvironment.getApplication()
        context.getSharedPreferences("bike",0).edit().putString("mapSource",MapSource.VECTOR.name).commit()
        context.getSharedPreferences(HrvSettings.STORE,0).edit().putBoolean("showOnFirstStart",false).commit()
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val prefs=RideRuntime.get(a).preferences
            val panel=MainActivity::class.java.getDeclaredField("mapPanel").apply { isAccessible=true }.get(a) as MapPanel
            MainActivity::class.java.getDeclaredMethod("showMenu",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,false)
            val parent=ShadowAlertDialog.getLatestAlertDialog()
            MainActivity::class.java.getDeclaredMethod("mapSettings").apply { isAccessible=true }.invoke(a)
            shadowOf(Looper.getMainLooper()).idle()
            val settings=ShadowAlertDialog.getLatestAlertDialog();val root=settings.window!!.decorView
            val initial=panel.mapView.map().layers().filterIsInstance<VtmStyledVectorLayer>().single()
            for(kind in listOf(MapLineKind.CYCLEWAYS,MapLineKind.CYCLE_ROUTES)) {
                root.findViewWithTag<BicycleFrameDropdown>("map_line_frame_${kind.id}").setExpanded(true)
                val slider=root.findViewWithTag<SeekBar>("map_line_width_${kind.id}")
                val listener=SeekBar::class.java.getDeclaredField("mOnSeekBarChangeListener").apply { isAccessible=true }.get(slider) as SeekBar.OnSeekBarChangeListener
                for(value in listOf(5,8,12)) {
                    slider.progress=value;listener.onProgressChanged(slider,value,true)
                    assertEquals((value+1)/2f,MapRouteStyles.read(prefs,kind).widthDp,0f)
                    assertSame(initial,panel.mapView.map().layers().filterIsInstance<VtmStyledVectorLayer>().single())
                }
            }
            root.findViewWithTag<Switch>(MapVectorLayer.CYCLE_ROUTES.key).performClick()
            root.findViewWithTag<Switch>(VtmBaseMap.cyclewaysKey).performClick()
            assertTrue(prefs.getBoolean(MapVectorLayer.CYCLE_ROUTES.key,false))
            assertFalse(prefs.getBoolean(VtmBaseMap.cyclewaysKey,true))
            assertTrue(panel.mapView.map().layers().none { it is VtmCyclingRoutesLayer })
            settings.dismiss();shadowOf(Looper.getMainLooper()).idle()
            assertTrue(parent.isShowing)
            assertSame("Still in settings",initial,panel.mapView.map().layers().filterIsInstance<VtmStyledVectorLayer>().single())
            parent.dismiss();shadowOf(Looper.getMainLooper()).idle()
            val applied=panel.mapView.map().layers().filterIsInstance<VtmStyledVectorLayer>().single()
            assertNotSame(initial,applied)
            assertTrue(panel.mapView.map().layers().any { it is VtmCyclingRoutesLayer })
            val style=MapPanel::class.java.getDeclaredField("mapStyle").apply { isAccessible=true }.get(panel)
            assertEquals(MapCyclingStyles.effective(prefs),style)
            a.onWindowFocusChanged(true)
            assertSame("Apply only once",applied,panel.mapView.map().layers().filterIsInstance<VtmStyledVectorLayer>().single())
        } finally { host.pause().stop().destroy() }
    }
}
