package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.util.TypedValue

internal object MapFieldSelection {
    val positions = listOf("Links", "Mitte", "Rechts")
    fun key(index: Int) = "mapDataField_$index"
    fun read(preferences: SharedPreferences, index: Int) =
        preferences.getString(key(index), null)?.let(DashboardField::fromId)
}

/** Three global value-only overlays. Non-interactive views let map gestures pass through. */
internal class MapDataFieldsView(context: Context) : LinearLayout(context) {
    private var fields = List<DashboardField?>(3) { null }
    private val values = List(3) { index -> TextView(context).apply {
        tag = "map_value_$index"
        gravity = Gravity.CENTER
        typeface = resources.getFont(R.font.bebas_neue)
        setTextColor(Color.BLACK); includeFontPadding = false; maxLines = 1
        setAutoSizeTextTypeUniformWithConfiguration(12, 32, 1, TypedValue.COMPLEX_UNIT_SP)
        val inset = (4 * resources.displayMetrics.density).toInt()
        setPadding(inset, 0, inset, 0)
    } }
    init {
        tag = "map_data_fields"; orientation = HORIZONTAL; isBaselineAligned = false
        values.forEach { addView(it, LayoutParams(0, -1, 1f)) }
        visibility = GONE
    }
    fun reload(preferences: SharedPreferences) {
        fields = List(3) { MapFieldSelection.read(preferences, it) }
    }
    fun show(data: LiveData, gps: GpsValues, bikeDistanceM: Long?, pulse: PulseValues) {
        fields.forEachIndexed { index, field ->
            val value = field?.value(data, gps, bikeDistanceM, pulse)
            val next = value?.takeIf { it.available }?.text.orEmpty()
            val view = values[index]
            if (view.text.toString() != next) view.text = next
            view.visibility = if (next.isEmpty()) INVISIBLE else VISIBLE
            view.importantForAccessibility = if (next.isEmpty()) View.IMPORTANT_FOR_ACCESSIBILITY_NO else View.IMPORTANT_FOR_ACCESSIBILITY_YES
            view.contentDescription = if (next.isEmpty()) null else "${field!!.title(data, gps)}: $next ${value!!.unit}"
        }
        visibility = if (values.any { it.visibility == VISIBLE }) VISIBLE else GONE
    }
}
