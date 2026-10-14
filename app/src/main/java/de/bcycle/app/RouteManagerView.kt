package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos

/** Fixed header/filter, one native recycled list, no per-row decoding or disk/network I/O. */
internal class RouteManagerView(context: Context,
    private val toggle: (ManagedRoute) -> Unit,
    private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)),
    private val details: (ManagedRoute) -> Unit = {}) : LinearLayout(context) {
    private fun dp(value: Int) = BicycleDesign.dp(context, value)
    internal var fullscreen = false
    private var all: List<ManagedRoute> = emptyList()
    private var filtered: List<ManagedRoute> = emptyList()
    private var selected: String? = null
    private var busy = false
    private val filter = Spinner(context).apply { tag = "route_filter"; contentDescription = "Strecken filtern" }
    private val status = BicycleComponents.text(context, themes, "").apply {
        setPadding(dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.horizontalPadding), 0)
    }
    private val rows = object : BaseAdapter() {
        override fun getCount() = filtered.size
        override fun getItem(position: Int) = filtered[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, recycled: View?, parent: ViewGroup): View =
            ((recycled as? Card) ?: Card()).apply { bind(filtered[position]) }
    }
    init {
        orientation = VERTICAL
        BicycleComponents.background(this, themes, ThemeRole.SECONDARY)
        val header = BicycleComponents.header(context, themes).apply { tag = "route_manager_header" }
        header.setPadding(dp(BicycleDesign.verticalPadding), 0, dp(BicycleDesign.verticalPadding), 0)
        header.addView(FrameLayout(context).apply {
            addView(BicycleComponents.heading(context, themes, "Streckenmanager"), FrameLayout.LayoutParams(-1, -1))
        }, LayoutParams(-1, -1))
        addView(header, LayoutParams(-1, dp(BicycleDesign.headerHeight)))
        fun filterBackground(tokens: ThemeTokens) = GradientDrawable().apply {
            setColor(tokens.tertiary); cornerRadius = dp(BicycleDesign.inputRadius).toFloat()
        }
        BicycleComponents.bindTheme(filter, themes) { tokens ->
            filter.background = LayerDrawable(arrayOf(filterBackground(tokens), BicycleIcons.drawable(context, BicycleIcon.Expand, tokens.foreground))).apply {
                setLayerSize(1, dp(BicycleDesign.iconSize), dp(BicycleDesign.iconSize))
                setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL); setLayerInsetEnd(1, dp(BicycleDesign.horizontalPadding))
            }
            filter.setPopupBackgroundDrawable(filterBackground(tokens))
        }
        filter.adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listOf("Alle", "Gefahren", "Importiert")) {
            private fun row(position: Int, recycled: View?, dropdown: Boolean): View =
                ((recycled as? TextView) ?: BicycleComponents.text(context, themes, "").apply {
                    gravity = Gravity.CENTER_VERTICAL; minHeight = dp(BicycleDesign.touchSize)
                    BicycleComponents.bindTheme(this, themes) { setTextColor(it.foreground); setBackgroundColor(if (dropdown) it.tertiary else Color.TRANSPARENT) }
                }).apply {
                    text = getItem(position); textSize = BicycleDesign.inputSp
                    setPadding(dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding), dp(if (dropdown) BicycleDesign.horizontalPadding else BicycleDesign.touchSize), dp(BicycleDesign.verticalPadding))
                }
            override fun getView(position: Int, convertView: View?, parent: ViewGroup) = row(position, convertView, false)
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) = row(position, convertView, true)
        }
        addView(filter, LayoutParams(-1, dp(BicycleDesign.touchSize)).apply {
            setMargins(dp(BicycleDesign.verticalPadding), dp(BicycleDesign.rowGap), dp(BicycleDesign.verticalPadding), 0)
        })
        BicycleComponents.controlSpacing(filter)
        filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { updateRows() }
        }
        addView(status, LayoutParams(-1, -2))
        addView(ListView(context).apply {
            tag = "route_list"; adapter = rows; dividerHeight = BicycleDesign.dividerPixels
            setPadding(dp(BicycleDesign.verticalPadding), 0, dp(BicycleDesign.verticalPadding), 0); clipToPadding = false
            scrollBarSize = dp(BicycleDesign.scrollbarSize)
            BicycleComponents.bindTheme(this, themes) {
                divider = ColorDrawable(it.foreground); dividerHeight = BicycleDesign.dividerPixels
                verticalScrollbarThumbDrawable = ColorDrawable(it.foreground)
                selector = GradientDrawable().apply { setColor(it.secondary); setStroke(BicycleDesign.dividerPixels, it.foreground) }
            }
        }, LayoutParams(-1, 0, 1f))
    }
    fun submit(entries: List<ManagedRoute>, active: String?, working: Boolean) {
        all = entries; selected = active; busy = working; updateRows()
    }
    private fun updateRows() {
        filtered = all.filter { when (filter.selectedItemPosition) {
            1 -> it.origin == TrackOrigin.RECORDED; 2 -> it.origin == TrackOrigin.IMPORTED; else -> true
        } }
        status.text = if (!busy && filtered.isEmpty()) "Keine Strecken in dieser Auswahl" else ""
        status.visibility = if (status.text.isEmpty()) GONE else VISIBLE
        rows.notifyDataSetChanged()
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (fullscreen) { super.onMeasure(widthMeasureSpec, heightMeasureSpec); return }
        val desired = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) dp(BicycleDesign.managerDefaultHeight)
            else minOf(MeasureSpec.getSize(heightMeasureSpec), dp(BicycleDesign.managerMaxHeight))
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY))
    }
    private inner class Card : LinearLayout(context) {
        private var entry: ManagedRoute? = null
        private val preview = RoutePreviewView(context)
        private val origin = ImageView(context)
        private val format = BicycleComponents.text(context, themes, "").apply { textSize = BicycleDesign.formatSp }
        private val title = BicycleComponents.text(context, themes, "").apply {
            textSize = BicycleDesign.rowTitleSp; maxLines = 1; ellipsize = TextUtils.TruncateAt.END; gravity = Gravity.CENTER_VERTICAL
        }
        private val eye = BicycleComponents.icon(context, themes, BicycleIcon.ShowOnMap,
            context.getString(R.string.route_list_visibility)) { entry?.takeIf { !busy && it.hasRoute && it.locallySaved }?.let(toggle) }
        private inner class Reading(labelId: Int? = null, unitId: Int? = null) : LinearLayout(context) {
            val number = BicycleComponents.text(context, themes, "").apply {
                typeface = resources.getFont(R.font.bebas_neue); textSize = BicycleDesign.routeMetricSp
                gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 1
                setAutoSizeTextTypeUniformWithConfiguration(if (labelId == null) 10 else 12, BicycleDesign.routeMetricSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            }
            val unit = TextView(context).apply {
                text = unitId?.let(context::getString).orEmpty(); typeface = resources.getFont(R.font.bebas_neue)
                textSize = BicycleDesign.formatSp; gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 1
                setTextColor(BicycleDesign.dashboardUnitColor)
                setAutoSizeTextTypeUniformWithConfiguration(8, BicycleDesign.formatSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            }
            val label = BicycleComponents.text(context, themes, labelId?.let(context::getString).orEmpty()).apply {
                textSize = BicycleDesign.routeMetricLabelSp; gravity = Gravity.CENTER; includeFontPadding = false
                maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            }
            init {
                orientation = VERTICAL
                addView(AnalysisValueRow(context, number, unit), LayoutParams(-1, dp(BicycleDesign.routeMetricHeight)))
                addView(label, LayoutParams(-1, -2))
            }
            fun bind(value: String?, id: String, kind: String) {
                number.text = value.orEmpty(); number.tag = "route_${kind}_$id"
                unit.visibility = if (value == null || unit.text.isEmpty()) GONE else VISIBLE
            }
        }
        private val date = Reading()
        private val distance = Reading(R.string.route_list_distance, R.string.analysis_km)
        private val moving = Reading(R.string.route_list_moving, R.string.analysis_hours)
        private val average = Reading(R.string.route_list_average, R.string.analysis_kmh)
        private val metrics = LinearLayout(context).apply {
            orientation = HORIZONTAL
            listOf(date, distance, moving, average).forEachIndexed { index, reading ->
                if (index > 0) addView(View(context).apply {
                    tag = "route_metric_divider"; alpha = BicycleDesign.dividerAlpha
                    BicycleComponents.bindTheme(this, themes) { setBackgroundColor(it.foreground) }
                }, LayoutParams(BicycleDesign.dividerPixels, -1))
                addView(reading, LayoutParams(0, -2, 1f))
            }
        }
        init {
            orientation = HORIZONTAL; gravity = Gravity.TOP
            setOnClickListener { entry?.takeIf { !busy }?.let(details) }
            setPadding(dp(BicycleDesign.verticalPadding), dp(BicycleDesign.rowGap), 0, dp(BicycleDesign.rowGap))
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                addView(LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(origin, LayoutParams(dp(BicycleDesign.iconSize), dp(BicycleDesign.iconSize)))
                    addView(format, LayoutParams(-2, -2).apply { marginStart = dp(BicycleDesign.verticalPadding) })
                }, LayoutParams(-1, dp(BicycleDesign.touchSize)))
                addView(preview, LayoutParams(-1, dp(BicycleDesign.previewHeight)))
            }, LayoutParams(dp(BicycleDesign.previewWidth), -2).apply { marginEnd = dp(BicycleDesign.rowGap) })
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                addView(LinearLayout(context).apply {
                    addView(title, LayoutParams(0, dp(BicycleDesign.touchSize), 1f))
                    addView(eye, LayoutParams(dp(BicycleDesign.touchSize), dp(BicycleDesign.touchSize)))
                }, LayoutParams(-1, -2))
                addView(metrics, LayoutParams(-1, -2).apply { topMargin = -dp(BicycleDesign.metadataSpacingTrim) + dp(BicycleDesign.routeMetricTopOffset) })
            }, LayoutParams(0, -2, 1f))
            BicycleComponents.bindTheme(this, themes) { paintState(it) }
        }
        private fun paintState(tokens: ThemeTokens) {
            preview.foreground = tokens.foreground
            val route = entry ?: return
            origin.setImageDrawable(BicycleIcons.drawable(context, when (route.origin) {
                TrackOrigin.IMPORTED -> BicycleIcon.TrackImported; TrackOrigin.RECORDED -> BicycleIcon.TrackRecorded; TrackOrigin.UNKNOWN -> BicycleIcon.TrackUnknown
            }, tokens.foreground))
            val eyeKind = if (selected == route.id) BicycleIcon.ActiveOnMap else BicycleIcon.ShowOnMap
            eye.setImageDrawable(BicycleIcons.drawable(context, eyeKind, tokens, selected != route.id))
            eye.setColorFilter(BicycleIcons.color(tokens, eyeKind, selected != route.id))
        }
        fun bind(route: ManagedRoute) {
            entry = route; tag = "route_entry_${route.id}"; isEnabled = !busy
            contentDescription = context.getString(R.string.analysis_open, route.name)
            title.text = route.name; title.tag = "route_name_${route.id}"; title.contentDescription = route.name
            origin.contentDescription = when (route.origin) { TrackOrigin.IMPORTED -> "Importierte Strecke"; TrackOrigin.RECORDED -> "Aufgezeichnete Fahrt"; TrackOrigin.UNKNOWN -> "Herkunft unbekannt" }
            format.text = route.format; format.contentDescription = "${route.format}-Datei"
            val recordedAt = route.details.dateMs.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
            date.bind(recordedAt?.format(DateTimeFormatter.ofPattern(context.getString(R.string.route_list_date_format))), route.id, "date")
            date.label.text = recordedAt?.format(DateTimeFormatter.ofPattern(context.getString(R.string.route_list_clock_format))).orEmpty()
            date.label.tag = "route_clock_${route.id}"
            distance.bind(route.details.distanceM?.takeIf { it.isFinite() && it >= 0 }?.let {
                String.format(Locale.GERMANY, "%.1f", it / 1000)
            }, route.id, "distance")
            moving.bind(route.details.movingMs?.takeIf { it >= 0 && (route.details.durationMs == null || it <= route.details.durationMs) }?.let {
                val minutes = it / 60000; "%d:%02d".format(Locale.GERMANY, minutes / 60, minutes % 60)
            }, route.id, "moving")
            average.bind(route.details.movingAverageKmh?.let { String.format(Locale.GERMANY, "%.1f", it) }, route.id, "average")
            metrics.tag = "route_metrics_${route.id}"
            preview.points = route.details.preview; preview.contentDescription = if (route.hasRoute) "Streckenvorschau" else "Keine GPS-Strecke gespeichert"
            eye.tag = "route_eye_${route.id}"; eye.isEnabled = !busy && route.hasRoute && route.locallySaved
            eye.contentDescription = context.getString(if (selected == route.id) R.string.route_list_hide_named else R.string.route_list_show_named, route.name)
            paintState(themes.active.tokens)
        }
    }
}

internal class RoutePreviewView(context: Context) : View(context) {
    var points: List<MapPoint> = emptyList(); set(value) { field = value; invalidate() }
    var foreground: Int = 0; set(value) { field = value; paint.color = value; invalidate() }
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = foreground; style = Paint.Style.STROKE
        strokeWidth = 2 * resources.displayMetrics.density; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = 4 * resources.displayMetrics.density
        if (points.size < 2) { canvas.drawRect(padding, height * .25f, width - padding, height * .75f, paint); return }
        val origin = points.first().longitude
        val factor = cos(Math.toRadians(points.first().latitude))
        fun x(p: MapPoint): Double = (((p.longitude - origin + 540) % 360) - 180) * factor
        val minX = points.minOf(::x); val maxX = points.maxOf(::x)
        val minY = points.minOf { it.latitude }; val maxY = points.maxOf { it.latitude }
        val scale = minOf((width - padding * 2) / (maxX - minX).coerceAtLeast(.000001),
            (height - padding * 2) / (maxY - minY).coerceAtLeast(.000001))
        path.reset()
        points.forEachIndexed { i, p ->
            val px = (width / 2.0 + (x(p) - (minX + maxX) / 2) * scale).toFloat()
            val py = (height / 2.0 - (p.latitude - (minY + maxY) / 2) * scale).toFloat()
            if (i == 0 || p.segmentStart) path.moveTo(px, py) else path.lineTo(px, py)
        }
        canvas.drawPath(path, paint)
    }
}
