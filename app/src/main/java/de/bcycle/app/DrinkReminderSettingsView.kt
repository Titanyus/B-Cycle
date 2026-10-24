package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.*

internal class DrinkReminderSettingsView(context:Context,themes:ThemeManager,prefs:SharedPreferences,
    canEdit:()->Boolean={true},changed:()->Unit={}):LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="drink_reminder_settings"
        val initial=DrinkReminderSettings.read(prefs)
        val enabled=Switch(context).apply {
            tag="drink_reminder_enabled";setText(R.string.drink_reminder_title);isChecked=initial.enabled
            BicycleDialogStyle.control(this,themes)
        }
        val details=LinearLayout(context).apply { orientation=VERTICAL;tag="drink_reminder_details";visibility=GONE }
        val expand=BicycleComponents.icon(context,themes,BicycleIcon.Expand,context.getString(R.string.drink_reminder_title)) {
            val open=details.visibility!=VISIBLE
            details.visibility=if(open) VISIBLE else GONE
            findViewWithTag<View>("drink_reminder_expand").apply {
                rotation=if(open) 180f else 0f
                stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
            }
        }.apply { tag="drink_reminder_expand";stateDescription=context.getString(R.string.zone_collapsed) }
        addView(LinearLayout(context).apply {
            BicycleComponents.switchAccordion(this,enabled,expand,themes,icon=BicycleIcon.DrinkReminder)
        },LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.sectionHeaderMinHeight)+2*BicycleDesign.linePixels(context,BicycleDesign.sectionRuleDp)))
        val value=BicycleComponents.text(context,themes,"").apply { tag="drink_reminder_interval_value";gravity=Gravity.CENTER }
        val slider=SeekBar(context).apply {
            tag="drink_reminder_interval";id=View.generateViewId();max=10;progress=(initial.minutes-10)/5
            contentDescription=context.getString(R.string.drink_reminder_interval)
            BicycleComponents.bindTheme(this,themes) {
                thumbTintList=ColorStateList.valueOf(it.primary);progressTintList=thumbTintList
                progressBackgroundTintList=ColorStateList.valueOf((it.foreground and 0x00ffffff) or 0x55000000)
            }
        }
        fun showValue() {
            value.text=context.getString(R.string.drink_reminder_minutes,10+slider.progress*5)
            slider.stateDescription=value.text
        }
        fun save() {
            if(!canEdit()) return
            DrinkReminderSettings.save(prefs,DrinkReminderConfig(enabled.isChecked,10+slider.progress*5));changed()
        }
        showValue()
        enabled.setOnCheckedChangeListener { _,_->save() }
        slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar:SeekBar)=Unit
            override fun onStopTrackingTouch(bar:SeekBar)=Unit
            override fun onProgressChanged(bar:SeekBar,progress:Int,fromUser:Boolean) { showValue();if(fromUser) save() }
        })
        val duration=GridLayout(context).apply { columnCount=2 }
        duration.addView(value,GridLayout.LayoutParams(GridLayout.spec(0),GridLayout.spec(1,1f)).apply { width=0;setGravity(Gravity.FILL_HORIZONTAL) })
        duration.addView(BicycleComponents.text(context,themes,context.getString(R.string.drink_reminder_interval)).apply {
            labelFor=slider.id;gravity=Gravity.CENTER_VERTICAL
        },GridLayout.LayoutParams(GridLayout.spec(1,GridLayout.CENTER),GridLayout.spec(0)).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        duration.addView(slider,GridLayout.LayoutParams(GridLayout.spec(1),GridLayout.spec(1,1f)).apply {
            width=0;height=BicycleDesign.dp(context,BicycleDesign.touchSize);setGravity(Gravity.FILL_HORIZONTAL)
        })
        details.addView(duration,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(details,LayoutParams(-1,-2))
    }
}
