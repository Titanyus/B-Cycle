package de.bcycle.app

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal object AnalysisGroups {
    val groups = listOf(
        R.string.analysis_overview to listOf(listOf(AnalysisMetric.DISTANCE), listOf(AnalysisMetric.MOVING, AnalysisMetric.ELAPSED),
            listOf(AnalysisMetric.AVG_SPEED, AnalysisMetric.MAX_SPEED), listOf(AnalysisMetric.ASCENT, AnalysisMetric.DESCENT)),
        R.string.analysis_ebike to listOf(listOf(AnalysisMetric.AVG_MOTOR), listOf(AnalysisMetric.RIDER_BIKE_RATIO), listOf(AnalysisMetric.BATTERY_USAGE)),
        R.string.analysis_cadence_heart_group to listOf(listOf(AnalysisMetric.AVG_CADENCE, AnalysisMetric.MAX_CADENCE), listOf(AnalysisMetric.AVG_HEART, AnalysisMetric.MAX_HEART)),
        R.string.analysis_power_group to listOf(listOf(AnalysisMetric.AVG_POWER), listOf(AnalysisMetric.MAX_POWER), listOf(AnalysisMetric.NP)),
        R.string.analysis_energy_group to listOf(listOf(AnalysisMetric.WORK), listOf(AnalysisMetric.CALORIES)),
        R.string.analysis_performance_group to listOf(listOf(AnalysisMetric.FTP), listOf(AnalysisMetric.EF), listOf(AnalysisMetric.RATIO)))
}

/** One scrolling body. It owns its typography so generic dialog styling must not overwrite it. */
internal class RouteAnalysisView(context: Context, private val themes: ThemeManager, val id: String,
    private val fullMap: (RouteAnalysis) -> Unit) : ScrollView(context) {
    private val body = BicycleComponents.dialogBody(context, themes)
    private var preview: RouteAnalysisMap? = null
    private var cancelled: (() -> Unit)? = null
    private var closed = false
    init { tag = "route_analysis_$id"; addView(body); loading() }
    fun cancellation(cancel: () -> Unit) { cancelled = cancel }
    fun dispose() { closed = true; cancelled?.invoke(); preview?.dispose() }
    private fun text(resource: Int, heading: Boolean = false) = BicycleComponents.text(context, themes, context.getString(resource), heading)
    private fun reset() { preview?.dispose(); preview = null; body.removeAllViews() }
    fun loading() { reset(); body.addView(text(R.string.analysis_loading)) }
    fun failed(retry: () -> Unit) {
        if (closed) return
        reset(); body.addView(text(R.string.analysis_failed))
        body.addView(BicycleComponents.action(context, themes, context.getString(R.string.analysis_retry), retry))
    }
    fun show(analysis: RouteAnalysis) {
        if (closed) return
        reset()
        if (analysis.entry.details.dateMs > 0) body.addView(BicycleComponents.text(context, themes,
            DateTimeFormatter.ofPattern(context.getString(R.string.analysis_date_format)).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(analysis.entry.details.dateMs))).apply {
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT; tag = "analysis_date"; textSize = 12f
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(BicycleDesign.smallGap) })
        AnalysisGroups.groups.forEach { (heading, alternatives) ->
            val available = if (heading == R.string.analysis_power_group && alternatives.flatten().any { it in analysis.metrics }) alternatives
                else alternatives.map { group -> group.filter { it in analysis.metrics } }.filter { it.isNotEmpty() }
            if (available.isNotEmpty()) {
                body.addView(text(heading, true).apply { isAccessibilityHeading = true }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(BicycleDesign.smallGap); topMargin = dp(BicycleDesign.verticalPadding) })
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; tag = "analysis_group_$heading"; isBaselineAligned = false }
                available.forEachIndexed { index, metrics ->
                    if (index > 0) row.addView(BicycleComponents.divider(context, themes).apply { alpha = .2f; tag = "analysis_vertical_divider" }, LinearLayout.LayoutParams(BicycleDesign.dividerPixels, -1))
                    row.addView(AnalysisMetricTile(context, themes, metrics, analysis.metrics), LinearLayout.LayoutParams(0, -1, 1f))
                }
                body.addView(row, LinearLayout.LayoutParams(-1, -2))
            }
        }
        analysis.route?.let { route ->
            body.addView(text(R.string.analysis_map_heading, true).apply { isAccessibilityHeading = true })
            val frame = FrameLayout(context)
            preview = RouteAnalysisMap(context, route, false)
            frame.addView(preview, FrameLayout.LayoutParams(-1, -1))
            frame.addView(BicycleComponents.icon(context, themes, BicycleIcon.Maximize, context.getString(R.string.analysis_map_fullscreen)) { fullMap(analysis) },
                FrameLayout.LayoutParams(dp(BicycleDesign.touchSize), dp(BicycleDesign.touchSize), Gravity.TOP or Gravity.END))
            body.addView(frame, LinearLayout.LayoutParams(-1, dp(BicycleDesign.analysisMapHeight)).apply { topMargin = dp(BicycleDesign.rowGap) })
        }
        if (analysis.series.isNotEmpty()) {
            body.addView(text(R.string.analysis_charts, true).apply { isAccessibilityHeading = true }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(BicycleDesign.verticalPadding) })
            analysis.series.forEach { body.addView(RouteChartView(context, themes, analysis, it), LinearLayout.LayoutParams(-1, dp(BicycleDesign.analysisChartHeight)).apply { topMargin = dp(BicycleDesign.smallGap) }) }
        }
        if (body.childCount == 0) body.addView(text(R.string.analysis_no_data))
    }
    private fun dp(n: Int) = BicycleDesign.dp(context, n)
}

internal class AnalysisMetricTile(context: Context, private val themes: ThemeManager, val alternatives: List<AnalysisMetric>, private val values: Map<AnalysisMetric, Double>) : FrameLayout(context) {
    private var index = 0
    private val value = BicycleComponents.text(context, themes, "").apply {
        typeface = resources.getFont(R.font.bebas_neue); textSize = BicycleDesign.analysisMetricSp; gravity = Gravity.CENTER
        includeFontPadding = false; maxLines = 1
        setAutoSizeTextTypeUniformWithConfiguration(16, BicycleDesign.analysisMetricSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
    }
    private val unit = TextView(context).apply {
        typeface = resources.getFont(R.font.bebas_neue); gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 1
        textSize = 13f; setTextColor(BicycleDesign.dashboardUnitColor)
        setAutoSizeTextTypeUniformWithConfiguration(8, 13, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        tag = "analysis_unit"
    }
    private val label = BicycleComponents.text(context, themes, "").apply {
        typeface = Typeface.DEFAULT; textSize = 11f; gravity = Gravity.CENTER; maxLines = 3
    }
    private val icon = ImageView(context)
    private val switching = ImageView(context)
    private val metrics = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
    init {
        tag = "analysis_metric_${alternatives.first().name}"; minimumHeight = dp(BicycleDesign.analysisTileMinHeight)
        val inset = dp(BicycleDesign.smallGap); setPadding(inset, dp(BicycleDesign.verticalPadding), inset, dp(BicycleDesign.verticalPadding))
        background = null
        metrics.addView(icon, LinearLayout.LayoutParams(dp(BicycleDesign.analysisMetricIconSize), dp(BicycleDesign.analysisMetricIconSize)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        metrics.addView(AnalysisValueRow(context, value, unit), LinearLayout.LayoutParams(-1, dp(40)))
        metrics.addView(label, LinearLayout.LayoutParams(-1, -2))
        addView(metrics, LayoutParams(-1, -2, Gravity.CENTER))
        if (alternatives.size > 1) {
            switching.setImageDrawable(BicycleIcons.drawable(context, BicycleIcon.SwitchMetric, themes.active.tokens.foreground))
            switching.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            addView(switching, LayoutParams(dp(14), dp(14), Gravity.TOP or Gravity.END))
            setOnClickListener { index = (index + 1) % alternatives.size; update() }
        }
        BicycleComponents.bindTheme(this, themes) { tokens ->
            icon.setImageDrawable(BicycleIcons.drawable(context, alternatives[index].icon, tokens.foreground))
            switching.setColorFilter(tokens.foreground)
        }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        listOf(icon, value, unit, label).forEach { it.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        update()
    }
    private fun update() {
        val metric = alternatives[index]; val number = values[metric]
        val formatted = if (number == null) context.getString(R.string.analysis_missing) else if (metric == AnalysisMetric.MOVING || metric == AnalysisMetric.ELAPSED) {
            val seconds = number.toLong(); context.getString(R.string.analysis_duration_format, seconds / 3600, seconds / 60 % 60, seconds % 60)
        } else context.getString(when (metric) {
            AnalysisMetric.EF, AnalysisMetric.RATIO, AnalysisMetric.RIDER_BIKE_RATIO -> R.string.analysis_ratio_format
            AnalysisMetric.DISTANCE, AnalysisMetric.AVG_SPEED, AnalysisMetric.MAX_SPEED -> R.string.analysis_number_format
            else -> R.string.analysis_integer_format
        }, number)
        val unitText = context.getString(metric.unit)
        value.text = formatted; unit.text = unitText; unit.visibility = if (unitText.isEmpty()) GONE else VISIBLE
        label.text = context.getString(metric.label)
        val tokens = themes.active.tokens
        icon.setImageDrawable(BicycleIcons.drawable(context, metric.icon, tokens.foreground))
        contentDescription = if (alternatives.size > 1) context.getString(R.string.analysis_switch, label.text, formatted, unitText, context.getString(alternatives[(index + 1) % alternatives.size].label))
            else context.getString(R.string.analysis_metric_description, label.text, formatted, unitText)
        if (metric == AnalysisMetric.FTP) contentDescription = "$contentDescription ${context.getString(R.string.analysis_ftp_explanation)}"
    }
    private fun dp(n: Int) = BicycleDesign.dp(context, n)
}

/** Measure once within available width, then center the combined number+unit and both vertical centers. */
internal class AnalysisValueRow(context: Context, private val value: TextView, private val unit: TextView) : android.view.ViewGroup(context) {
    private val gap = BicycleDesign.dp(context, BicycleDesign.smallGap)
    init { addView(value); addView(unit) }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (unit.visibility != GONE) unit.measure(MeasureSpec.makeMeasureSpec(available / 2, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
        val reserve = if (unit.visibility == GONE) 0 else unit.measuredWidth + gap
        value.measure(MeasureSpec.makeMeasureSpec((available - reserve).coerceAtLeast(0), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
        setMeasuredDimension(resolveSize(value.measuredWidth + reserve, widthMeasureSpec), resolveSize(maxOf(value.measuredHeight, if (unit.visibility == GONE) 0 else unit.measuredHeight), heightMeasureSpec))
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val y = (height - value.measuredHeight) / 2
        value.layout(0, y, value.measuredWidth, y + value.measuredHeight)
        if (unit.visibility != GONE) {
            val uy = (height - unit.measuredHeight) / 2
            unit.layout(0, uy, unit.measuredWidth, uy + unit.measuredHeight)
            // Native TextView autosizing finishes in layout: place actual glyphs, not oversized text boxes.
            val numberWidth = value.layout?.getLineWidth(0) ?: value.measuredWidth.toFloat()
            val unitWidth = unit.layout?.getLineWidth(0) ?: unit.measuredWidth.toFloat()
            val start = (width - numberWidth - gap - unitWidth) / 2
            value.offsetLeftAndRight((start - (value.measuredWidth - numberWidth) / 2).toInt())
            unit.offsetLeftAndRight((start + numberWidth + gap - (unit.measuredWidth - unitWidth) / 2).toInt())
        } else value.offsetLeftAndRight((width - value.measuredWidth) / 2)
    }
}

internal fun analysisHeader(context: Context, themes: ThemeManager, entry: ManagedRoute, back: () -> Unit): LinearLayout =
    BicycleComponents.header(context, themes).apply {
        tag = "route_analysis_header"; setPadding(BicycleDesign.dp(context, BicycleDesign.smallGap), 0, BicycleDesign.dp(context, BicycleDesign.verticalPadding), 0)
        addView(BicycleComponents.icon(context, themes, BicycleIcon.Back, context.getString(R.string.analysis_back), back))
        addView(BicycleComponents.heading(context, themes, entry.name).apply {
            ellipsize = TextUtils.TruncateAt.END; contentDescription = entry.name; tag = "analysis_title"
            setAutoSizeTextTypeUniformWithConfiguration(18, BicycleDesign.headingSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        }, LinearLayout.LayoutParams(0, -1, 1f))
    }
