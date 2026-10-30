package de.bcycle.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable

/** Functional Bosch colors, independent of the four theme roles. MCSP 0x1809 is a slot index, 0 = OFF. */
internal object BoschModeColors {
    fun color(mode: Long?, names: List<String>? = null): Int? {
        if (mode == null || mode !in 1L..4L) return null
        if (names != null) {
            val name = names.getOrNull(mode.toInt())?.trim() ?: return null
            if (name.isEmpty() || name.equals("OFF", true)) return null
        }
        return when (mode) {
            1L -> 0xff80ff28.toInt()
            2L -> 0xff009dff.toInt()
            3L -> 0xffff36ba.toInt()
            else -> 0xffff3030.toInt()
        }
    }
}

/** Top-only cached shaders, additive light, 1.5 s mode fades and a visible-only low-battery breath. */
internal class BoschModeGlow(private val edgeWidth: Float,
                            private val clock: () -> Long = { android.os.SystemClock.uptimeMillis() }) : Drawable() {
    internal var modeColor: Int? = null; private set
    internal var modeShader: Shader? = null; private set
    private var fromShader: Shader? = null
    private var fromColor = Color.TRANSPARENT
    private var targetColor = Color.TRANSPARENT
    private var started = -1L
    internal var warningColor: Int? = null; private set
    internal var warningShader: Shader? = null; private set
    private var breathingStarted = 0L
    private val matrix = Matrix()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val frame = Runnable { invalidateSelf() }
    private val maximumDepth get()=bounds.height()/2f
    private val width get() = (edgeWidth * 1.5f).coerceAtMost(maximumDepth)
    private fun progress() = if (started < 0) 1f else ((clock() - started) / 1500f).coerceIn(0f, 1f)
    private fun currentColor(): Int {
        val t = progress()
        // Premultiplied interpolation keeps OFF fades from becoming muddy black gradients.
        val a = Color.alpha(fromColor) * (1 - t); val b = Color.alpha(targetColor) * t
        val alpha = a + b
        if (alpha == 0f) return Color.TRANSPARENT
        fun channel(f: (Int) -> Int) = ((f(fromColor) * a + f(targetColor) * b) / alpha).toInt()
        return Color.argb(alpha.toInt(), channel(Color::red), channel(Color::green), channel(Color::blue))
    }
    fun update(color: Int?, batteryPercent: Long? = null) {
        val warning = when (batteryPercent) {
            in 0L..10L -> 0xffff3030.toInt()
            in 11L..20L -> 0xffffb000.toInt()
            else -> null
        }
        if (warningColor != warning) {
            warningColor = warning; breathingStarted = clock()
            warningShader=shader(warning ?: Color.TRANSPARENT)
            invalidateSelf()
        }
        if (modeColor == color) return
        fromColor = currentColor(); targetColor = color ?: Color.TRANSPARENT
        modeColor = color; started = clock(); rebuild(); invalidateSelf()
    }
    override fun onBoundsChange(bounds: Rect) { rebuild() }
    private fun shader(color:Int):Shader? {
        if(Color.alpha(color)==0 || bounds.isEmpty) return null
        val alpha=BicycleDesign.modeGlowAlpha*Color.alpha(color)/255
        val colors=intArrayOf((color and 0xffffff) or (alpha shl 24),
            (color and 0xffffff) or ((alpha/2) shl 24),color and 0xffffff)
        return RadialGradient(0f,0f,1f,colors,floatArrayOf(0f,.4f,1f),Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply { setScale(bounds.width()/2f,width);postTranslate(bounds.exactCenterX(),bounds.top.toFloat()) })
        }
    }
    private fun rebuild() {
        fromShader=shader(fromColor);modeShader=shader(targetColor)
        warningShader=shader(warningColor ?: Color.TRANSPARENT)
    }
    override fun draw(canvas: Canvas) {
        paint.shader = null; paint.blendMode = null; paint.alpha = 255; paint.color = Color.BLACK
        canvas.drawRect(bounds, paint)
        val t = progress()
        // Cosine easing has zero velocity at both ends: 2 s in, 2 s out.
        val breath = if (warningColor == null) 0f else
            ((1.0 - kotlin.math.cos(((clock() - breathingStarted).coerceAtLeast(0) % 4000) * kotlin.math.PI / 2000)) / 2).toFloat()
        val animatedWidth = (width * (1 + .5f * breath)).coerceAtMost(maximumDepth)
        val save = canvas.save(); canvas.clipRect(bounds)
        paint.color = Color.WHITE; paint.blendMode = android.graphics.BlendMode.PLUS
        fun layer(shader:Shader?,fraction:Float) {
            if(shader==null || fraction<=0) return
            paint.alpha=(255*fraction).toInt()
            matrix.reset();matrix.setScale(bounds.width()/2f,animatedWidth)
            matrix.postTranslate(bounds.exactCenterX(),bounds.top.toFloat());shader.setLocalMatrix(matrix)
            paint.shader=shader
            repeat(BicycleDesign.ambientGlowPasses) {
                canvas.drawRect(bounds.left.toFloat(),bounds.top.toFloat(),bounds.right.toFloat(),bounds.top+animatedWidth,paint)
            }
        }
        layer(fromShader,(1-t)*(1-breath));layer(modeShader,t*(1-breath));layer(warningShader,breath)
        paint.shader = null; paint.blendMode = null; paint.alpha = 255; canvas.restoreToCount(save)
        unscheduleSelf(frame)
        if (t >= 1) fromShader = null
        if (isVisible && (t < 1 || warningColor != null))
            scheduleSelf(frame, android.os.SystemClock.uptimeMillis() + if (t < 1) 16 else 33)
    }
    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        if (!visible) unscheduleSelf(frame)
        val changed = super.setVisible(visible, restart)
        if (visible && changed) invalidateSelf()
        return changed
    }
    override fun setAlpha(alpha: Int) { /* Functional status controls its own intensity. */ }
    override fun setColorFilter(colorFilter: ColorFilter?) { /* Never tint Bosch colors with the theme. */ }
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.OPAQUE
}
