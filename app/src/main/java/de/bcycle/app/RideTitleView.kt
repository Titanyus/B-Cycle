package de.bcycle.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.text.TextUtils
import android.view.Gravity
import android.widget.TextView

/** Fixed typography: long titles move slowly, fade at the end and start again. */
internal class RideTitleView(context:Context):TextView(context) {
    private var motion:ValueAnimator?=null
    private var offset=0f
    init { textSize=BicycleDesign.headingSp;typeface=resources.getFont(R.font.bebas_neue);setSingleLine();setHorizontallyScrolling(true) }
    private fun restart() {
        motion?.cancel();motion=null;offset=0f;alpha=1f
        val distance=(paint.measureText(text.toString())-(width-paddingLeft-paddingRight)).coerceAtLeast(0f)
        gravity=if(distance>0) Gravity.START or Gravity.CENTER_VERTICAL else Gravity.CENTER
        ellipsize=if(ValueAnimator.areAnimatorsEnabled()) null else TextUtils.TruncateAt.END
        if(distance<=0 || !isAttachedToWindow || windowVisibility!=VISIBLE || !ValueAnimator.areAnimatorsEnabled()) { invalidate();return }
        val travel=distance/(30*resources.displayMetrics.density)*1000
        val total=1500f+travel+800f+700f
        motion=ValueAnimator.ofFloat(0f,total).apply {
            duration=total.toLong();repeatCount=ValueAnimator.INFINITE;interpolator=android.view.animation.LinearInterpolator()
            addUpdateListener {
                val elapsed=it.animatedValue as Float
                offset=distance*((elapsed-1500)/travel).coerceIn(0f,1f)
                alpha=1-((elapsed-1500-travel-800)/700).coerceIn(0f,1f)
                invalidate()
            };start()
        }
    }
    override fun onDraw(canvas:Canvas) {
        val checkpoint=canvas.save();canvas.clipRect(paddingLeft,0,width-paddingRight,height);canvas.translate(-offset,0f)
        super.onDraw(canvas);canvas.restoreToCount(checkpoint)
    }
    override fun onTextChanged(text:CharSequence?,start:Int,lengthBefore:Int,lengthAfter:Int) { super.onTextChanged(text,start,lengthBefore,lengthAfter);if(isAttachedToWindow) post { restart() } }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { super.onSizeChanged(w,h,oldw,oldh);restart() }
    override fun onAttachedToWindow() { super.onAttachedToWindow();post { restart() } }
    override fun onWindowVisibilityChanged(visibility:Int) { super.onWindowVisibilityChanged(visibility);if(isAttachedToWindow) restart() }
    override fun onDetachedFromWindow() { motion?.cancel();motion=null;super.onDetachedFromWindow() }
}
