package de.bcycle.app

import android.app.Activity
import android.view.View
import android.widget.RadioButton
import android.widget.Switch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RideBackupControlsTest {
    @Test fun expandingNeverEnablesOrTransfersAndUnavailableWebdavCannotBeSelected() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            var available=false;val actions=mutableListOf<Pair<RideBackupDestination,RideBackupAction>>()
            val controls=RideBackupControls(c,themes,{available},{true}) { destination,action -> actions+=destination to action }
            val screen=DataSettingsView(c,themes) {}
            screen.install(DataAction.RIDE_BACKUP,controls,controls.findViewWithTag("ride_backup_enabled"))
            c.setContentView(screen)
            val toggle=screen.findViewWithTag<Switch>("ride_backup_enabled")
            screen.findViewWithTag<View>("data_expand_RIDE_BACKUP").performClick()
            assertFalse(toggle.isChecked);assertTrue(actions.isEmpty())
            assertEquals(View.VISIBLE,screen.findViewWithTag<View>("data_details_RIDE_BACKUP").visibility)
            val cloud=screen.findViewWithTag<RadioButton>("ride_backup_webdav")
            assertFalse(cloud.isEnabled)
            assertFalse(screen.findViewWithTag<View>("ride_backup_action_export").isEnabled)
            toggle.performClick();assertTrue(actions.isEmpty())
            screen.findViewWithTag<View>("ride_backup_action_export").performClick()
            assertEquals(listOf(RideBackupDestination.LOCAL to RideBackupAction.EXPORT),actions)
            available=true;controls.refreshConnection();cloud.performClick()
            screen.findViewWithTag<View>("ride_backup_action_import").performClick()
            assertEquals(RideBackupDestination.WEBDAV to RideBackupAction.IMPORT,actions.last())
            controls.working(true);assertFalse(toggle.isEnabled);assertFalse(cloud.isEnabled)
            assertFalse(screen.findViewWithTag<View>("ride_backup_action_import").isEnabled)
        } finally { host.pause().stop().destroy() }
    }
}
