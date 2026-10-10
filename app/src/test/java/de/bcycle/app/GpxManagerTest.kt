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
@Config(sdk = [33])
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
    @Test fun menuMovesImportIntoLibraryAndEyeAndTrashUpdateStoredRoutes() {
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
            fun views() = descendants(manager.window!!.decorView)
            assertTrue(views().filterIsInstance<TextView>().any { it.text.toString() == "GPX-Strecke importieren" })
            assertTrue(views().filterIsInstance<TextView>().any { it.text.toString().startsWith("Testfahrt\n") })
            views().single { it.tag == "route_eye_$id" }.performClick(); await(state)
            assertNull(state.route); assertNull(GpxLibrary(app.filesDir).load()); assertEquals(1, state.routes.size)
            views().single { it.tag == "route_eye_$id" }.performClick(); await(state)
            assertNotNull(state.route)
            views().single { it.tag == "route_delete_$id" }.performClick()
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            assertEquals(1, state.routes.size)
            views().single { it.tag == "route_delete_$id" }.performClick()
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick(); await(state)
            assertTrue(state.routes.isEmpty()); assertNull(state.route)
            val restored = GpxLibrary(app.filesDir); assertNull(restored.load()); assertTrue(restored.entries.isEmpty())
        } finally { controller.pause().stop().destroy() }
    }
}
