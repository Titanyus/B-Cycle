package de.bcycle.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.widget.Button

/** One native recording/bike action; the top cue is decoration within the same touch target. */
internal class DashboardRecordButton(context:Context,themes:ThemeManager):Button(context) {
    private val cap=BicycleDesign.dp(context,BicycleDesign.dashboardRecordCapHeight)
    private val cue=DashboardRecordCue(this)
    internal var recordIcon:Drawable?
        get()=cue.icon
        set(value) { cue.icon=value }
    init {
        isAllCaps=false;minWidth=0;minHeight=0;textSize=20f;gravity=android.view.Gravity.CENTER
        setPadding(BicycleDesign.dp(context,4),cap,BicycleDesign.dp(context,4),0)
        stateListAnimator=null;elevation=0f
        foreground=cue;foregroundGravity=android.view.Gravity.FILL
        BicycleComponents.bindTheme(this,themes) { tokens ->
            setTextColor(tokens.foreground);backgroundTintList=null
            background=RippleDrawable(ColorStateList.valueOf((tokens.foreground and 0xffffff) or 0x22000000),
                DashboardRecordSurface(context,tokens.primary),DashboardRecordSurface(context,Color.WHITE))
        }
    }
    override fun setEnabled(enabled:Boolean) {
        super.setEnabled(enabled)
        foreground?.alpha=if(enabled) 255 else 128
    }
}

/** Native foreground stays outside TextView's content clip and participates in its render node. */
private class DashboardRecordCue(private val view:android.view.View):Drawable(),Drawable.Callback {
    private val context get()=view.context
    private val cap=BicycleDesign.dp(context,BicycleDesign.dashboardRecordCapHeight)
    private val size=BicycleDesign.dp(context,BicycleDesign.dashboardRecordIconSize)
    var icon:Drawable?=null
        set(value) { field?.callback=null;field=value;value?.callback=this;position();invalidateSelf() }
    private fun position() { icon?.setBounds(bounds.centerX()-size/2,bounds.top+cap-size/2,bounds.centerX()+size/2,bounds.top+cap+size/2) }
    override fun onBoundsChange(bounds:Rect)=position()
    override fun draw(canvas:Canvas) {
        // A single-line TextView scrolls a wide internal text layout even with centered labels.
        // Keep the recording cue fixed to the physical button, outside that text scroll.
        val saved=canvas.save();canvas.translate(view.scrollX.toFloat(),view.scrollY.toFloat())
        icon?.draw(canvas);canvas.restoreToCount(saved)
    }
    override fun setAlpha(alpha:Int) { icon?.alpha=alpha;invalidateSelf() }
    override fun setColorFilter(filter:ColorFilter?) { icon?.colorFilter=filter;invalidateSelf() }
    override fun invalidateDrawable(who:Drawable)=invalidateSelf()
    override fun scheduleDrawable(who:Drawable,what:Runnable,whenMillis:Long)=scheduleSelf(what,whenMillis)
    override fun unscheduleDrawable(who:Drawable,what:Runnable)=unscheduleSelf(what)
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

/** Cached single silhouette: rounded rectangle with a smoothly joined circular rise. */
internal class DashboardRecordSurface(context:Context,color:Int):Drawable() {
    private val top=BicycleDesign.dp(context,BicycleDesign.dashboardRecordCapHeight).toFloat()
    private val radius=BicycleDesign.dp(context,BicycleDesign.dashboardRecordCornerRadius).toFloat()
    private val path=Path()
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color=color }
    override fun onBoundsChange(bounds:Rect) {
        val l=bounds.left.toFloat();val r=bounds.right.toFloat();val b=bounds.bottom.toFloat()
        val t=bounds.top+top;val cx=bounds.exactCenterX()
        val corner=minOf(radius,bounds.width()/4f,(b-t)/2f).coerceAtLeast(0f)
        val shoulder=top*1.3f
        path.reset();path.moveTo(l+corner,t);path.lineTo(cx-shoulder,t)
        path.cubicTo(cx-top*1.13f,t,cx-top*1.04f,t-top*.08f,cx-top*.95106f,t-top*.30902f)
        path.arcTo(RectF(cx-top,t-top,cx+top,t+top),198f,144f,false)
        path.cubicTo(cx+top*1.04f,t-top*.08f,cx+top*1.13f,t,cx+shoulder,t)
        path.lineTo(r-corner,t);path.quadTo(r,t,r,t+corner)
        path.lineTo(r,b-corner);path.quadTo(r,b,r-corner,b)
        path.lineTo(l+corner,b);path.quadTo(l,b,l,b-corner)
        path.lineTo(l,t+corner);path.quadTo(l,t,l+corner,t);path.close()
    }
    override fun draw(canvas:Canvas) { canvas.drawPath(path,paint) }
    override fun setAlpha(alpha:Int) { paint.alpha=alpha;invalidateSelf() }
    override fun setColorFilter(filter:ColorFilter?) { paint.colorFilter=filter;invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
