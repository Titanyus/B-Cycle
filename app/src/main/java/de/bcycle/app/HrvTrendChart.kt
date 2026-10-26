package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToLong

internal class HrvTrendChart(context:Context,themes:ThemeManager,
    private val selected:(HrvTrendPoint)->Unit,private val period:(LocalDate,LocalDate)->Unit):View(context) {
    private var tokens=themes.active.tokens
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=BicycleDesign.bodyTypeface }
    private val path=Path()
    private val bandEdges=Path()
    private val barNumber=java.text.NumberFormat.getNumberInstance().apply {
        isGroupingUsed=false;maximumFractionDigits=1
    }
    private val viewport=HrvTrendViewport()
    private var points:List<HrvTrendPoint> = emptyList()
    private var barLabels:List<Pair<HrvTrendPoint,String>> = emptyList()
    var axis=HrvTrendAxis(0.0,1.0);private set
    private var selectedId:String?=null
    private var downX=0f;private var downY=0f;private var moved=false
    private var multiTouch=false
    private val scroller=OverScroller(context)
    private var flingX=0
    private val dateFormat=DateTimeFormatter.ofPattern("dd.MM")
    private val slop=ViewConfiguration.get(context).scaledTouchSlop
    private val density=resources.displayMetrics.density
    private val plotLeft get()=BicycleDesign.dp(context,BicycleDesign.hrvTrendAxisWidth).toFloat()
    private val plotRight get()=(width-BicycleDesign.dp(context,8)).toFloat().coerceAtLeast(plotLeft+1)
    private fun x(day:LocalDate)=plotLeft+((day.toEpochDay()-(viewport.end-viewport.span))/viewport.span*(plotRight-plotLeft)).toFloat()
    private var pointerA=-1;private var pointerB=-1;private var pinchSpan=0.0
    private val pinching get()=pointerA>=0 && pointerB>=0
    private fun beginPinch(event:MotionEvent,exclude:Int=-1) {
        val indices=(0 until event.pointerCount).filter { it!=exclude }
        pointerA=indices.getOrNull(0)?.let(event::getPointerId) ?: -1
        pointerB=indices.getOrNull(1)?.let(event::getPointerId) ?: -1
        pinchSpan=distance(event)
    }
    private fun distance(event:MotionEvent):Double {
        val a=event.findPointerIndex(pointerA);val b=event.findPointerIndex(pointerB)
        return if(a>=0 && b>=0) kotlin.math.hypot((event.getX(a)-event.getX(b)).toDouble(),(event.getY(a)-event.getY(b)).toDouble()) else 0.0
    }
    private fun endPinch() { pointerA=-1;pointerB=-1;pinchSpan=0.0 }
    // Preserve native one-finger quick-scale; multi-finger zoom has no minimum-span cutoff.
    private val quickScale=ScaleGestureDetector(context,object:ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector:ScaleGestureDetector):Boolean {
            stopFling();viewport.zoom(detector.scaleFactor.toDouble(),((detector.focusX-plotLeft)/(plotRight-plotLeft)).toDouble());changed();return true
        }
    })
    private val gestures=GestureDetector(context,object:GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e:MotionEvent)=true
        override fun onScroll(e1:MotionEvent?,e2:MotionEvent,dx:Float,dy:Float):Boolean {
            if(!pinching && !quickScale.isInProgress && moved && e2.pointerCount==1) { viewport.pan(dx/(plotRight-plotLeft)*viewport.span);changed() }
            return true
        }
        override fun onFling(e1:MotionEvent?,e2:MotionEvent,velocityX:Float,velocityY:Float):Boolean {
            if(!moved || multiTouch || pinching || quickScale.isInProgress || abs(velocityX)<=abs(velocityY)) return false
            flingX=0
            scroller.fling(0,0,(-velocityX).toInt(),0,Int.MIN_VALUE,Int.MAX_VALUE,0,0)
            postInvalidateOnAnimation();return true
        }
        override fun onSingleTapUp(e:MotionEvent):Boolean {
            if(moved || e.x !in plotLeft..plotRight) return false
            val day=(viewport.end-viewport.span+(e.x-plotLeft)/(plotRight-plotLeft)*viewport.span).roundToLong()
            points.firstOrNull { it.day.toEpochDay()==day }?.let { select(it) }
            performClick();return true
        }
    })
    init {
        tag="hrv_trend_chart";isFocusable=true;isClickable=true
        contentDescription=context.getString(R.string.hrvt_gestures)
        BicycleComponents.bindTheme(this,themes) { tokens=it;invalidate() }
    }
    fun show(rows:List<HrvTrendPoint>,reset:Boolean=false,axis:HrvTrendAxis=HrvTrendAxis.from(rows,HrvTrendMetric.RMSSD)) {
        stopFling()
        points=rows;this.axis=axis
        barLabels=rows.mapNotNull { point -> point.value?.let { point to barNumber.format(it) } }
        if(reset) viewport.reset(rows.map { it.day })
        if(selectedId==null || rows.none { it.row.id==selectedId }) selectedId=rows.lastOrNull()?.row?.id
        rows.firstOrNull { it.row.id==selectedId }?.let { selected(it) }
        changed()
    }
    private fun select(point:HrvTrendPoint) { selectedId=point.row.id;selected(point);invalidate() }
    private fun changed() {
        period(LocalDate.ofEpochDay(kotlin.math.ceil(viewport.end-viewport.span).toLong()),LocalDate.ofEpochDay(kotlin.math.floor(viewport.end).toLong()))
        invalidate()
    }
    override fun performClick():Boolean { super.performClick();return true }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        if(event.actionMasked==MotionEvent.ACTION_DOWN) { stopFling();endPinch();downX=event.x;downY=event.y;moved=false;multiTouch=false }
        if(event.actionMasked==MotionEvent.ACTION_POINTER_DOWN) {
            stopFling();multiTouch=true;moved=true;beginPinch(event);parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.obtain(event).also { it.action=MotionEvent.ACTION_CANCEL;quickScale.onTouchEvent(it);it.recycle() }
        }
        if(event.actionMasked==MotionEvent.ACTION_MOVE && pinching) {
            val next=distance(event)
            if(next>0 && pinchSpan>0) {
                val a=event.findPointerIndex(pointerA);val b=event.findPointerIndex(pointerB)
                val focus=(event.getX(a)+event.getX(b))/2
                viewport.zoom(next/pinchSpan,((focus-plotLeft)/(plotRight-plotLeft)).toDouble());changed()
            }
            pinchSpan=next
        }
        if(event.actionMasked==MotionEvent.ACTION_POINTER_UP) beginPinch(event,event.actionIndex)
        if(event.actionMasked==MotionEvent.ACTION_MOVE && abs(event.x-downX)>slop && abs(event.x-downX)>abs(event.y-downY)) {
            moved=true;parent?.requestDisallowInterceptTouchEvent(true)
        }
        if(event.actionMasked==MotionEvent.ACTION_CANCEL) stopFling()
        if(!multiTouch) quickScale.onTouchEvent(event)
        gestures.onTouchEvent(event)
        if(event.actionMasked==MotionEvent.ACTION_UP || event.actionMasked==MotionEvent.ACTION_CANCEL) { endPinch();parent?.requestDisallowInterceptTouchEvent(false) }
        return true
    }
    private fun stopFling() { scroller.forceFinished(true) }
    override fun computeScroll() {
        if(!scroller.computeScrollOffset()) return
        val next=scroller.currX;val dx=next-flingX;flingX=next
        val before=viewport.end
        val days=dx/(plotRight-plotLeft)*viewport.span
        viewport.pan(days)
        if(dx!=0 && abs(viewport.end-before-days)>1e-6) stopFling()
        changed()
        if(!scroller.isFinished) postInvalidateOnAnimation()
    }
    override fun onDetachedFromWindow() { stopFling();endPinch();super.onDetachedFromWindow() }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val top=16*density;val bottom=(height-32*density).coerceAtLeast(top+1)
        val half=((plotRight-plotLeft)/viewport.span*.32).toFloat().coerceAtMost(12*density)
        val visible=points.filter { x(it.day)+half>=plotLeft && x(it.day)-half<=plotRight }
        fun y(value:Double)=bottom-((value-axis.minimum)/(axis.maximum-axis.minimum)*(bottom-top)).toFloat()
        paint.textSize=BicycleDesign.analysisLabelSp*resources.displayMetrics.scaledDensity
        paint.color=alpha(tokens.foreground,.5f);paint.textAlign=Paint.Align.RIGHT
        for(i in 0..4) {
            val value=axis.minimum+(axis.maximum-axis.minimum)*i/4;val at=y(value)
            paint.color=alpha(tokens.foreground,.12f);paint.strokeWidth=density
            canvas.drawLine(plotLeft,at,plotRight,at,paint)
            paint.color=alpha(tokens.foreground,.5f)
            canvas.drawText(String.format(java.util.Locale.getDefault(),if(axis.maximum-axis.minimum>=100) "%.0f" else "%.1f",value),plotLeft-8*density,at+4*density,paint)
        }
        paint.textAlign=Paint.Align.CENTER
        // Calendar-anchored labels move with their Monday bar, including partially clipped labels.
        val axisSave=canvas.save();canvas.clipRect(plotLeft,bottom,plotRight,height.toFloat())
        var monday=LocalDate.ofEpochDay(kotlin.math.floor(viewport.end-viewport.span).toLong()-7)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
        val last=LocalDate.ofEpochDay(kotlin.math.ceil(viewport.end).toLong()+7)
        while(!monday.isAfter(last)) {
            val at=x(monday);val label=monday.format(dateFormat)
            canvas.drawText(label,at,bottom+24*density,paint)
            val separator=at-paint.measureText(label)/2-4*density
            canvas.drawLine(separator,bottom+12*density,separator,bottom+26*density,paint)
            monday=monday.plusWeeks(1)
        }
        canvas.restoreToCount(axisSave)
        canvas.save();canvas.clipRect(plotLeft,top,plotRight,bottom)
        bandEdges.reset()
        for(index in 1 until points.size) {
            val a=points[index-1];val b=points[index]
            if(b.day.toEpochDay()-a.day.toEpochDay()!=1L || a.cohort!=b.cohort || a.value==null || b.value==null) continue
            if(a.lower!=null && a.upper!=null && b.lower!=null && b.upper!=null) {
                path.reset();path.moveTo(x(a.day),y(a.lower));path.lineTo(x(a.day),y(a.upper))
                path.lineTo(x(b.day),y(b.upper));path.lineTo(x(b.day),y(b.lower));path.close()
                paint.color=alpha(Color.WHITE,BicycleDesign.hrvTrendBandAlpha);paint.style=Paint.Style.FILL;canvas.drawPath(path,paint)
                bandEdges.moveTo(x(a.day),y(a.lower));bandEdges.lineTo(x(b.day),y(b.lower))
                bandEdges.moveTo(x(a.day),y(a.upper));bandEdges.lineTo(x(b.day),y(b.upper))
            }
        }
        visible.forEach { point -> point.value?.let { value ->
            paint.color=if(point.belowNormal) Color.WHITE else tokens.primary
            canvas.drawRect(x(point.day)-half,y(value),x(point.day)+half,bottom,paint)
        } }
        paint.color=Color.WHITE;paint.strokeWidth=density*BicycleDesign.hrvTrendBandLineDp
        paint.style=Paint.Style.STROKE;paint.strokeCap=Paint.Cap.ROUND
        paint.pathEffect=android.graphics.DashPathEffect(floatArrayOf(.1f*density,BicycleDesign.hrvTrendBandDotGapDp*density),0f)
        canvas.drawPath(bandEdges,paint)
        paint.pathEffect=null;paint.strokeCap=Paint.Cap.BUTT;paint.style=Paint.Style.FILL
        paint.color=Color.WHITE;paint.strokeWidth=2*density
        for(index in points.indices) {
            val point=points[index];val mean=point.average ?: continue
            val previous=points.getOrNull(index-1)
            // Include the segment crossing either edge; Canvas clips it continuously.
            if(x(point.day)<plotLeft || x(previous?.day ?: point.day)>plotRight) continue
            if(previous?.average!=null && previous.cohort==point.cohort && point.day.toEpochDay()-previous.day.toEpochDay()==1L)
                canvas.drawLine(x(previous.day),y(previous.average),x(point.day),y(mean),paint)
            canvas.drawCircle(x(point.day),y(mean),2*density,paint)
        }
        paint.textSize=BicycleDesign.hrvTrendBarValueSp*resources.displayMetrics.scaledDensity
        paint.textAlign=Paint.Align.CENTER;paint.color=tokens.foreground
        val gap=BicycleDesign.dp(context,BicycleDesign.hrvTrendBarValueGap).toFloat()
        // Every value stays centered on its own bar, even when adjacent labels overlap.
        barLabels.forEach { (point,label) ->
            val center=x(point.day);val halfWidth=paint.measureText(label)/2
            if(center+halfWidth>=plotLeft && center-halfWidth<=plotRight) {
                val baseline=(y(point.value!!)-gap).coerceAtLeast(top-paint.fontMetrics.ascent)
                canvas.drawText(label,center,baseline,paint)
            }
        }
        canvas.restore()
    }
    private fun alpha(color:Int,factor:Float)=Color.argb((Color.alpha(color)*factor).toInt(),Color.red(color),Color.green(color),Color.blue(color))
    override fun onInitializeAccessibilityNodeInfo(info:AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(ZOOM_IN,context.getString(R.string.hrvt_zoom_in)))
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(ZOOM_OUT,context.getString(R.string.hrvt_zoom_out)))
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,context.getString(R.string.hrvt_older)))
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,context.getString(R.string.hrvt_newer)))
    }
    override fun performAccessibilityAction(action:Int,args:Bundle?):Boolean {
        stopFling()
        when(action) {
            ZOOM_IN->viewport.zoom(2.0,.5)
            ZOOM_OUT->viewport.zoom(.5,.5)
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD->viewport.pan(-viewport.span/2)
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD->viewport.pan(viewport.span/2)
            else->return super.performAccessibilityAction(action,args)
        }
        changed();return true
    }
    companion object { val ZOOM_IN=generateViewId();val ZOOM_OUT=generateViewId() }
}
