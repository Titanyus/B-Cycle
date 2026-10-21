package de.bcycle.app

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar

internal class HrvSettingsView(context:Context,themes:ThemeManager,canEdit:()->Boolean={true}):LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="hrv_settings"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        val settings=HrvSettings(context)
        fun duration(rest:Boolean) {
            val minimum=if(rest) 15 else 60;val maximum=if(rest) 60 else 300
            val initial=settings.read().let { if(rest) it.restSeconds else it.measureSeconds }
            val label=BicycleComponents.text(context,themes,context.getString(if(rest) R.string.hrv_rest_duration else R.string.hrv_measure_duration))
            val value=BicycleComponents.text(context,themes,"").apply { gravity=Gravity.END;tag=if(rest) "hrv_rest_value" else "hrv_measure_value" }
            addView(LinearLayout(context).apply {
                gravity=Gravity.CENTER_VERTICAL
                addView(label,LayoutParams(0,-2,1f));addView(value,LayoutParams(-2,-2))
            },LayoutParams(-1,-2))
            val slider=SeekBar(context).apply {
                id=generateViewId();tag=if(rest) "hrv_rest_seconds" else "hrv_measure_seconds"
                max=(maximum-minimum)/15;progress=(initial-minimum)/15
                contentDescription=label.text;isEnabled=canEdit();label.labelFor=id
                BicycleComponents.bindTheme(this,themes) {
                    thumbTintList=ColorStateList.valueOf(it.accent);progressTintList=thumbTintList
                    progressBackgroundTintList=ColorStateList.valueOf((it.foreground and 0xffffff) or 0x55000000)
                }
            }
            fun show() {
                val seconds=minimum+slider.progress*15
                value.text=context.getString(R.string.hrv_duration_value,seconds/60,seconds%60)
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
            addView(slider,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.touchSize)).apply { bottomMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        }
        duration(true);duration(false)
    }
}
