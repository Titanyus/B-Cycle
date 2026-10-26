package de.bcycle.app

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import org.mapsforge.map.android.view.MapView
import kotlin.math.*

/** Continuous two-finger zoom without Android's minimum-span cutoff; native pan/fling stays intact. */
internal class MapPinchZoom(private val map:MapView,private val maximum:()->Int) {
    private var consuming=false
    private var pointerA=-1;private var pointerB=-1
    private var span=0.0;private var time=0L;private var movedAt=0L
    private var focusX=0.0;private var focusY=0.0
    private var velocity=0.0
    private var animation:ValueAnimator?=null
    fun stop() { animation?.cancel();animation=null }
    fun cancel() { stop();map.parent?.requestDisallowInterceptTouchEvent(false);consuming=false;pointerA=-1;pointerB=-1;span=0.0;velocity=0.0 }
    private fun begin(event:MotionEvent) {
        if(!consuming) {
            val cancel=MotionEvent.obtain(event).apply { action=MotionEvent.ACTION_CANCEL }
            map.onTouchEvent(cancel);cancel.recycle()
            map.onZoomEvent();consuming=true
        }
        stop();map.parent?.requestDisallowInterceptTouchEvent(true)
        pointerA=event.getPointerId(0);pointerB=event.getPointerId(1)
        span=distance(event);time=event.eventTime;movedAt=0;velocity=0.0
        rememberFocus(event)
    }
    private fun rememberFocus(event:MotionEvent) {
        val a=event.findPointerIndex(pointerA);val b=event.findPointerIndex(pointerB)
        if(a>=0 && b>=0) { focusX=(event.getX(a)+event.getX(b))/2.0;focusY=(event.getY(a)+event.getY(b))/2.0 }
    }
    private fun distance(event:MotionEvent):Double {
        val a=event.findPointerIndex(pointerA);val b=event.findPointerIndex(pointerB)
        if(a<0 || b<0) return 0.0
        return hypot((event.getX(a)-event.getX(b)).toDouble(),(event.getY(a)-event.getY(b)).toDouble())
    }
    private fun coast(now:Long) {
        if(movedAt==0L || now-movedAt>120 || abs(velocity)<.15) return
        val start=map.model.mapViewPosition.zoom
        val end=(start+velocity*.16).coerceIn(MapZoom.minimum.toDouble(),maximum().toDouble())
        if(abs(end-start)<.005) return
        animation=ValueAnimator.ofFloat(start.toFloat(),end.toFloat()).apply {
            duration=480;interpolator=DecelerateInterpolator(1.5f)
            addUpdateListener { map.model.mapViewPosition.setZoom((it.animatedValue as Float).toDouble(),false) }
            start()
        }
    }
    /** True only for our multi-pointer sequence; one-finger gestures remain Mapsforge-owned. */
    fun onTouch(event:MotionEvent):Boolean {
        if(event.actionMasked==MotionEvent.ACTION_DOWN) { cancel();return false }
        if(event.actionMasked==MotionEvent.ACTION_CANCEL) { val handled=consuming;cancel();return handled }
        if(event.actionMasked==MotionEvent.ACTION_POINTER_DOWN && event.pointerCount>=2) { begin(event);return true }
        if(!consuming) return false
        when(event.actionMasked) {
            MotionEvent.ACTION_MOVE -> if(pointerA>=0 && event.pointerCount>=2) {
                val next=distance(event)
                if(next>0 && span>0) {
                    val before=map.model.mapViewPosition.zoom
                    val zoom=(before+ln(next/span)/ln(2.0)).coerceIn(MapZoom.minimum.toDouble(),maximum().toDouble())
                    val oldX=focusX;val oldY=focusY
                    rememberFocus(event)
                    val ratio=2.0.pow(zoom-before)
                    val centerX=map.width*map.mapViewCenterX.toDouble()
                    val centerY=map.height*map.mapViewCenterY.toDouble()
                    val dx=focusX-centerX-(oldX-centerX)*ratio
                    val dy=focusY-centerY-(oldY-centerY)*ratio
                    if(abs(dx)>.001 || abs(dy)>.001) map.onMoveEvent()
                    map.model.mapViewPosition.setZoom(zoom,false)
                    if(abs(dx)>.001 || abs(dy)>.001) {
                        val rotated=org.mapsforge.core.model.Rotation(map.mapRotation.degrees,0f,0f).rotate(dx,dy)
                        map.model.mapViewPosition.moveCenter(rotated.x,rotated.y,false)
                    }
                    val dt=event.eventTime-time
                    if(dt>0 && dt<=120) {
                        val sample=((zoom-before)*1000/dt).coerceIn(-6.0,6.0)
                        velocity=if(velocity*sample<0) sample else velocity*.35+sample*.65
                    } else velocity=0.0
                    if(abs(zoom-before)>.0001) movedAt=event.eventTime
                }
                span=next;time=event.eventTime
            }
            MotionEvent.ACTION_POINTER_UP -> if(event.getPointerId(event.actionIndex) in listOf(pointerA,pointerB)) {
                if(event.pointerCount>2) {
                    val remaining=(0 until event.pointerCount).filter { it!=event.actionIndex }
                    pointerA=event.getPointerId(remaining[0]);pointerB=event.getPointerId(remaining[1]);span=distance(event);time=event.eventTime;velocity=0.0
                    rememberFocus(event)
                } else { coast(event.eventTime);pointerA=-1;pointerB=-1 }
            }
            MotionEvent.ACTION_UP -> { consuming=false;map.parent?.requestDisallowInterceptTouchEvent(false) }
        }
        return true
    }
}
