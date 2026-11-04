package de.bcycle.app

import android.content.Context
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch

internal class HrvSettingsView(context:Context,themes:ThemeManager,canEdit:()->Boolean={true}):LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="hrv_settings"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        val hrvHeader=BicycleForm.section(this,themes,R.string.hrvt_hrv)
        val controls=LinearLayout(context).apply { orientation=VERTICAL;tag="hrv_protocol_settings" }
        val settings=HrvSettings(context)
        fun duration(rest:Boolean) {
            val minimum=if(rest) 15 else 60;val maximum=if(rest) 60 else 300
            val initial=settings.read().let { if(rest) it.restSeconds else it.measureSeconds }
            val title=context.getString(if(rest) R.string.hrv_rest_duration else R.string.hrv_measure_duration)
            val slider=BicycleSettingSlider(context,themes) { progress ->
                val seconds=minimum+progress*15
                context.getString(R.string.hrv_duration_value,seconds/60,seconds%60)
            }.apply {
                id=generateViewId();tag=if(rest) "hrv_rest_seconds" else "hrv_measure_seconds"
                max=(maximum-minimum)/15;progress=(initial-minimum)/15;keyProgressIncrement=1
                contentDescription=title;isEnabled=canEdit()
            }
            fun show() {
                val seconds=minimum+slider.progress*15
                slider.stateDescription=context.getString(R.string.hrv_duration_seconds,seconds)
            }
            show()
            slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,progress:Int,fromUser:Boolean) {
                    show()
                    if(fromUser && canEdit()) {
                        val seconds=minimum+progress*15;val old=settings.read()
                        settings.write(if(rest) old.copy(restSeconds=seconds) else old.copy(measureSeconds=seconds))
                    }
                }
            })
            controls.addView(settingSliderRow(context,themes,title,slider),LayoutParams(-1,-2))
        }
        duration(true);duration(false)
        controls.addView(BicycleComponents.sectionDivider(context,themes))
        controls.addView(Switch(context).apply {
            tag="hrv_first_start";setText(R.string.hrv_first_start);isChecked=settings.read().showOnFirstStart
            BicycleDialogStyle.control(this,themes)
            setOnCheckedChangeListener { _,on -> settings.write(settings.read().copy(showOnFirstStart=on)) }
        },LayoutParams(-1,-2))
        fun signal(vibrate:Boolean) {
            controls.addView(Switch(context).apply {
                tag=if(vibrate) "hrv_vibrate_end" else "hrv_sound_end"
                setText(if(vibrate) R.string.hrv_vibrate_end else R.string.hrv_sound_end)
                isChecked=settings.read().let { if(vibrate) it.vibrateAtEnd else it.soundAtEnd }
                BicycleDialogStyle.control(this,themes)
                setOnCheckedChangeListener { _,on ->
                    val old=settings.read();settings.write(if(vibrate) old.copy(vibrateAtEnd=on) else old.copy(soundAtEnd=on))
                }
            },LayoutParams(-1,-2))
        }
        signal(true);signal(false)
        addView(controls,LayoutParams(-1,-2));hrvHeader.accordion(controls,true)
        val heading=BicycleForm.section(this,themes,R.string.hrvq_title)
        val questions=HrvQuestionSettingsView(context,themes)
        addView(questions,LayoutParams(-1,-2));heading.accordion(questions,false)
    }
}
