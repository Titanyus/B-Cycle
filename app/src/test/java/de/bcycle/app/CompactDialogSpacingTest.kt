package de.bcycle.app

import android.app.Activity
import android.app.AlertDialog
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class CompactDialogSpacingTest {
    @Test fun confirmationsUseSharedHeaderGapAndKeepTheirAction() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get()
            val themes=ThemeManager(activity.getSharedPreferences("bike",0))
            for(title in listOf(R.string.route_delete_title,R.string.ui_reset_title)) {
                var confirmed=false
                val dialog=BicycleDialogBuilder(activity,themes,DialogNavigation(),fullscreen=false)
                    .setTitle(title).setMessage("Beschreibung")
                    .setPositiveButton(R.string.route_action_delete) { _,_->confirmed=true }.show()
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                val message=dialog.findViewById<TextView>(android.R.id.message)
                assertEquals("Beschreibung",message.text.toString())
                assertTrue(message.paddingTop>=BicycleDesign.dp(activity,BicycleDesign.controlGap))
                BicycleDialogStyle.messageSpacing(dialog)
                assertEquals(BicycleDesign.dp(activity,BicycleDesign.controlGap),message.paddingTop)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                assertTrue(confirmed)
                dialog.dismiss()
            }
        } finally { controller.pause().stop().destroy() }
    }
}
