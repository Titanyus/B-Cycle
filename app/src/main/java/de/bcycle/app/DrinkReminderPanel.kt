package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout

/** Shared primary alert surface, touch dismissal and timeout for short dashboard cues. */
internal open class DashboardAlertPanel(context:Context,themes:ThemeManager,icon:BicycleIcon,
    message:String,dismissLabel:String,panelTag:String,messageLines:Int=2):LinearLayout(context) {
    private val timeout=Runnable { clear() }
    private val label=BicycleComponents.text(context,themes,message).apply {
        textSize=BicycleDesign.titleSp;typeface=BicycleDesign.strongTypeface;maxLines=messageLines
    }
    init {
        tag=panelTag;orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;visibility=GONE
        isClickable=true;isFocusable=true;accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE
        contentDescription=dismissLabel
        val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
        setPadding(gap*2,gap*2,gap*2,gap*2)
        BicycleAlertSurface.bind(this,themes)
        addView(ImageView(context).apply {
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
            BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,icon,it)) }
        },LayoutParams(BicycleDesign.dp(context,48),BicycleDesign.dp(context,48)).apply { marginEnd=gap*2 })
        addView(label,LayoutParams(0,-2,1f))
        setOnClickListener { clear() }
    }
    protected fun showFor(durationSeconds:Int,message:String?=null) {
        if(message!=null) label.text=message
        removeCallbacks(timeout);visibility=VISIBLE;postDelayed(timeout,durationSeconds*1000L)
    }
    fun clear() { removeCallbacks(timeout);visibility=GONE }
    override fun onDetachedFromWindow() { clear();super.onDetachedFromWindow() }
}

/** Dismissal does not change the periodic schedule. */
internal class DrinkReminderPanel(context:Context,themes:ThemeManager):DashboardAlertPanel(context,themes,
    BicycleIcon.DrinkReminder,context.getString(R.string.drink_reminder_message),
    context.getString(R.string.drink_reminder_dismiss),"drink_reminder_panel") {
    fun showReminder(durationSeconds:Int=15) {
        require(DrinkReminderSettings.validDuration(durationSeconds));showFor(durationSeconds)
    }
}

internal class ShiftBatteryWarningPanel(context:Context,themes:ThemeManager):DashboardAlertPanel(context,themes,
    BicycleIcon.Derailleur,"",context.getString(R.string.axs_warning_dismiss),"shift_battery_warning_panel",3) {
    fun showBattery(level:Int) { require(level in 0..19);showFor(10,context.getString(R.string.axs_warning_message,level)) }
}
