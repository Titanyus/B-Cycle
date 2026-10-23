package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvChartScrollingTest {
    private data class Label(val text:String,val x:Float,val y:Float,val clip:Rect)
    private class Labels(bitmap:Bitmap):Canvas(bitmap) {
        val labels=mutableListOf<Label>()
        override fun drawText(text:String,x:Float,y:Float,paint:Paint) {
            labels+=Label(text,x,y,Rect(clipBounds));super.drawText(text,x,y,paint)
        }
    }
    private fun layout(view:View) {
        view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));view.layout(0,0,360,400)
    }
    private fun labels(view:View):List<Label> {
        val bitmap=Bitmap.createBitmap(360,400,Bitmap.Config.ARGB_8888)
        return try { val canvas=Labels(bitmap);view.draw(canvas);canvas.labels } finally { bitmap.recycle() }
    }
    private val start=LocalDate.of(2024,1,1)
    private fun points()=(0..80).map { day ->
        val date=start.plusDays(day.toLong())
        HrvTrendPoint(HrvHistorySummary("day$day",date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),true,40.0,zoneId="UTC"),date,"shared",40.0,null,null,null)
    }
    private fun viewport(chart:HrvTrendChart)=HrvTrendChart::class.java.getDeclaredField("viewport").apply { isAccessible=true }.get(chart) as HrvTrendViewport
    @Test fun weeklyDatesStayOnTheirMondayAndMoveWithoutEdgeClamping() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=host.get();val chart=HrvTrendChart(activity,ThemeManager(activity.getSharedPreferences("weekly",0)),{},{_,_->})
            activity.setContentView(chart);layout(chart);chart.show(points(),true)
            val first=labels(chart).filter { it.text.matches(Regex("[0-9]{2}\\.[0-9]{2}")) }
            assertTrue(first.isNotEmpty())
            first.forEach { label ->
                val date=LocalDate.parse(label.text+".2024",DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                assertEquals(DayOfWeek.MONDAY,date.dayOfWeek)
                assertEquals(Rect(40,368,352,400),label.clip)
            }
            // Move the Monday at 18 March towards the right edge, retaining its date and position.
            val view=viewport(chart);view.pan(-3.5)
            val before=labels(chart).single { it.text=="18.03" }
            view.pan(-.25)
            val after=labels(chart).single { it.text=="18.03" }
            assertEquals(312f/14f*.25f,after.x-before.x,.001f)
            assertTrue(after.x>352f) // Its text is clipped, never pinned to the right edge.
        } finally { host.pause().stop().destroy() }
    }
    @Test fun measurementSecondsMoveThroughBothClippedEdgesInsteadOfSticking() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=host.get();val graph=HrvGraphView(activity,ThemeManager(activity.getSharedPreferences("seconds",0)))
            activity.setContentView(graph);layout(graph)
            graph.show(emptyList(),65000,false)
            val first=labels(graph).filter { it.y==374f }
            graph.show(emptyList(),66000,false)
            val second=labels(graph).filter { it.y==374f }
            assertEquals(8,first.size)
            first.forEach { label -> assertEquals(Rect(48,356,348,380),label.clip) }
            assertEquals(-5f,second.single { it.text=="60" }.x-first.single { it.text=="60" }.x,.001f)
            assertTrue(first.single { it.text=="0" }.x<48f)
            assertTrue(first.single { it.text=="70" }.x>348f)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun fastSwipeCoastsAfterReleaseAndNewTouchStopsTheFling() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=host.get();val chart=HrvTrendChart(activity,ThemeManager(activity.getSharedPreferences("fling",0)),{},{_,_->})
            activity.setContentView(chart);layout(chart);chart.show(points(),true)
            val axis=chart.axis;val time=SystemClock.uptimeMillis()
            fun event(offset:Long,action:Int,x:Float) { MotionEvent.obtain(time,time+offset,action,x,160f,0).also { chart.onTouchEvent(it);it.recycle() } }
            event(0,MotionEvent.ACTION_DOWN,100f);event(20,MotionEvent.ACTION_MOVE,160f);event(40,MotionEvent.ACTION_MOVE,220f);event(60,MotionEvent.ACTION_UP,250f)
            val released=viewport(chart).end
            repeat(15) { SystemClock.sleep(16);chart.computeScroll() }
            assertTrue(viewport(chart).end<released-1.0);assertEquals(axis,chart.axis)
            event(500,MotionEvent.ACTION_DOWN,180f);val stopped=viewport(chart).end
            repeat(15) { SystemClock.sleep(16);chart.computeScroll() }
            assertEquals(stopped,viewport(chart).end,0.0)
            event(800,MotionEvent.ACTION_CANCEL,180f)
        } finally { host.pause().stop().destroy() }
    }
}
