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
    DATA("", BicycleIcon.DataStorage.resource, R.string.data_title),
    RECOVERY("Ungespeicherte Fahrten speichern"),
    GPS_HELP("GPS-Messung erklärt"),
    DIAGNOSTIC("Diagnose speichern"),
    RESET("Standardbelegung herstellen"),
    FONT_LICENSE("Schrift & Lizenz"),
    MAP_LICENSE("Bibliotheken & Lizenzen");
    fun label(context: Context) = if (titleResource == 0) title else context.getString(titleResource)
}

/** One row per destination; the main menu alone uses double-height rows. */
internal class SettingsMenuView(context: Context, private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)),
    private val advanced: Boolean = false, private val selected: (SettingsAction) -> Unit) : LinearLayout(context) {
    init {
        orientation=VERTICAL
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        val actions=if(advanced) SettingsAction.entries.filter { it.ordinal>=SettingsAction.RECOVERY.ordinal } else
            listOf(SettingsAction.SENSORS,SettingsAction.PROFILE,SettingsAction.MAP,SettingsAction.DASHBOARDS,SettingsAction.ROUTES,SettingsAction.DATA,SettingsAction.ADVANCED)
        actions.forEachIndexed { index,action ->
            if(index>0) addView(BicycleComponents.divider(context,themes),LayoutParams(-1,BicycleDesign.dividerPixels))
            addView(BicycleComponents.listRow(context,themes).apply {
                tag="settings_${action.name}";contentDescription=action.label(context);isClickable=true;isFocusable=true
                minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize*(if(advanced) 1 else 2))
                setOnClickListener { selected(action) }
                if(action.icon!=0) addView(ImageView(context).apply {
                    setImageResource(action.icon);BicycleComponents.bindTheme(this,themes) { setColorFilter(it.foreground) }
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                },LayoutParams(BicycleDesign.dp(context,BicycleDesign.iconSize),BicycleDesign.dp(context,BicycleDesign.iconSize)))
                addView(BicycleComponents.text(context,themes,action.label(context)).apply { gravity=Gravity.CENTER_VERTICAL },
                    LayoutParams(0,-2,1f).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.verticalPadding) })
                addView(ImageView(context).apply { BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,BicycleIcon.Forward,it.foreground)) }
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LayoutParams(BicycleDesign.dp(context,BicycleDesign.iconSize),BicycleDesign.dp(context,BicycleDesign.iconSize)))
            },LayoutParams(-1,-2))
        }
    }
}
