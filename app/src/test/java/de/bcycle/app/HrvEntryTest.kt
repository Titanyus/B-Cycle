package de.bcycle.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.*
import de.bcycle.hrv.Phase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import java.time.LocalDate
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvEntryTest {
    private fun layout(view:View) {
        repeat(2) {
            view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
            view.layout(0,0,360,800);shadowOf(Looper.getMainLooper()).idle()
        }
    }
    private fun drain(controller:HrvController) {
        (HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as ExecutorService).submit {}.get(10,TimeUnit.SECONDS)
        shadowOf(Looper.getMainLooper()).idle()
    }
    @Test fun dailyOfferRepeatsUntilAcknowledgedAndReturnsOnNextDay() {
        val c=RuntimeEnvironment.getApplication();val settings=HrvSettings(c);val day=LocalDate.of(2026,10,25)
        assertFalse(HrvDailyLaunch(c).shouldShow(day))
        // Old versions marked viewing as handled; that marker must not suppress the new offer.
        c.getSharedPreferences("hrv-launch-state",0).edit().putString("lastDay",day.toString()).commit()
        settings.write(settings.read().copy(showOnFirstStart=true))
        repeat(2) { assertTrue(HrvDailyLaunch(c).shouldShow(day)) }
        HrvDailyLaunch(c).acknowledge(day)
        assertFalse(HrvDailyLaunch(c).shouldShow(day))
        assertTrue(HrvDailyLaunch(c).shouldShow(day.plusDays(1)))
        settings.write(settings.read().copy(showOnFirstStart=false))
        assertFalse(HrvDailyLaunch(c).shouldShow(day.plusDays(1)))
    }
    @Test fun outsideDismissRestoresDashboardAndHeartStartsOnlyWhenFreshRrArrives() {
        val c=RuntimeEnvironment.getApplication();HrvSettings(c).write(HrvMeasurementConfig(showOnFirstStart=true))
        var host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            var a=host.get();var root=a.findViewById<View>(android.R.id.content);layout(root)
            val overlay=root.findViewWithTag<View>("hrv_daily_prompt");assertNotNull(overlay)
            assertFalse(RideRuntime.get(a).hrv.busy)
            val bitmap=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888);root.draw(Canvas(bitmap))
            java.io.File("build/reports/ui/hrv-daily-prompt.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            overlay.performClick();assertNull(root.findViewWithTag<View>("hrv_daily_prompt"))
            host.pause().stop().start().resume();assertNull(root.findViewWithTag<View>("hrv_daily_prompt"))
            host.pause().stop().destroy()
            // A later calendar day (simulate persisted launch marker, without changing device clocks).
            c.getSharedPreferences("hrv-launch-state",0).edit().putString("handledDay",LocalDate.now().minusDays(1).toString()).commit()
            host=Robolectric.buildActivity(MainActivity::class.java).setup();a=host.get();root=a.findViewById(android.R.id.content)
            val runtime=RideRuntime.get(a);drain(runtime.hrv)
            root.findViewWithTag<View>("hrv_daily_start").performClick()
            val screen=ShadowAlertDialog.getLatestAlertDialog();assertTrue(screen.isShowing)
            assertFalse(runtime.hrv.busy) // It must not fabricate RR or bypass sensor readiness.
            host.pause().stop()
            runtime.hrv.packet(SystemClock.elapsedRealtime(),byteArrayOf(0x16,75,0,4));drain(runtime.hrv)
            assertFalse(runtime.hrv.busy) // A waiting automatic start must not run in the background.
            host.start().resume()
            runtime.hrv.packet(SystemClock.elapsedRealtime(),byteArrayOf(0x16,75,0,4));drain(runtime.hrv);drain(runtime.hrv)
            assertTrue(runtime.hrv.busy);assertEquals(Phase.REST,runtime.hrv.state.phase);assertFalse(runtime.recording)
            screen.dismiss();drain(runtime.hrv);drain(runtime.hrv);assertEquals(Phase.ABORTED,runtime.hrv.state.phase)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun returningFromSettingsShowsDashboardUntilTheOfferIsTapped() {
        val c=RuntimeEnvironment.getApplication()
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get();val runtime=RideRuntime.get(a)
            // Enter settings before enabling the daily offer, as with a preserved dialog stack.
            testDescendants(a.findViewById(android.R.id.content)).single { it.contentDescription==a.getString(R.string.ui_menu_open) }.performClick()
            val menu=ShadowAlertDialog.getLatestAlertDialog();assertTrue(menu.isShowing)
            host.pause().stop()
            HrvSettings(c).write(HrvMeasurementConfig(showOnFirstStart=true))
            host.start().resume()
            val root=a.findViewById<View>(android.R.id.content);layout(root)
            assertFalse(menu.isShowing)
            assertNotNull(root.findViewWithTag<View>("hrv_daily_prompt"))
            assertTrue(HrvDailyLaunch(c).shouldShow())
            // Lifecycle cleanup is not a manual dismissal. Repeat on another foreground start.
            host.pause().stop().start().resume()
            assertNotNull(root.findViewWithTag<View>("hrv_daily_prompt"))
            root.findViewWithTag<View>("hrv_daily_prompt").performClick()
            assertFalse(HrvDailyLaunch(c).shouldShow())
            host.pause().stop().start().resume()
            assertNull(root.findViewWithTag<View>("hrv_daily_prompt"))
            assertFalse(runtime.recording)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun ratingsUseNeutralOriginSleepTimesShareRowAndContinueWaitsForSuccessfulSave() {
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0));val settings=HrvQuestionSettings(a)
            settings.write(HrvQuestionConfig(setOf(HrvQuestion.WAKE,HrvQuestion.BED,HrvQuestion.SLEEP,HrvQuestion.SORENESS,HrvQuestion.FATIGUE,HrvQuestion.RPE,HrvQuestion.SPORT)))
            var done=0;var succeeds=false;var saved:HrvQuestionnaire?=null
            val form=HrvQuestionnaireView(a,themes,HrvQuestionnaire("2026-10-22"),null,complete={done++}) { value,finish -> saved=value;finish(succeeds) }
            a.setContentView(ScrollView(a).apply { addView(form) });layout(a.findViewById(android.R.id.content))
            assertEquals(2,form.findViewWithTag<LinearLayout>("hrvq_sleep_times").childCount)
            assertEquals(form.findViewWithTag<EditText>("hrvq_wake").top,form.findViewWithTag<EditText>("hrvq_bed").top)
            val sleep=form.findViewWithTag<BicycleRatingSlider>("hrvq_sleep")
            assertEquals(5,sleep.origin);assertEquals(5,sleep.progress)
            for(key in listOf("soreness","fatigue")) {
                val slider=form.findViewWithTag<BicycleRatingSlider>("hrvq_$key")
                assertEquals(0,slider.origin);assertEquals(0,slider.progress)
            }
            assertNull(form.findViewWithTag<View>("hrvq_sport"))
            assertNull(HrvQuestionSettingsView(a,themes).findViewWithTag<View>("hrvq_enabled_SPORT"))
            val old=JSONObject("""{"schema":1,"enabled":["SPORT","SLEEP"],"customNames":["","",""]}""")
            assertEquals(setOf(HrvQuestion.SLEEP),HrvQuestionConfig.decode(old).enabled)
            settings.preferences.edit().putString(HrvQuestionSettings.KEY,old.toString()).commit()
            val backed=SettingsBackupSupplement(a).export(BackupSection.PROFILE).getJSONObject(HrvQuestionSettings.STORE).getString(HrvQuestionSettings.KEY)
            assertFalse(backed.contains("SPORT"))
            assertFalse(testDescendants(form).filterIsInstance<TextView>().any { it.text.toString().startsWith("RPE ") })
            val button=form.findViewWithTag<View>("hrvq_analysis");button.performClick();assertEquals(0,done)
            assertTrue(saved!!.ratings.isEmpty()) // The initial zero/center is still unanswered.
            succeeds=true;button.performClick();assertEquals(1,done)
            // Render the actual native track: accent begins at center in both directions.
            val track=sleep.progressDrawable;track.setBounds(0,0,200,10)
            fun color(at:Int,progress:Int):Int {
                sleep.progress=progress
                val image=Bitmap.createBitmap(200,10,Bitmap.Config.ARGB_8888);track.draw(Canvas(image))
                return image.getPixel(at,5).also { image.recycle() }
            }
            assertEquals(themes.active.tokens.accent,color(120,6));assertNotEquals(themes.active.tokens.accent,color(20,6))
            assertEquals(themes.active.tokens.accent,color(70,2));assertNotEquals(themes.active.tokens.accent,color(130,2))
        } finally { host.pause().stop().destroy() }
    }
}
