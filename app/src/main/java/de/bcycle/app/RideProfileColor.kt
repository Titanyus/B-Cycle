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
        assignment(context,slot)?.let { confirm(context,id,it.toString()) }
    }
    private fun assignment(context:Context,slot:Int):JSONObject? {
        val prefs=context.getSharedPreferences("bike",0);val bikes=BikeProfileStore(context,prefs)
        val profile=bikes.all().singleOrNull { it.slot==slot } ?: return null
        val stored=JSONObject(prefs.getString(BikeUiConfigurationStore.KEY,"{}")!!).optJSONObject(slot.toString())
            ?.optJSONObject(ThemeManager.activeKey)?.optString("value")
        val primary=if(bikes.activeSlot==slot) ThemeManager(prefs).baseTokens.primary else
            BicycleThemes.findPreset(stored)?.tokens?.primary ?: ThemeManager(prefs).baseTokens.primary
        return JSONObject().put("slot",slot).put("bikeId",BikeIdentities.id(prefs,slot)).put("name",profile.name).put("primary",primary).put("type",profile.type.name)
    }
    /** One preference transaction for an explicitly confirmed group; original files/links/names stay untouched. */
    internal fun reassign(context:Context,expected:Map<String,String?>,targetId:String,targetType:BikeType):Int {
        val bike=context.getSharedPreferences("bike",0)
        val slot=BikeIdentities.slot(context,targetId) ?: error("Profile changed")
        val target=assignment(context,slot) ?: error("Profile changed")
        check(target.getString("bikeId")==targetId && target.getString("type")==targetType.name)
        val prefs=context.getSharedPreferences(STORE,0)
        check(expected.all { (id,row) -> prefs.getString(id,null)==row })
        val edit=prefs.edit()
        expected.forEach { (id,text) ->
            val row=text?.let(::JSONObject) ?: JSONObject()
            target.keys().forEach { key -> row.put(key,target.get(key)) }
            edit.putString(id,row.toString())
        }
        check(BikeIdentities.id(bike,slot)==targetId && BikeProfileStore(context,bike).all().any { it.slot==slot && it.type==targetType })
        check(edit.commit());RideProfileCatalogCache.invalidate(context)
        return expected.size
    }
    fun forget(context:Context,id:String) { check(context.getSharedPreferences(STORE,0).edit().remove(id).commit());RideProfileCatalogCache.invalidate(context) }
    fun confirm(context:Context,id:String,json:String) {
        val row=JSONObject(json);row.getInt("slot");row.getInt("primary")
        if(row.has("bikeId")) require(BikeIdentities.valid(row.getString("bikeId")))
        if(row.has("type")) BikeType.valueOf(row.getString("type"))
        check(context.getSharedPreferences(STORE,0).edit().putString(id,row.toString()).commit())
        RideProfileCatalogCache.invalidate(context)
    }
    internal fun catalog(context:Context)=RideProfileCatalogCache.get(context)
    fun slot(context:Context,id:String)=RideProfileCatalogCache.metadata(context,id)?.slot
    fun type(context:Context,id:String)=RideProfileCatalogCache.metadata(context,id)?.type
    fun snapshotType(context:Context,id:String)=RideProfileCatalogCache.metadata(context,id)?.originalType
    fun name(context:Context,id:String)=RideProfileCatalogCache.metadata(context,id)?.name
    fun color(context:Context,id:String)=RideProfileCatalogCache.metadata(context,id)?.color
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
        if(changed) { check(edit.commit());RideProfileCatalogCache.invalidate(context) }
    }
}
