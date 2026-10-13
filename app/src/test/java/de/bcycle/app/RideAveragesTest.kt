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
    @Test fun directValuesReuseHiddenAverageTrendsEvenWhenInstantaneousComparisonDisagrees() {
        val trend=AverageTrends()
        val pairs=listOf("SPEED" to "AVERAGE_SPEED", "GPS_SPEED" to "GPS_MOVING_AVERAGE",
            "CADENCE" to "AVERAGE_CADENCE", "HEART_RATE" to "HEART_AVERAGE")
        fun sample(second: Int, rising: Boolean) {
            val step=if(rising) second else 20-second
            // All direct values are below their rising averages, then above falling averages.
            val live=if(rising) 1 else 200
            val data=LiveData(live*100,otherValues=mapOf(LiveMetric.AVERAGE_SPEED to 2500L+step*10L,
                LiveMetric.CADENCE to live*2L, LiveMetric.AVERAGE_CADENCE to 160L+step*2L))
            val gps=GpsValues(speedKmh=live.toDouble(),movingAverageKmh=25.0+step*.1,overallAverageKmh=100.0-step)
            val pulse=PulseValues(live,130.0+step)
            for((direct,average) in pairs) {
                val actual=trend.indicator(DashboardField(direct),data,gps,pulse,second*1000L)
                val expected=trend.indicator(DashboardField(average),data,gps,pulse,second*1000L)
                assertEquals(direct,expected,actual)
                if(second==10 || second==20) assertEquals(direct,if(rising) 1 else -1,actual)
                if(second<8) assertEquals(direct,0,actual)
            }
        }
        for(second in 0..10) sample(second,true)
        for(second in 11..20) sample(second,false)
        for((direct,average) in pairs) assertEquals(trend.indicator(DashboardField(average),LiveData(),GpsValues(),PulseValues(),21000),
            trend.indicator(DashboardField(direct),LiveData(),GpsValues(),PulseValues(),21000))
        assertEquals(0,trend.indicator(DashboardField("MOTOR_POWER"),LiveData(),GpsValues(),PulseValues(),21000))
    }
    @Test fun directPowerUsesTheSameSmoothedAverageChevronThroughInstantaneousPedalingStops() {
        val trend=AverageTrends()
        for(second in 0..10) {
            val data=LiveData(riderPowerWatts=if(second==10) 0 else 300,otherValues=mapOf(LiveMetric.AVERAGE_POWER to 150L))
            val direct=trend.indicator(DashboardField("RIDER_POWER"),data,GpsValues(),PulseValues(),second*1000L)
            val average=trend.indicator(DashboardField("AVERAGE_POWER"),data,GpsValues(),PulseValues(),second*1000L)
            assertEquals(average,direct)
            if(second>=8) assertEquals(1,direct)
        }
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
