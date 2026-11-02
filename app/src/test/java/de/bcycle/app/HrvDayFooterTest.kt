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
            var opened:String?=null;var deleted:String?=null
            val view=HrvTrendsView(a,themes,series,delete={ deleted=it }) { opened=it }
            a.setContentView(view);layout(view)
            val footer=view.findViewWithTag<View>("bicycle_action_footer");val chart=view.findViewWithTag<HrvTrendChart>("hrv_trend_chart")
            assertEquals(View.GONE,footer.visibility);assertEquals(LocalDate.of(2024,1,31),chart.actionDay)
            view.findViewWithTag<View>("hrv_trend_HR").performClick();assertEquals(View.GONE,footer.visibility)
            tap(chart);assertEquals(View.VISIBLE,footer.visibility);assertEquals(LocalDate.of(2024,1,31),chart.actionDay)
            val image=Bitmap.createBitmap(chart.width,chart.height,Bitmap.Config.ARGB_8888)
            val colors=mutableListOf<Int>()
            chart.draw(object:Canvas(image) { override fun drawRect(l:Float,t:Float,r:Float,b:Float,p:Paint) { colors+=p.color;super.drawRect(l,t,r,b,p) } })
            val base=themes.active.tokens.primary;val half=Color.rgb(Color.red(base)/2,Color.green(base)/2,Color.blue(base)/2)
            assertEquals(1,colors.count { it==half });assertTrue(colors.count { it==base }>1);image.recycle()
            assertNull(view.findViewWithTag<View>("hrv_day_analysis"));assertNull(opened)
            view.findViewWithTag<View>("hrv_day_questionnaire").performClick();assertEquals("day30",opened)
            view.findViewWithTag<View>("hrv_day_delete").performClick();assertEquals("day30",deleted)
            view.findViewWithTag<View>("hrv_trend_RMSSD").performClick();assertEquals(View.VISIBLE,footer.visibility)
            view.findViewWithTag<View>("hrv_period_WEEK").performClick();assertEquals(View.GONE,footer.visibility);assertNull(chart.actionDay)
            view.findViewWithTag<View>("hrv_period_DAY").performClick();assertEquals(View.GONE,footer.visibility)
            tap(chart);view.updateSeries(series);assertEquals(View.GONE,footer.visibility);assertEquals(LocalDate.of(2024,1,31),chart.actionDay)
        } finally { host.pause().stop().destroy() }
    }
    private fun tapDay(chart:HrvTrendChart,day:LocalDate) {
        val viewport=HrvTrendChart::class.java.getDeclaredField("viewport").apply { isAccessible=true }.get(chart) as HrvTrendViewport
        val x=40+((day.toEpochDay()-(viewport.end-viewport.span))/viewport.span*(chart.width-48)).toFloat()
        val now=android.os.SystemClock.uptimeMillis()
        MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,150f,0).also { chart.onTouchEvent(it);it.recycle() }
        MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,x,150f,0).also { chart.onTouchEvent(it);it.recycle() }
    }
    @Test fun sharedDayAndFooterToggleStayInSyncAcrossSelectionEditingDeletionAndFreshEntry() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val controller=RideRuntime.get(a).hrv;drain(controller)
            val store=HrvStore(File(a.filesDir,"hrv-measurements"))
            val today=LocalDate.now();val yesterday=today.minusDays(1)
            val entries=Hrv4TrainingCsv.preview("date,time,rMSSD,HR,note\n$yesterday,07:15,20,70,Gestern\n$today,07:15,40,60,Heute\n".byteInputStream(),ZoneId.systemDefault()).entries
            entries.forEach(store::importHrv4Training)
            val id=entries.single { it.result.getJSONObject("external").getString("day")==yesterday.toString() }.result.getString("id")
            fun openScreen():android.app.AlertDialog {
                MainActivity::class.java.getDeclaredMethod("showHrvTrends").apply { isAccessible=true }.invoke(a)
                repeat(2) { drain(controller) }
                return org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().also { layout(it.window!!.decorView) }
            }
            fun assessment(decor:View)=decor.findViewWithTag<HrvDailyAssessmentView>("hrv_daily_assessment").assessment
            val screen=openScreen();val decor=screen.window!!.decorView
            val chart=decor.findViewWithTag<HrvTrendChart>("hrv_trend_chart")
            val question=decor.findViewWithTag<View>("hrv_day_questionnaire");val footer=question.parent as View
            val toggle=decor.findViewWithTag<View>("route_footer_toggle")
            val title=decor.findViewWithTag<android.widget.TextView>("bicycle_dialog_title")
            val format=java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
            assertEquals(today,assessment(decor).day);assertEquals(today.format(format),title.text.toString())
            assertEquals(40.0,assessment(decor).hrv.value!!,0.0);assertEquals(today,chart.actionDay)
            assertNull(decor.findViewWithTag<View>("hrv_day_analysis"));assertEquals(3,(footer as android.view.ViewGroup).childCount)
            assertEquals(View.GONE,footer.visibility)
            toggle.performClick();assertEquals(View.VISIBLE,footer.visibility)
            toggle.performClick();assertEquals(View.GONE,footer.visibility)
            tapDay(chart,yesterday);drain(controller)
            assertEquals(View.VISIBLE,footer.visibility);assertEquals(yesterday,assessment(decor).day)
            assertEquals(20.0,assessment(decor).hrv.value!!,0.0);assertEquals(yesterday.format(format),title.text.toString())
            assertEquals(yesterday,chart.actionDay)
            toggle.performClick();assertEquals(View.GONE,footer.visibility)
            toggle.performClick();assertEquals(View.VISIBLE,footer.visibility)
            // Queue two worker results; the older callback must not replace the latest selected day.
            tapDay(chart,today);tapDay(chart,yesterday);repeat(2) { drain(controller) }
            assertEquals(yesterday,assessment(decor).day);assertEquals(yesterday.format(format),title.text.toString())
            question.performClick();repeat(2) { drain(controller) }
            val questionnaire=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            assertNotSame(screen,questionnaire);assertNotNull(questionnaire.window!!.decorView.findViewWithTag<View>("hrvq_notes"));assertFalse(screen.isShowing)
            questionnaire.dismiss();repeat(2) { drain(controller) };assertTrue(screen.isShowing)
            assertEquals(yesterday,assessment(decor).day)
            decor.findViewWithTag<View>("hrv_day_delete").performClick()
            val confirm=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(id in store.ids())
            confirm.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertTrue(id in store.ids())
            decor.findViewWithTag<View>("hrv_day_delete").performClick()
            org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
            repeat(4) { drain(controller) }
            assertFalse(id in store.ids());assertEquals(yesterday,assessment(decor).day)
            assertNull(assessment(decor).score);assertFalse(question.isEnabled);assertTrue(screen.isShowing)
            assertTrue(decor.findViewWithTag<android.widget.TextView>("hrv_trend_RMSSD").contentDescription.endsWith(a.getString(R.string.hrvt_unavailable)))
            screen.dismiss()
            val fresh=openScreen();assertEquals(today,assessment(fresh.window!!.decorView).day)
            assertEquals(View.GONE,fresh.window!!.decorView.findViewWithTag<View>("hrv_day_questionnaire").let { (it.parent as View).visibility })
            fresh.dismiss()
        } finally { host.pause().stop().destroy() }
    }
    @Test fun freshEntryWithoutTodayMeasurementDoesNotSelectAnOlderDay() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val controller=RideRuntime.get(a).hrv;drain(controller)
            val yesterday=LocalDate.now().minusDays(1)
            val store=HrvStore(File(a.filesDir,"hrv-measurements"))
            Hrv4TrainingCsv.preview("date,time,rMSSD,HR\n$yesterday,07:15,40,60\n".byteInputStream(),ZoneId.systemDefault()).entries.forEach(store::importHrv4Training)
            MainActivity::class.java.getDeclaredMethod("showHrvTrends").apply { isAccessible=true }.invoke(a)
            repeat(2) { drain(controller) }
            val screen=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();val decor=screen.window!!.decorView;layout(decor)
            val assessment=decor.findViewWithTag<HrvDailyAssessmentView>("hrv_daily_assessment").assessment
            assertEquals(LocalDate.now(),assessment.day);assertNull(assessment.score)
            assertNull(decor.findViewWithTag<HrvTrendChart>("hrv_trend_chart").actionDay)
            assertFalse(decor.findViewWithTag<View>("hrv_day_result").isEnabled)
            assertTrue(decor.findViewWithTag<android.widget.TextView>("hrv_trend_RMSSD").text.endsWith(a.getString(R.string.hrvt_unavailable)))
            decor.findViewWithTag<View>("route_footer_toggle").performClick()
            assertEquals(View.VISIBLE,(decor.findViewWithTag<View>("hrv_day_result").parent as View).visibility)
            screen.dismiss()
        } finally { host.pause().stop().destroy() }
    }
    @Test @Config(qualifiers="w360dp-h1000dp-mdpi") fun horizontalDaySwipesUseTheSharedMotionAndKeepTitleChartAndFooterSynchronized() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val controller=RideRuntime.get(a).hrv;drain(controller)
            val today=LocalDate.now();val yesterday=today.minusDays(1)
            val store=HrvStore(File(a.filesDir,"hrv-measurements"))
            Hrv4TrainingCsv.preview("date,time,rMSSD,HR\n$yesterday,07:15,30,65\n$today,07:15,40,60\n".byteInputStream(),ZoneId.systemDefault(),HrvCsvDateOrder.YEAR_MONTH_DAY).entries.forEach(store::importHrv4Training)
            MainActivity::class.java.getDeclaredMethod("showHrvTrends").apply { isAccessible=true }.invoke(a)
            repeat(2) { drain(controller) }
            val screen=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();val decor=screen.window!!.decorView;layout(decor)
            decor.findViewWithTag<View>("route_footer_toggle").performClick();layout(decor)
            val pager=decor.findViewWithTag<DashboardPageViewport>("hrv_day_viewport")
            assertTrue(pager.height>0)
            val footer=decor.findViewWithTag<View>("hrv_day_result").parent as View
            val footerPosition=IntArray(2);footer.getLocationOnScreen(footerPosition)
            org.robolectric.shadows.ShadowChoreographer.setPaused(true)
            android.animation.ValueAnimator::class.java.getDeclaredMethod("setDurationScale",Float::class.javaPrimitiveType).invoke(null,1f)
            fun touch(action:Int,x:Float,time:Long) {
                MotionEvent.obtain(0,time,action,x,80f,0).also { pager.dispatchTouchEvent(it);it.recycle() }
            }
            fun motion()=DashboardPageViewport::class.java.getDeclaredField("animator").apply { isAccessible=true }.get(pager) as android.animation.ValueAnimator
            fun selected()=decor.findViewWithTag<HrvDailyAssessmentView>("hrv_daily_assessment").assessment.day
            val initialChart=decor.findViewWithTag<HrvTrendChart>("hrv_trend_chart")
            assertEquals("initial chart selection",today,initialChart.actionDay)
            touch(MotionEvent.ACTION_DOWN,120f,0);touch(MotionEvent.ACTION_MOVE,140f,30)
            assertEquals(4f,pager.getChildAt(0).translationX,.01f)
            touch(MotionEvent.ACTION_MOVE,200f,80)
            repeat(2) { drain(controller) };layout(decor)
            val adjacent=pager.findViewWithTag<View>("hrv_adjacent_preview")
            assertNotNull("neighboring day is ready while dragging",adjacent)
            assertEquals(yesterday,adjacent.findViewWithTag<HrvDailyAssessmentView>("hrv_daily_assessment").assessment.day)
            assertEquals("active day remains unchanged until UP",today,selected())
            assertEquals(today,decor.findViewWithTag<HrvTrendChart>("hrv_trend_chart").actionDay)
            touch(MotionEvent.ACTION_UP,200f,90)
            repeat(2) { drain(controller) };layout(decor);pager.viewTreeObserver.dispatchOnPreDraw()
            assertTrue("day=${selected()} visible=${RideRuntime.get(a).visible} width=${pager.width} x=${pager.getChildAt(0).translationX}",pager.running);assertEquals(yesterday,selected())
            assertEquals(yesterday,decor.findViewWithTag<HrvTrendChart>("hrv_trend_chart").actionDay)
            assertEquals(yesterday.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),decor.findViewWithTag<android.widget.TextView>("bicycle_dialog_title").text.toString())
            motion().currentPlayTime=80
            val after=IntArray(2);footer.getLocationOnScreen(after);assertEquals(footerPosition[1],after[1])
            motion().end();assertFalse(pager.running);assertEquals(1,pager.childCount)
            touch(MotionEvent.ACTION_DOWN,180f,1000);touch(MotionEvent.ACTION_MOVE,160f,1030);touch(MotionEvent.ACTION_MOVE,100f,1080);touch(MotionEvent.ACTION_UP,100f,1090)
            layout(decor);pager.viewTreeObserver.dispatchOnPreDraw();assertEquals(today,selected());motion().end()
            // Today is the upper boundary: bounce back instead of opening an empty future day.
            touch(MotionEvent.ACTION_DOWN,180f,2000);touch(MotionEvent.ACTION_MOVE,100f,2080);touch(MotionEvent.ACTION_UP,100f,2090)
            assertEquals(today,selected());motion().end();assertEquals(0f,pager.getChildAt(0).translationX,0f)
            screen.dismiss()
        } finally { org.robolectric.shadows.ShadowChoreographer.setPaused(false);host.pause().stop().destroy() }
    }
}
