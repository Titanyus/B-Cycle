package de.bcycle.app

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class RideProfileTypeTest {
    @Test fun liveProfileWinsAndDeletionKeepsLastTypeWithoutAttachingRidesToReusedSlots() {
        val c=RuntimeEnvironment.getApplication();val prefs=c.getSharedPreferences("bike",0)
        val store=BikeProfileStore(c,prefs);val first=store.current()
        val ride=java.util.UUID.randomUUID().toString()
        RideProfileColor.assign(c,ride,first.slot)
        val identity=BikeIdentities.id(prefs,first.slot)
        assertEquals(BikeType.EBIKE,RideProfileColor.type(c,ride))
        store.save(first.copy(type=BikeType.INDOOR))
        assertEquals(BikeType.INDOOR,RideProfileColor.type(c,ride))
        assertEquals(BikeType.EBIKE,RideProfileColor.snapshotType(c,ride))
        val survivor=store.add()!!;store.select(survivor.slot)
        assertTrue(store.remove(first.slot))
        assertNull(RideProfileColor.slot(c,ride));assertEquals(BikeType.INDOOR,RideProfileColor.type(c,ride))
        val settings=SettingsBackup(c,ThemeManager(prefs))
        val saved=settings.export(setOf(BackupSection.PROFILE))
        prefs.edit().remove(BikeIdentities.RETAINED_TYPES).commit()
        val document=settings.decode(saved);settings.apply(settings.plan(document,document.sections.keys))
        assertEquals(BikeType.INDOOR,RideProfileColor.type(c,ride))
        val backup=RideProfileColor.backup(c,ride)!!
        assertEquals(identity,backup.getString("bikeId"));assertEquals("INDOOR",backup.getString("type"))
        // A restored/new profile may occupy the same numeric slot but has a new UUID.
        val rows=JSONArray(prefs.getString(BikeProfileStore.KEY,"[]"));val replacement=org.json.JSONObject(rows.getJSONObject(0).toString())
            .put("slot",first.slot).put("type","EBIKE")
        rows.put(replacement);prefs.edit().putString(BikeProfileStore.KEY,rows.toString()).commit()
        BikeIdentities.newProfile(prefs,first.slot)
        assertNull(RideProfileColor.slot(c,ride));assertEquals(BikeType.INDOOR,RideProfileColor.type(c,ride))
        // A portable backup carries its own fallback, independently of local tombstones.
        RideProfileColor.confirm(c,ride,backup.toString())
        prefs.edit().remove("deletedBikeTypesV1").commit()
        assertEquals(BikeType.INDOOR,RideProfileColor.type(c,ride))
    }
}
