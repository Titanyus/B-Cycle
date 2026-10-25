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
    private data class Margins(val start:Int,val end:Int,val top:Int,val bottom:Int) {
        constructor(p:ViewGroup.MarginLayoutParams):this(p.marginStart,p.marginEnd,p.topMargin,p.bottomMargin)
    }
    private data class Geometry(val header:Margins,val body:Margins?,var glow:Boolean=false)
    private val geometry=java.util.WeakHashMap<BicycleSectionHeader,Geometry>()
    private fun margins(view:View,values:Margins) {
        val p=view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        if(p.marginStart==values.start && p.marginEnd==values.end && p.topMargin==values.top && p.bottomMargin==values.bottom) return
        p.marginStart=values.start;p.marginEnd=values.end;p.topMargin=values.top;p.bottomMargin=values.bottom
        view.layoutParams=p
    }
    /** Separate box spacing from the next-header push boundary; never add geometry repeatedly. */
    fun spacing(header:BicycleSectionHeader):Boolean {
        if(header.expanded==null) return false
        val parent=header.parent as? ViewGroup ?: return false
        val p=header.layoutParams as? ViewGroup.MarginLayoutParams ?: return false
        val body=header.cardContent
        val b=body?.layoutParams as? ViewGroup.MarginLayoutParams
        val flat=BicycleAppearance.flat(header.context)
        var state=geometry[header]
        if(flat) {
            if(state?.glow==true) {
                margins(header,state.header)
                if(body!=null && state.body!=null) margins(body,state.body)
                state.glow=false
            }
            return false
        }
        if(state==null || !state.glow) {
            state=Geometry(Margins(p),b?.let(::Margins),true);geometry[header]=state
        }
        val inset=BicycleDesign.dp(header.context,BicycleDesign.sectionCardInset)
        val gap=BicycleDesign.dp(header.context,BicycleDesign.sectionCardGap)
        val bottom=BicycleDesign.dp(header.context,BicycleDesign.sectionCardBottomPadding)
        margins(header,Margins(inset-parent.paddingLeft,inset-parent.paddingRight,0,if(header.expanded==true) 0 else gap))
        if(body!=null && b!=null) margins(body,Margins(inset*2-parent.paddingLeft,inset*2-parent.paddingRight,
            maxOf(state.body?.top ?: 0,BicycleDesign.dp(header.context,BicycleDesign.sectionContentGap)),bottom+gap))
        return true
    }
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
                        val bottom=header.cardContent?.takeIf { it.visibility==View.VISIBLE }?.let { it.bottom+BicycleDesign.dp(root.context,BicycleDesign.sectionCardBottomPadding) } ?: header.bottom
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
