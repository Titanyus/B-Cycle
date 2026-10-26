package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.text.NumberFormat

internal class DreeveMaintenanceView(context:Context,private val themes:ThemeManager,refresh:()->Unit):LinearLayout(context) {
    private val status=BicycleFeedback.status(context,themes).apply { tag="maintenance_status" }
    private val list=LinearLayout(context).apply { orientation=VERTICAL;tag="maintenance_tasks" }
    private val reload=BicycleComponents.action(context,themes,context.getString(R.string.maintenance_refresh),refresh).apply { tag="maintenance_refresh" }
    init {
        orientation=VERTICAL;tag="maintenance_screen"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        addView(BicycleComponents.description(context,themes,context.getString(R.string.maintenance_description)))
        addView(reload);addView(status);addView(list)
        BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
    }
    fun loading() { reload.isEnabled=false;list.removeAllViews();BicycleFeedback.update(status,context.getString(R.string.maintenance_loading)) }
    fun failed(problem:Int) { reload.isEnabled=true;BicycleFeedback.update(status,context.getString(problem)) }
    fun loaded(tasks:List<DreeveMaintenanceTask>,gears:Map<String,String>,gearProblem:Int?=null) {
        reload.isEnabled=true;list.removeAllViews()
        BicycleFeedback.update(status,context.getString(if(tasks.isEmpty()) R.string.maintenance_empty else R.string.maintenance_count,tasks.size)+
            (gearProblem?.let { "\n"+context.getString(R.string.maintenance_gear_warning,context.getString(it)) } ?: ""))
        val number=NumberFormat.getNumberInstance().apply { maximumFractionDigits=1;isGroupingUsed=false }
        fun amount(value:Double,unit:String)=number.format(value)+" "+context.getString(when(unit) {
            "km"->R.string.maintenance_km;"mi"->R.string.maintenance_miles;"hours"->R.string.maintenance_hours
            "days"->R.string.maintenance_days;"daysUsed"->R.string.maintenance_days_used;else->R.string.maintenance_unknown_unit
        })
        tasks.forEachIndexed { index,task ->
            if(index>0) list.addView(BicycleComponents.divider(context,themes),LayoutParams(-1,BicycleDesign.linePixels(context)))
            list.addView(LinearLayout(context).apply {
                orientation=VERTICAL;tag="maintenance_${task.id}"
                val pad=BicycleDesign.dp(context,BicycleDesign.controlGap);setPadding(0,pad,0,pad)
                addView(BicycleComponents.text(context,themes,task.component+" · "+task.label).apply {
                    typeface=BicycleDesign.strongTypeface;textSize=BicycleDesign.bodySp
                })
                addView(BicycleComponents.description(context,themes,context.getString(when {
                    task.overdue->R.string.maintenance_overdue;task.due->R.string.maintenance_due;else->R.string.maintenance_upcoming
                })))
                addView(BicycleComponents.description(context,themes,task.gearIds.takeIf { it.isNotEmpty() }
                    ?.joinToString(" · ") { gears[it] ?: it } ?: context.getString(R.string.maintenance_no_gear)))
                addView(BicycleComponents.description(context,themes,context.getString(R.string.maintenance_interval,amount(task.interval,task.unit))))
                task.remaining?.let { addView(BicycleComponents.description(context,themes,context.getString(
                    if(it<0) R.string.maintenance_past_by else R.string.maintenance_remaining,amount(kotlin.math.abs(it),task.unit)))) }
                task.lastPerformed?.let { addView(BicycleComponents.description(context,themes,context.getString(R.string.maintenance_last,it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))))) }
            },LayoutParams(-1,-2))
        }
    }
}
