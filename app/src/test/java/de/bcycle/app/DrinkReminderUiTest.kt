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
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
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
            val seconds=view.findViewWithTag<SeekBar>("drink_reminder_duration")
            assertEquals(11,seconds.max);assertEquals(2,seconds.progress)
            val durationListener=SeekBar::class.java.getDeclaredField("mOnSeekBarChangeListener").apply { isAccessible=true }.get(seconds) as SeekBar.OnSeekBarChangeListener
            for(step in 0..11) {
                seconds.progress=step;durationListener.onProgressChanged(seconds,step,true)
                assertEquals(5+5*step,DrinkReminderSettings.read(prefs).durationSeconds)
            }
            for(position in MessagePosition.entries) {
                view.findViewWithTag<View>("drink_reminder_position_${position.name}").performClick()
                assertEquals(position,DrinkReminderSettings.read(prefs).position)
                assertEquals(1,MessagePosition.entries.count { view.findViewWithTag<View>("drink_reminder_position_${it.name}").isSelected })
            }
            view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST));view.layout(0,0,360,view.measuredHeight)
            val image=android.graphics.Bitmap.createBitmap(360,view.height,android.graphics.Bitmap.Config.ARGB_8888)
            image.eraseColor(themes.active.tokens.secondary);view.draw(android.graphics.Canvas(image))
            java.io.File("build/reports/ui/drink-reminder-settings.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };image.recycle()


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
                val training=picker.findViewWithTag<TrainingHelpView>("training_help_controls")
                assertSame(training,controls.parent)
                assertSame(controls,training.getChildAt(training.childCount-1))
                assertEquals("bicycle_section_divider",training.getChildAt(training.childCount-2).tag)
                assertFalse(testDescendants(picker).filterIsInstance<BicycleSectionHeader>().any { it.contentDescription==a.getString(R.string.drink_reminder_title) })
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
            panel.showReminder(5);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));assertEquals(View.VISIBLE,panel.visibility)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertEquals(View.GONE,panel.visibility)
            panel.showReminder(60);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(59));assertEquals(View.VISIBLE,panel.visibility)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertEquals(View.GONE,panel.visibility)
            panel.showReminder();a.setContentView(View(a));assertEquals(View.GONE,panel.visibility)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun settingsAreProfileScopedCopiedAndBackedUpWithDesign() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            val bank=BikeUiConfigurationStore(prefs)
            val expected=DrinkReminderConfig(true,45,60,MessagePosition.TOP)
            bank.ensure(listOf(0,1));DrinkReminderSettings.save(prefs,expected);bank.save(0)
            bank.apply(1);assertEquals(DrinkReminderConfig(),DrinkReminderSettings.read(prefs))
            bank.copy(0,1);assertEquals(expected,DrinkReminderSettings.read(prefs))
            val backup=SettingsBackup(a,themes);val bytes=backup.export(setOf(BackupSection.DESIGN))
            DrinkReminderSettings.save(prefs,DrinkReminderConfig(false,10))
            backup.apply(backup.plan(backup.decode(bytes),setOf(BackupSection.DESIGN)))
            assertEquals(expected,DrinkReminderSettings.read(prefs))
            prefs.edit().putInt(DrinkReminderSettings.intervalKey,17).apply()
            assertEquals(30,DrinkReminderSettings.read(prefs).minutes)
        } finally { host.pause().stop().destroy() }
    }
}
