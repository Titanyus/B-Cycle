package de.bcycle.app

import android.app.AlertDialog
import android.os.Looper
import org.robolectric.Shadows.shadowOf
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PerformanceSettingsTest {
    private fun resolved(c:RecordingConfig)=c.copy(sourcePreferences=c.sourcePreferences ?: RecordingSources.defaults(c,c.indoor))
    private fun views(v: View): List<View> = listOf(v) + if (v is ViewGroup)
        (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    private fun open(activity: MainActivity): AlertDialog {
        MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible = true }.invoke(activity)
        return ShadowAlertDialog.getLatestAlertDialog().also {
            shadowOf(Looper.getMainLooper()).idle()
        }
    }
    @Test fun existingSelectionSurvivesAndNewIndependentSwitchesPersistInTheSamePreferences() {
        val app = RuntimeEnvironment.getApplication()
        val prefs = app.getSharedPreferences("bike", 0)
        val runtime = RideRuntime.get(app)
        runtime.saveConfig(RecordingConfig(setOf("RIDER_POWER", "CADENCE")))
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val dialog = open(controller.get())
        val checkboxes = views(dialog.window!!.decorView).filterIsInstance<Switch>().associateBy { it.tag }
        for (field in RecordField.summaries) assertFalse(checkboxes.getValue("record_${field.id}").isChecked)
        assertTrue(checkboxes.getValue("record_RIDER_POWER").isChecked)
        checkboxes.getValue("record_EFFICIENCY_FACTOR").isChecked = true
        assertEquals(app.getString(R.string.analysis_ftp_export), checkboxes.getValue("record_ESTIMATED_FTP").text.toString())
        checkboxes.getValue("record_ESTIMATED_FTP").isChecked = true
        assertFalse(checkboxes.getValue("record_NORMALIZED_POWER").isChecked)
        assertFalse(checkboxes.getValue("record_HEART_RATE").isChecked)
        val before = prefs.all.filterKeys { !it.startsWith("record") && it != BikeProfileStore.KEY }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(setOf("RIDER_POWER", "CADENCE", "EFFICIENCY_FACTOR", "ESTIMATED_FTP", "GPS_POSITION"), runtime.config.fields)
        assertEquals(before, prefs.all.filterKeys { !it.startsWith("record") && it != BikeProfileStore.KEY })
        controller.pause().stop().destroy()
        val recreated = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val restored = views(open(recreated.get()).window!!.decorView).filterIsInstance<Switch>().associateBy { it.tag }
            assertTrue(restored.getValue("record_EFFICIENCY_FACTOR").isChecked)
            assertTrue(restored.getValue("record_ESTIMATED_FTP").isChecked)
            assertFalse(restored.getValue("record_NORMALIZED_POWER").isChecked)
            assertEquals(runtime.config, RecordingConfig.decode(prefs.getStringSet("recordFields", null), prefs.getString("recordSpeed", null), prefs.getString("recordDistance", null),prefs.getStringSet("recordMetadata",null),prefs.getString(RecordingSources.KEY,null)))
        } finally { recreated.pause().stop().destroy() }
    }
    @Test fun defaultsDoNotEnableSummariesAndGpsDependencyDoesNotChangeExportSelection() {
        assertTrue(RecordField.summaries.none { it.id in RecordingConfig().fields })
        val selection = setOf("SPEED_POWER_RATIO")
        val config = RecordingConfig.decode(selection, "GPS_SPEED", "BIKE_DISTANCE")
        assertEquals(selection, config.fields); assertTrue(config.needsGps)
        assertFalse("GPS_SPEED" in config.fields); assertFalse("RIDER_POWER" in config.fields)
        assertEquals(setOf("CADENCE"), RecordingConfig.decode(setOf("CADENCE", "unknown-future-field"), null, null).fields)
    }
    @Test fun inlineFitSettingsKeepSelectionAcrossBikeSwitchesAndUseActiveThemeWithoutAnotherDialog() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val activity = controller.get(); val runtime = RideRuntime.get(activity)
            runtime.bikes.add()
            val before = runtime.config
            assertTrue(runtime.themes.activate("preset:solar-flare"))
            MainActivity::class.java.getDeclaredMethod("showSensorManager").apply { isAccessible = true }.invoke(activity)
            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            val content = views(dialog.window!!.decorView).single { it.tag == "bike_track_settings" } as ViewGroup
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.VISIBLE, content.visibility)
            val source=RecordField.options.single { it.id=="NORMALIZED_POWER" }.source
            val sourceContent=content.findViewWithTag<View>("fit_source_content_$source")
            assertEquals(View.GONE,sourceContent.visibility)
            content.findViewWithTag<View>("fit_source_$source").performClick()
            assertEquals(View.VISIBLE,sourceContent.visibility)
            val checkbox = views(content).filterIsInstance<Switch>().single { it.tag == "record_NORMALIZED_POWER" }
            assertEquals(runtime.themes.active.tokens.foreground, checkbox.currentTextColor)
            checkbox.isChecked = true
            dialog.window!!.decorView.findViewWithTag<View>("bike_profile_1").performClick()
            dialog.window!!.decorView.findViewWithTag<View>("bike_profile_0").performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertSame(dialog, ShadowAlertDialog.getLatestAlertDialog())
            assertEquals(View.VISIBLE,dialog.window!!.decorView.findViewWithTag<View>("fit_source_content_$source").visibility)
            assertTrue(dialog.window!!.decorView.findViewWithTag<Switch>("record_NORMALIZED_POWER").isChecked)
            assertEquals(resolved(before.copy(fields=before.fields+"NORMALIZED_POWER")),resolved(runtime.config))
            testBack(dialog)
            assertEquals(resolved(before.copy(fields=before.fields+"NORMALIZED_POWER")),resolved(runtime.config))
        } finally { controller.pause().stop().destroy() }
    }

}
