package de.bcycle.app

import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
class PageFooterVisibilityTest {
    @Test fun profilesAndRideDetailsUseTheSameHeaderToggleAndReopenWithFooterHidden() {
        val library=GpxLibrary(org.robolectric.RuntimeEnvironment.getApplication().filesDir).apply { load();add("<gpx><trk><trkseg><trkpt lat='50' lon='8'/><trkpt lat='50.01' lon='8.01'/></trkseg></trk></gpx>".byteInputStream(),"Testfahrt",select=false) }
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get()
            fun check() {
                shadowOf(android.os.Looper.getMainLooper()).idle()
                val screen=ShadowAlertDialog.getLatestAlertDialog();val root=screen.window!!.decorView
                root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));root.layout(0,0,360,800)
                val footer=root.findViewWithTag<View>("bicycle_fullscreen_footer")
                val button=root.findViewWithTag<View>("route_footer_toggle")
                val info=root.findViewWithTag<View>("settings_help")
                assertNotNull(button);assertNotNull(info);assertTrue(button.right<=info.left)
                assertEquals(View.GONE,footer.visibility);assertFalse(button.isActivated)
                button.performClick();assertEquals(View.VISIBLE,footer.visibility);assertTrue(button.isActivated)
                button.performClick();assertEquals(View.GONE,footer.visibility)
                button.performClick();screen.dismiss()
            }
            repeat(2) {
                MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible=true }.invoke(a)
                check()
            }
            val state=RideRuntime.get(a).mapState
            val until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5)
            while((state.busy || state.entries.none { it.id==library.entries.single().id }) && System.nanoTime()<until) { shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(5) }
            val entry=state.entries.single { it.id==library.entries.single().id }
            repeat(2) {
                MainActivity::class.java.getDeclaredMethod("routeAnalysis",ManagedRoute::class.java).apply { isAccessible=true }.invoke(a,entry)
                check()
            }
        } finally { host.pause().stop().destroy() }
    }
}
