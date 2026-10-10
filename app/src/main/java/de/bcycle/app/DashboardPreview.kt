package de.bcycle.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Layout diagram generated from the same row geometry as the actual dashboard. */
internal class DashboardPreview(private val template: DashboardTemplate) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas: Canvas) {
        canvas.save(); canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 100f, bounds.height() / 140f)
        val dataHeight = 130f * template.dataFraction
        val weights = template.rows.indices.sumOf { template.rowWeight(it).toDouble() }.toFloat()
        paint.style = Paint.Style.STROKE; paint.color = Color.WHITE; paint.strokeWidth = 2f
        canvas.drawRoundRect(4f, 4f, 96f, 136f, 3f, 3f, paint)
        var y = 5f
        template.rows.forEachIndexed { row, slots ->
            val height = dataHeight * template.rowWeight(row) / weights
            slots.indices.forEach { column ->
                val x = 5f + column * 90f / slots.size
                canvas.drawRect(x, y, x + 90f / slots.size, y + height, paint)
                // Small central strokes denote values without unreadable miniature text.
                canvas.drawLine(x + 90f / slots.size * .35f, y + height / 2, x + 90f / slots.size * .65f, y + height / 2, paint)
            }
            y += height
        }
        paint.style = Paint.Style.FILL; paint.color = Color.argb(55, 255, 255, 255)
        canvas.drawRect(5f, y, 95f, 135f, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.WHITE; paint.strokeWidth = 2f
        val cy = (y + 135f) / 2
        canvas.drawLine(20f, cy + 5f, 42f, cy - 5f, paint)
        canvas.drawLine(42f, cy - 5f, 66f, cy + 5f, paint)
        canvas.drawLine(66f, cy + 5f, 80f, cy - 3f, paint)
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth() = 100
    override fun getIntrinsicHeight() = 140
}
