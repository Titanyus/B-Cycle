package de.bcycle.app

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Lightweight category placeholders; replace with final artwork without changing profile identities. */
internal class BikePreview(private val category: BikeCategory) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    override fun draw(canvas: Canvas) {
        val save = canvas.save()
        val scale = minOf(bounds.width() / 84f, bounds.height() / 54f)
        canvas.translate(bounds.centerX() - 42f * scale, bounds.centerY() - 27f * scale); canvas.scale(scale, scale)
        val radius = if (category == BikeCategory.MOUNTAIN) 13f else 12f
        canvas.drawCircle(18f, 36f, radius, paint); canvas.drawCircle(66f, 36f, radius, paint)
        val upright = category == BikeCategory.CITY || category == BikeCategory.TREKKING
        val front = if (upright) 53f else 49f
        val frame = Path().apply { moveTo(18f,36f);lineTo(31f,17f);lineTo(42f,36f);close();moveTo(31f,17f);lineTo(front,17f);lineTo(42f,36f);moveTo(front-2,10f);lineTo(66f,36f) }
        canvas.drawPath(frame,paint);canvas.drawLine(26f,13f,36f,13f,paint);canvas.drawLine(31f,13f,31f,17f,paint)
        if (category == BikeCategory.ROAD || category == BikeCategory.GRAVEL) canvas.drawArc(47f,5f,61f,20f,-100f,210f,false,paint)
        else canvas.drawLine(front-2,10f,front+10,if(upright) 6f else 10f,paint)
        if (category == BikeCategory.TREKKING || category == BikeCategory.CITY) canvas.drawLine(5f,19f,24f,19f,paint)
        canvas.restoreToCount(save)
    }
    override fun setTint(tintColor: Int) { paint.color = tintColor;invalidateSelf() }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha;invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter;invalidateSelf() }
    @Deprecated("Drawable opacity") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
