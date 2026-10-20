package de.bcycle.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class SettingsBackupSupplementTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val prefs get()=c.getSharedPreferences("bike",0)
    @Test fun profilesNotificationsServiceConfigurationAndPortableIdsAreRestoredWithoutCredentialsOrJobs() {
        val bikes=BikeProfileStore(c,prefs);val second=bikes.add()!!
        bikes.select(second.slot);bikes.save(bikes.current().copy(name="Indoor privat",type=BikeType.INDOOR,indoorCorrection=IndoorCorrection(12,15)))
        val bikeId=BikeIdentities.id(prefs,second.slot)
        c.getSharedPreferences("message-notifications",0).edit().putBoolean("enabled",true)
            .putStringSet("packages",setOf("org.example.chat")).putInt("durationSeconds",35).putString("position","TOP").commit()
        c.getSharedPreferences("activity-service-dreeve",0).edit().putBoolean("enabled",true)
            .putString("address","https://dreeve.example/").putString("password","PRIVATE-CIPHER").commit()
        c.getSharedPreferences("intervals-settings",0).edit().putBoolean("enabled",true)
            .putString("athlete","i123").putString("password","PRIVATE-INTERVALS").commit()
        val backup=SettingsBackup(c,ThemeManager(prefs));val bytes=backup.export(BackupSection.entries.toSet())
        assertFalse(String(bytes).contains("PRIVATE-CIPHER"));assertFalse(String(bytes).contains("PRIVATE-INTERVALS"))
        prefs.edit().clear().commit()
        for(file in listOf("message-notifications","activity-service-dreeve","intervals-settings")) c.getSharedPreferences(file,0).edit().clear().commit()
        val doc=backup.decode(bytes);val plan=backup.plan(doc,doc.sections.keys);backup.apply(plan)
        val restored=BikeProfileStore(c,prefs)
        assertEquals(second.slot,restored.activeSlot)
        assertEquals("Indoor privat",restored.current().name);assertEquals(IndoorCorrection(12,15),restored.current().indoorCorrection)
        assertEquals(bikeId,BikeIdentities.id(prefs,second.slot))
        assertEquals(setOf("org.example.chat"),MessageNotificationSettings(c).packages)
        assertEquals(35,MessageNotificationSettings(c).durationSeconds)
        assertFalse(ActivityServiceSettings(c,ActivityService.DREEVE).read().enabled)
        assertEquals("https://dreeve.example/",ActivityServiceSettings(c,ActivityService.DREEVE).read().address)
        assertFalse(IntervalsSettings(c).read().enabled);assertEquals("i123",IntervalsSettings(c).read().athlete)
        assertFalse(c.getSharedPreferences("activity-service-dreeve",0).contains("password"))
        assertTrue(plan.cloudNeedsPassword)
    }
    @Test fun invalidPortableIdsAndUnknownCredentialFieldsAreRejectedBeforeAnyWrite() {
        BikeProfileStore(c,prefs)
        val backup=SettingsBackup(c,ThemeManager(prefs));val original=backup.export(setOf(BackupSection.PROFILE))
        val document=JSONObject(String(original))
        val extra=document.getJSONObject("sections").getJSONObject("riderProfile").getJSONObject("additional").getJSONObject("bike")
        extra.put(BikeIdentities.KEY,"{\"0\":\"not-a-uuid\"}")
        assertThrows(SettingsBackupFailure::class.java) { backup.decode(document.toString().toByteArray()) }
        val supplement=SettingsBackupSupplement(c)
        assertThrows(IllegalArgumentException::class.java) { supplement.plan(BackupSection.CLOUD,
            JSONObject().put("activity-service-dreeve",JSONObject().put("password","forbidden"))) }
        val invalid=JSONObject(String(original))
        val bike=invalid.getJSONObject("sections").getJSONObject("riderProfile").getJSONObject("additional").getJSONObject("bike")
        val profiles=org.json.JSONArray(bike.getString(BikeProfileStore.KEY))
        profiles.getJSONObject(0).put("slot",0.5);bike.put(BikeProfileStore.KEY,profiles.toString())
        assertThrows(SettingsBackupFailure::class.java) { backup.decode(invalid.toString().toByteArray()) }
    }
    @Test fun replacingProfilesCannotKeepOldDreeveSlotMappingsThatWereAbsentInTheBackup() {
        BikeProfileStore(c,prefs)
        val backup=SettingsBackup(c,ThemeManager(prefs));val data=backup.export(setOf(BackupSection.PROFILE))
        DreeveBikeMappings.save(c,"https://dreeve.example/",mapOf("external-bike" to 0))
        val doc=backup.decode(data);backup.apply(backup.plan(doc,doc.sections.keys))
        assertTrue(DreeveBikeMappings.read(c,"https://dreeve.example/").isEmpty())
    }
}
