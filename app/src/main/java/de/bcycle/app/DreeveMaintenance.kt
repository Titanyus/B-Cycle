package de.bcycle.app

import org.json.JSONObject
import java.time.LocalDate

/** Read-only Dreeve API snapshot. Progress and due flags are authoritative server values. */
internal data class DreeveMaintenanceTask(val id:String,val label:String,val component:String,val gearIds:List<String>,
    val interval:Double,val unit:String,val lastPerformed:LocalDate?,val elapsed:Double?,val remaining:Double?,
    val percentage:Double?,val due:Boolean,val overdue:Boolean) {
    companion object {
        fun decode(response:JSONObject):List<DreeveMaintenanceTask> = try {
            val rows=response.getJSONArray("maintenanceTasks")
            require(rows.length()<=10000)
            val tasks=(0 until rows.length()).map { index ->
                val row=rows.getJSONObject(index);val interval=row.getJSONObject("interval")
                fun number(key:String)=if(row.isNull(key)) null else row.getDouble(key).also { require(it.isFinite()) }
                val gears=row.getJSONArray("gearIds")
                require(gears.length()<=1000)
                val value=interval.getDouble("value");require(value.isFinite() && value>0)
                DreeveMaintenanceTask(row.getString("id").also { require(it.isNotBlank() && it.length<=512) },
                    row.getString("label").take(500),row.getJSONObject("component").getString("label").take(500),
                    (0 until gears.length()).map { gears.getString(it).also { id -> require(id.isNotBlank() && id.length<=512) } },
                    value,interval.getString("unit").take(64),if(row.isNull("lastPerformedOn")) null else LocalDate.parse(row.getString("lastPerformedOn")),
                    number("elapsed"),number("remaining"),number("percentage"),row.getBoolean("isDue"),row.getBoolean("isOverdue"))
            }
            require(tasks.map { it.id }.distinct().size==tasks.size)
            tasks // Dreeve already sorts most urgent first; never recompute due dates locally.
        } catch(_:Exception) { throw ActivityServiceFailure(false,R.string.service_response_failed) }
    }
}
