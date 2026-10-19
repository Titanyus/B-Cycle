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
    @Test fun exactStrengthBoundariesAreSymmetricAndHaveSmallQuietZones() {
        for((id,limits) in listOf("RIDER_POWER" to listOf(2.0,20.0,40.0),"SPEED" to listOf(1.0,5.0,15.0),
            "CADENCE" to listOf(1.0,5.0,15.0),"HEART_RATE" to listOf(1.0,3.0,7.0))) {
            for(sign in listOf(-1,1)) {
                assertEquals(0,AverageTrends.level(id,sign*limits[0]))
                assertEquals(sign,AverageTrends.level(id,sign*(limits[0]+.001)))
                assertEquals(sign,AverageTrends.level(id,sign*limits[1]))
                assertEquals(2*sign,AverageTrends.level(id,sign*(limits[1]+.001)))
                assertEquals(2*sign,AverageTrends.level(id,sign*limits[2]))
                assertEquals(3*sign,AverageTrends.level(id,sign*(limits[2]+.001)))
            }
        }
        assertEquals(0,AverageTrends.level("SPEED",Double.NaN))
        assertFalse(AverageTrends.supports(DashboardField("MOTOR_POWER")))
    }
    @Test fun directAndAverageFieldsCompareCurrentWithTheirOwnMeanNotChangesOfTheMean() {
        val trend=AverageTrends()
        val data=LiveData(3000,200,otherValues=mapOf(LiveMetric.AVERAGE_SPEED to 2500L,
            LiveMetric.CADENCE to 180L,LiveMetric.AVERAGE_CADENCE to 200L,LiveMetric.AVERAGE_POWER to 150L))
        val gps=GpsValues(speedKmh=30.0,movingAverageKmh=25.0,overallAverageKmh=20.0)
        val pulse=PulseValues(140,120.0)
        val pairs=listOf(Triple("SPEED","AVERAGE_SPEED",3),Triple("GPS_SPEED","GPS_MOVING_AVERAGE",3),
            Triple("CADENCE","AVERAGE_CADENCE",-2),Triple("RIDER_POWER","AVERAGE_POWER",2),Triple("HEART_RATE","HEART_AVERAGE",3))
        for(second in 0..10) repeat(20) {
            for((direct,average,expected) in pairs) {
                assertEquals(if(second<3) 0 else expected,trend.indicator(DashboardField(direct),data,gps,pulse,second*1000L))
                assertEquals(if(second<3) 0 else expected,trend.indicator(DashboardField(average),data,gps,pulse,second*1000L))
            }
            assertEquals(if(second<3) 0 else 3,trend.indicator(DashboardField("GPS_OVERALL_AVERAGE"),data,gps,pulse,second*1000L))
        }
    }
    @Test fun timeWeightedWindowKeepsShortPedalingPausesSoftButIncludesRealZeroAndResetsMissingAndStale() {
        val trend=AverageTrends();val field=DashboardField("RIDER_POWER")
        fun show(power:Int?,second:Int)=trend.indicator(field,LiveData(riderPowerWatts=power,otherValues=mapOf(LiveMetric.AVERAGE_POWER to 150L)),GpsValues(),PulseValues(),second*1000L)
        for(second in 0..3) assertEquals(if(second<3) 0 else 3,show(300,second))
        assertEquals(3,show(0,4))
        for(second in 5..7) show(0,second)
        assertEquals(-3,show(0,7))
        assertEquals(0,show(null,8));assertEquals(0,show(300,9))
        assertEquals(0,show(300,40))
        assertEquals(3,show(300,43))
    }
    @Test fun absentInvalidOrZeroReferenceNeverProducesAnApparentTrend() {
        val trend=AverageTrends();val field=DashboardField("GPS_SPEED")
        for(mean in listOf(null,0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY))
            for(second in 0..5) assertEquals(0,trend.indicator(field,LiveData(),GpsValues(speedKmh=30.0,movingAverageKmh=mean),PulseValues(),second*1000L))
        for(second in 10..13) trend.indicator(field,LiveData(),GpsValues(speedKmh=30.0,movingAverageKmh=25.0),PulseValues(),second*1000L)
        assertEquals(0,trend.indicator(field,LiveData(),GpsValues(movingAverageKmh=25.0),PulseValues(),14000))
        assertEquals(0,trend.indicator(field,LiveData(),GpsValues(speedKmh=30.0,movingAverageKmh=25.0),PulseValues(),15000))
        trend.clear()
        assertEquals(0,trend.indicator(field,LiveData(),GpsValues(speedKmh=30.0,movingAverageKmh=25.0),PulseValues(),18000))
    }
    @Test fun hysteresisPreventsFlappingAroundEachStrengthBoundary() {
        assertEquals(1,AverageTrends.level("SPEED",5.3,1))
        assertEquals(2,AverageTrends.level("SPEED",5.6,1))
        assertEquals(2,AverageTrends.level("SPEED",4.8,2))
        assertEquals(1,AverageTrends.level("SPEED",4.4,2))
        assertEquals(2,AverageTrends.level("HEART_RATE",7.2,2))
        assertEquals(3,AverageTrends.level("HEART_RATE",7.3,2))
        assertEquals(-3,AverageTrends.level("RIDER_POWER",-39.5,-3))
        assertEquals(-2,AverageTrends.level("RIDER_POWER",-38.9,-3))
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
