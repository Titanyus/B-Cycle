package de.bcycle.hrv

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sin

class CoveragePolicyTest {
    private fun packet(ticks:Int)=byteArrayOf(0x16,75,ticks.toByte(),(ticks ushr 8).toByte())
    private fun measurement():MeasurementSession=MeasurementSession(0).apply {
        for(second in 1..209) accept(second*1000L,packet(819+(12*sin(second/10.0)).toInt()))
        advance(210000)
    }
    @Test fun oneEightHundredMsRrPerSecondStillDisplaysHrvWithExplicitCoverageWarning() {
        val session=measurement();val value=Evaluate.session(session)
        assertEquals(Phase.COMPLETE,session.phase)
        assertTrue(value.coverage in .78.. .82)
        assertTrue(value.reasons.toString(),value.acceptable)
        assertEquals(listOf("LOW_RR_COVERAGE"),value.warnings)
        assertNotNull(value.corrected?.lnRmssd)
        assertEquals(179,session.rrTicks.size)
        assertTrue(session.rrTicks.sum()*1000.0/1024<session.protocol.measureMs)
    }
    @Test fun interruptedRrOrContactStillRejectEvenWithWarningPolicy() {
        val session=measurement();session.abort("DISCONNECTED",discardUncommittedCompletion=true)
        val value=Evaluate.session(session)
        assertFalse(value.acceptable);assertTrue("DISCONNECTED" in value.reasons)
        assertNull(value.corrected)
    }
    @Test fun excessiveDurationStillRejectsAndArtefactThresholdRemainsActive() {
        val session=MeasurementSession(0)
        for(second in 1..209) session.accept(second*1000L,packet(1229+(12*sin(second/10.0)).toInt()))
        session.advance(210000)
        assertTrue("RR_DURATION" in Evaluate.session(session).reasons)
        assertFalse(Evaluate.session(session).acceptable)
    }
}
