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
        fun bin(curve: RouteValueDistribution, value: Double) = ((value - curve.minimum) / (curve.maximum - curve.minimum) * curve.milliseconds.size).toInt().coerceIn(curve.milliseconds.indices)
        assertEquals(6000.0, power.totalMs, 0.0)
        assertEquals(1000.0, power.milliseconds[bin(power, 100.0)], 0.0)
        assertEquals(4000.0, power.milliseconds[bin(power, 200.0)], 0.0)
        assertEquals(1000.0, power.milliseconds[bin(power, 0.0)], 0.0)
        assertEquals(5000.0, heart.totalMs, 0.0)
        assertNull(a.result().power); assertNull(a.result().heart)
        assertEquals(power.totalMs, power.smoothed().sum(), .000001)
        assertTrue(power.smoothed().all { it.isFinite() && it >= 0 })
        assertEquals(6000.0, power.milliseconds.sum(), 0.0) // smoothing never mutates source
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
        val modest = RouteZoneAccumulator(profile)
        modest.add(0, null, 10.0, 80.0); modest.add(1000, null, 10.0, 80.0)
        assertTrue(modest.timeDistributions()[0].maximum > 300)
        assertTrue(modest.timeDistributions()[1].minimum <= 80)
        assertTrue(modest.timeDistributions()[1].maximum >= 160)
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
