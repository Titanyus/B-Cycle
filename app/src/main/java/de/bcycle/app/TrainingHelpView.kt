package de.bcycle.app

import android.content.Context
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.RadioGroup
import android.view.View

/** Same zone indicator and reminder group in profile settings and field selection. */
internal class TrainingHelpView(context:Context,private val themes:ThemeManager,initial:DashboardTrainingHelp,
    private val changed:(DashboardTrainingHelp)->Unit):LinearLayout(context) {
    private var config=initial
    private var binding=false
    private var lastMode=initial.mode.takeUnless { it==TrainingHelpMode.OFF } ?: TrainingHelpMode.ALL_ZONES
    var revealAnchor:View?=null
    private val box=BicycleGroupedBox(context,themes,standalone=true)
    private val enabled=Switch(context).apply { tag="training_help_enabled";setText(R.string.training_help_zone_indicator);BicycleDialogStyle.control(this,themes) }
    private val modes=RadioGroup(context).apply { orientation=HORIZONTAL;tag="training_help_modes" }
    private val modeButtons=TrainingHelpMode.entries.associateWith { mode ->
        BicycleSegmentButton(context,themes).apply {
            id=generateViewId()
            tag=when(mode) { TrainingHelpMode.OFF->"training_help_off";TrainingHelpMode.ALL_ZONES->"training_help_all";TrainingHelpMode.ZONE_TWO->"training_help_zone_two" }
            setText(when(mode) { TrainingHelpMode.OFF->R.string.zone_glow_off;TrainingHelpMode.ALL_ZONES->R.string.training_help_all_short;TrainingHelpMode.ZONE_TWO->R.string.training_help_zone_two_short })
        }
    }
    private val current=BicycleSegmentButton(context,themes,multiple=true).apply { tag="training_help_current";setText(R.string.training_help_current) }
    private val average=BicycleSegmentButton(context,themes,multiple=true).apply { tag="training_help_average";setText(R.string.training_help_average) }
    init {
        orientation=VERTICAL;tag="training_help_controls"
        box.tag="training_help_group";addView(box,LayoutParams(-1,-2))
        val frame=BicycleFrameDropdown(context,themes,context.getString(R.string.training_help_zone_indicator),open=false,
            trailing=enabled,leadingIcon=BicycleIcon.Training) { open ->
            if(open) BicycleContentSpacing.revealAfterLayout(revealAnchor ?: this@TrainingHelpView)
        }
        frame.tag="training_help_zone_frame";frame.expandControl.tag="training_help_expand"
        val details=frame.content.apply { tag="training_help_details" }
        BicycleComponents.segmentedControlRow(modes,themes)
        modeButtons.values.forEach { modes.addView(it,RadioGroup.LayoutParams(0,-2,1f)) }
        details.addView(modes,LayoutParams(-1,-2))
        details.addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.training_help_for)))
        details.addView(LinearLayout(context).apply {
            orientation=HORIZONTAL
            addView(current,LayoutParams(0,-2,1f));addView(average,LayoutParams(0,-2,1f))
            BicycleComponents.segmentedControlRow(this,themes)
        },LayoutParams(-1,-2))
        details.addView(BicycleComponents.description(context,themes,context.getString(R.string.training_help_explanation)).apply { tag="training_help_explanation" },LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        box.addEntry(frame)
        fun update(value:DashboardTrainingHelp) { display(value);changed(value) }
        enabled.setOnCheckedChangeListener { _,on -> if(!binding) update(config.copy(mode=if(on) lastMode else TrainingHelpMode.OFF)) }
        modes.setOnCheckedChangeListener { _,id -> if(!binding) {
            modeButtons.entries.firstOrNull { it.value.id==id }?.let { update(config.copy(mode=it.key)) }
        } }
        current.setOnCheckedChangeListener { _,on -> if(!binding) update(config.copy(current=on)) }
        average.setOnCheckedChangeListener { _,on -> if(!binding) update(config.copy(average=on)) }
        show(initial)
    }
    fun addDrinkReminder(view:DrinkReminderSettingsView) {
        view.frame.grouped=true
        box.addEntry(view)
    }
    fun show(value:DashboardTrainingHelp) {
        lastMode=value.mode.takeUnless { it==TrainingHelpMode.OFF } ?: TrainingHelpMode.ALL_ZONES
        display(value)
    }
    private fun display(value:DashboardTrainingHelp) {
        binding=true;config=value
        if(value.mode!=TrainingHelpMode.OFF) lastMode=value.mode
        enabled.isChecked=value.mode!=TrainingHelpMode.OFF
        modes.check(modeButtons.getValue(value.mode).id)
        current.isChecked=value.current;average.isChecked=value.average;binding=false
    }
}
