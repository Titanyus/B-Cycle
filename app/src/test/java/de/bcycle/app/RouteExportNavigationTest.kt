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
