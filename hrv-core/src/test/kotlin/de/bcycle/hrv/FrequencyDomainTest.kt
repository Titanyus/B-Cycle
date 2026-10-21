package de.bcycle.hrv

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class FrequencyDomainTest {
    @Test fun recoversKnownLfAndHfPowerFromIrregularBeatTiming() {
        val nn=mutableListOf<Double>();var seconds=0.0
        while(seconds<300) {
            val rr=800+40*sin(2*PI*.1*seconds)+20*sin(2*PI*.25*seconds)
            nn+=rr;seconds+=rr/1000
        }
        val f=FrequencyDomain.calculate(nn)!!
        // Sinusoid variance A²/2, with small finite-window/interpolation error.
        assertEquals(800.0,f.lfMs2,100.0);assertEquals(200.0,f.hfMs2,45.0)
        assertEquals(4.0,f.lfHf!!,.8);assertTrue(f.segments>=7)
    }
    @Test fun constantIntervalsHaveNoSpectralPowerAndNoFiniteRatio() {
        val f=FrequencyDomain.calculate(List(400) { 800.0 })!!
        assertEquals(0.0,f.lfMs2,1e-15);assertEquals(0.0,f.hfMs2,1e-15);assertNull(f.lfHf)
        assertNull(FrequencyDomain.calculate(List(30) { 800.0 }))
    }
    @Test fun oneMinuteProtocolAllowsOnlySmallBoundaryLoss() {
        val s=MeasurementSession(0,Protocol(0,60000))
        for(i in 1..59) { val rr=1024+(8*sin(i/5.0)).toInt();s.accept(i*1000L,byteArrayOf(0x16,60,rr.toByte(),(rr ushr 8).toByte())) }
        s.advance(60000)
        assertEquals(Phase.COMPLETE,s.phase)
        assertTrue(Evaluate.session(s).reasons.toString(),Evaluate.session(s).acceptable)
        s.rrTicks.clear();s.rrTicks.addAll(List(50) { 1024 })
        assertTrue("SHORT_SERIES" in Evaluate.session(s).reasons)
    }
}
