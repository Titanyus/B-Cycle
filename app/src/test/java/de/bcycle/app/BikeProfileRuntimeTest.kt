package de.bcycle.app

import android.os.Looper
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BikeProfileRuntimeTest {
    @After fun settle() {
        val r=RideRuntime.get(RuntimeEnvironment.getApplication());r.stopRecording();r.setVisible(false)
        val end=System.nanoTime()+10_000_000_000L
        do { shadowOf(Looper.getMainLooper()).idle();if(!r.saving && !r.mapState.busy) return;Thread.sleep(10) } while(System.nanoTime()<end)
        fail("Workers did not settle")
    }
    @Test fun profileSwitchRestoresFitConfigurationAndClearsPreviousLiveSensorData() {
        val r=RideRuntime.get(RuntimeEnvironment.getApplication())
        r.bikes.save(r.bikes.current().copy(name="MTB",bikeAddress="00:11:22:33:44:55"))
        r.saveConfig(RecordingConfig(setOf("GPS_SPEED","GPS_DISTANCE"),"GPS_SPEED","GPS_DISTANCE"))
        r.onData(LiveData(riderPowerWatts=200),true)
        assertNotNull(r.bikes.add()) // New installations now start with one profile.
        assertTrue(r.switchBike(1));assertNull(r.bikeAddress);assertNull(r.bikeData.riderPowerWatts)
        assertEquals(0L,r.gpsValues.elapsedMs)
        r.saveConfig(RecordingConfig(setOf("RIDER_POWER")))
        assertTrue(r.switchBike(0));assertEquals("GPS_SPEED",r.config.speedSource)
        assertEquals(setOf("GPS_SPEED","GPS_DISTANCE"),r.config.fields)
        assertTrue(r.switchBike(1));assertEquals(setOf("RIDER_POWER"),r.config.fields)
        r.setGpsEnabled(false)
        r.beginRecording()
        assertFalse(r.gpsEnabled);assertFalse(r.gpsSensor.active)
        assertFalse(r.switchBike(0));assertEquals(1,r.bikes.activeSlot)
    }
    @Test fun manualDisconnectRetainsAssociationAndEnablementAndExplicitConnectClearsTheSessionBlock() {
        val r=RideRuntime.get(RuntimeEnvironment.getApplication())
        r.bikes.save(r.bikes.current().copy(bikeAddress="00:11:22:33:44:55",heartAddress="00:11:22:33:44:66"))
        r.setAutoConnect(false)
        assertFalse(r.bikes.current().autoConnect)
        r.disconnectSensor(BikeSensor.EBIKE);r.disconnectSensor(BikeSensor.HEART)
        assertTrue(r.bikeEnabled);assertTrue(r.heartEnabled)
        assertNotNull(r.bikeAddress);assertNotNull(r.heartAddress)
        assertTrue(r.disconnected(BikeSensor.EBIKE));assertTrue(r.disconnected(BikeSensor.HEART))
        r.setVisible(true);assertTrue(r.disconnected(BikeSensor.EBIKE))
        r.connectAssignedSensors();assertFalse(r.disconnected(BikeSensor.EBIKE))
        assertFalse(r.bikes.current().autoConnect) // manual connect does not rewrite the startup preference.
        r.removeSensor(BikeSensor.HEART);assertNull(r.heartAddress);assertNotNull(r.bikeAddress)
    }
}
