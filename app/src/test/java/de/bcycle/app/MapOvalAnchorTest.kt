package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class MapOvalAnchorTest {
    @Test fun cardinalDirectionsStayOneThirdFromEdgesAndDiagonalCoursePointsAtCentreOnPortraitMap() {
        val oval=MapOvalAnchor()
        fun check(angle:Double,x:Double,y:Double) {
            val p=oval.point(angle,360,600)
            assertEquals(x,p.x.toDouble(),.000001);assertEquals(y,p.y.toDouble(),.000001)
        }
        check(0.0,.5,2.0/3);check(90.0,1.0/3,.5);check(180.0,.5,1.0/3);check(270.0,2.0/3,.5)
        for(degrees in 0..359) {
            val p=oval.point(degrees.toDouble(),360,600)
            val x=(.5-p.x)*360;val y=(.5-p.y)*600
            assertEquals(1.0,(x/60).pow(2)+(y/100).pow(2),.00001)
            assertTrue(p.x in 1f/3-.00001f..2f/3+.00001f)
            assertTrue(p.y in 1f/3-.00001f..2f/3+.00001f)
            assertEquals(degrees.toDouble(),PhoneHeadingFilter.normalize(Math.toDegrees(atan2(x,-y))),.0001)
        }
    }
    @Test fun headingChangesAreGradualRateLimitedAndStationaryOrMissingCourseHoldsAnchor() {
        val oval=MapOvalAnchor()
        val start=oval.at(180.0,true,0,360,600)
        assertEquals(2f/3,start.y,0f)
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
        assertEquals(1f/3,south.y,.0001f)
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
