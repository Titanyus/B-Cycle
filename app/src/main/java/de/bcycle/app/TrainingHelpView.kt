package de.bcycle.app

import android.content.Context
import android.widget.LinearLayout
import android.widget.Switch

/** Shared controls for the quick screen and Design & UI; one authoritative mode. */
internal class TrainingHelpView(context:Context,themes:ThemeManager,initial:DashboardTrainingHelp,
    private val changed:(DashboardTrainingHelp)->Unit):LinearLayout(context) {
    private var config=initial
    private var binding=false
    private val all=Switch(context).apply { tag="training_help_all";setText(R.string.training_help_all) }
    private val zoneTwo=Switch(context).apply { tag="training_help_zone_two";setText(R.string.training_help_zone_two) }
    private val current=Switch(context).apply { tag="training_help_current";setText(R.string.training_help_current) }
    private val average=Switch(context).apply { tag="training_help_average";setText(R.string.training_help_average) }
    init {
        orientation=VERTICAL;tag="training_help_controls"
        addView(BicycleComponents.description(context,themes,context.getString(R.string.training_help_explanation)))
        addView(all);addView(zoneTwo)
        addView(BicycleComponents.sectionDivider(context,themes))
        addView(BicycleComponents.text(context,themes,context.getString(R.string.training_help_for)))
        addView(current);addView(average)
        listOf(all,zoneTwo,current,average).forEach { BicycleDialogStyle.control(it,themes) }
        fun update(value:DashboardTrainingHelp) { show(value);changed(value) }
        all.setOnCheckedChangeListener { _,on -> if(!binding) update(config.copy(mode=if(on) TrainingHelpMode.ALL_ZONES else TrainingHelpMode.OFF)) }
        zoneTwo.setOnCheckedChangeListener { _,on -> if(!binding) update(config.copy(mode=if(on) TrainingHelpMode.ZONE_TWO else TrainingHelpMode.OFF)) }
        current.setOnCheckedChangeListener { _,on -> if(!binding) update(config.copy(current=on)) }
        average.setOnCheckedChangeListener { _,on -> if(!binding) update(config.copy(average=on)) }
        show(initial)
    }
    fun show(value:DashboardTrainingHelp) {
        binding=true;config=value
        all.isChecked=value.mode==TrainingHelpMode.ALL_ZONES;zoneTwo.isChecked=value.mode==TrainingHelpMode.ZONE_TWO
        current.isChecked=value.current;average.isChecked=value.average;binding=false
    }
}
