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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteExportNavigationTest {
    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
    @Test fun footerImportOffersBothOriginsWithIconsAndKeepsManagerOnCancel() {
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
            var choice = ShadowAlertDialog.getLatestAlertDialog()
            for (tag in listOf("route_import_planned", "route_import_recorded")) {
                val button = descendants(choice.window!!.decorView).single { it.tag == tag } as android.widget.Button
                assertNotNull(button.compoundDrawablesRelative[0])
            }
            choice.getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            for ((tag, code) in listOf("route_import_planned" to 8, "route_import_recorded" to 9)) {
                import.performClick(); choice = ShadowAlertDialog.getLatestAlertDialog()
                descendants(choice.window!!.decorView).single { it.tag == tag }.performClick()
                shadowOf(Looper.getMainLooper()).idle()
                val request = shadowOf(activity).nextStartedActivityForResult
                assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT, request.intent.action)
                assertEquals(code, request.requestCode)
                assertEquals(View.VISIBLE, manager.window!!.decorView.visibility)
            }
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
