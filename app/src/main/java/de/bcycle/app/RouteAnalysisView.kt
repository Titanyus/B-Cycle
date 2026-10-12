package de.bcycle.app

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
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
        R.string.analysis_power_group to listOf(listOf(AnalysisMetric.AVG_POWER), listOf(AnalysisMetric.MAX_POWER), listOf(AnalysisMetric.NP)),
        R.string.analysis_cadence_heart_group to listOf(listOf(AnalysisMetric.AVG_CADENCE, AnalysisMetric.MAX_CADENCE), listOf(AnalysisMetric.AVG_HEART, AnalysisMetric.MAX_HEART)),
        R.string.analysis_energy_group to listOf(listOf(AnalysisMetric.WORK), listOf(AnalysisMetric.CALORIES)),
        R.string.analysis_performance_group to listOf(listOf(AnalysisMetric.EF), listOf(AnalysisMetric.RATIO)))
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
        AnalysisGroups.groups.forEach { (heading, alternatives) ->
            val available = alternatives.map { group -> group.filter { it in analysis.metrics } }.filter { it.isNotEmpty() }
            if (available.isNotEmpty()) {
                body.addView(text(heading, true).apply { isAccessibilityHeading = true }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(BicycleDesign.smallGap); topMargin = dp(BicycleDesign.verticalPadding) })
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; tag = "analysis_group_$heading"; isBaselineAligned = false }
                available.forEachIndexed { index, metrics ->
                    row.addView(AnalysisMetricTile(context, themes, metrics, analysis.metrics), LinearLayout.LayoutParams(0, -1, 1f).apply { if (index > 0) marginStart = dp(BicycleDesign.smallGap) })
                }
                body.addView(row, LinearLayout.LayoutParams(-1, -2))
            }
        }
        analysis.route?.let { route ->
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
    private val label = BicycleComponents.text(context, themes, "").apply {
        typeface = Typeface.DEFAULT; textSize = 11f; gravity = Gravity.CENTER; maxLines = 3
    }
    private val icon = ImageView(context)
    private val switching = ImageView(context)
    private val metrics = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
    init {
        tag = "analysis_metric_${alternatives.first().name}"; minimumHeight = dp(BicycleDesign.analysisTileMinHeight)
        val inset = dp(BicycleDesign.smallGap); setPadding(inset, dp(BicycleDesign.verticalPadding), inset, dp(BicycleDesign.verticalPadding))
        BicycleComponents.background(this, themes, ThemeRole.TERTIARY, BicycleDesign.inputRadius)
        metrics.addView(icon, LinearLayout.LayoutParams(dp(BicycleDesign.analysisMetricIconSize), dp(BicycleDesign.analysisMetricIconSize)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        metrics.addView(value, LinearLayout.LayoutParams(-1, dp(40)))
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
        listOf(icon, value, label).forEach { it.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        update()
    }
    private fun update() {
        val metric = alternatives[index]; val number = values.getValue(metric)
        val formatted = if (metric == AnalysisMetric.MOVING || metric == AnalysisMetric.ELAPSED) {
            val seconds = number.toLong(); context.getString(R.string.analysis_duration_format, seconds / 3600, seconds / 60 % 60, seconds % 60)
        } else context.getString(when (metric) {
            AnalysisMetric.EF, AnalysisMetric.RATIO -> R.string.analysis_ratio_format
            AnalysisMetric.DISTANCE, AnalysisMetric.AVG_SPEED, AnalysisMetric.MAX_SPEED -> R.string.analysis_number_format
            else -> R.string.analysis_integer_format
        }, number)
        val unit = context.getString(metric.unit)
        value.text = SpannableString("$formatted $unit").apply { setSpan(RelativeSizeSpan(.48f), formatted.length + 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        label.text = context.getString(metric.label)
        val tokens = themes.active.tokens
        icon.setImageDrawable(BicycleIcons.drawable(context, metric.icon, tokens.foreground))
        contentDescription = if (alternatives.size > 1) context.getString(R.string.analysis_switch, label.text, formatted, unit, context.getString(alternatives[(index + 1) % alternatives.size].label))
            else context.getString(R.string.analysis_metric_description, label.text, formatted, unit)
    }
    private fun dp(n: Int) = BicycleDesign.dp(context, n)
}

internal fun analysisHeader(context: Context, themes: ThemeManager, entry: ManagedRoute, back: () -> Unit): LinearLayout =
    BicycleComponents.header(context, themes).apply {
        tag = "route_analysis_header"; setPadding(BicycleDesign.dp(context, BicycleDesign.smallGap), 0, BicycleDesign.dp(context, BicycleDesign.verticalPadding), 0)
        addView(BicycleComponents.icon(context, themes, BicycleIcon.Back, context.getString(R.string.analysis_back), back))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            addView(BicycleComponents.heading(context, themes, entry.name).apply {
                ellipsize = TextUtils.TruncateAt.END; gravity = Gravity.START or Gravity.CENTER_VERTICAL
                contentDescription = entry.name; tag = "analysis_title"
                setAutoSizeTextTypeUniformWithConfiguration(18, BicycleDesign.headingSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            }, LinearLayout.LayoutParams(-1, 0, 1f))
            if (entry.details.dateMs > 0) addView(BicycleComponents.text(context, themes,
                DateTimeFormatter.ofPattern(context.getString(R.string.analysis_date_format)).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(entry.details.dateMs))).apply { textSize = 12f; tag = "analysis_date" })
        }, LinearLayout.LayoutParams(0, -1, 1f))
    }
