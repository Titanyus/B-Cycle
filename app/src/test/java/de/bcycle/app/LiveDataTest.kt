package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class LiveDataTest {
    @Test fun partialUpdatesPreserveAllFieldsAndExplicitZero() {
        val data = LiveData(2500, 180, mapOf(LiveMetric.CADENCE to 170L), listOf("OFF", "ECO"), listOf(87L))
        val updated = data.merge(LiveData(riderPowerWatts = 0, otherValues = mapOf(LiveMetric.BATTERY to 80L)))
        assertEquals(2500, updated.speedHundredths)
        assertEquals(0, updated.riderPowerWatts)
        assertEquals(170L, updated.value(LiveMetric.CADENCE))
        assertEquals("ECO: 87 km", updated.displayRanges())
        assertFalse(LiveData().hasValues)
        assertTrue(LiveData.sample(LiveMetric.CADENCE, 0).hasValues)
    }

    @Test fun formatsPublishedScalesIncludingSignedTemperatureAndLargeOdometer() {
        assertEquals("85,0 U/min", LiveMetric.CADENCE.format(170))
        assertEquals("24,5 Nm", LiveMetric.RIDER_TORQUE.format(490))
        assertEquals("1234,6 km", LiveMetric.ODOMETER.format(1_234_567))
        assertEquals("28,6 °C", LiveMetric.BATTERY_TEMPERATURE.format(572))
        assertEquals("-5,0 °C", LiveMetric.BATTERY_TEMPERATURE.format(99))
        assertEquals("420,0 Wh", LiveMetric.REMAINING_ENERGY.format(4200))
        assertEquals("—", LiveData().display(LiveMetric.BATTERY))
    }

    @Test fun usesBikeSuppliedModeNamesAndDoesNotGuessUnalignedRangeLabels() {
        val data = LiveData(otherValues = mapOf(LiveMetric.ASSIST_MODE to 2L), assistNames = listOf("OFF", "ECO", "AUTO"), rangesKm = listOf(135L, 87L))
        assertEquals("AUTO", data.display(LiveMetric.ASSIST_MODE))
        assertEquals("ECO: 135 km · AUTO: 87 km", data.displayRanges())
        assertEquals("Stufe 1: 135 km · Stufe 2: 87 km", data.copy(assistNames = listOf("UNKNOWN")).displayRanges())
        assertEquals("Stufe 2", data.copy(assistNames = null).display(LiveMetric.ASSIST_MODE))
    }
}
