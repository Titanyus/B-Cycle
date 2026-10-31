package de.bcycle.app

import android.app.Activity
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteChartNavigationTest {
    private class Fixture(val distance:Boolean=true) {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        val c=host.get()
        val navigation=RouteChartNavigation(c)
        var blocked=false
        val parent=object:LinearLayout(c) {
            override fun requestDisallowInterceptTouchEvent(disallow:Boolean) { blocked=disallow;super.requestDisallowInterceptTouchEvent(disallow) }
        }.apply { orientation=LinearLayout.VERTICAL }
        val rows=AnalysisChart.entries.map { chart -> AnalysisSeries(chart,(0..100).map { AnalysisPoint(it*1000.0,it*60.0,if(it in 40..45) null else 50.0+it%15) }) }
        val analysis=RouteAnalysis(ManagedRoute("ride","Fahrt",true,"FIT",RouteDetails(),false),emptyMap(),null,distance,if(distance) 100.0 else 6000.0,rows)
        val charts=rows.map { RouteChartView(c,ThemeManager(c.getSharedPreferences("bike",0)),analysis,it,navigation) }
        val time=SystemClock.uptimeMillis()
        init {
            charts.forEach { parent.addView(it,LinearLayout.LayoutParams(360,140)) };c.setContentView(parent)
            parent.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));parent.layout(0,0,360,700)
        }
        fun event(chart:RouteChartView,offset:Long,action:Int,ids:IntArray,vararg xs:Float) {
            val properties=Array(ids.size) { i -> MotionEvent.PointerProperties().apply { id=ids[i];toolType=MotionEvent.TOOL_TYPE_FINGER } }
            val coordinates=Array(ids.size) { i -> MotionEvent.PointerCoords().apply { x=xs[i];y=70f;pressure=1f;size=1f } }
            MotionEvent.obtain(time,time+offset,action,ids.size,properties,coordinates,0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0).also { chart.onTouchEvent(it);it.recycle() }
        }
        fun close() { host.pause().stop().destroy() }
    }
    @Test fun closeFingersAndPointerReplacementKeepEveryPlotAndYScaleSynchronized() {
        for(distance in listOf(true,false)) {
            val f=Fixture(distance)
            try {
                val chart=f.charts[1];val viewport=f.navigation.viewport
                f.navigation.zoom(chart,32.0)
                val axes=f.charts.map { it.scale };val original=f.rows.map { it.points.toList() }
                f.event(chart,0,MotionEvent.ACTION_DOWN,intArrayOf(7),100f)
                f.event(chart,10,MotionEvent.ACTION_POINTER_DOWN or (1 shl 8),intArrayOf(7,42),100f,300f)
                f.event(chart,30,MotionEvent.ACTION_MOVE,intArrayOf(7,42),150f,250f)
                val wide=viewport.span
                f.event(chart,50,MotionEvent.ACTION_MOVE,intArrayOf(7,42),185f,215f)
                val close=viewport.span;assertTrue(close>wide)
                f.event(chart,70,MotionEvent.ACTION_MOVE,intArrayOf(7,42),192.5f,207.5f)
                assertEquals(close*2,viewport.span,1e-8);assertTrue(f.blocked)
                f.event(chart,90,MotionEvent.ACTION_POINTER_DOWN or (2 shl 8),intArrayOf(7,42,99),192.5f,207.5f,280f)
                val before=viewport.span
                f.event(chart,100,MotionEvent.ACTION_POINTER_UP or (1 shl 8),intArrayOf(7,42,99),192.5f,207.5f,280f)
                f.event(chart,120,MotionEvent.ACTION_MOVE,intArrayOf(7,99),180f,267.5f)
                assertEquals(before,viewport.span,1e-8)
                f.event(chart,140,MotionEvent.ACTION_MOVE,intArrayOf(7,99),160f,270f)
                assertTrue(viewport.span<before)
                f.event(chart,150,MotionEvent.ACTION_CANCEL,intArrayOf(7,99),160f,270f)
                assertFalse(f.blocked);assertEquals(axes,f.charts.map { it.scale });assertEquals(original,f.rows.map { it.points })
                assertEquals(1,f.charts.map { it.screenX(f.analysis.axisEnd*.5) }.distinct().size)
            } finally { f.close() }
        }
    }
    @Test fun swipeCoastsTogetherNewTouchOnAnotherPlotStopsItAndEdgesClamp() {
        val f=Fixture()
        try {
            val chart=f.charts.first();val viewport=f.navigation.viewport
            f.navigation.zoom(chart,4.0)
            f.event(chart,0,MotionEvent.ACTION_DOWN,intArrayOf(0),240f)
            f.event(chart,20,MotionEvent.ACTION_MOVE,intArrayOf(0),190f)
            f.event(chart,40,MotionEvent.ACTION_MOVE,intArrayOf(0),140f)
            f.event(chart,60,MotionEvent.ACTION_UP,intArrayOf(0),100f)
            val released=viewport.start
            repeat(8) { SystemClock.sleep(16);chart.computeScroll() }
            assertTrue(viewport.start>released)
            assertEquals(1,f.charts.map { it.screenX(50.0) }.distinct().size)
            val other=f.charts.last();f.event(other,500,MotionEvent.ACTION_DOWN,intArrayOf(0),150f)
            val stopped=viewport.start
            repeat(8) { SystemClock.sleep(16);chart.computeScroll();other.computeScroll() }
            assertEquals(stopped,viewport.start,0.0)
            f.event(other,520,MotionEvent.ACTION_CANCEL,intArrayOf(0),150f)
            viewport.pan(100.0);assertEquals(1.0-viewport.span,viewport.start,0.0)
            viewport.pan(-100.0);assertEquals(0.0,viewport.start,0.0)
            f.navigation.zoom(other,.001);assertEquals(1.0,viewport.span,0.0);assertEquals(0.0,viewport.start,0.0)
            f.event(other,600,MotionEvent.ACTION_DOWN,intArrayOf(0),240f)
            f.event(other,620,MotionEvent.ACTION_MOVE,intArrayOf(0),100f)
            assertFalse(f.blocked);assertEquals(0.0,viewport.start,0.0)
        } finally { f.close() }
    }
    @Test fun zoomedCurveUsesTheSharedXRangeAndClipsAtThePlotWithoutFillingActualGaps() {
        val f=Fixture()
        try {
            val chart=f.charts[1];f.navigation.zoom(chart,4.0)
            val image=android.graphics.Bitmap.createBitmap(360,140,android.graphics.Bitmap.Config.ARGB_8888)
            try {
                val canvas=android.graphics.Canvas(image);canvas.drawColor(android.graphics.Color.BLACK);chart.draw(canvas)
                val gap=chart.screenX(42.0).toInt();val valid=chart.screenX(55.0).toInt()
                assertEquals(android.graphics.Color.BLACK,image.getPixel(gap,112))
                assertNotEquals(android.graphics.Color.BLACK,image.getPixel(valid,112))
                assertEquals(android.graphics.Color.BLACK,image.getPixel(chart.plotLeft.toInt()-2,112))
            } finally { image.recycle() }
        } finally { f.close() }
    }
    @Test fun detachStopsKineticsAndAccessibilityChangesTheSharedRange() {
        val f=Fixture()
        try {
            val chart=f.charts.first()
            assertTrue(chart.performAccessibilityAction(R.id.route_chart_zoom_in,null))
            assertEquals(.5,f.navigation.viewport.span,0.0)
            assertTrue(f.charts.last().performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null))
            assertEquals(.5,f.navigation.viewport.start,0.0)
            f.event(chart,0,MotionEvent.ACTION_DOWN,intArrayOf(0),100f)
            f.event(chart,20,MotionEvent.ACTION_MOVE,intArrayOf(0),150f)
            f.event(chart,40,MotionEvent.ACTION_UP,intArrayOf(0),200f)
            f.parent.removeView(chart);val stopped=f.navigation.viewport.start
            repeat(8) { SystemClock.sleep(16);chart.computeScroll() };assertEquals(stopped,f.navigation.viewport.start,0.0)
            f.navigation.dispose()
        } finally { f.close() }
    }
    @Test fun roundTickLabelsSlidePartiallyUnderTheFixedEdgeRatherThanDisappearing() {
        val f=Fixture()
        try {
            val chart=f.charts.first()
            f.navigation.zoom(chart,10.0)
            val text=android.graphics.Paint().apply { textSize=9f }
            val clipLeft=chart.plotLeft+text.measureText("47.0")+4f
            val counts=listOf(3f,0f,-3f).map { shift ->
                val tickX=clipLeft+shift
                val from=48.0-(tickX-chart.plotLeft)/(chart.plotRight-chart.plotLeft)*10.0
                f.navigation.viewport.pan(from/100-f.navigation.viewport.start)
                val image=android.graphics.Bitmap.createBitmap(360,140,android.graphics.Bitmap.Config.ARGB_8888)
                try {
                    val canvas=android.graphics.Canvas(image);canvas.drawColor(android.graphics.Color.BLACK);chart.draw(canvas)
                    var ink=0
                    for(x in kotlin.math.ceil(clipLeft.toDouble()).toInt()..(clipLeft+12).toInt())
                        for(y in 120..132) if(image.getPixel(x,y)!=android.graphics.Color.BLACK) ink++
                    ink
                } finally { image.recycle() }
            }
            assertTrue("Clipped glyph remains partially visible: $counts",counts[1]>0)
            assertTrue("The glyph leaves progressively: $counts",counts.first()>counts.last())
        } finally { f.close() }
    }

}
