package de.bcycle.app

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor

/** Native fast-scroll sections follow the already filtered/sorted list, without changing its order. */
internal class RouteListSections(entries:List<ManagedRoute>,sort:RouteSort,missing:String,
    zone:ZoneId=ZoneId.systemDefault(),locale:Locale=Locale.getDefault()) {
    val labels:Array<String>
    private val starts:IntArray
    init {
        val names=mutableListOf<String>();val positions=mutableListOf<Int>()
        val month=DateTimeFormatter.ofPattern("LLLL yyyy",locale)
        entries.forEachIndexed { index,route ->
            val value=sort.value(route)
            val label=if(value==null) missing else when(sort) {
                RouteSort.DATE->Instant.ofEpochMilli(route.details.dateMs).atZone(zone).format(month)
                RouteSort.DISTANCE->range(value/1000,10,"km",locale)
                RouteSort.MOVING->range(value/3600000,1,"h",locale)
                RouteSort.SPEED->range(value,5,"km/h",locale)
            }
            if(names.lastOrNull()!=label) { names.add(label);positions.add(index) }
        }
        labels=names.toTypedArray();starts=positions.toIntArray()
    }
    private fun range(value:Double,step:Int,unit:String,locale:Locale):String {
        val lower=floor(value/step)*step
        return String.format(locale,"%.0f–%.0f %s",lower,lower+step,unit)
    }
    fun position(section:Int)=if(starts.isEmpty()) 0 else starts[section.coerceIn(0,starts.lastIndex)]
    fun section(position:Int):Int {
        if(starts.isEmpty()) return 0
        val index=starts.binarySearch(position.coerceAtLeast(0))
        return if(index>=0) index else (-index-2).coerceAtLeast(0)
    }
}
