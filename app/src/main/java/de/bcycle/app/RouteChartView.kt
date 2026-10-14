package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View


/** Compact native Canvas chart; geometry/labels cached when its size changes, not while scrolling. */
internal class RouteChartView(context: Context, themes: ThemeManager, val analysis: RouteAnalysis, val series: AnalysisSeries) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Path()
    private val area = Path()
    private var foreground = 0; private var curve = 0
    private val density = resources.displayMetrics.density
    private val scaled = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 1f, resources.displayMetrics)
    internal val plotLeft get() = 58f * density
    internal val plotRight get() = width - 8f * density
    internal fun screenX(position: Double) = plotLeft + (position / analysis.axisEnd).toFloat() * (plotRight - plotLeft)
    internal var scale=RouteChartScale(0.0,5.0,emptyList()); private set
    private val minimum get()=scale.minimum
    private val maximum get()=scale.maximum
    private val xLabels = Array(5) { "" }; private var yLabels = emptyList<String>()
    private val title = context.getString(series.chart.detailLabel)
    private val unit = context.getString(series.chart.unit)
    private val axisUnit = context.getString(if (analysis.distanceAxis) R.string.analysis_km else R.string.analysis_minutes)
    init {
        tag = "analysis_chart_${series.chart.name}"
        contentDescription = context.getString(R.string.analysis_chart_description, title, axisUnit, unit)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        BicycleComponents.bindTheme(this, themes) { tokens ->
            foreground = tokens.foreground; curve = series.chart.color(tokens.secondary); invalidate()
        }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        scale=RouteChartScale.calculate(series.chart,series.points.mapNotNull { it.value })
        val top = 12 * density; val bottom = h - 24 * density
        line.reset(); area.reset(); var connected = false; var lastX = 0f; var firstX = 0f
        fun closeArea() { if (connected) { area.lineTo(lastX, bottom); area.lineTo(firstX, bottom); area.close() } }
        for (point in series.points) {
            val distance = if (analysis.distanceAxis) point.distance?.div(1000) else point.seconds
            if (point.value == null || !point.value.isFinite() || distance == null || !distance.isFinite()) { closeArea(); connected = false }
            if (point.value != null && point.value.isFinite() && distance != null && distance.isFinite() && distance in 0.0..analysis.axisEnd) {
                val x = screenX(distance); val y = bottom - ((point.value - minimum) / (maximum - minimum)).toFloat() * (bottom - top)
                if (!connected) { line.moveTo(x, y); area.moveTo(x, bottom); area.lineTo(x, y); firstX = x }
                else { line.lineTo(x, y); area.lineTo(x, y) }
                connected = true; lastX = x
            }
        }
        closeArea()
        for (i in xLabels.indices) xLabels[i] = context.getString(R.string.analysis_axis_format, analysis.axisEnd * i / 4 / if (analysis.distanceAxis) 1 else 60)
        yLabels=scale.marks.map { context.getString(if(it%1.0==0.0) R.string.analysis_integer_format else R.string.analysis_axis_format,it) }
    }
    override fun onDraw(canvas: Canvas) {
        val top = 12 * density; val bottom = height - 24 * density
        paint.style = Paint.Style.STROKE; paint.strokeWidth = density * .5f; paint.color = foreground; paint.alpha = 35
        for (i in 0..4) { val x = screenX(analysis.axisEnd * i / 4); canvas.drawLine(x, top, x, bottom, paint) }
        for (mark in scale.marks) { val y = bottom - (bottom - top) * ((mark-minimum)/(maximum-minimum)).toFloat(); canvas.drawLine(plotLeft, y, plotRight, y, paint) }
        paint.style = Paint.Style.FILL; paint.color = curve; paint.alpha = 32; canvas.drawPath(area, paint)
        paint.style = Paint.Style.STROKE; paint.color = curve; paint.alpha = 255; paint.strokeWidth = 1.5f * density
        canvas.drawPath(line, paint)
        paint.style = Paint.Style.FILL; paint.color = foreground; paint.alpha = 180; paint.textSize = 9 * scaled
        paint.textAlign = Paint.Align.RIGHT
        for ((i,mark) in scale.marks.withIndex()) canvas.drawText(yLabels[i], plotLeft - 4 * density, bottom - (bottom - top) * ((mark-minimum)/(maximum-minimum)).toFloat() + paint.textSize / 3, paint)
        paint.textAlign = Paint.Align.CENTER
        for (i in 0..4) {
            paint.textAlign = if (i == 4) Paint.Align.RIGHT else if (i == 0) Paint.Align.LEFT else Paint.Align.CENTER
            canvas.drawText(xLabels[i], screenX(analysis.axisEnd * i / 4), height - 9 * density, paint)
        }
        paint.textAlign = Paint.Align.RIGHT; canvas.drawText(unit, plotRight, top - 2 * density, paint)
        canvas.drawText(axisUnit, plotRight, height.toFloat(), paint)
        paint.alpha = 255; paint.textSize = BicycleDesign.analysisSideLabelSp * scaled
        val available = height - 12 * density
        if (paint.measureText(title) > available) paint.textSize *= available / paint.measureText(title)
        paint.textAlign = Paint.Align.CENTER
        canvas.save(); canvas.translate(12 * density, height / 2f); canvas.rotate(-90f)
        canvas.drawText(title, 0f, paint.textSize / 3, paint); canvas.restore()
    }
}
