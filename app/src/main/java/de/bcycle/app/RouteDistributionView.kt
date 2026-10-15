package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.view.View

/** Square native distribution plot. Histogram reduction is already complete on the loader worker;
 * display paths and shaders are cached by size. draw() performs no data analysis or smoothing.
 */
internal class RouteDistributionView(context: Context, themes: ThemeManager, val distribution: RouteValueDistribution) : View(context) {
    private val density = resources.displayMetrics.density
    private val scaled = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 1f, resources.displayMetrics)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = BicycleDesign.strongTypeface; textAlign = Paint.Align.CENTER }
    private val line = Path()
    private val area = Path()
    private val plot = RectF()
    private var fill: Shader? = null
    private var foreground = 0
    private val smooth = distribution.smoothed()
    private val title = context.getString(distribution.chart.detailLabel)
    private val unit = context.getString(distribution.chart.unit)
    private val marks = (listOf(distribution.minimum) + (1..3).map {
        kotlin.math.round((distribution.minimum + (distribution.maximum - distribution.minimum) * it / 4) / 5) * 5
    }.filter { it > distribution.minimum && it < distribution.maximum } + distribution.maximum).distinct()
    private val labels = marks.map { context.getString(R.string.analysis_integer_format, it) }
    private val zoneLabels = distribution.bands.map { context.getString(R.string.analysis_distribution_zone_number, it.zone) }
    private val zoneShaders = mutableListOf<Shader>()
    init {
        tag = "analysis_distribution_${distribution.chart.name}"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.analysis_distribution_description, title,
            context.getString(R.string.analysis_integer_format, distribution.minimum),
            context.getString(R.string.analysis_integer_format, distribution.maximum), unit)
        BicycleComponents.bindTheme(this, themes) { foreground = it.foreground; invalidate() }
    }
    internal fun plotBounds() = RectF(plot)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(width, heightMeasureSpec))
    }
    private fun x(value: Double) = plot.left + ((value - distribution.minimum) / (distribution.maximum - distribution.minimum)).toFloat() * plot.width()
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        plot.set((BicycleDesign.analysisSideLabelWidth + BicycleDesign.verticalPadding) * density, 30 * density, w - 12 * density, h - 30 * density)
        line.reset(); area.reset(); zoneShaders.clear()
        if (plot.width() <= 0 || plot.height() <= 0 || smooth.isEmpty()) return
        distribution.bands.forEach { band ->
            val rgb = band.color and 0x00ffffff
            zoneShaders += LinearGradient(0f, plot.top, 0f, plot.bottom, intArrayOf(rgb or (100 shl 24), rgb or (25 shl 24)), null, Shader.TileMode.CLAMP)
        }
        val peak = (smooth.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
        val points = smooth.mapIndexed { i, value -> PointF(plot.left + plot.width() * (i + .5f) / smooth.size, plot.bottom - (value / peak).toFloat() * plot.height() * .82f) }
        line.moveTo(plot.left, points.first().y)
        var previous = PointF(plot.left, points.first().y)
        for (next in points) {
            // Quadratic corner-cutting stays in the non-negative convex hull: no spline overshoot.
            line.quadTo(previous.x, previous.y, (previous.x + next.x) / 2, (previous.y + next.y) / 2)
            previous = next
        }
        line.quadTo(previous.x, previous.y, plot.right, points.last().y)
        area.set(line); area.lineTo(plot.right, plot.bottom); area.lineTo(plot.left, plot.bottom); area.close()
        val top = plot.bottom - plot.height() * .82f
        val alpha = LiveGraphStyle.fillAlpha(GraphDisplayMode.GRAPH_ONLY)
        fill = LinearGradient(0f, top, 0f, plot.bottom, intArrayOf(Color.argb(alpha,255,255,255), Color.argb(alpha/3,255,255,255), Color.TRANSPARENT), floatArrayOf(0f,.55f,1f), Shader.TileMode.CLAMP)
    }
    override fun onDraw(canvas: Canvas) {
        if (plot.width() <= 0 || plot.height() <= 0) return
        val saved = canvas.save(); canvas.clipRect(plot)
        paint.style = Paint.Style.FILL; paint.alpha = 255
        distribution.bands.forEachIndexed { i, band ->
            paint.shader = zoneShaders[i]
            canvas.drawRect(x(band.lower), plot.top, x(band.upper), plot.bottom, paint)
        }
        paint.shader = null; paint.color = foreground; paint.alpha = 30; paint.strokeWidth = density * .5f
        marks.forEach { value -> canvas.drawLine(x(value), plot.top, x(value), plot.bottom, paint) }
        paint.color = Color.WHITE; paint.alpha = 255; paint.shader = fill
        canvas.drawPath(area, paint)
        paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        for ((width, alpha) in listOf(LiveGraphStyle.OUTER_GLOW_WIDTH_DP to 24, LiveGraphStyle.INNER_GLOW_WIDTH_DP to 60, LiveGraphStyle.LINE_WIDTH_DP to LiveGraphStyle.LINE_ALPHA)) {
            paint.strokeWidth = width * density; paint.alpha = alpha; canvas.drawPath(line, paint)
        }
        canvas.restoreToCount(saved)
        paint.shader = null; paint.style = Paint.Style.FILL; paint.color = foreground; paint.textSize = BicycleDesign.analysisLabelSp * scaled
        paint.textAlign = Paint.Align.CENTER
        distribution.bands.forEachIndexed { i, band ->
            paint.textSize = BicycleDesign.analysisLabelSp * scaled
            val available = (x(band.upper) - x(band.lower)) * .8f
            val measured = paint.measureText(zoneLabels[i])
            if (measured > available) paint.textSize *= available / measured
            canvas.drawText(zoneLabels[i], (x(band.lower) + x(band.upper)) / 2, plot.top - 8 * density, paint)
        }
        paint.textSize = BicycleDesign.analysisLabelSp * scaled
        paint.color = BicycleDesign.dashboardUnitColor
        marks.forEachIndexed { i, value ->
            paint.textAlign = when (i) { 0 -> Paint.Align.LEFT; marks.lastIndex -> Paint.Align.RIGHT; else -> Paint.Align.CENTER }
            canvas.drawText(labels[i], x(value), plot.bottom + 16 * density, paint)
        }
        paint.textAlign = Paint.Align.RIGHT; canvas.drawText(unit, plot.right, height.toFloat() - density, paint)
        titlePaint.color = foreground; titlePaint.textSize = BicycleDesign.analysisSideLabelSp * scaled
        canvas.save(); canvas.translate(BicycleDesign.analysisSideLabelWidth * density / 2, height / 2f); canvas.rotate(-90f)
        canvas.drawText(title, 0f, -(titlePaint.ascent() + titlePaint.descent()) / 2, titlePaint); canvas.restore()
    }
}
