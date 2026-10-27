package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.*
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvMoonChartTest {
    @Test fun allFourPhasesSitOnTopRuleAndFollowCalendarDaysWithoutRequiringMeasurements() {
        val oldZone=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("moon-test",0))
            val chart=HrvTrendChart(c,themes,{},{_,_->})
            c.setContentView(chart)
            chart.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));chart.layout(0,0,720,400)
            val first=LocalDate.of(2024,3,30);val last=LocalDate.of(2024,4,27)
            // Only endpoints have measurements; principal phases on the missing days still appear.
            val rows=listOf(first,last).map { day -> HrvTrendPoint(HrvHistorySummary(day.toString(),day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),true,40.0),day,"shared",40.0,null,null,null) }
            var chosen:LocalDate?=null
            chart.onDaySelected={ chosen=it }
            chart.show(rows,true);assertNull(chosen);chart.performAccessibilityAction(HrvTrendChart.ZOOM_OUT,null)
            val viewport=HrvTrendChart::class.java.getDeclaredField("viewport").apply { isAccessible=true }.get(chart) as HrvTrendViewport
            val radius=6
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                val bitmap=Bitmap.createBitmap(720,400,Bitmap.Config.ARGB_8888)
                val canvas=Canvas(bitmap);canvas.drawColor(themes.active.tokens.secondary);chart.draw(canvas)
                for(event in LunarPhases.between(first,last,ZoneOffset.UTC)) {
                    val center=(40+(event.day(ZoneOffset.UTC).toEpochDay()-(viewport.end-viewport.span))/viewport.span*(712-40)).toInt()
                    val left=bitmap.getPixel(center-radius/2,16);val right=bitmap.getPixel(center+radius/2,16)
                    val bright=themes.active.tokens.foreground;val dark=themes.active.tokens.secondary
                    when(event.phase) {
                        LunarPhase.FULL->{ assertEquals(bright,left);assertEquals(bright,right) }
                        LunarPhase.NEW->{ assertEquals(dark,left);assertEquals(dark,right) }
                        LunarPhase.FIRST_QUARTER->{ assertEquals(dark,left);assertEquals(bright,right) }
                        LunarPhase.LAST_QUARTER->{ assertEquals(bright,left);assertEquals(dark,right) }
                    }
                }
                if(!flat) java.io.File("build/reports/ui/hrv-moons.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                bitmap.recycle()
                assertTrue(chart.contentDescription.toString().contains("Vollmond"))
                val at=(40+(last.toEpochDay()-(viewport.end-viewport.span))/viewport.span*(712-40)).toFloat()
                val now=android.os.SystemClock.uptimeMillis()
                for((action,time) in listOf(android.view.MotionEvent.ACTION_DOWN to now,android.view.MotionEvent.ACTION_UP to now+50)) {
                    val event=android.view.MotionEvent.obtain(now,time,action,at,200f,0);chart.onTouchEvent(event);event.recycle()
                }
                assertEquals(last,chosen)
            }
        } finally { host.pause().stop().destroy();TimeZone.setDefault(oldZone) }
    }
}
