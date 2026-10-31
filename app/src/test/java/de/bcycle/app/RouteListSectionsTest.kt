package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class RouteListSectionsTest {
    private val zone=ZoneId.of("Europe/Berlin")
    private fun route(id:String,date:String,km:Double?=10.0,hours:Double?=1.0)=ManagedRoute(id,id,true,"FIT",
        RouteDetails(LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),km?.times(1000),movingMs=hours?.times(3600000)?.toLong()),false)
    @Test fun monthAndYearSectionsFollowVisibleOrderAndPointToTheirFirstRide() {
        val routes=listOf(route("a","2026-03-15"),route("b","2026-03-01"),route("c","2026-01-01"),route("d","2025-03-01"))
        val index=RouteListSections(routes,RouteSort.DATE,"—",zone,Locale.GERMAN)
        assertArrayEquals(arrayOf("März 2026","Januar 2026","März 2025"),index.labels)
        assertEquals(0,index.position(0));assertEquals(2,index.position(1));assertEquals(3,index.position(2))
        assertEquals(0,index.section(1));assertEquals(1,index.section(2));assertEquals(2,index.section(3))
        val ascending=RouteListSections(routes.reversed(),RouteSort.DATE,"—",zone,Locale.GERMAN)
        assertEquals("März 2025",ascending.labels.first());assertEquals(2,ascending.position(2))
        val filtered=RouteListSections(routes.takeLast(2),RouteSort.DATE,"—",zone,Locale.GERMAN)
        assertEquals(1,filtered.position(1))
    }
    @Test fun numericSortsUseTheirOwnRangesAndUnknownValuesRemainNavigable() {
        val routes=listOf(route("a","2026-01-01",95.0,2.0),route("b","2026-01-01",75.0,2.0),route("c","2026-01-01",72.0,2.0),route("missing","2026-01-01",null,null))
        val distance=RouteListSections(routes,RouteSort.DISTANCE,"—",zone,Locale.GERMAN)
        assertArrayEquals(arrayOf("100 km","75 km","—"),distance.labels)
        assertEquals(2,distance.position(1));assertEquals(1,distance.section(2))
        assertArrayEquals(arrayOf("3 h","—"),RouteListSections(routes,RouteSort.MOVING,"—",zone,Locale.GERMAN).labels)
        assertArrayEquals(arrayOf("50 km/h","40 km/h","—"),RouteListSections(routes,RouteSort.SPEED,"—",zone,Locale.GERMAN).labels)
    }
    @Test fun agreedThresholdsAreExclusiveAndContinueWithTheirSpecifiedSteps() {
        for((value,upper) in listOf(0.0 to 10.0,9.999 to 10.0,10.0 to 25.0,25.0 to 50.0,50.0 to 75.0,75.0 to 100.0,100.0 to 125.0,125.0 to 150.0,150.0 to 200.0,200.0 to 250.0,250.0 to 300.0,399.0 to 400.0))
            assertEquals(upper,RouteListSections.upper(RouteSort.DISTANCE,value*1000),0.0)
        for((value,upper) in listOf(0.0 to .5,.5 to 1.0,1.0 to 1.5,1.5 to 2.0,2.0 to 3.0,3.0 to 4.0))
            assertEquals(upper,RouteListSections.upper(RouteSort.MOVING,value*3600000),0.0)
        for((value,upper) in listOf(0.0 to 20.0,19.99 to 20.0,20.0 to 25.0,25.0 to 30.0,44.9 to 45.0))
            assertEquals(upper,RouteListSections.upper(RouteSort.SPEED,value),0.0)
    }
    @Test fun filteredSummariesUseOnlyKnownValuesAndWeightSpeedByMovingTime() {
        val rows=listOf(route("fast","2026-10-01",40.0,1.0),route("slow","2026-10-02",20.0,2.0),
            route("planned","2026-10-03",10.0,null),route("noDistance","2026-10-04",null,1.0),
            route("otherYear","2025-10-01",1000.0,1.0))
        val query=RouteQuery().apply { fromDate=LocalDate.of(2026,1,1);toDate=LocalDate.of(2026,12,31) }
        val index=RouteListSections(query.apply(rows,{null},zone),RouteSort.DATE,"Ohne Angabe",zone,Locale.GERMAN)
        val group=index.groups.single()
        assertEquals(4,group.count);assertEquals(70000.0,group.distanceM!!,0.0)
        assertEquals(4*3600000.0,group.movingMs!!,0.0)
        assertEquals(20.0,group.averageKmh!!,0.0) // (40+20) / (1+2), not (40+10)/2.
        assertEquals(group,index.groupAt(0));assertNull(index.groupAt(1))
        val missing=RouteListSections(listOf(route("none","2026-10-01",null,null)),RouteSort.DATE,"Ohne Angabe",zone).groups.single()
        assertEquals(1,missing.count);assertNull(missing.distanceM);assertNull(missing.movingMs);assertNull(missing.averageKmh)
    }
    @Test fun emptyListsAndOutOfRangeNativeRequestsAreSafe() {
        val empty=RouteListSections(emptyList(),RouteSort.DATE,"—")
        assertTrue(empty.labels.isEmpty());assertEquals(0,empty.position(-1));assertEquals(0,empty.section(500))
        val one=RouteListSections(listOf(route("a","2026-01-01")),RouteSort.DATE,"—")
        assertEquals(0,one.position(20));assertEquals(0,one.position(-4));assertEquals(0,one.section(500))
    }
}
