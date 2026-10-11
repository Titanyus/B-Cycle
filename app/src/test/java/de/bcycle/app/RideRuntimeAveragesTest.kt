package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RideRuntimeAveragesTest {
    @Test fun onlyNewCadenceSamplesAffectTheDerivedMeanAndSensorRecordsRemainOriginal() {
        val runtime = RideRuntime.get(RuntimeEnvironment.getApplication())
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
    }
}
