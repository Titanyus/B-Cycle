package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.OverScroller
import kotlin.math.*

internal data class StatisticsYearSeries(val year:Int,val color:Int,val values:List<Double>)

/** Weekly coordinates are week numbers, not a fixed pixel width. Zoom never changes the data/Y scale. */
internal class StatisticsViewport {
    var span=14.0;private set
    var end=13.5;private set
    private var count=53
    fun size(count:Int) {
        this.count=count.coerceAtLeast(1);span=span.coerceIn(minOf(4.0,this.count.toDouble()),this.count.toDouble());clamp()
    }
    private fun clamp() { end=end.coerceIn(-.5+span,count-.5) }
    fun pan(units:Double) { end+=units;clamp() }
    fun zoom(factor:Double,anchor:Double) {
        if(!factor.isFinite() || factor<=0) return
        val position=anchor.coerceIn(0.0,1.0);val pivot=end-span+span*position
        span=(span/factor).coerceIn(minOf(4.0,count.toDouble()),count.toDouble())
        end=pivot+span*(1-position);clamp()
    }
}

internal class StatisticsDistanceChart(context:Context,private val themes:ThemeManager):View(context) {
    private data class Curve(val series:StatisticsYearSeries,val path:Path,val points:List<PointF>,val labels:List<String>)
    private val density=resources.displayMetrics.density
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=BicycleDesign.bodyTypeface }
    private data class ScaleLabel(val text:String,val left:Float,val baseline:Float,val box:RectF)
    private data class PointLabel(val text:String,val x:Float,val baseline:Float,val color:Int)
    private val rect=RectF()
    private var scaleLayout:List<ScaleLabel> = emptyList()
    private var valueLayout:List<PointLabel> = emptyList()
    private val number=java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits=0 }
    private var series:List<StatisticsYearSeries> = emptyList()
    private var labels:List<String> = emptyList()
    private var curves:List<Curve> = emptyList()
    private var scaleLabels:List<String> = emptyList()
    private var pointLabels:List<List<String>> = emptyList()
    private var maximum=1.0
    private var weekly=false
    internal val viewport=StatisticsViewport()
    private val scroller=OverScroller(context)
    private var flingX=0
    private var downX=0f;private var downY=0f;private var moved=false;private var multi=false
    private var pointerA=-1;private var pointerB=-1;private var pinchSpan=0.0;private var pinchFocus=0f
    private val slop=ViewConfiguration.get(context).scaledTouchSlop
    private val pinching get()=pointerA>=0 && pointerB>=0
    private val span get()=if(weekly) viewport.span else labels.size.toDouble().coerceAtLeast(1.0)
    private val end get()=if(weekly) viewport.end else labels.size-.5
    private fun x(index:Int)=rect.left+((index-(end-span))/span*rect.width()).toFloat()
    private val gestures=GestureDetector(context,object:GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e:MotionEvent)=true
        override fun onScroll(e1:MotionEvent?,e2:MotionEvent,dx:Float,dy:Float):Boolean {
            if(moved && !multi && !pinching) { viewport.pan(dx/rect.width().coerceAtLeast(1f)*viewport.span);rebuild() }
            return true
        }
        override fun onFling(e1:MotionEvent?,e2:MotionEvent,velocityX:Float,velocityY:Float):Boolean {
            if(!moved || multi || abs(velocityX)<=abs(velocityY)) return false
            flingX=0;scroller.fling(0,0,(-velocityX).toInt(),0,Int.MIN_VALUE,Int.MAX_VALUE,0,0);postInvalidateOnAnimation();return true
        }
        override fun onSingleTapUp(e:MotionEvent):Boolean { performClick();return true }
    })
    init {
        tag="statistics_distance_chart";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES;isFocusable=true
        BicycleComponents.bindTheme(this,themes) { invalidate() }
    }
    fun show(values:List<StatisticsYearSeries>,labels:List<String>,weekly:Boolean) {
        stop();this.series=values;this.labels=labels;this.weekly=weekly;isClickable=weekly
        if(weekly) viewport.size(labels.size)
        val peak=values.maxOfOrNull { it.values.maxOrNull() ?: 0.0 } ?: 0.0
        val unit=when { peak>1000->500.0;peak>200->100.0;peak>50->25.0;else->5.0 }
        maximum=ceil(peak/unit).times(unit).coerceAtLeast(unit)
        val axisNumber=java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits=2 }
        scaleLabels=(1..4).map { context.getString(R.string.statistics_km,axisNumber.format(maximum*it/4)) }
        pointLabels=values.map { it.values.map(number::format) }
        contentDescription=labels.indices.joinToString("; ") { i -> labels[i]+": "+values.mapNotNull { row -> row.values.getOrNull(i)?.let { row.year.toString()+" "+context.getString(R.string.statistics_km,number.format(it)) } }.joinToString(" / ") }
        rebuild()
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { rect.set(0f,24*density,w.toFloat(),h-28*density);rebuild() }
    private fun rebuild() {
        curves=series.mapIndexed { index,row ->
            val points=row.values.mapIndexed { i,value -> PointF(x(i),rect.bottom-(value/maximum).toFloat()*rect.height()) }
            Curve(row,Path().also { MonotoneGraphPath.append(it,points) },points,pointLabels[index])
        }
        paint.textSize=BicycleDesign.analysisLabelSp*resources.displayMetrics.scaledDensity
        scaleLayout=scaleLabels.mapIndexed { i,label ->
            val baseline=rect.bottom-rect.height()*(i+1)/4-3*density
            val left=rect.left+4*density
            ScaleLabel(label,left,baseline,RectF(rect.left,baseline+paint.ascent(),left+paint.measureText(label)+2*density,baseline+paint.descent()))
        }
        paint.textSize=BicycleDesign.statisticsWeekValueSp*resources.displayMetrics.scaledDensity
        val ascent=paint.ascent();val descent=paint.descent();val labelHeight=descent-ascent
        val used=scaleLayout.map { RectF(it.box) }.toMutableList()
        val placed=mutableListOf<PointLabel>()
        curves.forEach { curve -> curve.points.forEachIndexed { i,p ->
            val text=curve.labels[i];val half=paint.measureText(text)/2
            if(p.x+half>=rect.left && p.x-half<=rect.right) {
                val above=(p.y-4*density).coerceAtLeast(-ascent)
                val below=(p.y+4*density-ascent).coerceAtMost(rect.bottom-descent)
                val higher=(above-labelHeight-4*density).coerceAtLeast(-ascent)
                val options=listOf(above,below,higher).map { baseline -> baseline to RectF(p.x-half-2*density,baseline+ascent,p.x+half+2*density,baseline+descent) }
                val chosen=options.firstOrNull { (_,box) -> used.none { RectF.intersects(it,box) } } ?: options.first()
                used+=chosen.second;placed+=PointLabel(text,p.x,chosen.first,curve.series.color)
            }
        } }
        valueLayout=placed
        invalidate()
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        paint.shader=null;paint.pathEffect=null;paint.style=Paint.Style.STROKE;paint.strokeWidth=density*.5f
        paint.color=themes.active.tokens.foreground;paint.alpha=60
        for(i in 1..4) { val y=rect.bottom-rect.height()*i/4;canvas.drawLine(rect.left,y,rect.right,y,paint) }
        paint.alpha=255
        val save=canvas.save();canvas.clipRect(rect.left,0f,rect.right,rect.bottom)
        for(curve in curves) {
            paint.color=curve.series.color;paint.strokeWidth=2*density;paint.style=Paint.Style.STROKE;canvas.drawPath(curve.path,paint)
            paint.style=Paint.Style.FILL
            curve.points.forEach { p -> canvas.drawCircle(p.x,p.y,2*density,paint) }
        }
        paint.textAlign=Paint.Align.CENTER;paint.textSize=BicycleDesign.statisticsWeekValueSp*resources.displayMetrics.scaledDensity
        valueLayout.forEach { label -> paint.color=label.color;canvas.drawText(label.text,label.x,label.baseline,paint) }
        canvas.restoreToCount(save)
        paint.style=Paint.Style.FILL;paint.color=themes.active.tokens.secondaryTextColor
        paint.textSize=BicycleDesign.analysisLabelSp*resources.displayMetrics.scaledDensity;paint.textAlign=Paint.Align.CENTER
        val stride=ceil((labels.maxOfOrNull { paint.measureText(it) } ?: 0f).plus((if(weekly) 8 else 2)*density)/(rect.width().coerceAtLeast(1f)/span)).toInt().coerceAtLeast(1)
        val axisSave=canvas.save();canvas.clipRect(rect.left,rect.bottom,rect.right,height.toFloat())
        labels.forEachIndexed { i,label -> if(i%stride==0) canvas.drawText(label,x(i),height-7*density,paint) }
        canvas.restoreToCount(axisSave)
        // Inside the full-width plot; there is deliberately no zero/baseline label.
        paint.textAlign=Paint.Align.LEFT
        scaleLayout.forEach { label ->
            paint.color=themes.active.tokens.secondaryTextColor;canvas.drawText(label.text,label.left,label.baseline,paint)
        }
    }
    private fun beginPinch(event:MotionEvent,exclude:Int=-1) {
        val ids=(0 until event.pointerCount).filter { it!=exclude }
        pointerA=ids.getOrNull(0)?.let(event::getPointerId) ?: -1;pointerB=ids.getOrNull(1)?.let(event::getPointerId) ?: -1
        pinchSpan=distance(event);pinchFocus=focus(event)
    }
    private fun distance(e:MotionEvent):Double { val a=e.findPointerIndex(pointerA);val b=e.findPointerIndex(pointerB);return if(a>=0 && b>=0) hypot((e.getX(a)-e.getX(b)).toDouble(),(e.getY(a)-e.getY(b)).toDouble()) else 0.0 }
    private fun focus(e:MotionEvent):Float { val a=e.findPointerIndex(pointerA);val b=e.findPointerIndex(pointerB);return if(a>=0 && b>=0) (e.getX(a)+e.getX(b))/2 else 0f }
    private fun stop() { scroller.forceFinished(true) }
    fun pause() { stop() }
    override fun performClick():Boolean { super.performClick();return true }
    override fun onTouchEvent(e:MotionEvent):Boolean {
        if(!weekly) return false
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN->{ stop();pointerA=-1;pointerB=-1;downX=e.x;downY=e.y;moved=false;multi=false }
            MotionEvent.ACTION_POINTER_DOWN->{ stop();multi=true;moved=true;beginPinch(e);parent?.requestDisallowInterceptTouchEvent(true) }
            MotionEvent.ACTION_MOVE->{
                if(pinching) {
                    val next=distance(e);val focus=focus(e);val width=rect.width().coerceAtLeast(1f)
                    if(next>0 && pinchSpan>0) {
                        viewport.zoom(next/pinchSpan,((pinchFocus-rect.left)/width).toDouble())
                        viewport.pan((pinchFocus-focus)/width*viewport.span);rebuild()
                    }
                    pinchSpan=next;pinchFocus=focus
                } else if(abs(e.x-downX)>slop && abs(e.x-downX)>abs(e.y-downY)) { moved=true;parent?.requestDisallowInterceptTouchEvent(true) }
            }
            MotionEvent.ACTION_POINTER_UP->beginPinch(e,e.actionIndex)
            MotionEvent.ACTION_CANCEL->stop()
        }
        gestures.onTouchEvent(e)
        if(e.actionMasked==MotionEvent.ACTION_UP || e.actionMasked==MotionEvent.ACTION_CANCEL) { pointerA=-1;pointerB=-1;parent?.requestDisallowInterceptTouchEvent(false) }
        return true
    }
    override fun computeScroll() {
        if(!scroller.computeScrollOffset()) return
        val next=scroller.currX;val dx=next-flingX;flingX=next
        val before=viewport.end;val units=dx/rect.width().coerceAtLeast(1f)*viewport.span;viewport.pan(units)
        if(dx!=0 && abs(viewport.end-before-units)>1e-6) stop()
        rebuild();if(!scroller.isFinished) postInvalidateOnAnimation()
    }
    override fun onDetachedFromWindow() { stop();pointerA=-1;pointerB=-1;super.onDetachedFromWindow() }
    override fun onInitializeAccessibilityNodeInfo(info:AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if(weekly) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction(ZOOM_IN,context.getString(R.string.hrvt_zoom_in)))
            info.addAction(AccessibilityNodeInfo.AccessibilityAction(ZOOM_OUT,context.getString(R.string.hrvt_zoom_out)))
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        }
    }
    override fun performAccessibilityAction(action:Int,args:Bundle?):Boolean {
        if(!weekly) return super.performAccessibilityAction(action,args)
        stop()
        when(action) {
            ZOOM_IN->viewport.zoom(2.0,.5);ZOOM_OUT->viewport.zoom(.5,.5)
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD->viewport.pan(-viewport.span/2)
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD->viewport.pan(viewport.span/2)
            else->return super.performAccessibilityAction(action,args)
        }
        rebuild();return true
    }
    companion object { val ZOOM_IN=generateViewId();val ZOOM_OUT=generateViewId() }
}
