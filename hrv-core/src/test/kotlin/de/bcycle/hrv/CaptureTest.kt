package de.bcycle.hrv

import org.junit.Assert.*
import org.junit.Test

class CaptureTest {
    private fun rr(ticks: Int = 1024) = byteArrayOf(0x16, 60, ticks.toByte(), (ticks shr 8).toByte())

    @Test fun parserRetainsEveryRrTickAndContactMetadata() {
        val p = HeartRatePacket.decode(byteArrayOf(0x1f, 0, 1, 2, 0, 0, 4, 0, 0))!!
        assertEquals(256, p.bpm); assertEquals(Contact.PRESENT, p.contact)
        assertEquals(listOf(1024, 0), p.rrTicks)
        val lost = HeartRatePacket.decode(byteArrayOf(0x14, 70, 0, 4))!!
        assertNull(lost.bpm); assertEquals(Contact.LOST, lost.contact)
        assertEquals(listOf(1024), lost.rrTicks)
        assertNull(HeartRatePacket.decode(byteArrayOf(0x10, 60, 0)))
        assertNull(HeartRatePacket.decode(byteArrayOf(0x20, 60)))
        assertEquals(emptyList<Int>(), HeartRatePacket.decode(byteArrayOf(0, 60))!!.rrTicks)
    }

    @Test fun protocolUsesFixedBoundariesAndExcludesFirstAcquisitionBatchAndLatePackets() {
        val s = MeasurementSession(1000)
        for (at in 2000L..210_000L step 1000) s.accept(at, rr())
        s.advance(211_000)
        assertEquals(Phase.COMPLETE, s.phase)
        assertEquals(179, s.rrTicks.size)
        assertEquals(30_000, s.protocol.restMs)
        assertEquals(180_000, s.protocol.measureMs)
        val count = s.packets.size
        s.accept(211_001, rr(800))
        assertEquals(count, s.packets.size)
        val e = Evaluate.session(s)
        assertNotNull(e.corrected)
        assertTrue(e.reasons.contains("ZERO_VARIABILITY"))
    }

    @Test fun knownGapOrContactLossNeverBecomesAContinuousMetric() {
        for ((bytes, reason) in listOf(byteArrayOf(0x14, 60, 0, 4) to "CONTACT_LOST", rr(0) to "ZERO_RR")) {
            val s = MeasurementSession(0)
            s.accept(1000, bytes)
            assertEquals(reason, s.problem); assertEquals(Phase.ABORTED, s.phase)
            assertNull(Evaluate.session(s).corrected)
        }
        val gap = MeasurementSession(0)
        gap.advance(5001)
        assertEquals("RR_GAP", gap.problem)
        assertNull(Evaluate.session(gap).corrected)
    }

    @Test fun acquisitionAcceptsMultipleRrPerBatchAndDoesNotDeduplicateEqualValues() {
        val s = MeasurementSession(0)
        for (at in 1000L..29_000L step 1000) s.accept(at, rr())
        s.accept(30_000, rr())
        val batch = byteArrayOf(0x16, 60, 0, 4, 0, 4)
        s.accept(32_000, batch); s.accept(34_000, batch)
        assertEquals(listOf(1024, 1024, 1024, 1024), s.rrTicks)
        assertEquals(Phase.MEASURE, s.phase)
        val d = s.diagnostics(35_000)
        assertEquals(5_000L, d.measurementElapsedMs)
        assertEquals(4000.0, d.rrDurationMs, 0.0)
        assertEquals(4, d.rrCount)
        assertEquals(2, d.selectedPacketCount)
        assertEquals(32, d.sessionPacketCount)
        assertEquals(2, d.maxRrPerSelectedPacket)
        assertEquals(2000L, d.firstSelectedAtMs)
        assertEquals(4000L, d.lastSelectedAtMs)
        assertEquals(0.8, d.coverage, 0.0)
    }

    @Test fun regularNotificationsDoNotImplyCompleteBeatCoverage() {
        val s = MeasurementSession(0)
        assertEquals(0.0, s.diagnostics(10_000).coverage, 0.0)
        for (i in 1..209) s.accept(i * 1000L, rr(819))
        s.advance(210_000)
        val d = s.diagnostics(220_000)
        assertEquals(180_000L, d.measurementElapsedMs)
        assertEquals(179, d.rrCount)
        assertEquals(179, d.selectedPacketCount)
        assertEquals(1, d.maxRrPerSelectedPacket)
        assertEquals(143165.0390625, d.rrDurationMs, 1e-8)
        assertFalse(Evaluate.session(s).reasons.contains("RR_DURATION"))
        assertTrue(Evaluate.session(s).warnings.contains("LOW_RR_COVERAGE"))
    }

    @Test fun receiptTimesMustBeOrderedButTwoBatchesInTheSameMillisecondRemainValid() {
        val s = MeasurementSession(0)
        s.accept(1000, rr()); s.accept(1000, rr())
        assertEquals(Phase.REST, s.phase)
        s.accept(999, rr())
        assertEquals("CLOCK_ORDER", s.problem)
        assertEquals(Phase.ABORTED, s.phase)
        assertNull(Evaluate.session(s).corrected)
    }
}
