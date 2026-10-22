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
    init { setSingleLine();setHorizontallyScrolling(true);BicycleComponents.headerTypography(this) }
    private fun titleWidth()=paint.measureText(transformationMethod?.getTransformation(text,this)?.toString() ?: text.toString())
    private fun restart() {
        motion?.cancel();motion=null;offset=0f;alpha=1f
        val distance=(titleWidth()-(width-paddingLeft-paddingRight)).coerceAtLeast(0f)
        gravity=if(distance>0) Gravity.START or Gravity.CENTER_VERTICAL else Gravity.CENTER
        ellipsize=if(ValueAnimator.areAnimatorsEnabled()) null else TextUtils.TruncateAt.END
        if(distance<=0 || !isAttachedToWindow || windowVisibility!=VISIBLE || !ValueAnimator.areAnimatorsEnabled()) { invalidate();return }
        val timeline=TextScrollerMotion(distance,resources.displayMetrics.density)
        alpha=0f
        motion=ValueAnimator.ofFloat(0f,timeline.durationMs.toFloat()).apply {
            duration=kotlin.math.ceil(timeline.durationMs).toLong();repeatCount=ValueAnimator.INFINITE;interpolator=android.view.animation.LinearInterpolator()
            addUpdateListener {
                val elapsed=it.animatedValue as Float
                val frame=timeline.frame(elapsed.toDouble())
                offset=frame.offset;alpha=frame.alpha
                invalidate()
            };start()
        }
    }
    override fun onDraw(canvas:Canvas) {
        val textLayout=layout
        val available=width-compoundPaddingLeft-compoundPaddingRight
        if(textLayout==null || ellipsize!=null || titleWidth()<=available) {
            super.onDraw(canvas);return
        }
        // TextView.onDraw clips to its viewport again after translation, hiding the
        // unseen suffix. Draw its complete shaped layout inside one stationary clip.
        val checkpoint=canvas.save()
        canvas.clipRect(compoundPaddingLeft,0,width-compoundPaddingRight,height)
        val top=extendedPaddingTop+(height-extendedPaddingTop-extendedPaddingBottom-textLayout.height)/2f
        canvas.translate(compoundPaddingLeft-offset,top)
        paint.color=currentTextColor;paint.drawableState=drawableState
        textLayout.draw(canvas)
        canvas.restoreToCount(checkpoint)
    }
    override fun onTextChanged(text:CharSequence?,start:Int,lengthBefore:Int,lengthAfter:Int) { super.onTextChanged(text,start,lengthBefore,lengthAfter);if(isAttachedToWindow) post { restart() } }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { super.onSizeChanged(w,h,oldw,oldh);restart() }
    override fun onAttachedToWindow() { super.onAttachedToWindow();post { restart() } }
    override fun onWindowVisibilityChanged(visibility:Int) { super.onWindowVisibilityChanged(visibility);if(isAttachedToWindow) restart() }
    override fun onDetachedFromWindow() { motion?.cancel();motion=null;super.onDetachedFromWindow() }
}
