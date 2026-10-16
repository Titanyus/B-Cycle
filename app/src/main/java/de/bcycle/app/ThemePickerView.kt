package de.bcycle.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** One palette-independent grid; only the three preview strips use each candidate's colors. */
internal class ThemePickerView(context: Context, private val themes: ThemeManager) : LinearLayout(context) {
    private fun dp(value: Int) = BicycleDesign.dp(context, value)
    private var columns = 0
    private val fontScale = resources.configuration.fontScale.coerceAtLeast(1f)
    private val minimumWidth = dp(BicycleDesign.themeTileMinimumWidth)
    private val cards = themes.presets.map(::card) + card(null)
    init {
        orientation = VERTICAL
        setPadding(0, dp(BicycleDesign.verticalPadding), 0, dp(BicycleDesign.verticalPadding))
        BicycleComponents.background(this, themes, ThemeRole.SECONDARY)
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val scaledMinimum = (minimumWidth * fontScale).toInt()
        val next = (width / (scaledMinimum + dp(BicycleDesign.smallGap))).coerceIn(1, BicycleDesign.themeColumns)
        if (next != columns) {
            columns = next
            removeAllViews()
            cards.forEach { (it.parent as? android.view.ViewGroup)?.removeView(it) }
            cards.chunked(columns).forEach { group ->
                val row = LinearLayout(context).apply { orientation = HORIZONTAL; isBaselineAligned = false }
                group.forEach { row.addView(it, LayoutParams(0, -1, 1f).apply {
                    setMargins(dp(BicycleDesign.presetTileGap), dp(BicycleDesign.smallGap), dp(BicycleDesign.presetTileGap), dp(BicycleDesign.smallGap))
                }) }
                repeat(columns - group.size) { row.addView(View(context), LayoutParams(0, -1, 1f)) }
                addView(row, LayoutParams(-1, (dp(BicycleDesign.themeTileHeight) * fontScale).toInt()))
            }
            addView(BicycleComponents.sectionDivider(context, themes))
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
    private fun card(preset: PresetTheme?): LinearLayout = LinearLayout(context).apply {
        orientation = VERTICAL; gravity = Gravity.CENTER
        setPadding(dp(BicycleDesign.verticalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.verticalPadding))
        tag = preset?.id ?: "theme_reserved"
        minimumHeight = dp(BicycleDesign.touchSize)
        contentDescription = preset?.name ?: "Reserviert"
        isEnabled = preset != null; isClickable = preset != null; isFocusable = preset != null
        if (preset != null) setOnClickListener { themes.activate(preset.id) }
        val title = TextView(context).apply {
            text = preset?.name?.uppercase(java.util.Locale.ROOT) ?: "RESERVIERT"
            gravity = Gravity.CENTER
            typeface = resources.getFont(R.font.bebas_neue)
            textSize = BicycleDesign.themeNameSp
            maxLines = 2
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            BicycleComponents.bindTheme(this, themes) { setTextColor(it.foreground) }
        }
        addView(title, LayoutParams(-1, (dp(BicycleDesign.themeNameHeight) * fontScale).toInt()))
        val sample = LinearLayout(context).apply {
            orientation = VERTICAL; clipToOutline = true
            background = GradientDrawable().apply { setColor(themes.active.tokens.secondary); cornerRadius = dp(BicycleDesign.inputRadius).toFloat() }
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        if (preset != null) {
            listOf(preset.tokens.primary, preset.tokens.secondary, preset.tokens.tertiary).forEachIndexed { index, color ->
                sample.addView(View(context).apply { tag = "${preset.id}:strip:$index"; setBackgroundColor(color) }, LayoutParams(-1, 0, 1f))
            }
        } else {
            sample.addView(ImageView(context).apply {
                setPadding(dp(BicycleDesign.rowGap), dp(BicycleDesign.rowGap), dp(BicycleDesign.rowGap), dp(BicycleDesign.rowGap))
                scaleType = ImageView.ScaleType.CENTER
                BicycleComponents.bindTheme(this, themes) { setImageDrawable(BicycleIcons.drawable(context, BicycleIcon.AddSensor, it.foreground)) }
            }, LayoutParams(-1, -1))
        }
        addView(sample, LayoutParams(-1, 0, 1f))
        BicycleComponents.bindTheme(this, themes) { tokens ->
            isSelected = preset != null && themes.active.id == preset.id
            stateDescription = if (preset == null) "Noch nicht verfügbar" else if (isSelected) "Ausgewählt" else "Nicht ausgewählt"
            background = GradientDrawable().apply {
                setColor(tokens.secondary); cornerRadius = dp(BicycleDesign.inputRadius).toFloat()
                val color = if (isSelected) BicycleComponents.selectionBorder(tokens,true) else (tokens.foreground and 0x00ffffff) or (85 shl 24)
                if (preset == null) setStroke(BicycleDesign.linePixels(context), color, dp(BicycleDesign.smallGap).toFloat(), dp(BicycleDesign.smallGap).toFloat())
                else setStroke(if (isSelected) dp(BicycleDesign.themeSelectedBorder) else BicycleDesign.linePixels(context), color)
            }
        }
    }
}
