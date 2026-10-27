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
    @Test fun yearsWeeklyScrollingAndBikeProfileColorsWorkInBothDesigns() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val c=host.get();val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val data=RideStatistics(2026,mapOf(2025 to List(12) { (it+1)*75.0 },2026 to List(12) { if(it<10) (it+1)*100.0 else 0.0 }),
                List(53) { if(it<43) (it%8)*35.0 else 0.0 },listOf(BikeTimeShare("one","Conway",3600000.0,Color.RED),BikeTimeShare("two","Matrix",10800000.0,Color.BLUE)),20)
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                val content=RideStatisticsView(c,themes)
                val scroll=ScrollView(c).apply { addView(content) };c.setContentView(scroll)
                content.show(data);layout(scroll);shadowOf(android.os.Looper.getMainLooper()).idle()
                val current=content.findViewWithTag<BicycleSegmentButton>("statistics_year_2026")
                val previous=content.findViewWithTag<BicycleSegmentButton>("statistics_year_2025")
                assertTrue(current.isChecked);assertFalse(previous.isChecked)
                previous.performClick();assertTrue(previous.isChecked);assertTrue(current.isChecked)
                current.performClick();assertFalse(current.isChecked);assertTrue(previous.isChecked)
                val weekly=content.findViewWithTag<HorizontalScrollView>("statistics_weeks_scroll")
                assertTrue(weekly.getChildAt(0).width>weekly.width*4)
                weekly.scrollTo(1000,0);assertEquals(1000,weekly.scrollX)
                val pie=content.findViewWithTag<View>("statistics_bike_pie")
                val image=Bitmap.createBitmap(pie.width,pie.height,Bitmap.Config.ARGB_8888);pie.draw(Canvas(image))
                assertEquals(Color.RED,image.getPixel(pie.width/2+60,pie.height/2-60))
                assertEquals(Color.BLUE,image.getPixel(pie.width/2-60,pie.height/2+60));image.recycle()
                val labels=all(content).filterIsInstance<TextView>().map { it.text.toString() }
                assertTrue(labels.any { it.contains("Conway") && it.contains("25") && it.contains("1 h") })
                assertTrue(labels.any { it.contains("Matrix") && it.contains("75") && it.contains("3 h") })
                val count=content.childCount;content.show(data);assertEquals(count,content.childCount);assertFalse(current.isChecked)
                if(!flat) {
                    current.performClick();layout(scroll)
                    val preview=Bitmap.createBitmap(content.width,content.height,Bitmap.Config.ARGB_8888)
                    content.draw(Canvas(preview))
                    java.io.File("build/reports/ui/statistics.png").apply { parentFile!!.mkdirs() }.outputStream().use { preview.compress(Bitmap.CompressFormat.PNG,100,it) };preview.recycle()
                }
            }
        } finally { host.pause().stop().destroy() }
    }
}
