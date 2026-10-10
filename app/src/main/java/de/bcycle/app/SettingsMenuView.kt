package de.bcycle.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

internal enum class SettingsAction(val title: String, val icon: Int = 0) {
    SENSORS("Sensoren verwalten", R.drawable.ic_settings_sensors),
    RECORDING("Aufzeichnungsmanager", R.drawable.ic_settings_recording),
    MAP("Kartenmanager", R.drawable.ic_settings_map),
    DASHBOARDS("Dashboards", R.drawable.ic_settings_dashboards),
    ROUTES("Streckenmanager", R.drawable.ic_settings_routes),
    ADVANCED("Erweitert", R.drawable.ic_settings_advanced),
    RECOVERY("Ungespeicherte Fahrten speichern"),
    GPS_HELP("GPS-Messung erklärt"),
    DIAGNOSTIC("Diagnose speichern"),
    RESET("Standardbelegung herstellen"),
    FONT_LICENSE("Schrift & Lizenz"),
    MAP_LICENSE("Kartenbibliothek & Lizenz")
}

/** Native, scrollable menu content. Six fixed actions in two rows of three. */
internal class SettingsMenuView(context: Context, private val selected: (SettingsAction) -> Unit) : LinearLayout(context) {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun ripple() = RippleDrawable(ColorStateList.valueOf(Color.argb(45, 255, 255, 255)), null, ColorDrawable(Color.WHITE))
    init {
        orientation = VERTICAL
        addView(ImageView(context).apply {
            tag = "settings_brand"
            contentDescription = "B|Cycle"
            setImageResource(R.drawable.ic_settings_brand)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundResource(R.drawable.settings_header_background)
        }, LayoutParams(-1, dp(124)))
        val body = LinearLayout(context).apply {
            orientation = VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(8))
        }
        addView(body, LayoutParams(-1, -2))
        val advanced = LinearLayout(context).apply { orientation = VERTICAL; visibility = GONE; tag = "settings_advanced_content" }
        var advancedTile: View? = null
        var built = false
        fun toggleAdvanced() {
            val expand = advanced.visibility != VISIBLE
            if (expand && !built) {
                SettingsAction.entries.filter { it.ordinal in SettingsAction.RECOVERY.ordinal..SettingsAction.MAP_LICENSE.ordinal }.forEach { action ->
                    advanced.addView(entry(action.title) { selected(action) }.apply { tag = "settings_${action.name}" })
                }
                built = true
            }
            advanced.visibility = if (expand) VISIBLE else GONE
            advancedTile?.stateDescription = if (expand) "Ausgeklappt" else "Eingeklappt"
        }
        val actions = SettingsAction.entries.take(6)
        for (rowIndex in 0..1) {
            val row = LinearLayout(context).apply { orientation = HORIZONTAL; isBaselineAligned = false; tag = "settings_row_$rowIndex" }
            for (column in 0..2) {
                val action = actions[rowIndex * 3 + column]
                val tile = tile(action) { if (action == SettingsAction.ADVANCED) toggleAdvanced() else selected(action) }
                if (action == SettingsAction.ADVANCED) { advancedTile = tile; tile.stateDescription = "Eingeklappt" }
                row.addView(tile, LayoutParams(0, dp(148), 1f))
            }
            body.addView(row, LayoutParams(-1, -2))
        }
        body.addView(View(context).apply { tag = "settings_separator"; setBackgroundColor(Color.argb(150, 255, 255, 255)); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(-1, 1).apply { topMargin = dp(12); bottomMargin = dp(4) })
        body.addView(advanced)

    }
    private fun tile(action: SettingsAction, onClick: () -> Unit) = LinearLayout(context).apply {
        orientation = VERTICAL; gravity = Gravity.CENTER; setPadding(dp(3), dp(12), dp(3), dp(12))
        background = ripple(); isClickable = true; isFocusable = true
        contentDescription = action.title; tag = "settings_${action.name}"
        setOnClickListener { onClick() }
        addView(ImageView(context).apply { setImageResource(action.icon); importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(dp(64), dp(64)))
        addView(TextView(context).apply {
            text = when (action) { SettingsAction.SENSORS -> "Sensoren\nverwalten"; SettingsAction.RECORDING -> "Aufzeichnungs-\nmanager"; SettingsAction.MAP -> "Karten-\nmanager"; SettingsAction.ROUTES -> "Strecken-\nmanager"; else -> action.title }
            gravity = Gravity.CENTER; setTextColor(Color.WHITE); maxLines = 2; includeFontPadding = false
            typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            setAutoSizeTextTypeUniformWithConfiguration(10, 14, 1, TypedValue.COMPLEX_UNIT_SP)
        }, LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
    }
    private fun entry(title: String, action: () -> Unit) = TextView(context).apply {
        text = title; textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER_VERTICAL
        minHeight = dp(48); setPadding(dp(12), dp(8), dp(12), dp(8))
        background = ripple(); isClickable = true; isFocusable = true; setOnClickListener { action() }
    }
}
