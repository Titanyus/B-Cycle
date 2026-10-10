package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class HeartRateMeasurementTest {
    @Test fun decodesEightBitAndSixteenBitHeartRatesAndContactLoss() {
        assertEquals(145, HeartRateMeasurement.decode(byteArrayOf(0, 145.toByte()))?.bpm)
        assertEquals(256, HeartRateMeasurement.decode(byteArrayOf(1, 0, 1))?.bpm)
        assertNull(HeartRateMeasurement.decode(byteArrayOf(4, 145.toByte()))?.bpm)
        assertEquals(145, HeartRateMeasurement.decode(byteArrayOf(6, 145.toByte()))?.bpm)
        assertNull(HeartRateMeasurement.decode(byteArrayOf(0, 0))?.bpm)
    }
    @Test fun optionalEnergyAndRrIntervalsHaveCorrectLengthsAndUnits() {
        val parsed = HeartRateMeasurement.decode(byteArrayOf(0x18, 145.toByte(), 10, 0, 0, 4, 0, 2))!!
        assertEquals(10, parsed.energyKj)
        assertEquals(listOf(1.0, 0.5), parsed.rrSeconds)
        for (bytes in listOf(byteArrayOf(), byteArrayOf(1, 145.toByte()), byteArrayOf(8, 145.toByte(), 1), byteArrayOf(16, 145.toByte(), 1), byteArrayOf(0, 145.toByte(), 1)))
            assertNull(HeartRateMeasurement.decode(bytes))
    }
}
