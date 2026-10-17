package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class MapMotionTest {
    private fun fix(lon:Double,bearing:Double=0.0)=GpsValues(latitude=51.0,longitude=lon,bearingDegrees=bearing,elapsedMs=1234)
    @Test fun delayedPlaybackInterpolatesWholeFixIntervalAndKeepsTripValuesAndShortHeadingArc() {
        val motion=MapMotion()
        assertEquals(fix(10.0,359.0),motion.move(fix(10.0,359.0),0,true))
        motion.move(fix(10.001,1.0),1000,true)
        val mid=motion.at(1600)
        assertEquals(10.0005,mid.longitude!!,.000001)
        assertEquals(0.0,mid.bearingDegrees!!,.000001)
        assertEquals(1234L,mid.elapsedMs)
        assertEquals(fix(10.001,1.0),motion.at(2100));assertFalse(motion.active)
    }
    @Test fun retargetNeverJumpsAndStandstillLargeJumpsAndResumeSnapWithoutExtrapolation() {
        val motion=MapMotion();motion.move(fix(10.0),0,true)
        motion.move(fix(10.001),1000,true)
        val mid=motion.at(1600)
        assertEquals(mid.longitude,motion.move(fix(10.002),1600,true).longitude)
        assertEquals(fix(10.002),motion.move(fix(10.002),1601,false));assertFalse(motion.active)
        assertEquals(fix(12.0),motion.move(fix(12.0),1602,true));assertFalse(motion.active)
        motion.reset();assertEquals(fix(14.0),motion.move(fix(14.0),1603,true))
    }
    @Test fun antimeridianUsesShortArcAndRepeatedMetadataDoesNotExtendPlayback() {
        val motion=MapMotion();motion.move(fix(179.9998),0,true)
        motion.move(fix(-179.9998),1000,true)
        assertEquals(-180.0,motion.at(1600).longitude!!,.000001)
        motion.move(fix(-179.9998),1700,true)
        assertEquals(fix(-179.9998),motion.at(2100));assertFalse(motion.active)
        assertEquals(fix(-179.9998),motion.at(8000));assertFalse(motion.active)
    }
    @Test fun oneHertzUpdatesKeepMovingAcrossOld450MillisecondStopAndAcrossFixBoundaries() {
        val motion=MapMotion();motion.move(fix(10.0),0,true)
        for(i in 1..6) {
            motion.move(fix(10.0+i*.0001),i*1000L,true)
            if(i>=2) {
                val a=motion.at(i*1000L+200).longitude!!
                val b=motion.at(i*1000L+600).longitude!!
                val c=motion.at(i*1000L+900).longitude!!
                assertTrue(a<b && b<c);assertTrue(motion.active)
                assertTrue(c<=10.0+i*.0001)
            }
        }
        val last=fix(10.0006)
        assertEquals(last,motion.at(10000));assertFalse(motion.active)
    }
    @Test fun realGpsOutageIsNotBridgedByInventedIntermediatePositions() {
        val motion=MapMotion();motion.move(fix(10.0),0,true)
        motion.move(fix(10.0001),1000,true)
        assertEquals(fix(10.0001),motion.at(5000));assertFalse(motion.active)
        assertEquals(fix(10.0002),motion.move(fix(10.0002),7000,true))
        assertFalse(motion.active)
    }
    @Test fun irregularUpdatesStayMonotonicBoundedAndDoNotMoveGpsSourceValues() {
        val motion=MapMotion();motion.move(fix(10.0),0,true)
        var previous=10.0
        val samples=listOf(900L to fix(10.0001),2100L to fix(10.0002),3000L to fix(10.0003),4200L to fix(10.0004))
        var index=0
        for(now in 0L..8000L step 30) {
            while(index<samples.size && now>=samples[index].first) { motion.move(samples[index].second,now,true);index++ }
            val displayed=motion.at(now)
            val lon=displayed.longitude!!
            assertTrue(lon>=previous-1e-10);assertTrue(lon<=10.0+index*.0001+1e-10)
            previous=lon
        }
        assertFalse(motion.active)
    }
}
