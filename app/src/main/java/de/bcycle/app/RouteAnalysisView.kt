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
            listOf(AnalysisMetric.AVG_SPEED, AnalysisMetric.MAX_SPEED)),
        R.string.analysis_elevation to listOf(listOf(AnalysisMetric.ASCENT), listOf(AnalysisMetric.DESCENT)),
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
    private var weatherTask:java.util.concurrent.Future<*>?=null
    private var weatherRevision=0L
    var correctElevation: (() -> Unit)? = null
    fun correctionStatus(busy:Boolean,error:String="") {
        findViewWithTag<TextView>("analysis_correction_status")?.let { BicycleFeedback.update(it,if(busy) context.getString(R.string.elevation_correcting) else error,hideWhenEmpty=true) }
    }
    init { tag = "route_analysis_$id"; BicycleComponents.scrollStyle(this); addView(body); loading() }
    fun cancellation(cancel: () -> Unit) { cancelled = cancel }
    fun dispose() { closed = true; weatherTask?.cancel(true); cancelled?.invoke(); preview?.dispose() }
    private fun text(resource: Int, heading: Boolean = false) = BicycleComponents.text(context, themes, context.getString(resource), heading)
    private fun reset() { weatherTask?.cancel(true); weatherRevision++;preview?.dispose(); preview = null; body.removeAllViews() }
    fun loading() { reset(); body.addView(BicycleFeedback.status(context, themes, context.getString(R.string.analysis_loading))) }
    fun failed(retry: () -> Unit) {
        if (closed) return
        reset(); body.addView(BicycleFeedback.status(context, themes, context.getString(R.string.analysis_failed)))
        body.addView(BicycleComponents.action(context, themes, context.getString(R.string.analysis_retry), retry))
    }
    fun show(analysis: RouteAnalysis) {
        if (closed) return
        reset()
        if (analysis.entry.details.dateMs > 0) body.addView(BicycleComponents.text(context, themes,
            DateTimeFormatter.ofPattern(context.getString(R.string.analysis_date_format)).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(analysis.entry.details.dateMs))).apply {
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT; tag = "analysis_date"; textSize = BicycleDesign.rowTitleSp
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(BicycleDesign.smallGap) })
        analysis.weatherTarget?.let { target ->
            val holder=FrameLayout(context).apply { visibility=GONE }
            body.addView(holder,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(BicycleDesign.smallGap) })
            val revision=weatherRevision
            weatherTask=RideWeather.load(context.applicationContext,id,target) { data ->
                if(!closed && revision==weatherRevision && data!=null) {
                    holder.removeAllViews();holder.addView(RideWeatherRow(context,themes,data));holder.visibility=VISIBLE
                }
            }
        }
        var groupsShown=0
        AnalysisGroups.groups.forEach { (heading, alternatives) ->
            val canCorrect=analysis.entry.format=="FIT" && analysis.entry.origin==TrackOrigin.RECORDED && analysis.entry.hasRoute
            val available = if(heading==R.string.analysis_elevation && canCorrect) alternatives
                else if (heading == R.string.analysis_power_group && alternatives.flatten().any { it in analysis.metrics }) alternatives
                else alternatives.map { group -> group.filter { it in analysis.metrics } }.filter { it.isNotEmpty() }
            if (available.isNotEmpty()) {
                if(groupsShown++>0) body.addView(BicycleComponents.sectionDivider(context,themes,BicycleDesign.analysisDividerGap).apply {
                    tag="analysis_section_divider"
                })
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; tag = "analysis_group_$heading"; isBaselineAligned = false }
                available.forEachIndexed { index, metrics ->
                    if (index > 0) row.addView(BicycleComponents.divider(context, themes).apply { alpha = .2f; tag = "analysis_vertical_divider" }, LinearLayout.LayoutParams(BicycleDesign.linePixels(context), -1))
                    row.addView(AnalysisMetricTile(context, themes, metrics, analysis.metrics), LinearLayout.LayoutParams(0, -1, 1f))
                }
                val section = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(row, LinearLayout.LayoutParams(-1, -2))
                    val distribution = when (heading) {
                        R.string.analysis_power_group -> analysis.zones.power
                        R.string.analysis_cadence_heart_group -> analysis.zones.heart
                        else -> null
                    }
                    if(heading==R.string.analysis_elevation && canCorrect) {
                        addView(BicycleFeedback.status(context,themes).apply { tag="analysis_correction_status";visibility=GONE })
                    }
                    if (distribution != null) addView(RouteZoneBars(context, themes, distribution,
                        if (heading == R.string.analysis_power_group) ZoneGlowColors.powerColors else TrainingZoneColors.values,
                        heading), LinearLayout.LayoutParams(-1, -2))
                }
                body.addView(LinearLayout(context).apply {
                    orientation=LinearLayout.HORIZONTAL;isBaselineAligned=false
                    addView(AnalysisSideLabel(context,themes,heading),LinearLayout.LayoutParams(dp(BicycleDesign.analysisSideLabelWidth),-1))
                    addView(section,LinearLayout.LayoutParams(0,-2,1f))
                }, LinearLayout.LayoutParams(-1, -2))
            }
        }
        if (analysis.bestEfforts.isNotEmpty()) {
            if (groupsShown > 0) body.addView(BicycleComponents.sectionDivider(context, themes,BicycleDesign.analysisDividerGap))
            val weight = RiderProfileStore(context.getSharedPreferences("bike", Context.MODE_PRIVATE)).read().weightKg
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; isBaselineAligned = false; tag = "analysis_best_efforts"
                addView(AnalysisSideLabel(context, themes, R.string.analysis_best_efforts), LinearLayout.LayoutParams(dp(BicycleDesign.analysisSideLabelWidth), -1))
            }
            analysis.bestEfforts.take(4).sortedBy { it.durationMs }.forEachIndexed { index, effort ->
                if (index > 0) row.addView(BicycleComponents.divider(context, themes).apply { alpha = .2f }, LinearLayout.LayoutParams(BicycleDesign.linePixels(context), -1))
                row.addView(BestEffortTile(context, themes, effort, weight), LinearLayout.LayoutParams(0, -1, 1f))
            }
            body.addView(row, LinearLayout.LayoutParams(-1, dp(BicycleDesign.analysisTileMinHeight)))
        }
        analysis.route?.let { route ->
            val frame = object:FrameLayout(context) {
                override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
                    val width=MeasureSpec.getSize(widthMeasureSpec)
                    super.onMeasure(widthMeasureSpec,MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY))
                }
            }.apply { tag="analysis_map_frame" }
            preview = RouteAnalysisMap(context, route, false)
            frame.addView(preview, FrameLayout.LayoutParams(-1, -1))
            frame.addView(BicycleComponents.icon(context, themes, BicycleIcon.Maximize, context.getString(R.string.analysis_map_fullscreen)) { fullMap(analysis) }.apply {
                BicycleComponents.bindTheme(this,themes) { setColorFilter(android.graphics.Color.BLACK) }
            },
                FrameLayout.LayoutParams(dp(BicycleDesign.touchSize), dp(BicycleDesign.touchSize), Gravity.TOP or Gravity.END))
            body.addView(frame, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(BicycleDesign.rowGap) })
        }
        if (analysis.series.isNotEmpty()) {
            analysis.series.forEach { body.addView(RouteChartView(context, themes, analysis, it), LinearLayout.LayoutParams(-1, dp(BicycleDesign.analysisChartHeight)).apply { topMargin = dp(BicycleDesign.smallGap) }) }
        }
        analysis.distributions.forEach { distribution ->
            body.addView(RouteDistributionView(context, themes, distribution), LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(BicycleDesign.smallGap)
            })
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
        textSize = BicycleDesign.analysisUnitSp; BicycleComponents.secondaryText(this,themes)
        setAutoSizeTextTypeUniformWithConfiguration(BicycleDesign.analysisUnitMinSp, BicycleDesign.analysisUnitSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        tag = "analysis_unit"
    }
    private val label = BicycleComponents.text(context, themes, "").apply {
        typeface = Typeface.DEFAULT; textSize = BicycleDesign.analysisLabelSp; gravity = Gravity.CENTER; maxLines = 3
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
        label.text = context.getString(if(metric==AnalysisMetric.NP) R.string.analysis_np_short else metric.label)
        val tokens = themes.active.tokens
        icon.setImageDrawable(BicycleIcons.drawable(context, metric.icon, tokens.foreground))
        contentDescription = if (alternatives.size > 1) context.getString(R.string.analysis_switch, label.text, formatted, unitText, context.getString(alternatives[(index + 1) % alternatives.size].let { if(it==AnalysisMetric.NP) R.string.analysis_np_short else it.label }))
            else context.getString(R.string.analysis_metric_description, label.text, formatted, unitText)
        if (metric == AnalysisMetric.FTP) contentDescription = "$contentDescription ${context.getString(R.string.analysis_ftp_explanation)}"
    }
    private fun dp(n: Int) = BicycleDesign.dp(context, n)
}

internal class BestEffortTile(context: Context, themes: ThemeManager, effort: BestEffort, weightKg: Double?) : LinearLayout(context) {
    init {
        orientation = VERTICAL; gravity = Gravity.CENTER; tag = "analysis_effort_${effort.durationMs}"
        val inset = BicycleDesign.dp(context, BicycleDesign.smallGap)
        setPadding(inset, inset, inset, inset)
        val seconds = effort.durationMs / 1000
        val period = when {
            seconds >= 3600 -> context.getString(R.string.analysis_effort_hours, seconds / 3600)
            seconds >= 60 -> context.getString(R.string.analysis_effort_minutes, seconds / 60)
            else -> context.getString(R.string.analysis_effort_seconds, seconds)
        }
        addView(BicycleComponents.text(context, themes, period).apply {
            gravity = Gravity.CENTER; textSize = BicycleDesign.analysisLabelSp; maxLines = 1
        }, LayoutParams(-1, -2))
        fun numeric(text: String, size: Float) = TextView(context).apply {
            this.text = text; typeface = resources.getFont(R.font.bebas_neue); textSize = size
            gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 1
            setAutoSizeTextTypeUniformWithConfiguration(BicycleDesign.analysisUnitMinSp, size.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        }
        val watts = numeric(context.getString(R.string.analysis_integer_format, effort.watts), BicycleDesign.analysisMetricSp)
        BicycleComponents.bindTheme(watts, themes) { watts.setTextColor(it.foreground) }
        val unit = numeric(context.getString(R.string.analysis_watts), BicycleDesign.analysisUnitSp).apply { BicycleComponents.secondaryText(this,themes) }
        addView(AnalysisValueRow(context, watts, unit), LayoutParams(-1, BicycleDesign.dp(context, 40)))
        val relative = effort.wattsPerKg(weightKg)?.let { context.getString(R.string.analysis_ratio_format, it) }
            ?: context.getString(R.string.analysis_missing)
        addView(BicycleComponents.text(context, themes, context.getString(R.string.analysis_effort_relative, relative)).apply {
            gravity = Gravity.CENTER; textSize = BicycleDesign.analysisUnitSp; maxLines = 1
            setAutoSizeTextTypeUniformWithConfiguration(BicycleDesign.analysisUnitMinSp, BicycleDesign.analysisUnitSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            BicycleComponents.secondaryText(this,themes)
        }, LayoutParams(-1, -2))
        contentDescription = context.getString(R.string.analysis_effort_description, period, watts.text, relative)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        for (i in 0 until childCount) getChildAt(i).importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }
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

internal fun analysisHeader(context: Context, themes: ThemeManager, entry: ManagedRoute, rename: (() -> Unit)? = null, back: () -> Unit): LinearLayout =
    BicycleComponents.header(context, themes).apply {
        tag = "route_analysis_header"; setPadding(BicycleDesign.dp(context, BicycleDesign.smallGap), 0, BicycleDesign.dp(context, BicycleDesign.verticalPadding), 0)
        addView(BicycleComponents.icon(context, themes, BicycleIcon.Back, context.getString(R.string.analysis_back), back))
        addView(BicycleComponents.heading(context, themes, entry.name).apply {
            ellipsize = TextUtils.TruncateAt.END; contentDescription = entry.name; tag = "analysis_title"
            if(rename!=null) { isFocusable=true;setOnClickListener { rename() } }
            setAutoSizeTextTypeUniformWithConfiguration(18, BicycleDesign.headingSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        }, LinearLayout.LayoutParams(0, -1, 1f))
    }
