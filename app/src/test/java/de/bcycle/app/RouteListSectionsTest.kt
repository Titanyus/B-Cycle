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
        assertArrayEquals(arrayOf("90–100 km","70–80 km","—"),distance.labels)
        assertEquals(1,distance.position(1));assertEquals(1,distance.section(2))
        assertArrayEquals(arrayOf("2–3 h","—"),RouteListSections(routes,RouteSort.MOVING,"—",zone,Locale.GERMAN).labels)
        assertArrayEquals(arrayOf("45–50 km/h","35–40 km/h","—"),RouteListSections(routes,RouteSort.SPEED,"—",zone,Locale.GERMAN).labels)
    }
    @Test fun emptyListsAndOutOfRangeNativeRequestsAreSafe() {
        val empty=RouteListSections(emptyList(),RouteSort.DATE,"—")
        assertTrue(empty.labels.isEmpty());assertEquals(0,empty.position(-1));assertEquals(0,empty.section(500))
        val one=RouteListSections(listOf(route("a","2026-01-01")),RouteSort.DATE,"—")
        assertEquals(0,one.position(20));assertEquals(0,one.position(-4));assertEquals(0,one.section(500))
    }
}
