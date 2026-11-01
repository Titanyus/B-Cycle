package de.bcycle.app

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.RadioGroup
import android.widget.ScrollView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HrvMetricChoicesTest {
    @Test fun primaryScoreAndThreeExtraTilesShareSelectionAndKeepMoreHighlightedWhenCollapsed() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("hrv-metrics",0))
            val rows=(0..30).map { i ->
                val date=LocalDate.of(2026,10,1).plusDays(i.toLong())
                HrvHistorySummary("d$i",date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),true,30.0+i%9,
                    metrics=mapOf("restingHrBpm" to 60.0+i%3,"sdnnMs" to 20.0+i%5,"pnn50Percent" to 10.0+i%4,"lfMs2" to 100.0+i,"hfMs2" to 50.0+i))
            }
            val series=HrvTrendMetric.entries.associateWith { HrvTrends.series(rows,it) }
            for(flat in listOf(false,true)) for(width in listOf(320,360)) {
                themes.setFlatDesign(flat)
                val view=HrvTrendsView(a,themes,series) {}
                val scroll=ScrollView(a).apply { addView(view) };a.setContentView(scroll)
                fun layout() { scroll.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));scroll.layout(0,0,width,1000) }
                layout()
                val primary=view.findViewWithTag<RadioGroup>("hrv_trend_metrics")
                val extra=view.findViewWithTag<RadioGroup>("hrv_trend_advanced")
                val more=view.findViewWithTag<BicycleSegmentButton>("hrv_trend_more")
                assertEquals(5,primary.childCount);assertEquals(3,extra.childCount)
                assertEquals(listOf("hrv_trend_HRV","hrv_trend_HR","hrv_trend_SDNN","hrv_trend_RMSSD","hrv_trend_more"),(0 until primary.childCount).map { primary.getChildAt(it).tag })
                assertEquals(View.GONE,extra.visibility);assertTrue((primary.getChildAt(0) as BicycleSegmentButton).isChecked)
                val chart=view.findViewWithTag<HrvTrendChart>("hrv_trend_chart")
                assertEquals(HrvTrendAxis.from(series.getValue(HrvTrendMetric.HRV),HrvTrendMetric.HRV),chart.axis)
                for(i in 0 until primary.childCount) assertTrue(primary.getChildAt(i).width>=48)
                assertEquals("More label must fit",1,more.lineCount)
                more.performClick();layout()
                assertEquals(View.VISIBLE,extra.visibility);assertFalse(more.isChecked)
                assertTrue((primary.getChildAt(0) as BicycleSegmentButton).isChecked)
                for(metric in listOf(HrvTrendMetric.PNN50,HrvTrendMetric.LF,HrvTrendMetric.HF)) {
                    val choice=view.findViewWithTag<BicycleSegmentButton>("hrv_trend_${metric.name}")
                    choice.performClick();assertTrue(choice.isChecked);assertTrue(more.isChecked)
                    assertFalse((primary.getChildAt(0) as BicycleSegmentButton).isChecked)
                    assertEquals(HrvTrendAxis.from(series.getValue(metric),metric),chart.axis)
                }
                more.performClick();assertEquals(View.GONE,extra.visibility);assertTrue(more.isChecked)
                view.findViewWithTag<View>("hrv_trend_HRV").performClick();assertFalse(more.isChecked)
                assertTrue((primary.getChildAt(0) as BicycleSegmentButton).isChecked)
                more.performClick();layout()
                val image=Bitmap.createBitmap(width,1000,Bitmap.Config.ARGB_8888)
                try {
                    image.eraseColor(themes.active.tokens.secondary);scroll.draw(Canvas(image))
                    java.io.File("build/reports/ui/hrv-metric-choices-$width-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
                } finally { image.recycle() }
            }
        } finally { host.pause().stop().destroy() }
    }
}
