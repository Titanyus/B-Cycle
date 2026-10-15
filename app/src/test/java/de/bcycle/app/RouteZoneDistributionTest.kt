package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class RouteZoneDistributionTest {
    private val profile = RiderProfile(ftpW = 200, restingHeartRate = 60, maximumHeartRate = 160)
    @Test fun distanceNotTimeWeightsEachSensorIndependentlyAndZeroPowerIsZoneOne() {
        val a = RouteZoneAccumulator(profile)
        a.add(0, 0.0, 0.0, 110.0)
        a.add(1000, 10.0, 200.0, 150.0)
        a.add(2000, 40.0, null, 150.0)
        a.add(3000, 60.0, null, null)
        a.add(4000, 80.0, null, null)
        val result = a.result()
        assertEquals(listOf(10.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0), result.power!!.meters)
        assertEquals(listOf(25.0, 0.0, 0.0, 75.0, 0.0, 0.0, 0.0), result.power.percentages)
        assertEquals(60.0, result.heart!!.totalMeters, 0.0)
        assertEquals(100.0 / 6, result.heart.percentages[0], .00001)
        assertEquals(500.0 / 6, result.heart.percentages[4], .00001)
    }
    @Test fun exactPowerAndHeartBoundariesUseExistingProfileModels() {
        for (watts in listOf(0.0, 110.0, 111.0, 150.0, 151.0, 180.0, 181.0, 210.0, 211.0, 240.0, 241.0, 300.0, 301.0)) {
            val a = RouteZoneAccumulator(profile)
            a.add(0, 0.0, watts, null); a.add(1000, 10.0, watts, null)
            val zones = a.result().power!!.meters
            assertEquals(listOf(Coggan.zone(watts, 200)!! - 1), zones.indices.filter { zones[it] > 0 })
        }
        for (bpm in listOf(109, 110, 119, 120, 129, 130, 139, 140, 149, 150, 160, 161)) {
            val a = RouteZoneAccumulator(profile)
            a.add(0, 0.0, null, bpm.toDouble()); a.add(1000, 10.0, null, bpm.toDouble())
            val expected = profile.heartZones()!!.zoneFor(bpm)
            if (expected == null) assertNull(a.result().heart)
            else assertEquals(10.0, a.result().heart!!.meters[expected.index - 1], 0.0)
        }
    }
    @Test fun noBridgingGapsMissingDistanceSegmentsResetsOrTimerPauses() {
        val a = RouteZoneAccumulator(profile)
        a.add(0, 0.0, 200.0, 130.0)
        a.add(1000, 10.0, 200.0, 130.0) // count 10
        a.add(10000, 100.0, 200.0, 130.0) // time gap
        a.add(11000, null, 200.0, 130.0)
        a.add(12000, 120.0, 200.0, 130.0) // missing distance
        a.add(13000, 130.0, 200.0, 130.0, segmentStart = true)
        a.add(14000, 0.0, 200.0, 130.0) // counter reset
        a.timer(true); a.add(15000, 150.0, 200.0, 130.0)
        a.timer(false); a.add(16000, 160.0, 200.0, 130.0)
        a.add(17000, 170.0, 200.0, 130.0) // count 10
        a.add(17000, 200.0, 200.0, 130.0) // duplicate time
        assertEquals(20.0, a.result().power!!.totalMeters, .00001)
        assertEquals(20.0, a.result().heart!!.totalMeters, .00001)
    }
    @Test fun missingProfileInvalidSensorsOrNoDistanceNeverFabricateDistribution() {
        for (p in listOf(RiderProfile(), profile)) {
            val a = RouteZoneAccumulator(p)
            a.add(0, 0.0, Double.NaN, -1.0); a.add(1000, 10.0, null, null)
            assertNull(a.result().power); assertNull(a.result().heart)
        }
        val noProfile = RouteZoneAccumulator(RiderProfile())
        noProfile.add(0, 0.0, 200.0, 130.0); noProfile.add(1000, 10.0, 200.0, 130.0)
        assertEquals(RouteZoneDistributions(), noProfile.result())
        val stationary = RouteZoneAccumulator(profile)
        stationary.add(0, 0.0, 200.0, 130.0); stationary.add(1000, 0.0, 200.0, 130.0)
        assertEquals(RouteZoneDistributions(), stationary.result())
    }
}
