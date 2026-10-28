package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.widget.FrameLayout

/** Only the upper part of a phone; status glyphs are the same renderer as on the dashboard. */
internal class DashboardStatusPhonePreview(context:Context,themes:ThemeManager,strip:DashboardStatusStrip):FrameLayout(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect=RectF()
    private val inset=BicycleDesign.dp(context,BicycleDesign.controlGap).toFloat()
    private val radius=BicycleDesign.dp(context,BicycleDesign.dashboardStatusPhoneRadius).toFloat()
    private var surface=themes.active.tokens.secondary
    private var border=themes.active.tokens.foreground
    init {
        tag="dashboard_status_phone_preview";setWillNotDraw(false);clipChildren=true
        addView(strip,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.dashboardStatusPreviewHeight)).apply { setMargins(inset.toInt(),inset.toInt(),inset.toInt(),0) })
        BicycleComponents.bindTheme(this,themes) { surface=it.secondary;border=it.foreground;invalidate() }
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        // Extend the body below the view: only the rounded top of the handset is visible.
        rect.set(.5f,.5f,width-.5f,height+radius)
        paint.style=Paint.Style.FILL;paint.color=Color.BLACK;canvas.drawRoundRect(rect,radius,radius,paint)
        paint.style=Paint.Style.STROKE;paint.color=border;paint.alpha=(255*BicycleDesign.controlBorderAlpha).toInt()
        paint.strokeWidth=BicycleDesign.dp(context,BicycleDesign.controlBorder).toFloat();canvas.drawRoundRect(rect,radius,radius,paint)
        paint.alpha=255;paint.style=Paint.Style.FILL;paint.color=surface
        rect.set(inset,inset,width-inset,height+radius)
        canvas.drawRoundRect(rect,radius-inset,radius-inset,paint)
    }
}
