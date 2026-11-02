package de.bcycle.app

import android.animation.ValueAnimator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator

/** Same stop action, with a display-only recording cue shared by every bike profile. */
internal class RecordingButtonIndicator(context:Context,private val foreground:Int,private val redraw:()->Unit):Drawable() {
    companion object {
        const val CYCLE_MS=4_000L
        internal fun dotAt(elapsed:Long):Float {
            val phase=Math.floorMod(elapsed,CYCLE_MS)
            val linear=when {
                phase<1_500->0f
                phase<2_000->(phase-1_500)/500f
                phase<3_500->1f
                else->1f-(phase-3_500)/500f
            }
            return ((1-kotlin.math.cos(linear*Math.PI))/2).toFloat()
        }
    }
    var powerSaving=false
        set(value) { if(field!=value) { field=value;stop();if(value) showFraction(1f) } }
    private val stop=BicycleIcons.drawable(context,BicycleIcon.StopRecording,foreground)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator:ValueAnimator?=null
    private var opacity=255
    internal var dotFraction=0f;private set
    private val handler=Handler(Looper.getMainLooper())
    private var startedAt=0L
    private val advance=Runnable { advanceCycle() }
    internal var running=false;private set
    internal val transitioning get()=animator?.isStarted==true
    private fun showFraction(value:Float) {
        if(dotFraction==value) return
        dotFraction=value;redraw()
    }
    fun setAnimating(enabled:Boolean) {
        if(!enabled) { if(running || dotFraction!=0f) stop();return }
        if(powerSaving) { if(running) stop();showFraction(1f);return }
        if(running) return
        if(!ValueAnimator.areAnimatorsEnabled()) { showFraction(1f);return }
        running=true;startedAt=SystemClock.uptimeMillis();advanceCycle()
    }
    private fun advanceCycle() {
        if(!running) return
        if(!ValueAnimator.areAnimatorsEnabled()) { stop();showFraction(1f);return }
        val now=SystemClock.uptimeMillis()
        val phase=Math.floorMod(now-startedAt,CYCLE_MS)
        showFraction(dotAt(phase))
        val boundary=when {
            phase<1500->1500L
            phase<2000->2000L
            phase<3500->3500L
            else->CYCLE_MS
        }
        if(phase<1500 || phase in 2000..3499) {
            // No animator or frame callbacks while the symbol remains unchanged.
            handler.postAtTime(advance,now+boundary-phase)
            return
        }
        animator=ValueAnimator.ofFloat(phase.toFloat(),boundary.toFloat()).apply {
            duration=boundary-phase;interpolator=LinearInterpolator()
            addUpdateListener { showFraction(dotAt((it.animatedValue as Float).toLong())) }
            addListener(object:AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation:Animator) {
                    if(!running || animator!==animation) return
                    animator=null;handler.post(advance)
                }
            })
        }
        animator?.start()
    }
    fun stop() {
        running=false;handler.removeCallbacks(advance)
        val previous=animator;animator=null;previous?.cancel()
        showFraction(0f)
    }
    override fun onBoundsChange(bounds:Rect) { stop.bounds=bounds }
    override fun draw(canvas:Canvas) {
        stop.alpha=((1-dotFraction)*opacity).toInt();stop.draw(canvas)
        if(dotFraction<=0f) return
        val size=minOf(bounds.width(),bounds.height()).toFloat()
        // A fine foreground halo keeps the red point legible on red PRIMARY surfaces too.
        paint.style=Paint.Style.FILL;paint.color=foreground;paint.alpha=(opacity*dotFraction).toInt()
        canvas.drawCircle(bounds.exactCenterX(),bounds.exactCenterY(),size*.38f,paint)
        paint.color=Color.RED;paint.alpha=(opacity*dotFraction).toInt()
        canvas.drawCircle(bounds.exactCenterX(),bounds.exactCenterY(),size*.31f,paint)
    }
    override fun setAlpha(alpha:Int) { opacity=alpha;redraw() }
    override fun setColorFilter(filter:ColorFilter?) { stop.colorFilter=filter }
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
