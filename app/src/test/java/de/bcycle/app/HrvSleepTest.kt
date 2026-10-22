package de.bcycle.app

import android.app.Activity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HrvSleepTest {
    @Test fun combinedSelectionUpgradesLegacySingleTimeAndRetainsAnswersWhenHidden() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0));val settings=HrvQuestionSettings(a)
            settings.write(HrvQuestionConfig(enabled=setOf(HrvQuestion.WAKE)))
            assertTrue(settings.read().enabled.containsAll(setOf(HrvQuestion.WAKE,HrvQuestion.BED)))
            assertFalse(HrvQuestion.SLEEP_DURATION in HrvQuestionConfig().enabled)
            val controls=HrvQuestionSettingsView(a,themes)
            val combined=controls.findViewWithTag<CheckBox>("hrvq_enabled_WAKE")
            assertTrue(combined.isChecked);assertEquals(a.getString(R.string.hrvq_sleep_times),combined.text.toString())
            assertNull(controls.findViewWithTag<View>("hrvq_enabled_BED"))
            combined.performClick();assertFalse(HrvQuestion.WAKE in settings.read().enabled);assertFalse(HrvQuestion.BED in settings.read().enabled)
            controls.findViewWithTag<CheckBox>("hrvq_enabled_SLEEP_DURATION").performClick()
            assertEquals(setOf(HrvQuestion.SLEEP_DURATION),settings.read().enabled)
            assertEquals(settings.read(),HrvQuestionConfig.decode(settings.read().json()))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun decimalSleepHoursValidateAndRoundTripWithoutDerivingThemFromTimes() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            HrvQuestionSettings(a).write(HrvQuestionConfig(enabled=setOf(HrvQuestion.SLEEP_DURATION)))
            var saved:HrvQuestionnaire?=null
            val view=HrvQuestionnaireView(a,themes,HrvQuestionnaire("2024-10-16",extras=mapOf("wake" to "07:00","bed" to "23:00")),null) { q,done -> saved=q;done(true) }
            a.setContentView(view)
            assertNull(view.findViewWithTag<View>("hrvq_sleep_times"))
            val hours=view.findViewWithTag<EditText>("hrvq_sleepHours")
            hours.setText("7,5");view.flush();assertEquals("7.5",saved!!.extras["sleepHours"])
            hours.setText("25");view.flush();assertEquals("7.5",saved!!.extras["sleepHours"]);assertNotNull(hours.error)
            assertEquals(saved,HrvQuestionnaire.decode(saved!!.json()))
            assertEquals("07:00",saved!!.extras["wake"])
        } finally { host.pause().stop().destroy() }
    }
    @Test fun csvSleepDurationUsesHoursAndKeepsTheOriginalValue() {
        val preview=Hrv4TrainingCsv.preview("date,time,rMSSD,sleep_time\n2024-16-10,07:00,40,7.25\n".byteInputStream(),ZoneOffset.UTC)
        val entry=preview.entries.single()
        assertEquals("7.25",entry.questionnaire.extras["sleepHours"])
        assertEquals("7.25",entry.result.getJSONObject("external").getJSONObject("originalColumns").getString("sleep_time"))
        assertThrows(IllegalArgumentException::class.java) { entry.questionnaire.copy(extras=mapOf("sleepHours" to "-1")).validate() }
    }
}
