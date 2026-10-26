package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class TrainerDistanceTest {
    private fun r(value:Double,at:Long)=SourceReading(value,at)
    @Test fun speedPairsIntegrateAndDuplicatesDoNotCountTwice() {
        val d=TrainerDistance()
        d.add(0,null,r(36.0,0));d.add(1000,null,r(36.0,1000))
        assertEquals(10.0,d.meters!!,.00001)
        d.add(1500,null,r(36.0,1000));assertEquals(10.0,d.meters!!,.00001)
        d.add(2000,null,r(0.0,2000));assertEquals(15.0,d.meters!!,.00001)
        d.add(3000,null,r(0.0,3000));assertEquals(15.0,d.meters!!,.00001)
    }
    @Test fun counterWinsAndSourceChangesOnlyAnchorWithoutJumps() {
        val d=TrainerDistance()
        d.add(0,r(100.0,0),r(36.0,0));d.add(1000,r(112.0,1000),r(36.0,1000))
        assertEquals(12.0,d.meters!!,.00001)
        d.add(7000,null,r(36.0,7000));d.add(8000,null,r(36.0,8000))
        assertEquals(22.0,d.meters!!,.00001)
        d.add(9000,r(800.0,9000),r(36.0,9000));assertEquals(22.0,d.meters!!,.00001)
        d.add(10000,r(810.0,10000),r(36.0,10000));assertEquals(32.0,d.meters!!,.00001)
        d.add(11000,r(0.0,11000),null);assertEquals(32.0,d.meters!!,.00001)
        d.add(12000,r(5.0,12000),null);assertEquals(37.0,d.meters!!,.00001)
    }
    @Test fun gapsDisconnectInvalidAndOldSamplesNeverInventDistance() {
        val d=TrainerDistance()
        assertNull(d.add(0,null,null))
        d.add(0,null,r(36.0,0));d.add(6000,null,r(36.0,0))
        d.add(7000,null,r(36.0,7000));assertEquals(0.0,d.meters!!,.00001)
        d.add(8000,null,r(Double.NaN,8000));d.add(9000,null,r(36.0,9000))
        d.add(10000,null,r(36.0,10000));assertEquals(10.0,d.meters!!,.00001)
        d.add(10000,null,r(36.0,9500));d.add(11000,null,r(36.0,11000))
        assertEquals(20.0,d.meters!!,.00001)
        d.pause();d.add(12000,null,r(36.0,12000));assertEquals(20.0,d.meters!!,.00001)
        d.reset();assertNull(d.meters)
    }
}
