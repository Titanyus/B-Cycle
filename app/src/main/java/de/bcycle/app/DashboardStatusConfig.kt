package de.bcycle.app

import android.content.SharedPreferences

internal enum class DashboardStatusItem(val label:Int,val icon:BicycleIcon) {
    EMPTY(R.string.dashboard_status_empty,BicycleIcon.Cancel),
    TIME(R.string.dashboard_status_time,BicycleIcon.Duration),
    NOTIFICATIONS(R.string.notifications_title,BicycleIcon.Notifications),
    VPN(R.string.dashboard_status_vpn,BicycleIcon.Connection),
    BATTERY(R.string.battery_phone,BicycleIcon.Battery),
    CELLULAR(R.string.dashboard_status_cellular,BicycleIcon.Sensors)
}

/** Six stable positions, left 0..2 and right 3..5. Never store live status values. */
internal data class DashboardStatusConfig(val slots:List<DashboardStatusItem> = List(6) { DashboardStatusItem.EMPTY }) {
    init { require(slots.size==6);require(slots.filter { it!=DashboardStatusItem.EMPTY }.let { it.size==it.distinct().size }) }
    val populated get()=slots.any { it!=DashboardStatusItem.EMPTY }
    fun uses(item:DashboardStatusItem)=item in slots
    fun assign(index:Int,item:DashboardStatusItem):DashboardStatusConfig {
        require(index in slots.indices)
        return copy(slots=slots.mapIndexed { i,old -> when { i==index->item;item!=DashboardStatusItem.EMPTY && old==item->DashboardStatusItem.EMPTY;else->old } })
    }
    fun move(from:Int,to:Int):DashboardStatusConfig {
        require(from in slots.indices && to in slots.indices && from/3==to/3)
        val side=(from/3)*3;val reordered=slots.subList(side,side+3).toMutableList().apply { add(to-side,removeAt(from-side)) }
        return copy(slots=slots.toMutableList().apply { for(i in 0..2) this[side+i]=reordered[i] })
    }
    fun encode()=slots.joinToString(",") { it.name }
    companion object {
        const val key="dashboardStatusSlots"
        fun parse(text:String)=DashboardStatusConfig(text.split(',').map(DashboardStatusItem::valueOf))
        fun read(prefs:SharedPreferences)=runCatching { parse(prefs.getString(key,"")!!) }.getOrDefault(DashboardStatusConfig())
        fun write(prefs:SharedPreferences,value:DashboardStatusConfig) { prefs.edit().putString(key,value.encode()).apply() }
    }
}

internal data class DashboardStatusSnapshot(val time:String="",val vpn:Boolean=false,val battery:Int?=null,val notifications:Int=0,val cellular:Int?=null)
