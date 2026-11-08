package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.view.ViewGroup
import kotlin.math.abs
import kotlin.math.roundToInt

/** Compact CR10 picker. The two readings scale together to the adjacent note's height. */
internal class RideRpePicker(context:Context,themes:ThemeManager,private val rideId:String):View(context) {
    private val number=Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=BicycleDesign.narrowTypeface(context);textAlign=Paint.Align.CENTER }
    private val description=TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface=BicycleDesign.narrowTypeface(context) }
    private var value=RideAnnotations.read(context,rideId).rpe
    private var label:StaticLayout?=null
    private var numberBaseline=0f
    private var descriptionY=0f
    private var down=0f;private var initial=0;private var moved=false
    private val slop=ViewConfiguration.get(context).scaledTouchSlop
    private val step=BicycleDesign.dp(context,24).toFloat()
    init {
        tag="ride_rpe_picker";isClickable=true;isFocusable=true
        minimumWidth=BicycleDesign.dp(context,BicycleDesign.touchSize)
        BicycleComponents.bindTheme(this,themes) { number.color=it.foreground;description.color=it.foreground;invalidate() }
        describe()
    }
    private fun describe() {
        val text=value?.let { "$it · ${context.getString(RideRpe.label(it))}" } ?: context.getString(R.string.ride_rpe_unrated)
        contentDescription=context.getString(R.string.ride_rpe_question);stateDescription=text
    }
    private fun change(next:Int) {
        val bounded=next.coerceIn(0,10)
        if(value==bounded) return
        value=bounded;RideAnnotations.rpe(context,rideId,bounded);describe();prepare();invalidate()
    }
    private fun prepare() {
        if(width<=0 || height<=0) return
        val scaled=resources.displayMetrics.scaledDensity
        val text=value?.let { context.getString(RideRpe.label(it)) } ?: context.getString(R.string.ride_rpe_unrated)
        data class Fit(val layout:StaticLayout,val numberTop:Int,val numberHeight:Int,val labelTop:Int,val labelBottom:Int)
        val bounds=Rect()
        fun fit(scale:Float):Fit {
            number.textSize=56*scaled*scale;description.textSize=14*scaled*scale
            val digits=value?.toString() ?: "–"
            number.getTextBounds(digits,0,digits.length,bounds)
            val top=bounds.top;val numberHeight=bounds.height()
            val layout=StaticLayout.Builder.obtain(text,0,text.length,description,width).setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false).build()
            var labelTop=Int.MAX_VALUE;var labelBottom=Int.MIN_VALUE
            for(line in 0 until layout.lineCount) {
                description.getTextBounds(text,layout.getLineStart(line),layout.getLineEnd(line),bounds)
                labelTop=minOf(labelTop,layout.getLineBaseline(line)+bounds.top)
                labelBottom=maxOf(labelBottom,layout.getLineBaseline(line)+bounds.bottom)
            }
            return Fit(layout,top,numberHeight,labelTop,labelBottom)
        }
        var low=.1f;var high=4f
        repeat(14) {
            val middle=(low+high)/2;val next=fit(middle)
            val total=next.numberHeight+4*resources.displayMetrics.density*middle+next.labelBottom-next.labelTop
            if(total<=height-2 && number.measureText(value?.toString() ?: "–")<=width) low=middle else high=middle
        }
        val fitted=fit(low);label=fitted.layout
        numberBaseline=1f-fitted.numberTop
        descriptionY=height-1f-fitted.labelBottom
    }

    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { prepare() }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        canvas.drawText(value?.toString() ?: "–",width/2f,numberBaseline,number)
        val saved=canvas.save();canvas.translate(0f,descriptionY);label?.draw(canvas);canvas.restoreToCount(saved)
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN->{ down=event.y;initial=value ?: 0;moved=false;parent?.requestDisallowInterceptTouchEvent(true);isPressed=true;return true }
            MotionEvent.ACTION_MOVE->{
                val delta=down-event.y
                if(abs(delta)>slop) moved=true
                if(moved) change(initial+(delta/step).roundToInt())
                return true
            }
            MotionEvent.ACTION_UP->{ isPressed=false;parent?.requestDisallowInterceptTouchEvent(false);if(!moved) performClick();return true }
            MotionEvent.ACTION_CANCEL->{ isPressed=false;parent?.requestDisallowInterceptTouchEvent(false);return true }
        }
        return super.onTouchEvent(event)
    }
    override fun performClick():Boolean { super.performClick();if(value==null) change(0);return true }
    override fun onKeyDown(key:Int,event:KeyEvent):Boolean=when(key) {
        KeyEvent.KEYCODE_DPAD_UP->{ change((value ?: 0)+1);true }
        KeyEvent.KEYCODE_DPAD_DOWN->{ change((value ?: 0)-1);true }
        else->super.onKeyDown(key,event)
    }
    override fun onInitializeAccessibilityNodeInfo(info:AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info);info.className=android.widget.NumberPicker::class.java.name
        info.rangeInfo=AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,0f,10f,(value ?: 0).toFloat())
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS)
    }
    override fun performAccessibilityAction(action:Int,args:android.os.Bundle?):Boolean {
        when(action) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD->change((value ?: 0)+1)
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD->change((value ?: 0)-1)
            android.R.id.accessibilityActionSetProgress->{
                val next=args?.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE) ?: return false
                if(!next.isFinite()) return false
                change(next.roundToInt())
            }
            else->return super.performAccessibilityAction(action,args)
        }
        return true
    }
}

/** The note defines the row height; no additional empty space below the RPE block. */
internal class RideSummaryFeedbackRow(context:Context,themes:ThemeManager,id:String):ViewGroup(context) {
    val note=RideNoteEditor(context,themes,id)
    private val picker=RideRpePicker(context,themes,id)
    init { tag="ride_summary_feedback";addView(note);addView(picker) }
    override fun onMeasure(w:Int,h:Int) {
        val width=MeasureSpec.getSize(w);val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
        val right=(width*.30f).roundToInt().coerceAtLeast(BicycleDesign.dp(context,BicycleDesign.touchSize))
        note.measure(MeasureSpec.makeMeasureSpec((width-right-gap).coerceAtLeast(1),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED))
        picker.measure(MeasureSpec.makeMeasureSpec(right,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(note.measuredHeight,MeasureSpec.EXACTLY))
        setMeasuredDimension(resolveSize(width,w),resolveSize(note.measuredHeight,h))
    }
    override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int) {
        note.layout(0,0,note.measuredWidth,note.measuredHeight)
        picker.layout(width-picker.measuredWidth,0,width,picker.measuredHeight)
    }
}
