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
    @Test fun liveIndicatorsCompareAgainstHiddenAveragesAndUseGpsMovingAverage() {
        val trend=AverageTrends()
        val data=LiveData(3000,200,mapOf(LiveMetric.AVERAGE_SPEED to 2500L,LiveMetric.AVERAGE_POWER to 150L,
            LiveMetric.CADENCE to 180L,LiveMetric.AVERAGE_CADENCE to 160L))
        val gps=GpsValues(speedKmh=30.0,movingAverageKmh=32.0,overallAverageKmh=20.0)
        val pulse=PulseValues(bpm=145,average=130.0)
        for(id in listOf("SPEED","RIDER_POWER","CADENCE","HEART_RATE")) assertEquals(id,1,trend.indicator(DashboardField(id),data,gps,pulse,0))
        assertEquals(-1,trend.indicator(DashboardField("GPS_SPEED"),data,gps,pulse,0))
        assertEquals(0,trend.indicator(DashboardField("GPS_SPEED"),data,gps.copy(speedKmh=32.0),pulse,1))
        assertEquals(0,trend.indicator(DashboardField("RIDER_POWER"),data.copy(riderPowerWatts=150),gps,pulse,1))
        assertEquals(0,trend.indicator(DashboardField("RIDER_POWER"),LiveData(riderPowerWatts=200),gps,pulse,1))
        assertEquals(0,trend.indicator(DashboardField("HEART_RATE"),data,gps,PulseValues(average=130.0),1))
        assertEquals(0,trend.indicator(DashboardField("MOTOR_POWER"),data,gps,pulse,1))
    }
    @Test fun powerAverageDirectionSurvivesWholeWattPlateausAndIncludesRealZeroAndStaleReset() {
        val trend=AverageTrends();val field=DashboardField("AVERAGE_POWER")
        fun direction(power:Int?,seconds:Int)=trend.indicator(field,LiveData(riderPowerWatts=power,otherValues=mapOf(LiveMetric.AVERAGE_POWER to 150L)),GpsValues(),PulseValues(),seconds*1000L)
        for(second in 0..7) assertEquals(0,direction(200,second))
        for(second in 8..180) { repeat(20) { assertEquals(1,direction(200,second)) } }
        for(second in 181..191) direction(0,second)
        assertEquals(-1,direction(0,191))
        for(second in 192..202) direction(150,second)
        assertEquals(0,direction(150,202))
        assertEquals(0,direction(null,203));assertEquals(0,direction(200,204))
        assertEquals(0,direction(200,250))
        for(second in 251..258) direction(200,second)
        assertEquals(1,direction(200,258))
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
