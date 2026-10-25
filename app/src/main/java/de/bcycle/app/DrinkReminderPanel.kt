package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout

/** Short, dismissible dashboard cue; dismissing it does not change the periodic schedule. */
internal class DrinkReminderPanel(context:Context,themes:ThemeManager):LinearLayout(context) {
    private val timeout=Runnable { clear() }
    init {
        tag="drink_reminder_panel";orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;visibility=GONE
        isClickable=true;isFocusable=true;accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE
        contentDescription=context.getString(R.string.drink_reminder_dismiss)
        val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
        setPadding(gap*2,gap*2,gap*2,gap*2)
        BicycleDialogSurface.bind(this,themes,BicycleDialogSurface.alert)
        addView(ImageView(context).apply {
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
            BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,BicycleIcon.DrinkReminder,it)) }
        },LayoutParams(BicycleDesign.dp(context,48),BicycleDesign.dp(context,48)).apply { marginEnd=gap*2 })
        addView(BicycleComponents.text(context,themes,context.getString(R.string.drink_reminder_message)).apply {
            textSize=BicycleDesign.titleSp;typeface=BicycleDesign.strongTypeface;maxLines=2
        },LayoutParams(0,-2,1f))
        setOnClickListener { clear() }
    }
    fun showReminder(durationSeconds:Int=15) {
        require(DrinkReminderSettings.validDuration(durationSeconds))
        removeCallbacks(timeout);visibility=VISIBLE;postDelayed(timeout,durationSeconds*1000L)
    }
    fun clear() { removeCallbacks(timeout);visibility=GONE }
    override fun onDetachedFromWindow() { clear();super.onDetachedFromWindow() }
}
