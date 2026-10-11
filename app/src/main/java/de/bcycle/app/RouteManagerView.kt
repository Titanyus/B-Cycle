package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
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

/** Fixed filter and native recycled rows. No per-row map, decoder or sensor subscriptions. */
internal class RouteManagerView(context: Context,
                                private val toggle: (ManagedRoute) -> Unit,
                                private val actions: (ManagedRoute) -> Unit, settings: () -> Unit = {},
                                private val uploadStatus: (String) -> String = { "" }) : LinearLayout(context) {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private var all: List<ManagedRoute> = emptyList()
    private var filtered: List<ManagedRoute> = emptyList()
    private var selected: String? = null
    private var busy = false
    private val filter = Spinner(context).apply { tag = "route_filter"; contentDescription = "Strecken filtern" }
    private val status = TextView(context).apply { textSize = 12f; setTextColor(Color.LTGRAY); setPadding(dp(12), dp(4), dp(12), dp(4)) }
    private val rows = object : BaseAdapter() {
        override fun getCount() = filtered.size
        override fun getItem(position: Int) = filtered[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, recycled: View?, parent: ViewGroup): View {
            val card = (recycled as? Card) ?: Card()
            card.bind(filtered[position]); return card
        }
    }
    init {
        orientation = VERTICAL; setBackgroundColor(context.getColor(R.color.settings_red))
        addView(ImageView(context).apply {
            tag = "route_brand"
            setImageResource(R.drawable.ic_settings_brand); scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "B|Cycle"; setPadding(dp(24), dp(12), dp(24), dp(12))
            setBackgroundResource(R.drawable.settings_header_background)
        }, LayoutParams(-1, dp(88)))
        addView(LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply { text = "Streckenmanager"; textSize = 23f; setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD); setPadding(dp(12), dp(12), 0, dp(8)); isAccessibilityHeading = true }, LayoutParams(0, -2, 1f))
            addView(Button(context).apply { text = "⋮"; textSize = 28f; setTextColor(Color.WHITE); background = null
                tag = "route_settings"; contentDescription = "Streckeneinstellungen öffnen"; minWidth = 0; minimumWidth = 0
                setPadding(0, 0, 0, 0); setOnClickListener { settings() } }, LayoutParams(dp(48), dp(48)))
        }, LayoutParams(-1, -2))
        val filterColor = Color.rgb(116, 0, 0)
        fun filterBackground() = GradientDrawable().apply { setColor(filterColor); cornerRadius = dp(6).toFloat() }
        filter.background = LayerDrawable(arrayOf(filterBackground(), RouteSymbolDrawable(RouteSymbol.DOWN))).apply {
            setLayerSize(1, dp(20), dp(20)); setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL); setLayerInsetEnd(1, dp(12))
        }
        filter.setPopupBackgroundDrawable(filterBackground())
        filter.adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listOf("Alle", "Gefahren", "Importiert")) {
            private fun row(position: Int, recycled: View?, dropdown: Boolean): View = ((recycled as? TextView) ?: TextView(context)).apply {
                text = getItem(position); textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(8), dp(if (dropdown) 16 else 40), dp(8)); minHeight = dp(48)
                setBackgroundColor(if (dropdown) filterColor else Color.TRANSPARENT)
            }
            override fun getView(position: Int, convertView: View?, parent: ViewGroup) = row(position, convertView, false)
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) = row(position, convertView, true)
        }
        addView(filter, LayoutParams(-1, dp(48)).apply { leftMargin = dp(8); rightMargin = dp(8); bottomMargin = dp(4) })
        filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { updateRows() }
        }
        addView(status)
        val list = ListView(context).apply {
            tag = "route_list"; adapter = rows
            divider = android.graphics.drawable.ColorDrawable(Color.WHITE); dividerHeight = 1
            setPadding(dp(8), 0, 0, dp(8)); clipToPadding = false
            setOnItemClickListener { _, _, position, _ -> if (!busy) actions(filtered[position]) }
        }
        addView(list, LayoutParams(-1, 0, 1f))
    }
    fun submit(entries: List<ManagedRoute>, active: String?, working: Boolean) {
        all = entries; selected = active; busy = working; updateRows()
    }
    private fun updateRows() {
        filtered = all.filter { when (filter.selectedItemPosition) { 1 -> it.recorded; 2 -> !it.recorded; else -> true } }
        status.text = if (busy) "Dateien werden verarbeitet …" else if (filtered.isEmpty()) "Keine Strecken in dieser Auswahl" else "${filtered.size} ${if (filtered.size == 1) "Strecke" else "Strecken"}"
        rows.notifyDataSetChanged()
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val max = MeasureSpec.getSize(heightMeasureSpec)
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val desired = if (mode == MeasureSpec.UNSPECIFIED) dp(580) else minOf(max, dp(680))
        findViewWithTag<View>("route_brand").visibility = if (desired < dp(450)) GONE else VISIBLE
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY))
    }
    private inner class Card : LinearLayout(context) {
        private val preview = RoutePreviewView(context)
        private val title = text(16f, true).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        private val badges = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        private val date = text(14f).apply { maxLines = 2; symbol(RouteSymbol.DATE) }
        private val distance = text(14f).apply { symbol(RouteSymbol.DISTANCE) }
        private val duration = text(14f).apply { symbol(RouteSymbol.TIME) }
        private val metrics = object : LinearLayout(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val free = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                distance.measure(free, free); duration.measure(free, free)
                val needed = (if (distance.visibility == GONE) 0 else distance.measuredWidth) +
                    (if (duration.visibility == GONE) 0 else duration.measuredWidth) +
                    (if (distance.visibility != GONE && duration.visibility != GONE) dp(12) else 0)
                val direction = if (needed > MeasureSpec.getSize(widthMeasureSpec)) VERTICAL else HORIZONTAL
                if (orientation != direction) orientation = direction
                (duration.layoutParams as LayoutParams).marginStart = if (direction == HORIZONTAL && distance.visibility != GONE) dp(12) else 0
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }.apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(distance, LayoutParams(-2, -2)); addView(duration, LayoutParams(-2, -2).apply { marginStart = dp(12) }) }
        private fun action() = ImageButton(context).apply {
            setPadding(dp(3), dp(3), dp(3), dp(3)); scaleType = ImageView.ScaleType.FIT_CENTER; background = null
        }
        private val eye = action().apply { setPadding(dp(10), dp(10), dp(10), dp(10)) }
        private val more = action().apply { setPadding(dp(10), dp(10), dp(10), dp(10)); setImageDrawable(RouteSymbolDrawable(RouteSymbol.MORE)) }
        init {
            orientation = VERTICAL; setPadding(dp(10), dp(8), 0, dp(8))
            addView(LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(title, LayoutParams(0, dp(28), 1f).apply { rightMargin = dp(6) }); addView(badges, LayoutParams(-2, -2).apply { marginEnd = dp(12) }) }, LayoutParams(-1, -2))
            addView(LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(preview, LayoutParams(dp(58), dp(76)))
                addView(LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(8), 0, 0, 0)
                    addView(date, LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
                    addView(metrics, LayoutParams(-1, -2))
                    addView(LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.END
                        addView(eye, LayoutParams(dp(48), dp(48))); addView(more, LayoutParams(dp(48), dp(48)))
                    }, LayoutParams(-1, dp(48))) }, LayoutParams(0, -2, 1f))
            }, LayoutParams(-1, -2))
        }
        private fun TextView.symbol(kind: RouteSymbol) {
            setCompoundDrawablesRelative(RouteSymbolDrawable(kind).apply { setBounds(0, 0, dp(18), dp(18)) }, null, null, null)
            compoundDrawablePadding = dp(5); gravity = Gravity.CENTER_VERTICAL
        }
        private fun text(size: Float, bold: Boolean = false) = TextView(context).apply {
            textSize = size; setTextColor(Color.WHITE); maxLines = 1
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        private fun badge(kind: RouteSymbol, description: String) {
            badges.addView(ImageView(context).apply {
                setImageDrawable(RouteSymbolDrawable(kind)); scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = description; tooltipText = description
                setPadding(dp(3), dp(3), dp(3), dp(3))
                background = GradientDrawable().apply { setColor(context.getColor(R.color.settings_red)); cornerRadius = dp(4).toFloat() }
            }, LayoutParams(dp(26), dp(26)).apply { marginStart = dp(3) })
        }
        fun bind(entry: ManagedRoute) {
            title.text = entry.name; title.tag = "route_name_${entry.id}"; title.contentDescription = entry.name
            badges.removeAllViews()
            badge(if (entry.recorded) RouteSymbol.RIDE else RouteSymbol.IMPORT, if (entry.recorded) "Aufgezeichnete Fahrt" else "Importierte Strecke")
            badge(if (entry.format == "FIT") RouteSymbol.FIT else RouteSymbol.GPX, "${entry.format}-Datei")
            badge(RouteSymbol.LOCAL, "In der Streckensammlung gespeichert")
            val cloud = uploadStatus(entry.id)
            if (cloud.isNotEmpty()) badge(when {
                cloud == "Nextcloud gesichert" -> RouteSymbol.CLOUD
                cloud.startsWith("Upload fehlgeschlagen") -> RouteSymbol.ERROR
                else -> RouteSymbol.PENDING
            }, cloud)
            date.text = if (entry.details.dateMs > 0) DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(entry.details.dateMs)) else "Datum unbekannt"
            date.tag = "route_date_${entry.id}"
            distance.text = entry.details.distanceM?.let { String.format(Locale.GERMANY, "%.1f km", it / 1000) }.orEmpty()
            distance.visibility = if (entry.details.distanceM == null) GONE else VISIBLE; distance.tag = "route_distance_${entry.id}"
            duration.text = entry.details.durationMs?.let { val minutes = it / 60000; "%d:%02d h".format(Locale.GERMANY, minutes / 60, minutes % 60) }.orEmpty()
            duration.visibility = if (entry.details.durationMs == null) GONE else VISIBLE; duration.tag = "route_duration_${entry.id}"
            metrics.visibility = if (entry.details.distanceM == null && entry.details.durationMs == null) GONE else VISIBLE
            preview.points = entry.details.preview; preview.contentDescription = if (entry.hasRoute) "Streckenvorschau" else "Keine GPS-Strecke gespeichert"
            eye.tag = "route_eye_${entry.id}"; eye.setImageDrawable(RouteActionIcon(false, entry.id == selected))
            eye.contentDescription = if (entry.id == selected) "${entry.name} ausblenden" else "${entry.name} anzeigen"
            eye.visibility = if (entry.hasRoute) VISIBLE else GONE; eye.isEnabled = !busy
            eye.setOnClickListener { toggle(entry) }
            more.tag = "route_more_${entry.id}"; more.contentDescription = "Aktionen für ${entry.name}"; more.isEnabled = !busy
            more.setOnClickListener { actions(entry) }
        }
    }

}

internal class RoutePreviewView(context: Context) : View(context) {
    var points: List<MapPoint> = emptyList(); set(value) { field = value; invalidate() }
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE
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
