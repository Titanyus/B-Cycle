package de.bcycle.app

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Only the compatibility boundary knows these removed options. Never export or apply them. */
internal object RetiredProfileSettings {
    val designKeys=setOf("modeGlowEnabled","modeGlowPlacement","bikeLightPlacement")
    const val warningKey="shiftBatteryWarning"
    fun cleanBank(bank:JSONObject):JSONObject = bank.apply {
        keys().forEach { slot -> optJSONObject(slot)?.let { row -> designKeys.forEach(row::remove) } }
    }
    fun cleanProfiles(rows:JSONArray):JSONArray = rows.apply {
        repeat(length()) { optJSONObject(it)?.remove(warningKey) }
    }
    /** Idempotent, exact-key migration; preserve all unrelated values and malformed source JSON. */
    fun purge(prefs:SharedPreferences) {
        val edit=prefs.edit();var changed=false
        designKeys.filter(prefs::contains).forEach { edit.remove(it);changed=true }
        fun json(key:String,clean:(String)->String) {
            val raw=prefs.getString(key,null) ?: return
            val next=runCatching { clean(raw) }.getOrNull() ?: return
            if(normalized(raw)==next) return
            edit.putString(key,next);changed=true
        }
        // Compare structurally through normalized JSON so formatting alone does not cause writes.
        json(BikeUiConfigurationStore.KEY) { cleanBank(JSONObject(it)).toString() }
        json(BikeProfileStore.KEY) { cleanProfiles(JSONArray(it)).toString() }
        if(changed) edit.apply()
    }
    private fun normalized(raw:String)=runCatching {
        if(raw.trimStart().startsWith("[")) JSONArray(raw).toString() else JSONObject(raw).toString()
    }.getOrNull()
}
