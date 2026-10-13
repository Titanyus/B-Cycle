package de.bcycle.app

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Shared inset foreground; does not participate in measuring or replace graph backgrounds. */
internal class DashboardEditOutline(density: Float, color: Int) : Drawable() {
    internal val inset = 4f * density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.5f * density
        pathEffect = DashPathEffect(floatArrayOf(5f * density, 4f * density), 0f)
    }
    internal var color: Int
        get() = paint.color
        set(value) { paint.color = value; invalidateSelf() }
    override fun draw(canvas: Canvas) {
        if (bounds.width() > inset * 2 && bounds.height() > inset * 2)
            canvas.drawRect(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset, paint)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
