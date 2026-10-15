package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class RouteElevationTest {
    @Test fun rollingHillsCountEntireConfirmedSlopesWithoutLosingEverySummitRemainder() {
        val elevation=RouteElevation()
        // Twenty modest 10.4 m hills: old stepped anchor yields only 180 m instead of 208 m.
        elevation.add(100.0)
        repeat(20) {
            for(i in 1..26) elevation.add(100.0+i*.4)
            for(i in 25 downTo 0) elevation.add(100.0+i*.4)
        }
        assertEquals(208.0,elevation.ascent!!,1e-8)
        assertEquals(208.0,elevation.descent!!,1e-8)
    }
    @Test fun monotonicRampsAndSmallCounterMovementsDoNotDoubleCount() {
        val elevation=RouteElevation()
        listOf(100.0,103.0,105.0,104.0,105.0,108.4,107.0,108.4,105.4,100.0).forEach { elevation.add(it) }
        assertEquals(8.4,elevation.ascent!!,1e-8)
        assertEquals(8.4,elevation.descent!!,1e-8)
    }
    @Test fun noiseBelowReversalThresholdIsNotAccumulatedAndBoundariesAreInclusive() {
        val noise=RouteElevation()
        repeat(1000) { noise.add(listOf(100.0,101.4,99.0,100.2)[it%4]) }
        assertEquals(0.0,noise.ascent!!,0.0);assertEquals(0.0,noise.descent!!,0.0)
        val boundary=RouteElevation()
        listOf(100.0,102.99).forEach { boundary.add(it) }
        assertEquals(0.0,boundary.ascent!!,0.0)
        boundary.add(103.0);assertEquals(3.0,boundary.ascent!!,0.0)
        boundary.add(100.0);assertEquals(3.0,boundary.descent!!,0.0)
    }
    @Test fun missingInvalidAndExplicitSegmentsNeverInventClimbsAndResetClearsTotals() {
        val elevation=RouteElevation()
        assertNull(elevation.ascent)
        elevation.add(100.0);assertNull(elevation.ascent)
        elevation.add(110.0);assertEquals(10.0,elevation.ascent!!,0.0)
        elevation.add(null);elevation.add(1000.0);elevation.add(1003.0)
        assertEquals(13.0,elevation.ascent!!,0.0)
        elevation.add(500.0,start=true);elevation.add(497.0)
        assertEquals(3.0,elevation.descent!!,0.0)
        elevation.add(Double.NaN);elevation.add(100.0)
        elevation.add(Double.POSITIVE_INFINITY);elevation.add(400.0)
        elevation.add(20000.0);elevation.add(10.0)
        assertEquals(13.0,elevation.ascent!!,0.0);assertEquals(3.0,elevation.descent!!,0.0)
        elevation.reset();assertNull(elevation.ascent);assertNull(elevation.descent)
    }
}
