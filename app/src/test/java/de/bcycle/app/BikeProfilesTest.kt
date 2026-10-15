package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class BikeProfilesTest {
    @Test fun migrationRetainsLegacyPairingsAndFitSelectionOnlyInBikeOneAndIsIdempotent() {
        val app=RuntimeEnvironment.getApplication();val prefs=app.getSharedPreferences("profiles-migration",0)
        prefs.edit().putString("address","00:11:22:33:44:55").putString("name","Bosch")
            .putString("heartAddress","00:11:22:33:44:66").putBoolean("gpsEnabled",true)
            .putStringSet("recordFields",setOf("GPS_SPEED","GPS_DISTANCE","ESTIMATED_FTP"))
            .putString("recordSpeed","GPS_SPEED").putString("recordDistance","GPS_DISTANCE").apply()
        val store=BikeProfileStore(app,prefs)
        assertEquals(4,store.all().size);assertEquals(0,store.activeSlot)
        val first=store.current();assertEquals("00:11:22:33:44:55",first.bikeAddress);assertEquals("Bosch",first.bikeName)
        assertEquals(setOf("GPS_SPEED","GPS_DISTANCE","ESTIMATED_FTP"),first.recording.fields)
        assertTrue(first.gpsEnabled)
        assertTrue(store.all().drop(1).all { it.bikeAddress==null && it.heartAddress==null })
        store.save(first.copy(name="Mein MTB",wheelMm=2255,category=BikeCategory.MOUNTAIN))
        assertEquals("Mein MTB",BikeProfileStore(app,prefs).current().name)
        assertEquals("Bosch",prefs.getString("name",null)) // bike name and Bluetooth device name are independent.
    }
    @Test fun namesMetadataSensorAssignmentsAndFitSourcesRemainIndependentAcrossSwitchesAndRestart() {
        val app=RuntimeEnvironment.getApplication();val prefs=app.getSharedPreferences("profiles-switch",0)
        val store=BikeProfileStore(app,prefs)
        store.save(store.current().copy(name="Berg",wheelMm=2290,bikeAddress="00:11:22:33:44:55",
            recording=RecordingConfig(setOf("GPS_SPEED","SPEED"),"GPS_SPEED","GPS_DISTANCE")))
        store.select(1)
        assertNull(prefs.getString("address",null))
        store.save(store.current().copy(name="Straße",category=BikeCategory.ROAD,wheelMm=2100,autoConnect=false,
            heartAddress="00:11:22:33:44:66",recording=RecordingConfig(setOf("RIDER_POWER"))))
        store.select(0)
        assertEquals("Berg",store.current().name);assertEquals("GPS_SPEED",prefs.getString("recordSpeed",null))
        assertEquals("00:11:22:33:44:55",prefs.getString("address",null));assertNull(prefs.getString("heartAddress",null))
        store.select(1)
        val restored=BikeProfileStore(app,prefs).current()
        assertEquals("Straße",restored.name);assertEquals(2100,restored.wheelMm);assertEquals(BikeCategory.ROAD,restored.category)
        assertFalse(restored.autoConnect);assertNull(restored.bikeAddress);assertEquals(setOf("RIDER_POWER"),restored.recording.fields)
        // Existing settings restore may still update active compatibility keys.
        prefs.edit().putStringSet("recordFields",setOf("CADENCE")).apply()
        assertEquals(setOf("CADENCE"),store.current().recording.fields)
        store.select(2);store.select(1);assertEquals(setOf("CADENCE"),store.current().recording.fields)
    }
    @Test fun malformedStoredProfileFallsBackAndInactiveTypesCannotBeActivated() {
        val app=RuntimeEnvironment.getApplication();val prefs=app.getSharedPreferences("profiles-invalid",0)
        prefs.edit().putString(BikeProfileStore.KEY,"bad JSON").apply()
        val store=BikeProfileStore(app,prefs);assertEquals(4,store.all().size)
        assertEquals(1,BikeType.entries.count { it.available })
        assertTrue(BikeType.EBIKE.available)
        assertTrue(BikeSensor.GPS !in BikeType.INDOOR.sensors)
        assertFalse(BikeSensor.POWER.supported)
        assertThrows(IllegalArgumentException::class.java) { store.save(store.current().copy(name="")) }
        assertThrows(IllegalArgumentException::class.java) { store.save(store.current().copy(type=BikeType.INDOOR)) }
        assertThrows(IllegalArgumentException::class.java) { store.save(store.current().copy(wheelMm=0)) }
    }
}
