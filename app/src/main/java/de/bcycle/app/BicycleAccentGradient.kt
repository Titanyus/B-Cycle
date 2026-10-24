package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.content.Context
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Shared theme-derived shades and angle convention: 0° right, positive clockwise. */
internal object BicycleAccentGradient {
    fun mix(accent:Int,target:Int,fraction:Float)=Color.rgb(
        (Color.red(accent)+(Color.red(target)-Color.red(accent))*fraction).roundToInt(),
        (Color.green(accent)+(Color.green(target)-Color.green(accent))*fraction).roundToInt(),
        (Color.blue(accent)+(Color.blue(target)-Color.blue(accent))*fraction).roundToInt())
    fun dark(accent:Int)=mix(accent,Color.BLACK,BicycleDesign.inputFocusDarken)
    fun light(accent:Int)=mix(accent,Color.WHITE,BicycleDesign.inputFocusLighten)
    fun shader(bounds:RectF,angle:Int,colors:IntArray,positions:FloatArray?=null):LinearGradient {
        val radians=Math.toRadians(angle.toDouble());val dx=cos(radians).toFloat();val dy=sin(radians).toFloat()
        val reach=(bounds.width()*abs(dx)+bounds.height()*abs(dy))/2
        return LinearGradient(bounds.centerX()-reach*dx,bounds.centerY()-reach*dy,
            bounds.centerX()+reach*dx,bounds.centerY()+reach*dy,colors,positions,Shader.TileMode.CLAMP)
    }
    fun fillDark(accent:Int,foreground:Int):Int {
        // Bright accents often need black text: avoid darkening them past its readable range.
        for(step in (BicycleDesign.inputFocusDarken*100).roundToInt() downTo 0) {
            val candidate=mix(accent,Color.BLACK,step/100f)
            if(ThemeValidator.contrast(foreground,candidate)>=ThemeValidator.minimumTextContrast) return candidate
        }
        return accent
    }
    fun foreground(tokens:ThemeTokens):Int = if(ThemeValidator.contrast(tokens.foreground,tokens.accent)>=ThemeValidator.minimumTextContrast) tokens.foreground
        else if(ThemeValidator.contrast(Color.BLACK,tokens.accent)>=ThemeValidator.contrast(Color.WHITE,tokens.accent)) Color.BLACK else Color.WHITE
    fun fill(context:Context,tokens:ThemeTokens,foreground:Int=foreground(tokens)):GradientDrawable = object:GradientDrawable() {
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect=RectF()
        private val border=GradientDrawable().apply { setColor(Color.TRANSPARENT) }
        private val radius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat()
        init { setColor(tokens.accent);cornerRadius=radius;border.cornerRadius=radius }
        override fun setStroke(width:Int,color:Int) { super.setStroke(width,color);border.setStroke(width,color) }
        override fun onBoundsChange(bounds:Rect) {
            super.onBoundsChange(bounds);rect.set(bounds);border.bounds=bounds
            if(bounds.width()>0 && bounds.height()>0) paint.shader=shader(rect,BicycleDesign.accentFillAngle,intArrayOf(tokens.accent,fillDark(tokens.accent,foreground)))
        }
        override fun draw(canvas:Canvas) { paint.alpha=alpha;canvas.drawRoundRect(rect,radius,radius,paint);border.alpha=alpha;border.draw(canvas) }
    }
}
