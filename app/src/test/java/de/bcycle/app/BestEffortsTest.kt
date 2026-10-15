package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class BestEffortsTest {
    private fun engine() = PerformanceEngine(powerDurationsMs = BestEfforts.durationsMs)
    private fun power(e: PerformanceEngine, ms: Long, watts: Double?) = e.accept(
        PerformanceEvent(PerformanceChannel.POWER, ms, watts, source = PowerSource.BOSCH_RIDER.name))

    @Test fun fourLongestCompleteWindowsIncludeTwoHoursAndUseRiderPower() {
        val e = engine()
        for (second in 0..7200) {
            power(e, second * 1000L, 200.0)
            if (second in listOf(1199, 1200, 3599, 3600, 7199, 7200)) {
                val selected = BestEfforts.select(e.bestPowerAverages())
                val expected = when (second) {
                    1199 -> listOf(480L, 300L, 60L, 5L)
                    1200, 3599 -> listOf(1200L, 480L, 300L, 60L)
                    3600, 7199 -> listOf(3600L, 1200L, 480L, 300L)
                    else -> listOf(7200L, 3600L, 1200L, 480L)
                }
                assertEquals(expected.map { it * 1000 }, selected.map { it.durationMs })
                assertTrue(selected.all { it.watts == 200.0 })
            }
        }
        assertEquals(190.0, e.snapshot().estimatedFtpW!!, .000001)
        assertEquals(2.5, BestEfforts.select(e.bestPowerAverages()).first().wattsPerKg(80.0)!!, .000001)
    }
    @Test fun zerosAreIncludedAndMissingStaleAndPausedIntervalsNeverJoin() {
        val e = engine()
        for (second in 0..60) power(e, second * 1000L, if (second < 30) 200.0 else 0.0)
        assertEquals(100.0, e.bestPowerAverages().getValue(60000), .000001)
        for (mode in listOf("missing", "stale", "pause")) {
            val split = engine()
            for (second in 0..30) power(split, second * 1000L, 200.0)
            when (mode) {
                "missing" -> power(split, 30000, null)
                "stale" -> split.advance(40000)
                else -> {
                    split.accept(PerformanceEvent(PerformanceChannel.PAUSE, 30000))
                    split.accept(PerformanceEvent(PerformanceChannel.RESUME, 40000))
                }
            }
            for (second in 40..70) power(split, second * 1000L, 200.0)
            assertFalse(mode, 60000L in split.bestPowerAverages())
            assertEquals(200.0, split.bestPowerAverages().getValue(5000), .000001)
        }
        val noPower = engine()
        for (second in 0..7200) noPower.accept(PerformanceEvent(PerformanceChannel.SPEED, second * 1000L, 10.0))
        assertTrue(noPower.bestPowerAverages().isEmpty())
    }
    @Test fun selectionAndRelativePowerNeverInventUnavailableValues() {
        assertTrue(BestEfforts.select(emptyMap()).isEmpty())
        assertTrue(BestEfforts.select(mapOf(5000L to Double.NaN, 60000L to -1.0)).isEmpty())
        val zero = BestEffort(5000, 0.0)
        assertEquals(0.0, zero.wattsPerKg(80.0)!!, 0.0)
        for (weight in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) assertNull(zero.wattsPerKg(weight))
    }
}
