package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

internal data class RideProfileMetadata(val slot:Int?,val type:BikeType?,val name:String?,val color:Int?,val originalType:BikeType?)
internal class RideProfileCatalog(val values:Map<String,RideProfileMetadata>,internal val raw:Map<String,String>)

/** One application preference owner at a time; no Activity retained. All cache inputs are explicit. */
internal object RideProfileCatalogCache {
    private val lock=Any()
    private var owner:Owner?=null
    private class Owner(val bike:SharedPreferences,val rides:SharedPreferences) {
        val changes=AtomicLong()
        val listener=SharedPreferences.OnSharedPreferenceChangeListener { _,_->changes.incrementAndGet() }
        var inputs:List<Any?>?=null
        var catalog:RideProfileCatalog?=null
        init { rides.registerOnSharedPreferenceChangeListener(listener) }
        fun inputs()=listOf(bike.getString(BikeProfileStore.KEY,"[]"),bike.getString(BikeIdentities.KEY,"{}"),
            bike.getString(BikeIdentities.RETAINED_TYPES,"{}"),bike.getString(BikeUiConfigurationStore.KEY,"{}"),
            bike.getString(ThemeManager.activeKey,null),bike.getInt(BikeProfileStore.ACTIVE,0),changes.get())
    }
    private fun owner(c:Context):Owner {
        val bike=c.getSharedPreferences("bike",0);val rides=c.getSharedPreferences("ride-profile-colors",0)
        if(owner?.bike!==bike || owner?.rides!==rides) {
            owner?.let { it.rides.unregisterOnSharedPreferenceChangeListener(it.listener) }
            owner=Owner(bike,rides)
        }
        return checkNotNull(owner)
    }
    fun invalidate(c:Context)=synchronized(lock) { owner(c).changes.incrementAndGet();Unit }
    fun get(c:Context):RideProfileCatalog=synchronized(lock) { get(owner(c)) }
    fun metadata(c:Context,id:String):RideProfileMetadata?=synchronized(lock) {
        val state=owner(c);var snapshot=get(state)
        // Also covers a direct background restore before Android dispatches preference listeners.
        if(snapshot.raw[id]!=state.rides.getString(id,null)) { state.changes.incrementAndGet();snapshot=get(state) }
        snapshot.values[id]
    }
    private fun get(state:Owner):RideProfileCatalog {
        val inputs=state.inputs()
        if(inputs!=state.inputs || state.catalog==null) {
            val raw=state.rides.all.mapNotNull { (key,value)->(value as? String)?.let { key to it } }.toMap()
            state.catalog=build(inputs,raw);state.inputs=inputs
        }
        return checkNotNull(state.catalog)
    }
    private fun build(inputs:List<Any?>,raw:Map<String,String>):RideProfileCatalog {
        val values=runCatching {
            val rows=runCatching { JSONArray(inputs[0] as String) }.getOrDefault(JSONArray())
            val ids=runCatching { JSONObject(inputs[1] as String) }.getOrDefault(JSONObject())
            val retained=runCatching { JSONObject(inputs[2] as String) }.getOrDefault(JSONObject())
            val bank=runCatching { JSONObject(inputs[3] as String) }.getOrDefault(JSONObject())
            val profiles=(0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.takeIf { row -> row.has("slot") } }
            val slots=profiles.map { it.getInt("slot") }.groupBy { ids.optString(it.toString()) }
                .filter { (id,slots)->BikeIdentities.valid(id) && slots.size==1 }.mapValues { it.value.single() }
            val bySlot=profiles.groupBy { it.optInt("slot") }.mapValues { it.value.first() }
            raw.mapNotNull { (id,text)->runCatching {
                val row=JSONObject(text)
                val identity=row.optString("bikeId").takeIf { row.has("bikeId") }
                val slot=if(identity!=null) slots[identity] else runCatching { row.getInt("slot") }.getOrNull()
                val profile=bySlot[slot]
                val original=runCatching { BikeType.valueOf(row.getString("type")) }.getOrNull()
                val tombstone=identity?.let { runCatching { BikeType.valueOf(retained.getString(it)) }.getOrNull() }
                val live=profile?.optString("type")?.takeIf(String::isNotBlank)
                val type=runCatching { if(live!=null) BikeType.valueOf(live) else tombstone ?: original }.getOrNull()
                val name=profile?.optString("name")?.takeIf(String::isNotBlank) ?: row.optString("name").takeIf(String::isNotBlank)
                val theme=if(slot==inputs[5] && profile!=null) inputs[4] as? String else
                    bank.optJSONObject(slot?.toString())?.optJSONObject(ThemeManager.activeKey)?.optString("value")
                val color=runCatching { if(profile==null) row.getInt("primary") else BicycleThemes.findPreset(theme)?.tokens?.primary ?: row.getInt("primary") }.getOrNull()
                id to RideProfileMetadata(slot,type,name,color,original)
            }.getOrNull() }.toMap()
        }.getOrDefault(emptyMap())
        return RideProfileCatalog(values,raw)
    }
}
