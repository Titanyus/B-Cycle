package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class ConfirmedBikePauseTest {
    @Test fun onlyFreshAccurateSustainedStationaryEvidenceWithoutBikeOrPowerConfirmsRest() {
        val pause=ConfirmedBikePause()
        fun update(at:Long,bike:Boolean=false,power:Boolean=false,speed:Double?=0.0,fix:Long?=at,accuracy:Double?=3.0)=
            pause.update(at,true,bike,power,speed,fix,accuracy)
        for(at in 0L..9000L step 1000) assertFalse(update(at))
        assertFalse(update(9999));assertTrue(update(10000))
        assertFalse(update(11000,bike=true));assertFalse(update(12000,power=true))
        assertFalse(update(13000,speed=3.0));assertFalse(update(14000))
        for(at in 15000L..23000L step 1000) assertFalse(update(at))
        assertTrue(update(24000));assertFalse(update(25000,fix=10000))
        assertFalse(update(26000,accuracy=100.0));assertFalse(update(27000,fix=null))
        assertFalse(update(28000,speed=null));assertFalse(pause.update(40000,false,false,false,0.0,40000,3.0))
        pause.reset();assertFalse(update(50000));assertFalse(update(59000))
    }
    private fun power(at:Long,watts:Double?)=PerformanceEvent(PerformanceChannel.POWER,at,watts,source=PowerSource.BOSCH_RIDER.name)
    @Test fun longConfirmedRestLeavesWholeRideClockButExcludesOnlyNpTimeAndNeverJoinsWindows() {
        val engine=PerformanceEngine()
        repeat(60) { engine.accept(power(it*1000L,200.0)) }
        engine.accept(power(60000,null));engine.accept(PerformanceEvent(PerformanceChannel.NP_PAUSE,60000,1.0))
        for(second in 61 until 2760) engine.accept(PerformanceEvent(PerformanceChannel.NP_PAUSE,second*1000L,1.0))
        engine.advance(2760000)
        for(second in 2760 until 2820) engine.accept(power(second*1000L,300.0))
        engine.advance(2820000)
        val v=engine.snapshot()
        assertEquals(2820000L,v.quality.activeMs);assertEquals(2700000L,v.quality.npPausedMs)
        assertEquals(1.0,v.quality.npPowerCoverage,0.0);assertEquals(1.0,v.quality.npWindowCoverage,0.0)
        assertEquals(62L,v.quality.npWindows)
        assertEquals(((200.0.pow(4)+300.0.pow(4))/2).pow(.25),v.normalizedPowerW!!,1e-8)
        assertEquals(30.0,v.knownMechanicalWorkKj,1e-8)
        assertNull(v.mechanicalWorkKj) // Existing whole-recording quality policy is unchanged.
    }
    @Test fun stalePauseEvidenceCannotExcludeUnobservedTime() {
        val e=PerformanceEngine()
        e.accept(PerformanceEvent(PerformanceChannel.NP_PAUSE,0,1.0));e.advance(60000)
        assertEquals(5000L,e.snapshot().quality.npPausedMs)
        assertNull(e.snapshot().normalizedPowerW)
    }
    @Test fun unconfirmedOutageStillCountsAsMissingAndAFlagCannotHideValidPower() {
        val unknown=PerformanceEngine()
        repeat(60) { unknown.accept(power(it*1000L,200.0)) }
        unknown.accept(power(60000,null));unknown.advance(2760000)
        assertNull(unknown.snapshot().normalizedPowerW);assertEquals(0L,unknown.snapshot().quality.npPausedMs)
        val valid=PerformanceEngine()
        repeat(60) { second ->
            val t=second*1000L;valid.accept(power(t,200.0));valid.accept(PerformanceEvent(PerformanceChannel.NP_PAUSE,t,1.0))
        }
        valid.advance(60000)
        assertEquals(200.0,valid.snapshot().normalizedPowerW!!,1e-8)
        assertEquals(0L,valid.snapshot().quality.npPausedMs)
    }
}
