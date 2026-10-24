package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/** Focus decoration only. Native focus, editing, padding and validation remain with the field. */
internal object BicycleInputFrame {
    fun colors(accent:Int):IntArray {
        fun mix(target:Int,fraction:Float)=Color.rgb(
            (Color.red(accent)+(Color.red(target)-Color.red(accent))*fraction).roundToInt(),
            (Color.green(accent)+(Color.green(target)-Color.green(accent))*fraction).roundToInt(),
            (Color.blue(accent)+(Color.blue(target)-Color.blue(accent))*fraction).roundToInt())
        val dark=mix(Color.BLACK,BicycleDesign.inputFocusDarken)
        return intArrayOf(dark,accent,mix(Color.WHITE,BicycleDesign.inputFocusLighten),accent,dark)
    }
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
            val angle=Math.toRadians(BicycleDesign.inputFocusAngle.toDouble())
            val dx=cos(angle).toFloat();val dy=sin(angle).toFloat()
            val reach=(bounds.width()*dx+bounds.height()*dy)/2
            if(reach>0) paint.shader=LinearGradient(rect.centerX()-reach*dx,rect.centerY()-reach*dy,
                rect.centerX()+reach*dx,rect.centerY()+reach*dy,stops,null,Shader.TileMode.CLAMP)
        }
        override fun draw(canvas:Canvas) {
            if(active) canvas.drawRoundRect(rect,radius,radius,paint) else super.draw(canvas)
        }
    }
    /** Draw in the scroll viewport, so row bounds cannot cut the outer glow in half. */
    private class Glow(private val field:View):Drawable(),View.OnAttachStateChangeListener {
        var accent=Color.TRANSPARENT
        private var host:ViewGroup?=null
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE }
        private val frame=RectF();private val ring=RectF();private val inside=Path()
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
        fun refresh() { host?.let { setBounds(0,0,it.width,it.height);it.invalidate() };invalidateSelf() }
        override fun onViewAttachedToWindow(v:View)=attach()
        override fun onViewDetachedFromWindow(v:View) { host?.overlay?.remove(this);host=null }
        override fun draw(canvas:Canvas) {
            val owner=host ?: return
            if(!field.isFocused || !field.isEnabled || !field.isShown || !field.getGlobalVisibleRect(visible)) return
            field.getLocationInWindow(fieldPosition);owner.getLocationInWindow(hostPosition)
            frame.set((fieldPosition[0]-hostPosition[0]).toFloat(),(fieldPosition[1]-hostPosition[1]).toFloat(),
                (fieldPosition[0]-hostPosition[0]+field.width).toFloat(),(fieldPosition[1]-hostPosition[1]+field.height).toFloat())
            val density=field.resources.displayMetrics.density
            val radius=BicycleDesign.inputRadius*density
            val spread=BicycleDesign.inputFocusGlow*density
            inside.reset();inside.addRoundRect(frame,radius,radius,Path.Direction.CW)
            val saved=canvas.save()
            canvas.clipRect(0,0,owner.width,owner.height);canvas.clipOutPath(inside)
            (owner as? BicycleStickyScrollView)?.clipInputGlow(canvas)
            val steps=BicycleDesign.inputFocusGlowSteps
            paint.strokeWidth=spread/steps*2
            for(i in steps downTo 0) {
                val distance=spread*i/steps
                val strength=exp(-4.0*i*i/(steps*steps)).toFloat()
                paint.color=(accent and 0xffffff) or ((255*BicycleDesign.inputFocusGlowAlpha*strength).roundToInt() shl 24)
                ring.set(frame);ring.inset(-distance,-distance)
                canvas.drawRoundRect(ring,radius+distance,radius+distance,paint)
            }
            canvas.restoreToCount(saved)
        }
        override fun setAlpha(alpha:Int)=Unit
        override fun setColorFilter(colorFilter:ColorFilter?)=Unit
        @Deprecated("Deprecated in Java") override fun getOpacity()=PixelFormat.TRANSLUCENT
    }
}
