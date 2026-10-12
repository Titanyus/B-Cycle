package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class RouteMovingAverageTest {
    @Test fun movingAverageUsesMovingTimeRatherThanElapsedTime() {
        val details = RouteDetails(distanceM = 30000.0, durationMs = 7200000, movingMs = 3600000)
        assertEquals(30.0, details.movingAverageKmh!!, 0.000001)
        assertEquals(30.0, RouteDetails.from(details.json()).movingAverageKmh!!, 0.000001)
        assertEquals(0.0, details.copy(distanceM = 0.0).movingAverageKmh!!, 0.0)
    }
    @Test fun unavailableAndInconsistentInputsNeverProduceInventedAverages() {
        val valid = RouteDetails(distanceM = 30000.0, durationMs = 7200000, movingMs = 3600000)
        listOf(valid.copy(distanceM = null), valid.copy(movingMs = null), valid.copy(movingMs = 0),
            valid.copy(movingMs = -1), valid.copy(movingMs = 7200001), valid.copy(distanceM = -1.0),
            valid.copy(distanceM = Double.NaN), valid.copy(distanceM = Double.POSITIVE_INFINITY)
        ).forEach { assertNull(it.movingAverageKmh) }
        assertEquals(30.0, valid.copy(durationMs = null).movingAverageKmh!!, 0.000001)
    }
}
