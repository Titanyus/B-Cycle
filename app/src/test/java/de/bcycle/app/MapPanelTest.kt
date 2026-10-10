package de.bcycle.app

import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MapPanelTest {
    private fun descendants(v: View): List<View> = listOf(v) + if (v is ViewGroup)
        (0 until v.childCount).flatMap { descendants(v.getChildAt(it)) } else emptyList()
    private fun measure(view: View, width: Int, height: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
    }
    @Test fun controlsHideAfterFiveSecondsAndFullscreenReusesMap() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = controller.get().findViewById<ViewGroup>(android.R.id.content)
            measure(root, 1080, 2100)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            val full = descendants(panel).single { it.contentDescription == "Karte im Vollbild" }
            val controls = full.parent as View
            val toolbar = descendants(root).single { it.tag == "ride_toolbar" }
            val dashboard = descendants(root).filterIsInstance<DashboardView>().single()
            val dataHeight = dashboard.height
            val mapHeight = panel.height
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
            assertEquals(View.GONE, controls.visibility)
            assertEquals(View.GONE, toolbar.visibility)
            measure(root, 1080, 2100)
            assertEquals(dataHeight, dashboard.height)
            assertTrue(panel.height > mapHeight)
            panel.setFollow(false)
            val now = SystemClock.uptimeMillis()
            MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 100f, 100f, 0).also { panel.mapView.dispatchTouchEvent(it); it.recycle() }
            MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, 100f, 100f, 0).also { panel.mapView.dispatchTouchEvent(it); it.recycle() }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400))
            assertEquals(View.VISIBLE, controls.visibility)
            assertEquals(View.VISIBLE, toolbar.visibility)
            measure(root, 1080, 2100)
            assertEquals(dataHeight, dashboard.height)
            assertEquals(mapHeight, panel.height)
            assertFalse(controller.get().getSharedPreferences("bike", 0).getBoolean("mapFollow", true))
            full.performClick(); measure(root, 1080, 2100)
            assertEquals(View.GONE, descendants(root).filterIsInstance<DashboardView>().single().visibility)
            assertSame(panel, descendants(root).filterIsInstance<MapPanel>().single())
            full.performClick(); measure(root, 1080, 2100)
            assertEquals(View.VISIBLE, descendants(root).filterIsInstance<DashboardView>().single().visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun headingUpCameraKeepsGpsInLowerThirdAndNorthSettingResetsRotation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = controller.get().findViewById<ViewGroup>(android.R.id.content)
            measure(root, 1080, 2100)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            val runtime = RideRuntime.get(controller.get())
            val now = SystemClock.elapsedRealtime()
            runtime.ride.gps.accept(GpsFix(now, 51.0, 10.0, 3.0, 5.0, .2, bearingDegrees = 90.0, bearingAccuracyDegrees = 3.0), now)
            panel.setFollow(true); panel.refresh()
            assertEquals(-90f, panel.mapView.mapRotation.degrees, .001f)
            val pixel = panel.mapView.mapViewProjection.toPixels(org.mapsforge.core.model.LatLong(51.0, 10.0), true)
            assertEquals(panel.width / 2.0, pixel.x, 1.0)
            assertEquals(panel.height * .68, pixel.y, 1.0)
            descendants(panel).single { it.contentDescription == "Fahrtrichtung oben · wechseln zu Norden" }.performClick()
            assertEquals(0f, panel.mapView.mapRotation.degrees, .001f)
            assertEquals(.5f, panel.mapView.mapViewCenterY, .001f)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun fourPagesHaveOneResizableMapAndIndependentPersistedFields() {
        val app = RuntimeEnvironment.getApplication()
        val prefs = app.getSharedPreferences("bike", 0)
        prefs.edit().clear().putString("dashboard_1", DashboardLayout.defaults.replace(0, DashboardField("HEART_RATE")).encode()).commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val root = controller.get().findViewById<ViewGroup>(android.R.id.content)
            measure(root, 1080, 2100)
            val panel = descendants(root).filterIsInstance<MapPanel>().single()
            val smallHeight = panel.height
            val next = descendants(panel).single { it.contentDescription == "Nächstes Dashboard" }
            val expected = listOf(4, 2, 3, 7)
            expected.forEachIndexed { index, count ->
                next.performClick(); measure(root, 1080, 2100)
                val dashboard = descendants(root).filterIsInstance<DashboardView>().single()
                assertEquals(count, descendants(dashboard).count { it.isClickable })
                assertSame(panel, descendants(root).filterIsInstance<MapPanel>().single())
                assertTrue(panel.height > 0)
                if (index == 0) {
                    assertTrue(panel.height > smallHeight)
                    assertTrue(descendants(dashboard).first { it.isClickable }.contentDescription.toString().startsWith("Pulsmesser"))
                }
            }
            assertEquals(0, prefs.getInt("dashboardPage", -1))
            assertNull(prefs.getString("dashboard", null))
        } finally { controller.pause().stop().destroy() }
    }
}
