package de.bcycle.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class CleanupCompatibilityTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val p get()=c.getSharedPreferences("bike",0)
    private fun seed() {
        val store=BikeProfileStore(c,p);store.add();store.ui.save(0)
        val rows=JSONArray(p.getString(BikeProfileStore.KEY,"[]"))
        repeat(rows.length()) { rows.getJSONObject(it).put("shiftBatteryWarning",false) }
        val bank=JSONObject(p.getString(BikeUiConfigurationStore.KEY,"{}"))
        bank.keys().forEach { key ->
            val row=bank.getJSONObject(key)
            row.put("modeGlowEnabled",JSONObject().put("type","boolean").put("value",false))
            row.put("bikeLightPlacement",JSONObject().put("type","string").put("value","BOTTOM"))
            row.put("mapNight",JSONObject().put("type","boolean").put("value",true))
        }
        p.edit().putString(BikeProfileStore.KEY,rows.toString()).putString(BikeUiConfigurationStore.KEY,bank.toString())
            .putBoolean("modeGlowEnabled",false).putString("modeGlowPlacement","SIDES").putString("bikeLightPlacement","BOTTOM")
            .putString("unrelated-secret-reference","preserve").commit()
    }
    @Test fun migrationRemovesOnlyRetiredFieldsAndIsIdempotentAcrossAllProfiles() {
        seed();val before=BikeProfileStore(c,p,initialize=false).all()
        RetiredProfileSettings.purge(p);val store=BikeProfileStore(c,p)
        assertEquals(before,store.all());assertEquals("preserve",p.getString("unrelated-secret-reference",null))
        RetiredProfileSettings.designKeys.forEach { assertFalse(p.contains(it)) }
        val rows=JSONArray(p.getString(BikeProfileStore.KEY,"[]"));repeat(rows.length()) { assertFalse(rows.getJSONObject(it).has("shiftBatteryWarning")) }
        val bank=JSONObject(p.getString(BikeUiConfigurationStore.KEY,"{}"))
        bank.keys().forEach { key ->
            val row=bank.getJSONObject(key);RetiredProfileSettings.designKeys.forEach { assertFalse(row.has(it)) }
            assertTrue(row.getJSONObject("mapNight").getBoolean("value"))
        }
        val once=p.all.toMap();RetiredProfileSettings.purge(p);assertEquals(once,p.all)
    }
    @Test fun readOnlyBackupProjectionDoesNotMigrateStoredPreferencesButOmitsRetiredFields() {
        seed();val before=p.all.toMap();val store=BikeProfileStore(c,p,initialize=false)
        assertFalse(store.backupJson().contains("shiftBatteryWarning"))
        val bank=store.ui.backupBank(store.activeSlot)
        RetiredProfileSettings.designKeys.forEach { assertFalse(bank.contains(it)) }
        assertEquals(before,p.all)
    }
    @Test fun legacyFullProfileBackupIsAcceptedAndSanitizedBeforeRestoration() {
        seed();val backup=SettingsBackup(c,ThemeManager(p))
        val json=JSONObject(String(backup.export(setOf(BackupSection.PROFILE,BackupSection.DESIGN))))
        val sections=json.getJSONObject("sections")
        sections.getJSONObject(BackupSection.DESIGN.id).put("modeGlow",false).put("modeGlowPlacement","SIDES").put("bikeLightPlacement","BOTTOM")
        val additional=sections.getJSONObject(BackupSection.PROFILE.id).getJSONObject("additional").getJSONObject("bike")
        val rows=JSONArray(additional.getString(BikeProfileStore.KEY));rows.getJSONObject(0).put("shiftBatteryWarning",false)
        additional.put(BikeProfileStore.KEY,rows.toString())
        val bank=JSONObject(additional.getString(BikeUiConfigurationStore.KEY))
        bank.getJSONObject("0").put("modeGlowEnabled",JSONObject().put("type","boolean").put("value",false))
        additional.put(BikeUiConfigurationStore.KEY,bank.toString())
        val plan=backup.plan(backup.decode(json.toString().toByteArray()),setOf(BackupSection.PROFILE,BackupSection.DESIGN))
        backup.apply(plan)
        RetiredProfileSettings.designKeys.forEach { assertFalse(p.contains(it)) }
        assertFalse(p.getString(BikeProfileStore.KEY,"")!!.contains("shiftBatteryWarning"))
        assertFalse(p.getString(BikeUiConfigurationStore.KEY,"")!!.contains("modeGlowEnabled"))
        assertEquals(2,BikeProfileStore(c,p).all().size)
    }
    @Test fun malformedStoredJsonIsNotOverwrittenByCleanup() {
        p.edit().putString(BikeProfileStore.KEY,"malformed").putString(BikeUiConfigurationStore.KEY,"[").commit()
        RetiredProfileSettings.purge(p)
        assertEquals("malformed",p.getString(BikeProfileStore.KEY,null));assertEquals("[",p.getString(BikeUiConfigurationStore.KEY,null))
    }
}
