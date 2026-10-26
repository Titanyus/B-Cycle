package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.text.NumberFormat

internal class DreeveMaintenanceView(context:Context,private val themes:ThemeManager,refresh:()->Unit):LinearLayout(context) {
    private val status=BicycleFeedback.status(context,themes).apply { tag="maintenance_status" }
    private val list=LinearLayout(context).apply {
        orientation=VERTICAL;tag="maintenance_tasks"
        val pad=dp(BicycleDesign.horizontalPadding);setPadding(pad,0,pad,0)
    }
    private val reload=BicycleComponents.action(context,themes,context.getString(R.string.maintenance_refresh),refresh).apply { tag="maintenance_refresh" }
    private val expandedBikes=mutableMapOf<String?,Boolean>()
    private val expandedParts=mutableMapOf<Pair<String?,String>,Boolean>()
    private val number=NumberFormat.getNumberInstance().apply { maximumFractionDigits=1;isGroupingUsed=false }
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    init {
        orientation=VERTICAL;tag="maintenance_screen";clipToPadding=false
        setPadding(0,0,0,dp(BicycleDesign.horizontalPadding))
        addView(LinearLayout(context).apply {
            orientation=VERTICAL;val pad=dp(BicycleDesign.horizontalPadding);setPadding(pad,0,pad,0)
            addView(reload);addView(status)
        })
        addView(list)
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
    }
    fun loading() { reload.isEnabled=false;list.removeAllViews();BicycleFeedback.update(status,context.getString(R.string.maintenance_loading)) }
    fun failed(problem:Int) { reload.isEnabled=true;BicycleFeedback.update(status,context.getString(problem)) }
    fun loaded(tasks:List<DreeveMaintenanceTask>,gears:Map<String,String>,gearProblem:Int?=null) {
        reload.isEnabled=true;list.removeAllViews()
        BicycleFeedback.update(status,context.getString(if(tasks.isEmpty()) R.string.maintenance_empty else R.string.maintenance_count,tasks.size)+
            (gearProblem?.let { "\n"+context.getString(R.string.maintenance_gear_warning,context.getString(it)) } ?: ""))
        val groups=MaintenanceGrouping.bikes(tasks)
        expandedBikes.keys.retainAll(groups.keys)
        val activeParts=mutableSetOf<Pair<String?,String>>()
        groups.forEach { (gear,tasks) ->
            val body=LinearLayout(context).apply { orientation=VERTICAL;tag="maintenance_bike_body_$gear" }
            val header=BicycleSectionHeader(context,themes,if(gear==null) context.getString(R.string.maintenance_no_gear) else gears[gear] ?: gear).apply {
                tag="maintenance_bike_$gear"
            }
            list.addView(header,LayoutParams(-1,-2).apply { marginStart=-list.paddingLeft;marginEnd=-list.paddingRight })
            list.addView(body,LayoutParams(-1,-2))
            header.accordion(body,expandedBikes.getOrPut(gear) { true }) { expandedBikes[gear]=it }
            MaintenanceGrouping.components(tasks).forEach { (component,partTasks) ->
                val key=gear to component;activeParts+=key
                val content=LinearLayout(context).apply { orientation=VERTICAL;tag="maintenance_component_body_${gear}_$component" }
                val toggle=BicycleComponents.action(context,themes,partTasks.first().component) {}.apply {
                    tag="maintenance_component_${gear}_$component"
                }
                BicycleDialogStyle.accordion(toggle,themes);BicycleDialogStyle.actionIcon(toggle,themes,BicycleIcon.Expand)
                fun show(open:Boolean) {
                    expandedParts[key]=open;content.visibility=if(open) VISIBLE else GONE
                    toggle.stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
                }
                toggle.setOnClickListener { show(content.visibility!=VISIBLE) };show(expandedParts.getOrPut(key) { true })
                body.addView(toggle);BicycleComponents.controlSpacing(toggle)
                partTasks.forEachIndexed { index,task ->
                    if(index>0) content.addView(BicycleComponents.sectionDivider(context,themes,BicycleDesign.controlGap))
                    content.addView(taskRow(task),LayoutParams(-1,-2))
                }
                body.addView(content,LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap);bottomMargin=dp(BicycleDesign.controlGap) })
            }
            val end=View(context);list.addView(end,LayoutParams(-1,0));header.sectionEnd=end
        }
        expandedParts.keys.retainAll(activeParts)
        requestLayout()
    }
    private fun amount(value:Double,unit:String)=number.format(value)+" "+context.getString(when(unit) {
        "km"->R.string.maintenance_km;"mi"->R.string.maintenance_miles;"hours"->R.string.maintenance_hours
        "days"->R.string.maintenance_days;"daysUsed"->R.string.maintenance_days_used;else->R.string.maintenance_unknown_unit
    })
    private fun taskRow(task:DreeveMaintenanceTask)=LinearLayout(context).apply {
        orientation=VERTICAL;tag="maintenance_${task.id}"
        stateDescription=context.getString(when { task.overdue->R.string.maintenance_overdue;task.due->R.string.maintenance_due;else->R.string.maintenance_upcoming })
        val pad=dp(BicycleDesign.controlGap);setPadding(0,pad,0,pad)
        addView(LinearLayout(context).apply {
            tag=BicycleDesign.inlineControlRowTag;gravity=Gravity.CENTER_VERTICAL;isBaselineAligned=false
            addView(BicycleComponents.text(context,themes,context.getString(R.string.maintenance_every,task.label,amount(task.interval,task.unit))).apply {
                typeface=BicycleDesign.strongTypeface
            },LayoutParams(0,-2,1f).apply { marginEnd=pad })
            addView(BicycleComponents.action(context,themes,actionLabel(task.label)) {}.apply {
                tag="maintenance_action_${task.id}";isEnabled=false
                tooltipText=context.getString(R.string.maintenance_action_unavailable)
                contentDescription=context.getString(R.string.maintenance_action_for,task.label,context.getString(R.string.maintenance_action_unavailable))
            },LayoutParams(-2,-2))
        })
        addView(BicycleComponents.description(context,themes,context.getString(R.string.maintenance_last,
            task.lastPerformed?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) ?: context.getString(R.string.maintenance_never))))
        if(task.gearIds.size>1) addView(BicycleComponents.description(context,themes,context.getString(R.string.maintenance_shared)))
        if(task.due && !task.overdue) addView(BicycleComponents.description(context,themes,context.getString(R.string.maintenance_due)))
        addView(LinearLayout(context).apply {
            gravity=Gravity.BOTTOM
            fun metric(text:String,align:Int)=BicycleComponents.text(context,themes,text).apply {
                gravity=align;BicycleComponents.bindTheme(this,themes) { setTextColor(if(task.overdue) MaintenancePalette.red else MaintenancePalette.green) }
            }
            addView(metric(task.elapsed?.let { amount(it,task.unit) } ?: context.getString(R.string.analysis_missing),Gravity.START),LayoutParams(0,-2,1f))
            val remaining=task.remaining?.let { context.getString(if(it<0) R.string.maintenance_overdue_amount else R.string.maintenance_remaining_amount,amount(kotlin.math.abs(it),task.unit)) }
                ?: context.getString(when { task.overdue->R.string.maintenance_overdue;task.due->R.string.maintenance_due;else->R.string.analysis_missing })
            addView(metric(remaining,Gravity.END),LayoutParams(0,-2,1f))
        },LayoutParams(-1,-2).apply { topMargin=pad })
        addView(MaintenanceProgress(context,themes,task),LayoutParams(-1,dp(BicycleDesign.analysisZoneBarHeight+2*BicycleDesign.smallGap)))
        if(task.overdue) addView(BicycleComponents.description(context,themes,context.getString(R.string.maintenance_target,amount(task.interval,task.unit))))
    }
    private fun actionLabel(label:String)=context.getString(when(label.trim().lowercase(java.util.Locale.ROOT)) {
        "messen","measure"->R.string.maintenance_measured
        "tauschen","replace"->R.string.maintenance_replaced
        "wechseln","change"->R.string.maintenance_changed_part
        "reinigen","clean"->R.string.maintenance_cleaned
        "schmieren","lube","lubricate"->R.string.maintenance_lubricated
        "laden","charge"->R.string.maintenance_charged
        else->R.string.maintenance_done
    })
}
