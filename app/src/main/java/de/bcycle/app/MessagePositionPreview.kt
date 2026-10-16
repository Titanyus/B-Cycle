package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.Drawable

/** Same 100 × 140 screen outline and line weight as DashboardPreview. */
internal class MessagePositionPreview(private val position: MessagePosition): Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas: Canvas) {
        canvas.save();canvas.translate(bounds.left.toFloat(),bounds.top.toFloat())
        canvas.scale(bounds.width()/100f,bounds.height()/140f)
        paint.color=Color.WHITE;paint.style=Paint.Style.STROKE;paint.strokeWidth=2f
        canvas.drawRoundRect(4f,4f,96f,136f,3f,3f,paint)
        canvas.drawLine(5f,48f,95f,48f,paint);canvas.drawLine(5f,88f,95f,88f,paint)
        canvas.drawLine(50f,5f,50f,48f,paint)
        canvas.drawLine(16f,28f,37f,28f,paint);canvas.drawLine(63f,28f,84f,28f,paint)
        canvas.drawLine(32f,68f,68f,68f,paint)
        canvas.drawLine(15f,118f,38f,100f,paint);canvas.drawLine(38f,100f,70f,120f,paint)
        val top=when(position) { MessagePosition.BOTTOM->96f;MessagePosition.CENTER->54f;MessagePosition.TOP->8f }
        paint.style=Paint.Style.FILL;paint.color=Color.argb(55,255,255,255)
        canvas.drawRoundRect(9f,top,91f,top+32f,3f,3f,paint)
        paint.style=Paint.Style.STROKE;paint.color=Color.WHITE;paint.strokeWidth=3f
        canvas.drawRoundRect(9f,top,91f,top+32f,3f,3f,paint)
        paint.strokeWidth=2f;canvas.drawLine(17f,top+10,72f,top+10,paint);canvas.drawLine(17f,top+21,82f,top+21,paint)
        canvas.restore()
    }
    override fun setAlpha(alpha:Int) { paint.alpha=alpha;invalidateSelf() }
    override fun setColorFilter(filter:ColorFilter?) { paint.colorFilter=filter;invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth()=100
    override fun getIntrinsicHeight()=140
}
