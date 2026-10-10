package de.bcycle.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.ReplacementSpan

/** Monochrome source symbols that scale with TextView's automatic font sizing. */
internal class SourceIconSpan(val source: String) : ReplacementSpan() {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 1.4f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int =
        (paint.textSize * 1.1f).toInt()
    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val size = paint.textSize
        canvas.save(); canvas.translate(x, y + paint.ascent() * .95f); canvas.scale(size / 24f, size / 24f)
        if (source == "GPS") {
            canvas.drawCircle(12f, 12f, 10f, stroke)
            canvas.drawOval(7f, 2f, 17f, 22f, stroke)
            canvas.drawLine(2f, 12f, 22f, 12f, stroke)
            canvas.drawArc(3f, 6f, 21f, 12f, 0f, 180f, false, stroke)
            canvas.drawArc(3f, 12f, 21f, 18f, 180f, 180f, false, stroke)
        } else {
            canvas.drawCircle(5f, 17f, 4f, stroke); canvas.drawCircle(19f, 17f, 4f, stroke)
            canvas.drawLine(5f, 17f, 9f, 9f, stroke); canvas.drawLine(9f, 9f, 13f, 17f, stroke)
            canvas.drawLine(5f, 17f, 13f, 17f, stroke); canvas.drawLine(13f, 17f, 17f, 9f, stroke)
            canvas.drawLine(9f, 9f, 17f, 9f, stroke); canvas.drawLine(19f, 17f, 16f, 6f, stroke)
            canvas.drawLine(16f, 6f, 20f, 6f, stroke); canvas.drawLine(7f, 6f, 11f, 6f, stroke)
            canvas.drawLine(9f, 6f, 9f, 9f, stroke)
        }
        canvas.restore()
    }
}
