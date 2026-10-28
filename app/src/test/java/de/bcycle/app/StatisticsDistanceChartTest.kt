package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatisticsDistanceChartTest {
    private data class Label(val text:String,val x:Float,val y:Float,val color:Int,val align:Paint.Align,val clip:Rect,val textWidth:Float,val top:Float,val bottom:Float)
    private class Drawing(bitmap:Bitmap):Canvas(bitmap) {
        val labels=mutableListOf<Label>();val pathColors=mutableListOf<Int>();val rectColors=mutableListOf<Int>()
        override fun drawText(text:String,x:Float,y:Float,paint:Paint) { labels+=Label(text,x,y,paint.color,paint.textAlign,Rect(clipBounds),paint.measureText(text),y+paint.ascent(),y+paint.descent());super.drawText(text,x,y,paint) }
        override fun drawPath(path:Path,paint:Paint) { pathColors+=paint.color;super.drawPath(path,paint) }
        override fun drawRect(rect:RectF,paint:Paint) { rectColors+=paint.color;super.drawRect(rect,paint) }
    }
    private fun layout(chart:View) { chart.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(240,View.MeasureSpec.EXACTLY));chart.layout(0,0,360,240) }
    private fun draw(chart:View):Drawing { val image=Bitmap.createBitmap(360,240,Bitmap.Config.ARGB_8888);return Drawing(image).also { chart.draw(it);image.recycle() } }
    private fun series(weeks:Boolean)=listOf(StatisticsYearSeries(2025,Color.BLUE,List(if(weeks) 52 else 12) { 101.0+it }),
        StatisticsYearSeries(2026,Color.GREEN,List(if(weeks) 53 else 12) { 301.0+it }))
    @Test fun fullWidthPlotHasInsideRightScalesNoZeroAndColoredPointValuesForBothModes() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val chart=StatisticsDistanceChart(c,ThemeManager(c.getSharedPreferences("statistics-axis",0)))
            c.setContentView(chart);layout(chart)
            for(weeks in listOf(false,true)) {
                val labels=if(weeks) (1..53).map { it.toString() } else listOf("Jan","Feb","Mär","Apr","Mai","Jun","Jul","Aug","Sep","Okt","Nov","Dez")
                chart.show(series(weeks),labels,weeks)
                val drawing=draw(chart)
                assertEquals(setOf(Color.BLUE,Color.GREEN),drawing.pathColors.toSet())
                assertFalse(drawing.rectColors.any { it==Color.BLUE || it==Color.GREEN })
                val scale=drawing.labels.filter { it.text.endsWith(" km") }
                assertEquals(4,scale.size);assertFalse(scale.any { it.text=="0 km" })
                assertTrue(scale.all { it.align==Paint.Align.RIGHT && it.x==356f && it.y in 24f..212f })
                val pointLabels=drawing.labels.filter { it.color==Color.BLUE || it.color==Color.GREEN }
                assertTrue(pointLabels.any { it.color==Color.BLUE });assertTrue(pointLabels.any { it.color==Color.GREEN })
                assertTrue(pointLabels.all { it.align==Paint.Align.CENTER && it.clip.left==0 && it.clip.right==360 })
                val axis=drawing.labels.filter { it.y==233f }
                if(weeks) assertTrue(axis.all { it.text.matches(Regex("[0-9]{1,2}")) }) else assertEquals(labels,axis.map { it.text })
                val before=scale.map { it.text }
                if(weeks) chart.performAccessibilityAction(StatisticsDistanceChart.ZOOM_OUT,null)
                assertEquals(before,draw(chart).labels.filter { it.text.endsWith(" km") }.map { it.text })
                assertTrue(chart.contentDescription.toString().contains("2025"));assertTrue(chart.contentDescription.toString().contains("2026"))
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun overlappingPointValuesStayPresentAndAvoidTheInsideScale() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val chart=StatisticsDistanceChart(c,ThemeManager(c.getSharedPreferences("statistics-values",0)))
            c.setContentView(chart);layout(chart)
            chart.show(listOf(StatisticsYearSeries(2025,Color.BLUE,List(12) { 1000.0 }),StatisticsYearSeries(2026,Color.GREEN,List(12) { 1000.0 })),(1..12).map { it.toString() },false)
            val drawing=draw(chart)
            val values=drawing.labels.filter { it.color==Color.BLUE || it.color==Color.GREEN }
            assertEquals(24,values.size)
            val axes=drawing.labels.filter { it.text.endsWith(" km") }
            for(value in values) for(axis in axes) {
                val point=RectF(value.x-value.textWidth/2,value.top,value.x+value.textWidth/2,value.bottom)
                val scale=RectF(axis.x-axis.textWidth,axis.top,axis.x,axis.bottom)
                assertFalse("Point label must not be hidden by scale",RectF.intersects(point,scale))
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun closeFingerPinchContinuesAndPointerChangesKeepDataAndScaleStable() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val chart=StatisticsDistanceChart(c,ThemeManager(c.getSharedPreferences("statistics-pinch",0)))
            c.setContentView(chart);layout(chart);chart.show(series(true),(1..53).map { it.toString() },true)
            chart.viewport.zoom(100.0,.5);val data=chart.contentDescription.toString()
            val scale=draw(chart).labels.filter { it.text.endsWith(" km") }.map { it.text }
            fun event(time:Long,action:Int,ids:IntArray,vararg xs:Float) {
                val props=Array(ids.size) { i -> MotionEvent.PointerProperties().apply { id=ids[i];toolType=MotionEvent.TOOL_TYPE_FINGER } }
                val coords=Array(ids.size) { i -> MotionEvent.PointerCoords().apply { x=xs[i];y=100f;pressure=1f;size=1f } }
                MotionEvent.obtain(0,time,action,ids.size,props,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0).also { chart.onTouchEvent(it);it.recycle() }
            }
            event(0,MotionEvent.ACTION_DOWN,intArrayOf(7),160f)
            event(10,MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),intArrayOf(7,42),160f,200f)
            event(30,MotionEvent.ACTION_MOVE,intArrayOf(7,42),168f,192f)
            val close=chart.viewport.span
            event(50,MotionEvent.ACTION_MOVE,intArrayOf(7,42),174f,186f)
            assertEquals(close*2,chart.viewport.span,1e-8)
            event(60,MotionEvent.ACTION_POINTER_DOWN or (2 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),intArrayOf(7,42,99),174f,186f,250f)
            event(70,MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),intArrayOf(7,42,99),174f,186f,250f)
            val before=chart.viewport.span
            event(90,MotionEvent.ACTION_MOVE,intArrayOf(7,99),164f,240f);assertEquals(before,chart.viewport.span,1e-8)
            event(100,MotionEvent.ACTION_CANCEL,intArrayOf(7,99),164f,240f)
            assertEquals(data,chart.contentDescription.toString());assertEquals(scale,draw(chart).labels.filter { it.text.endsWith(" km") }.map { it.text })
            for(shift in listOf(2.1,3.2,-.3)) {
                chart.viewport.pan(shift)
                // An accessibility zoom updates the cached screen geometry, keeping all source values.
                chart.performAccessibilityAction(StatisticsDistanceChart.ZOOM_OUT,null)
                val labels=draw(chart).labels.filter { it.color==Color.BLUE }
                assertTrue(labels.all { it.text.toInt() in 101..152 })
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun horizontalSwipeCoastsAndAnotherTouchStopsIt() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val chart=StatisticsDistanceChart(c,ThemeManager(c.getSharedPreferences("statistics-fling",0)))
            c.setContentView(chart);layout(chart);chart.show(series(true),(1..53).map { it.toString() },true)
            chart.viewport.pan(12.0)
            val time=SystemClock.uptimeMillis()
            fun event(offset:Long,action:Int,x:Float) { MotionEvent.obtain(time,time+offset,action,x,100f,0).also { chart.onTouchEvent(it);it.recycle() } }
            event(0,MotionEvent.ACTION_DOWN,300f);event(20,MotionEvent.ACTION_MOVE,230f);event(40,MotionEvent.ACTION_MOVE,160f);event(50,MotionEvent.ACTION_UP,120f)
            val released=chart.viewport.end
            repeat(6) { org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(16));chart.computeScroll() }
            assertTrue(chart.viewport.end>released)
            event(200,MotionEvent.ACTION_DOWN,200f);val stopped=chart.viewport.end
            repeat(6) { org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMillis(16));chart.computeScroll() }
            assertEquals(stopped,chart.viewport.end,0.0)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun viewportClampsToRealWeeksAndPreservesTheZoomAnchor() {
        val viewport=StatisticsViewport();viewport.size(53)
        val pivot=viewport.end-viewport.span*.75
        viewport.zoom(2.0,.25)
        assertEquals(pivot,viewport.end-viewport.span*.75,1e-9)
        viewport.zoom(.001,.5);assertEquals(53.0,viewport.span,0.0)
        viewport.pan(1e9);assertEquals(52.5,viewport.end,0.0)
        viewport.zoom(1000.0,.5);assertEquals(4.0,viewport.span,0.0)
        viewport.pan(-1e9);assertEquals(3.5,viewport.end,0.0)
        viewport.zoom(Double.NaN,.5);assertEquals(4.0,viewport.span,0.0)
        viewport.size(52);assertTrue(viewport.end<=51.5)
    }
}
