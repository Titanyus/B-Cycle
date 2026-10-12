package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class PerformanceEngineTest {
    private fun power(t: Long, watts: Double?) = PerformanceEvent(PerformanceChannel.POWER, t, watts, source = PowerSource.BOSCH_RIDER.name)
    private fun series(values: List<Double>, hz: Int = 1, pulse: Double? = 150.0, speed: Double? = 10.0): PerformanceValues {
        val engine = PerformanceEngine()
        values.forEachIndexed { second, watts ->
            repeat(hz) { i ->
                val time = second * 1000L + i * (1000 / hz)
                engine.accept(power(time, watts))
                if (pulse != null) engine.accept(PerformanceEvent(PerformanceChannel.HEART_RATE, time, pulse, source = "HEART_RATE"))
                if (speed != null) engine.accept(PerformanceEvent(PerformanceChannel.SPEED, time, speed, source = "GPS_SPEED"))
            }
        }
        engine.advance(values.size * 1000L)
        return engine.snapshot()
    }
    @Test fun oneHourAt200WattsMatchesPhysicalAndTrainingReferenceValues() {
        val v = series(List(3600) { 200.0 })
        assertEquals(200.0, v.normalizedPowerW!!, 1e-9)
        assertEquals(720.0, v.mechanicalWorkKj!!, 1e-9)
        assertEquals(717.0172084130019, v.caloriesKcal!!, .00001)
        assertEquals(.18, v.speedToPowerRatio!!, 1e-9)
        assertEquals(4.0 / 3, v.efficiencyFactor!!, 1e-9)
        assertEquals(3571, v.quality.npWindows)
        assertEquals(1.0, v.quality.powerCoverage, 0.0)
    }
    @Test fun independentlyCalculatedIntervalFixtureMatchesFourthPowerReference() {
        val v = series(List(30) { 300.0 } + List(30) { 100.0 })
        assertEquals(223.0694887793096, v.normalizedPowerW!!, 1e-8)
        assertEquals(12.0, v.mechanicalWorkKj!!, 1e-9)
    }
    @Test fun shortPowerSpikesAreSmoothedOverThirtySeconds() {
        val v = series(List(30) { 200.0 } + List(3) { 1000.0 } + List(57) { 200.0 })
        assertEquals(248.34143176587477, v.normalizedPowerW!!, 1e-8)
        assertEquals(20.4, v.mechanicalWorkKj!!, 1e-9)
    }
    @Test fun hiitAndTrueCoastingAreIncludedRatherThanDroppedAsMissing() {
        val hiit = series(List(120) { if (it % 30 < 15) 500.0 else 0.0 })
        assertEquals(250.0, hiit.normalizedPowerW!!, 1e-8)
        assertEquals(30.0, hiit.mechanicalWorkKj!!, 1e-9)
        val coasting = series(List(30) { 200.0 } + List(30) { 0.0 })
        assertEquals(135.3952187526803, coasting.normalizedPowerW!!, 1e-8)
        assertEquals(6.0, coasting.mechanicalWorkKj!!, 1e-9)
        assertEquals(1.0, coasting.quality.powerCoverage, 0.0)
    }
    @Test fun changingSampleRatesDoesNotChangeTimeWeightedResultsOrMultiplyWork() {
        val watts = List(30) { 300.0 } + List(30) { 100.0 }
        val a = series(watts, 1); val b = series(watts, 10)
        assertEquals(a.normalizedPowerW!!, b.normalizedPowerW!!, 1e-8)
        assertEquals(a.mechanicalWorkKj!!, b.mechanicalWorkKj!!, 1e-8)
        assertEquals(a.efficiencyFactor!!, b.efficiencyFactor!!, 1e-8)
    }
    @Test fun irregularSubsecondIntervalsIntegrateActualDurationWithoutLinearInterpolation() {
        val e = PerformanceEngine()
        e.accept(power(0, 100.0)); e.accept(power(250, 300.0)); e.accept(power(1900, 0.0)); e.advance(2000)
        assertEquals(.520, e.snapshot().mechanicalWorkKj!!, 1e-9)
        assertNull(e.snapshot().normalizedPowerW)
    }
    @Test fun twoMinuteCoastingWithSilentZeroMatchesExplicitZeroSamples() {
        val engine = PerformanceEngine()
        val reference = series(List(30) { 200.0 } + List(120) { 0.0 } + List(30) { 200.0 })
        for (second in 0 until 180) {
            val at = second * 1000L
            if (second < 30 || second >= 150 || second == 30)
                engine.accept(power(at, if (second == 30) 0.0 else 200.0))
            engine.accept(PerformanceEvent(PerformanceChannel.BIKE_ACTIVITY, at, 10.0, source = "SPEED"))
            engine.accept(PerformanceEvent(PerformanceChannel.SPEED, at, 10.0, source = "GPS_SPEED"))
            engine.accept(PerformanceEvent(PerformanceChannel.HEART_RATE, at, 150.0))
        }
        engine.advance(180000)
        val actual = engine.snapshot()
        assertEquals(reference.normalizedPowerW!!, actual.normalizedPowerW!!, 1e-8)
        assertEquals(reference.mechanicalWorkKj!!, actual.mechanicalWorkKj!!, 1e-8)
        assertEquals(reference.caloriesKcal!!, actual.caloriesKcal!!, 1e-8)
        assertEquals(reference.speedToPowerRatio!!, actual.speedToPowerRatio!!, 1e-8)
        assertEquals(reference.efficiencyFactor!!, actual.efficiencyFactor!!, 1e-8)
        assertEquals(115000L, actual.quality.continuedZeroMs)
        assertEquals(1.0, actual.quality.powerCoverage, 0.0)
    }
    @Test fun gpsCannotMaintainBikeZeroAndLostBikeContinuityRequiresNewPower() {
        val stale = PerformanceEngine(); stale.accept(power(0, 0.0).copy(receivedAtMs = -10000))
        stale.accept(PerformanceEvent(PerformanceChannel.BIKE_ACTIVITY, 0, 10.0, source = "SPEED")); stale.advance(1000)
        assertEquals(0L, stale.snapshot().quality.powerMs)
        val gpsOnly = PerformanceEngine(); gpsOnly.accept(power(0, 0.0))
        for (second in 0..60) gpsOnly.accept(PerformanceEvent(PerformanceChannel.SPEED, second * 1000L, 10.0, source = "GPS_SPEED"))
        assertNull(gpsOnly.snapshot().mechanicalWorkKj)
        val engine = PerformanceEngine(); engine.accept(power(0, 0.0))
        engine.accept(PerformanceEvent(PerformanceChannel.BIKE_ACTIVITY, 0, 10.0, source = "SPEED"))
        engine.advance(20000)
        engine.accept(PerformanceEvent(PerformanceChannel.BIKE_ACTIVITY, 20000, 10.0, source = "SPEED"))
        engine.advance(21000)
        assertEquals(5000L, engine.snapshot().quality.powerMs)
        assertEquals(0L, engine.snapshot().quality.continuedZeroMs)
        val nonzero = PerformanceEngine(); nonzero.accept(power(0, 200.0))
        for (second in 0..60) nonzero.accept(PerformanceEvent(PerformanceChannel.BIKE_ACTIVITY, second * 1000L, 10.0, source = "SPEED"))
        assertEquals(1.0, nonzero.snapshot().knownMechanicalWorkKj, 1e-9)
        assertNull(nonzero.snapshot().mechanicalWorkKj)
    }
    @Test fun longGapsStopTheIntegralAndCannotBecomeFakeZeroWindows() {
        val e = PerformanceEngine(); e.accept(power(0, 200.0)); e.advance(60_000)
        val v = e.snapshot()
        assertEquals(1.0, v.knownMechanicalWorkKj, 1e-9)
        assertEquals(5000L, v.quality.powerMs); assertEquals(55000L, v.quality.longestPowerGapMs)
        assertNull(v.normalizedPowerW); assertNull(v.mechanicalWorkKj); assertNull(v.caloriesKcal); assertNull(v.efficiencyFactor)
        e.accept(power(60_000, 200.0)); e.advance(61_000)
        assertEquals(1.2, e.snapshot().knownMechanicalWorkKj, 1e-9)
    }
    @Test fun explicitDisconnectInvalidatesImmediatelyAndCannotBeBridgedAfterReconnect() {
        val e = PerformanceEngine(); e.accept(power(0, 200.0))
        e.accept(power(1500, null).copy(connected = false)); e.advance(10_000)
        e.accept(power(10_000, 200.0)); e.advance(11_000)
        assertEquals(.5, e.snapshot().knownMechanicalWorkKj, 1e-9)
        assertEquals(2500L, e.snapshot().quality.powerMs)
        assertNull(e.snapshot().normalizedPowerW)
    }
    @Test fun allMissingAndInvalidPowerNeverProducesAnEnergyEstimate() {
        for (watts in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, -1.0, 65536.0)) {
            val e = PerformanceEngine(); e.accept(power(0, watts)); e.advance(60_000)
            assertNull(e.snapshot().mechanicalWorkKj); assertNull(e.snapshot().caloriesKcal)
        }
    }
    @Test fun shortRidesGetWorkButNoMisleadingNormalizedPowerOrEf() {
        val v = series(List(29) { 200.0 })
        assertEquals(5.8, v.mechanicalWorkKj!!, 1e-8)
        assertNull(v.normalizedPowerW); assertNull(v.efficiencyFactor)
        assertEquals(200.0, series(List(30) { 200.0 }).normalizedPowerW!!, 1e-8)
    }
    @Test fun zeroPowerIsValidButRatioCannotDivideByZero() {
        val v = series(List(60) { 0.0 })
        assertEquals(0.0, v.normalizedPowerW!!, 0.0); assertEquals(0.0, v.mechanicalWorkKj!!, 0.0)
        assertEquals(0.0, v.caloriesKcal!!, 0.0); assertNull(v.speedToPowerRatio)
        assertEquals(0.0, v.efficiencyFactor!!, 0.0)
    }
    @Test fun missingHeartRatePreventsEfWithoutPreventingPowerMetrics() {
        val v = series(List(60) { 200.0 }, pulse = null)
        assertEquals(200.0, v.normalizedPowerW!!, 1e-9)
        assertNull(v.efficiencyFactor); assertNotNull(v.caloriesKcal)
        assertNull(series(List(60) { 200.0 }, pulse = 0.0).efficiencyFactor)
    }
    @Test fun unmatchedSpeedAndHeartIntervalsAreNotSilentlyComparedToWholeRidePower() {
        val e = PerformanceEngine()
        for (second in 0 until 60) {
            val t = second * 1000L; e.accept(power(t, 200.0))
            if (second >= 30) {
                e.accept(PerformanceEvent(PerformanceChannel.SPEED, t, 10.0, source = "SPEED"))
                e.accept(PerformanceEvent(PerformanceChannel.HEART_RATE, t, 150.0))
            }
        }
        e.advance(60_000); assertNull(e.snapshot().speedToPowerRatio); assertNull(e.snapshot().efficiencyFactor)
        assertNotNull(e.snapshot().normalizedPowerW)
    }
    @Test fun pauseExcludesTimeAndClearsContinuityWithoutResettingAccumulatedWork() {
        val e = PerformanceEngine()
        for (second in 0 until 30) e.accept(power(second * 1000L, 200.0))
        e.accept(PerformanceEvent(PerformanceChannel.PAUSE, 30_000)); e.advance(90_000)
        e.accept(PerformanceEvent(PerformanceChannel.RESUME, 90_000))
        for (second in 90 until 120) e.accept(power(second * 1000L, 200.0))
        e.advance(120_000)
        assertEquals(60000L, e.snapshot().quality.activeMs)
        assertEquals(12.0, e.snapshot().mechanicalWorkKj!!, 1e-8)
        // Only two completed windows, not a window spanning the recording pause.
        assertEquals(2, e.snapshot().quality.npWindows)
        assertEquals(200.0, e.snapshot().normalizedPowerW!!, 1e-9) // Both intentional segments are complete.
    }
    @Test fun onlyBoschRiderSourceIsActiveAndOtherMeterIdentitiesArePreparedWithoutBeingMixed() {
        val e = PerformanceEngine()
        e.accept(power(0, 1000.0).copy(source = PowerSource.PEDAL.name)); e.advance(1000)
        assertEquals(0.0, e.snapshot().knownMechanicalWorkKj, 0.0)
        e.accept(power(1000, 200.0)); e.advance(2000)
        assertEquals(.2, e.snapshot().knownMechanicalWorkKj, 1e-9)
    }
    @Test fun receiptAgeAndRealSensorAgeBothLimitValidityAndFutureOrReorderedTimesAreRejected() {
        val e = PerformanceEngine()
        e.accept(power(0, 200.0).copy(receivedAtMs = -4000)); e.advance(2000)
        assertEquals(.2, e.snapshot().knownMechanicalWorkKj, 1e-9)
        assertThrows(IllegalArgumentException::class.java) { e.accept(power(1999, 200.0)) }
        val other = PerformanceEngine()
        other.accept(power(0, 200.0).copy(sensorTimeMs = 1000)); other.advance(1000)
        assertEquals(0.0, other.snapshot().knownMechanicalWorkKj, 0.0)
    }
    @Test fun smallGapWorkRemainsObservedNotExtrapolatedAndNpWindowsNeverPadMissingData() {
        val e = PerformanceEngine()
        for (second in 0 until 120) e.accept(power(second * 1000L, if (second == 60) null else 200.0))
        e.advance(120000)
        assertEquals(23.8, e.snapshot().mechanicalWorkKj!!, 1e-8)
        assertEquals(119000L, e.snapshot().quality.powerMs)
        assertNull(e.snapshot().normalizedPowerW)
    }
    @Test fun comparisonSourceCannotOverwriteOrInvalidateBoschAndRetainsItsOwnAge() {
        val engine = PerformanceEngine()
        engine.accept(power(0, 200.0))
        engine.accept(PerformanceEvent(PerformanceChannel.POWER, 1000, 900.0, source = PowerSource.PEDAL.name))
        engine.advance(2000)
        assertEquals(.4, engine.snapshot().mechanicalWorkKj!!, 1e-9)
        assertEquals(200.0, engine.latestPower!!.watts!!, 0.0)
        assertEquals(900.0, engine.powerSources.getValue(PowerSource.PEDAL).watts!!, 0.0)
        assertEquals(1000L, engine.powerSources.getValue(PowerSource.PEDAL).ageMs(2000))
        assertTrue(engine.powerSources.getValue(PowerSource.PEDAL).usableAt(2000))
        assertFalse(engine.powerSources.getValue(PowerSource.PEDAL).usableAt(7000))
    }

}
