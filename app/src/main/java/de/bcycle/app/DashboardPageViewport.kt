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
    private val touchSlop=ViewConfiguration.get(context).scaledTouchSlop
    private var startX=0f
    private var startY=0f
    private var candidate=false
    private var dragging=false
    private var velocity:VelocityTracker?=null
    private val commitDistance get()=minOf(BicycleDesign.dashboardSwipeCommitDp*resources.displayMetrics.density,width*.15f).coerceAtLeast(touchSlop*3f)
    var running=false;private set
    init { tag="dashboard_page_viewport";clipChildren=true;clipToPadding=true }
    fun setPage(view:View) {
        check(page==null);page=view;addView(view,LayoutParams(-1,-1))
    }
    fun slide(direction:Int,change:()->Unit) {
        if(running) return // Consume rapid extra flings without queuing stale page selections.
        val live=page
        if(live==null || direction==0 || !animationsEnabled() || !isAttachedToWindow || !isLaidOut || width<=0 || height<=0) {
            cancelTransition();change();return
        }
        val start=live.translationX
        val shot=capture(live) ?: run { cancelTransition();change();return }
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
                val motion=ValueAnimator.ofFloat(0f,1f).apply {
                    duration=(BicycleDesign.dashboardPageTransitionMs*(abs(distance+start)/width).coerceIn(.3f,1f)).toLong()
                    interpolator=PathInterpolator(.2f,0f,.1f,1f)
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
        resetGesture()
        generation++;removePending()
        animator?.let { it.removeAllListeners();it.removeAllUpdateListeners();it.cancel() };animator=null
        page?.translationX=0f
        picture?.let { old -> old.setImageDrawable(null);removeView(old) }
        picture=null;bitmap?.recycle();bitmap=null;running=false
    }
    private fun resetGesture() {
        candidate=false;dragging=false;velocity?.recycle();velocity=null
    }
    override fun onInterceptTouchEvent(event:MotionEvent):Boolean {
        if(running) return true
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetGesture();startX=event.x;startY=event.y
                candidate=changePage!=null && swipeEnabledAt?.invoke(startX,startY)==true
                if(candidate) velocity=VelocityTracker.obtain().also { it.addMovement(event) }
            }
            MotionEvent.ACTION_MOVE -> if(candidate) {
                if(swipeEnabledAt?.invoke(startX,startY)!=true) { resetGesture();return false }
                val dx=event.x-startX;val dy=event.y-startY
                if(abs(dy)>touchSlop && abs(dy)>abs(dx)) { resetGesture();return false }
                if(abs(dx)>touchSlop && abs(dx)>abs(dy)*1.5f) {
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
        if(running) return true
        if(!dragging) return true // Remaining events of an already completed/aborted gesture are consumed.
        velocity?.addMovement(event)
        when(event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                dragTo(event.x)
            }
            MotionEvent.ACTION_UP -> {
                velocity?.computeCurrentVelocity(1000)
                val offset=page?.translationX ?: 0f;val speed=velocity?.xVelocity ?: 0f
                val fling=abs(offset)>=24*resources.displayMetrics.density && abs(speed)>=400*resources.displayMetrics.density && speed*offset>0
                if(abs(offset)>=commitDistance || fling) commitDrag() else returnDrag()
            }
            MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN -> returnDrag()
        }
        return true
    }
    private fun dragTo(x:Float) {
        page?.translationX=(x-startX).coerceIn(-width.toFloat(),width.toFloat())
        if(abs(page?.translationX ?: 0f)>=commitDistance) commitDrag()
    }
    private fun commitDrag() {
        val offset=page?.translationX ?: 0f
        val change=changePage;resetGesture()
        if(change!=null && offset!=0f) change(if(offset<0) 1 else -1) else returnDrag()
    }
    private fun returnDrag() {
        resetGesture()
        val live=page ?: return;val start=live.translationX
        if(start==0f || !animationsEnabled()) { live.translationX=0f;return }
        val token=++generation;running=true
        animator=ValueAnimator.ofFloat(start,0f).apply {
            duration=BicycleDesign.dashboardDragReturnMs;interpolator=PathInterpolator(.2f,0f,.1f,1f)
            addUpdateListener { live.translationX=it.animatedValue as Float }
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
}
