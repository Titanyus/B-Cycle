package de.bcycle.app

import android.content.Context
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.RadioGroup
import android.view.View
import android.view.Gravity

/** Shared controls for the quick screen and Design & UI; one authoritative mode. */
internal class TrainingHelpView(context:Context,private val themes:ThemeManager,initial:DashboardTrainingHelp,
    private val changed:(DashboardTrainingHelp)->Unit):LinearLayout(context) {
    private var config=initial
    private var binding=false
    var revealAnchor:View?=null
    private val modes=RadioGroup(context).apply { orientation=HORIZONTAL;tag="training_help_modes" }
    private val modeButtons=TrainingHelpMode.entries.associateWith { mode ->
        BicycleSegmentButton(context,themes).apply {
            id=generateViewId()
            tag=when(mode) { TrainingHelpMode.OFF->"training_help_off";TrainingHelpMode.ALL_ZONES->"training_help_all";TrainingHelpMode.ZONE_TWO->"training_help_zone_two" }
            setText(when(mode) { TrainingHelpMode.OFF->R.string.zone_glow_off;TrainingHelpMode.ALL_ZONES->R.string.training_help_all_short;TrainingHelpMode.ZONE_TWO->R.string.training_help_zone_two_short })
        }
    }
    private val current=Switch(context).apply { tag="training_help_current";setText(R.string.training_help_current) }
    private val average=Switch(context).apply { tag="training_help_average";setText(R.string.training_help_average) }
    init {
        orientation=VERTICAL;tag="training_help_controls"
        addView(BicycleComponents.description(context,themes,context.getString(R.string.training_help_explanation)))
        BicycleComponents.segmentedControlRow(modes,themes)
        modeButtons.values.forEach { modes.addView(it,RadioGroup.LayoutParams(0,-2,1f)) }
        val details=LinearLayout(context).apply {
            orientation=VERTICAL;tag="training_help_details";visibility=GONE
            addView(BicycleComponents.sectionDivider(context,themes))
            addView(BicycleComponents.text(context,themes,context.getString(R.string.training_help_for)))
            addView(current);addView(average)
        }
        val expand=BicycleComponents.icon(context,themes,BicycleIcon.Expand,context.getString(R.string.training_help_details)) {
            val open=details.visibility!=VISIBLE
            details.visibility=if(open) VISIBLE else GONE
            if(open) BicycleContentSpacing.revealAfterLayout(revealAnchor ?: this@TrainingHelpView)
            findViewWithTag<View>("training_help_expand").apply {
                rotation=if(open) 180f else 0f
                stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
            }
        }.apply { tag="training_help_expand";stateDescription=context.getString(R.string.zone_collapsed) }
        addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;tag=BicycleDesign.inlineControlRowTag
            addView(modes,LayoutParams(0,-2,1f))
            addView(expand,LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize)).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        },LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(details,LayoutParams(-1,-2))
        listOf(current,average).forEach { BicycleDialogStyle.control(it,themes) }
        fun update(value:DashboardTrainingHelp) { show(value);changed(value) }
        modes.setOnCheckedChangeListener { _,id -> if(!binding) {
            modeButtons.entries.firstOrNull { it.value.id==id }?.let { update(config.copy(mode=it.key)) }
        } }
        current.setOnCheckedChangeListener { _,on -> if(!binding) update(config.copy(current=on)) }
        average.setOnCheckedChangeListener { _,on -> if(!binding) update(config.copy(average=on)) }
        show(initial)
    }
    fun addDrinkReminder(view:DrinkReminderSettingsView) {
        addView(BicycleComponents.sectionDivider(context,themes))
        addView(view,LayoutParams(-1,-2))
    }
    fun show(value:DashboardTrainingHelp) {
        binding=true;config=value
        modes.check(modeButtons.getValue(value.mode).id)
        current.isChecked=value.current;average.isChecked=value.average;binding=false
    }
}
