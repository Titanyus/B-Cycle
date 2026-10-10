package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

/** Four saved pages; six graphical presets with a draft until Save is pressed. */
internal class DashboardPresetPicker(context: Context, initial: List<DashboardTemplate>, initialPage: Int) : LinearLayout(context) {
    val selection = initial.toMutableList()
    private var page = initialPage
    private val choices = mutableMapOf<DashboardTemplate, View>()
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    init {
        orientation = VERTICAL
        addView(TextView(context).apply { text = "Dashboard-Seite wählen"; setTextColor(Color.WHITE); textSize = 16f })
        val pages = RadioGroup(context).apply { orientation = HORIZONTAL }
        for (i in 0..3) pages.addView(RadioButton(context).apply {
            id = View.generateViewId(); text = "${i + 1}"; tag = "preset_page_$i"
            contentDescription = "Dashboard ${i + 1} bearbeiten"; isChecked = i == page
            setOnClickListener { page = i; refresh() }
        }, RadioGroup.LayoutParams(0, dp(48), 1f))
        addView(pages)
        for (rowIndex in 0..1) {
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; tag = "preset_row_$rowIndex" }
            for (column in 0..2) {
                val template = DashboardTemplate.entries[rowIndex * 3 + column]
                val tile = LinearLayout(context).apply {
                    orientation = VERTICAL; gravity = Gravity.CENTER; setPadding(dp(6), dp(8), dp(6), dp(8))
                    tag = "preset_${template.name}"; isClickable = true; isFocusable = true; contentDescription = template.title
                    setOnClickListener { selection[page] = template; refresh() }
                    addView(ImageView(context).apply { setImageDrawable(DashboardPreview(template)); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(dp(60), dp(84)))
                    addView(TextView(context).apply { text = "${template.slots.size} Werte"; setTextColor(Color.WHITE); textSize = 13f; gravity = Gravity.CENTER; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(-1, dp(24)))
                }
                choices[template] = tile
                row.addView(tile, LayoutParams(0, dp(132), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
            }
            addView(row)
        }
        refresh()
    }
    private fun refresh() {
        choices.forEach { (template, tile) ->
            val active = selection[page] == template
            tile.isSelected = active; tile.stateDescription = if (active) "Ausgewählt" else "Nicht ausgewählt"
            tile.background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat(); setColor(if (active) Color.argb(35, 255, 255, 255) else Color.TRANSPARENT)
                setStroke(dp(1), if (active) Color.WHITE else Color.argb(50, 255, 255, 255))
            }
        }
    }
}
