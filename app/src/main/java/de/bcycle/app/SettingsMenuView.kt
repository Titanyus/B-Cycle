package de.bcycle.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

internal enum class SettingsAction(val title: String, val icon: Int = 0, val titleResource: Int = 0) {
    PROFILES("", BicycleIcon.Bikes.resource, titleResource=R.string.profiles_title),
    SENSORS("", BicycleIcon.Bikes.resource, titleResource = R.string.bikes_title),
    MAP("", BicycleIcon.MapManager.resource, titleResource = R.string.ui_settings_map),
    DASHBOARDS("", BicycleIcon.Dashboard.resource, titleResource = R.string.ui_settings_dashboards),
    ROUTES("", BicycleIcon.Route.resource, titleResource = R.string.ui_settings_routes),
    STATISTICS("", BicycleIcon.Distance.resource, titleResource=R.string.statistics_title),
    ADVANCED("", BicycleIcon.Advanced.resource, titleResource = R.string.ui_settings_advanced),
    PROFILE("", BicycleIcon.RiderProfile.resource, R.string.ui_settings_profile),
    DATA("", BicycleIcon.DataStorage.resource, R.string.data_title),
    NOTIFICATIONS("", BicycleIcon.Notifications.resource, R.string.notifications_title),
    HRV("", BicycleIcon.Heart.resource, R.string.hrv_settings_title),
    MAINTENANCE("", BicycleIcon.Work.resource, R.string.maintenance_title),
    RECOVERY("", titleResource = R.string.ui_settings_recovery),
    GPS_HELP("", titleResource = R.string.ui_settings_gps_help),
    DIAGNOSTIC("", titleResource = R.string.ui_settings_diagnostic),
    RESET("", titleResource = R.string.ui_settings_reset),
    SOURCES("", BicycleIcon.Info.resource, R.string.sources_title),
    FONT_LICENSE("", titleResource = R.string.ui_settings_font_license),
    MAP_LICENSE("", titleResource = R.string.ui_settings_map_license);
    fun label(context: Context) = if (titleResource == 0) title else context.getString(titleResource)
}

/** One row per destination; main menu uses compact League Gothic labels. */
internal class SettingsMenuView(context: Context, private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)),
    private val advanced: Boolean = false, private val selected: (SettingsAction) -> Unit) : LinearLayout(context) {
    val isMainPage get() = !advanced
    init {
        orientation=VERTICAL
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        val actions=if(advanced) SettingsAction.entries.filter { it.ordinal>=SettingsAction.RECOVERY.ordinal && it !in setOf(SettingsAction.RECOVERY,SettingsAction.GPS_HELP) } else
            listOf(SettingsAction.ROUTES,SettingsAction.STATISTICS,SettingsAction.HRV,SettingsAction.MAINTENANCE,SettingsAction.PROFILE,SettingsAction.PROFILES,SettingsAction.NOTIFICATIONS,SettingsAction.DATA,SettingsAction.ADVANCED)
        actions.forEachIndexed { index,action ->
            if(index>0 && (advanced || action==SettingsAction.PROFILE || action==SettingsAction.ADVANCED))
                addView(BicycleComponents.divider(context,themes).apply { tag="settings_group_divider" },LayoutParams(-1,BicycleDesign.linePixels(context)).apply {
                    if(!advanced) { topMargin=BicycleDesign.dp(context,2*BicycleDesign.sectionDividerGap);bottomMargin=topMargin }
                })
            addView(BicycleComponents.listRow(context,themes).apply {
                tag="settings_${action.name}";contentDescription=action.label(context);isClickable=true;isFocusable=true
                minimumHeight=BicycleDesign.dp(context,if(advanced) BicycleDesign.touchSize else BicycleDesign.settingsRowHeight)
                if(!advanced) setPadding(paddingLeft,BicycleDesign.dp(context,BicycleDesign.settingsRowVerticalPadding),paddingRight,BicycleDesign.dp(context,BicycleDesign.settingsRowVerticalPadding))
                setOnClickListener { selected(action) }
                if(action.icon!=0) addView(ImageView(context).apply {
                    setImageResource(action.icon);BicycleComponents.bindTheme(this,themes) { setColorFilter(it.foreground) }
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                },LayoutParams(BicycleDesign.dp(context,if(advanced) BicycleDesign.iconSize else BicycleDesign.settingsIconSize),BicycleDesign.dp(context,if(advanced) BicycleDesign.iconSize else BicycleDesign.settingsIconSize)))
                addView(BicycleComponents.text(context,themes,action.label(context)).apply { gravity=Gravity.CENTER_VERTICAL; textSize=if(advanced) BicycleDesign.bodySp else BicycleDesign.sectionTitleSp; if(!advanced) typeface=BicycleDesign.narrowTypeface(context) },
                    LayoutParams(0,-2,1f).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.verticalPadding) })
                addView(ImageView(context).apply { BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,BicycleIcon.Forward,it.foreground)) }
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LayoutParams(BicycleDesign.dp(context,if(advanced) BicycleDesign.iconSize else BicycleDesign.settingsChevronSize),BicycleDesign.dp(context,if(advanced) BicycleDesign.iconSize else BicycleDesign.settingsChevronSize)))
            },LayoutParams(-1,-2))
        }
        if (!advanced) addView(FrameLayout(context).apply {
            tag = "settings_brand_art"
            addView(ImageView(context).apply {
                setImageResource(R.drawable.settings_brand)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }, FrameLayout.LayoutParams(-1, -2))
            addView(TextView(context).apply {
                text = context.getString(R.string.settings_build_info, AppVersion.name, AppVersion.buildDate())
                tag = "settings_build_info"
                gravity = Gravity.CENTER
                textSize = BicycleDesign.analysisLabelSp
                setTextColor(android.graphics.Color.WHITE)
            }, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = BicycleDesign.dp(context, BicycleDesign.verticalPadding)
            })
        }, LayoutParams(-1, -2))
    }
}
