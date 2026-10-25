package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.SeekBar
import kotlin.math.ceil

/** Native SeekBar interaction with one shared track and a value inside its solid PRIMARY thumb. */
internal class BicycleSettingSlider(context:Context,themes:ThemeManager,private val format:(Int)->String):SeekBar(context) {
    private var tokens=themes.active.tokens
    private val textPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=BicycleDesign.bodyTypeface }
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var pillWidth=0;private var pillHeight=0
    private var measuredRange:IntRange?=null;private var measuredTextSize=0f
    val valueText get()=format(progress)
    private val pill=object:Drawable() {
        override fun getIntrinsicWidth()=pillWidth
        override fun getIntrinsicHeight()=pillHeight
        override fun isStateful()=true
        override fun onStateChange(state:IntArray):Boolean { invalidateSelf();return true }
        override fun draw(canvas:Canvas) {
            paint.style=Paint.Style.FILL;paint.color=tokens.primary;paint.alpha=if(isEnabled) 255 else 102
            val radius=bounds.height()/2f
            canvas.drawRoundRect(bounds.left.toFloat(),bounds.top.toFloat(),bounds.right.toFloat(),bounds.bottom.toFloat(),radius,radius,paint)
            textPaint.color=tokens.foreground;textPaint.alpha=if(isEnabled) Color.alpha(tokens.foreground) else (Color.alpha(tokens.foreground)*.4f).toInt()
            textPaint.textAlign=Paint.Align.CENTER
            canvas.drawText(valueText,bounds.exactCenterX(),bounds.exactCenterY()-(textPaint.ascent()+textPaint.descent())/2,textPaint)
        }
        override fun setAlpha(alpha:Int)=Unit
        override fun setColorFilter(filter:ColorFilter?)=Unit
        @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
    }
    private val track=object:Drawable() {
        override fun onLevelChange(level:Int):Boolean { invalidateSelf();return true }
        override fun draw(canvas:Canvas) {
            paint.style=Paint.Style.STROKE;paint.strokeWidth=BicycleDesign.dp(context,3).toFloat();paint.strokeCap=Paint.Cap.ROUND
            paint.color=(tokens.foreground and 0xffffff) or 0x55000000;paint.alpha=if(isEnabled) 85 else 34
            val y=bounds.exactCenterY();val rtl=layoutDirection==View.LAYOUT_DIRECTION_RTL
            val start=if(rtl) bounds.right.toFloat() else bounds.left.toFloat()
            val end=if(rtl) bounds.left.toFloat() else bounds.right.toFloat()
            canvas.drawLine(start,y,end,y,paint)
            paint.color=tokens.primary;paint.alpha=if(isEnabled) 255 else 102
            canvas.drawLine(start,y,start+(end-start)*level/10000f,y,paint)
        }
        override fun setAlpha(alpha:Int)=Unit
        override fun setColorFilter(filter:ColorFilter?)=Unit
        @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
    }
    init {
        minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
        thumbTintList=null;progressTintList=null;progressBackgroundTintList=null
        tickMark=null;splitTrack=false;progressDrawable=track;thumb=pill
        BicycleComponents.bindTheme(this,themes) { tokens=it;invalidate() }
    }
    override fun onMeasure(widthSpec:Int,heightSpec:Int) {
        textPaint.textSize=BicycleDesign.settingSliderValueSp*resources.displayMetrics.scaledDensity
        if(measuredRange!=min..max || measuredTextSize!=textPaint.textSize) {
            measuredRange=min..max;measuredTextSize=textPaint.textSize
            val padding=BicycleDesign.dp(context,BicycleDesign.controlGap)
            pillWidth=ceil((min..max).maxOf { textPaint.measureText(format(it)) }+2*padding).toInt().coerceAtLeast(BicycleDesign.dp(context,BicycleDesign.settingSliderPillWidth))
            pillHeight=ceil(textPaint.descent()-textPaint.ascent()+padding).toInt().coerceAtLeast(BicycleDesign.dp(context,BicycleDesign.settingSliderPillHeight))
            thumbOffset=pillWidth/2
            setPadding((pillWidth+1)/2,0,(pillWidth+1)/2,0)
        }
        super.onMeasure(widthSpec,heightSpec)
    }
    override fun onInitializeAccessibilityNodeInfo(info:AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info);info.stateDescription=valueText
    }
}

/** Same centre-to-centre pitch as the recording-source dropdown rows; no values above the track. */
internal fun settingSliderRow(context:Context,themes:ThemeManager,title:String,slider:BicycleSettingSlider)=LinearLayout(context).apply {
    orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setTag(R.id.inline_control_row,true)
    minimumHeight=BicycleDesign.dp(context,BicycleDesign.settingSliderRowHeight)
    val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
    setPadding(0,gap,0,gap)
    if(slider.id==View.NO_ID) slider.id=View.generateViewId()
    addView(BicycleComponents.text(context,themes,title).apply { labelFor=slider.id;gravity=Gravity.CENTER_VERTICAL },
        LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=gap })
    addView(slider,LinearLayout.LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.touchSize),2f))
}
