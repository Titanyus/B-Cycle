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
    private data class Key(val primary:Int,val strength:Double)
    private val palettes=android.util.LruCache<Key,ChromaticColor.Palette>(64)
    @Synchronized fun palette(primary:Int,chromatic:Double=BicycleInputStyle.defaultChromatic):ChromaticColor.Palette {
        val key=Key(primary,chromatic)
        return palettes.get(key) ?: ChromaticColor.palette(primary,BicycleDesign.inputFocusLighten.toDouble(),BicycleDesign.inputFocusDarken.toDouble(),chromatic).also { palettes.put(key,it) }
    }
    fun dark(accent:Int,chromatic:Double=BicycleInputStyle.defaultChromatic)=palette(accent,chromatic).dark
    fun light(accent:Int,chromatic:Double=BicycleInputStyle.defaultChromatic)=palette(accent,chromatic).light
    fun shader(bounds:RectF,angle:Int,colors:IntArray,positions:FloatArray?=null):LinearGradient {
        val radians=Math.toRadians(angle.toDouble());val dx=cos(radians).toFloat();val dy=sin(radians).toFloat()
        val reach=(bounds.width()*abs(dx)+bounds.height()*abs(dy))/2
        return LinearGradient(bounds.centerX()-reach*dx,bounds.centerY()-reach*dy,
            bounds.centerX()+reach*dx,bounds.centerY()+reach*dy,colors,positions,Shader.TileMode.CLAMP)
    }
    private data class FillKey(val primary:Int,val foreground:Int,val strength:Double)
    private val fillShades=android.util.LruCache<FillKey,Int>(64)
    @Synchronized fun fillDark(accent:Int,foreground:Int,chromatic:Double=BicycleInputStyle.defaultChromatic):Int {
        val key=FillKey(accent,foreground,chromatic)
        fillShades.get(key)?.let { return it }
        // Keep the existing text-contrast safeguard; only its color math changes.
        for(step in (BicycleDesign.inputFocusDarken*100).roundToInt() downTo 0) {
            val candidate=ChromaticColor.tone(accent,step/100.0,chromatic,false)
            if(ThemeValidator.contrast(foreground,candidate)>=ThemeValidator.minimumTextContrast) {
                fillShades.put(key,candidate);return candidate
            }
        }
        fillShades.put(key,accent);return accent
    }
    fun foreground(tokens:ThemeTokens):Int = if(ThemeValidator.contrast(tokens.foreground,tokens.primary)>=ThemeValidator.minimumTextContrast) tokens.foreground
        else if(ThemeValidator.contrast(Color.BLACK,tokens.primary)>=ThemeValidator.contrast(Color.WHITE,tokens.primary)) Color.BLACK else Color.WHITE
    fun fill(context:Context,tokens:ThemeTokens,foreground:Int=foreground(tokens),chromatic:Double=BicycleInputStyle.strength(context)):GradientDrawable = if(BicycleAppearance.flat(context)) GradientDrawable().apply { setColor(tokens.primary);cornerRadius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat() } else object:GradientDrawable() {
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect=RectF()
        private val border=GradientDrawable().apply { setColor(Color.TRANSPARENT) }
        private val radius=BicycleInputStyle.radius(context)
        init { setColor(tokens.primary);cornerRadius=radius;border.cornerRadius=radius }
        override fun setStroke(width:Int,color:Int) { super.setStroke(width,color);border.setStroke(width,color) }
        override fun onBoundsChange(bounds:Rect) {
            super.onBoundsChange(bounds);rect.set(bounds);border.bounds=bounds
            if(bounds.width()>0 && bounds.height()>0) paint.shader=shader(rect,BicycleDesign.accentFillAngle,BicycleInputStyle.resolve(BicycleInputStyle.fill,palette(tokens.primary,chromatic),fillDark(tokens.primary,foreground,chromatic)),BicycleInputStyle.fill.map { it.position }.toFloatArray())
        }
        override fun draw(canvas:Canvas) { paint.alpha=alpha;canvas.drawRoundRect(rect,radius,radius,paint);border.alpha=alpha;border.draw(canvas) }
    }
}
