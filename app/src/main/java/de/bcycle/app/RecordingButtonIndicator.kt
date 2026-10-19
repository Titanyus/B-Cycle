package de.bcycle.app

import android.animation.ValueAnimator
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
    private val stop=BicycleIcons.drawable(context,BicycleIcon.StopRecording,foreground)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator:ValueAnimator?=null
    private var opacity=255
    internal var dotFraction=0f;private set
    internal val running get()=animator?.isStarted==true
    fun setAnimating(enabled:Boolean) {
        if(!enabled) { if(running || dotFraction!=0f) stop();return }
        if(running) return
        if(!ValueAnimator.areAnimatorsEnabled()) { dotFraction=1f;redraw();return }
        animator=ValueAnimator.ofFloat(0f,CYCLE_MS.toFloat()).apply {
            duration=CYCLE_MS;repeatCount=ValueAnimator.INFINITE;interpolator=LinearInterpolator()
            addUpdateListener { dotFraction=dotAt((it.animatedValue as Float).toLong());redraw() }
            start()
        }
    }
    fun stop() { animator?.cancel();animator=null;dotFraction=0f;redraw() }
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
