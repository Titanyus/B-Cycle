package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RideStatisticsViewTest {
    private fun all(view:View):List<View> = listOf(view)+if(view is ViewGroup) (0 until view.childCount).flatMap { all(view.getChildAt(it)) } else emptyList()
    private fun layout(view:View) {
        repeat(2) {
            view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY))
            view.layout(0,0,1080,1920)
        }
    }
    private fun data()=RideStatistics(2026,mapOf(2025 to List(12) { (it+1)*75.0 },2026 to List(12) { if(it<10) (it+1)*100.0 else 0.0 }),
        mapOf(2025 to List(52) { (it%7)*20.0 },2026 to List(53) { if(it<43) (it%8)*35.0 else 0.0 }),
        mapOf(2025 to listOf(BikeTimeShare("one","Conway",7200000.0,Color.RED)),
            2026 to listOf(BikeTimeShare("one","Conway",3600000.0,Color.RED),BikeTimeShare("two","Matrix",10800000.0,Color.BLUE))),20)
    @Test fun sharedYearsUpdateOneSwitchableCurveAndBikeSharesInBothDesigns() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0));val data=data()
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                val content=RideStatisticsView(c,themes);val scroll=ScrollView(c).apply { addView(content) };c.setContentView(scroll)
                content.show(data);layout(scroll);shadowOf(android.os.Looper.getMainLooper()).idle()
                val current=content.findViewWithTag<BicycleSegmentButton>("statistics_year_2026")
                val previous=content.findViewWithTag<BicycleSegmentButton>("statistics_year_2025")
                assertTrue(current.isChecked);assertFalse(previous.isChecked)
                val chart=content.findViewWithTag<StatisticsDistanceChart>("statistics_distance_chart")
                assertEquals(1,all(content).filterIsInstance<StatisticsDistanceChart>().size)
                assertEquals(content.width-content.paddingLeft-content.paddingRight,chart.width)
                assertNotNull(current.compoundDrawablesRelative[0]);assertTrue(current.compoundDrawablesRelative[0] is StatisticsYearDot)
                val dot=current.compoundDrawablesRelative[0]
                fun dotColor():Int {
                    val image=Bitmap.createBitmap(dot.bounds.width(),dot.bounds.height(),Bitmap.Config.ARGB_8888);dot.draw(Canvas(image))
                    val color=image.getPixel(image.width/2,image.height/2);image.recycle();return color
                }
                val color=dotColor()
                previous.performClick();assertTrue(previous.isChecked);assertTrue(current.isChecked)
                assertTrue(chart.contentDescription.toString().contains("2025"));assertTrue(chart.contentDescription.toString().contains("2026"))
                content.findViewWithTag<View>("statistics_period_week").performClick();layout(scroll)
                assertTrue(chart.performAccessibilityAction(StatisticsDistanceChart.ZOOM_IN,null));val zoom=chart.viewport.span
                assertTrue(chart.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null));val end=chart.viewport.end
                content.findViewWithTag<View>("statistics_period_month").performClick()
                content.findViewWithTag<View>("statistics_period_week").performClick()
                assertEquals(zoom,chart.viewport.span,0.0);assertEquals(end,chart.viewport.end,0.0)
                current.performClick();assertFalse(current.isChecked);assertEquals(color,dotColor())
                assertFalse(chart.contentDescription.toString().contains("2026"));assertTrue(chart.contentDescription.toString().contains("2025"))
                layout(scroll)
                val pie=content.findViewWithTag<View>("statistics_bike_pie")
                val image=Bitmap.createBitmap(pie.width,pie.height,Bitmap.Config.ARGB_8888);pie.draw(Canvas(image))
                assertEquals(Color.RED,image.getPixel(pie.width/2,pie.height/2));image.recycle()
                val labels=all(content).filterIsInstance<TextView>().map { it.text.toString() }
                assertTrue(labels.any { it.contains("Conway") && it.contains("100") && it.contains("2 h") });assertFalse(labels.any { it.contains("Matrix") })
                val count=content.childCount;content.show(data);assertEquals(count,content.childCount);assertFalse(current.isChecked)
                previous.performClick();assertEquals(View.GONE,chart.visibility);assertNull(content.findViewWithTag<View>("statistics_bike_pie"))
                content.show(data.copy(rideCount=21));assertEquals(View.GONE,chart.visibility)
                content.findViewWithTag<BicycleSegmentButton>("statistics_year_2026").performClick()
                content.findViewWithTag<BicycleSegmentButton>("statistics_year_2025").performClick()
                content.findViewWithTag<View>("statistics_period_month").performClick();layout(scroll)
                if(!flat) {
                    val preview=Bitmap.createBitmap(content.width,content.height,Bitmap.Config.ARGB_8888);content.draw(Canvas(preview))
                    java.io.File("build/reports/ui/statistics.png").apply { parentFile!!.mkdirs() }.outputStream().use { preview.compress(Bitmap.CompressFormat.PNG,100,it) };preview.recycle()
                    content.findViewWithTag<View>("statistics_period_week").performClick()
                    chart.performAccessibilityAction(StatisticsDistanceChart.ZOOM_OUT,null)
                    chart.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null)
                    layout(scroll)
                    val weekly=Bitmap.createBitmap(content.width,content.height,Bitmap.Config.ARGB_8888);content.draw(Canvas(weekly))
                    java.io.File("build/reports/ui/statistics-week.png").apply { parentFile!!.mkdirs() }.outputStream().use { weekly.compress(Bitmap.CompressFormat.PNG,100,it) };weekly.recycle()
                }
            }
        } finally { host.pause().stop().destroy() }
    }
}
