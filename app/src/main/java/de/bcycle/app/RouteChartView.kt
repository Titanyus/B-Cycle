package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View


/** Compact native Canvas chart; geometry/labels cached when its size changes, not while scrolling. */
internal class RouteChartView(context: Context, themes: ThemeManager, val analysis: RouteAnalysis, val series: AnalysisSeries, internal val navigation:RouteChartNavigation=RouteChartNavigation(context)) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = BicycleDesign.strongTypeface
        textAlign = Paint.Align.CENTER
    }
    private val area = Path()
    private var foreground = 0; private var curve = 0
    private val density = resources.displayMetrics.density
    private val scaled = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 1f, resources.displayMetrics)
    internal val plotLeft get() = 58f * density
    internal val plotRight get() = width - 8f * density
    internal fun screenX(position: Double) = plotLeft + ((position / analysis.axisEnd-navigation.viewport.start)/navigation.viewport.span).toFloat() * (plotRight - plotLeft)
    private fun baseX(position:Double)=plotLeft+(position/analysis.axisEnd).toFloat()*(plotRight-plotLeft)
    internal var scale=RouteChartScale(0.0,5.0,emptyList()); private set
    private val minimum get()=scale.minimum
    private val maximum get()=scale.maximum
    private var labelsStart=Double.NaN;private var labelsSpan=Double.NaN
    private val xLabels = Array(5) { "" }; private var yLabels = emptyList<String>()
    private val title = context.getString(series.chart.detailLabel)
    private val unit = context.getString(series.chart.unit)
    private val axisUnit = context.getString(if (analysis.distanceAxis) R.string.analysis_km else R.string.analysis_minutes)
    init {
        tag = "analysis_chart_${series.chart.name}"
        contentDescription = context.getString(R.string.analysis_chart_description, title, axisUnit, unit)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isClickable=true;isFocusable=true
        BicycleComponents.bindTheme(this, themes) { tokens ->
            foreground = tokens.foreground; curve = series.chart.color(themes.baseTokens.secondary); invalidate()
        }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        scale=RouteChartScale.calculate(series.chart,series.points.mapNotNull { it.value })
        val top = 12 * density; val bottom = h - 24 * density
        area.reset()
        fun y(value: Double) = bottom - ((value - minimum) / (maximum - minimum)).toFloat() * (bottom - top)
        val segments = RouteChartCurve.prepare(series, analysis.distanceAxis, analysis.axisEnd, plotRight - plotLeft)
        for (points in segments) {
            val segment = Path()
            val first = points.first()
            segment.moveTo(baseX(first.x), y(first.value))
            val tangents = RouteChartCurve.tangents(points)
            for (i in 1 until points.size) {
                val a = points[i - 1]; val b = points[i]; val dx = (b.x - a.x) / 3
                if (dx <= 0) segment.lineTo(baseX(b.x), y(b.value))
                else segment.cubicTo(baseX(a.x + dx), y(a.value + tangents[i - 1] * dx),
                    baseX(b.x - dx), y(b.value - tangents[i] * dx), baseX(b.x), y(b.value))
            }
            segment.lineTo(baseX(points.last().x), bottom)
            segment.lineTo(baseX(first.x), bottom); segment.close(); area.addPath(segment)
        }
        labelsStart=Double.NaN
        yLabels=scale.marks.map { context.getString(if(it%1.0==0.0) R.string.analysis_integer_format else R.string.analysis_axis_format,it) }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();navigation.attach(this) }
    override fun onDetachedFromWindow() { navigation.detach(this);super.onDetachedFromWindow() }
    override fun onTouchEvent(event:android.view.MotionEvent)=navigation.touch(this,event)
    override fun performClick():Boolean { super.performClick();return true }
    override fun computeScroll() { navigation.computeScroll(this) }
    override fun onInitializeAccessibilityNodeInfo(info:android.view.accessibility.AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.isScrollable=navigation.viewport.zoomed
        if(info.isScrollable) {
            info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
            info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        }
        info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(R.id.route_chart_zoom_in,context.getString(R.string.route_chart_zoom_in)))
        info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(R.id.route_chart_zoom_out,context.getString(R.string.route_chart_zoom_out)))
    }
    override fun performAccessibilityAction(action:Int,args:android.os.Bundle?):Boolean {
        when(action) {
            R.id.route_chart_zoom_in->navigation.zoom(this,2.0)
            R.id.route_chart_zoom_out->navigation.zoom(this,.5)
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD->navigation.scroll(this,true)
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD->navigation.scroll(this,false)
            else->return super.performAccessibilityAction(action,args)
        }
        return true
    }
    override fun onDraw(canvas: Canvas) {
        val viewport=navigation.viewport
        if(labelsStart!=viewport.start || labelsSpan!=viewport.span) {
            for(i in xLabels.indices) xLabels[i]=context.getString(R.string.analysis_axis_format,analysis.axisEnd*(viewport.start+viewport.span*i/4)/if(analysis.distanceAxis) 1 else 60)
            labelsStart=viewport.start;labelsSpan=viewport.span
        }
        val top = 12 * density; val bottom = height - 24 * density
        paint.style = Paint.Style.STROKE; paint.strokeWidth = density * .5f; paint.color = foreground; paint.alpha = 35
        for (i in 0..4) { val x = plotLeft+(plotRight-plotLeft)*i/4; canvas.drawLine(x, top, x, bottom, paint) }
        for (mark in scale.marks) { val y = bottom - (bottom - top) * ((mark-minimum)/(maximum-minimum)).toFloat(); canvas.drawLine(plotLeft, y, plotRight, y, paint) }
        paint.style = Paint.Style.FILL; paint.color = curve; paint.alpha = BicycleDesign.analysisChartFillAlpha
        canvas.save();canvas.clipRect(plotLeft,top,plotRight,bottom)
        canvas.translate(plotLeft-((plotRight-plotLeft)*viewport.start/viewport.span).toFloat(),0f)
        canvas.scale((1/viewport.span).toFloat(),1f);canvas.translate(-plotLeft,0f)
        canvas.drawPath(area,paint);canvas.restore()
        paint.style = Paint.Style.FILL; paint.color = foreground; paint.alpha = 180; paint.textSize = 9 * scaled
        paint.textAlign = Paint.Align.RIGHT
        for ((i,mark) in scale.marks.withIndex()) canvas.drawText(yLabels[i], plotLeft - 4 * density, bottom - (bottom - top) * ((mark-minimum)/(maximum-minimum)).toFloat() + paint.textSize / 3, paint)
        paint.textAlign = Paint.Align.CENTER
        for (i in 0..4) {
            paint.textAlign = if (i == 4) Paint.Align.RIGHT else if (i == 0) Paint.Align.LEFT else Paint.Align.CENTER
            canvas.drawText(xLabels[i], plotLeft+(plotRight-plotLeft)*i/4, height - 9 * density, paint)
        }
        paint.textAlign = Paint.Align.RIGHT; canvas.drawText(unit, plotRight, top - 2 * density, paint)
        canvas.drawText(axisUnit, plotRight, height.toFloat(), paint)
        titlePaint.color = foreground
        titlePaint.textSize = BicycleDesign.analysisSideLabelSp * scaled
        val available = (height - BicycleDesign.dp(context, BicycleDesign.verticalPadding) * 2).coerceAtLeast(1)
        val measured = titlePaint.measureText(title)
        if (measured > available) titlePaint.textSize *= available / measured
        canvas.save(); canvas.translate(BicycleDesign.dp(context, BicycleDesign.analysisSideLabelWidth) / 2f, height / 2f); canvas.rotate(-90f)
        canvas.drawText(title, 0f, -(titlePaint.ascent() + titlePaint.descent()) / 2, titlePaint); canvas.restore()
    }
}
