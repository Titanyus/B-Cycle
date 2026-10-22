package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ScrollView
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
import java.io.File
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvTrendsTest {
    private val start=LocalDate.of(2024,1,1)
    private val context get()=RuntimeEnvironment.getApplication()
    private fun row(day:Int,value:Double=40.0,valid:Boolean=true)=HrvHistorySummary("day$day",start.plusDays(day.toLong()).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
        valid,value,zoneId="UTC",metrics=mapOf("rmssdMs" to value,"restingHrBpm" to 60.0),restMs=30000,measureMs=180000)
    private fun drain(controller:HrvController) {
        (HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as ExecutorService).submit {}.get(10,TimeUnit.SECONDS)
        shadowOf(Looper.getMainLooper()).idle()
    }
    @Test fun calendarWindowsExcludeTodayFromNormalRangeAndRequireEnoughValues() {
        val rows=(0..80).map { row(it,if(it==80) 10000.0 else it.toDouble()) }
        val points=HrvTrends.series(rows,HrvTrendMetric.RMSSD);val last=points.last()
        assertEquals((74.0+75+76+77+78+79+10000)/7,last.average!!,1e-9)
        assertEquals(49.5-sqrt(305.0),last.lower!!,1e-9)
        assertEquals(49.5+sqrt(305.0),last.upper!!,1e-9)
        assertNull(points[3].average);assertEquals(2.0,points[4].average!!,0.0)
        assertNull(points[13].lower);assertNotNull(points[14].lower)
        val sparse=HrvTrends.series(listOf(0,1,2,3,20).map { row(it) },HrvTrendMetric.RMSSD)
        assertNull(sparse.last().average)
    }
    @Test fun latestRejectedReplacesEarlierValidAndSourcesAndProtocolsNeverShareReferenceValues() {
        val rows=(0..20).map { row(it) }
        val later=rows.last().copy(id="late",utcMs=rows.last().utcMs+1000,acceptable=false)
        val selected=HrvTrends.series(rows+later,HrvTrendMetric.RMSSD)
        assertEquals(21,selected.size);assertEquals("late",selected.last().row.id)
        assertNull(selected.last().value);assertNull(selected.last().average)
        val otherProtocol=HrvTrends.series(rows+row(21,500.0).copy(measureMs=60000),HrvTrendMetric.RMSSD)
        assertNull(otherProtocol.last().average);assertNull(otherProtocol.last().lower)
        val imported=row(21,500.0).copy(importedDay=start.plusDays(21).toString(),importedTime="00:00",acceptable=false)
        val otherSource=HrvTrends.series(rows+imported,HrvTrendMetric.RMSSD)
        assertEquals(500.0,otherSource.last().value!!,0.0);assertNull(otherSource.last().lower)
    }
    @Test fun streamingUsesCorrectedScalarsAndDoesNotInventImportedFrequencyUnits() {
        val root=File(context.cacheDir,"trend-stream-${UUID.randomUUID()}")
        try {
            val id=UUID.randomUUID().toString();File(root,id).mkdirs()
            val native=JSONObject().put("format","bicycle-hrv").put("schema",1).put("id",id).put("utcMs",0).put("zone","UTC")
                .put("acceptable",true).put("restMs",30000).put("measureMs",180000)
                .put("raw",JSONObject().put("rmssdMs",999))
                .put("corrected",JSONObject().put("rmssdMs",42).put("sdnnMs",35).put("restingHrBpm",60).put("pnn50Percent",7))
                .put("frequency",JSONObject().put("lfMs2",120).put("hfMs2",240))
            val file=File(root,"$id/result.json").apply { writeText(native.toString()) };val bytes=file.readBytes()
            val summary=HrvStore(root).list().single()
            assertEquals(42.0,summary.metrics["rmssdMs"]!!,0.0);assertEquals(120.0,summary.metrics["lfMs2"]!!,0.0)
            assertEquals(180000L,summary.measureMs);assertArrayEquals(bytes,file.readBytes())
            val csv=Hrv4TrainingCsv.preview("date,time,rMSSD,LF,HF\n2024-16-01,07:00,40,100,200\n".byteInputStream(),java.time.ZoneOffset.UTC)
            Hrv4TrainingCsv.import(csv,HrvStore(root))
            val imported=HrvStore(root).list().first { it.importedDay!=null }
            assertEquals(40.0,imported.metrics["rmssdMs"]!!,0.0)
            assertNull(HrvTrends.series(listOf(imported),HrvTrendMetric.LF).single().value)
            assertEquals("100",HrvStore(root).read(imported.id).getJSONObject("external").getJSONObject("originalColumns").getString("LF"))
        } finally { root.deleteRecursively() }
    }
    @Test fun viewportZoomAndPanStayWithinDataAndKeepStatisticsIndependent() {
        val rows=(0..90).map { row(it) };val points=HrvTrends.series(rows,HrvTrendMetric.RMSSD)
        val view=HrvTrendViewport();view.reset(points.map { it.day })
        assertEquals(14.0,view.span,0.0);view.zoom(2.0,.5);assertEquals(7.0,view.span,0.0)
        view.pan(-10000.0);assertTrue(view.contains(start));view.pan(10000.0);assertTrue(view.contains(start.plusDays(90)))
        view.zoom(.001,.5);assertEquals(91.0,view.span,0.0)
        assertTrue(view.contains(start));assertTrue(view.contains(start.plusDays(90)))
        assertEquals(points,HrvTrends.series(rows,HrvTrendMetric.RMSSD))
    }
    @Test fun fixedAxisFitsWholeSeriesAndReferenceBandAndOnlyBelowRangeIsMarked() {
        val point=HrvTrendPoint(row(0),start,"native",60.0,62.0,58.0,65.0)
        assertEquals(HrvTrendAxis(53.0,70.0),HrvTrendAxis.from(listOf(point),HrvTrendMetric.HR))
        val rmssd=HrvTrendAxis.from(listOf(point),HrvTrendMetric.RMSSD)
        assertEquals(58*.95,rmssd.minimum,1e-9);assertEquals(65*1.05,rmssd.maximum,1e-9)
        assertFalse(point.belowNormal);assertTrue(point.copy(value=57.0).belowNormal)
        assertFalse(point.copy(value=58.0).belowNormal);assertFalse(point.copy(lower=null).belowNormal)
        val whole=HrvTrendAxis.from(listOf(point,point.copy(value=100.0)),HrvTrendMetric.HR)
        assertEquals(105.0,whole.maximum,0.0)
        assertEquals(HrvTrendAxis(0.0,1.0),HrvTrendAxis.from(emptyList(),HrvTrendMetric.HR))
        val constant=HrvTrendAxis.from(listOf(point.copy(value=0.0,average=null,lower=null,upper=null)),HrvTrendMetric.RMSSD)
        assertEquals(0.0,constant.minimum,0.0);assertTrue(constant.maximum>constant.minimum)
    }
    @Test fun metricSelectionZoomAndDetailsRenderNativelyAndSettingsOpenAnalysis() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            val rows=(0..75).map { row(it,35.0+it%12) }
            val series=HrvTrendMetric.entries.associateWith { HrvTrends.series(rows,it) }
            var opened:String?=null
            val view=HrvTrendsView(a,themes,series) { opened=it }
            val scroll=ScrollView(a).apply { setBackgroundColor(themes.active.tokens.secondary);addView(view) };a.setContentView(scroll)
            repeat(2) { scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));scroll.layout(0,0,360,1000);shadowOf(Looper.getMainLooper()).idle() }
            val chart=view.findViewWithTag<HrvTrendChart>("hrv_trend_chart")
            val label=view.findViewWithTag<android.widget.TextView>("hrv_trend_selected")
            assertTrue(label.text.contains("RMSSD"));view.findViewWithTag<View>("hrv_trend_HR").performClick();assertTrue(label.text.contains("Puls"))
            val axisWidth=BicycleDesign.hrvTrendAxisWidth.toFloat()
            val tapX=axisWidth+(chart.width-axisWidth-8f)*11.5f/14f
            MotionEvent.obtain(2000,2000,MotionEvent.ACTION_DOWN,tapX,150f,0).also { chart.onTouchEvent(it);it.recycle() }
            MotionEvent.obtain(2000,2020,MotionEvent.ACTION_UP,tapX,150f,0).also { chart.onTouchEvent(it);it.recycle() }
            assertTrue(label.text.contains(start.plusDays(73).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))))
            assertTrue(chart.performAccessibilityAction(HrvTrendChart.ZOOM_IN,null))
            chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null)
            assertNotNull(chart.createAccessibilityNodeInfo().actionList.firstOrNull { it.id==HrvTrendChart.ZOOM_OUT })
            view.findViewWithTag<View>("hrv_trend_detail").performClick();assertEquals("day73",opened)
            view.findViewWithTag<View>("hrv_trend_RMSSD").performClick()
            chart.show(series.getValue(HrvTrendMetric.RMSSD),reset=true)
            val image=Bitmap.createBitmap(360,1000,Bitmap.Config.ARGB_8888);scroll.draw(Canvas(image))
            File("build/reports/ui/hrv-trends.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            var requested=false
            val settings=HrvSettingsView(a,themes,openAnalysis={requested=true})
            settings.findViewWithTag<View>("hrv_open_analysis").performClick();assertTrue(requested)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun realSettingsNavigationOpensEmptyAnalysisAndBackPreservesSettings() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=host.get()
            MainActivity::class.java.getDeclaredMethod("showMenu",Boolean::class.javaPrimitiveType).apply { isAccessible=true }.invoke(a,false)
            org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().window!!.decorView.findViewWithTag<View>("settings_HRV").performClick()
            shadowOf(Looper.getMainLooper()).idle()
            val settings=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            settings.window!!.decorView.findViewWithTag<View>("hrv_open_analysis").performClick()
            repeat(3) { drain(RideRuntime.get(a).hrv) }
            val dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            val root=dialog.window!!.decorView
            assertNotNull(root.findViewWithTag<View>("hrv_trends"));assertNotNull(root.findViewWithTag<View>("hrv_trend_empty"))
            assertEquals(SettingsHelpPage.HRV_TRENDS,SettingsHelp.resolve(dialog))
            assertFalse(root.findViewWithTag<View>("hrv_trend_detail").isEnabled)
            dialog.dismiss();shadowOf(Looper.getMainLooper()).idle()
            assertTrue(settings.isShowing);assertNotNull(settings.window!!.decorView.findViewWithTag<View>("hrv_open_analysis"))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun nativePinchAndHorizontalDragChangeOnlyTheDisplayedPeriod() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("bike",0))
            var first=start;var last=start;var chosen:String?=null
            val points=HrvTrends.series((0..80).map { row(it) },HrvTrendMetric.RMSSD)
            val chart=HrvTrendChart(a,themes,{ chosen=it.row.id }) { f,l -> first=f;last=l }
            a.setContentView(chart);chart.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));chart.layout(0,0,360,400)
            chart.show(points,true);val before=last.toEpochDay()-first.toEpochDay();val fixedAxis=chart.axis
            fun event(time:Long,action:Int,vararg xs:Float) {
                val props=Array(xs.size) { i -> MotionEvent.PointerProperties().apply { id=i;toolType=MotionEvent.TOOL_TYPE_FINGER } }
                val coords=Array(xs.size) { i -> MotionEvent.PointerCoords().apply { x=xs[i];y=150f;pressure=1f;size=1f } }
                MotionEvent.obtain(0,time,action,xs.size,props,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0).also { chart.onTouchEvent(it);it.recycle() }
            }
            event(0,MotionEvent.ACTION_DOWN,90f)
            event(10,MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),90f,270f)
            event(30,MotionEvent.ACTION_MOVE,60f,300f);event(50,MotionEvent.ACTION_MOVE,30f,330f)
            event(70,MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),30f,330f)
            event(80,MotionEvent.ACTION_UP,30f)
            assertTrue(last.toEpochDay()-first.toEpochDay()<before)
            val zoomedFirst=first
            event(1000,MotionEvent.ACTION_DOWN,100f);event(1020,MotionEvent.ACTION_MOVE,220f);event(1040,MotionEvent.ACTION_UP,220f)
            assertTrue(first<zoomedFirst);assertEquals("day80",chosen);assertEquals(fixedAxis,chart.axis)
        } finally { host.pause().stop().destroy() }
    }
}
