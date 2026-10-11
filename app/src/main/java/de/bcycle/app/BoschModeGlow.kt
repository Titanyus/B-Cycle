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

/** Cached shaders, additive light, and a bounded 1.5 s fade. No animation runs while idle. */
internal class BoschModeGlow(private val edgeWidth: Float,
                            private val clock: () -> Long = { android.os.SystemClock.uptimeMillis() }) : Drawable() {
    internal var modeColor: Int? = null; private set
    internal var leftShader: Shader? = null; private set
    private var rightShader: Shader? = null
    private var fromLeft: Shader? = null
    private var fromRight: Shader? = null
    private var fromColor = Color.TRANSPARENT
    private var targetColor = Color.TRANSPARENT
    private var started = -1L
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val frame = Runnable { invalidateSelf() }
    private val width get() = (edgeWidth * 1.2f).coerceAtMost(bounds.width() / 2f)
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
    fun update(color: Int?) {
        if (modeColor == color) return
        fromColor = currentColor(); targetColor = color ?: Color.TRANSPARENT
        modeColor = color; started = clock(); rebuild(); invalidateSelf()
    }
    override fun onBoundsChange(bounds: Rect) { rebuild() }
    private fun edges(color: Int): Pair<Shader?, Shader?> {
        if (Color.alpha(color) == 0 || bounds.isEmpty) return null to null
        val alpha = BicycleDesign.modeGlowAlpha * Color.alpha(color) / 255
        val colors = intArrayOf((color and 0x00ffffff) or (alpha shl 24),
            (color and 0x00ffffff) or ((alpha / 2) shl 24), color and 0x00ffffff)
        fun edge(x: Float): Shader = RadialGradient(0f, 0f, 1f, colors, floatArrayOf(0f, .4f, 1f), Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply { setScale(width, bounds.height() / 2f); postTranslate(x, bounds.exactCenterY()) })
        }
        return edge(bounds.left.toFloat()) to edge(bounds.right.toFloat())
    }
    private fun rebuild() {
        edges(fromColor).let { fromLeft = it.first; fromRight = it.second }
        edges(targetColor).let { leftShader = it.first; rightShader = it.second }
    }
    override fun draw(canvas: Canvas) {
        paint.shader = null; paint.blendMode = null; paint.alpha = 255; paint.color = Color.BLACK
        canvas.drawRect(bounds, paint)
        val t = progress()
        val save = canvas.save(); canvas.clipRect(bounds)
        paint.color = Color.WHITE; paint.blendMode = android.graphics.BlendMode.PLUS
        fun layer(left: Shader?, right: Shader?, fraction: Float) {
            if (left == null || fraction <= 0) return
            paint.alpha = (255 * fraction).toInt()
            // Three light contributions = three times the former intensity, capped by the display.
            repeat(3) {
                paint.shader = left
                canvas.drawRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.left + width, bounds.bottom.toFloat(), paint)
                paint.shader = right
                canvas.drawRect(bounds.right - width, bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), paint)
            }
        }
        layer(fromLeft, fromRight, 1 - t); layer(leftShader, rightShader, t)
        paint.shader = null; paint.blendMode = null; paint.alpha = 255; canvas.restoreToCount(save)
        unscheduleSelf(frame)
        if (t < 1 && isVisible) scheduleSelf(frame, android.os.SystemClock.uptimeMillis() + 16)
        else { fromLeft = null; fromRight = null }
    }
    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        if (!visible) unscheduleSelf(frame)
        return super.setVisible(visible, restart)
    }
    override fun setAlpha(alpha: Int) { /* Functional status controls its own intensity. */ }
    override fun setColorFilter(colorFilter: ColorFilter?) { /* Never tint Bosch colors with the theme. */ }
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.OPAQUE
}

internal object ModeGlowSettings {
    const val key = "modeGlowEnabled"
    fun enabled(preferences: android.content.SharedPreferences) = preferences.getBoolean(key, false)
    fun save(preferences: android.content.SharedPreferences, enabled: Boolean) { preferences.edit().putBoolean(key, enabled).apply() }
}
