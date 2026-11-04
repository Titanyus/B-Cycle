package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.view.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.OverScroller
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.*

internal object FitnessChartColors {
    val fitness=Color.rgb(45,179,222)
    val fatigue=Color.rgb(214,64,224)
    val form=Color.rgb(255,203,56)
}

/** One calendar axis, two vertically aligned plots. Gaps remain gaps; smoothing changes no values. */
internal class IntervalsFitnessChart(context:Context,private val themes:ThemeManager,
    private val today:()->LocalDate=LocalDate::now):View(context) {
    var rangeChanged:()->Unit={}
    var selected:(IntervalsFitnessDay)->Unit={}
    private var values:Map<LocalDate,IntervalsFitnessDay> = emptyMap()
    private var end=today().toEpochDay().toDouble()
    private var span=7
    private var selectedDay=today()
    private val d=resources.displayMetrics.density
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val upper=RectF();private val lower=RectF()
    private data class Curve(val path:Path,val dots:List<PointF>,val color:Int)
    private var curves:List<Curve> = emptyList()
    private var loadMax=10.0;private var formMin=-10.0;private var formMax=10.0
    private val dates=DateTimeFormatter.ofPattern("dd.MM.")
    private val numbers=java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits=1 }
    private val scroller=OverScroller(context)
    private var flingX=0;private var downX=0f;private var downY=0f;private var dragging=false
    private val slop=ViewConfiguration.get(context).scaledTouchSlop
    private val gestures=GestureDetector(context,object:GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e:MotionEvent)=true
        override fun onScroll(a:MotionEvent?,b:MotionEvent,dx:Float,dy:Float):Boolean {
            if(!dragging) return false
            pan(dx);return true
        }
        override fun onFling(a:MotionEvent?,b:MotionEvent,vx:Float,vy:Float):Boolean {
            if(!dragging || abs(vx)<=abs(vy)) return false
            flingX=0;scroller.fling(0,0,-vx.toInt(),0,Int.MIN_VALUE,Int.MAX_VALUE,0,0);postInvalidateOnAnimation();return true
        }
        override fun onSingleTapUp(e:MotionEvent):Boolean {
            val day=(end-span+1+(e.x-upper.left)/upper.width().coerceAtLeast(1f)*(span-1)).roundToLong()
            select(LocalDate.ofEpochDay(day.coerceIn(oldest().toEpochDay(),newest().toEpochDay())));performClick();return true
        }
    })
    init {
        tag="intervals_fitness_chart";isFocusable=true;isClickable=true
        contentDescription=context.getString(R.string.intervals_fitness_chart)
        BicycleComponents.bindTheme(this,themes) { invalidate() }
    }
    fun oldest():LocalDate=LocalDate.ofEpochDay(floor(end-span+1).toLong())
    fun newest():LocalDate=LocalDate.ofEpochDay(ceil(end).toLong())
    fun period(period:IntervalsFitnessPeriod) { scroller.forceFinished(true);span=period.days;if(selectedDay<oldest() || selectedDay>newest()) select(newest());rebuild();rangeChanged() }
    fun goToday() { scroller.forceFinished(true);end=today().toEpochDay().toDouble();select(today());rebuild();rangeChanged() }
    fun show(history:IntervalsFitnessHistory) { values=history.days.associateBy { it.day };rebuild();select(selectedDay) }
    private fun select(day:LocalDate) {
        selectedDay=day
        val point=values[day] ?: IntervalsFitnessDay(day,null,null)
        selected(point)
        contentDescription=context.getString(R.string.intervals_fitness_chart)+" $day: "+
            listOf(point.fitness,point.fatigue,point.form).joinToString(" / ") { it?.let(numbers::format) ?: "--" }
        invalidate()
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
        val bottom=h-28*d
        upper.set(0f,20*d,w.toFloat(),20*d+(bottom-20*d)*.63f)
        lower.set(0f,upper.bottom+18*d,w.toFloat(),bottom);rebuild()
    }
    private fun x(day:LocalDate)=upper.left+((day.toEpochDay()-(end-span+1))/(span-1)).toFloat()*upper.width()
    private fun rebuild() {
        if(upper.width()<=0) return
        val rows=(oldest().minusDays(1).toEpochDay()..newest().plusDays(1).toEpochDay()).map { epoch ->
            val day=LocalDate.ofEpochDay(epoch);values[day] ?: IntervalsFitnessDay(day,null,null)
        }
        fun nice(value:Double):Double {
            val magnitude=10.0.pow(floor(log10(value.coerceAtLeast(1.0))))
            val step=magnitude/2;return ceil(value/step)*step
        }
        loadMax=nice((rows.flatMap { listOfNotNull(it.fitness,it.fatigue) }.maxOrNull() ?: 1.0)*1.08).coerceAtLeast(10.0)
        formMin=-nice(abs(minOf(-10.0,rows.mapNotNull { it.form }.minOrNull() ?: -10.0))*1.08)
        formMax=nice(maxOf(10.0,rows.mapNotNull { it.form }.maxOrNull() ?: 10.0)*1.08)
        fun series(rect:RectF,min:Double,max:Double,color:Int,get:(IntervalsFitnessDay)->Double?):Curve {
            val path=Path();val dots=ArrayList<PointF>();val run=ArrayList<PointF>()
            fun flush() { if(run.size==1) dots.add(run.first()) else MonotoneGraphPath.append(path,run);run.clear() }
            for(row in rows) { val value=get(row)
                if(value==null) flush() else run.add(PointF(x(row.day),rect.bottom-((value-min)/(max-min)).toFloat()*rect.height()))
            };flush();return Curve(path,dots,color)
        }
        curves=listOf(series(upper,0.0,loadMax,FitnessChartColors.fitness) { it.fitness },
            series(upper,0.0,loadMax,FitnessChartColors.fatigue) { it.fatigue },series(lower,formMin,formMax,FitnessChartColors.form) { it.form })
        invalidate()
    }
    private fun pan(pixels:Float) {
        val next=(end+pixels/upper.width().coerceAtLeast(1f)*(span-1)).coerceAtMost(today().toEpochDay().toDouble())
        if(next!=end) { end=next;if(selectedDay<oldest() || selectedDay>newest()) select(newest());rebuild() } else scroller.forceFinished(true)
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas);val tokens=themes.active.tokens
        paint.typeface=BicycleDesign.bodyTypeface;paint.textSize=BicycleDesign.analysisLabelSp*resources.displayMetrics.scaledDensity
        paint.style=Paint.Style.STROKE;paint.strokeWidth=d*.5f;paint.color=tokens.foreground;paint.alpha=45
        for(rect in listOf(upper,lower)) for(i in 0..4) canvas.drawLine(rect.left,rect.bottom-rect.height()*i/4,rect.right,rect.bottom-rect.height()*i/4,paint)
        paint.alpha=255
        for((i,curve) in curves.withIndex()) {
            val rect=if(i<2) upper else lower;val saved=canvas.save();canvas.clipRect(rect)
            paint.color=curve.color;paint.style=Paint.Style.STROKE;paint.strokeWidth=1.8f*d;canvas.drawPath(curve.path,paint)
            paint.style=Paint.Style.FILL;curve.dots.forEach { canvas.drawCircle(it.x,it.y,2*d,paint) }
            canvas.restoreToCount(saved)
        }
        val selectedX=x(selectedDay)
        if(selectedX in upper.left..upper.right) {
            paint.style=Paint.Style.STROKE;paint.strokeWidth=d;paint.color=tokens.foreground;paint.alpha=120
            canvas.drawLine(selectedX,upper.top,selectedX,lower.bottom,paint);paint.alpha=255
        }
        val zero=lower.bottom-((0-formMin)/(formMax-formMin)).toFloat()*lower.height()
        paint.color=tokens.foreground;paint.strokeWidth=d;paint.alpha=100;canvas.drawLine(0f,zero,width.toFloat(),zero,paint);paint.alpha=255
        paint.style=Paint.Style.FILL;paint.textAlign=Paint.Align.LEFT;paint.color=tokens.secondaryTextColor
        for(i in 1..4) {
            canvas.drawText(numbers.format(loadMax*i/4),4*d,upper.bottom-upper.height()*i/4+12*d,paint)
            canvas.drawText(numbers.format(formMin+(formMax-formMin)*i/4),4*d,lower.bottom-lower.height()*i/4+12*d,paint)
        }
        canvas.drawText(context.getString(R.string.intervals_fitness_form),4*d,lower.top-4*d,paint)
        val stride=when { span<=7->1;span<=30->7;else->14 }
        paint.textAlign=Paint.Align.CENTER
        val save=canvas.save();canvas.clipRect(0f,lower.bottom,width.toFloat(),height.toFloat())
        var epoch=oldest().toEpochDay();while(epoch<=newest().toEpochDay()) {
            if(Math.floorMod(epoch,stride.toLong())==0L) { val day=LocalDate.ofEpochDay(epoch);canvas.drawText(dates.format(day),x(day),height-6*d,paint) };epoch++
        };canvas.restoreToCount(save)
    }
    override fun computeScroll() {
        if(scroller.computeScrollOffset()) {
            val next=scroller.currX;pan((next-flingX).toFloat());flingX=next;postInvalidateOnAnimation()
            if(scroller.isFinished) rangeChanged()
        }
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { scroller.forceFinished(true);downX=event.x;downY=event.y;dragging=false }
            MotionEvent.ACTION_MOVE -> if(abs(event.x-downX)>slop && abs(event.x-downX)>abs(event.y-downY)) {
                dragging=true;parent?.requestDisallowInterceptTouchEvent(true)
            }
        }
        gestures.onTouchEvent(event)
        if(event.actionMasked==MotionEvent.ACTION_UP || event.actionMasked==MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
            if(scroller.isFinished && dragging) rangeChanged()
        }
        return true
    }
    override fun performClick():Boolean { super.performClick();return true }
    override fun onInitializeAccessibilityNodeInfo(info:AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info);info.isScrollable=true
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
    }
    override fun performAccessibilityAction(action:Int,args:android.os.Bundle?):Boolean {
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD || action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
            end=(end+if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) -(span-1) else span-1).coerceAtMost(today().toEpochDay().toDouble())
            select(newest());rebuild();rangeChanged();return true
        };return super.performAccessibilityAction(action,args)
    }
    override fun onDetachedFromWindow() { scroller.forceFinished(true);super.onDetachedFromWindow() }
}
