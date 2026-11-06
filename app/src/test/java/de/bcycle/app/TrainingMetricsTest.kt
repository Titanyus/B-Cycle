package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class TrainingMetricsTest {
    private fun workout(vararg steps:TrainingStep)=TrainingWorkout("0".repeat(64),"Test","","Ride","Test","zwo",steps.toList())
    private fun step(seconds:Double,power:Double)=TrainingStep("",seconds,listOf(TrainingTarget(TrainingUnit.FTP,power)))
    @Test fun steadyPowerGivesExpectedIntensityAndLoadAndDoesNotTrustProviderDescription() {
        val training=workout(step(3360.0,.72)).copy(description="Workout score 49")
        val metrics=TrainingMetrics.of(training,RiderProfile())
        assertEquals(3360.0,metrics.seconds!!,0.0);assertEquals(1,metrics.sections)
        assertEquals(.72,metrics.intensity!!,1e-10)
        assertEquals(48.384,metrics.load!!,1e-8)
        val watts=workout(TrainingStep("",3600.0,listOf(TrainingTarget(TrainingUnit.WATTS,200.0))))
        assertEquals(100.0,TrainingMetrics.of(watts,RiderProfile(ftpW=200)).load!!,1e-8)
        assertEquals(25.0,TrainingMetrics.of(watts,RiderProfile(ftpW=400)).load!!,1e-8)
        assertNull(TrainingMetrics.of(watts,RiderProfile()).load)
    }
    @Test fun rampMatchesAnalyticRollingPowerAndSplittingStepsDoesNotChangeResults() {
        val ramp=workout(TrainingStep("",120.0,listOf(TrainingTarget(TrainingUnit.FTP,.4,1.0,true))))
        val result=TrainingMetrics.of(ramp,RiderProfile())
        // 30 s rolling mean increases from .475 at t=30 to .925 at t=120.
        val expected=((.925.pow(5)-.475.pow(5))/(5*(.925-.475))).pow(.25)
        assertEquals(expected,result.intensity!!,1e-10)
        val split=workout(TrainingStep("",60.0,listOf(TrainingTarget(TrainingUnit.FTP,.4,.7,true))),TrainingStep("",60.0,listOf(TrainingTarget(TrainingUnit.FTP,.7,1.0,true))))
        assertEquals(result.intensity!!,TrainingMetrics.of(split,RiderProfile()).intensity!!,1e-10)
        assertEquals(2,TrainingMetrics.of(split,RiderProfile()).sections)
        assertEquals(.7,TrainingMetrics.of(workout(TrainingStep("",30.0,listOf(TrainingTarget(TrainingUnit.FTP,.4,1.0,true)))),RiderProfile()).intensity!!,1e-10)
    }
    @Test fun intervalsIncludeRecoveryAndUnknownTargetsNeverBecomeZeroPower() {
        val training=workout(step(600.0,.5),step(300.0,1.2),step(300.0,.4),step(300.0,1.2),step(600.0,.4))
        val result=TrainingMetrics.of(training,RiderProfile())
        assertEquals(2100.0,result.seconds!!,0.0);assertEquals(5,result.sections)
        assertTrue(result.intensity!!>.72);assertTrue(result.intensity!!<1.2)
        assertEquals(2100.0/3600*result.intensity!!.pow(2)*100,result.load!!,1e-10)
        assertNull(TrainingMetrics.of(workout(step(20.0,.5)),RiderProfile()).load)
        val free=workout(step(300.0,.5),TrainingStep("Free",60.0,emptyList()))
        assertNull(TrainingMetrics.of(free,RiderProfile()).intensity)
        val open=workout(TrainingStep("",null,listOf(TrainingTarget(TrainingUnit.FTP,.7)),"OPEN",null))
        assertNull(TrainingMetrics.of(open,RiderProfile()).seconds)
        assertNull(TrainingMetrics.of(open,RiderProfile()).load)
        assertNull(TrainingMetrics.of(workout(TrainingStep("",300.0,listOf(TrainingTarget(TrainingUnit.HR_BPM,120.0)))),RiderProfile()).intensity)
    }
}
