package de.bcycle.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Small scalable eye/trash icons, without another icon library. */
internal class RouteActionIcon(private val trash: Boolean, private val selected: Boolean = false) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (trash) Color.rgb(235, 90, 90) else Color.WHITE
        style = Paint.Style.STROKE; strokeWidth = 1.8f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    override fun draw(canvas: Canvas) {
        canvas.save(); canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        if (trash) {
            canvas.drawLine(5f, 7f, 19f, 7f, paint)
            canvas.drawLine(9f, 4f, 15f, 4f, paint)
            canvas.drawLine(9f, 4f, 9f, 7f, paint); canvas.drawLine(15f, 4f, 15f, 7f, paint)
            path.reset(); path.moveTo(7f, 7f); path.lineTo(8f, 20f); path.lineTo(16f, 20f); path.lineTo(17f, 7f)
            canvas.drawPath(path, paint)
            canvas.drawLine(10f, 10f, 10.5f, 17f, paint); canvas.drawLine(14f, 10f, 13.5f, 17f, paint)
        } else {
            path.reset(); path.moveTo(3f, 12f); path.cubicTo(8f, 4f, 16f, 4f, 21f, 12f)
            path.cubicTo(16f, 20f, 8f, 20f, 3f, 12f); canvas.drawPath(path, paint)
            canvas.drawCircle(12f, 12f, 3f, paint)
            if (!selected) canvas.drawLine(4f, 3f, 20f, 21f, paint)
        }
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth() = 24
    override fun getIntrinsicHeight() = 24
}
