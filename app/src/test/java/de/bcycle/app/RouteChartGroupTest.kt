package de.bcycle.app

import android.app.Activity
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w360dp-h800dp-mdpi")
class RouteChartGroupTest {
    @Test fun pinchContinuesAcrossPlotsAndGapsWithOneSharedViewportAndSmallFingerDistances() {
        for(count in listOf(2,5)) {
            val host=Robolectric.buildActivity(Activity::class.java).setup()
            try {
                val c=host.get();val navigation=RouteChartNavigation(c);val themes=ThemeManager(c.getSharedPreferences("bike",0))
                val series=AnalysisChart.entries.take(count).map { AnalysisSeries(it,(0..100).map { x -> AnalysisPoint(x*1000.0,x*60.0,50.0+x%15) }) }
                val analysis=RouteAnalysis(ManagedRoute("ride","Ride",true,"FIT",RouteDetails(),false),emptyMap(),null,true,100.0,series)
                val group=RouteChartGroup(c,navigation)
                val charts=series.map { RouteChartView(c,themes,analysis,it,navigation) }
                charts.forEach { group.addView(it,LinearLayout.LayoutParams(-1,140).apply { topMargin=8 }) }
                val scroll=ScrollView(c).apply { addView(group) };c.setContentView(scroll)
                scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));scroll.layout(0,0,360,800)
                val started=SystemClock.uptimeMillis()
                fun touch(time:Long,action:Int,vararg points:Pair<Float,Float>) {
                    val properties=Array(points.size) { i -> MotionEvent.PointerProperties().apply { id=7+i*11;toolType=MotionEvent.TOOL_TYPE_FINGER } }
                    val coords=Array(points.size) { i -> MotionEvent.PointerCoords().apply { x=points[i].first;y=points[i].second;pressure=1f;size=1f } }
                    MotionEvent.obtain(started,started+time,action,points.size,properties,coords,0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0).also { scroll.dispatchTouchEvent(it);it.recycle() }
                }
                navigation.zoom(charts.first(),8.0)
                touch(0,MotionEvent.ACTION_DOWN,100f to 50f)
                touch(10,MotionEvent.ACTION_POINTER_DOWN or (1 shl 8),100f to 50f,300f to 210f)
                val before=navigation.viewport.span
                // Both pointers cross the boundary into different diagrams, without restarting.
                touch(30,MotionEvent.ACTION_MOVE,150f to 170f,250f to 330f)
                val crossed=navigation.viewport.span
                assertTrue(crossed>before);assertEquals(0,scroll.scrollY)
                touch(50,MotionEvent.ACTION_MOVE,190f to 300f,210f to 310f)
                assertTrue(navigation.viewport.span>crossed)
                assertEquals(1,charts.map { it.screenX(50.0) }.distinct().size)
                touch(60,MotionEvent.ACTION_POINTER_UP or (1 shl 8),190f to 300f,210f to 310f)
                touch(70,MotionEvent.ACTION_UP,190f to 300f)
                // A new gesture may start between diagrams, and cancellation releases the page.
                touch(90,MotionEvent.ACTION_DOWN,100f to 151f)
                touch(100,MotionEvent.ACTION_POINTER_DOWN or (1 shl 8),100f to 151f,200f to 151f)
                val gap=navigation.viewport.span
                touch(120,MotionEvent.ACTION_MOVE,50f to 200f,250f to 200f)
                assertTrue(navigation.viewport.span<gap)
                touch(130,MotionEvent.ACTION_CANCEL,50f to 200f,250f to 200f)
                assertEquals(1,charts.map { it.screenX(50.0) }.distinct().size)
            } finally { host.pause().stop().destroy() }
        }
    }
    @Test fun verticalOneFingerDragStillScrollsThePageAfterZoom() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val c=host.get();val nav=RouteChartNavigation(c);val themes=ThemeManager(c.getSharedPreferences("bike",0))
            val series=AnalysisSeries(AnalysisChart.entries.first(),(0..10).map { AnalysisPoint(it*1000.0,it*60.0,50.0) })
            val analysis=RouteAnalysis(ManagedRoute("ride","Ride",true,"FIT",RouteDetails(),false),emptyMap(),null,true,10.0,listOf(series))
            val group=RouteChartGroup(c,nav);val chart=RouteChartView(c,themes,analysis,series,nav)
            group.addView(chart,LinearLayout.LayoutParams(-1,1400))
            val scroll=ScrollView(c).apply { addView(group) };c.setContentView(scroll)
            scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));scroll.layout(0,0,360,600)
            nav.zoom(chart,4.0)
            val time=SystemClock.uptimeMillis()
            listOf(Triple(0L,MotionEvent.ACTION_DOWN,400f),Triple(20L,MotionEvent.ACTION_MOVE,350f),Triple(40L,MotionEvent.ACTION_MOVE,250f),Triple(50L,MotionEvent.ACTION_MOVE,180f),Triple(60L,MotionEvent.ACTION_UP,150f)).forEach { (at,action,y) ->
                MotionEvent.obtain(time,time+at,action,150f,y,0).also { scroll.dispatchTouchEvent(it);it.recycle() }
            }
            assertTrue(scroll.scrollY>0)
        } finally { host.pause().stop().destroy() }
    }
}
