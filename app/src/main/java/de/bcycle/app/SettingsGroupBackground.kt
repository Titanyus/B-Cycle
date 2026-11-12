package de.bcycle.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable

/** Original artwork spans the whole card width, top-aligned without stretching. */
internal class SettingsGroupBackground(private val context:Context,resource:Int):GradientDrawable() {
    private val bitmap=BitmapFactory.decodeResource(context.resources,resource,BitmapFactory.Options().apply { inScaled=false })
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val clip=Path()
    private val target=RectF()
    private var outline:GradientDrawable?=null
    private val radius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat()
    init { cornerRadius=radius }
    fun update(tokens:ThemeTokens) {
        setColor(tokens.tertiary)
        outline=BicycleComponents.controlBackground(context,tokens,android.graphics.Color.TRANSPARENT).apply { bounds=this@SettingsGroupBackground.bounds }
        invalidateSelf()
    }
    override fun onBoundsChange(bounds:Rect) {
        super.onBoundsChange(bounds)
        clip.reset();clip.addRoundRect(RectF(bounds),radius,radius,Path.Direction.CW)
        target.set(bounds.left.toFloat(),bounds.top.toFloat(),bounds.right.toFloat(),bounds.top+bitmap.height*bounds.width().toFloat()/bitmap.width)
        outline?.bounds=bounds
    }
    override fun draw(canvas:Canvas) {
        super.draw(canvas)
        val saved=canvas.save();canvas.clipPath(clip)
        canvas.drawBitmap(bitmap,null,target,paint)
        canvas.restoreToCount(saved)
        outline?.draw(canvas)
    }
}
