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
    @Test fun displayedFilterUsesActualVisibleRouteCombinesWithOthersAndResets() {
        val q=RouteQuery();q.displayedOnly=true
        assertEquals(listOf("b"),q.apply(entries,{null},zone,displayedId="b").map { it.id })
        assertTrue(q.apply(entries,{null},zone,displayedId=null).isEmpty())
        q.bikeTypes=setOf(BikeType.EBIKE)
        assertTrue(q.apply(entries,{null},zone,displayedId="b").isEmpty())
        q.bikeTypes=emptySet()
        assertEquals(setOf(BikeType.INDOOR),q.availability(entries,{null},zone,displayedId="b").bikeTypes)
        assertEquals(1,q.activeCount)
        q.sort=RouteSort.DISTANCE;q.clearFilters()
        assertFalse(q.displayedOnly);assertEquals(0,q.activeCount);assertEquals(RouteSort.DISTANCE,q.sort)
        assertEquals(entries.size,q.apply(entries,{null},zone).size)
    }
    @Test fun independentGroupsCombineWhileKindsAndBikesAllowMultipleSelectionsAndUnknownAssignments() {
        val q=RouteQuery();q.bikeTypes=setOf(BikeType.EBIKE,BikeType.INDOOR)
        q.bikes=setOf(2);assertEquals(listOf("a"),q.apply(entries,{ if(it=="a") 2 else null },zone).map { it.id })
        q.bikes=setOf(-1);assertEquals(listOf("b"),q.apply(entries,{ if(it=="a") 2 else null },zone).map { it.id })
        q.kinds=emptySet();q.bikeTypes=emptySet();q.bikes=emptySet();q.distance=RouteRange(20.0,60.0);q.moving=RouteRange(.5,1.5);q.speed=RouteRange(25.0,35.0)
        assertEquals(listOf("a"),q.apply(entries,{ null },zone).map { it.id });assertEquals(3,q.activeCount)
    }
    @Test fun originAndBikeTypeAreIndependentAndRecordedIncludesLegacyRides() {
        val q=RouteQuery()
        q.kinds=setOf(RouteKind.RECORDED)
        assertEquals(listOf("b","a","missing"),q.apply(entries,{ null },zone).map { it.id })
        q.bikeTypes=setOf(BikeType.BIKE)
        assertTrue(q.apply(entries,{ null },zone).isEmpty())
        q.kinds=setOf(RouteKind.IMPORTED,RouteKind.RECORDED)
        assertEquals(listOf("c"),q.apply(entries,{ null },zone).map { it.id })
        q.kinds=setOf(RouteKind.IMPORTED)
        assertEquals(listOf("c"),q.apply(entries,{ null },zone).map { it.id })
        assertEquals(2,q.activeCount)
        q.clearFilters();assertTrue(q.bikeTypes.isEmpty());assertEquals(0,q.activeCount)
    }
    @Test fun availabilityUsesOtherGroupsAndIgnoresItsOwnMultiSelection() {
        val q=RouteQuery();val profile:(String)->Int?={ if(it=="a") 0 else if(it=="b") 1 else null }
        q.kinds=setOf(RouteKind.IMPORTED)
        assertEquals(setOf(BikeType.BIKE),q.availability(entries,profile,zone).bikeTypes)
        assertEquals(setOf(-1),q.availability(entries,profile,zone).bikes)
        assertEquals(setOf(RouteKind.IMPORTED,RouteKind.RECORDED),q.availability(entries,profile,zone).kinds)
        q.kinds=setOf(RouteKind.RECORDED);q.bikeTypes=setOf(BikeType.EBIKE)
        assertEquals(setOf(BikeType.EBIKE,BikeType.INDOOR),q.availability(entries,profile,zone).bikeTypes)
        assertEquals(setOf(0),q.availability(entries,profile,zone).bikes)
        q.bikes=setOf(1)
        assertEquals(setOf(BikeType.INDOOR),q.availability(entries,profile,zone).bikeTypes)
        assertEquals(setOf(0),q.availability(entries,profile,zone).bikes)
        assertEquals(setOf(1),q.bikes);assertEquals(setOf(BikeType.EBIKE),q.bikeTypes)
    }
    @Test fun dateAndRangesConstrainAvailabilityWithoutMutatingTheQuery() {
        val q=RouteQuery();q.fromDate=LocalDate.parse("2026-10-19")
        assertEquals(setOf(BikeType.INDOOR),q.availability(entries,{ null },zone).bikeTypes)
        q.distance=RouteRange(100.0,null)
        val none=q.availability(entries,{ null },zone)
        assertTrue(none.kinds.isEmpty());assertTrue(none.bikeTypes.isEmpty());assertTrue(none.bikes.isEmpty())
        assertTrue(q.availability(emptyList(),{ null },zone).kinds.isEmpty())
        assertEquals(LocalDate.parse("2026-10-19"),q.fromDate);assertEquals(RouteRange(100.0,null),q.distance)
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
