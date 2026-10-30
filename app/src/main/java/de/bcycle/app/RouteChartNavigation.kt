package de.bcycle.app

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.widget.OverScroller
import kotlin.math.abs
import kotlin.math.hypot

/** Shared normalized X range; Y scales and the underlying ride remain unchanged. */
internal class RouteChartViewport {
    var start=0.0;private set
    var span=1.0;private set
    val zoomed get()=span<1.0-1e-9
    fun zoom(factor:Double,anchor:Double) {
        if(!factor.isFinite() || factor<=0 || !anchor.isFinite()) return
        val at=anchor.coerceIn(0.0,1.0)
        val pivot=start+span*at
        span=(span/factor).coerceIn(1.0/64,1.0)
        start=(pivot-span*at).coerceIn(0.0,1.0-span)
    }
    fun pan(delta:Double) { if(delta.isFinite()) start=(start+delta).coerceIn(0.0,1.0-span) }
}

/** Same pointer-distance pinch and native OverScroller inertia as HrvTrendChart.
 * One controller per details page keeps every time/distance plot synchronized.
 */
internal class RouteChartNavigation(context:Context) {
    val viewport=RouteChartViewport()
    private val charts=mutableSetOf<RouteChartView>()
    private var host:RouteChartView?=null
    private val scroller=OverScroller(context)
    private var flingX=0
    private val slop=ViewConfiguration.get(context).scaledTouchSlop
    private var downX=0f;private var downY=0f;private var moved=false;private var multiTouch=false
    private var pointerA=-1;private var pointerB=-1;private var pinchSpan=0.0
    private val pinching get()=pointerA>=0 && pointerB>=0
    private val plotWidth get()=host?.let { (it.plotRight-it.plotLeft).coerceAtLeast(1f) } ?: 1f
    private fun anchor(x:Float)=host?.let { ((x-it.plotLeft)/plotWidth).toDouble() } ?: .5
    private fun changed() { charts.forEach { it.invalidate() } }
    fun attach(chart:RouteChartView) { charts.add(chart) }
    fun detach(chart:RouteChartView) {
        charts.remove(chart)
        if(host===chart) { stop();host=null }
    }
    fun stop() { scroller.forceFinished(true);endPinch();host?.parent?.requestDisallowInterceptTouchEvent(false) }
    fun dispose() { stop();charts.clear();host=null }
    private fun distance(event:MotionEvent):Double {
        val a=event.findPointerIndex(pointerA);val b=event.findPointerIndex(pointerB)
        return if(a>=0 && b>=0) hypot((event.getX(a)-event.getX(b)).toDouble(),(event.getY(a)-event.getY(b)).toDouble()) else 0.0
    }
    private fun beginPinch(event:MotionEvent,exclude:Int=-1) {
        val indices=(0 until event.pointerCount).filter { it!=exclude }
        pointerA=indices.getOrNull(0)?.let(event::getPointerId) ?: -1
        pointerB=indices.getOrNull(1)?.let(event::getPointerId) ?: -1
        pinchSpan=distance(event)
    }
    private fun endPinch() { pointerA=-1;pointerB=-1;pinchSpan=0.0 }
    private val quickScale=ScaleGestureDetector(context,object:ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector:ScaleGestureDetector):Boolean {
            scroller.forceFinished(true)
            viewport.zoom(detector.scaleFactor.toDouble(),anchor(detector.focusX));changed();return true
        }
    })
    private val gestures=GestureDetector(context,object:GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e:MotionEvent)=true
        override fun onScroll(e1:MotionEvent?,e2:MotionEvent,dx:Float,dy:Float):Boolean {
            if(viewport.zoomed && !pinching && !quickScale.isInProgress && moved && e2.pointerCount==1) {
                viewport.pan(dx/plotWidth*viewport.span);changed()
            }
            return true
        }
        override fun onFling(e1:MotionEvent?,e2:MotionEvent,velocityX:Float,velocityY:Float):Boolean {
            if(!viewport.zoomed || !moved || multiTouch || pinching || quickScale.isInProgress || abs(velocityX)<=abs(velocityY)) return false
            flingX=0;scroller.fling(0,0,(-velocityX).toInt(),0,Int.MIN_VALUE,Int.MAX_VALUE,0,0)
            host?.postInvalidateOnAnimation();return true
        }
        override fun onSingleTapUp(e:MotionEvent):Boolean { host?.performClick();return true }
    })
    fun touch(chart:RouteChartView,event:MotionEvent):Boolean {
        if(event.actionMasked==MotionEvent.ACTION_DOWN) {
            stop();host=chart;downX=event.x;downY=event.y;moved=false;multiTouch=false
        }
        if(event.actionMasked==MotionEvent.ACTION_POINTER_DOWN) {
            scroller.forceFinished(true);multiTouch=true;moved=true;beginPinch(event)
            chart.parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.obtain(event).also { it.action=MotionEvent.ACTION_CANCEL;quickScale.onTouchEvent(it);it.recycle() }
        }
        if(event.actionMasked==MotionEvent.ACTION_MOVE && pinching) {
            val next=distance(event)
            if(next>0 && pinchSpan>0) {
                val a=event.findPointerIndex(pointerA);val b=event.findPointerIndex(pointerB)
                viewport.zoom(next/pinchSpan,anchor((event.getX(a)+event.getX(b))/2));changed()
            }
            pinchSpan=next
        }
        if(event.actionMasked==MotionEvent.ACTION_POINTER_UP) beginPinch(event,event.actionIndex)
        if(event.actionMasked==MotionEvent.ACTION_MOVE && viewport.zoomed && abs(event.x-downX)>slop && abs(event.x-downX)>abs(event.y-downY)) {
            moved=true;chart.parent?.requestDisallowInterceptTouchEvent(true)
        }
        if(event.actionMasked==MotionEvent.ACTION_CANCEL) stop()
        if(!multiTouch) quickScale.onTouchEvent(event)
        if(quickScale.isInProgress) chart.parent?.requestDisallowInterceptTouchEvent(true)
        gestures.onTouchEvent(event)
        if(event.actionMasked==MotionEvent.ACTION_UP || event.actionMasked==MotionEvent.ACTION_CANCEL) {
            endPinch();chart.parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }
    fun computeScroll(chart:RouteChartView) {
        if(host!==chart || !scroller.computeScrollOffset()) return
        val next=scroller.currX;val dx=next-flingX;flingX=next
        val before=viewport.start;val delta=dx/plotWidth*viewport.span
        viewport.pan(delta)
        if(dx!=0 && abs(viewport.start-before-delta)>1e-9) scroller.forceFinished(true)
        changed()
        if(!scroller.isFinished) chart.postInvalidateOnAnimation()
    }
    fun scroll(chart:RouteChartView,forward:Boolean) {
        stop();host=chart;viewport.pan(viewport.span*(if(forward) .5 else -.5));changed()
    }
    fun zoom(chart:RouteChartView,factor:Double) { stop();host=chart;viewport.zoom(factor,.5);changed() }
}
