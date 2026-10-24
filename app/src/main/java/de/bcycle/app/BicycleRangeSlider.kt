package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import kotlin.math.abs
import kotlin.math.roundToInt

/** Two handles on one track; bounded discrete steps, keyboard and separate accessibility nodes. */
internal class BicycleRangeSlider(context:Context,themes:ThemeManager,private val label:String,
    private val format:(Double)->String,private val change:(Double,Double)->Unit):View(context) {
    private var maximum=1.0
    private var step=1.0
    internal var lower=0.0;private set
    internal var upper=1.0;private set
    private var active=0
    private var dragging=false
    private var accessibilityFocused=-1
    private var hovered=-1
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var tokens=themes.active.tokens
    private fun dp(value:Int)=BicycleDesign.dp(context,value).toFloat()
    init {
        isFocusable=true;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
        minimumHeight=dp(BicycleDesign.touchSize).toInt()
        BicycleComponents.bindTheme(this,themes) { tokens=it;invalidate() }
    }
    fun show(max:Double,increment:Double,from:Double,to:Double) {
        maximum=max.coerceAtLeast(increment);step=increment
        lower=from.coerceIn(0.0,maximum);upper=to.coerceIn(lower,maximum)
        contentDescription="$label · ${format(lower)} – ${format(upper)}";invalidate()
    }
    override fun onMeasure(w:Int,h:Int) { setMeasuredDimension(resolveSize(dp(240).toInt(),w),resolveSize(minimumHeight,h)) }
    private fun x(value:Double)=dp(20)+(width-2*dp(20)).coerceAtLeast(1f)*(value/maximum).toFloat()
    override fun onDraw(canvas:Canvas) {
        val y=height/2f
        paint.strokeWidth=dp(3);paint.strokeCap=Paint.Cap.ROUND
        paint.color=(tokens.foreground and 0xffffff) or 0x55000000
        canvas.drawLine(x(0.0),y,x(maximum),y,paint)
        paint.color=tokens.primary;canvas.drawLine(x(lower),y,x(upper),y,paint)
        listOf(lower,upper).forEachIndexed { i,value ->
            paint.color=tokens.foreground;canvas.drawCircle(x(value),y,dp(if(isFocused && active==i) 10 else 8),paint)
            paint.color=tokens.primary;canvas.drawCircle(x(value),y,dp(5),paint)
        }
    }
    private fun move(value:Double) {
        val v=((value/step).roundToInt()*step).coerceIn(0.0,maximum)
        if(active==0) lower=v.coerceAtMost(upper) else upper=v.coerceAtLeast(lower)
        show(maximum,step,lower,upper);change(lower,upper)
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        if(!isEnabled) return false
        fun value()=((event.x-dp(20))/(width-2*dp(20)).coerceAtLeast(1f)*maximum).coerceIn(0.0,maximum)
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val left=abs(event.x-x(lower));val right=abs(event.x-x(upper))
                active=if(left==right) if(event.x<x(lower)) 0 else 1 else if(left<right) 0 else 1
                dragging=true;requestFocus();parent?.requestDisallowInterceptTouchEvent(true);move(value());return true
            }
            MotionEvent.ACTION_MOVE -> if(dragging) { move(value());return true }
            MotionEvent.ACTION_UP -> if(dragging) { move(value());dragging=false;parent?.requestDisallowInterceptTouchEvent(false);performClick();return true }
            MotionEvent.ACTION_CANCEL -> { dragging=false;parent?.requestDisallowInterceptTouchEvent(false);return true }
        }
        return super.onTouchEvent(event)
    }
    override fun performClick():Boolean { super.performClick();return true }
    private fun accessibilityEvent(type:Int,id:Int) {
        val event=AccessibilityEvent.obtain(type)
        event.setSource(this,id);event.packageName=context.packageName
        parent?.requestSendAccessibilityEvent(this,event)
    }
    override fun dispatchHoverEvent(event:MotionEvent):Boolean {
        val manager=context.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
        if(manager?.isTouchExplorationEnabled!=true) return super.dispatchHoverEvent(event)
        val next=if(event.actionMasked==MotionEvent.ACTION_HOVER_EXIT) -1 else if(abs(event.x-x(lower))<abs(event.x-x(upper))) 0 else 1
        if(next!=hovered) {
            if(hovered>=0) accessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_EXIT,hovered)
            hovered=next
            if(next>=0) accessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER,next)
        }
        return true
    }
    override fun onKeyDown(keyCode:Int,event:KeyEvent):Boolean = when(keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT -> { move((if(active==0) lower else upper)+if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT) -step else step);true }
        KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN -> { active=1-active;invalidate();true }
        else->super.onKeyDown(keyCode,event)
    }
    override fun getAccessibilityNodeProvider()=object:AccessibilityNodeProvider() {
        override fun createAccessibilityNodeInfo(id:Int):AccessibilityNodeInfo {
            if(id==HOST_VIEW_ID) return AccessibilityNodeInfo.obtain(this@BicycleRangeSlider).apply {
                this@BicycleRangeSlider.onInitializeAccessibilityNodeInfo(this)
                addChild(this@BicycleRangeSlider,0);addChild(this@BicycleRangeSlider,1)
            }
            return AccessibilityNodeInfo.obtain().apply {
                setSource(this@BicycleRangeSlider,id);setParent(this@BicycleRangeSlider)
                packageName=context.packageName;className=android.widget.SeekBar::class.java.name
                isEnabled=this@BicycleRangeSlider.isEnabled;isFocusable=true;isVisibleToUser=isShown
                isAccessibilityFocused=accessibilityFocused==id
                val value=if(id==0) lower else upper
                contentDescription="$label · ${context.getString(if(id==0) R.string.route_filter_from else R.string.route_filter_to)} · ${format(value)}"
                rangeInfo=AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,
                    (if(id==0) 0.0 else lower).toFloat(),(if(id==0) upper else maximum).toFloat(),value.toFloat())
                val bounds=Rect((x(value)-dp(24)).toInt(),0,(x(value)+dp(24)).toInt(),height)
                setBoundsInParent(bounds);val pos=IntArray(2);getLocationOnScreen(pos);bounds.offset(pos[0],pos[1]);setBoundsInScreen(bounds)
                addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS)
                addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                addAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                addAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
            }
        }
        override fun performAction(id:Int,action:Int,args:Bundle?):Boolean {
            if(id !in 0..1) return this@BicycleRangeSlider.performAccessibilityAction(action,args)
            if(!isEnabled) return false
            active=id
            when(action) {
                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS -> {
                    accessibilityFocused=id
                    accessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,id);invalidate();return true
                }
                AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS -> {
                    accessibilityFocused=-1
                    accessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,id);return true
                }
                AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id -> {
                    val value=args?.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE)?.toDouble() ?: return false
                    if(!value.isFinite()) return false
                    move(value);return true
                }
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> move((if(id==0) lower else upper)+step)
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> move((if(id==0) lower else upper)-step)
                else->return false
            }
            return true
        }
    }
}
