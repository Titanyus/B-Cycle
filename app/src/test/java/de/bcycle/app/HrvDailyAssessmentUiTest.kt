package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvDailyAssessmentUiTest {
    @Test fun actualMeasurementAndEvaluationCirclesKeepThePageGapBelowTheHeader() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val controller=RideRuntime.get(a).hrv
            fun drain() {
                val worker=HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as java.util.concurrent.ExecutorService
                worker.submit {}.get(10,java.util.concurrent.TimeUnit.SECONDS)
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            }
            for((method,tag) in listOf("showHrv" to "hrv_circle","showHrvTrends" to "hrva_circle")) {
                MainActivity::class.java.getDeclaredMethod(method).apply { isAccessible=true }.invoke(a)
                repeat(2) { drain() }
                val screen=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
                val decor=screen.window!!.decorView
                repeat(2) {
                    decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
                    decor.layout(0,0,360,800)
                }
                val header=decor.findViewWithTag<View>("bicycle_navigation_header")
                val circle=decor.findViewWithTag<View>(tag)
                val h=IntArray(2);val p=IntArray(2);header.getLocationOnScreen(h);circle.getLocationOnScreen(p)
                val d=a.resources.displayMetrics.density
                val gap=(p[1]-h[1]-header.height)/d
                val radius=minOf(circle.width,circle.height)/2f-(if(tag=="hrv_circle") 12 else 24)*d
                val stroke=BicycleDesign.hrvRingWidth*d*(if(tag=="hrv_circle") 1 else 2)
                val visibleGap=gap+(circle.height/2f-radius-stroke/2)/d
                println("$tag: pageGap=$gap dp; visibleRingGap=$visibleGap dp; circle=${circle.width}x${circle.height}")
                assertEquals(BicycleDesign.hrvPageContentGap.toFloat(),gap,.01f)
                screen.dismiss()
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun twoArcsAndUnboxedAdviceRenderInBothDesignsAndMissingDataStayUnavailable() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("assessment-ui",0));val day=LocalDate.of(2026,10,28)
            val row=HrvHistorySummary("example",0,true,40.0)
            val hrv=HrvTrendPoint(row,day,"RMSSD",40.0,null,30.0,45.0)
            val pulse=hrv.copy(value=52.0,lower=48.0,upper=57.0)
            for(flat in listOf(false,true)) for(width in listOf(320,360)) {
                themes.setFlatDesign(flat)
                val data=HrvDailyAssessment(day,hrv,pulse,2.0,4,HrvAutonomicIndices(-1.4,2.5))
                val content=HrvDailyAssessmentView(a,themes,data)
                val scroll=ScrollView(a).apply { addView(content) };a.setContentView(scroll)
                scroll.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));scroll.layout(0,0,width,900)
                val advice=content.findViewWithTag<TextView>("hrva_advice").text.toString()
                assertTrue(advice.startsWith(a.getString(data.advice.label)+".\n"));assertEquals(1,advice.count { it=='\n' })
                assertNull(content.findViewWithTag<View>("hrva_subjective_period"))
                assertTrue(content.findViewWithTag<View>("hrva_circle").contentDescription.toString().contains("52 bpm"))
                assertNull(content.findViewWithTag<View>("hrva_advice_text").background)
                assertEquals("2,5",content.findViewWithTag<View>("hrva_activation").findViewWithTag<TextView>("hrva_index_value").text.toString().replace('.',','))
                val image=Bitmap.createBitmap(width,900,Bitmap.Config.ARGB_8888);Canvas(image).apply { drawColor(themes.active.tokens.secondary);scroll.draw(this) }
                java.io.File("build/reports/ui/hrv-day-new-$width-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
            val missing=HrvDailyAssessmentView(a,themes,HrvDailyAssessment(day,hrv.copy(value=null,lower=null,upper=null),null,null,0))
            assertTrue(missing.findViewWithTag<View>("hrva_circle").contentDescription.toString().contains(a.getString(R.string.hrva_range_missing)))
            assertTrue(missing.findViewWithTag<TextView>("hrva_advice").text.startsWith(a.getString(R.string.hrva_unavailable)+".\n"))
            assertEquals(a.getString(R.string.hrva_index_missing),missing.findViewWithTag<View>("hrva_pns").findViewWithTag<TextView>("hrva_index_value").text.toString())
        } finally { host.pause().stop().destroy() }
    }
    @Test fun corridorMarkersStayFixedAndOnlyTheDrawingClampsOutliers() {
        assertEquals(.25f,hrvCorridorFraction(30.0,30.0,50.0)!!,0f)
        assertEquals(.75f,hrvCorridorFraction(50.0,30.0,50.0)!!,0f)
        assertEquals(.5f,hrvCorridorFraction(40.0,30.0,50.0)!!,0f)
        assertEquals(1f,hrvCorridorFraction(100.0,30.0,50.0)!!,0f)
        assertNull(hrvCorridorFraction(40.0,null,null));assertNull(hrvCorridorFraction(40.0,40.0,40.0))
    }
    @Test fun missingIndexShowsTheRemainingDaysOrItsActualReasonBesideTheName() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("index-hints",0))
            for(label in listOf("PNS-Index","Aktivierungsindex")) for(width in listOf(288,328)) {
                for((days,expected) in listOf(0 to "noch 14 Tage bis zum ersten Wert",13 to "noch 1 Tag bis zum ersten Wert",
                    14 to a.getString(R.string.hrva_index_constant),null to a.getString(R.string.hrva_index_missing))) {
                    val index=HrvIndexView(a,themes,label,null,days);a.setContentView(index)
                    index.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                    index.layout(0,0,width,index.measuredHeight)
                    assertEquals(expected,index.findViewWithTag<TextView>("hrva_index_value").text.toString())
                    val row=index.getChildAt(0) as android.view.ViewGroup
                    assertEquals(label,(row.getChildAt(0) as TextView).text.toString())
                    assertTrue(row.getChildAt(0).right<=row.getChildAt(1).left)
                    assertTrue(row.getChildAt(1).right<=width)
                }
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun indexDisplayKeepsHalfForNormalAndExtendsOuterBuffersToFourWithoutChangingValues() {
        assertEquals(0f,hrvIndexOffsetFraction(0.0),0f)
        for((value,position) in listOf(1.0 to .25f,2.0 to (1f/3),3.0 to (5f/12),4.0 to .5f,10.0 to .5f)) {
            assertEquals(position,hrvIndexOffsetFraction(value),.000001f)
            assertEquals(-position,hrvIndexOffsetFraction(-value),.000001f)
        }
        assertEquals(.125f,hrvIndexOffsetFraction(.5),0f)
        val c=org.robolectric.RuntimeEnvironment.getApplication()
        val index=HrvIndexView(c,ThemeManager(c.getSharedPreferences("index-range",0)),"PNS-Index",5.5)
        assertEquals("5,5",index.findViewWithTag<TextView>("hrva_index_value").text.toString().replace('.',','))
    }
    @Test fun detailArrivalPinsDayUntilContactThenPushesItAndCanScrollBack() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get()
            val day=View(a).apply { minimumHeight=100 }
            val detail=View(a).apply { minimumHeight=500 }
            val scroll=HrvEvaluationScrollView(a,day,detail);a.setContentView(scroll)
            fun layout() { scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));scroll.layout(0,0,360,600) }
            layout();assertTrue(detail.top>=600)
            scroll.scrollTo(0,200);assertEquals(200f,day.translationY,0f);assertEquals(0f,day.top+day.translationY-scroll.scrollY,0f)
            scroll.scrollTo(0,500);assertEquals(500f,day.translationY,0f)
            assertEquals(24f,detail.top-scroll.scrollY-(day.top+day.translationY-scroll.scrollY+day.height),0f)
            scroll.scrollTo(0,520);assertEquals(-20f,day.top+day.translationY-scroll.scrollY,0f)
            scroll.scrollTo(0,100);assertEquals(0f,day.top+day.translationY-scroll.scrollY,0f)
            scroll.showDetails();layout()
            repeat(60) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(16))
                scroll.computeScroll()
            }
            assertEquals(BicycleDesign.dp(a,BicycleDesign.horizontalPadding),detail.top-scroll.scrollY)
        } finally { host.pause().stop().destroy() }
    }
}
