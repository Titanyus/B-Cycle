package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
internal class RouteManagerView(context: Context, import: () -> Unit, recover: () -> Unit,
                                private val toggle: (ManagedRoute) -> Unit,
                                private val actions: (ManagedRoute) -> Unit) : LinearLayout(context) {
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
        addView(TextView(context).apply { text = "Streckenmanager"; textSize = 23f; setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD); setPadding(dp(12), dp(12), dp(12), dp(8)); isAccessibilityHeading = true })
        filter.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listOf("Alle", "Gefahren", "Importiert"))
        addView(filter, LayoutParams(-1, dp(48)).apply { leftMargin = dp(8); rightMargin = dp(8) })
        filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { updateRows() }
        }
        addView(LinearLayout(context).apply {
            orientation = HORIZONTAL
            fun action(title: String, tagName: String, click: () -> Unit) = Button(context).apply {
                text = title; textSize = 12f; isAllCaps = false; tag = tagName; minHeight = dp(48); setOnClickListener { click() }
            }
            addView(action("Strecke importieren\nGPX / FIT", "route_import", import), LayoutParams(0, -2, 1f))
            addView(action("FIT-Fahrt\nübernehmen", "route_recover", recover), LayoutParams(0, -2, 1f))
        }, LayoutParams(-1, -2).apply { leftMargin = dp(8); rightMargin = dp(8) })
        addView(status)
        val list = ListView(context).apply {
            tag = "route_list"; adapter = rows; divider = null; dividerHeight = dp(8)
            setPadding(dp(8), 0, dp(8), dp(8)); clipToPadding = false
            setOnItemClickListener { _, _, position, _ -> if (!busy) actions(filtered[position]) }
        }
        addView(list, LayoutParams(-1, 0, 1f))
    }
    fun submit(entries: List<ManagedRoute>, active: String?, working: Boolean) {
        all = entries; selected = active; busy = working; updateRows()
    }
    private fun updateRows() {
        filtered = all.filter { when (filter.selectedItemPosition) { 1 -> it.recorded; 2 -> !it.recorded; else -> true } }
        status.text = if (busy) "Dateien werden verarbeitet …" else if (filtered.isEmpty()) "Keine Strecken in dieser Auswahl" else "${filtered.size} Strecken · lokal gespeichert"
        rows.notifyDataSetChanged()
        findViewWithTag<View>("route_import").isEnabled = !busy
        findViewWithTag<View>("route_recover").isEnabled = !busy
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
        private val title = text(16f, true)
        private val badge = text(13f)
        private val date = text(12f)
        private val metrics = text(14f)
        private val local = text(12f)
        private val eye = ImageButton(context).apply { setPadding(dp(12), dp(12), dp(12), dp(12)); background = null }
        private val more = Button(context).apply { text = "⋮"; textSize = 26f; setTextColor(Color.WHITE); background = null
            minWidth = 0; minimumWidth = 0; setPadding(0, 0, 0, 0) }
        init {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(10), dp(4), dp(10))
            background = GradientDrawable().apply { setColor(Color.rgb(116, 0, 0)); cornerRadius = dp(12).toFloat() }
            addView(preview, LayoutParams(dp(58), dp(76)))
            val labels = LinearLayout(context).apply { orientation = VERTICAL; setPadding(dp(8), 0, 0, 0)
                listOf(title, badge, date, metrics, local).forEach { addView(it, LayoutParams(-1, -2).apply { bottomMargin = dp(4) }) } }
            addView(labels, LayoutParams(0, -2, 1f))
            addView(LinearLayout(context).apply { orientation = VERTICAL
                addView(more, LayoutParams(dp(48), dp(48))); addView(eye, LayoutParams(dp(48), dp(48))) })
        }
        private fun text(size: Float, bold: Boolean = false) = TextView(context).apply {
            textSize = size; setTextColor(Color.WHITE); maxLines = 2
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        fun bind(entry: ManagedRoute) {
            title.text = entry.name; title.tag = "route_name_${entry.id}"
            badge.text = "${if (entry.recorded) "Gefahren" else "Importiert"} · ${entry.format}"
            date.text = if (entry.details.dateMs > 0) DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(entry.details.dateMs)) else "Datum unbekannt"
            metrics.text = listOfNotNull(entry.details.distanceM?.let { String.format(Locale.GERMANY, "%.1f km", it / 1000) },
                entry.details.durationMs?.let { val minutes = it / 60000; "%d:%02d h".format(minutes / 60, minutes % 60) }).joinToString("  |  ")
            metrics.visibility = if (metrics.text.isEmpty()) GONE else VISIBLE
            local.text = if (entry.recorded) "Lokal gespeichert" else ""
            local.visibility = if (entry.recorded) VISIBLE else GONE
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
