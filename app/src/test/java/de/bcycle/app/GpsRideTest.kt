package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.cos

class GpsRideTest {
    private fun fix(second: Int, metres: Double = 0.0, speed: Double? = 0.0, altitude: Double? = 100.0,
                    accuracy: Double = 5.0, vertical: Double? = 3.0, seaLevel: Boolean = false) =
        GpsFix(10_000L + second * 1000, 50.0, 8.0 + metres / (111_195.08 * cos(Math.toRadians(50.0))),
            accuracy, speed, 0.2, altitude, vertical, seaLevel)
    private fun feed(ride: GpsRide, fix: GpsFix) = ride.accept(fix, fix.timeMs)

    @Test fun standingStillWithPositionAndAltitudeNoiseAddsNeitherDistanceNorElevation() {
        val ride = GpsRide()
        for (second in 0..120) feed(ride, fix(second, (second % 3 - 1) * 1.0, altitude = 100.0 + second % 5 - 2))
        val values = ride.snapshot(130_000)
        assertEquals(0.0, values.distanceM!!, 0.001)
        assertEquals(0.0, values.speedKmh!!, 0.001)
        assertEquals(0.0, values.ascentM!!, 0.001)
        assertEquals(0.0, values.descentM!!, 0.001)
        assertEquals(120_000L, values.stoppedMs)
        assertNull(values.gradientPercent)
    }

    @Test fun knownRideSeparatesMovingAndOverallAverageAndEstimatesClimbingGradient() {
        val ride = GpsRide()
        for (second in 0..60) feed(ride, fix(second, second * 5.0, 5.0, 100 + second * 0.5))
        var values = ride.snapshot(70_000)
        assertEquals(300.0, values.distanceM!!, 0.1)
        assertEquals(18.0, values.movingAverageKmh!!, 0.01)
        assertEquals(18.0, values.speedKmh!!, 0.01)
        assertEquals(18.0, values.maximumKmh!!, 0.01)
        assertEquals(10.0, values.gradientPercent!!, 0.1)
        assertTrue(values.ascentM!! in 23.0..30.0)
        assertEquals(0.0, values.descentM!!, 0.001)
        for (second in 61..90) feed(ride, fix(second, 300.0, 0.0, 130.0))
        values = ride.snapshot(100_000)
        assertEquals(18.0, values.movingAverageKmh!!, 0.01)
        assertEquals(12.0, values.overallAverageKmh!!, 0.01)
        assertEquals(60_000L, values.movingMs)
        assertEquals(30_000L, values.stoppedMs)
        assertNull(values.gradientPercent)
    }

    @Test fun overallAverageUsesTheDistanceObservationTimeNotScreenTicksOrDeliveryDelay() {
        val ride = GpsRide()
        for (second in 0..60) feed(ride, fix(second, second * 5.0, 5.0))
        var previous = ride.snapshot(70_000).overallAverageKmh!!
        for (second in 61..75) {
            val measured = fix(second, 300 + (second - 60) * 8.0, 8.0)
            // Extra pulse/Bosch/UI redraws and delayed GPS delivery must not dilute the average.
            assertEquals(previous, ride.snapshot(measured.timeMs - 100).overallAverageKmh!!, .000001)
            assertTrue(ride.accept(measured, measured.timeMs + 300))
            val current = ride.snapshot(measured.timeMs + 300).overallAverageKmh!!
            assertTrue(current > previous)
            assertEquals(current, ride.snapshot(measured.timeMs + 900).overallAverageKmh!!, .000001)
            previous = current
        }
        feed(ride, fix(76, 420.0, 0.0))
        assertTrue(ride.snapshot(86_000).overallAverageKmh!! < previous)
        assertEquals(76_000L, ride.snapshot(86_000).elapsedMs)
    }
    @Test fun descendingCountsSeparatelyAndIsNegativeGradient() {
        val ride = GpsRide()
        for (second in 0..60) feed(ride, fix(second, second * 5.0, 5.0, 100 - second * 0.5))
        val values = ride.snapshot(70_000)
        assertEquals(0.0, values.ascentM!!, 0.001)
        assertTrue(values.descentM!! in 23.0..30.0)
        assertEquals(-10.0, values.gradientPercent!!, 0.1)
    }

    @Test fun inaccurateStaleDuplicateAndTeleportFixesCannotInventDistance() {
        val ride = GpsRide()
        assertTrue(feed(ride, fix(0)))
        assertFalse(feed(ride, fix(0, 50.0, 10.0)))
        assertFalse(ride.accept(fix(-1, 50.0, 10.0), 10_000))
        assertFalse(feed(ride, fix(1, 500.0, 10.0)))
        assertFalse(feed(ride, fix(2, 800.0, 10.0, accuracy = 50.0)))
        assertFalse(ride.accept(fix(3, 800.0, 10.0), 20_000))
        assertTrue(feed(ride, fix(11, 1000.0)))
        assertEquals(0.0, ride.snapshot(21_000).distanceM!!, 0.001)
        assertEquals(5, ride.rejected)
    }

    @Test fun signalGapDoesNotBridgePositionsAndPauseDoesNotCountBackgroundTime() {
        val ride = GpsRide()
        feed(ride, fix(0, 0.0, 5.0)); feed(ride, fix(1, 5.0, 5.0))
        assertNull(ride.snapshot(17_000).speedKmh)
        feed(ride, fix(21, 800.0, 5.0))
        var values = ride.snapshot(31_000)
        assertEquals(5.0, values.distanceM!!, 0.01)
        assertEquals(1000L, values.movingMs)
        ride.pause()
        ride.snapshot(100_000)
        feed(ride, fix(101, 2000.0, 5.0))
        values = ride.snapshot(111_000)
        assertEquals(5.0, values.distanceM!!, 0.01)
        assertEquals(21_000L, values.elapsedMs)
        ride.reset()
        assertNull(ride.snapshot(111_000).distanceM)
        assertNull(ride.snapshot(111_000).ascentM)
    }

    @Test fun missingVerticalAccuracySuppressesElevationAndSingleSpikeIsFiltered() {
        val ride = GpsRide()
        for (second in 0..5) feed(ride, fix(second, second * 5.0, 5.0, vertical = null))
        assertNull(ride.snapshot(15_000).altitudeM)
        assertNull(ride.snapshot(15_000).ascentM)
        for (second in 6..30) feed(ride, fix(second, second * 5.0, 5.0, if (second == 20) 2000.0 else 100.0))
        val values = ride.snapshot(40_000)
        assertEquals(100.0, values.altitudeM!!, 0.001)
        assertEquals(0.0, values.ascentM!!, 0.001)
        assertEquals(0.0, values.descentM!!, 0.001)
    }

    @Test fun altitudeDatumSwitchDoesNotInventAnAscentAndUnreliableSpeedIsIgnored() {
        val ride = GpsRide()
        for (second in 0..10) feed(ride, fix(second, second * 5.0, 5.0))
        feed(ride, fix(11, 55.0, 5.0, 150.0, seaLevel = true))
        val values = ride.snapshot(21_000)
        assertEquals(0.0, values.ascentM!!, 0.001)
        assertEquals(150.0, values.altitudeM!!, 0.001)
        assertTrue(values.seaLevel)
        val other = GpsRide()
        feed(other, fix(0).copy(speedMps = 20.0, speedAccuracyMps = 5.0))
        feed(other, fix(1).copy(speedMps = 100.0))
        assertEquals(0.0, other.snapshot(11_000).maximumKmh!!, 0.001)
    }

    @Test fun implausibleSingleSpeedSpikeDoesNotRaiseMaximum() {
        val ride = GpsRide()
        for (second in 0..10) feed(ride, fix(second, second * 5.0, if (second == 5) 30.0 else 5.0))
        assertEquals(18.0, ride.snapshot(20_000).maximumKmh!!, 0.01)
    }

    @Test fun positionOnlyFallbackAccumulatesMovementBelowIndividualPositionUncertainty() {
        val ride = GpsRide()
        for (second in 0..60) feed(ride, fix(second, second * 5.0, speed = null))
        var values = ride.snapshot(70_000)
        assertEquals(300.0, values.distanceM!!, 0.1)
        assertEquals(18.0, values.speedKmh!!, 0.01)
        assertEquals(18.0, values.movingAverageKmh!!, 0.4)
        for (second in 61..70) feed(ride, fix(second, 300.0, speed = null))
        values = ride.snapshot(80_000)
        assertEquals(300.0, values.distanceM!!, 0.1)
        assertEquals(0.0, values.speedKmh!!, 0.001)
    }

    @Test fun bikeDistanceUsesOdometerDeltasWithoutDoublingPartialUpdatesOrBridgingPauses() {
        val ride = BikeRide()
        assertNull(ride.distanceM)
        ride.update(14_791_749)
        assertEquals(0L, ride.distanceM)
        ride.update(14_791_937); ride.update(14_791_937); ride.update(null)
        assertEquals(188L, ride.distanceM)
        ride.pause(); ride.update(14_792_500)
        assertEquals(188L, ride.distanceM)
        ride.update(14_792_520)
        assertEquals(208L, ride.distanceM)
        ride.update(20) // A reset/swapped counter cannot produce negative kilometres.
        assertEquals(208L, ride.distanceM)
        ride.reset(20); ride.update(30)
        assertEquals(10L, ride.distanceM)
    }
}
