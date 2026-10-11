package de.bcycle.app

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowListPopupWindow

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteExportNavigationTest {
    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
    @Test fun footerDropdownOffersBothOriginsWithIconsWithoutOpeningAnotherDialog() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            descendants(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            val root = ShadowAlertDialog.getLatestAlertDialog()
            descendants(root.window!!.decorView).single { it.tag == "settings_ROUTES" }.performClick()
            val manager = ShadowAlertDialog.getLatestAlertDialog()
            val state = RideRuntime.get(activity).mapState
            val deadline = System.currentTimeMillis() + 5000
            while (state.busy && System.currentTimeMillis() < deadline) { Thread.sleep(10); shadowOf(Looper.getMainLooper()).idle() }
            val import = manager.getButton(AlertDialog.BUTTON_NEUTRAL)
            assertEquals("route_import", import.tag)
            assertNotNull(import.compoundDrawablesRelative[0])
            assertFalse(descendants(manager.window!!.decorView).any { it.tag == "route_recover" })
            import.performClick()
            var choice = ShadowListPopupWindow.getLatestListPopupWindow()
            assertSame(manager, ShadowAlertDialog.getLatestAlertDialog())
            assertTrue(choice.isShowing)
            for (position in 0..1) {
                val row = choice.listView!!.adapter.getView(position, null, choice.listView) as android.widget.TextView
                assertNotNull(row.compoundDrawablesRelative[0])
            }
            choice.dismiss(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            for ((position, code) in listOf(0 to 8, 1 to 9)) {
                import.performClick(); choice = ShadowListPopupWindow.getLatestListPopupWindow()
                val list = choice.listView!!
                list.performItemClick(list.adapter.getView(position, null, list), position, position.toLong())
                shadowOf(Looper.getMainLooper()).idle()
                assertFalse(choice.isShowing)
                val request = shadowOf(activity).nextStartedActivityForResult
                assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT, request.intent.action)
                assertEquals(code, request.requestCode)
                assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
                assertSame(manager, ShadowAlertDialog.getLatestAlertDialog())
            }
            import.performClick(); choice = ShadowListPopupWindow.getLatestListPopupWindow()
            manager.dismiss(); shadowOf(Looper.getMainLooper()).idle(); assertFalse(choice.isShowing)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun routeActionsUseAnchoredDropdownAndEditingReturnsToSameManager() {
        val context = RuntimeEnvironment.getApplication()
        GpxLibrary(context.filesDir).add("""<gpx version="1.1"><trk><trkseg><trkpt lat="50" lon="8"/><trkpt lat="50.01" lon="8.01"/></trkseg></trk></gpx>""".byteInputStream(), "Testrunde.gpx")
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get()
            descendants(activity.findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            val root = ShadowAlertDialog.getLatestAlertDialog()
            descendants(root.window!!.decorView).single { it.tag == "settings_ROUTES" }.performClick()
            val manager = ShadowAlertDialog.getLatestAlertDialog()
            val state = RideRuntime.get(activity).mapState
            val deadline = System.currentTimeMillis() + 5000
            while (state.busy && System.currentTimeMillis() < deadline) { Thread.sleep(10); shadowOf(Looper.getMainLooper()).idle() }
            val more = descendants(manager.window!!.decorView).single { it.tag == "route_more_${state.entries.single().id}" }
            more.performClick()
            var popup = ShadowListPopupWindow.getLatestListPopupWindow()
            assertSame(more, popup.anchorView)
            assertSame(manager, ShadowAlertDialog.getLatestAlertDialog())
            fun choose(title: String) {
                val list = popup.listView!!
                val position = (0 until list.adapter.count).single { list.adapter.getItem(it) == title }
                list.performItemClick(list.adapter.getView(position, null, list), position, position.toLong())
                shadowOf(Looper.getMainLooper()).idle()
                assertFalse(popup.isShowing)
            }
            choose("Umbenennen")
            val rename = ShadowAlertDialog.getLatestAlertDialog()
            assertEquals("Testrunde", (descendants(rename.window!!.decorView).single { it.tag == "route_rename" } as android.widget.EditText).text.toString())
            rename.getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            more.performClick(); popup = ShadowListPopupWindow.getLatestListPopupWindow()
            choose("Aus Sammlung löschen")
            ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, state.entries.size)
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun settingsCancelReturnsToSameManagerAndSaveAppliesIndependentOutputsWithoutClosingRootMenu() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        fun latest() = ShadowAlertDialog.getLatestAlertDialog()
        fun click(screen: AlertDialog, tag: String) = descendants(screen.window!!.decorView).single { it.tag == tag }.performClick()
        try {
            descendants(controller.get().findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            click(latest(), "settings_ROUTES")
            val manager = latest()
            click(manager, "route_settings")
            latest().getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            click(manager, "route_settings")
            val settings = latest()
            (descendants(settings.window!!.decorView).single { it.tag == "export_save" } as CheckBox).isChecked = false
            settings.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            val deadline = System.currentTimeMillis() + 5000
            while (settings.isShowing && System.currentTimeMillis() < deadline) { Thread.sleep(10); shadowOf(Looper.getMainLooper()).idle() }
            assertFalse(settings.isShowing)
            assertFalse(RouteExportSettings(RuntimeEnvironment.getApplication()).read().save)
            assertFalse(RouteExportSettings(RuntimeEnvironment.getApplication()).read().webdav)
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
}
