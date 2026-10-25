package de.bcycle.app

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup

/** A background only: no reparenting, copied header, extra scroll host or changed hit targets. */
internal object BicycleSectionCards {
    private val installed=java.util.WeakHashMap<ViewGroup,Boolean>()
    fun apply(root:View) {
        if(root !is ViewGroup) return
        val headers=(0 until root.childCount).map(root::getChildAt).filterIsInstance<BicycleSectionHeader>()
            .filter { it.expanded!=null }
        if(headers.isNotEmpty() && installed.put(root,true)==null) {
            val manager=headers.first().manager
            var original=root.background
            val cards=object:Drawable() {
                private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
                override fun draw(canvas:Canvas) {
                    if(BicycleAppearance.flat(root.context)) { original?.let { it.bounds=bounds;it.draw(canvas) };return }
                    val tokens=manager.active.tokens
                    // The section cards need a distinct page surface; dialog styling stays independent.
                    paint.color=tokens.secondary;canvas.drawRect(bounds,paint)
                    paint.color=tokens.tertiary
                    val radius=BicycleDesign.dp(root.context,BicycleDesign.sectionCardRadius).toFloat()
                    for(i in 0 until root.childCount) {
                        val header=root.getChildAt(i) as? BicycleSectionHeader ?: continue
                        if(header.visibility!=View.VISIBLE || header.expanded==null) continue
                        val bottom=header.cardContent?.takeIf { it.visibility==View.VISIBLE }?.bottom ?: header.bottom
                        canvas.drawRoundRect(header.left.toFloat(),header.top.toFloat(),header.right.toFloat(),bottom.toFloat(),radius,radius,paint)
                    }
                }
                override fun setAlpha(alpha:Int) = Unit
                override fun setColorFilter(filter:ColorFilter?) = Unit
                @Deprecated("Native drawable contract") override fun getOpacity()=PixelFormat.TRANSLUCENT
            }
            BicycleComponents.bindTheme(root,manager) {
                if(root.background!==cards) original=root.background
                root.background=cards
                root.invalidate()
            }
        }
        (0 until root.childCount).map(root::getChildAt).forEach(::apply)
    }
    /** Neutral body backgrounds must not cover the rounded shared card; keep Flat's original. */
    fun content(content:View,header:BicycleSectionHeader) {
        var original=content.background
        val transparent=object:Drawable() {
            override fun draw(canvas:Canvas) = Unit
            override fun setAlpha(alpha:Int) = Unit
            override fun setColorFilter(filter:ColorFilter?) = Unit
            @Deprecated("Native drawable contract") override fun getOpacity()=PixelFormat.TRANSPARENT
        }
        BicycleComponents.bindTheme(content,header.manager) {
            if(content.background!==transparent) original=content.background
            content.background=if(BicycleAppearance.flat(content.context)) original else transparent
            (header.parent as? View)?.invalidate()
        }
    }
}
