package de.bcycle.app

import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup

/** Same cached header shadow mirrored upwards; no layout, elevation or touch changes. */
internal class BicycleFooterShadow private constructor(private val footer:View,alwaysGlow:Boolean):Drawable(),View.OnAttachStateChangeListener {
    companion object {
        fun bind(view:View,alwaysGlow:Boolean=false) { if(view.getTag(R.id.footer_shadow)==null) view.setTag(R.id.footer_shadow,BicycleFooterShadow(view,alwaysGlow)) }
    }
    private val shadow=BicycleHeaderShadow(footer.context,alwaysGlow)
    private var owner:ViewGroup?=null
    private val a=IntArray(2);private val b=IntArray(2)
    private val layout=View.OnLayoutChangeListener { _,_,_,_,_,_,_,_,_->refresh() }
    init { footer.addOnAttachStateChangeListener(this);footer.addOnLayoutChangeListener(layout);if(footer.isAttachedToWindow) attach() }
    private fun refresh() { owner?.let { setBounds(0,0,it.width,it.height);it.invalidate() } }
    private fun attach() { owner=(footer.parent as? ViewGroup)?.also { it.overlay.add(this);it.addOnLayoutChangeListener(layout) };refresh() }
    override fun onViewAttachedToWindow(v:View)=attach()
    override fun onViewDetachedFromWindow(v:View) { owner?.let { it.overlay.remove(this);it.removeOnLayoutChangeListener(layout) };owner=null }
    override fun draw(canvas:Canvas) {
        val parent=owner ?: return
        if(!footer.isShown || parent.tag=="bicycle_action_footer" || parent.tag=="bicycle_fullscreen_footer") return
        footer.getLocationInWindow(a);parent.getLocationInWindow(b)
        val saved=canvas.save();canvas.translate(parent.scrollX.toFloat(),parent.scrollY.toFloat())
        canvas.clipRect(0,0,parent.width,parent.height)
        shadow.drawUp(canvas,(a[0]-b[0]).toFloat(),(a[1]-b[1]).toFloat(),(a[0]-b[0]+footer.width).toFloat())
        canvas.restoreToCount(saved)
    }
    override fun setAlpha(alpha:Int)=Unit
    override fun setColorFilter(filter:ColorFilter?)=Unit
    @Deprecated("Deprecated in Java") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
