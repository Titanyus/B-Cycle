package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class RouteValueDistributionTest {
    private val profile = RiderProfile(ftpW = 200, restingHeartRate = 60, maximumHeartRate = 160)
    @Test fun histogramIsTimeWeightedEvenWithoutDistanceAndIndependentOfDistanceBars() {
        val a = RouteZoneAccumulator(profile)
        a.add(0, null, 100.0, 120.0)
        a.add(1000, null, 200.0, 150.0)
        a.add(5000, null, 0.0, null)
        a.add(6000, null, null, null)
        val curves = a.timeDistributions()
        assertEquals(listOf(AnalysisChart.POWER, AnalysisChart.HEART), curves.map { it.chart })
        val power = curves[0]; val heart = curves[1]
        fun bin(curve: RouteValueDistribution, value: Double):Int {
            val position=(value-curve.minimum)/(curve.maximum-curve.minimum)*curve.milliseconds.size
            return (if(curve.chart==AnalysisChart.POWER) kotlin.math.ceil(position).toInt()-1 else position.toInt()).coerceIn(curve.milliseconds.indices)
        }
        assertEquals(6000.0, power.totalMs, 0.0)
        assertEquals(1000.0, power.milliseconds[bin(power, 100.0)], 0.0)
        assertEquals(4000.0, power.milliseconds[bin(power, 200.0)], 0.0)
        assertEquals(1000.0, power.milliseconds[bin(power, 0.0)], 0.0)
        assertEquals(5000.0, heart.totalMs, 0.0)
        assertEquals(144.0,heart.mean!!,0.0) // 1 second at 120 plus 4 seconds at 150.
        assertEquals(150.0,power.mean!!,0.0)
        assertNull(a.result().power); assertNull(a.result().heart)
        assertEquals(power.totalMs, power.bars.sumOf { it.milliseconds }, .000001)
        assertTrue(power.bars.all { it.milliseconds.isFinite() && it.milliseconds >= 0 })
        assertEquals(6000.0, power.milliseconds.sum(), 0.0) // display partitioning never mutates source
    }
    @Test fun gapsAndPausesAreNotCountedAndMissingValuesAreNotZero() {
        val a = RouteZoneAccumulator(profile)
        a.add(0, 0.0, 100.0, 130.0); a.add(1000, 10.0, null, null)
        a.add(2000, 20.0, 200.0, 140.0)
        a.add(12000, 120.0, 200.0, 140.0) // gap
        a.timer(true); a.add(13000, 130.0, 200.0, 140.0)
        a.timer(false); a.add(14000, 140.0, 200.0, 140.0)
        a.add(15000, 150.0, 200.0, 140.0, segmentStart = true)
        a.add(16000, 160.0, null, null)
        a.timeDistributions().forEach { assertEquals(2000.0, it.totalMs, 0.0) }
        assertEquals(0.0, a.timeDistributions().first().milliseconds.first(), 0.0)
    }
    @Test fun axesCoverMeasuredPeaksAndAllProfileZonesAndPreserveKnownColors() {
        val a = RouteZoneAccumulator(profile)
        a.add(0, 0.0, 999.0, 190.0); a.add(1000, 10.0, 1200.0, 200.0)
        val power = a.timeDistributions()[0]; val heart = a.timeDistributions()[1]
        assertTrue(power.maximum >= 1200); assertTrue(heart.maximum >= 200)
        assertEquals(1200.0, power.observedMaximum, 0.0)
        assertEquals(ZoneGlowColors.powerColors, power.bands.map { it.color })
        assertEquals(TrainingZoneColors.values, heart.bands.map { it.color })
        assertEquals(Coggan.boundaries.map { it * 2.0 }, power.bands.dropLast(1).map { it.upper })
        assertEquals(0.0, power.minimum, 0.0)
        for(distribution in listOf(power,heart)) {
            assertEquals(distribution.totalMs,distribution.bars.sumOf { it.milliseconds },.000001)
            val step=distribution.bars.first().upper-distribution.bars.first().lower
            assertTrue(distribution.bars.all { kotlin.math.abs((it.upper-it.lower)-step)<.000001 })
            assertTrue(distribution.bars.size<=BicycleDesign.analysisDistributionMaxBars)
        }
        val modest = RouteZoneAccumulator(profile)
        modest.add(0, null, 10.0, 80.0); modest.add(1000, null, 10.0, 80.0)
        assertTrue(modest.timeDistributions()[0].maximum > 300)
        assertTrue(modest.timeDistributions()[1].minimum <= 80)
        assertTrue(modest.timeDistributions()[1].maximum >= 160)
    }
    @Test fun fixedWattAndBpmStepsPreserveTimeAndExactZoneThresholds() {
        val power=RouteTimeHistogram(65535)
        for((value,time) in listOf(0.0 to 1000L,139.0 to 2000L,140.0 to 3000L,500.0 to 1000L)) { power.observe(value);power.add(value,time) }
        val result=power.result(AnalysisChart.POWER,profile.copy(ftpW=253))!!
        assertEquals(7000.0,result.bars.sumOf { it.milliseconds },0.0)
        assertEquals(253*.55,result.bands.first().upper,.000001)
        assertEquals(listOf(3000.0,3000.0,0.0,0.0,0.0,0.0,1000.0),result.zoneMilliseconds) // 139 and 140 share a display bin, but belong to different zones.
        assertTrue(result.bars.all { it.upper-it.lower==5.0 })
        assertEquals(5000.0,result.bars.single { it.lower==135.0 && it.upper==140.0 }.milliseconds,0.0)
        val heart=RouteTimeHistogram(250)
        for(value in listOf(80.0,120.0,159.0,180.0)) { heart.observe(value);heart.add(value,1000) }
        val pulse=heart.result(AnalysisChart.HEART,profile)!!
        assertEquals(4000.0,pulse.bars.sumOf { it.milliseconds },0.0)
        assertEquals(3000.0,pulse.zoneMilliseconds.sum(),0.0) // 80 bpm now belongs to open Z1; 180 remains above the profile maximum.
        assertEquals(1000.0,pulse.zoneMilliseconds.last(),0.0)
        assertTrue(pulse.bars.all { it.upper-it.lower==2.0 })
        val boundaryPower=RouteTimeHistogram(65535)
        boundaryPower.observe(110.0);boundaryPower.add(110.0,1000)
        assertEquals(1000.0,boundaryPower.result(AnalysisChart.POWER,profile)!!.bars.single { it.upper==110.0 }.milliseconds,0.0)
        val boundaryHeart=RouteTimeHistogram(250)
        boundaryHeart.observe(200.0);boundaryHeart.observe(160.0);boundaryHeart.add(160.0,1000)
        assertEquals(1000.0,boundaryHeart.result(AnalysisChart.HEART,profile)!!.bars.single { it.upper==160.0 }.milliseconds,0.0)
        val huge=RouteTimeHistogram(65535);huge.observe(65535.0);huge.add(65535.0,1000)
        val bounded=huge.result(AnalysisChart.POWER,profile)!!
        assertTrue(bounded.bars.size<=BicycleDesign.analysisDistributionMaxBars)
        assertEquals(1000.0,bounded.bars.sumOf { it.milliseconds },0.0)
        val step=bounded.bars.first().upper-bounded.bars.first().lower
        assertTrue(bounded.bars.all { it.upper-it.lower==step })
    }
    @Test fun missingProfileAndUnavailableSensorDoNotCreateFakeCurvesAndMemoryIsBounded() {
        val empty = RouteZoneAccumulator(RiderProfile())
        empty.add(0, null, 100.0, 130.0); empty.add(1000, null, 100.0, 130.0)
        assertTrue(empty.timeDistributions().isEmpty())
        val noPulse = RouteZoneAccumulator(profile)
        for (i in 0..10000) noPulse.add(i * 1000L, null, (i % 1000).toDouble(), null)
        assertEquals(1, noPulse.timeDistributions().size)
        assertTrue(noPulse.timeDistributions().single().milliseconds.size <= 128)
        assertEquals(10_000_000.0, noPulse.timeDistributions().single().totalMs, 0.0)
    }
}
