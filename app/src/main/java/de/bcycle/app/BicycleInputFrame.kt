package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.math.roundToInt
import kotlin.math.sin

/** Focus decoration only. Native focus, editing, padding and validation remain with the field. */
internal object BicycleInputFrame {
    fun colors(accent:Int)=intArrayOf(BicycleAccentGradient.dark(accent),accent,BicycleAccentGradient.light(accent),accent,BicycleAccentGradient.dark(accent))
    fun positions()=floatArrayOf(0f,.5f-BicycleDesign.inputFocusHighlightHalf,.5f,.5f+BicycleDesign.inputFocusHighlightHalf,1f)
    fun background(view:View,tokens:ThemeTokens):GradientDrawable {
        val binding=(view.getTag(R.id.input_frame) as? Glow) ?: Glow(view).also { view.setTag(R.id.input_frame,it) }
        binding.accent=tokens.accent;binding.invalidateSelf()
        return Frame(view,tokens) { binding.refresh() }
    }
    private class Frame(view:View,tokens:ThemeTokens,private val refresh:()->Unit):GradientDrawable() {
        private val density=view.resources.displayMetrics.density
        private val radius=BicycleDesign.inputRadius*density
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE;strokeWidth=BicycleDesign.inputFocusStroke*density }
        private val stops=colors(tokens.accent)
        private val rect=RectF()
        private var active=false
        init {
            setColor(Color.TRANSPARENT);cornerRadius=radius
            setStroke(BicycleDesign.dp(view.context,BicycleDesign.controlBorder),Color.argb(
                (Color.alpha(tokens.foreground)*BicycleDesign.controlBorderAlpha).toInt(),Color.red(tokens.foreground),Color.green(tokens.foreground),Color.blue(tokens.foreground)))
        }
        override fun isStateful()=true
        override fun onStateChange(stateSet:IntArray):Boolean {
            val next=android.R.attr.state_focused in stateSet && android.R.attr.state_enabled in stateSet
            val changed=next!=active;active=next
            if(changed) { invalidateSelf();refresh() }
            return super.onStateChange(stateSet) || changed
        }
        override fun onBoundsChange(bounds:Rect) {
            super.onBoundsChange(bounds)
            val inset=paint.strokeWidth/2
            rect.set(bounds.left+inset,bounds.top+inset,bounds.right-inset,bounds.bottom-inset)
            if(bounds.width()>0 && bounds.height()>0) paint.shader=BicycleAccentGradient.shader(RectF(bounds),BicycleDesign.inputFocusAngle,stops,positions())
        }
        override fun draw(canvas:Canvas) {
            if(active) canvas.drawRoundRect(rect,radius,radius,paint) else super.draw(canvas)
        }
    }
    /** Draw in the scroll viewport, so row bounds cannot cut the outer glow in half. */
    private class Glow(private val field:View):Drawable(),View.OnAttachStateChangeListener {
        var accent=Color.TRANSPARENT
            set(value) { if(field!=value) { field=value;clearCache() } }
        private data class Blurs(val width:Int,val height:Int,val density:Float,val pad:Float,val near:Bitmap,val outer:Bitmap)
        private var cache:Blurs?=null
        private fun clearCache() { cache?.let { it.near.recycle();it.outer.recycle() };cache=null }
        private var host:ViewGroup?=null
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE }
        private val frame=RectF();private val bitmapBounds=RectF();private val inside=Path()
        private val fieldPosition=IntArray(2);private val hostPosition=IntArray(2)
        private val visible=Rect()
        init {
            field.addOnAttachStateChangeListener(this)
            field.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_->refresh() }
            if(field.isAttachedToWindow) attach()
        }
        private fun attach() {
            var parent=field.parent;var candidate:ViewGroup?=null
            while(parent is ViewGroup) {
                candidate=parent
                if(parent is ScrollView) break
                parent=parent.parent
            }
            if(candidate!==host) { host?.overlay?.remove(this);host=candidate;host?.overlay?.add(this) }
            refresh()
        }
        fun refresh() { if(!field.isFocused || !field.isEnabled) clearCache();host?.let { setBounds(0,0,it.width,it.height);it.invalidate() };invalidateSelf() }
        override fun onViewAttachedToWindow(v:View)=attach()
        override fun onViewDetachedFromWindow(v:View) { host?.overlay?.remove(this);host=null;clearCache() }
        private fun blurs(density:Float):Blurs {
            cache?.takeIf { it.width==field.width && it.height==field.height && it.density==density }?.let { return it }
            clearCache()
            val wide=BicycleDesign.inputFocusOuterBlur*density
            val estimate=(field.width+2*wide+8)*(field.height+2*wide+8)
            val scale=minOf(1f,sqrt(BicycleDesign.inputFocusMaxBlurPixels/estimate))
            val pad=ceil(wide+3/scale+BicycleDesign.inputFocusStroke*density)
            val source=Bitmap.createBitmap(ceil((field.width+2*pad)*scale).toInt(),ceil((field.height+2*pad)*scale).toInt(),Bitmap.Config.ARGB_8888)
            val canvas=Canvas(source);canvas.scale(scale,scale);canvas.translate(pad,pad)
            val stroke=BicycleDesign.inputFocusStroke*density
            val contour=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style=Paint.Style.STROKE;strokeWidth=stroke
                shader=BicycleAccentGradient.shader(RectF(0f,0f,field.width.toFloat(),field.height.toFloat()),BicycleDesign.inputFocusAngle,colors(accent),positions())
            }
            canvas.drawRoundRect(stroke/2,stroke/2,field.width-stroke/2,field.height-stroke/2,
                BicycleDesign.inputRadius*density,BicycleDesign.inputRadius*density,contour)
            try {
                val near=BicycleContourBlur.blur(source,BicycleDesign.inputFocusNearBlur*density*scale)
                try {
                    return Blurs(field.width,field.height,density,pad,near,BicycleContourBlur.blur(source,wide*scale)).also { cache=it }
                } catch(e:Exception) { near.recycle();throw e }
            } finally { source.recycle() }
        }
        override fun draw(canvas:Canvas) {
            val owner=host ?: return
            if(!field.isFocused || !field.isEnabled || !field.isShown || !field.getGlobalVisibleRect(visible)) return
            field.getLocationInWindow(fieldPosition);owner.getLocationInWindow(hostPosition)
            frame.set((fieldPosition[0]-hostPosition[0]).toFloat(),(fieldPosition[1]-hostPosition[1]).toFloat(),
                (fieldPosition[0]-hostPosition[0]+field.width).toFloat(),(fieldPosition[1]-hostPosition[1]+field.height).toFloat())
            val density=field.resources.displayMetrics.density
            val radius=BicycleDesign.inputRadius*density
            val blurred=blurs(density)
            inside.reset();inside.addRoundRect(frame,radius,radius,Path.Direction.CW)
            val saved=canvas.save()
            canvas.clipRect(0,0,owner.width,owner.height)
            (owner as? BicycleStickyScrollView)?.clipInputGlow(canvas)
            bitmapBounds.set(frame.left-blurred.pad,frame.top-blurred.pad,frame.right+blurred.pad,frame.bottom+blurred.pad)
            paint.blendMode=BlendMode.SCREEN
            val outer=canvas.save();canvas.clipOutPath(inside)
            paint.alpha=(255*BicycleDesign.inputFocusOuterAlpha).roundToInt()
            canvas.drawBitmap(blurred.outer,null,bitmapBounds,paint);canvas.restoreToCount(outer)
            paint.alpha=(255*BicycleDesign.inputFocusNearAlpha).roundToInt()
            canvas.drawBitmap(blurred.near,null,bitmapBounds,paint)
            canvas.restoreToCount(saved)
        }
        override fun setAlpha(alpha:Int)=Unit
        override fun setColorFilter(colorFilter:ColorFilter?)=Unit
        @Deprecated("Deprecated in Java") override fun getOpacity()=PixelFormat.TRANSLUCENT
    }
}
