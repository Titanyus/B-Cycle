package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class MapOvalAnchorTest {
    @Test fun cardinalDirectionsFollowZoomFourteenMarginsAndDiagonalCoursePointsAtCentreOnPortraitMap() {
        val oval=MapOvalAnchor()
        fun check(angle:Double,x:Double,y:Double) {
            val p=oval.point(angle,360,600)
            assertEquals(x,p.x.toDouble(),.000001);assertEquals(y,p.y.toDouble(),.000001)
        }
        check(0.0,.5,.66);check(90.0,.34,.5);check(180.0,.5,.34);check(270.0,.66,.5)
        for(degrees in 0..359) {
            val p=oval.point(degrees.toDouble(),360,600)
            val x=(.5-p.x)*360;val y=(.5-p.y)*600
            assertEquals(1.0,(x/57.6).pow(2)+(y/96).pow(2),.00001)
            assertTrue(p.x in (.34f-.00001f)..(.66f+.00001f))
            assertTrue(p.y in (.34f-.00001f)..(.66f+.00001f))
            assertEquals(degrees.toDouble(),PhoneHeadingFilter.normalize(Math.toDegrees(atan2(x,-y))),.0001)
        }
    }
    @Test fun zoomMarginsInterpolateAndRadiusChangesSoftlyEvenWhileStationary() {
        val oval=MapOvalAnchor()
        for(zoom in 6..11) assertEquals(MapOvalAnchor.Anchor(.5f,.5f),oval.point(90.0,360,600,zoom.toDouble()))
        for((zoom,margin) in listOf(12.0 to .4,12.5 to .385,13.0 to .37,14.0 to .34,15.0 to .31,16.0 to .28,17.0 to .25,18.0 to .25,19.0 to .25)) {
            assertEquals(margin,oval.point(180.0,360,600,zoom).y.toDouble(),.00001)
        }
        assertEquals(.5f,oval.at(0.0,false,0,360,600,11.0).y,0f)
        val next=oval.at(0.0,false,33,360,600,17.0)
        assertTrue(next.y>.5f && next.y<.75f);assertTrue(oval.active)
        for(now in 66L..6000L step 33) oval.at(0.0,false,now,360,600,17.0)
        assertEquals(.75f,oval.at(null,false,6000,360,600,17.0).y,.0001f)
        assertFalse(oval.active)
    }
    @Test fun headingChangesAreGradualRateLimitedAndStationaryOrMissingCourseHoldsAnchor() {
        val oval=MapOvalAnchor()
        val start=oval.at(180.0,true,0,360,600)
        assertEquals(.66f,start.y,0f)
        val next=oval.at(180.0,true,33,360,600)
        val angle=PhoneHeadingFilter.normalize(Math.toDegrees(atan2((.5-next.x)*360,-(.5-next.y)*600)))
        assertTrue(abs(PhoneHeadingFilter.delta(0.0,angle))<=2.0)
        assertTrue(oval.active)
        val held=oval.at(90.0,false,66,360,600)
        assertEquals(next,held);assertFalse(oval.active)
        assertEquals(held,oval.at(null,true,1000,360,600))
        assertEquals(held,oval.at(Double.NaN,true,2000,360,600))
        for(now in 2033L..12000L step 33) oval.at(180.0,true,now,360,600)
        val south=oval.at(180.0,true,12000,360,600)
        assertEquals( .34f,south.y,.0001f)
    }
    @Test fun northWrapAndTinyCourseNoiseDoNotSendAnchorAroundTheWrongSide() {
        val oval=MapOvalAnchor()
        for(now in 0L..6000L step 33) oval.at(350.0,true,now,360,300)
        for(now in 6033L..12000L step 33) {
            val p=oval.at(10.0,true,now,360,300)
            assertTrue(p.y>.65f)
        }
        oval.reset()
        val initial=oval.at(0.0,true,0,360,300)
        for(now in 100L..2000L step 100) assertEquals(initial,oval.at(if(now%200==0L) 2.0 else 358.0,true,now,360,300))
        assertFalse(oval.active)
    }
}
