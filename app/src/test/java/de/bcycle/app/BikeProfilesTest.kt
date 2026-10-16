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
        assertEquals(1,store.all().size);assertEquals(0,store.activeSlot)
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
        store.add()
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
        store.add();store.select(2);store.select(1);assertEquals(setOf("CADENCE"),store.current().recording.fields)
    }
    @Test fun malformedStoredProfileFallsBackAndInactiveTypesCannotBeActivated() {
        val app=RuntimeEnvironment.getApplication();val prefs=app.getSharedPreferences("profiles-invalid",0)
        prefs.edit().putString(BikeProfileStore.KEY,"bad JSON").apply()
        val store=BikeProfileStore(app,prefs);assertEquals(1,store.all().size)
        assertEquals(1,BikeType.entries.count { it.available })
        assertTrue(BikeType.EBIKE.available)
        assertTrue(BikeSensor.GPS !in BikeType.INDOOR.sensors)
        assertFalse(BikeSensor.POWER.supported)
        assertThrows(IllegalArgumentException::class.java) { store.save(store.current().copy(name="")) }
        assertThrows(IllegalArgumentException::class.java) { store.save(store.current().copy(type=BikeType.INDOOR)) }
        assertThrows(IllegalArgumentException::class.java) { store.save(store.current().copy(wheelMm=0)) }
    }
    @Test fun oldFourSlotJsonMigratesWithoutLosingOrderActiveBindingsOrPhotos() {
        val app=RuntimeEnvironment.getApplication();val prefs=app.getSharedPreferences("old-four",0)
        val old=org.json.JSONArray()
        repeat(4) { id -> old.put(org.json.JSONObject().put("name","Old $id").put("type","EBIKE").put("category","ROAD")
            .put("wheelMm",2100+id).put("autoConnect",false).put("gpsEnabled",true).put("bikeEnabled",true).put("heartEnabled",true)
            .put("bikeAddress","address-$id").put("fields",org.json.JSONArray().put("RIDER_POWER")).put("speed","SPEED").put("distance","BIKE_DISTANCE")) }
        prefs.edit().putString(BikeProfileStore.KEY,old.toString()).putInt(BikeProfileStore.ACTIVE,2)
            .putString("address","active-address").putStringSet("recordFields",setOf("CADENCE")).apply()
        val store=BikeProfileStore(app,prefs)
        assertEquals(listOf(0,1,2,3),store.all().map { it.slot });assertEquals("active-address",store.current().bikeAddress)
        val photo=BikePhotos.file(app,2).apply { parentFile!!.mkdirs();writeText("photo") }
        assertTrue(store.move(2,0));assertEquals(2,store.activeSlot);assertEquals("Old 2",store.current().name)
        store.select(3);store.select(2)
        assertEquals(setOf("CADENCE"),store.current().recording.fields);assertEquals("photo",photo.readText())
        assertEquals(listOf(2,0,1,3),BikeProfileStore(app,prefs).all().map { it.slot })
        photo.delete()
    }
    @Test fun sixLimitDeletionAndReorderKeepStableIdsAndActiveCompatibilityKeys() {
        val app=RuntimeEnvironment.getApplication();val prefs=app.getSharedPreferences("dynamic-six",0)
        val store=BikeProfileStore(app,prefs)
        repeat(5) { assertNotNull(store.add()) };assertNull(store.add());assertEquals(6,store.all().size)
        store.select(4);store.save(store.current().copy(name="Four",bikeAddress="sensor-4",wheelMm=2400))
        assertTrue(store.move(4,0));assertEquals(4,store.activeSlot);assertEquals("sensor-4",prefs.getString("address",null))
        assertFalse(store.remove(4));assertTrue(store.remove(5))
        val new=store.add()!!;assertEquals(6,new.slot);assertNull(store.add()) // deleted identities are never reused
        store.select(new.slot);assertNull(store.current().bikeAddress)
        store.select(4);assertEquals("Four",store.current().name);assertEquals(2400,store.current().wheelMm)
        assertFalse(store.move(4,6));assertFalse(store.move(99,0));assertFalse(store.remove(99))
        assertEquals(store.all(),BikeProfileStore(app,prefs).all())
    }
    @Test fun finalBikeCannotBeDeletedAndIndoorHasOnlyTrainerAndHeart() {
        val app=RuntimeEnvironment.getApplication();val store=BikeProfileStore(app,app.getSharedPreferences("last-bike",0))
        assertFalse(store.remove(0));assertEquals(1,store.all().size)
        assertEquals(listOf(BikeSensor.TRAINER,BikeSensor.HEART),BikeType.INDOOR.sensors)
        assertFalse(BikeType.INDOOR.available)
    }

}
