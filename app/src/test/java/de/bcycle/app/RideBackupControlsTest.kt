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
            var available=false;val actions=mutableListOf<RideBackupDestination>()
            val controls=BackupDestinationView(c,themes,{available}) { actions+=it }
            val screen=DataSettingsView(c,themes) {}
            screen.install(DataAction.RIDE_BACKUP,controls)
            c.setContentView(screen)
            screen.findViewWithTag<View>("data_RIDE_BACKUP").performClick()
            assertTrue(actions.isEmpty())
            assertEquals(View.VISIBLE,screen.findViewWithTag<View>("data_details_RIDE_BACKUP").visibility)
            val cloud=controls.findViewWithTag<View>("backup_target_webdav")
            assertFalse(cloud.isEnabled)
            controls.findViewWithTag<View>("backup_target_local").performClick()
            assertEquals(listOf(RideBackupDestination.LOCAL),actions)
            available=true;controls.refresh();cloud.performClick()
            assertEquals(RideBackupDestination.WEBDAV,actions.last())
        } finally { host.pause().stop().destroy() }
    }
}
