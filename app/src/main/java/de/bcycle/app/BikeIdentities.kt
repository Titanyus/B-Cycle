package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.UUID

/** Portable identities are independent of profile slots and display names. */
internal object BikeIdentities {
    const val KEY="bikeIdentitiesV1"
    private val lock=Any()
    private var cachedKeys:Pair<String,String>?=null
    private var cachedSlots:Map<String,Int> = emptyMap()
    fun id(prefs:SharedPreferences,slot:Int):String=synchronized(lock) {
        require(slot>=0)
        val all=JSONObject(prefs.getString(KEY,"{}")!!)
        all.optString(slot.toString()).takeIf { valid(it) } ?: UUID.randomUUID().toString().also {
            all.put(slot.toString(),it);check(prefs.edit().putString(KEY,all.toString()).commit())
        }
    }
    fun valid(value:String)=runCatching { UUID.fromString(value).toString()==value }.getOrDefault(false)
    fun newProfile(prefs:SharedPreferences,slot:Int)=synchronized(lock) {
        val all=JSONObject(prefs.getString(KEY,"{}")!!)
        all.put(slot.toString(),UUID.randomUUID().toString())
        check(prefs.edit().putString(KEY,all.toString()).commit())
    }
    fun slot(context:Context,id:String):Int? {
        if(!valid(id)) return null
        val prefs=context.getSharedPreferences("bike",0)
        val keys=prefs.getString(KEY,"{}")!! to prefs.getString(BikeProfileStore.KEY,"[]")!!
        return synchronized(lock) {
            if(cachedKeys!=keys) {
                val ids=JSONObject(keys.first);val profiles=org.json.JSONArray(keys.second)
                cachedSlots=(0 until profiles.length()).map { profiles.getJSONObject(it).getInt("slot") }
                    .groupBy { ids.optString(it.toString()) }.filter { (uuid,slots) -> valid(uuid) && slots.size==1 }.mapValues { it.value.single() }
                cachedKeys=keys
            }
            cachedSlots[id]
        }
    }
}
