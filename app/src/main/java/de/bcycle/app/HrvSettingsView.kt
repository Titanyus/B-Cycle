package de.bcycle.app

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.view.View
import android.widget.FrameLayout

internal class HrvSettingsView(context:Context,themes:ThemeManager,openMeasurement:()->Unit={},canEdit:()->Boolean={true},openAnalysis:()->Unit={}):LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="hrv_settings"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        val settings=HrvSettings(context)
        val open=BicycleComponents.textAction(context,themes,context.getString(R.string.hrv_measurement_open),openMeasurement).apply {
            tag="hrv_open_measurement";typeface=BicycleDesign.strongTypeface
            setPadding(pad,0,pad,0);compoundDrawablePadding=BicycleDesign.dp(context,BicycleDesign.controlGap)
        }
        BicycleComponents.bindTheme(open,themes) { tokens ->
            open.setCompoundDrawablesRelativeWithIntrinsicBounds(BicycleIcons.drawable(context,BicycleIcon.Heart,tokens.primary),null,null,null)
            styleHrvPill(open)
        }
        addView(FrameLayout(context).apply { addView(open,FrameLayout.LayoutParams(-2,-2,Gravity.CENTER)) },LayoutParams(-1,-2).apply { bottomMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        val analysis=BicycleComponents.textAction(context,themes,context.getString(R.string.hrvt_title),openAnalysis).apply {
            tag="hrv_open_analysis";setPadding(pad,0,pad,0)
            BicycleComponents.bindTheme(this,themes) { styleHrvPill(this) }
        }
        addView(FrameLayout(context).apply { addView(analysis,FrameLayout.LayoutParams(-2,-2,Gravity.CENTER)) },LayoutParams(-1,-2).apply { bottomMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        val hint=LinearLayout(context).apply {
            orientation=VERTICAL;tag="hrv_first_start_details";visibility=GONE
            addView(BicycleComponents.description(context,themes,context.getString(R.string.hrv_first_start_hint)))
        }
        val first=Switch(context).apply {
            tag="hrv_first_start";setText(R.string.hrv_first_start);isChecked=settings.read().showOnFirstStart
            BicycleDialogStyle.control(this,themes)
            setOnCheckedChangeListener { _,on -> settings.write(settings.read().copy(showOnFirstStart=on)) }
        }
        val expand=BicycleComponents.icon(context,themes,BicycleIcon.Expand,context.getString(R.string.hrv_first_start)) {
            val shown=hint.visibility!=VISIBLE;hint.visibility=if(shown) VISIBLE else GONE
            findViewWithTag<View>("hrv_first_start_expand").apply {
                rotation=if(shown) 180f else 0f
                stateDescription=context.getString(if(shown) R.string.zone_expanded else R.string.zone_collapsed)
            }
        }.apply { tag="hrv_first_start_expand";stateDescription=context.getString(R.string.zone_collapsed) }
        addView(LinearLayout(context).apply { BicycleComponents.switchAccordion(this,first,expand,themes,icon=BicycleIcon.Heart) },
            LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.sectionHeaderMinHeight)+2*BicycleDesign.linePixels(context,BicycleDesign.sectionRuleDp)))
        addView(hint,LayoutParams(-1,-2))
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
                    thumbTintList=ColorStateList.valueOf(it.primary);progressTintList=thumbTintList
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
        addView(BicycleComponents.sectionDivider(context,themes))
        fun signal(vibrate:Boolean) {
            addView(Switch(context).apply {
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
        val heading=BicycleForm.section(this,themes,R.string.hrvq_title)
        val questions=HrvQuestionSettingsView(context,themes)
        addView(questions,LayoutParams(-1,-2));heading.accordion(questions,false)
    }
}
