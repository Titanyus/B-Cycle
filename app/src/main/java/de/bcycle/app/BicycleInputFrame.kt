package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.view.View

/** Focus decoration only. Native focus, editing, padding and validation remain with the field. */
internal object BicycleInputFrame {
    fun background(view:View,tokens:ThemeTokens):GradientDrawable {
        return Frame(view,tokens)
    }
    /** Plain active contour; native focus and selection state stay with the owner. */
    fun decorate(view:View,tokens:ThemeTokens,active:Boolean,fill:Drawable?=null,radiusDp:Int=BicycleDesign.inputRadius,verticalInsetFraction:Float=0f):Drawable {
        val frame=Frame(view,tokens,active,BicycleDesign.dp(view.context,radiusDp).toFloat())
        val surface=if(fill==null) frame else android.graphics.drawable.LayerDrawable(arrayOf(fill,frame))
        return if(verticalInsetFraction==0f) surface else InsetDrawable(surface,0f,verticalInsetFraction,0f,verticalInsetFraction)
    }
    fun selectionBackground(view:View,tokens:ThemeTokens,selected:Boolean):Drawable {
        val fill=GradientDrawable().apply {
            setColor(if(selected) tokens.primary else tokens.secondary)
            cornerRadius=BicycleDesign.dp(view.context,BicycleDesign.inputRadius).toFloat()
        }
        if(!selected) return fill
        val outline=GradientDrawable().apply {
            setColor(Color.TRANSPARENT);cornerRadius=BicycleDesign.dp(view.context,BicycleDesign.inputRadius).toFloat()
            setStroke(BicycleDesign.linePixels(view.context,BicycleDesign.activeControlStroke),tokens.primary)
        }
        return android.graphics.drawable.LayerDrawable(arrayOf(fill,outline))
    }
    fun header(view:View,tokens:ThemeTokens,role:ThemeRole=ThemeRole.PRIMARY):Drawable {
        return android.graphics.drawable.ColorDrawable(tokens.color(role))
    }
    private class Frame(view:View,tokens:ThemeTokens,private val selected:Boolean?=null,private val radius:Float=BicycleDesign.dp(view.context,BicycleDesign.inputRadius).toFloat()):GradientDrawable() {
        private val density=view.resources.displayMetrics.density
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE;strokeWidth=BicycleDesign.activeControlStroke*density }
        private val primary=tokens.primary
        private val rect=RectF()
        private var active=selected==true && view.isEnabled
        init {
            setColor(Color.TRANSPARENT);cornerRadius=radius
            setStroke(BicycleDesign.dp(view.context,BicycleDesign.controlBorder),Color.argb(
                (Color.alpha(tokens.foreground)*BicycleDesign.controlBorderAlpha).toInt(),Color.red(tokens.foreground),Color.green(tokens.foreground),Color.blue(tokens.foreground)))
        }
        override fun getConstantState():ConstantState?=null
        override fun isStateful()=true
        override fun onStateChange(stateSet:IntArray):Boolean {
            val next=(selected ?: (android.R.attr.state_focused in stateSet)) && android.R.attr.state_enabled in stateSet
            val changed=next!=active;active=next
            if(changed) invalidateSelf()
            return super.onStateChange(stateSet) || changed
        }
        override fun onBoundsChange(bounds:Rect) {
            super.onBoundsChange(bounds)
            val inset=paint.strokeWidth/2
            rect.set(bounds.left+inset,bounds.top+inset,bounds.right-inset,bounds.bottom-inset)
            paint.color=primary
        }
        override fun draw(canvas:Canvas) {
            if(active) canvas.drawRoundRect(rect,radius,radius,paint) else super.draw(canvas)
        }
    }
}
