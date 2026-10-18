package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RouteQueryTest {
    private val zone=ZoneId.of("Europe/Berlin")
    private fun route(id:String,date:String,type:BikeType?,km:Double?,hours:Double?,imported:Boolean=false)=
        ManagedRoute(id,id,!imported,"FIT",RouteDetails(LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),
            km?.times(1000),movingMs=hours?.times(3600000)?.toLong(),bikeType=type),false,
            if(imported) TrackOrigin.IMPORTED else TrackOrigin.RECORDED)
    private val entries=listOf(route("a","2026-10-18",BikeType.EBIKE,30.0,1.0),route("b","2026-10-19",BikeType.INDOOR,50.0,2.0),
        route("c","2026-10-17",BikeType.BIKE,10.0,1.0,true),route("missing","2026-10-16",null,null,null))
    @Test fun independentGroupsCombineWhileKindsAndBikesAllowMultipleSelectionsAndUnknownAssignments() {
        val q=RouteQuery();q.kinds=setOf(RouteKind.EBIKE,RouteKind.INDOOR)
        q.bikes=setOf(2);assertEquals(listOf("a"),q.apply(entries,{ if(it=="a") 2 else null },zone).map { it.id })
        q.bikes=setOf(-1);assertEquals(listOf("b"),q.apply(entries,{ if(it=="a") 2 else null },zone).map { it.id })
        q.kinds=emptySet();q.bikes=emptySet();q.distance=RouteRange(20.0,60.0);q.moving=RouteRange(.5,1.5);q.speed=RouteRange(25.0,35.0)
        assertEquals(listOf("a"),q.apply(entries,{ null },zone).map { it.id });assertEquals(3,q.activeCount)
    }
    @Test fun localDateRangeIncludesEntireEndDayAndHandlesCalendarBoundary() {
        val q=RouteQuery();q.fromDate=LocalDate.parse("2026-10-17");q.toDate=LocalDate.parse("2026-10-18")
        val late=entries[0].copy(id="late",details=entries[0].details.copy(dateMs=LocalDate.parse("2026-10-19").atStartOfDay(zone).toInstant().toEpochMilli()-1))
        assertEquals(listOf("late","a","c"),q.apply(entries+late,{ null },zone).map { it.id })
    }
    @Test fun eachMetricSortsBothWaysWithUnavailableValuesLastAndResetPreservesSorting() {
        val q=RouteQuery()
        for(sort in RouteSort.entries) for(desc in listOf(true,false)) {
            q.sort=sort;q.descending=desc
            val result=q.apply(entries,{ null },zone)
            val values=result.mapNotNull { sort.value(it) }
            assertEquals(if(desc) values.sortedDescending() else values.sorted(),values)
            if(sort!=RouteSort.DATE) assertEquals("missing",result.last().id)
        }
        q.kinds=setOf(RouteKind.IMPORTED);q.clearFilters();assertEquals(0,q.activeCount)
        assertEquals(RouteSort.SPEED,q.sort);assertFalse(q.descending)
    }
}
