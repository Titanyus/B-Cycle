package de.bcycle.app

import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor

internal data class RouteListGroup(val label:String,val first:Int,val count:Int,
    val distanceM:Double?,val movingMs:Double?,val averageKmh:Double?)

/** Prepared once from filtered/sorted rows. Headers and fast navigation use the same boundaries. */
internal class RouteListSections(entries:List<ManagedRoute>,sort:RouteSort,missing:String,
    zone:ZoneId=ZoneId.systemDefault(),locale:Locale=Locale.getDefault(),
    title:(RouteSort,Double)->String={ kind,upper ->
        val unit=when(kind) { RouteSort.DISTANCE->"km";RouteSort.MOVING->if(upper<1) "min" else "h";else->"km/h" }
        val number=if(kind==RouteSort.MOVING && upper<1) upper*60 else upper
        String.format(locale,if(number%1.0==0.0) "%.0f %s" else "%.1f %s",number,unit)
    }) {
    val labels:Array<String>
    val groups:List<RouteListGroup>
    private val starts:IntArray
    init {
        val names=mutableListOf<String>();val positions=mutableListOf<Int>()
        val month=DateTimeFormatter.ofPattern("LLLL yyyy",locale)
        var previous:Any?=null
        entries.forEachIndexed { index,route ->
            val value=sort.value(route)
            val key:Any=when {
                value==null->"missing"
                sort==RouteSort.DATE->YearMonth.from(Instant.ofEpochMilli(route.details.dateMs).atZone(zone))
                else->upper(sort,value)
            }
            if(key!=previous) {
                val label=when(key) {
                    is YearMonth->key.atDay(1).format(month)
                    is Double->title(sort,key)
                    else->missing
                }
                names.add(label);positions.add(index);previous=key
            }
        }
        labels=names.toTypedArray();starts=positions.toIntArray()
        groups=starts.indices.map { index -> summarize(labels[index],starts[index],entries.subList(starts[index],starts.getOrNull(index+1) ?: entries.size)) }
    }
    fun groupAt(position:Int):RouteListGroup?=starts.binarySearch(position).takeIf { it>=0 }?.let(groups::get)
    fun position(section:Int)=if(starts.isEmpty()) 0 else starts[section.coerceIn(0,starts.lastIndex)]
    fun section(position:Int):Int {
        if(starts.isEmpty()) return 0
        val index=starts.binarySearch(position.coerceAtLeast(0))
        return if(index>=0) index else (-index-2).coerceAtLeast(0)
    }
    companion object {
        /** Upper bounds are exclusive, including exact threshold values in the following group. */
        internal fun upper(sort:RouteSort,value:Double):Double=when(sort) {
            RouteSort.DISTANCE->{
                val km=value/1000
                listOf(10.0,25.0,50.0,75.0,100.0,125.0,150.0,200.0).firstOrNull { km<it }
                    ?: (200+(floor((km-200)/50)+1)*50)
            }
            RouteSort.MOVING->{
                val hours=value/3600000
                listOf(.5,1.0,1.5,2.0).firstOrNull { hours<it } ?: floor(hours)+1
            }
            RouteSort.SPEED->if(value<20) 20.0 else 20+(floor((value-20)/5)+1)*5
            RouteSort.DATE->error("Date groups use YearMonth")
        }
        private fun summarize(label:String,first:Int,rows:List<ManagedRoute>):RouteListGroup {
            var distance=0.0;var distanceCount=0
            var moving=0.0;var movingCount=0
            var pairDistance=0.0;var pairMoving=0.0
            rows.forEach { row ->
                val d=row.details.distanceM?.takeIf { it.isFinite() && it>=0 }
                val t=row.details.movingMs?.takeIf { it>=0 && (row.details.durationMs==null || it<=row.details.durationMs) }?.toDouble()
                if(d!=null) { distance+=d;distanceCount++ }
                if(t!=null) { moving+=t;movingCount++ }
                if(d!=null && t!=null && t>0) { pairDistance+=d;pairMoving+=t }
            }
            return RouteListGroup(label,first,rows.size,distance.takeIf { distanceCount>0 && it.isFinite() },
                moving.takeIf { movingCount>0 && it.isFinite() },
                (pairDistance*3600/pairMoving).takeIf { pairMoving>0 && it.isFinite() })
        }
    }
}
