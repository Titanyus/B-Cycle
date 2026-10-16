package de.bcycle.app

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.TextView

/** Preserve native text, sizing, accessibility and compound padding; place its icon by the text. */
internal class BicycleTextIcon(private val view:TextView,private val icon:Drawable):Drawable() {
    override fun draw(canvas:Canvas) {
        val layout=view.layout
        val centered=view.gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.CENTER_HORIZONTAL
        val offset=if(centered && layout!=null && layout.lineCount>0) {
            if(view.layoutDirection==View.LAYOUT_DIRECTION_RTL) {
                val right=(0 until layout.lineCount).maxOf { layout.getLineRight(it) }
                view.compoundPaddingLeft+right+view.compoundDrawablePadding-(view.width-view.paddingRight-bounds.width())
            } else {
                val left=(0 until layout.lineCount).minOf { layout.getLineLeft(it) }
                view.compoundPaddingLeft+left-view.compoundDrawablePadding-bounds.width()-view.paddingLeft
            }
        } else 0f
        val saved=canvas.save();canvas.translate(offset,0f);icon.draw(canvas);canvas.restoreToCount(saved)
    }
    override fun onBoundsChange(bounds:android.graphics.Rect) { icon.setBounds(0,0,bounds.width(),bounds.height()) }
    override fun setAlpha(alpha:Int) { icon.alpha=alpha }
    override fun setColorFilter(colorFilter:ColorFilter?) { icon.colorFilter=colorFilter }
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
