package de.bcycle.app

import java.time.LocalDate
import java.time.ZoneId

internal enum class RouteKind(val label:Int) {
    IMPORTED(R.string.ui_routes_filter_imported),BIKE(R.string.bikes_type_bike),EBIKE(R.string.bikes_type_ebike),INDOOR(R.string.bikes_type_indoor);
    companion object {
        fun of(route:ManagedRoute)=if(route.origin==TrackOrigin.IMPORTED) IMPORTED else when(route.details.bikeType) {
            BikeType.BIKE->BIKE;BikeType.EBIKE->EBIKE;BikeType.INDOOR->INDOOR;null->null
        }
    }
}
internal enum class RouteSort(val label:Int) {
    DATE(R.string.route_filter_date),DISTANCE(R.string.route_filter_distance),MOVING(R.string.route_filter_moving),SPEED(R.string.route_filter_speed);
    fun value(route:ManagedRoute):Double?=when(this) {
        DATE->route.details.dateMs.takeIf { it>0 }?.toDouble()
        DISTANCE->route.details.distanceM
        MOVING->route.details.movingMs?.toDouble()
        SPEED->route.details.movingAverageKmh
    }?.takeIf { it.isFinite() && it>=0 }
}
internal data class RouteRange(val from:Double?=null,val to:Double?=null) {
    val active get()=from!=null || to!=null
    fun accepts(value:Double?):Boolean = !active || (value!=null && value.isFinite() &&
        (from==null || value>=from) && (to==null || value<=to))
}
internal class RouteQuery {
    var kinds=setOf<RouteKind>();var bikes=setOf<Int>() // -1 = no recorded profile assignment
    var fromDate:LocalDate?=null;var toDate:LocalDate?=null
    var distance=RouteRange();var moving=RouteRange();var speed=RouteRange()
    var sort=RouteSort.DATE;var descending=true
    val activeCount get()=listOf(kinds.isNotEmpty(),bikes.isNotEmpty(),fromDate!=null || toDate!=null,distance.active,moving.active,speed.active).count { it }
    fun clearFilters() { kinds=emptySet();bikes=emptySet();fromDate=null;toDate=null;distance=RouteRange();moving=RouteRange();speed=RouteRange() }
    fun apply(entries:List<ManagedRoute>,profile:(String)->Int?,zone:ZoneId=ZoneId.systemDefault()):List<ManagedRoute> {
        val from=fromDate?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
        val until=toDate?.plusDays(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
        return entries.filter { route ->
            (kinds.isEmpty() || RouteKind.of(route) in kinds) &&
                (bikes.isEmpty() || (profile(route.id) ?: -1) in bikes) &&
                ((from==null && until==null) || (route.details.dateMs>0 && (from==null || route.details.dateMs>=from) && (until==null || route.details.dateMs<until))) &&
                distance.accepts(route.details.distanceM?.div(1000)) && moving.accepts(route.details.movingMs?.div(3600000.0)) && speed.accepts(route.details.movingAverageKmh)
        }.sortedWith { a,b ->
            val av=sort.value(a);val bv=sort.value(b)
            val order=when { av==null && bv==null->0;av==null->1;bv==null->-1;else->av.compareTo(bv)*(if(descending) -1 else 1) }
            if(order!=0) order else compareValues(b.details.dateMs,a.details.dateMs).takeIf { it!=0 } ?: a.id.compareTo(b.id)
        }
    }
}
