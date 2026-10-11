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
    private val actions: (ManagedRoute) -> Unit, settings: () -> Unit = {},
    private val uploadStatus: (String) -> String = { "" },
    private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)),
    private val uploadState: (ManagedRoute) -> RouteUploadStatus = { RouteUploadStatus(UploadState.NOT_UPLOADED, uploadStatus(it.id)) },
    private val upload: (ManagedRoute) -> Unit = {},
    private val local: (ManagedRoute) -> Unit = {}) : LinearLayout(context) {
    private fun dp(value: Int) = BicycleDesign.dp(context, value)
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
        val header = BicycleComponents.header(context, themes)
        header.setPadding(dp(BicycleDesign.verticalPadding), 0, dp(BicycleDesign.verticalPadding), 0)
        header.addView(FrameLayout(context).apply {
            addView(BicycleComponents.heading(context, themes, "Streckenmanager"), FrameLayout.LayoutParams(-1, -1))
            addView(BicycleComponents.icon(context, themes, BicycleIcon.SettingsOptions, "Streckeneinstellungen öffnen", settings)
                .apply { tag = "route_settings" }, FrameLayout.LayoutParams(dp(BicycleDesign.touchSize), dp(BicycleDesign.touchSize), Gravity.END or Gravity.CENTER_VERTICAL))
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
        status.text = if (busy) "Dateien werden verarbeitet …" else if (filtered.isEmpty()) "Keine Strecken in dieser Auswahl" else ""
        status.visibility = if (status.text.isEmpty()) GONE else VISIBLE
        rows.notifyDataSetChanged()
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
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
        private fun metric(kind: BicycleIcon) = BicycleComponents.text(context, themes, "").apply {
            textSize = BicycleDesign.bodySp; gravity = Gravity.CENTER_VERTICAL
            BicycleComponents.bindTheme(this, themes) { tokens ->
                setCompoundDrawablesRelative(BicycleIcons.drawable(context, kind, tokens.foreground).apply {
                    setBounds(0, 0, dp(BicycleDesign.iconSize), dp(BicycleDesign.iconSize))
                }, null, null, null)
            }
            compoundDrawablePadding = dp(BicycleDesign.verticalPadding)
        }
        private val date = metric(BicycleIcon.Date)
        private val metrics = metric(BicycleIcon.Distance).apply { setSingleLine(true) }
        private fun action(kind: BicycleIcon, description: String, click: (ManagedRoute) -> Unit) =
            BicycleComponents.icon(context, themes, kind, description) { entry?.let(click) }
        private val more = action(BicycleIcon.MoreOptions, "Streckenaktionen", actions)
        private val eye = action(BicycleIcon.ShowOnMap, "Kartenanzeige", toggle)
        private val cloud = action(BicycleIcon.CloudNotUploaded, "Cloud-Upload") { route ->
            val state = uploadState(route)
            if (state.state == UploadState.FAILED && state.message.isNotEmpty())
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            upload(route)
        }
        private val saved = action(BicycleIcon.LocalSaved, "Lokale Speicherung", local)
        init {
            orientation = HORIZONTAL; gravity = Gravity.TOP
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
                    addView(more, LayoutParams(dp(BicycleDesign.touchSize), dp(BicycleDesign.touchSize)))
                }, LayoutParams(-1, -2))
                addView(date, LayoutParams(-1, -2).apply { topMargin = -dp(BicycleDesign.metadataSpacingTrim) })
                val metadata = LinearLayout(context).apply {
                    orientation = VERTICAL
                    addView(metrics, LayoutParams(-1, -2))
                }
                val buttons = LinearLayout(context).apply {
                    orientation = HORIZONTAL; gravity = Gravity.END
                    listOf(eye, cloud, saved).forEach {
                        it.setPadding(dp((BicycleDesign.compactActionWidth - BicycleDesign.iconSize) / 2),
                            dp((BicycleDesign.touchSize - BicycleDesign.iconSize) / 2),
                            dp((BicycleDesign.compactActionWidth - BicycleDesign.iconSize) / 2),
                            dp((BicycleDesign.touchSize - BicycleDesign.iconSize) / 2))
                        addView(it, LayoutParams(dp(BicycleDesign.compactActionWidth), dp(BicycleDesign.touchSize)))
                    }
                }
                addView(object : LinearLayout(context) {
                    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                        val free = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                        metrics.measure(free, free)
                        val labelWidth = if (metrics.visibility == GONE) 0 else metrics.measuredWidth
                        val buttonWidth = listOf(eye, cloud, saved).count { it.visibility != GONE } * dp(BicycleDesign.compactActionWidth)
                        val beside = labelWidth + buttonWidth <= MeasureSpec.getSize(widthMeasureSpec)
                        orientation = if (beside) HORIZONTAL else VERTICAL
                        (metadata.layoutParams as LayoutParams).apply { width = if (beside) 0 else -1; weight = if (beside) 1f else 0f }
                        (buttons.layoutParams as LayoutParams).width = if (beside) -2 else -1
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                    }
                }.apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(metadata, LayoutParams(0, -2, 1f))
                    addView(buttons, LayoutParams(-2, dp(BicycleDesign.touchSize)))
                }, LayoutParams(-1, -2).apply { topMargin = -dp(BicycleDesign.metadataSpacingTrim) })
            }, LayoutParams(0, -2, 1f))
            BicycleComponents.bindTheme(this, themes) { paintState(it) }
        }
        private fun paintState(tokens: ThemeTokens) {
            preview.foreground = tokens.foreground
            val route = entry ?: return
            origin.setImageDrawable(BicycleIcons.drawable(context, when (route.origin) {
                TrackOrigin.IMPORTED -> BicycleIcon.TrackImported; TrackOrigin.RECORDED -> BicycleIcon.TrackRecorded; TrackOrigin.UNKNOWN -> BicycleIcon.TrackUnknown
            }, tokens.foreground))
            eye.setImageDrawable(BicycleIcons.drawable(context, BicycleIcon.ShowOnMap, tokens.foreground, selected != route.id))
            val state = uploadState(route)
            cloud.setImageDrawable(BicycleIcons.drawable(context, when (state.state) {
                UploadState.UPLOADING -> BicycleIcon.Loading
                UploadState.FAILED -> BicycleIcon.CloudFailed
                else -> BicycleIcon.CloudUploaded
            }, tokens.foreground, state.state == UploadState.NOT_UPLOADED || state.state == UploadState.WAITING))
            saved.setImageDrawable(BicycleIcons.drawable(context, BicycleIcon.LocalSaved, tokens.foreground, !route.locallySaved))
        }
        fun bind(route: ManagedRoute) {
            entry = route; title.text = route.name; title.tag = "route_name_${route.id}"; title.contentDescription = route.name
            origin.contentDescription = when (route.origin) { TrackOrigin.IMPORTED -> "Importierte Strecke"; TrackOrigin.RECORDED -> "Aufgezeichnete Fahrt"; TrackOrigin.UNKNOWN -> "Herkunft unbekannt" }
            format.text = route.format; format.contentDescription = "${route.format}-Datei"
            date.text = if (route.details.dateMs > 0) DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm 'h'").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(route.details.dateMs)) else "Datum unbekannt"
            date.tag = "route_date_${route.id}"
            val distanceText = route.details.distanceM?.let { String.format(Locale.GERMANY, "%.1f km", it / 1000) }
            val durationText = route.details.movingMs?.let { val minutes = it / 60000; "%d:%02d h".format(Locale.GERMANY, minutes / 60, minutes % 60) }
            metrics.text = listOfNotNull(distanceText, durationText).joinToString(" · ")
            metrics.visibility = if (metrics.text.isEmpty()) GONE else VISIBLE
            metrics.tag = "route_metrics_${route.id}"
            preview.points = route.details.preview; preview.contentDescription = if (route.hasRoute) "Streckenvorschau" else "Keine GPS-Strecke gespeichert"
            eye.tag = "route_eye_${route.id}"; eye.isEnabled = !busy && route.hasRoute && route.locallySaved
            eye.contentDescription = if (selected == route.id) "${route.name} ausblenden" else if (!route.hasRoute) "Keine GPS-Strecke gespeichert" else "${route.name} anzeigen"
            val state = uploadState(route)
            cloud.visibility = if (route.origin == TrackOrigin.RECORDED && route.format == "FIT") VISIBLE else GONE
            cloud.tag = "route_cloud_${route.id}"; cloud.isEnabled = !busy && route.locallySaved && state.state !in setOf(UploadState.WAITING, UploadState.UPLOADING, UploadState.UPLOADED)
            cloud.contentDescription = state.message.ifEmpty { "${route.name} zu Nextcloud hochladen" }
            saved.tag = "route_local_${route.id}"; saved.isEnabled = !busy
            saved.contentDescription = if (route.locallySaved) "In der Streckensammlung gespeichert" else "Lokale Originaldatei fehlt · erneut importieren"
            more.tag = "route_more_${route.id}"; more.contentDescription = "Aktionen für ${route.name}"; more.isEnabled = !busy
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
