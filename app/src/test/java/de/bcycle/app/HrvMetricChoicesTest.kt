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
                val more=view.findViewWithTag<BicycleSegmentButton>("hrv_trend_more")
                assertEquals(5,primary.childCount);assertNull(view.metricPopup)
                assertEquals(listOf("hrv_trend_HRV","hrv_trend_HR","hrv_trend_SDNN","hrv_trend_RMSSD","hrv_trend_more"),(0 until primary.childCount).map { primary.getChildAt(it).tag })
                assertTrue((primary.getChildAt(0) as BicycleSegmentButton).isChecked)
                val chart=view.findViewWithTag<HrvTrendChart>("hrv_trend_chart")
                assertEquals(HrvTrendAxis.from(series.getValue(HrvTrendMetric.HRV),HrvTrendMetric.HRV),chart.axis)
                for(i in 0 until primary.childCount) {
                    val tile=primary.getChildAt(i) as BicycleSegmentButton
                    assertTrue(tile.width>=48)
                    assertEquals("full tile height: ${tile.tag}",primary.paddingTop,tile.top)
                    assertEquals("full highlight height: ${tile.tag}",primary.height-primary.paddingBottom,tile.bottom)
                    val textCenter=tile.top+tile.baseline-tile.layout.getLineBaseline(0)+tile.layout.height/2f
                    assertEquals("centred text: ${tile.tag}",primary.height/2f,textCenter,1f)
                }
                assertEquals("More label must fit",1,more.lineCount)
                val chartTop=chart.top
                more.performClick();layout()
                val popup=checkNotNull(view.metricPopup)
                val extra=popup.contentView as RadioGroup
                extra.measure(View.MeasureSpec.makeMeasureSpec(popup.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                extra.layout(0,0,popup.width,extra.measuredHeight)
                assertTrue(popup.isShowing);assertEquals(3,extra.childCount);assertFalse(more.isChecked)
                assertEquals(chartTop,chart.top)
                assertEquals((2..4).sumOf { primary.getChildAt(it).width }+extra.paddingLeft+extra.paddingRight,popup.width)
                for(i in 0 until extra.childCount) assertEquals(primary.getChildAt(i+2).width,extra.getChildAt(i).width)
                assertTrue((primary.getChildAt(0) as BicycleSegmentButton).isChecked)
                for(metric in listOf(HrvTrendMetric.PNN50,HrvTrendMetric.LF,HrvTrendMetric.HF)) {
                    if(view.metricPopup==null) more.performClick()
                    val choice=view.metricPopup!!.contentView.findViewWithTag<BicycleSegmentButton>("hrv_trend_${metric.name}")
                    choice.performClick();assertTrue(choice.isChecked);assertTrue(more.isChecked);assertNull(view.metricPopup)
                    assertFalse((primary.getChildAt(0) as BicycleSegmentButton).isChecked)
                    assertEquals(HrvTrendAxis.from(series.getValue(metric),metric),chart.axis)
                }
                more.performClick();assertTrue(more.isChecked)
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
