package de.bcycle.app

import android.animation.ValueAnimator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.sign

internal data class PagePreview(val view:View,val release:()->Unit={})

/** Slides one live dashboard/map tree against a short-lived outgoing picture. Footer is outside. */
internal class DashboardPageViewport(context:Context,
    private val animationsEnabled:()->Boolean={ValueAnimator.areAnimatorsEnabled()}):FrameLayout(context) {
    private var page:View?=null
    private var picture:ImageView?=null
    private var bitmap:Bitmap?=null
    private var pending:ViewTreeObserver.OnPreDrawListener?=null
    private var observer:ViewTreeObserver?=null
    private var animator:ValueAnimator?=null
    private var generation=0
    var swipeEnabledAt:((Float,Float)->Boolean)?=null
    var changePage:((Int)->Unit)?=null
    var preparePageSnapshot:(((Boolean)->Unit)->Unit)?=null
    var releasePageSnapshot:(()->Unit)?=null
    var prepareAdjacentPage:((Int,(PagePreview?)->Unit)->Unit)?=null
    private var adjacent:PagePreview?=null
    private var adjacentDirection=0
    private var adjacentGeneration=0
    private val touchSlop=ViewConfiguration.get(context).scaledTouchSlop
    private val minimumFlingVelocity=maxOf(400*resources.displayMetrics.density,ViewConfiguration.get(context).scaledMinimumFlingVelocity.toFloat())
    private val maximumFlingVelocity=ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()
    private var startX=0f
    private var startY=0f
    private var dragOriginX=0f
    private var handoffVelocity=0f
    private var waitingForPage=false
    private var candidate=false
    private var dragging=false
    private var velocity:VelocityTracker?=null
    var running=false;private set
    init { tag="dashboard_page_viewport";clipChildren=true;clipToPadding=true }
    fun setPage(view:View,wrapContent:Boolean=false) {
        check(page==null);page=view;addView(view,LayoutParams(-1,if(wrapContent) -2 else -1))
    }
    /** Retain the outgoing day while its local model is being prepared; do not animate a loading label. */
    fun awaitPage() { waitingForPage=true }
    fun returnToPage() { waitingForPage=false;returnDrag() }
    fun slide(direction:Int,change:()->Unit) {
        if(running) return // Consume rapid extra flings without queuing stale page selections.
        waitingForPage=false
        val live=page
        if(live==null || direction==0 || !animationsEnabled() || !isAttachedToWindow || !isLaidOut || width<=0 || height<=0) {
            cancelTransition();change();return
        }
        val prepare=preparePageSnapshot
        if(prepare!=null) {
            val token=++generation;running=true
            prepare { ready ->
                if(token!=generation || !running) return@prepare
                running=false
                if(ready) startSlide(direction,change) else { cancelTransition();change() }
                releasePageSnapshot?.invoke()
            }
        } else startSlide(direction,change)
    }
    private fun startSlide(direction:Int,change:()->Unit) {
        val live=page ?: return
        val start=live.translationX
        val speed=handoffVelocity;handoffVelocity=0f
        val shot=capture(live) ?: run { cancelTransition();change();return }
        clearAdjacent()
        val token=++generation
        running=true;bitmap=shot
        val outgoing=ImageView(context).apply {
            tag="dashboard_page_outgoing";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
            scaleType=ImageView.ScaleType.FIT_XY;setImageBitmap(shot)
        }
        picture=outgoing
        addView(outgoing,LayoutParams(width,height))
        val distance=width.toFloat()*if(direction>0) 1 else -1
        outgoing.translationX=start;live.translationX=distance+start
        try { change() } catch(e:Exception) { cancelTransition();throw e }
        val next=object:ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw():Boolean {
                removePending()
                if(!running || token!=generation) return true
                // A complete manual swipe already placed the new page at its destination.
                if(abs(distance+start)<1f) { cancelTransition();return true }
                val motion=ValueAnimator.ofFloat(0f,1f).apply {
                    val remaining=abs(distance+start)
                    val movingSpeed=if(speed*start>0) abs(speed) else 0f
                    val base=BicycleDesign.dashboardPageTransitionMs*(remaining/width).coerceIn(.3f,1f)
                    duration=if(movingSpeed>0) minOf(base,2000*remaining/movingSpeed).toLong().coerceAtLeast(90L) else base.toLong()
                    interpolator=if(start!=0f) pageHandoffInterpolator(movingSpeed*duration/1000/remaining.coerceAtLeast(1f)) else PathInterpolator(.2f,0f,.1f,1f)
                    addUpdateListener {
                        val fraction=it.animatedValue as Float
                        outgoing.translationX=start-(distance+start)*fraction;live.translationX=outgoing.translationX+distance
                    }
                    addListener(object:AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation:Animator) { if(token==generation) cancelTransition() }
                    })
                }
                animator=motion;motion.start()
                return true
            }
        }
        pending=next;observer=viewTreeObserver.also { it.addOnPreDrawListener(next) }
        invalidate()
    }
    private fun capture(view:View):Bitmap? {
        if(view.width<=0 || view.height<=0) return null
        // At most 16 MB and one allocation per switch. Transparency preserves the window's mode light.
        val scale=sqrt(4_000_000.0/(view.width.toDouble()*view.height)).coerceAtMost(1.0)
        var result:Bitmap?=null
        return try {
            val w=(view.width*scale).toInt().coerceAtLeast(1);val h=(view.height*scale).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also {
                result=it;it.density=resources.displayMetrics.densityDpi
                val canvas=Canvas(it);canvas.scale(w.toFloat()/view.width,h.toFloat()/view.height);view.draw(canvas)
            }
        } catch(_:OutOfMemoryError) { result?.recycle();null }
          catch(_:RuntimeException) { result?.recycle();null } // Unsupported software drawing: switch immediately.
    }
    private fun removePending() {
        pending?.let { listener -> observer?.takeIf { it.isAlive }?.removeOnPreDrawListener(listener) }
        pending=null;observer=null
    }
    fun cancelTransition() {
        waitingForPage=false;handoffVelocity=0f
        resetGesture()
        clearAdjacent()
        generation++;removePending();releasePageSnapshot?.invoke()
        animator?.let { it.removeAllListeners();it.removeAllUpdateListeners();it.cancel() };animator=null
        page?.translationX=0f
        picture?.let { old -> old.setImageDrawable(null);removeView(old) }
        picture=null;bitmap?.recycle();bitmap=null;running=false
    }
    private fun resetGesture() {
        candidate=false;dragging=false;velocity?.recycle();velocity=null
    }
    override fun onInterceptTouchEvent(event:MotionEvent):Boolean {
        if(running || waitingForPage) return true
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetGesture();clearAdjacent();startX=event.x;startY=event.y
                candidate=changePage!=null && swipeEnabledAt?.invoke(startX,startY)==true
                if(candidate) velocity=VelocityTracker.obtain().also { it.addMovement(event) }
            }
            MotionEvent.ACTION_MOVE -> if(candidate) {
                if(swipeEnabledAt?.invoke(startX,startY)!=true) { resetGesture();return false }
                val dx=event.x-startX;val dy=event.y-startY
                if(abs(dy)>touchSlop && abs(dy)>abs(dx)) { resetGesture();return false }
                if(abs(dx)>touchSlop && abs(dx)>abs(dy)*1.5f) {
                    // Recognition consumes the slop: start at its edge, not at the original down point.
                    dragOriginX=startX+sign(dx)*touchSlop
                    dragging=true;parent?.requestDisallowInterceptTouchEvent(true)
                    velocity?.addMovement(event)
                    dragTo(event.x)
                    return true
                }
                velocity?.addMovement(event)
            }
            MotionEvent.ACTION_POINTER_DOWN,MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> resetGesture()
        }
        return super.onInterceptTouchEvent(event)
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        if(running || waitingForPage) return true
        // A non-clickable day circle leaves this container owning DOWN. Android then skips
        // interception on MOVE, so recognize the same gesture here as for clickable dashboard tiles.
        if(!dragging && candidate && event.actionMasked==MotionEvent.ACTION_MOVE) {
            onInterceptTouchEvent(event);return true
        }
        if(!dragging) {
            if(event.actionMasked in intArrayOf(MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN)) resetGesture()
            return true // Remaining events of an already completed/aborted gesture are consumed.
        }
        velocity?.addMovement(event)
        when(event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                dragTo(event.x)
            }
            MotionEvent.ACTION_UP -> {
                dragTo(event.x)
                velocity?.computeCurrentVelocity(1000,maximumFlingVelocity)
                val offset=page?.translationX ?: 0f;val speed=velocity?.xVelocity ?: 0f
                val fast=abs(speed)>=minimumFlingVelocity
                // Project 200 ms forward only for a deliberate fling. A reverse fling cancels
                // the departure even beyond halfway, keeping a last-moment pullback possible.
                val projection=if(fast && speed*offset>0) abs(speed)*.2f else 0f
                val enoughDrag=abs(offset)>=24*resources.displayMetrics.density
                if(enoughDrag && !(fast && speed*offset<0) && abs(offset)+projection>=width*.5f) commitDrag() else returnDrag()
            }
            MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN -> returnDrag()
        }
        return true
    }
    private fun dragTo(x:Float) {
        page?.translationX=(x-dragOriginX).coerceIn(-width.toFloat(),width.toFloat())
        val offset=page?.translationX ?: 0f
        val direction=if(offset<0) 1 else if(offset>0) -1 else 0
        if(direction!=adjacentDirection) {
            clearAdjacent()
            if(direction!=0) {
                adjacentDirection=direction
                val token=++adjacentGeneration
                prepareAdjacentPage?.invoke(direction) { preview ->
                    if(token!=adjacentGeneration || !dragging || !isAttachedToWindow) { preview?.release?.invoke();return@invoke }
                    if(preview!=null) {
                        adjacent=preview
                        preview.view.importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                        addView(preview.view,LayoutParams(-1,-1))
                        positionAdjacent()
                    }
                }
            }
        }
        positionAdjacent()
    }
    internal fun hasAdjacentDrag(direction:Int)=dragging && adjacentDirection==direction && isAttachedToWindow
    private fun positionAdjacent() { adjacent?.view?.translationX=(page?.translationX ?: 0f)+width*adjacentDirection }
    private fun clearAdjacent() {
        adjacentGeneration++;adjacentDirection=0
        val old=adjacent;adjacent=null
        old?.let { removeView(it.view);it.release() }
    }
    private fun commitDrag() {
        val offset=page?.translationX ?: 0f
        velocity?.computeCurrentVelocity(1000,maximumFlingVelocity);handoffVelocity=velocity?.xVelocity ?: 0f
        val change=changePage;resetGesture()
        if(change!=null && offset!=0f) change(if(offset<0) 1 else -1) else returnDrag()
    }
    private fun returnDrag() {
        handoffVelocity=0f
        resetGesture()
        val live=page ?: return;val start=live.translationX
        if(start==0f || !animationsEnabled()) { live.translationX=0f;return }
        val token=++generation;running=true
        animator=ValueAnimator.ofFloat(start,0f).apply {
            duration=BicycleDesign.dashboardDragReturnMs;interpolator=PathInterpolator(.2f,0f,.1f,1f)
            addUpdateListener { live.translationX=it.animatedValue as Float;positionAdjacent() }
            addListener(object:AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation:Animator) { if(token==generation) cancelTransition() }
            })
            start()
        }
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
        super.onSizeChanged(w,h,oldw,oldh)
        if(oldw>0 && w!=oldw) cancelTransition()
    }
    override fun onDetachedFromWindow() { cancelTransition();super.onDetachedFromWindow() }
    override fun onWindowFocusChanged(hasWindowFocus:Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if(!hasWindowFocus) cancelTransition()
    }
}

/** Cubic Hermite handoff: preserve initial speed, arrive at rest, remain monotone without overshoot. */
internal fun pageHandoffInterpolator(slope:Float):android.animation.TimeInterpolator {
    val initial=slope.coerceIn(0f,3f)
    return android.animation.TimeInterpolator { t ->
        val square=t*t;val cube=square*t
        -2*cube+3*square+initial*(cube-2*square+t)
    }
}
