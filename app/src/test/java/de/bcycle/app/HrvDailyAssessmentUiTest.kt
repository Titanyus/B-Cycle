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
                assertEquals(a.getString(data.advice.label),content.findViewWithTag<TextView>("hrva_advice").text.toString())
                assertTrue(content.findViewWithTag<View>("hrva_circle").contentDescription.toString().contains("52 bpm"))
                assertNull(content.findViewWithTag<View>("hrva_advice_text").background)
                assertEquals("2,5",content.findViewWithTag<View>("hrva_activation").findViewWithTag<TextView>("hrva_index_value").text.toString().replace('.',','))
                val image=Bitmap.createBitmap(width,900,Bitmap.Config.ARGB_8888);Canvas(image).apply { drawColor(themes.active.tokens.secondary);scroll.draw(this) }
                java.io.File("build/reports/ui/hrv-day-new-$width-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
            val missing=HrvDailyAssessmentView(a,themes,HrvDailyAssessment(day,hrv.copy(value=null,lower=null,upper=null),null,null,0))
            assertTrue(missing.findViewWithTag<View>("hrva_circle").contentDescription.toString().contains(a.getString(R.string.hrva_range_missing)))
            assertEquals(a.getString(R.string.hrva_unavailable),missing.findViewWithTag<TextView>("hrva_advice").text.toString())
            assertEquals(a.getString(R.string.hrvt_unavailable),missing.findViewWithTag<View>("hrva_pns").findViewWithTag<TextView>("hrva_index_value").text.toString())
        } finally { host.pause().stop().destroy() }
    }
    @Test fun corridorMarkersStayFixedAndOnlyTheDrawingClampsOutliers() {
        assertEquals(.25f,hrvCorridorFraction(30.0,30.0,50.0)!!,0f)
        assertEquals(.75f,hrvCorridorFraction(50.0,30.0,50.0)!!,0f)
        assertEquals(.5f,hrvCorridorFraction(40.0,30.0,50.0)!!,0f)
        assertEquals(1f,hrvCorridorFraction(100.0,30.0,50.0)!!,0f)
        assertNull(hrvCorridorFraction(40.0,null,null));assertNull(hrvCorridorFraction(40.0,40.0,40.0))
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
