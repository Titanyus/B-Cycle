package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class MapMotionTest {
    private fun fix(lon: Double, bearing: Double = 0.0) = GpsValues(latitude = 51.0, longitude = lon, bearingDegrees = bearing, elapsedMs = 1234)
    @Test fun firstFixSnapsThenPositionAndHeadingMoveTogetherWithoutChangingTripValues() {
        val motion = MapMotion()
        assertEquals(fix(10.0, 359.0), motion.move(fix(10.0, 359.0), 0, true))
        motion.move(fix(10.001, 1.0), 100, true)
        val mid = motion.at(325)
        assertEquals(10.0005, mid.longitude!!, .000001)
        assertEquals(0.0, mid.bearingDegrees!!, .000001)
        assertEquals(1234L, mid.elapsedMs)
        assertEquals(fix(10.001, 1.0), motion.at(550)); assertFalse(motion.active)
    }
    @Test fun retargetStartsAtDisplayedPositionAndStandstillOrLargeJumpSnaps() {
        val motion = MapMotion(); motion.snap(fix(10.0))
        motion.move(fix(10.001), 0, true)
        val mid = motion.at(225)
        assertEquals(mid.longitude, motion.move(fix(10.002), 225, true).longitude)
        assertEquals(fix(10.002), motion.move(fix(10.002), 226, false)); assertFalse(motion.active)
        assertEquals(fix(12.0), motion.move(fix(12.0), 227, true)); assertFalse(motion.active)
        motion.reset(); assertEquals(fix(14.0), motion.move(fix(14.0), 228, true))
    }
    @Test fun antimeridianUsesShortArcAndRepeatedTargetDoesNotRestartAnimation() {
        val motion = MapMotion(); motion.snap(fix(179.9998))
        motion.move(fix(-179.9998), 0, true)
        assertEquals(-180.0, motion.at(225).longitude!!, .000001)
        motion.move(fix(-179.9998), 250, true)
        assertEquals(fix(-179.9998), motion.at(450)); assertFalse(motion.active)
    }
}
