package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class RideAveragesTest {
    @Test fun pedalingPausesAndMissingCadenceDoNotDiluteTheAverageAndResetStartsANewRide() {
        val mean = PedalingAverage()
        listOf(80.0, 0.0, Double.NaN, 0.0, 100.0).forEach(mean::add)
        assertEquals(90.0, mean.value!!, .000001)
        mean.reset(); mean.add(0.0); assertNull(mean.value)
        mean.add(60.0); assertEquals(60.0, mean.value!!, .000001)
    }
    @Test fun trendUsesAboutTenSecondsIgnoresRedrawFrequencyAndClearsMissingOrStaleValues() {
        val trend = AverageTrends()
        for (second in 0..7) repeat(40) { assertEquals(0, trend.direction("AVERAGE_SPEED", 25.0 + second * .1, second * 1000L)) }
        assertEquals(1, trend.direction("AVERAGE_SPEED", 26.0, 10_000))
        for (second in 11..20) trend.direction("AVERAGE_SPEED", 26.0 - (second - 10) * .2, second * 1000L)
        assertEquals(-1, trend.direction("AVERAGE_SPEED", 24.0, 20_000))
        assertEquals(0, trend.direction("AVERAGE_SPEED", null, 21_000))
        assertEquals(0, trend.direction("AVERAGE_SPEED", 25.0, 22_000))
        assertEquals(0, trend.direction("AVERAGE_SPEED", 29.0, 60_000))
        for (second in 0..10) assertEquals(0, trend.direction("HEART_AVERAGE", 140.0, second * 1000L))
    }
    @Test fun gradualLongRideTrendRemainsVisibleAndReturnsAfterAnOffscreenGap() {
        val trend = AverageTrends()
        // After three hours at 25 km/h, riding at 30 changes the mean by <0.005 in ten seconds.
        fun mean(seconds: Int) = (25.0 * 10800 + 30.0 * seconds) / (10800 + seconds)
        for (second in 0..10) trend.direction("GPS_OVERALL_AVERAGE", mean(second), second * 1000L)
        assertEquals(1, trend.direction("GPS_OVERALL_AVERAGE", mean(10), 10000))
        assertEquals(0, trend.direction("GPS_OVERALL_AVERAGE", mean(60), 60000))
        for (second in 61..70) trend.direction("GPS_OVERALL_AVERAGE", mean(second), second * 1000L)
        assertEquals(1, trend.direction("GPS_OVERALL_AVERAGE", mean(70), 70000))
        for (second in 71..81) trend.direction("GPS_OVERALL_AVERAGE", mean(70) - (second - 70) * .0004, second * 1000L)
        assertEquals(-1, trend.direction("GPS_OVERALL_AVERAGE", mean(70) - .0044, 81000))
    }
    @Test fun movingTimeExcludesStandstillAndNeverBridgesMissingSpeedOrLongGaps() {
        val moving = MovingTime()
        moving.add(0, 5.0); moving.add(1000, 5.0); moving.add(2000, 0.0)
        moving.add(3000, null); moving.add(4000, 5.0); moving.add(5000, 5.0)
        moving.add(30000, 5.0); moving.add(31000, 5.0)
        assertEquals(3000L, moving.value)
        assertEquals(18.0, RouteDetails(distanceM = 15.0, durationMs = 31000, movingMs = moving.value).movingAverageKmh!!, .001)
        assertNull(RouteDetails(distanceM = 15.0, durationMs = 31000).movingAverageKmh)
    }
}
