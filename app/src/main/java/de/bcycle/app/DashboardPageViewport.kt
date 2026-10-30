package de.bcycle.app

import android.animation.ValueAnimator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.sqrt

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
    var running=false;private set
    init { tag="dashboard_page_viewport";clipChildren=true;clipToPadding=true }
    fun setPage(view:View) {
        check(page==null);page=view;addView(view,LayoutParams(-1,-1))
    }
    fun slide(direction:Int,change:()->Unit) {
        if(running) return // Consume rapid extra flings without queuing stale page selections.
        val live=page
        if(live==null || direction==0 || !animationsEnabled() || !isAttachedToWindow || !isLaidOut || width<=0 || height<=0) {
            change();return
        }
        val shot=capture(live) ?: run { change();return }
        val token=++generation
        running=true;bitmap=shot
        val outgoing=ImageView(context).apply {
            tag="dashboard_page_outgoing";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
            scaleType=ImageView.ScaleType.FIT_XY;setImageBitmap(shot)
        }
        picture=outgoing
        addView(outgoing,LayoutParams(width,height))
        val distance=width.toFloat()*if(direction>0) 1 else -1
        live.translationX=distance
        try { change() } catch(e:Exception) { cancelTransition();throw e }
        val next=object:ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw():Boolean {
                removePending()
                if(!running || token!=generation) return true
                val motion=ValueAnimator.ofFloat(0f,1f).apply {
                    duration=BicycleDesign.dashboardPageTransitionMs
                    interpolator=PathInterpolator(.2f,0f,.1f,1f)
                    addUpdateListener {
                        val fraction=it.animatedValue as Float
                        live.translationX=distance*(1-fraction);outgoing.translationX=-distance*fraction
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
        generation++;removePending()
        animator?.let { it.removeAllListeners();it.removeAllUpdateListeners();it.cancel() };animator=null
        page?.translationX=0f
        picture?.let { old -> old.setImageDrawable(null);removeView(old) }
        picture=null;bitmap?.recycle();bitmap=null;running=false
    }
    override fun onInterceptTouchEvent(event:MotionEvent)=running || super.onInterceptTouchEvent(event)
    override fun onTouchEvent(event:MotionEvent)=running || super.onTouchEvent(event)
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
        super.onSizeChanged(w,h,oldw,oldh)
        if(oldw>0 && w!=oldw) cancelTransition()
    }
    override fun onDetachedFromWindow() { cancelTransition();super.onDetachedFromWindow() }
}
