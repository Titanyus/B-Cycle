package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
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
class HrvContextUiTest {
    @Test fun sixIconsStayInOneRowAndColorSwitchingKeepsValuesAxisAndDayActions() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("context-ui",0));val start=LocalDate.of(2026,10,1)
            val rows=(0..30).map { i -> val day=start.plusDays(i.toLong());HrvHistorySummary("day$i",day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),true,40.0,zoneId=ZoneId.systemDefault().id) }
            val series=HrvTrendMetric.entries.associateWith { HrvTrends.series(rows,it) }
            val answers=(0..30).associate { i -> val day=start.plusDays(i.toLong());day to HrvQuestionnaire(day.toString(),ratings=if(i==30) emptyMap() else mapOf("sleep" to 10-(i%5)*2)) }
            var result:String?=null
            val view=HrvTrendsView(a,themes,series,result={result=it},dayContext=HrvContextColors.daily(answers)) {}
            a.setContentView(view)
            fun layout(width:Int) { view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1100,View.MeasureSpec.EXACTLY));view.layout(0,0,width,1100) }
            for(flat in listOf(false,true)) for(width in listOf(320,360)) {
                themes.setFlatDesign(flat);layout(width)
                val choices=view.findViewWithTag<RadioGroup>("hrv_context_choices");assertEquals(6,choices.childCount)
                val first=choices.getChildAt(0)
                for(i in 0..5) { val child=choices.getChildAt(i) as BicycleSegmentButton;assertEquals(first.top,child.top);assertEquals(first.height,child.height);assertTrue(child.text.isEmpty());assertTrue(child.right<=choices.width) }
                assertTrue(choices.top>=view.findViewWithTag<View>("hrv_trend_range").bottom)
                assertNull(view.findViewWithTag<View>("hrv_trend_selected"));assertNull(view.findViewWithTag<View>("hrv_trend_statistics"));assertNull(view.findViewWithTag<View>("hrv_trend_detail"))
                val chart=view.findViewWithTag<HrvTrendChart>("hrv_trend_chart");assertEquals(454,chart.height)
                val axis=chart.axis
                val legend=view.findViewWithTag<TextView>("hrv_context_name")
                for(mode in HrvContextMode.entries) {
                    view.findViewWithTag<View>("hrv_context_${mode.name}").performClick();layout(width)
                    val expected=HrvContextColors.legend(mode)
                    if(expected==null) assertEquals(a.getString(mode.label),legend.text.toString())
                    else {
                        val spans=(legend.text as android.text.Spanned).getSpans(0,legend.text.length,HrvLegendSquare::class.java)
                        assertEquals(expected.colors,spans.map { it.color })
                        assertTrue(legend.text.startsWith(a.getString(mode.label)+" · "+a.getString(expected.low)))
                        assertTrue(legend.text.endsWith(a.getString(expected.high)))
                        assertTrue("legend clipped at $width: ${legend.text}",legend.layout.getEllipsisCount(legend.lineCount-1)==0)
                    }
                    assertEquals(axis,chart.axis)
                }
                view.findViewWithTag<View>("hrv_context_SLEEP").performClick()
                assertTrue(view.findViewWithTag<TextView>("hrv_context_name").text.startsWith(a.getString(R.string.hrvc_sleep)+" · "));assertEquals(axis,chart.axis)
                val image=Bitmap.createBitmap(chart.width,chart.height,Bitmap.Config.ARGB_8888);val colors=mutableListOf<Int>()
                chart.draw(object:Canvas(image) { override fun drawRect(l:Float,t:Float,r:Float,b:Float,p:Paint) { colors+=p.color;super.drawRect(l,t,r,b,p) } })
                val grey=TrainingZoneColors.grey
                val selectedGrey=Color.rgb(Color.red(grey)/2,Color.green(grey)/2,Color.blue(grey)/2)
                assertTrue("Missing answer is grey (selected bar dimmed): $colors",colors.contains(grey) || colors.contains(selectedGrey));assertTrue("All five zones are represented: $colors",colors.containsAll(TrainingZoneColors.powerValues.drop(2)));image.recycle()
                if(width==360 && !flat) {
                    val preview=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);Canvas(preview).apply { drawColor(themes.active.tokens.secondary);view.draw(this) }
                    java.io.File("build/reports/ui/hrv-context.png").apply { parentFile!!.mkdirs() }.outputStream().use { preview.compress(Bitmap.CompressFormat.PNG,100,it) };preview.recycle()
                }
                view.findViewWithTag<View>("hrv_context_STANDARD").performClick();assertEquals(axis,chart.axis)
            }
            val chart=view.findViewWithTag<HrvTrendChart>("hrv_trend_chart");val x=40+(chart.width-48)*13.5f/14
            val now=android.os.SystemClock.uptimeMillis()
            android.view.MotionEvent.obtain(now,now,0,x,150f,0).also { chart.onTouchEvent(it);it.recycle() }
            android.view.MotionEvent.obtain(now,now+20,1,x,150f,0).also { chart.onTouchEvent(it);it.recycle() }
            view.findViewWithTag<View>("hrv_day_result").performClick();assertEquals("day30",result)
            assertEquals(3,(view.findViewWithTag<View>("hrv_day_result").parent as android.view.ViewGroup).childCount)
        } finally { host.pause().stop().destroy() }
    }
}
