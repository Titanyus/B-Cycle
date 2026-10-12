package de.bcycle.app

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GpxManagerTest {
    private fun descendants(v: View): List<View> = listOf(v) + if (v is ViewGroup)
        (0 until v.childCount).flatMap { descendants(v.getChildAt(it)) } else emptyList()
    private fun await(state: MapState) {
        shadowOf(Looper.getMainLooper()).idle()
        val deadline = System.nanoTime() + 10_000_000_000L
        while (state.busy && System.nanoTime() < deadline) { Thread.sleep(10); shadowOf(Looper.getMainLooper()).idle() }
        assertFalse("GPX worker did not complete", state.busy)
    }
    @Test fun libraryEyeTogglesDashboardTrackAndDetailFooterDeletionUpdatesStoredRoutes() {
        val app = RuntimeEnvironment.getApplication()
        val library = GpxLibrary(app.filesDir)
        library.load()
        library.add("<gpx><rte><rtept lat='50' lon='8'/><rtept lat='50.01' lon='8.01'/></rte></gpx>".byteInputStream(), "Testfahrt.gpx")
        val id = library.entries.single().id
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val state = RideRuntime.get(activity).mapState; await(state)
            descendants(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            val menu = ShadowAlertDialog.getLatestAlertDialog()
            val items = descendants(menu.window!!.decorView)
            assertFalse(items.filterIsInstance<TextView>().any { it.text.toString() == "GPX-Strecke importieren" })
            items.single { it.tag == "settings_ROUTES" }.performClick()
            val manager = ShadowAlertDialog.getLatestAlertDialog()
            fun views(): List<View> {
                val decor = manager.window!!.decorView
                decor.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1800, View.MeasureSpec.EXACTLY))
                decor.layout(0, 0, 1080, 1800)
                return descendants(decor)
            }
            await(state)
            assertTrue(views().any { it.tag == "route_import" })
            assertTrue(views().filterIsInstance<TextView>().any { it.text.toString() == "Testfahrt" })
            activity.getSharedPreferences("bike", 0).edit().putBoolean("mapGpxVisible", false).apply()
            RideRuntime.get(activity).observers.toList().forEach { it() }
            assertEquals(activity.getString(R.string.route_list_show_named, "Testfahrt"), views().single { it.tag == "route_eye_$id" }.contentDescription)
            views().single { it.tag == "route_eye_$id" }.performClick(); await(state)
            assertSame(manager, ShadowAlertDialog.getLatestAlertDialog())
            assertNotNull(state.route); assertEquals(id, state.selectedRoute)
            assertTrue(activity.getSharedPreferences("bike", 0).getBoolean("mapGpxVisible", false))
            assertEquals(activity.getString(R.string.route_list_hide_named, "Testfahrt"), views().single { it.tag == "route_eye_$id" }.contentDescription)
            views().single { it.tag == "route_eye_$id" }.performClick(); await(state)
            assertSame(manager, ShadowAlertDialog.getLatestAlertDialog())
            assertFalse(activity.getSharedPreferences("bike", 0).getBoolean("mapGpxVisible", true))
            assertNotNull(state.route); assertEquals(id, state.selectedRoute)
            views().single { it.tag == "route_eye_$id" }.performClick(); await(state)
            assertTrue(activity.getSharedPreferences("bike", 0).getBoolean("mapGpxVisible", false))
            val decor = manager.window!!.decorView
            val bitmap = android.graphics.Bitmap.createBitmap(decor.width, decor.height, android.graphics.Bitmap.Config.ARGB_8888)
            decor.draw(android.graphics.Canvas(bitmap))
            java.io.File("build/reports/settings/route-manager-dialog.png").apply { parentFile!!.mkdirs()
                outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
            assertNotNull(GpxLibrary(app.filesDir).load()); assertEquals(1, state.routes.size)
            assertFalse(views().any { it.tag == "route_more_$id" })
            views().single { it.tag == "route_entry_$id" }.performClick(); shadowOf(Looper.getMainLooper()).idle()
            val details = ShadowAlertDialog.getLatestAlertDialog()
            details.window!!.decorView.findViewWithTag<View>("detail_delete").performClick()
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, details.window!!.decorView.visibility)
            assertEquals(1, state.routes.size)
            details.window!!.decorView.findViewWithTag<View>("detail_delete").performClick()
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick(); await(state)
            assertTrue(state.routes.isEmpty()); assertNull(state.route)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            val restored = GpxLibrary(app.filesDir); assertNull(restored.load()); assertTrue(restored.entries.isEmpty())
        } finally { controller.pause().stop().destroy() }
    }
}
