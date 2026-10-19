package de.bcycle.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/** Three fixed glyph positions, separately faded up/down; no text/layout changes per frame. */
internal class TrendChevrons(private val host:View,private val settled:()->Unit = {}) {
    companion object { const val FADE_MS=500L }
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND }
    private var animator:ValueAnimator?=null
    private val weights=FloatArray(6)
    internal var level=0;private set
    val visible get()=level!=0 || weights.any { it>.001f }
    init {
        host.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v:View)=Unit
            override fun onViewDetachedFromWindow(v:View) { snap() }
        })
    }
    private fun target()=FloatArray(6) { i -> if(level>0 && i<level || level<0 && i in 3 until 3-level) 1f else 0f }
    fun update(next:Int) {
        if(next==level) return
        animator?.removeAllListeners();animator?.cancel();animator=null
        level=next.coerceIn(-3,3)
        if(!host.isAttachedToWindow || !host.isShown || !ValueAnimator.areAnimatorsEnabled()) { snap();return }
        val from=weights.copyOf();val to=target()
        animator=ValueAnimator.ofFloat(0f,1f).apply {
            duration=FADE_MS
            addUpdateListener { animation ->
                val fraction=animation.animatedValue as Float
                for(i in weights.indices) weights[i]=from[i]+(to[i]-from[i])*fraction
                host.invalidate()
            }
            addListener(object:AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation:Animator) { animator=null;settled() }
            })
            start()
        }
    }
    fun snap() {
        animator?.removeAllListeners();animator?.cancel();animator=null
        target().copyInto(weights);host.invalidate();settled()
    }
    fun clear() { level=0;snap() }
    fun draw(canvas:Canvas,x:Float,y:Float,size:Float,color:Int) {
        for(i in weights.indices) {
            val alpha=weights[i];if(alpha<=.001f) continue
            val index=i%3
            val offset=when(index) { 1->-size*.8f;2->size*.8f;else->0f }
            val faded=Color.argb((Color.alpha(color)*alpha).toInt(),Color.red(color),Color.green(color),Color.blue(color))
            drawChevron(canvas,paint,x,y+offset,size,if(i<3) 1 else -1,faded)
        }
    }
}
