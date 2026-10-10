package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

/** Fixed source dropdown with a separate scrolling list. Reuses all existing field groups. */
internal class FieldPickerView(context: Context, data: LiveData, gps: GpsValues,
    bikeDistanceM: Long?, pulse: PulseValues, selected: DashboardField?, allowEmpty: Boolean,
    choose: (DashboardField?) -> Unit) : LinearLayout(context) {
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    init {
        orientation = VERTICAL; setPadding(dp(16), dp(8), dp(16), 0)
        val groups = DashboardField.groups(data, selected).groupBy { it.source }
        val sources = groups.keys.toList()
        val selector = Spinner(context).apply {
            tag = "field_source"; contentDescription = "Sensorquelle auswählen"
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, sources)
        }
        addView(selector, LayoutParams(-1, dp(48)))
        if (allowEmpty) addView(RadioButton(context).apply {
            text = "Leer"; tag = "map_field_empty"; isChecked = selected == null
            setOnClickListener { choose(null) }
        }, LayoutParams(-1, dp(48)))
        val rows = LinearLayout(context).apply { orientation = VERTICAL }
        val list = ScrollView(context).apply { tag = "field_list"; addView(rows) }
        // AT_MOST measurement keeps this bounded by the dialog even on short screens.
        addView(list, LayoutParams(-1, 0, 1f))
        var shown = -1
        fun populate(position: Int) {
            if (shown == position) return
            shown = position; rows.removeAllViews(); list.scrollTo(0, 0)
            for (section in groups.getValue(sources[position])) {
                rows.addView(TextView(context).apply {
                    text = section.title; textSize = 14f; setTextColor(Color.LTGRAY)
                    setPadding(0, dp(10), 0, dp(4)); isAccessibilityHeading = true
                })
                for (field in section.fields) {
                    val value = field.value(data, gps, bikeDistanceM, pulse)
                    val title = if (field.id == "RIDER_POWER") "Fahrerleistung (eigene)" else field.title(data, gps)
                    rows.addView(RadioButton(context).apply {
                        tag = field.id; text = "$title${if (value.unit.isNotEmpty()) " · ${value.unit}" else ""}"
                        textSize = 16f; setTextColor(Color.WHITE); minHeight = dp(48)
                        isChecked = selected == field
                        contentDescription = if (field.showSource) "${field.source} · $text" else text
                        setOnClickListener { choose(field) }
                    })
                }
            }
        }
        selector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = populate(position)
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        val initial = sources.indexOf(selected?.source).coerceAtLeast(0)
        selector.setSelection(initial); populate(initial)
    }
}
