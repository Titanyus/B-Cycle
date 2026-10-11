package de.bcycle.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable

/** Small white vector symbols, shared by route rows and import actions. */
internal enum class RouteSymbol { IMPORT, ROUTE, RIDE, FIT, GPX, LOCAL, CLOUD, PENDING, ERROR, DATE, DISTANCE, TIME, MORE, DOWN }

internal class RouteSymbolDrawable(private val symbol: RouteSymbol) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = 1.7f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    override fun draw(canvas: Canvas) {
        canvas.save(); canvas.translate(bounds.left.toFloat(), bounds.top.toFloat()); canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        paint.style = Paint.Style.STROKE
        fun line(x: Float, y: Float, x2: Float, y2: Float) = canvas.drawLine(x, y, x2, y2, paint)
        when (symbol) {
            RouteSymbol.IMPORT -> {
                line(12f, 3f, 12f, 15f); line(7f, 10f, 12f, 15f); line(12f, 15f, 17f, 10f)
                path.reset(); path.moveTo(4f, 15f); path.lineTo(4f, 20f); path.lineTo(20f, 20f); path.lineTo(20f, 15f); canvas.drawPath(path, paint)
            }
            RouteSymbol.ROUTE -> {
                canvas.drawCircle(5f, 19f, 2.5f, paint)
                path.reset(); path.moveTo(8f, 19f); path.lineTo(14f, 19f); path.cubicTo(21f, 19f, 21f, 12f, 14f, 12f)
                path.lineTo(9f, 12f); path.cubicTo(4f, 12f, 4f, 6f, 9f, 6f); path.lineTo(12f, 6f); canvas.drawPath(path, paint)
                canvas.drawCircle(17f, 5f, 3f, paint); line(15f, 7f, 17f, 10f); line(17f, 10f, 19f, 7f)
            }
            RouteSymbol.RIDE -> {
                line(5f, 3f, 5f, 21f); canvas.drawRect(5f, 3f, 20f, 14f, paint)
                paint.style = Paint.Style.FILL
                for (row in 0..1) for (col in 0..2) if ((row + col) % 2 == 0)
                    canvas.drawRect(5f + col * 5, 3f + row * 5.5f, 10f + col * 5, 8.5f + row * 5.5f, paint)
            }
            RouteSymbol.FIT, RouteSymbol.GPX -> {
                path.reset(); path.moveTo(5f, 3f); path.lineTo(15f, 3f); path.lineTo(20f, 8f); path.lineTo(20f, 21f); path.lineTo(5f, 21f); path.close()
                canvas.drawPath(path, paint); line(15f, 3f, 15f, 8f); line(15f, 8f, 20f, 8f)
                paint.style = Paint.Style.FILL; paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = 7f; paint.textAlign = Paint.Align.CENTER
                canvas.drawText(symbol.name, 12.5f, 16.5f, paint)
            }
            RouteSymbol.LOCAL -> {
                canvas.drawRoundRect(4f, 3f, 20f, 21f, 1.5f, 1.5f, paint)
                canvas.drawRect(8f, 3f, 16f, 9f, paint); canvas.drawRect(8f, 14f, 16f, 21f, paint)
            }
            RouteSymbol.CLOUD, RouteSymbol.PENDING, RouteSymbol.ERROR -> {
                path.reset(); path.moveTo(5f, 19f); path.cubicTo(0f, 18f, 2f, 11f, 6f, 11f)
                path.cubicTo(5f, 3f, 17f, 3f, 18f, 11f); path.cubicTo(24f, 10f, 24f, 19f, 19f, 19f)
                canvas.drawPath(path, paint)
                when (symbol) {
                    RouteSymbol.CLOUD -> { line(8f, 16f, 11f, 19f); line(11f, 19f, 16f, 13f) }
                    RouteSymbol.PENDING -> { line(12f, 20f, 12f, 12f); line(9f, 15f, 12f, 12f); line(12f, 12f, 15f, 15f) }
                    else -> { line(12f, 12f, 12f, 16f); paint.style = Paint.Style.FILL; canvas.drawCircle(12f, 19f, 1f, paint) }
                }
            }
            RouteSymbol.DATE -> {
                canvas.drawRoundRect(3f, 5f, 21f, 21f, 2f, 2f, paint); line(3f, 10f, 21f, 10f)
                line(8f, 3f, 8f, 7f); line(16f, 3f, 16f, 7f); line(7f, 14f, 10f, 14f); line(14f, 17f, 17f, 17f)
            }
            RouteSymbol.DISTANCE -> {
                path.reset(); path.moveTo(3f, 18f); path.lineTo(9f, 12f); path.lineTo(13f, 15f); path.lineTo(21f, 6f); canvas.drawPath(path, paint)
                line(15f, 6f, 21f, 6f); line(21f, 6f, 21f, 12f)
            }
            RouteSymbol.TIME -> { canvas.drawCircle(12f, 12f, 9f, paint); line(12f, 6f, 12f, 12f); line(12f, 12f, 16f, 14f) }
            RouteSymbol.MORE -> { paint.style = Paint.Style.FILL; for (y in listOf(4f, 12f, 20f)) canvas.drawCircle(12f, y, 1.4f, paint) }
            RouteSymbol.DOWN -> { line(6f, 9f, 12f, 15f); line(12f, 15f, 18f, 9f) }
        }
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity() = PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth() = 24
    override fun getIntrinsicHeight() = 24
}
