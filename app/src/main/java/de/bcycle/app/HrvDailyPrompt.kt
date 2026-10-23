package de.bcycle.app

import android.app.Activity
import android.graphics.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/** Blur only the dashboard subtree; this overlay stays sharp and never starts recording. */
internal class HrvDailyPrompt(private val activity:Activity,private val host:FrameLayout,
    private val dashboard:View,themes:ThemeManager,acknowledge:()->Unit,start:()->Unit):AutoCloseable {
    private val accessibility=dashboard.importantForAccessibility
    private var closed=false
    private val back=OnBackInvokedCallback { close() }
    private val overlay=FrameLayout(activity).apply {
        tag="hrv_daily_prompt";isClickable=true;isFocusable=true;elevation=BicycleDesign.dp(activity,BicycleDesign.sectionHeaderRaisedZ).toFloat()
        contentDescription=activity.getString(R.string.hrv_daily_dismiss)
        setOnClickListener { acknowledge();close() }
        addView(HrvDailyHeart(activity,themes) { acknowledge();close();start() },FrameLayout.LayoutParams(
            BicycleDesign.dp(activity,BicycleDesign.hrvPromptDiameter),BicycleDesign.dp(activity,BicycleDesign.hrvPromptDiameter),Gravity.CENTER))
    }
    init {
        dashboard.setRenderEffect(RenderEffect.createBlurEffect(BicycleDesign.hrvPromptBlurPx,BicycleDesign.hrvPromptBlurPx,Shader.TileMode.CLAMP))
        dashboard.importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        host.addView(overlay,FrameLayout.LayoutParams(-1,-1))
        activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY,back)
        overlay.findViewWithTag<View>("hrv_daily_start").requestFocus()
    }
    override fun close() {
        if(closed) return
        closed=true;activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(back)
        host.removeView(overlay);dashboard.setRenderEffect(null);dashboard.importantForAccessibility=accessibility
    }
}

internal class HrvDailyHeart(context:android.content.Context,themes:ThemeManager,start:()->Unit):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val heart=Path().apply {
        moveTo(50f,88f);cubicTo(42f,80f,10f,57f,10f,32f)
        cubicTo(10f,12f,35f,4f,50f,24f);cubicTo(65f,4f,90f,12f,90f,32f)
        cubicTo(90f,57f,58f,80f,50f,88f);close()
    }
    private var tokens=themes.active.tokens
    init {
        tag="hrv_daily_start";isClickable=true;isFocusable=true
        contentDescription=context.getString(R.string.hrv_daily_start)
        setOnClickListener { start() }
        BicycleComponents.bindTheme(this,themes) { tokens=it;invalidate() }
    }
    override fun getAccessibilityClassName():CharSequence=android.widget.Button::class.java.name
    override fun onMeasure(w:Int,h:Int) {
        val available=(parent as? View)?.width?.takeIf { it>0 } ?: resources.displayMetrics.widthPixels
        val size=minOf(MeasureSpec.getSize(w),available-2*BicycleDesign.dp(context,BicycleDesign.horizontalPadding)).coerceAtLeast(1)
        setMeasuredDimension(size,size)
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        val dx=event.x-width/2f;val dy=event.y-height/2f
        if(event.actionMasked==MotionEvent.ACTION_DOWN && dx*dx+dy*dy>width*width/4f) return false
        return super.onTouchEvent(event)
    }
    override fun onDraw(canvas:Canvas) {
        val size=minOf(width,height).toFloat();val cx=width/2f;val cy=height/2f
        paint.color=tokens.tertiary;canvas.drawCircle(cx,cy,size/2,paint)
        canvas.save();canvas.translate(cx-size/2,cy-size/2);canvas.scale(size/100,size/100)
        paint.color=tokens.accent;canvas.drawPath(heart,paint);canvas.restore()
        paint.color=if(ThemeValidator.contrast(Color.WHITE,tokens.accent)>=ThemeValidator.contrast(Color.BLACK,tokens.accent)) Color.WHITE else Color.BLACK
        paint.typeface=BicycleDesign.strongTypeface;paint.textAlign=Paint.Align.CENTER
        val text=context.getString(R.string.hrv_daily_start)
        paint.textSize=android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,BicycleDesign.titleSp,resources.displayMetrics)
        if(paint.measureText(text)>size*.6f) paint.textSize*=size*.6f/paint.measureText(text)
        canvas.drawText(text,cx,cy-(paint.ascent()+paint.descent())/2,paint)
    }
}
