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
        var config=initial
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
        },LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.sectionHeaderMinHeight)))
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
            config=config.copy(enabled=enabled.isChecked,minutes=10+slider.progress*5)
            DrinkReminderSettings.save(prefs,config);changed()
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
        val seconds=BicycleComponents.text(context,themes,context.getString(R.string.drink_reminder_seconds,initial.durationSeconds)).apply {
            tag="drink_reminder_duration_value";gravity=Gravity.CENTER
        }
        val durationSlider=BicycleRatingSlider(context,themes,0,11).apply {
            tag="drink_reminder_duration";id=View.generateViewId();progress=(initial.durationSeconds-5)/5
            contentDescription=context.getString(R.string.drink_reminder_duration)
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,progress:Int,fromUser:Boolean) {
                    val value=5+progress*5;seconds.text=context.getString(R.string.drink_reminder_seconds,value);stateDescription=seconds.text
                    if(fromUser && canEdit()) { config=config.copy(durationSeconds=value);save() }
                }
            })
        }
        details.addView(GridLayout(context).apply {
            columnCount=2;tag="drink_reminder_duration_row"
            addView(seconds,GridLayout.LayoutParams(GridLayout.spec(0),GridLayout.spec(1,1f)).apply { width=0;setGravity(Gravity.FILL_HORIZONTAL) })
            addView(BicycleComponents.text(context,themes,context.getString(R.string.drink_reminder_duration)).apply {
                labelFor=durationSlider.id;gravity=Gravity.CENTER_VERTICAL
            },GridLayout.LayoutParams(GridLayout.spec(1,GridLayout.CENTER),GridLayout.spec(0)).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            addView(durationSlider,GridLayout.LayoutParams(GridLayout.spec(1),GridLayout.spec(1,1f)).apply {
                width=0;height=BicycleDesign.dp(context,BicycleDesign.touchSize);setGravity(Gravity.FILL_HORIZONTAL)
            })
        },LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        details.addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.notification_position_heading)),LayoutParams(-1,-2).apply {
            topMargin=BicycleDesign.dp(context,BicycleDesign.sectionContentGap)
        })
        details.addView(MessagePositionChoices(context,themes,initial.position,"drink_reminder_position",canEdit) {
            config=config.copy(position=it);save()
        },LayoutParams(-1,-2))
        addView(details,LayoutParams(-1,-2))
    }
}
