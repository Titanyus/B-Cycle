package de.bcycle.app

import android.content.Context
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
    private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)),
    graphPicker: GraphOverlayPicker? = null,
    choose: (DashboardField?) -> Unit) : LinearLayout(context) {
    private fun dp(n: Int) = BicycleDesign.dp(context, n)
    init {
        orientation = VERTICAL; setPadding(dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.horizontalPadding), 0)
        BicycleComponents.background(this, themes, ThemeRole.SECONDARY)
        val groups = DashboardField.groups(data, selected).groupBy { it.source }
        val sources = groups.keys.toList()
        val selector = Spinner(context).apply {
            tag = "field_source"; contentDescription = "Sensorquelle auswählen"
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, sources)
        }
        BicycleDialogStyle.spinner(selector, themes)
        addView(selector, LayoutParams(-1, dp(BicycleDesign.touchSize)))
        if (allowEmpty) addView(RadioButton(context).apply {
            text = "Leer"; tag = "map_field_empty"; isChecked = selected == null
            BicycleDialogStyle.control(this, themes)
            setOnClickListener { choose(null) }
        }, LayoutParams(-1, dp(BicycleDesign.touchSize)))
        val rows = LinearLayout(context).apply { orientation = VERTICAL }
        val list = ScrollView(context).apply { tag = "field_list"; addView(rows) }
        BicycleDialogStyle.content(list, themes)
        // AT_MOST measurement keeps this bounded by the dialog even on short screens.
        addView(list, LayoutParams(-1, 0, 1f))
        var shown = -1
        fun populate(position: Int) {
            if (shown == position) return
            shown = position; rows.removeAllViews(); list.scrollTo(0, 0)
            for (section in groups.getValue(sources[position])) {
                rows.addView(TextView(context).apply {
                    text = section.title; textSize = BicycleDesign.bodySp; BicycleDialogStyle.control(this, themes)
                    setPadding(0, dp(BicycleDesign.rowGap), 0, dp(BicycleDesign.smallGap)); isAccessibilityHeading = true
                })
                for (field in section.fields) {
                    val value = field.value(data, gps, bikeDistanceM, pulse)
                    val title = if (field.id == "RIDER_POWER") "Fahrerleistung (eigene)" else field.title(data, gps)
                    rows.addView(RadioButton(context).apply {
                        tag = field.id; text = "$title${if (value.unit.isNotEmpty()) " · ${value.unit}" else ""}"
                        BicycleDialogStyle.control(this, themes); minHeight = dp(BicycleDesign.touchSize)
                        isChecked = selected == field
                        contentDescription = if (field.showSource) "${field.source} · $text" else text
                        setOnClickListener { choose(field) }
                    })
                }
            }
            if (graphPicker != null) rows.addView(graphPicker)
        }
        selector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = populate(position)
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        val initial = sources.indexOf(selected?.source).coerceAtLeast(0)
        selector.setSelection(initial); populate(initial)
    }
}
