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
    @Test fun oneSnapshotSharesEachMetricAcrossEveryCurrentAverageAndGpsAlias() {
        val engine=AverageTrends()
        val input=PrimaryTrendInputs(speed=TrendReading(30.0,25.0,RecordingSource.BIKE),
            power=TrendReading(200.0,150.0,RecordingSource.POWER),cadence=TrendReading(90.0,100.0,RecordingSource.POWER),
            heart=TrendReading(140.0,120.0,RecordingSource.HEART))
        val pairs=listOf(Triple("SPEED","AVERAGE_SPEED",3),Triple("GPS_SPEED","GPS_MOVING_AVERAGE",3),
            Triple("CADENCE","AVERAGE_CADENCE",-2),Triple("RIDER_POWER","AVERAGE_POWER",2),Triple("HEART_RATE","HEART_AVERAGE",3))
        for(second in 0..10) {
            val snapshot=engine.update(input,second*1000L)
            repeat(20) {
                pairs.forEach { (direct,average,expected) ->
                    assertEquals(if(second<3) 0 else expected,snapshot.indicator(DashboardField(direct)))
                    assertEquals(snapshot.indicator(DashboardField(direct)),snapshot.indicator(DashboardField(average)))
                }
                assertEquals(snapshot.speed,snapshot.indicator(DashboardField("GPS_OVERALL_AVERAGE")))
            }
        }
        val changed=engine.update(input.copy(speed=TrendReading(40.0,25.0,RecordingSource.GPS)),12000)
        assertEquals(0,changed.speed);assertEquals(2,changed.power)
        assertEquals(3,engine.update(input.copy(speed=TrendReading(40.0,25.0,RecordingSource.GPS)),15000).speed)
    }
    @Test fun timeWeightedWindowKeepsShortPedalingPausesSoftButIncludesRealZeroAndResetsMissingAndStale() {
        val trend=AverageTrends();val field=DashboardField("RIDER_POWER")
        fun show(power:Int?,second:Int)=trend.update(PrimaryTrendInputs(power=TrendReading(power?.toDouble(),150.0)),second*1000L).indicator(field)
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
            for(second in 0..5) assertEquals(0,trend.update(PrimaryTrendInputs(speed=TrendReading(30.0,mean)),second*1000L).indicator(field))
        for(second in 10..13) trend.update(PrimaryTrendInputs(speed=TrendReading(30.0,25.0)),second*1000L).indicator(field)
        assertEquals(0,trend.update(PrimaryTrendInputs(speed=TrendReading(null,25.0)),14000).indicator(field))
        assertEquals(0,trend.update(PrimaryTrendInputs(speed=TrendReading(30.0,25.0)),15000).indicator(field))
        trend.clear()
        assertEquals(0,trend.update(PrimaryTrendInputs(speed=TrendReading(30.0,25.0)),18000).indicator(field))
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
