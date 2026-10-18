package de.bcycle.app

import android.content.Context
import org.json.JSONObject

/** Local ride metadata; deliberately independent of FIT bytes and upload receipts. */
internal object RideProfileColor {
    private const val STORE="ride-profile-colors"
    fun snapshot(context:Context):String {
        val prefs=context.getSharedPreferences("bike",0)
        val bikes=BikeProfileStore(context,prefs)
        return JSONObject().put("slot",bikes.activeSlot).put("primary",ThemeManager(prefs).baseTokens.primary).put("name",bikes.current().name).toString()
    }
    fun confirm(context:Context,id:String,json:String) {
        val row=JSONObject(json);row.getInt("slot");row.getInt("primary")
        check(context.getSharedPreferences(STORE,0).edit().putString(id,row.toString()).commit())
    }
    fun slot(context:Context,id:String):Int?=runCatching {
        context.getSharedPreferences(STORE,0).getString(id,null)?.let(::JSONObject)?.getInt("slot")
    }.getOrNull()
    fun name(context:Context,id:String):String?=runCatching {
        val row=context.getSharedPreferences(STORE,0).getString(id,null)?.let(::JSONObject) ?: return null
        val prefs=context.getSharedPreferences("bike",0)
        val profiles=org.json.JSONArray(prefs.getString(BikeProfileStore.KEY,"[]")!!)
        val slot=row.getInt("slot")
        (0 until profiles.length()).map { profiles.getJSONObject(it) }.firstOrNull { it.optInt("slot")==slot }
            ?.optString("name")?.takeIf { it.isNotBlank() } ?: row.optString("name").takeIf { it.isNotBlank() }
    }.getOrNull()
    fun color(context:Context,id:String):Int?=runCatching {
        val row=context.getSharedPreferences(STORE,0).getString(id,null)?.let(::JSONObject) ?: return null
        val prefs=context.getSharedPreferences("bike",0)
        val slot=row.getInt("slot")
        val profiles=org.json.JSONArray(prefs.getString(BikeProfileStore.KEY,"[]")!!)
        if((0 until profiles.length()).none { profiles.getJSONObject(it).optInt("slot")==slot }) return row.getInt("primary")
        if(prefs.getInt(BikeProfileStore.ACTIVE,0)==slot)
            return BicycleThemes.findPreset(prefs.getString(ThemeManager.activeKey,null))?.tokens?.primary ?: row.getInt("primary")
        val stored=JSONObject(prefs.getString(BikeUiConfigurationStore.KEY,"{}")!!)
            .optJSONObject(slot.toString())?.optJSONObject(ThemeManager.activeKey)?.optString("value")
        BicycleThemes.findPreset(stored)?.tokens?.primary ?: row.getInt("primary")
    }.getOrNull()
}
