package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[33])
class RideStatisticsTest {
    private val zone=ZoneId.of("Europe/Berlin")
    private fun ride(id:String,date:String,km:Double=10.0,moving:Long?=60000,duration:Long?=120000,origin:TrackOrigin=TrackOrigin.RECORDED)=
        ManagedRoute(id,id,false,"GPX",RouteDetails(LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),km*1000,duration,movingMs=moving),true,origin)
    private fun profiles()=RideProfileCatalog(mapOf(
        "one" to RideProfileMetadata(0,BikeType.EBIKE,"E-Bike",0xffaabbcc.toInt(),BikeType.EBIKE),
        "two" to RideProfileMetadata(0,BikeType.EBIKE,"E-Bike",0xffaabbcc.toInt(),BikeType.EBIKE)),
        mapOf("one" to """{"bikeId":"bike-one"}""","two" to """{"bikeId":"bike-one"}"""))
    @Test fun sumsLocalMonthsAndIsoWeeksWithoutCountingPlannedRoutes() {
        val cache=RideStatisticsCache()
        val entries=listOf(ride("one","2025-12-29"),ride("two","2026-01-01",20.0),ride("other","2026-01-30",30.0),
            ride("planned","2026-01-01",999.0,origin=TrackOrigin.IMPORTED),ride("last","2027-01-01",40.0))
        val result=cache.get(entries,profiles(),2026,zone)
        assertEquals(4,result.rideCount)
        assertEquals(10.0,result.months[2025]!![11],0.0)
        assertEquals(50.0,result.months[2026]!![0],0.0)
        assertEquals(53,result.weeks.size)
        assertEquals(30.0,result.weeks[0],0.0) // Includes the December dates belonging to ISO week 1.
        assertEquals(40.0,result.weeks.last(),0.0) // January 1, 2027 still belongs to ISO year 2026.
        assertEquals(52,cache.get(entries,profiles(),2025,zone).weeks.size)
    }
    @Test fun timeSharesKeepBikeIdentityProfileColorsAndFallbackDuration() {
        val cache=RideStatisticsCache()
        val entries=listOf(ride("one","2026-01-01"),ride("two","2026-02-01",moving=null),ride("unknown","2026-02-01",moving=180000))
        val result=cache.get(entries,profiles(),2026,zone)
        val bike=result.bikes.single { it.id=="bike-one" }
        assertEquals("E-Bike",bike.name);assertEquals(0xffaabbcc.toInt(),bike.color)
        assertEquals(180000.0,bike.milliseconds,0.0)
        assertEquals(120000.0,result.bikes.single { it.id=="" }.milliseconds,0.0) // Invalid moving > elapsed uses duration.
        val changed=profiles().let { RideProfileCatalog(it.values.mapValues { entry -> entry.value.copy(color=0xff112233.toInt()) },it.raw) }
        assertEquals(0xff112233.toInt(),cache.get(entries,changed,2026,zone).bikes.single { it.id=="bike-one" }.color)
    }
    @Test fun cachedSnapshotChangesOnlyForRelevantInputsAndHandlesMissingValues() {
        val cache=RideStatisticsCache();val entries=listOf(ride("one","2026-01-01"))
        val first=cache.get(entries,profiles(),2026,zone)
        assertSame(first,cache.get(entries.map { it.copy(name="renamed",hasRoute=false) },profiles(),2026,zone))
        assertNotSame(first,cache.get(entries.map { it.copy(details=it.details.copy(distanceM=12345.0)) },profiles(),2026,zone))
        assertEquals(0,cache.get(emptyList(),profiles(),2026,zone).rideCount)
        val missing=ride("none","2026-01-01").copy(details=RouteDetails())
        val result=cache.get(listOf(missing),profiles(),2026,zone)
        assertTrue(result.months.isEmpty());assertTrue(result.bikes.isEmpty());assertTrue(result.weeks.all { it==0.0 })
    }
}
