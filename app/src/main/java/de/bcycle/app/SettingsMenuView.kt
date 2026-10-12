package de.bcycle.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

internal enum class SettingsAction(val title: String, val icon: Int = 0, val titleResource: Int = 0) {
    SENSORS("Sensoren & Aufzeichnung", BicycleIcon.Sensors.resource),
    MAP("Kartenmanager", BicycleIcon.MapManager.resource),
    DASHBOARDS("Design & UI", BicycleIcon.Dashboard.resource),
    ROUTES("Streckenmanager", BicycleIcon.Route.resource),
    ADVANCED("Erweitert", BicycleIcon.Advanced.resource),
    PROFILE("", BicycleIcon.RiderProfile.resource, R.string.rider_profile),
    RECOVERY("Ungespeicherte Fahrten speichern"),
    GPS_HELP("GPS-Messung erklärt"),
    DIAGNOSTIC("Diagnose speichern"),
    RESET("Standardbelegung herstellen"),
    FONT_LICENSE("Schrift & Lizenz"),
    MAP_LICENSE("Bibliotheken & Lizenzen");
    fun label(context: Context) = if (titleResource == 0) title else context.getString(titleResource)
}

/** Native, scrollable menu content. Six fixed actions in two rows of three. */
internal class SettingsMenuView(context: Context, private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)), private val selected: (SettingsAction) -> Unit) : LinearLayout(context) {
    private fun dp(value: Int) = BicycleDesign.dp(context, value)
    private fun ripple(view: View) = BicycleComponents.bindTheme(view, themes) { tokens ->
        view.background = RippleDrawable(ColorStateList.valueOf((tokens.foreground and 0x00ffffff) or (45 shl 24)), null, ColorDrawable(tokens.foreground))
    }
    init {
        orientation = VERTICAL
        BicycleComponents.background(this, themes, ThemeRole.SECONDARY)
        addView(ImageView(context).apply {
            tag = "settings_brand"
            contentDescription = "B|Cycle"
            setImageResource(R.drawable.ic_settings_brand)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(BicycleDesign.headerHorizontalPadding), dp(BicycleDesign.headerHorizontalPadding), dp(BicycleDesign.headerHorizontalPadding), dp(BicycleDesign.headerHorizontalPadding))
            BicycleComponents.background(this, themes, ThemeRole.PRIMARY, BicycleDesign.dialogRadius, header = true)
            BicycleComponents.bindTheme(this, themes) { setColorFilter(it.foreground) }
        }, LayoutParams(-1, dp(BicycleDesign.menuHeaderHeight)))
        val body = LinearLayout(context).apply {
            orientation = VERTICAL; setPadding(dp(BicycleDesign.rowGap), dp(BicycleDesign.rowGap), dp(BicycleDesign.rowGap), dp(BicycleDesign.verticalPadding))
        }
        addView(body, LayoutParams(-1, -2))
        val advanced = LinearLayout(context).apply { orientation = VERTICAL; visibility = GONE; tag = "settings_advanced_content" }
        var advancedTile: View? = null
        var built = false
        fun toggleAdvanced() {
            val expand = advanced.visibility != VISIBLE
            if (expand && !built) {
                SettingsAction.entries.filter { it.ordinal in SettingsAction.RECOVERY.ordinal..SettingsAction.MAP_LICENSE.ordinal }.forEach { action ->
                    advanced.addView(entry(action.label(context)) { selected(action) }.apply { tag = "settings_${action.name}" })
                }
                built = true
            }
            advanced.visibility = if (expand) VISIBLE else GONE
            advancedTile?.stateDescription = if (expand) "Ausgeklappt" else "Eingeklappt"
        }
        val actions = listOf(SettingsAction.SENSORS, SettingsAction.PROFILE, SettingsAction.MAP, SettingsAction.DASHBOARDS, SettingsAction.ROUTES, SettingsAction.ADVANCED)
        for (rowIndex in 0..1) {
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; isBaselineAligned = false; tag = "settings_row_$rowIndex" }
            for (column in 0..2) {
                val action = actions[rowIndex * 3 + column]
                val tile = tile(action) { if (action == SettingsAction.ADVANCED) toggleAdvanced() else selected(action) }
                if (action == SettingsAction.ADVANCED) { advancedTile = tile; tile.stateDescription = "Eingeklappt" }
                row.addView(tile, LayoutParams(0, dp(BicycleDesign.menuTileHeight), 1f))
            }
            body.addView(row, LayoutParams(-1, -2))
        }
        body.addView(BicycleComponents.sectionDivider(context, themes).apply { tag = "settings_separator" })
        body.addView(advanced)

    }
    private fun tile(action: SettingsAction, onClick: () -> Unit) = LinearLayout(context).apply {
        orientation = VERTICAL; gravity = Gravity.CENTER; setPadding(dp(BicycleDesign.menuTileHorizontalPadding), dp(BicycleDesign.rowGap), dp(BicycleDesign.menuTileHorizontalPadding), dp(BicycleDesign.rowGap))
        ripple(this); isClickable = true; isFocusable = true
        contentDescription = action.label(context); tag = "settings_${action.name}"
        setOnClickListener { onClick() }
        addView(ImageView(context).apply { setImageResource(action.icon); BicycleComponents.bindTheme(this, themes) { setColorFilter(it.foreground) }; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(dp(BicycleDesign.menuIconSize), dp(BicycleDesign.menuIconSize)))
        addView(TextView(context).apply {
            text = when (action) { SettingsAction.SENSORS -> "Sensoren &\nAufzeichnung"; SettingsAction.MAP -> "Karten-\nmanager"; SettingsAction.ROUTES -> "Strecken-\nmanager"; else -> action.label(context) }
            gravity = Gravity.CENTER; BicycleComponents.bindTheme(this, themes) { setTextColor(it.foreground) }; maxLines = 2; includeFontPadding = false
            typeface = Typeface.DEFAULT
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            setAutoSizeTextTypeUniformWithConfiguration(10, 14, 1, TypedValue.COMPLEX_UNIT_SP)
        }, LayoutParams(-1, dp(BicycleDesign.touchSize)).apply { topMargin = dp(BicycleDesign.verticalPadding) })
    }
    private fun entry(title: String, action: () -> Unit) = TextView(context).apply {
        text = title; textSize = 15f; BicycleComponents.bindTheme(this, themes) { setTextColor(it.foreground) }; gravity = Gravity.CENTER_VERTICAL
        minHeight = dp(BicycleDesign.touchSize); setPadding(dp(BicycleDesign.rowGap), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.rowGap), dp(BicycleDesign.verticalPadding))
        ripple(this); isClickable = true; isFocusable = true; setOnClickListener { action() }
    }
}
