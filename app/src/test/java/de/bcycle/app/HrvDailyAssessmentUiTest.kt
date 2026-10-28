package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
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
    private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    @Test fun rendersOurCardsAndValuesInBothDesignsWithoutTrendRows() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("assessment-ui",0));val day=LocalDate.of(2026,10,28)
            val row=HrvHistorySummary("example",0,true,40.0)
            val hrv=HrvTrendPoint(row,day,"RMSSD",40.0,null,30.0,45.0)
            val pulse=hrv.copy(value=52.0,lower=48.0,upper=51.0)
            for(flat in listOf(false,true)) for(width in listOf(320,360)) {
                themes.setFlatDesign(flat)
                for((name,raw,burden) in listOf(Triple("plan",50.0,2.0),Triple("limit",25.0,2.0),Triple("easy",25.0,8.0))) {
                    val data=HrvDailyAssessment(day,hrv.copy(value=raw),pulse,burden,4)
                    val content=HrvDailyAssessmentView(a,themes,data)
                    val scroll=ScrollView(a).apply { addView(content) };a.setContentView(scroll)
                    BicycleDialogStyle.content(content,themes)
                    assertEquals(BicycleDesign.hrvAssessmentScoreSp,content.findViewWithTag<TextView>("hrva_score").textSize/a.resources.displayMetrics.scaledDensity,.01f)
                    scroll.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));scroll.layout(0,0,width,900)
                    assertEquals(data.advice.color,content.findViewWithTag<TextView>("hrva_score").currentTextColor)
                    assertEquals(a.getString(data.advice.label),content.findViewWithTag<TextView>("hrva_advice").text.toString())
                    assertEquals("52 bpm",content.findViewWithTag<TextView>("hrva_pulse").text.toString())
                    assertTrue(content.findViewWithTag<TextView>("hrva_subjective_period").text.contains("4 von 4"))
                    val texts=all(content).filterIsInstance<TextView>().map { it.text.toString() }
                    assertTrue(texts.none { it.contains("Trend") || it.contains("Baseline") })
                    assertTrue(content.findViewWithTag<View>("hrva_hrv_gauge").contentDescription.toString().contains(a.getString(R.string.hrva_range,""," ").substringBefore(":")))
                    if(width==360) {
                        val image=Bitmap.createBitmap(width,900,Bitmap.Config.ARGB_8888);Canvas(image).apply { drawColor(themes.active.tokens.secondary);scroll.draw(this) }
                        java.io.File("build/reports/ui/hrv-daily-$name-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
                    }
                }
            }
            val missing=HrvDailyAssessmentView(a,themes,HrvDailyAssessment(day,hrv.copy(value=null,lower=null,upper=null),null,null,0))
            assertEquals(a.getString(R.string.hrvt_unavailable),missing.findViewWithTag<TextView>("hrva_score").text.toString())
            assertEquals(a.getString(R.string.hrva_unavailable),missing.findViewWithTag<TextView>("hrva_advice").text.toString())
        } finally { host.pause().stop().destroy() }
    }
}
