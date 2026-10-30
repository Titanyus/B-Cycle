package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
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
        val frame=BicycleFrameDropdown(context,themes,context.getString(R.string.drink_reminder_title),open=false,
            trailing=enabled,leadingIcon=BicycleIcon.DrinkReminder)
        frame.expandControl.tag="drink_reminder_expand"
        val details=frame.content.apply { tag="drink_reminder_details" }
        addView(frame,LayoutParams(-1,-2))
        val slider=BicycleSettingSlider(context,themes) { context.getString(R.string.drink_reminder_minutes,10+it*5) }.apply {
            tag="drink_reminder_interval";max=10;progress=(initial.minutes-10)/5
            contentDescription=context.getString(R.string.drink_reminder_interval)
        }
        fun save() {
            if(!canEdit()) return
            config=config.copy(enabled=enabled.isChecked,minutes=10+slider.progress*5)
            DrinkReminderSettings.save(prefs,config);changed()
        }
        enabled.setOnCheckedChangeListener { _,_->save() }
        slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar:SeekBar)=Unit
            override fun onStopTrackingTouch(bar:SeekBar)=Unit
            override fun onProgressChanged(bar:SeekBar,progress:Int,fromUser:Boolean) {
                bar.stateDescription=slider.valueText;if(fromUser) save()
            }
        })
        details.addView(settingSliderRow(context,themes,context.getString(R.string.drink_reminder_interval),slider),LayoutParams(-1,-2))
        val durationSlider=BicycleSettingSlider(context,themes) { context.getString(R.string.drink_reminder_seconds,5+it*5) }.apply {
            tag="drink_reminder_duration";max=11;progress=(initial.durationSeconds-5)/5
            contentDescription=context.getString(R.string.drink_reminder_duration)
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,progress:Int,fromUser:Boolean) {
                    bar.stateDescription=valueText
                    if(fromUser && canEdit()) { config=config.copy(durationSeconds=5+progress*5);save() }
                }
            })
        }
        details.addView(settingSliderRow(context,themes,context.getString(R.string.drink_reminder_duration),durationSlider).apply {
            tag="drink_reminder_duration_row"
        },LayoutParams(-1,-2))
        details.addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.notification_position_heading)),LayoutParams(-1,-2).apply {
            topMargin=BicycleDesign.dp(context,BicycleDesign.sectionContentGap)
        })
        details.addView(MessagePositionChoices(context,themes,initial.position,"drink_reminder_position",canEdit) {
            config=config.copy(position=it);save()
        },LayoutParams(-1,-2))
    }
}
