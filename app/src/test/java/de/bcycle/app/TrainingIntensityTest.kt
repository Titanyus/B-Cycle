package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class TrainingIntensityTest {
    private fun workout(vararg targets:TrainingTarget)=TrainingWorkout("0".repeat(64),"Test","","Ride","Test","zwo",targets.map { TrainingStep("",60.0,listOf(it)) })
    private fun tag(vararg targets:TrainingTarget)=TrainingIntensity.of(workout(*targets),RiderProfile(ftpW=200))
    @Test fun thresholdsAreExactAndIntermediateEnduranceIsNotInvented() {
        assertEquals(TrainingIntensity.EASY,tag(TrainingTarget(TrainingUnit.FTP,.699)))
        assertNull(tag(TrainingTarget(TrainingUnit.FTP,.70)))
        assertNull(tag(TrainingTarget(TrainingUnit.FTP,.879)))
        assertEquals(TrainingIntensity.SWEET_SPOT,tag(TrainingTarget(TrainingUnit.FTP,.88)))
        assertEquals(TrainingIntensity.SWEET_SPOT,tag(TrainingTarget(TrainingUnit.FTP,.949)))
        assertEquals(TrainingIntensity.HARD,tag(TrainingTarget(TrainingUnit.FTP,.95)))
    }
    @Test fun rampsRangesAndShortSprintsDetermineIntensityDespiteLongRecovery() {
        assertEquals(TrainingIntensity.HARD,tag(TrainingTarget(TrainingUnit.FTP,.40),TrainingTarget(TrainingUnit.FTP,1.8)))
        assertEquals(TrainingIntensity.HARD,tag(TrainingTarget(TrainingUnit.FTP,1.1,.4,true)))
        assertEquals(TrainingIntensity.SWEET_SPOT,tag(TrainingTarget(TrainingUnit.FTP,.4,.92,true)))
        assertEquals(TrainingIntensity.HARD,tag(TrainingTarget(TrainingUnit.FTP,.9,1.0)))
        assertEquals(TrainingIntensity.EASY,tag(TrainingTarget(TrainingUnit.WATTS,130.0)))
        assertEquals(TrainingIntensity.SWEET_SPOT,tag(TrainingTarget(TrainingUnit.WATTS,180.0)))
        assertEquals(TrainingIntensity.HARD,tag(TrainingTarget(TrainingUnit.WATTS,200.0)))
    }
    @Test fun unknownReferencesAndHeartRateDoNotProducePowerTags() {
        assertNull(TrainingIntensity.of(workout(TrainingTarget(TrainingUnit.WATTS,150.0)),RiderProfile()))
        assertNull(tag(TrainingTarget(TrainingUnit.HR_BPM,120.0)))
        assertNull(tag(TrainingTarget(TrainingUnit.POWER_ZONE,2.0)))
        assertNull(tag(TrainingTarget(TrainingUnit.FTP,.5),TrainingTarget(TrainingUnit.POWER_ZONE,3.0)))
    }
}
