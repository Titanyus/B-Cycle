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
    HRV("", BicycleIcon.Advanced.resource, R.string.hrv_settings_title),
    MAINTENANCE("", BicycleIcon.Work.resource, R.string.maintenance_title),
    RECOVERY("", titleResource = R.string.ui_settings_recovery),
    GPS_HELP("", titleResource = R.string.ui_settings_gps_help),
    DIAGNOSTIC("", titleResource = R.string.ui_settings_diagnostic),
    RESET("", titleResource = R.string.ui_settings_reset),
    SOURCES("", BicycleIcon.Info.resource, R.string.sources_title),
    FONT_LICENSE("", titleResource = R.string.ui_settings_font_license),
    MAP_LICENSE("", titleResource = R.string.ui_settings_map_license),
    ROUTE_TEMPLATES("",BicycleIcon.Route.resource,titleResource=R.string.route_templates_title),
    HRV_ANALYSIS("",BicycleIcon.ChartLine.resource,titleResource=R.string.hrvt_title),
    HRV_MEASUREMENT("",BicycleIcon.HeartPulse.resource,titleResource=R.string.hrv_measurement_open),
    TRAINING("",BicycleIcon.Training.resource,titleResource=R.string.training_title);
    fun label(context: Context) = if (titleResource == 0) title else context.getString(titleResource)
}

/** One row per destination; main menu uses compact League Gothic labels. */
internal class SettingsMenuView(context: Context, private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)),
    private val advanced: Boolean = false, private val selected: (SettingsAction) -> Unit) : LinearLayout(context) {
    val isMainPage get() = !advanced
    init {
        orientation=VERTICAL
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        if(advanced) {
            val actions=SettingsAction.entries.filter { it.ordinal in SettingsAction.RECOVERY.ordinal..SettingsAction.MAP_LICENSE.ordinal && it !in setOf(SettingsAction.RECOVERY,SettingsAction.GPS_HELP,SettingsAction.ROUTE_TEMPLATES) }
            actions.forEachIndexed { index,action ->
                if(index>0) addView(BicycleComponents.divider(context,themes),LayoutParams(-1,BicycleDesign.linePixels(context)))
                addView(menuRow(action,action.label(context),false),LayoutParams(-1,-2))
            }
        } else {
            addGroup("Bike",BicycleIcon.Bikes,listOf(SettingsAction.ROUTES,SettingsAction.ROUTE_TEMPLATES,SettingsAction.STATISTICS,SettingsAction.MAINTENANCE),
                listOf(SettingsAction.PROFILE,SettingsAction.PROFILES,SettingsAction.NOTIFICATIONS,SettingsAction.DATA))
            addGroup(context.getString(R.string.training_title),BicycleIcon.Training,listOf(SettingsAction.TRAINING),emptyList())
            addGroup("HRV",BicycleIcon.Heart,listOf(SettingsAction.HRV_MEASUREMENT,SettingsAction.HRV_ANALYSIS),listOf(SettingsAction.HRV))
            addView(menuRow(SettingsAction.ADVANCED,SettingsAction.ADVANCED.label(context),false),LayoutParams(-1,-2))
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
        }, LayoutParams(-1, -2).apply {
            topMargin=BicycleDesign.dp(context,BicycleDesign.settingsRowHeight)/2
        })
    }
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    private fun menuRow(action:SettingsAction,label:String,compact:Boolean):LinearLayout = BicycleComponents.listRow(context,themes).apply {
        tag="settings_${action.name}";contentDescription=label;isClickable=true;isFocusable=true
        minimumHeight=dp(if(compact || advanced) BicycleDesign.touchSize else BicycleDesign.settingsRowHeight)
        if(compact) BicycleComponents.bindTheme(this,themes) { background=null }
        if(compact) setPadding(dp(BicycleDesign.controlGap),dp(BicycleDesign.settingsRowVerticalPadding),dp(BicycleDesign.smallGap),dp(BicycleDesign.settingsRowVerticalPadding))
        setOnClickListener { selected(action) }
        if(action.icon!=0) addView(ImageView(context).apply {
            setImageResource(action.icon);BicycleComponents.bindTheme(this,themes) { setColorFilter(it.foreground) }
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        },LayoutParams(dp(BicycleDesign.iconSize),dp(BicycleDesign.iconSize)))
        addView(BicycleComponents.text(context,themes,label).apply {
            gravity=Gravity.CENTER_VERTICAL
            textSize=if(compact || advanced) BicycleDesign.bodySp else BicycleDesign.sectionTitleSp
            typeface=if(compact || advanced) BicycleDesign.bodyTypeface else BicycleDesign.narrowTypeface(context)
        },LayoutParams(0,-2,1f).apply { marginStart=dp(if(compact) BicycleDesign.smallGap else BicycleDesign.verticalPadding) })

    }
    private fun addGroup(title:String,icon:BicycleIcon,left:List<SettingsAction>,right:List<SettingsAction>) {
        val group=LinearLayout(context).apply {
            orientation=VERTICAL;tag="settings_box_$title"
            setPadding(dp(BicycleDesign.controlGap),dp(BicycleDesign.controlGap),dp(BicycleDesign.controlGap),dp(BicycleDesign.controlGap))
            BicycleComponents.bindTheme(this,themes) {
                background=BicycleComponents.controlBackground(context,it,it.tertiary)
                val pad=dp(if(BicycleAppearance.flat(context)) BicycleDesign.horizontalPadding else BicycleDesign.controlGap)
                setPadding(pad,pad,pad,pad)
                (layoutParams as? LayoutParams)?.let { params ->
                    val gap=dp(if(BicycleAppearance.flat(context)) BicycleDesign.smallGap else BicycleDesign.controlGap)
                    if(params.leftMargin!=gap || params.rightMargin!=gap) { params.leftMargin=gap;params.rightMargin=gap;layoutParams=params }
                }
            }
        }
        group.addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;isBaselineAligned=false;minimumHeight=dp(BicycleDesign.settingsRowHeight)
            BicycleComponents.bindTheme(this,themes) { setPadding(dp(BicycleDesign.horizontalPadding),0,0,dp(if(BicycleAppearance.flat(context)) BicycleDesign.horizontalPadding else BicycleDesign.controlGap)) }
            addView(ImageView(context).apply { BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,icon,it.foreground)) };importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LayoutParams((dp(BicycleDesign.settingsIconSize)*1.2f).toInt(),(dp(BicycleDesign.settingsIconSize)*1.2f).toInt()))
            addView(BicycleComponents.text(context,themes,"/").apply { typeface=BicycleDesign.narrowTypeface(context);textSize=BicycleDesign.headingSp;BicycleComponents.bindTheme(this,themes) { setTextColor(it.primary) };importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LayoutParams(-2,-2).apply { marginStart=dp(BicycleDesign.controlGap);marginEnd=marginStart })
            addView(BicycleComponents.text(context,themes,title).apply { typeface=BicycleDesign.narrowTypeface(context);textSize=BicycleDesign.headingSp;isAccessibilityHeading=true },LayoutParams(0,-2,1f))
        },LayoutParams(-1,-2))
        fun column(actions:List<SettingsAction>,side:String)=BicycleGroupedBox(context,themes).apply {
            tag="settings_column_${title}_$side"
            actions.forEach { action ->
                val label=when(action) {
                    SettingsAction.ROUTES -> context.getString(R.string.settings_group_routes)
                    SettingsAction.NOTIFICATIONS -> context.getString(R.string.settings_group_messages)
                    SettingsAction.STATISTICS -> context.getString(R.string.settings_group_statistics)
                    SettingsAction.HRV_MEASUREMENT -> context.getString(R.string.settings_group_measurement)
                    SettingsAction.HRV_ANALYSIS -> context.getString(R.string.settings_group_analysis)
                    SettingsAction.HRV -> context.getString(R.string.settings_group_hrv_settings)
                    else -> action.label(context)
                }
                addEntry(menuRow(action,label,true))
            }
        }
        group.addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;isBaselineAligned=false
            addView(column(left,"left"),LayoutParams(0,if(title=="HRV") -2 else -1,1f))
            addView(column(right,"right"),LayoutParams(0,if(title=="HRV") -2 else -1,1f).apply { gravity=Gravity.TOP;marginStart=dp(BicycleDesign.controlGap) })
        },LayoutParams(-1,-2))
        addView(group,LayoutParams(-1,-2).apply { val gap=dp(BicycleDesign.controlGap);val side=dp(if(BicycleAppearance.flat(context)) BicycleDesign.smallGap else BicycleDesign.controlGap);setMargins(side,gap,side,gap) })
    }

}
