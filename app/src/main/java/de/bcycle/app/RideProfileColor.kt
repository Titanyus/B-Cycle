package de.bcycle.app

import android.content.Context
import org.json.JSONObject

/** Local ride metadata; deliberately independent of FIT bytes and upload receipts. */
internal object RideProfileColor {
    private const val STORE="ride-profile-colors"
    fun snapshot(context:Context):String {
        val prefs=context.getSharedPreferences("bike",0)
        val bikes=BikeProfileStore(context,prefs)
        return JSONObject().put("slot",bikes.activeSlot).put("bikeId",BikeIdentities.id(prefs,bikes.activeSlot)).put("primary",ThemeManager(prefs).baseTokens.primary).put("name",bikes.current().name).put("type",bikes.current().type.name).toString()
    }
    fun assign(context:Context,id:String,slot:Int) {
        val prefs=context.getSharedPreferences("bike",0);val bikes=BikeProfileStore(context,prefs)
        val profile=bikes.all().singleOrNull { it.slot==slot } ?: return
        val stored=JSONObject(prefs.getString(BikeUiConfigurationStore.KEY,"{}")!!).optJSONObject(slot.toString())
            ?.optJSONObject(ThemeManager.activeKey)?.optString("value")
        val primary=if(bikes.activeSlot==slot) ThemeManager(prefs).baseTokens.primary else
            BicycleThemes.findPreset(stored)?.tokens?.primary ?: ThemeManager(prefs).baseTokens.primary
        confirm(context,id,JSONObject().put("slot",slot).put("bikeId",BikeIdentities.id(prefs,slot)).put("name",profile.name).put("primary",primary).put("type",profile.type.name).toString())
    }
    fun forget(context:Context,id:String) { check(context.getSharedPreferences(STORE,0).edit().remove(id).commit()) }
    fun confirm(context:Context,id:String,json:String) {
        val row=JSONObject(json);row.getInt("slot");row.getInt("primary")
        if(row.has("bikeId")) require(BikeIdentities.valid(row.getString("bikeId")))
        if(row.has("type")) BikeType.valueOf(row.getString("type"))
        check(context.getSharedPreferences(STORE,0).edit().putString(id,row.toString()).commit())
    }
    fun slot(context:Context,id:String):Int?=runCatching {
        context.getSharedPreferences(STORE,0).getString(id,null)?.let(::JSONObject)?.let { resolvedSlot(context,it) }
    }.getOrNull()
    fun type(context:Context,id:String):BikeType?=runCatching {
        val row=JSONObject(context.getSharedPreferences(STORE,0).getString(id,null) ?: return null)
        val prefs=context.getSharedPreferences("bike",0)
        val slot=resolvedSlot(context,row)
        val profiles=org.json.JSONArray(prefs.getString(BikeProfileStore.KEY,"[]")!!)
        val live=(0 until profiles.length()).map { profiles.getJSONObject(it) }.find { it.optInt("slot")==slot }
            ?.optString("type")?.takeIf { it.isNotBlank() }
        val retained=row.optString("bikeId").takeIf { it.isNotBlank() }?.let { BikeIdentities.retainedType(prefs,it)?.name }
        BikeType.valueOf(live ?: retained ?: row.getString("type"))
    }.getOrNull()
    fun snapshotType(context:Context,id:String):BikeType?=runCatching {
        BikeType.valueOf(JSONObject(context.getSharedPreferences(STORE,0).getString(id,null) ?: return null).getString("type"))
    }.getOrNull()
    fun name(context:Context,id:String):String?=runCatching {
        val row=context.getSharedPreferences(STORE,0).getString(id,null)?.let(::JSONObject) ?: return null
        val prefs=context.getSharedPreferences("bike",0)
        val profiles=org.json.JSONArray(prefs.getString(BikeProfileStore.KEY,"[]")!!)
        val slot=resolvedSlot(context,row)
        (0 until profiles.length()).map { profiles.getJSONObject(it) }.firstOrNull { it.optInt("slot")==slot }
            ?.optString("name")?.takeIf { it.isNotBlank() } ?: row.optString("name").takeIf { it.isNotBlank() }
    }.getOrNull()
    fun color(context:Context,id:String):Int?=runCatching {
        val row=context.getSharedPreferences(STORE,0).getString(id,null)?.let(::JSONObject) ?: return null
        val prefs=context.getSharedPreferences("bike",0)
        val slot=resolvedSlot(context,row) ?: return row.getInt("primary")
        val profiles=org.json.JSONArray(prefs.getString(BikeProfileStore.KEY,"[]")!!)
        if((0 until profiles.length()).none { profiles.getJSONObject(it).optInt("slot")==slot }) return row.getInt("primary")
        if(prefs.getInt(BikeProfileStore.ACTIVE,0)==slot)
            return BicycleThemes.findPreset(prefs.getString(ThemeManager.activeKey,null))?.tokens?.primary ?: row.getInt("primary")
        val stored=JSONObject(prefs.getString(BikeUiConfigurationStore.KEY,"{}")!!)
            .optJSONObject(slot.toString())?.optJSONObject(ThemeManager.activeKey)?.optString("value")
        BicycleThemes.findPreset(stored)?.tokens?.primary ?: row.getInt("primary")
    }.getOrNull()
    private fun resolvedSlot(context:Context,row:JSONObject):Int? =
        if(row.has("bikeId")) BikeIdentities.slot(context,row.getString("bikeId")) else row.getInt("slot")
    fun backup(context:Context,id:String):JSONObject? {
        val value=context.getSharedPreferences(STORE,0).getString(id,null) ?: return null
        val row=JSONObject(value)
        if(!row.has("bikeId")) row.put("bikeId",BikeIdentities.id(context.getSharedPreferences("bike",0),row.getInt("slot")))
        type(context,id)?.let { row.put("type",it.name) }
        return row
    }
    fun ensureIdentities(context:Context) {
        val prefs=context.getSharedPreferences(STORE,0);val edit=prefs.edit();var changed=false
        val profiles=org.json.JSONArray(context.getSharedPreferences("bike",0).getString(BikeProfileStore.KEY,"[]")!!)
        prefs.all.forEach { (id,value) -> if(value is String) runCatching {
            val row=JSONObject(value)
            if(!row.has("bikeId")) {
                row.put("bikeId",BikeIdentities.id(context.getSharedPreferences("bike",0),row.getInt("slot")))
                edit.putString(id,row.toString());changed=true
            }
            if(!row.has("type")) {
                val slot=resolvedSlot(context,row)
                (0 until profiles.length()).map { profiles.getJSONObject(it) }.find { it.optInt("slot")==slot }
                    ?.optString("type")?.takeIf { value -> BikeType.entries.any { it.name==value } }?.let {
                        row.put("type",it);edit.putString(id,row.toString());changed=true
                    }
            }
        } }
        if(changed) check(edit.commit())
    }
}
