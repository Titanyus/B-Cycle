package de.bcycle.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

/** Four saved pages; six graphical presets with a draft until Save is pressed. */
internal class DashboardPresetPicker(context: Context, initial: List<DashboardTemplate>, initialPage: Int, private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0))) : LinearLayout(context) {
    val selection = initial.toMutableList()
    private var page = initialPage
    private val choices = mutableMapOf<DashboardTemplate, View>()
    private fun dp(n: Int) = BicycleDesign.dp(context, n)
    init {
        orientation = VERTICAL
        addView(TextView(context).apply { text = "Dashboard-Seite wählen"; BicycleDialogStyle.control(this, themes); textSize = BicycleDesign.inputSp })
        val pages = RadioGroup(context).apply { orientation = HORIZONTAL }
        for (i in 0..3) pages.addView(RadioButton(context).apply {
            id = View.generateViewId(); text = "${i + 1}"; tag = "preset_page_$i"
            BicycleDialogStyle.control(this, themes)
            contentDescription = "Dashboard ${i + 1} bearbeiten"; isChecked = i == page
            setOnClickListener { page = i; refresh() }
        }, RadioGroup.LayoutParams(0, dp(BicycleDesign.touchSize), 1f))
        addView(pages)
        for (rowIndex in 0..1) {
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; tag = "preset_row_$rowIndex" }
            for (column in 0..2) {
                val template = DashboardTemplate.entries[rowIndex * 3 + column]
                val tile = LinearLayout(context).apply {
                    orientation = VERTICAL; gravity = Gravity.CENTER; setPadding(dp(BicycleDesign.inputRadius), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.inputRadius), dp(BicycleDesign.verticalPadding))
                    tag = "preset_${template.name}"; isClickable = true; isFocusable = true; contentDescription = template.title
                    setOnClickListener { selection[page] = template; refresh() }
                    addView(ImageView(context).apply { setImageDrawable(DashboardPreview(template)); BicycleComponents.bindTheme(this, themes) { setColorFilter(it.foreground) }; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(dp(BicycleDesign.presetPreviewWidth), dp(BicycleDesign.presetPreviewHeight)))
                    addView(TextView(context).apply { text = "${template.slots.size} Werte"; BicycleDialogStyle.control(this, themes); textSize = BicycleDesign.bodySp; gravity = Gravity.CENTER; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(-1, dp(BicycleDesign.iconSize)))
                }
                BicycleComponents.bindTheme(tile, themes) { paintChoice(tile, it) }
                choices[template] = tile
                row.addView(tile, LayoutParams(0, dp(BicycleDesign.presetTileHeight), 1f).apply { setMargins(dp(BicycleDesign.presetTileGap), dp(BicycleDesign.presetTileGap), dp(BicycleDesign.presetTileGap), dp(BicycleDesign.presetTileGap)) })
            }
            addView(row)
        }
        refresh()
    }
    private fun paintChoice(tile: View, tokens: ThemeTokens) {
        tile.background = GradientDrawable().apply {
            cornerRadius = dp(BicycleDesign.inputRadius).toFloat()
            setColor(if (tile.isSelected) tokens.tertiary else tokens.secondary)
            setStroke(BicycleDesign.dividerPixels, tokens.foreground)
        }
    }
    private fun refresh() {
        choices.forEach { (template, tile) ->
            val active = selection[page] == template
            tile.isSelected = active; tile.stateDescription = if (active) "Ausgewählt" else "Nicht ausgewählt"
            paintChoice(tile, themes.active.tokens)
        }
    }
}
