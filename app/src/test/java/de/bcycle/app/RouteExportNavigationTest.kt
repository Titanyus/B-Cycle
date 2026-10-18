package de.bcycle.app

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
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
            val close = manager.getButton(AlertDialog.BUTTON_POSITIVE)
            assertTrue(close == null || !close.isShown)
            assertEquals(R.drawable.ic_lucide_log_out, BicycleIcon.Close.resource)
            assertEquals(R.drawable.ic_lucide_x, BicycleIcon.Cancel.resource)
            val import = manager.window!!.decorView.findViewWithTag<BicycleFooterAction>("route_import")
            assertEquals("route_import", import.tag)
            assertNotNull(import.image.drawable)
            assertEquals("Strecke importieren", (import.getChildAt(1) as android.widget.TextView).text.toString())
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
    @Test fun routeActionsUseDetailFooterAndEditingReturnsToSameDetailsAndManager() {
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
            val id = state.entries.single().id
            val list = manager.window!!.decorView.findViewWithTag<android.widget.ListView>("route_list")
            val card = list.adapter.getView(0, null, list)
            assertNull(card.findViewWithTag<View>("route_more_$id"))
            card.performClick(); shadowOf(Looper.getMainLooper()).idle()
            val details = ShadowAlertDialog.getLatestAlertDialog()
            details.window!!.decorView.findViewWithTag<View>("detail_upload").performClick()
            val upload=ShadowListPopupWindow.getLatestListPopupWindow()
            descendants(upload.listView!!.parent as View).single { it.tag=="route_transfer_configure" }.performClick()
            shadowOf(Looper.getMainLooper()).idle()
            val data=ShadowAlertDialog.getLatestAlertDialog();val dataRoot=data.window!!.decorView
            dataRoot.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));dataRoot.layout(0,0,360,800)
            shadowOf(Looper.getMainLooper()).idle()
            assertNotNull(dataRoot.findViewWithTag<View>("data_settings"))
            assertNotNull(dataRoot.findViewWithTag<View>("data_routes_heading"))
            testBack(data);shadowOf(Looper.getMainLooper()).idle()
            assertTrue(details.isShowing)
            details.window!!.decorView.findViewWithTag<View>("detail_rename").performClick()
            val rename = ShadowAlertDialog.getLatestAlertDialog()
            shadowOf(Looper.getMainLooper()).idle()
            val nameInput=descendants(rename.window!!.decorView).single { it.tag=="route_rename" } as android.widget.EditText
            assertEquals("Testrunde",nameInput.text.toString())
            assertEquals(0,rename.window!!.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            assertTrue(nameInput.requestFocus());assertTrue(nameInput.showSoftInputOnFocus)
            assertNotNull(nameInput.onCreateInputConnection(android.view.inputmethod.EditorInfo()))
            testBack(rename); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, details.window!!.decorView.visibility)
            details.window!!.decorView.findViewWithTag<View>("detail_delete").performClick()
            testBack(ShadowAlertDialog.getLatestAlertDialog())
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, state.entries.size)
            assertEquals(View.VISIBLE, details.window!!.decorView.visibility)
            details.onBackPressed(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun localSettingsCancelReturnsToSameDataScreenAndSaveKeepsCloudConfiguration() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        fun latest() = ShadowAlertDialog.getLatestAlertDialog()
        fun click(screen: AlertDialog, tag: String) = descendants(screen.window!!.decorView).single { it.tag == tag }.performClick()
        try {
            descendants(controller.get().findViewById(android.R.id.content)).single { it.contentDescription == "Menü öffnen" }.performClick()
            click(latest(), "settings_DATA")
            val manager = latest()
            click(manager, "data_LOCAL")
            click(manager,"data_LOCAL"); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            click(manager, "data_LOCAL")
            val settings = latest()
            (descendants(settings.window!!.decorView).single { it.tag == "export_save" } as Switch).isChecked = false
            assertSame(manager,settings)
            click(manager,"data_LOCAL")
            val deadline = System.currentTimeMillis() + 5000
            while (RouteExportSettings(controller.get()).read().save && System.currentTimeMillis() < deadline) { Thread.sleep(10); shadowOf(Looper.getMainLooper()).idle() }
            assertTrue(settings.isShowing)
            assertFalse(RouteExportSettings(RuntimeEnvironment.getApplication()).read().save)
            assertFalse(RouteExportSettings(RuntimeEnvironment.getApplication()).read().webdav)
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy() }
    }
}
