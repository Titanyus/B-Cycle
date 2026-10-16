package de.bcycle.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.ReplacementSpan

/** Monochrome source symbols that scale with TextView's automatic font sizing. */
internal class SourceIconSpan(val source: String, context: android.content.Context, color: Int = Color.WHITE) : ReplacementSpan() {
    private val icon = BicycleIcons.drawable(context,SensorIcons.forSource(source),color).apply { setBounds(0,0,24,24) }
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int =
        (paint.textSize * 1.1f).toInt()
    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val size = paint.textSize
        canvas.save(); canvas.translate(x, y + paint.ascent() * .95f); canvas.scale(size / 24f, size / 24f)
        icon.draw(canvas)
        canvas.restore()
    }
}
