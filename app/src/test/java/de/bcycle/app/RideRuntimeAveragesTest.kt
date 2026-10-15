package de.bcycle.app

import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class RideRuntimeAveragesTest {
    @After fun finishBackgroundWorkBeforeTheTestContextIsDisposed() {
        val runtime = RideRuntime.get(RuntimeEnvironment.getApplication())
        runtime.stopRecording(); runtime.setVisible(false)
        val deadline = System.nanoTime() + 10_000_000_000L
        do {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            if (!runtime.saving && !runtime.mapState.busy) return
            Thread.sleep(10)
        } while (System.nanoTime() < deadline)
        fail("Recording and route workers did not finish")
    }
    @Test fun onlyNewCadenceSamplesAffectTheDerivedMeanAndSensorRecordsRemainOriginal() {
        val runtime = RideRuntime.get(RuntimeEnvironment.getApplication())
        runtime.beginRecording()
        val cadence = LiveMetric.CADENCE
        runtime.onSamples(listOf(McspSample(cadence.address, 160, true)))
        runtime.onData(LiveData(otherValues = mapOf(cadence to 160L)), true)
        // Cached values in unrelated packets are not extra cadence observations.
        repeat(30) { runtime.onData(runtime.bikeData.merge(LiveData(riderPowerWatts = 180)), true) }
        runtime.onSamples(listOf(McspSample(cadence.address, 0, true)))
        runtime.onSamples(listOf(McspSample(cadence.address, 200, true)))
        runtime.onSamples(listOf(McspSample(cadence.address, 0, true)))
        runtime.onData(LiveData(otherValues = mapOf(cadence to 0L, LiveMetric.AVERAGE_CADENCE to 45L)), true)
        assertEquals(180L, runtime.dashboardBikeData.value(LiveMetric.AVERAGE_CADENCE))
        assertEquals(45L, runtime.bikeData.value(LiveMetric.AVERAGE_CADENCE))
        assertEquals(0L, runtime.bikeData.value(cadence))
        assertEquals(0.0, recordingValues(runtime.bikeData, GpsValues(), null, PulseValues())["CADENCE"]!!, .001)
        runtime.stopRecording()
        runtime.onSamples(listOf(McspSample(cadence.address, 240, true)))
        assertEquals(180L, runtime.dashboardBikeData.value(LiveMetric.AVERAGE_CADENCE))
    }

    @Test fun idleSamplesKeepLiveReadingsButTripAveragesAndWheelDistanceOnlyChangeDuringRecording() {
        val runtime = RideRuntime.get(RuntimeEnvironment.getApplication())
        runtime.setVisible(true)
        val heart = org.robolectric.util.ReflectionHelpers.getField<(HeartRateMeasurement) -> Unit>(runtime.heart, "measurement")
        org.robolectric.util.ReflectionHelpers.setField(runtime.heart, "live", true)
        fun receive(cadence: Long, odometer: Long, bpm: Int) {
            runtime.onSamples(listOf(McspSample(LiveMetric.CADENCE.address, cadence, true)))
            runtime.onData(LiveData(otherValues = mapOf(LiveMetric.CADENCE to cadence, LiveMetric.ODOMETER to odometer)), true)
            heart(HeartRateMeasurement(bpm, null, emptyList()))
        }
        receive(160, 1000, 120); receive(200, 1100, 140)
        assertEquals(140, runtime.pulseValues.bpm)
        assertNull(runtime.pulseValues.average); assertNull(runtime.pulseValues.maximum)
        assertNull(runtime.dashboardBikeData.value(LiveMetric.AVERAGE_CADENCE))
        assertNull(runtime.ride.bike.distanceM)
        assertEquals(0L, runtime.gpsValues.elapsedMs)

        runtime.beginRecording()
        receive(120, 1200, 100); receive(180, 1250, 140)
        assertEquals(150L, runtime.dashboardBikeData.value(LiveMetric.AVERAGE_CADENCE))
        assertEquals(120.0, runtime.pulseValues.average!!, .001)
        assertEquals(140, runtime.pulseValues.maximum)
        assertEquals(50L, runtime.ride.bike.distanceM)
        runtime.stopRecording()
        receive(240, 1300, 180)
        assertEquals(180, runtime.pulseValues.bpm)
        assertEquals(150L, runtime.dashboardBikeData.value(LiveMetric.AVERAGE_CADENCE))
        assertEquals(120.0, runtime.pulseValues.average!!, .001)
        assertEquals(140, runtime.pulseValues.maximum)
        assertEquals(50L, runtime.ride.bike.distanceM)
        runtime.setVisible(false)
    }
}
