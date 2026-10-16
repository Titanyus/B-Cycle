package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.Drawable

/** Same cached radial-light approach as BoschModeGlow, localized to the bottom of one tile. */
internal class ZoneAmbientGlow(private val clock: () -> Long = { android.os.SystemClock.uptimeMillis() }) : Drawable() {
    private var from = Color.TRANSPARENT
    private var target = Color.TRANSPARENT
    private var started = -1500L
    private var cachedColor: Int? = null
    private var shader: Shader? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private val frame = Runnable { invalidateSelf() }
    internal fun currentColor(): Int {
        val t = ((clock() - started) / 1500f).coerceIn(0f, 1f)
        val eased = t * t * (3 - 2 * t)
        return ZoneGlowColors.mix(from, target, eased)
    }
    fun update(color: Int?) {
        val next = color ?: Color.TRANSPARENT
        if (next == target) return
        from = currentColor(); target = next; started = clock(); invalidateSelf()
    }
    override fun onBoundsChange(bounds: Rect) { cachedColor = null }
    override fun draw(canvas: Canvas) {
        val color = currentColor()
        if (cachedColor != color) {
            cachedColor = color
            val alpha = Color.alpha(color) * BicycleDesign.modeGlowAlpha / 255
            val rgb = color and 0xffffff
            shader = if (alpha == 0) null else RadialGradient(0f, 0f, 1f,
                intArrayOf(rgb or (alpha shl 24), rgb or ((alpha / 2) shl 24), rgb), floatArrayOf(0f, .4f, 1f), Shader.TileMode.CLAMP)
        }
        if (!bounds.isEmpty && shader != null) {
            matrix.setScale((bounds.width() * .5f - .5f).coerceAtLeast(.5f), bounds.height() * .4f)
            matrix.postTranslate(bounds.exactCenterX(), bounds.bottom.toFloat())
            shader!!.setLocalMatrix(matrix); paint.shader = shader
            val save = canvas.save(); canvas.clipRect(bounds)
            paint.blendMode=BlendMode.PLUS
            repeat(BicycleDesign.ambientGlowPasses) { canvas.drawRect(bounds, paint) }
            paint.blendMode=null; canvas.restoreToCount(save)
        }
        unscheduleSelf(frame)
        if (isVisible && clock() - started < 1500) scheduleSelf(frame, android.os.SystemClock.uptimeMillis() + 33)
    }
    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        if (!visible) unscheduleSelf(frame)
        val changed = super.setVisible(visible, restart)
        if (visible && changed) invalidateSelf()
        return changed
    }
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
