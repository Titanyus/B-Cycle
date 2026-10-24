package de.bcycle.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.widget.SeekBar

/** Native discrete slider semantics; highlight runs from the question's neutral starting point. */
internal class BicycleRatingSlider(context:Context,themes:ThemeManager,internal val origin:Int,steps:Int):SeekBar(context) {
    private val track=object:Drawable() {
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap=Paint.Cap.ROUND;strokeWidth=BicycleDesign.dp(context,3).toFloat()
        }
        var tokens=themes.active.tokens
        private var opacity=255
        override fun draw(canvas:Canvas) {
            fun x(fraction:Float)=bounds.left+bounds.width()*(if(layoutDirection==android.view.View.LAYOUT_DIRECTION_RTL) 1-fraction else fraction)
            val y=bounds.exactCenterY()
            paint.color=(tokens.foreground and 0xffffff) or 0x55000000;paint.alpha=85*opacity/255
            canvas.drawLine(x(0f),y,x(1f),y,paint)
            paint.color=tokens.primary;paint.alpha=Color.alpha(tokens.primary)*opacity/255
            canvas.drawLine(x(origin.toFloat()/max),y,x(level/10000f),y,paint)
        }
        override fun onLevelChange(level:Int):Boolean { invalidateSelf();return true }
        override fun onLayoutDirectionChanged(layoutDirection:Int):Boolean { invalidateSelf();return true }
        override fun setAlpha(alpha:Int) { opacity=alpha;invalidateSelf() }
        override fun setColorFilter(filter:ColorFilter?) { paint.colorFilter=filter;invalidateSelf() }
        @Deprecated("Drawable opacity") override fun getOpacity()=PixelFormat.TRANSLUCENT
    }
    init {
        max=steps;progressTintList=null;progressBackgroundTintList=null;progressDrawable=track
        splitTrack=false
        BicycleComponents.bindTheme(this,themes) {
            track.tokens=it;track.invalidateSelf()
            thumbTintList=ColorStateList.valueOf(it.primary)
            tickMarkTintList=ColorStateList.valueOf((it.foreground and 0xffffff) or 0x55000000)
        }
    }
}
