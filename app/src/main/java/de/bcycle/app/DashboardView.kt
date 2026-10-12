package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/** Native, accessible metric tiles arranged by the selected template. */
class DashboardView(context: Context, private val selectField: (Int) -> Unit) : LinearLayout(context) {
    internal val modeGlow = BoschModeGlow(BicycleDesign.dp(context, BicycleDesign.modeGlowWidth).toFloat())
    fun showModeGlow(enabled: Boolean, mode: Long?, names: List<String>? = null, batteryPercent: Long? = null) {
        modeGlow.update(if (enabled) BoschModeColors.color(mode, names) else null, batteryPercent)
    }
    private val trends = AverageTrends()
    private val numbers = resources.getFont(R.font.bebas_neue)
    private val captions = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
    private val tiles = mutableListOf<Tile>()
    private var layout = DashboardLayout.defaults
    private var data = LiveData()
    private var gps = GpsValues()
    private var bikeDistanceM: Long? = null
    private var pulse = PulseValues()
    private var landscape = false
    private var viewportWide: Boolean? = null
    private var viewportWidth = 0
    private var captionGeometry = ""
    private var viewportDataHeight = 0
    var template = DashboardTemplate.DATA
        private set
    var changePage: ((Int) -> Unit)? = null

    /** Use the whole dashboard/map viewport so short presets do not look like landscape. */
    fun heightForViewport(width: Int, available: Int): Int {
        val wide = width > DashboardTemplate.DATA.heightForViewport(available, dp(4))
        viewportWide = wide
        viewportWidth = width
        viewportDataHeight = template.heightForViewport(available, dp(4), wide)
        if (wide != landscape) { landscape = wide; buildTiles(wide) }
        return viewportDataHeight
    }

    fun setTemplate(next: DashboardTemplate) {
        if (next == template) return
        template = next; buildTiles(landscape); requestLayout()
    }

    init {
        orientation = VERTICAL
        background = modeGlow
        buildTiles(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        modeGlow.setBounds(0, 0, w, h)
    }

    private fun buildTiles(wide: Boolean) {
        removeAllViews(); tiles.clear(); captionGeometry = ""
        val indexed = List(DashboardLayout.FIELD_COUNT) { Tile(it) }
        tiles.addAll(indexed)
        if (template != DashboardTemplate.DATA) {
            orientation = VERTICAL
            template.rows.forEachIndexed { index, slots ->
                if (index > 0) addView(line(), LayoutParams(LayoutParams.MATCH_PARENT, 1))
                addView(pair(slots, indexed), LayoutParams(LayoutParams.MATCH_PARENT, 0, template.rowWeight(index, wide)))
            }
            show(layout, data, gps, bikeDistanceM, pulse)
            return
        }
        orientation = if (wide) HORIZONTAL else VERTICAL
        if (wide) {
            addView(indexed[2].container, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(line(), LayoutParams(1, LayoutParams.MATCH_PARENT))
            val right = LinearLayout(context).apply { orientation = VERTICAL }
            for ((rowIndex, indexes) in listOf(listOf(0, 1), listOf(3, 4), listOf(5, 6)).withIndex()) {
                if (rowIndex > 0) right.addView(line(), LayoutParams(LayoutParams.MATCH_PARENT, 1))
                right.addView(pair(indexes, indexed), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
            }
            addView(right, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            show(layout, data, gps, bikeDistanceM, pulse)
            return
        }
        val groups = template.rows
        for ((rowIndex, indexes) in groups.withIndex()) {
            if (rowIndex > 0) addView(line(), LayoutParams(LayoutParams.MATCH_PARENT, 1))
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; isBaselineAligned = false }
            for ((column, index) in indexes.withIndex()) {
                if (column > 0) row.addView(line(), LayoutParams(1, LayoutParams.MATCH_PARENT))
                val tile = indexed[index]
                row.addView(tile.container, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, 0, template.rowWeight(rowIndex)))
        }
        show(layout, data, gps, bikeDistanceM, pulse)
    }

    private fun pair(indexes: List<Int>, indexed: List<Tile>) = LinearLayout(context).apply {
        orientation = HORIZONTAL; isBaselineAligned = false
        indexes.forEachIndexed { column, index ->
            if (column > 0) addView(line(), LayoutParams(1, LayoutParams.MATCH_PARENT))
            addView(indexed[index].container, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        val wide = if (width == viewportWidth && measuredHeight == viewportDataHeight) viewportWide ?: false
            else width > measuredHeight
        if (wide != landscape) { landscape = wide; buildTiles(wide) }
        val available = MeasureSpec.getSize(heightMeasureSpec)
        // Keep the former title/value space; reclaim the unit row and removed header.
        // Short viewports use their full reference area so text remains readable.
        val reference = available - if (available >= dp(450)) dp(88) else 0
        val compact = (reference * .85f + dp(16) * (if (wide) 3 else 4) * .15f).toInt().coerceAtMost(available)
        val height = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST)
            MeasureSpec.makeMeasureSpec(compact, MeasureSpec.EXACTLY) else heightMeasureSpec
        val geometry = "$width:$height:" + template.slots.joinToString("|") { tiles[it].title.text.toString() }
        if (geometry != captionGeometry) {
            captionGeometry = geometry
            for (index in template.slots) {
                tiles[index].title.layoutParams = (tiles[index].title.layoutParams as LayoutParams).apply { this.height = 0; weight = tiles[index].captionWeight }
            }
        }
        super.onMeasure(widthMeasureSpec, height)
        // A fixed caption font must not be shrunk to fit a short row. Reserve its
        // actual text height when needed, leaving the value the remaining space.
        var captionsChanged = false
        for (index in template.slots) {
            val title = tiles[index].title
            val needed = title.layout?.height ?: continue
            if (needed > title.measuredHeight) {
                title.layoutParams = (title.layoutParams as LayoutParams).apply { this.height = needed; weight = 0f }
                captionsChanged = true
            }
        }
        if (captionsChanged) super.onMeasure(widthMeasureSpec, height)

    }

    fun show(layout: DashboardLayout, data: LiveData, gps: GpsValues = GpsValues(), bikeDistanceM: Long? = null, pulse: PulseValues = PulseValues()) {
        this.layout = layout; this.data = data; this.gps = gps; this.bikeDistanceM = bikeDistanceM
        this.pulse = pulse
        tiles.forEachIndexed { index, tile -> tile.show(layout.fields[index], data) }
    }

    private fun line() = View(context).apply { setBackgroundColor(Color.rgb(44, 44, 44)); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    private inner class Tile(index: Int) {
        private val threeColumns = template.rows.any { it.size == 3 && index in it }
        private val swipe = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val first = e1 ?: return false
                val change = changePage ?: return false
                val dx = e2.x - first.x; val dy = e2.y - first.y
                if (kotlin.math.abs(dx) > dp(70) && kotlin.math.abs(dx) > 2 * kotlin.math.abs(dy)) {
                    change(if (dx < 0) 1 else -1); return true
                }
                return false
            }
        })
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            val horizontal = if (threeColumns) BicycleDesign.tripleHorizontalPadding else 10
            setPadding(dp(horizontal), dp(8), dp(horizontal), dp(4))
            isClickable = true; isFocusable = true
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(Color.rgb(24, 24, 24)))
                addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
            }
            setOnClickListener { selectField(index) }
            setOnTouchListener { _, event -> swipe.onTouchEvent(event) }
        }
        val captionWeight = if (index == 2 && (template == DashboardTemplate.DATA || template.largeTop)) .10f else .20f
        val title = TextView(context).apply {
            typeface = captions; gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 2
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            textSize = BicycleDesign.dashboardCaptionSp; setTextColor(BicycleDesign.dashboardUnitColor)
        }
        private val value = text(10, 240, numbers, 1).apply {
            if (threeColumns) {
                setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE)
                textSize = BicycleDesign.tripleValueSp; ellipsize = android.text.TextUtils.TruncateAt.END
            }
        }
        private val unit = text(5, 16, captions, 1).apply {
            if (threeColumns) { setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE); textSize = BicycleDesign.tripleUnitSp }
        }

        private val row = ValueRow(value, unit, threeColumns)

        init {
            container.addView(title, LayoutParams(LayoutParams.MATCH_PARENT, 0,
                captionWeight))
            container.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, 0, .65f))
        }

        fun show(field: DashboardField, data: LiveData) {
            val next = field.value(data, gps, bikeDistanceM, pulse)
            val caption = if (field.showSource) "${field.source} · ${field.title(data, gps)}" else field.title(data, gps)
            val short = field.dashboardTitle(data).uppercase(Locale.GERMANY)
            val captionKey = "${field.captionIcon ?: ""}:$short"
            if (title.tag != captionKey) {
                title.tag = captionKey
                title.text = if (field.captionIcon != null) {
                    android.text.SpannableString("\uFFFC $short").apply {
                        setSpan(SourceIconSpan(field.captionIcon!!, context), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                } else short
            }
            row.direction = trends.indicator(field, data, gps, pulse, android.os.SystemClock.elapsedRealtime())
            val compactDistance = if (threeColumns && next.unit == "km") next.text.replace(',', '.').toDoubleOrNull()?.takeIf { kotlin.math.abs(it) >= 100 }
                ?.let { String.format(Locale.GERMANY, "%.1f", it) } else null
            setText(value, compactDistance ?: next.text)
            setText(unit, if (threeColumns && next.unit == "h:mm:ss") context.getString(R.string.analysis_hours) else next.unit)
            unit.visibility = if (next.unit.isEmpty()) GONE else VISIBLE
            val font = if (field.numeric) numbers else captions
            if (value.typeface !== font) value.typeface = font
            val color = if (next.available) Color.WHITE else Color.rgb(119, 119, 119)
            if (value.currentTextColor != color) value.setTextColor(color)
            unit.setTextColor(BicycleDesign.dashboardUnitColor)
            container.contentDescription = "$caption: ${if (next.available) "${next.text} ${next.unit}" else "noch kein Wert"}. Antippen, um das Feld zu ändern."
        }
    }

    /** Keep the number centred in its tile, with the unit following its actual text edge. */
    private inner class ValueRow(private val value: TextView, private val unit: TextView, private val threeColumns: Boolean) : ViewGroup(context) {
        var direction = 0
            set(next) { if (field != next) {
                val spacingChanged = (field == 0) != (next == 0)
                field = next; if (threeColumns && spacingChanged) requestLayout(); invalidate()
            } }
        private val chevronPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE; strokeCap = android.graphics.Paint.Cap.ROUND
        }
        private val chevronSpace get() = kotlin.math.ceil(minOf(dp(14).toFloat(), value.textSize * .28f) + dp(4)).toInt()
        init { addView(value); addView(unit) }
        override fun dispatchDraw(canvas: android.graphics.Canvas) {
            super.dispatchDraw(canvas)
            if (direction != 0) {
                val size = minOf(dp(14).toFloat(), value.textSize * .28f)
                val textWidth = value.layout?.getLineWidth(0) ?: 0f
                val x = if (threeColumns) value.left - dp(4) - size else width / 2f - textWidth / 2f - dp(6) - size
                drawChevron(canvas, chevronPaint, x.coerceAtLeast(0f),
                    height / 2f, size, direction, value.currentTextColor)
            }
        }
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            if (threeColumns) {
                if (unit.visibility != GONE) unit.measure(MeasureSpec.makeMeasureSpec(width / 2, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
                val reserve = chevronSpace + if (unit.visibility == GONE) 0 else unit.measuredWidth + dp(4)
                value.measure(MeasureSpec.makeMeasureSpec((width - reserve).coerceAtLeast(1), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
                setMeasuredDimension(width, height)
                return
            }
            if (unit.visibility != GONE) unit.measure(
                MeasureSpec.makeMeasureSpec(width / 4, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
            val reserve = if (unit.visibility == GONE) 0 else unit.measuredWidth + dp(6)
            value.measure(MeasureSpec.makeMeasureSpec((width - 2 * reserve).coerceAtLeast(1), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
            setMeasuredDimension(width, height)
        }
        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            if (threeColumns) {
                val reserve = if (direction == 0) 0 else chevronSpace
                val unitWidth = if (unit.visibility == GONE) 0 else unit.measuredWidth + dp(4)
                val start = (width - reserve - value.measuredWidth - unitWidth) / 2
                value.layout(start + reserve, 0, start + reserve + value.measuredWidth, value.measuredHeight)
                if (unit.visibility != GONE) {
                    val y = (height - unit.measuredHeight).coerceAtLeast(0) / 2
                    unit.layout(value.right + dp(4), y, value.right + dp(4) + unit.measuredWidth, y + unit.measuredHeight)
                }
                return
            }
            val start = (width - value.measuredWidth) / 2
            value.layout(start, 0, start + value.measuredWidth, value.measuredHeight)
            if (unit.visibility != GONE) {
                // TextView finalises automatic font sizing during layout, before vertical centring.
                unit.layout(0, 0, unit.measuredWidth, unit.measuredHeight)
                val textWidth = value.layout?.getLineWidth(0) ?: 0f
                val x = (width / 2f + textWidth / 2f + dp(6)).toInt()
                val y = (height - unit.measuredHeight).coerceAtLeast(0) / 2
                unit.offsetLeftAndRight(x); unit.offsetTopAndBottom(y)
            }
        }
    }

    private fun text(min: Int, max: Int, font: Typeface, lines: Int) = TextView(context).apply {
        typeface = font; gravity = Gravity.CENTER
        setTextColor(Color.WHITE); includeFontPadding = false
        maxLines = lines
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setAutoSizeTextTypeUniformWithConfiguration(min, max, 1, TypedValue.COMPLEX_UNIT_SP)
    }
    private fun setText(view: TextView, next: String) { if (view.text.toString() != next) view.text = next }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
