package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvTrendPeriodUiTest {
    private data class Label(val text:String,val x:Float,val y:Float,val align:Paint.Align,val clip:Rect)
    private data class Line(val x:Float,val y:Float,val endX:Float,val endY:Float)
    private class Drawing(bitmap:Bitmap):Canvas(bitmap) {
        val labels=mutableListOf<Label>();val lines=mutableListOf<Line>()
        override fun drawText(text:String,x:Float,y:Float,paint:Paint) { labels+=Label(text,x,y,paint.textAlign,Rect(clipBounds));super.drawText(text,x,y,paint) }
        override fun drawLine(x:Float,y:Float,endX:Float,endY:Float,paint:Paint) { lines+=Line(x,y,endX,endY);super.drawLine(x,y,endX,endY,paint) }
    }
    private fun layout(view:View,height:Int=400) {
        view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,360,height)
    }
    private fun row(day:LocalDate,value:Double=40.0,hr:Double?=60.0)=HrvHistorySummary(day.toString(),day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),true,value,
        zoneId="UTC",metrics=mapOf("rmssdMs" to value,"sdnnMs" to value+5)+if(hr==null) emptyMap() else mapOf("restingHrBpm" to hr))
    private fun viewport(chart:HrvTrendChart)=HrvTrendChart::class.java.getDeclaredField("viewport").apply { isAccessible=true }.get(chart) as HrvTrendViewport
    @Test fun mondayRuleIsTheBarEdgeAtEveryZoomAndLabelStartsToItsRight() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val chart=HrvTrendChart(a,ThemeManager(a.getSharedPreferences("monday-edge",0)),{},{_,_->})
            a.setContentView(chart);layout(chart)
            chart.show(HrvTrends.series((0..180).map { row(LocalDate.of(2024,1,1).plusDays(it.toLong())) },HrvTrendMetric.RMSSD),true)
            val viewport=viewport(chart);val image=Bitmap.createBitmap(360,400,Bitmap.Config.ARGB_8888)
            try {
                for(factor in listOf(1.0,.14,.77)) {
                    viewport.zoom(factor,.5);viewport.pan(-.23)
                    val drawing=Drawing(image);chart.draw(drawing)
                    val labels=drawing.labels.filter { it.y==392f }
                    assertTrue(labels.isNotEmpty())
                    for(label in labels) {
                        val day=LocalDate.parse(label.text+".2024",java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        val center=40+(day.toEpochDay()-(viewport.end-viewport.span))/viewport.span*312
                        val half=minOf(312/viewport.span*.32,12.0)
                        val expected=(center-half).toFloat()
                        assertEquals(Paint.Align.LEFT,label.align);assertEquals(expected+4,label.x,.001f)
                        assertTrue(drawing.lines.any { kotlin.math.abs(it.x-expected)<.001f && it.endX==it.x && it.y==16f && it.endY==394f })
                        assertEquals(Rect(40,368,352,400),label.clip)
                    }
                }
            } finally { image.recycle() }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun weekAndMonthAxisLabelsAreCompactAndTapsSelectWholeCalendarBuckets() {
        val oldLocale=Locale.getDefault();Locale.setDefault(Locale.GERMAN)
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();var selected:HrvTrendPoint?=null;var first:LocalDate?=null;var last:LocalDate?=null
            val chart=HrvTrendChart(a,ThemeManager(a.getSharedPreferences("period-axis",0)),{selected=it}) { f,l -> first=f;last=l }
            a.setContentView(chart);layout(chart)
            val daily=HrvTrends.series((0..420).map { row(LocalDate.of(2024,1,1).plusDays(it.toLong()),30.0+it%19) },HrvTrendMetric.RMSSD)
            val axis=HrvTrendAxis.from(daily,HrvTrendMetric.RMSSD)
            val image=Bitmap.createBitmap(360,400,Bitmap.Config.ARGB_8888)
            try {
                for(mode in listOf(HrvTrendPeriod.WEEK,HrvTrendPeriod.MONTH)) {
                    val points=HrvTrendAggregation.series(daily,mode);chart.show(points,true,axis,mode)
                    val drawing=Drawing(image);chart.draw(drawing)
                    val labels=drawing.labels.filter { it.y==392f };assertTrue(labels.isNotEmpty())
                    if(mode==HrvTrendPeriod.WEEK) {
                        assertTrue(labels.all { it.text.matches(Regex("(?:KW)?[0-9]{2}")) })
                        assertEquals(java.time.DayOfWeek.MONDAY,first!!.dayOfWeek);assertEquals(java.time.DayOfWeek.SUNDAY,last!!.dayOfWeek)
                    } else {
                        val months=(1..12).map { LocalDate.of(2024,it,1).format(java.time.format.DateTimeFormatter.ofPattern("MMM",Locale.GERMAN)).removeSuffix(".") }
                        assertTrue(labels.all { it.text in months || it.text in months.map { name -> name.take(1).uppercase(Locale.GERMAN) } });assertTrue(labels.none { it.text.contains("2024") || it.text.contains(".") })
                        assertEquals(1,first!!.dayOfMonth);assertEquals(last!!.lengthOfMonth(),last!!.dayOfMonth)
                    }
                    val target=points[points.lastIndex-2];val viewport=viewport(chart)
                    val x=(40+(mode.index(target.day)-(viewport.end-viewport.span))/viewport.span*312).toFloat()
                    val now=android.os.SystemClock.uptimeMillis()
                    MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,160f,0).also { chart.onTouchEvent(it);it.recycle() }
                    MotionEvent.obtain(now,now+30,MotionEvent.ACTION_UP,x,160f,0).also { chart.onTouchEvent(it);it.recycle() }
                    assertEquals(target,selected);assertEquals(axis,chart.axis)
                    assertTrue(chart.performAccessibilityAction(HrvTrendChart.ZOOM_IN,null));assertEquals(axis,chart.axis)
                }
            } finally { image.recycle() }
        } finally { host.pause().stop().destroy();Locale.setDefault(oldLocale) }
    }
    @Test fun crowdedAxesShortenEveryLabelInsteadOfDroppingWeeksOrMonths() {
        val oldLocale=Locale.getDefault();Locale.setDefault(Locale.GERMAN)
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val chart=HrvTrendChart(a,ThemeManager(a.getSharedPreferences("hrv-adaptive-axis",0)),{},{_,_->})
            a.setContentView(chart);layout(chart)
            val daily=HrvTrends.series((0..900).map { row(LocalDate.of(2023,1,1).plusDays(it.toLong()),30.0+it%19) },HrvTrendMetric.RMSSD)
            val axis=HrvTrendAxis.from(daily,HrvTrendMetric.RMSSD)
            val image=Bitmap.createBitmap(360,400,Bitmap.Config.ARGB_8888)
            try {
                for(mode in listOf(HrvTrendPeriod.WEEK,HrvTrendPeriod.MONTH)) {
                    chart.show(HrvTrendAggregation.series(daily,mode),true,axis,mode)
                    val viewport=viewport(chart)
                    fun drawing():Drawing=Drawing(image).also { chart.draw(it) }
                    fun verifyEveryTick(drawing:Drawing,compact:Boolean) {
                        val labels=drawing.labels.filter { it.y==392f }
                        val separators=drawing.lines.filter { it.x==it.endX && it.y==16f && it.endY==394f }
                        assertEquals(separators.size,labels.size)
                        for(i in labels.indices) assertEquals(separators[i].x+4f,labels[i].x,.001f)
                        if(mode==HrvTrendPeriod.WEEK) assertTrue(labels.all { it.text.matches(Regex(if(compact) "[0-9]{2}" else "KW[0-9]{2}")) })
                        else assertTrue(labels.all { if(compact) it.text.length==1 else it.text.length>=3 })
                        assertEquals(axis,chart.axis)
                    }
                    viewport.zoom(2.0,.5);verifyEveryTick(drawing(),false) // Seven buckets have room for the full labels.
                    viewport.zoom(.25,.5);val before=drawing();verifyEveryTick(before,true)
                    viewport.pan(-.2);val after=drawing();verifyEveryTick(after,true)
                    val beforeLabels=before.labels.filter { it.y==392f }.map { it.text }
                    val afterLabels=after.labels.filter { it.y==392f }.map { it.text }
                    assertEquals(beforeLabels,afterLabels)
                    viewport.zoom(.5,.5);verifyEveryTick(drawing(),true) // Even at smaller zoom, none are culled.
                }
            } finally { image.recycle() }
        } finally { host.pause().stop().destroy();Locale.setDefault(oldLocale) }
    }
    @Test fun periodControlsUpdateAllMetricsAndKeepAnOlderSelectedMonthAcrossMetricChanges() {
        val oldLocale=Locale.getDefault();Locale.setDefault(Locale.GERMAN)
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("period-view",0))
            val rows=(0..420).map { i ->
                val day=LocalDate.of(2024,1,1).plusDays(i.toLong())
                row(day,20.0+(i%10),if(day.dayOfMonth==day.lengthOfMonth()) null else 60.0+i%3)
            }
            val series=HrvTrendMetric.entries.associateWith { HrvTrends.series(rows,it) }
            var opened:String?=null
            val view=HrvTrendsView(a,themes,series) { opened=it };a.setContentView(view);layout(view,1100)
            val chart=view.findViewWithTag<HrvTrendChart>("hrv_trend_chart")
            var periodTitle="";view.onPeriodSelected={periodTitle=it}
            view.findViewWithTag<View>("hrv_period_MONTH").performClick();layout(view,1100)
            assertEquals(View.GONE,view.findViewWithTag<View>("hrv_trend_detail").visibility)
            assertTrue(periodTitle.contains("2025"));assertEquals(HrvTrendAxis.from(HrvTrendAggregation.series(series.getValue(HrvTrendMetric.RMSSD),HrvTrendPeriod.MONTH),HrvTrendMetric.RMSSD),chart.axis)
            val month=LocalDate.of(2024,10,1);val viewport=viewport(chart)
            val x=(40+(HrvTrendPeriod.MONTH.index(month)-(viewport.end-viewport.span))/viewport.span*(chart.width-48)).toFloat()
            val now=android.os.SystemClock.uptimeMillis()
            MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,150f,0).also { chart.onTouchEvent(it);it.recycle() }
            MotionEvent.obtain(now,now+30,MotionEvent.ACTION_UP,x,150f,0).also { chart.onTouchEvent(it);it.recycle() }
            assertEquals("Oktober 2024",periodTitle)
            for(metric in listOf(HrvTrendMetric.RMSSD,HrvTrendMetric.HR,HrvTrendMetric.SDNN)) {
                view.findViewWithTag<View>("hrv_trend_${metric.name}").performClick()
                val detail=view.findViewWithTag<TextView>("hrv_trend_selected").text.toString()
                assertTrue(detail.contains("Oktober 2024"));assertTrue(detail.contains("31.10.2024"))
                val value=HrvTrendAggregation.series(series.getValue(metric),HrvTrendPeriod.MONTH).single { it.day==month }.value!!
                val label=view.findViewWithTag<TextView>("hrv_trend_${metric.name}").text.toString()
                assertTrue(label.endsWith(String.format(Locale.GERMAN,"%.1f",value)))
            }
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat);layout(view,1100)
                val image=Bitmap.createBitmap(360,1100,Bitmap.Config.ARGB_8888)
                val canvas=Canvas(image);canvas.drawColor(themes.active.tokens.secondary);view.draw(canvas)
                java.io.File("build/reports/ui/hrv-period-month-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
            view.findViewWithTag<View>("hrv_period_DAY").performClick()
            assertEquals(View.VISIBLE,view.findViewWithTag<View>("hrv_trend_detail").visibility)
            view.findViewWithTag<View>("hrv_trend_detail").performClick();assertTrue(opened!!.startsWith("2024-10"))
        } finally { host.pause().stop().destroy();Locale.setDefault(oldLocale) }
    }
    @Test fun eachResolutionUsesItsOwnCompleteSeriesAxisAndKeepsItDuringNavigation() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get()
            val rows=(0..420).map { i -> row(LocalDate.of(2024,1,1).plusDays(i.toLong()),if(i==120) 300.0 else 40.0,if(i==120) 150.0 else 60.0) }
            val series=HrvTrendMetric.entries.associateWith { HrvTrends.series(rows,it) }
            val view=HrvTrendsView(a,ThemeManager(a.getSharedPreferences("period-independent-scale",0)),series) {}
            a.setContentView(view);layout(view,1100)
            val chart=view.findViewWithTag<HrvTrendChart>("hrv_trend_chart")
            for(metric in listOf(HrvTrendMetric.RMSSD,HrvTrendMetric.HR)) {
                view.findViewWithTag<View>("hrv_trend_${metric.name}").performClick()
                val daily=HrvTrendAxis.from(series.getValue(metric),metric)
                for(mode in listOf(HrvTrendPeriod.DAY,HrvTrendPeriod.WEEK,HrvTrendPeriod.MONTH,HrvTrendPeriod.DAY)) {
                    view.findViewWithTag<View>("hrv_period_${mode.name}").performClick()
                    val points=HrvTrendAggregation.series(series.getValue(metric),mode)
                    val expected=HrvTrendAxis.from(points,metric)
                    assertEquals(expected,chart.axis)
                    if(mode!=HrvTrendPeriod.DAY) assertTrue(expected.maximum-expected.minimum<daily.maximum-daily.minimum)
                    val values=points.flatMap { listOfNotNull(it.value,it.average,it.lower,it.upper) }
                    assertTrue(values.all { it>=expected.minimum && it<=expected.maximum })
                    chart.performAccessibilityAction(HrvTrendChart.ZOOM_IN,null)
                    viewport(chart).pan(-20.0)
                    chart.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null)
                    assertEquals(expected,chart.axis)
                }
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun runningPeriodReportsElapsedDaysAndUnavailableMetricHasNoInventedValue() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val today=LocalDate.now()
            val rows=listOf(row(today.withDayOfMonth(1),30.0),row(today,50.0)).distinctBy { it.id }
            val series=HrvTrendMetric.entries.associateWith { HrvTrends.series(rows,it) }
            val view=HrvTrendsView(a,ThemeManager(a.getSharedPreferences("running-period",0)),series) {};a.setContentView(view);layout(view,1100)
            view.findViewWithTag<View>("hrv_period_MONTH").performClick()
            val detail=view.findViewWithTag<TextView>("hrv_trend_selected").text.toString()
            assertTrue(detail.contains("bisher"));assertTrue(detail.contains("${rows.size} von ${today.dayOfMonth} Tagen"))
            view.findViewWithTag<View>("hrv_trend_HF").performClick()
            assertEquals(View.VISIBLE,view.findViewWithTag<View>("hrv_trend_empty").visibility)
            assertTrue(view.findViewWithTag<TextView>("hrv_trend_HF").text.toString().endsWith("—"))
            assertTrue(view.findViewWithTag<TextView>("hrv_trend_selected").text.toString().contains("0 von ${today.dayOfMonth} Tagen"))
        } finally { host.pause().stop().destroy() }
    }
}
