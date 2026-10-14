package de.bcycle.app

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

/** The actual Compose-enabled MainActivity and reparented fullscreen dialog, not a preview-only scene. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComposeDialogIntegrationTest {
    @get:Rule val ui=createEmptyComposeRule()
    private fun latest():AlertDialog { shadowOf(Looper.getMainLooper()).idle();ui.waitForIdle();return ShadowAlertDialog.getLatestAlertDialog() }
    private fun menu(activity:MainActivity):AlertDialog {
        testDescendants(activity.findViewById(android.R.id.content)).single { it.contentDescription=="Menü öffnen" }.performClick()
        return latest()
    }
    @Test fun fullscreenHostRetainsCompositionAndBackRestoresSameParentAndScroll() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val parent=menu(activity)
            ui.onNodeWithTag("settings_PROFILE").performClick();val profile=latest()
            assertTrue(testDescendants(profile.window!!.decorView).any { it is ComposeDialogContent })
            assertFalse(testDescendants(profile.window!!.decorView).any { it is RiderProfileView })
            ui.onNodeWithTag("rider_height").performScrollTo().performTextInput("182")
            assertEquals(182,RideRuntime.get(activity).riderProfiles.read().heightCm)
            testBack(profile);ui.waitForIdle();assertEquals(View.VISIBLE,parent.window!!.decorView.visibility)
            ui.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_ADVANCED"))
            ui.onNodeWithTag("settings_ADVANCED").performClick();val advanced=latest()
            ui.onNodeWithTag("settings_FONT_LICENSE").performClick();val license=latest()
            testBack(license);ui.waitForIdle();assertEquals(View.VISIBLE,advanced.window!!.decorView.visibility)
            testBack(advanced);ui.waitForIdle();assertEquals(View.VISIBLE,parent.window!!.decorView.visibility)
        } finally { controller.pause().stop().destroy();shadowOf(Looper.getMainLooper()).idle() }
    }
    @Test fun fieldEditorChangesPersistForOnlyTheSelectedTileAndSourceDropdownKeepsGraphMode() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();activity.setDashboardEditing(true)
            testDescendants(activity.findViewById(android.R.id.content)).single { it.tag=="dashboard_tile_2" }.performClick();latest()
            ui.onNodeWithTag("field_list").performScrollToNode(hasTestTag("graph_SPEED"));ui.onNodeWithTag("graph_SPEED").performClick()
            ui.onNodeWithTag("field_list").performScrollToNode(hasTestTag("graph_display_mode"));ui.onNodeWithTag("graph_display_mode").performClick();ui.onNodeWithTag("graph_display_mode:1").performClick()
            val prefs=RideRuntime.get(activity).preferences;val config=GraphOverlayConfig.read(prefs,0)
            assertEquals(listOf(AnalysisChart.SPEED),config.slots[2]);assertEquals(GraphDisplayMode.GRAPH_ONLY,config.displayModes[2])
            assertEquals(GraphDisplayMode.WITH_DATA,config.displayModes[0])
            ui.onNodeWithTag("field_source").performClick();ui.onNodeWithTag("field_source:0").performClick()
            ui.onNodeWithTag("field_list").performScrollToNode(hasTestTag("GPS_SPEED"));ui.onNodeWithTag("GPS_SPEED").performClick();latest()
            assertEquals("GPS_SPEED",DashboardLayout.decode(prefs.getString("dashboard",null)).fields[2].id)
            assertEquals(GraphDisplayMode.GRAPH_ONLY,GraphOverlayConfig.read(prefs,0).displayModes[2])
        } finally { controller.pause().stop().destroy();shadowOf(Looper.getMainLooper()).idle() }
    }
    @Test fun recordingFieldsAndLocalSettingsPersistThroughTheActualServices() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity=controller.get();val parent=menu(activity);val runtime=RideRuntime.get(activity)
            ui.onNodeWithTag("settings_SENSORS").performClick();val sensors=latest()
            ui.onNodeWithTag("sensor_recording_toggle").performScrollTo().performClick()
            ui.onNodeWithTag("sensor_list").performScrollToNode(hasTestTag("record_ESTIMATED_FTP"));ui.onNodeWithTag("record_ESTIMATED_FTP").performClick()
            assertTrue("ESTIMATED_FTP" in runtime.config.fields)
            ui.onNodeWithTag("sensor_list").performScrollToNode(hasTestTag("record_speed"));ui.onNodeWithTag("record_speed").performClick();ui.onNodeWithTag("record_speed:1").performClick()
            assertEquals("GPS_SPEED",runtime.config.speedSource);assertTrue("ESTIMATED_FTP" in runtime.config.fields)
            testBack(sensors);ui.waitForIdle()
            ui.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_DATA"));ui.onNodeWithTag("settings_DATA").performClick();val data=latest()
            ui.onNodeWithTag("data_LOCAL").performClick();val local=latest()
            ui.onNodeWithTag("export_save").performClick()
            val deadline=System.nanoTime()+5_000_000_000L
            while(RouteExportSettings(activity).read().save && System.nanoTime()<deadline) { Thread.sleep(10);shadowOf(Looper.getMainLooper()).idle() }
            assertFalse(RouteExportSettings(activity).read().save)
            testBack(local);ui.waitForIdle();assertEquals(View.VISIBLE,data.window!!.decorView.visibility)
            ui.onNodeWithTag("data_BACKUP").performClick();val backup=latest()
            ui.onNodeWithTag("backup_none").performClick();assertFalse(backup.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled)
            ui.onNodeWithTag("backup_all").performClick();assertTrue(backup.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled)
            testBack(backup);ui.waitForIdle();testBack(data);ui.waitForIdle();testBack(parent);ui.waitForIdle()
        } finally { controller.pause().stop().destroy();shadowOf(Looper.getMainLooper()).idle() }
    }
    @Test fun dismissReleasesTheDialogLifecycleAndCompositionSubscriptions() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val screen=menu(controller.get())
            val host=testDescendants(screen.window!!.decorView).filterIsInstance<ComposeDialogContent>().single()
            assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED,host.lifecycle.currentState)
            testBack(screen);ui.waitForIdle();shadowOf(Looper.getMainLooper()).idle()
            assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED,host.lifecycle.currentState)
        } finally { controller.pause().stop().destroy();shadowOf(Looper.getMainLooper()).idle() }
    }

}
