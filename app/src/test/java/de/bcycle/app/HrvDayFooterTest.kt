package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvDayFooterTest {
    private fun layout(view:View) { view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));view.layout(0,0,360,1000) }
    private fun tap(chart:HrvTrendChart) {
        val x=40+(chart.width-48)*13.5f/14f;val now=android.os.SystemClock.uptimeMillis()
        MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,150f,0).also { chart.onTouchEvent(it);it.recycle() }
        MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,x,150f,0).also { chart.onTouchEvent(it);it.recycle() }
    }
    private fun drain(controller:HrvController) {
        (HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as ExecutorService).submit {}.get(10,TimeUnit.SECONDS)
        shadowOf(Looper.getMainLooper()).idle()
    }
    @Test fun explicitTapDimsOnlyOneBarAndOpensDayActionsUntilResolutionChanges() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("day-footer",0))
            val rows=(0..30).map { i -> val day=LocalDate.of(2024,1,1).plusDays(i.toLong());HrvHistorySummary("day$i",day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),true,40.0,zoneId=ZoneId.systemDefault().id,metrics=mapOf("rmssdMs" to 40.0,"restingHrBpm" to 60.0)) }
            val series=HrvTrendMetric.entries.associateWith { HrvTrends.series(rows,it) }
            var opened:String?=null;var deleted:String?=null;var assessed:String?=null
            val view=HrvTrendsView(a,themes,series,delete={ deleted=it },assess={ assessed=it }) { opened=it }
            a.setContentView(view);layout(view)
            val footer=view.findViewWithTag<View>("bicycle_action_footer");val chart=view.findViewWithTag<HrvTrendChart>("hrv_trend_chart")
            assertEquals(View.GONE,footer.visibility);assertNull(chart.actionDay)
            view.findViewWithTag<View>("hrv_trend_HR").performClick();assertEquals(View.GONE,footer.visibility)
            tap(chart);assertEquals(View.VISIBLE,footer.visibility);assertEquals(LocalDate.of(2024,1,31),chart.actionDay)
            val image=Bitmap.createBitmap(chart.width,chart.height,Bitmap.Config.ARGB_8888)
            val colors=mutableListOf<Int>()
            chart.draw(object:Canvas(image) { override fun drawRect(l:Float,t:Float,r:Float,b:Float,p:Paint) { colors+=p.color;super.drawRect(l,t,r,b,p) } })
            val base=themes.active.tokens.primary;val half=Color.rgb(Color.red(base)/2,Color.green(base)/2,Color.blue(base)/2)
            assertEquals(1,colors.count { it==half });assertTrue(colors.count { it==base }>1);image.recycle()
            val analysis=view.findViewWithTag<View>("hrv_day_analysis");assertTrue(analysis.isEnabled);analysis.performClick();assertEquals("day30",assessed);assertNull(opened)
            view.findViewWithTag<View>("hrv_day_questionnaire").performClick();assertEquals("day30",opened)
            view.findViewWithTag<View>("hrv_day_delete").performClick();assertEquals("day30",deleted)
            view.findViewWithTag<View>("hrv_trend_RMSSD").performClick();assertEquals(View.VISIBLE,footer.visibility)
            view.findViewWithTag<View>("hrv_period_WEEK").performClick();assertEquals(View.GONE,footer.visibility);assertNull(chart.actionDay)
            view.findViewWithTag<View>("hrv_period_DAY").performClick();assertEquals(View.GONE,footer.visibility)
            tap(chart);view.updateSeries(series);assertEquals(View.GONE,footer.visibility);assertNull(chart.actionDay)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun fixedFooterOpensQuestionnaireDirectlyAndDeletionRequiresConfirmationThenRefreshes() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val controller=RideRuntime.get(a).hrv;drain(controller)
            val store=HrvStore(File(a.filesDir,"hrv-measurements"))
            val entry=Hrv4TrainingCsv.preview("date,time,rMSSD,HR,note\n2024-10-16,07:15,40,60,Mein Tag\n".byteInputStream(),ZoneId.of("Europe/Berlin")).entries.single()
            val id=entry.result.getString("id");store.importHrv4Training(entry)
            MainActivity::class.java.getDeclaredMethod("showHrvTrends").apply { isAccessible=true }.invoke(a);drain(controller)
            val screen=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();val decor=screen.window!!.decorView;layout(decor)
            val chart=decor.findViewWithTag<HrvTrendChart>("hrv_trend_chart")
            val question=decor.findViewWithTag<View>("hrv_day_questionnaire");val footer=question.parent as View
            assertEquals("bicycle_fullscreen_footer",footer.tag);assertEquals("bicycle_fullscreen",(footer.parent as View).tag)
            assertEquals(View.GONE,footer.visibility);tap(chart);assertEquals(View.VISIBLE,footer.visibility)
            decor.findViewWithTag<View>("hrv_day_analysis").performClick();drain(controller)
            val assessment=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            assertNotNull(assessment.window!!.decorView.findViewWithTag<View>("hrv_daily_assessment"))
            assertEquals(SettingsHelpPage.HRV_DAY,SettingsHelp.resolve(assessment))
            assertTrue(assessment.window!!.decorView.findViewWithTag<android.widget.TextView>("bicycle_dialog_title").text.contains("16.10.2024"))
            assessment.dismiss();shadowOf(Looper.getMainLooper()).idle()
            question.performClick();drain(controller);drain(controller)
            val questionnaire=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            assertNotSame(screen,questionnaire);assertNotNull(questionnaire.window!!.decorView.findViewWithTag<View>("hrvq_notes"));assertFalse(screen.isShowing) // Existing navigation suspends the parent while a child is visible.
            questionnaire.dismiss();shadowOf(Looper.getMainLooper()).idle();assertTrue(screen.isShowing)
            decor.findViewWithTag<View>("hrv_day_delete").performClick()
            val confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(id in store.ids())
            confirm.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(id in store.ids())
            decor.findViewWithTag<View>("hrv_day_delete").performClick()
            org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
            // Native confirmation posts its click; delete then posts a separate series reload.
            repeat(4) { drain(controller) }
            assertFalse(id in store.ids());assertEquals(View.GONE,footer.visibility);assertTrue(screen.isShowing)
            screen.dismiss()
        } finally { host.pause().stop().destroy() }
    }
}
