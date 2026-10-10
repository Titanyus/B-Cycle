package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/** Seven native, accessible tiles: 2 / 1 large / 2 / 2. No drawing or UI dependency. */
class DashboardView(context: Context, private val selectField: (Int) -> Unit) : LinearLayout(context) {
    private val numbers = resources.getFont(R.font.bebas_neue)
    private val captions = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
    private val tiles = mutableListOf<Tile>()
    private var layout = DashboardLayout.defaults
    private var data = LiveData()
    private var gps = GpsValues()
    private var bikeDistanceM: Long? = null
    private var landscape = false

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.BLACK)
        buildTiles(false)
    }

    private fun buildTiles(wide: Boolean) {
        removeAllViews(); tiles.clear()
        val indexed = List(7) { Tile(it) }
        tiles.addAll(indexed)
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
            show(layout, data, gps, bikeDistanceM)
            return
        }
        val groups = listOf(listOf(0, 1), listOf(2), listOf(3, 4), listOf(5, 6))
        for ((rowIndex, indexes) in groups.withIndex()) {
            if (rowIndex > 0) addView(line(), LayoutParams(LayoutParams.MATCH_PARENT, 1))
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; isBaselineAligned = false }
            for ((column, index) in indexes.withIndex()) {
                if (column > 0) row.addView(line(), LayoutParams(1, LayoutParams.MATCH_PARENT))
                val tile = indexed[index]
                row.addView(tile.container, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, 0, if (rowIndex == 1) 2f else 1f))
        }
        show(layout, data, gps, bikeDistanceM)
    }

    private fun pair(indexes: List<Int>, indexed: List<Tile>) = LinearLayout(context).apply {
        orientation = HORIZONTAL; isBaselineAligned = false
        indexes.forEachIndexed { column, index ->
            if (column > 0) addView(line(), LayoutParams(1, LayoutParams.MATCH_PARENT))
            addView(indexed[index].container, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wide = MeasureSpec.getSize(widthMeasureSpec) > MeasureSpec.getSize(heightMeasureSpec)
        if (wide != landscape) { landscape = wide; buildTiles(wide) }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun show(layout: DashboardLayout, data: LiveData, gps: GpsValues = GpsValues(), bikeDistanceM: Long? = null) {
        this.layout = layout; this.data = data; this.gps = gps; this.bikeDistanceM = bikeDistanceM
        tiles.forEachIndexed { index, tile -> tile.show(layout.fields[index], data) }
    }

    private fun line() = View(context).apply { setBackgroundColor(Color.rgb(44, 44, 44)); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    private inner class Tile(index: Int) {
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isClickable = true; isFocusable = true
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(Color.rgb(24, 24, 24)))
                addState(intArrayOf(), ColorDrawable(Color.BLACK))
            }
            setOnClickListener { selectField(index) }
        }
        private val title = text(6, 18, captions, 2)
        private val value = text(10, 240, numbers, 1)
        private val unit = text(5, 16, captions, 2)

        init {
            container.addView(title, LayoutParams(LayoutParams.MATCH_PARENT, 0, .20f))
            container.addView(value, LayoutParams(LayoutParams.MATCH_PARENT, 0, .65f))
            container.addView(unit, LayoutParams(LayoutParams.MATCH_PARENT, 0, .15f))
        }

        fun show(field: DashboardField, data: LiveData) {
            val next = field.value(data, gps, bikeDistanceM)
            val caption = "${field.source} · ${field.title(data, gps)}"
            setText(title, caption.uppercase(Locale.GERMANY))
            setText(value, next.text)
            setText(unit, next.unit)
            val font = if (field.numeric) numbers else captions
            if (value.typeface !== font) value.typeface = font
            val color = if (next.available) Color.WHITE else Color.rgb(119, 119, 119)
            if (value.currentTextColor != color) value.setTextColor(color)
            unit.setTextColor(Color.rgb(163, 163, 163))
            container.contentDescription = "$caption: ${if (next.available) "${next.text} ${next.unit}" else "noch kein Wert"}. Antippen, um das Feld zu ändern."
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
