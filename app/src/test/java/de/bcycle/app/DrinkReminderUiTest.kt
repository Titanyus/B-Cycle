package de.bcycle.app

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class DrinkReminderUiTest {
    @Test fun switchAndAccordionAreIndependentAndIntervalUsesFiveMinuteSteps() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("drink-test",0);val themes=ThemeManager(prefs)
            val view=DrinkReminderSettingsView(a,themes,prefs);a.setContentView(view)
            val toggle=view.findViewWithTag<Switch>("drink_reminder_enabled")
            val details=view.findViewWithTag<View>("drink_reminder_details")
            assertFalse(toggle.isChecked);assertEquals(View.GONE,details.visibility)
            view.findViewWithTag<View>("drink_reminder_expand").performClick()
            assertEquals(View.VISIBLE,details.visibility);assertFalse(toggle.isChecked)
            toggle.performClick();assertTrue(DrinkReminderSettings.read(prefs).enabled)
            val slider=view.findViewWithTag<SeekBar>("drink_reminder_interval")
            assertEquals(10,slider.max);assertEquals(4,slider.progress)
            assertEquals("30 min",view.findViewWithTag<TextView>("drink_reminder_interval_value").text.toString())
            val listener=android.widget.SeekBar::class.java.getDeclaredField("mOnSeekBarChangeListener").apply { isAccessible=true }.get(slider) as SeekBar.OnSeekBarChangeListener
            for(step in 0..10) {
                slider.progress=step;listener.onProgressChanged(slider,step,true)
                assertEquals(10+step*5,DrinkReminderSettings.read(prefs).minutes)
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun quickAccessIsIndependentOfSelectedFieldAndUsesTheSameProfileSetting() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            for(id in listOf("GPS_SPEED","HEART_RATE")) {
                val controls=DrinkReminderSettingsView(a,themes,prefs)
                val picker=FieldPickerView(a,LiveData(),GpsValues(),null,PulseValues(),DashboardField(id),false,
                    themes=themes,trainingHelp=TrainingHelpView(a,themes,DashboardTrainingHelp()) {},drinkReminder=controls,choose={})
                a.setContentView(picker)
                assertNotNull(picker.findViewWithTag<View>("drink_reminder_interval"))
                assertNotSame(picker.findViewWithTag<View>("field_list"),controls.parent)
                picker.findViewWithTag<Switch>("drink_reminder_enabled").performClick()
                val profile=DrinkReminderSettingsView(a,themes,prefs)
                assertEquals(DrinkReminderSettings.read(prefs).enabled,profile.findViewWithTag<Switch>("drink_reminder_enabled").isChecked)
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun panelDismissesOnTapAndTimeoutAndCancelsOnDetach() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val panel=DrinkReminderPanel(a,ThemeManager(a.getSharedPreferences("drink-panel",0)))
            a.setContentView(panel)
            panel.showReminder();assertEquals(View.VISIBLE,panel.visibility)
            panel.performClick();assertEquals(View.GONE,panel.visibility)
            panel.showReminder();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(14));assertEquals(View.VISIBLE,panel.visibility)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertEquals(View.GONE,panel.visibility)
            panel.showReminder();a.setContentView(View(a));assertEquals(View.GONE,panel.visibility)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun settingsAreProfileScopedCopiedAndBackedUpWithDesign() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            val bank=BikeUiConfigurationStore(prefs)
            bank.ensure(listOf(0,1));DrinkReminderSettings.save(prefs,DrinkReminderConfig(true,45));bank.save(0)
            bank.apply(1);assertEquals(DrinkReminderConfig(),DrinkReminderSettings.read(prefs))
            bank.copy(0,1);assertEquals(DrinkReminderConfig(true,45),DrinkReminderSettings.read(prefs))
            val backup=SettingsBackup(a,themes);val bytes=backup.export(setOf(BackupSection.DESIGN))
            DrinkReminderSettings.save(prefs,DrinkReminderConfig(false,10))
            backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.DESIGN)))
            assertEquals(DrinkReminderConfig(true,45),DrinkReminderSettings.read(prefs))
            prefs.edit().putInt(DrinkReminderSettings.intervalKey,17).apply()
            assertEquals(30,DrinkReminderSettings.read(prefs).minutes)
        } finally { host.pause().stop().destroy() }
    }
}
