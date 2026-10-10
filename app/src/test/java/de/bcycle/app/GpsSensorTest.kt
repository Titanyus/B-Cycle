package de.bcycle.app

import android.Manifest
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GpsSensorTest {
    @Test fun precisePermissionIsRequiredAndStoppedSensorCannotReceiveOrReviveValues() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val manager = context.getSystemService(LocationManager::class.java)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val ride = GpsRide()
        val messages = mutableListOf<String>()
        val sensor = GpsSensor(context, ride) { messages += it }
        sensor.start()
        assertFalse(sensor.active)
        assertTrue(messages.last().contains("Präzisen"))
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        sensor.start()
        assertTrue(sensor.active)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        val fix = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 50.0; longitude = 8.0; accuracy = 5f; altitude = 100.0
            speed = 5f; speedAccuracyMetersPerSecond = 0.2f; verticalAccuracyMeters = 3f
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos(); time = System.currentTimeMillis()
        }
        shadowOf(manager).simulateLocation(fix)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, ride.accepted)
        assertEquals(18.0, ride.snapshot(SystemClock.elapsedRealtime()).speedKmh!!, 0.01)
        sensor.stop()
        assertFalse(sensor.active)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        fix.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        shadowOf(manager).simulateLocation(fix)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, ride.accepted)
        assertNull(ride.snapshot(SystemClock.elapsedRealtime()).speedKmh)
    }
}
