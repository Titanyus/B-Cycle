package de.bcycle.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.ReplacementSpan

/** Monochrome source symbols that scale with TextView's automatic font sizing. */
internal class SourceIconSpan(val source: String, context: android.content.Context, color: Int = Color.WHITE) : ReplacementSpan() {
    private val dish = context.getDrawable(when (source) {
        "Leistung" -> R.drawable.ic_lucide_zap
        "Pulsmesser" -> R.drawable.ic_lucide_heart
        "Wetter" -> R.drawable.ic_lucide_cloud_sun
        "Light" -> R.drawable.ic_lucide_lightbulb
        "Sunrise" -> R.drawable.ic_lucide_sunrise
        "Sunset" -> R.drawable.ic_lucide_sunset
        else -> R.drawable.ic_lucide_satellite_dish
    })!!.mutate().apply { setTint(color); setBounds(0, 0, 24, 24) }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.4f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int =
        (paint.textSize * 1.1f).toInt()
    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val size = paint.textSize
        canvas.save(); canvas.translate(x, y + paint.ascent() * .95f); canvas.scale(size / 24f, size / 24f)
        if (source == "Leistung" || source == "GPS" || source == "Pulsmesser" || source == "Wetter" || source == "Sunrise" || source == "Sunset" || source == "Light") {
            dish.draw(canvas)
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
