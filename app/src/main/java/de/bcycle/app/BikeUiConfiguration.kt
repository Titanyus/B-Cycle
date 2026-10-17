package de.bcycle.app

import android.content.SharedPreferences

/** Per-bike display and map configuration, projected into existing active preferences. */
internal data class BikeUiConfiguration(val version:Int=1,val design:Map<String,Any?>,val maps:Map<String,Any?>,val selectedGpxRouteId:String?=null)
internal object BikeUiConfigurationScope {
    private val designKeys=setOf(ThemeManager.activeKey,ThemeManager.settingsLightKey,ThemeManager.customsKey,ModeGlowSettings.key,
        ModeGlowSettings.placementKey,DashboardFullscreen.key,DashboardPages.KEY,"dashboardPage",GraphDisplaySettings.key,
        MapFieldSelection.key(0),MapFieldSelection.key(1),MapFieldSelection.key(2))
    private val mapKeys=setOf("mapSource","mapNight","mapFollow","mapHeading",MapOvalAnchor.preference,"mapTrackVisible","mapGpxVisible",
        "mapZoom","mapLatitude","mapLongitude","displayFitId","displayGpxId",FitElevationCorrection.preference)
    fun designKey(key:String)=key in designKeys || (0 until DashboardPages.MAX).any { key in DashboardPages.keys(it) }
    fun mapKey(key:String)=key in mapKeys
    fun capture(prefs:SharedPreferences,selectedGpxRouteId:String?=null)=BikeUiConfiguration(design=prefs.all.filterKeys(::designKey).toMap(),maps=prefs.all.filterKeys(::mapKey).toMap(),selectedGpxRouteId=selectedGpxRouteId)
}

/** The active preferences remain the compatibility boundary for all existing controls. */
internal class BikeUiConfigurationStore(private val prefs:SharedPreferences) {
    companion object { const val KEY="bikeUiProfilesV1" }
    private fun bank()=runCatching { org.json.JSONObject(prefs.getString(KEY,"{}")!!) }.getOrDefault(org.json.JSONObject())
    private fun encode(values:Map<String,Any?>)=org.json.JSONObject().apply {
        values.forEach { (k,v) ->
            val kind=when(v) { is String->"string";is Boolean->"boolean";is Int->"int";is Long->"long";is Float->"float";is Set<*>->"set";else->return@forEach }
            put(k,org.json.JSONObject().put("type",kind).put("value",if(v is Set<*>) org.json.JSONArray(v.toList()) else v))
        }
    }
    private fun decode(row:org.json.JSONObject):Map<String,Any?> = buildMap {
        row.keys().forEach { k ->
            if(!BikeUiConfigurationScope.designKey(k) && !BikeUiConfigurationScope.mapKey(k)) return@forEach
            runCatching {
                val item=row.getJSONObject(k)
                val value:Any=when(item.getString("type")) {
                    "string"->item.getString("value");"boolean"->item.getBoolean("value")
                    "int"->item.getInt("value");"long"->item.getLong("value");"float"->item.getDouble("value").toFloat()
                    "set"->item.getJSONArray("value").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                    else->return@forEach
                }
                put(k,value)
            }
        }
    }
    private fun live()=BikeUiConfigurationScope.capture(prefs).let { it.design+it.maps }
    fun ensure(slots:List<Int>) {
        val rows=bank();val baseline=encode(live())
        slots.forEach { id ->
            if(!rows.has(id.toString())) rows.put(id.toString(),org.json.JSONObject(baseline.toString()))
            // GPX selection is migrated by the existing library worker after profile initialization.
            val row=rows.getJSONObject(id.toString())
            if(!row.has("displayGpxId") && prefs.contains("displayGpxId"))
                row.put("displayGpxId",encode(mapOf("displayGpxId" to prefs.getString("displayGpxId",""))).get("displayGpxId"))
        }
        val valid=slots.map(Int::toString).toSet()
        rows.keys().asSequence().toList().filter { it !in valid }.forEach(rows::remove)
        prefs.edit().putString(KEY,rows.toString()).apply()
    }
    fun save(slot:Int) { val rows=bank();rows.put(slot.toString(),encode(live()));prefs.edit().putString(KEY,rows.toString()).apply() }
    fun apply(slot:Int) {
        val row=bank().optJSONObject(slot.toString()) ?: return
        val values=decode(row);val edit=prefs.edit()
        prefs.all.keys.filter { BikeUiConfigurationScope.designKey(it) || BikeUiConfigurationScope.mapKey(it) }.forEach(edit::remove)
        values.forEach { (k,v) -> when(v) {
            is String->edit.putString(k,v);is Boolean->edit.putBoolean(k,v);is Int->edit.putInt(k,v)
            is Long->edit.putLong(k,v);is Float->edit.putFloat(k,v);is Set<*>->edit.putStringSet(k,v.filterIsInstance<String>().toSet())
        } }
        edit.apply()
    }
    fun copy(source:Int,target:Int) {
        val rows=bank();val row=rows.getJSONObject(source.toString())
        rows.put(target.toString(),org.json.JSONObject(row.toString()));prefs.edit().putString(KEY,rows.toString()).apply()
        apply(target)
    }
    fun remove(slot:Int) { val rows=bank();rows.remove(slot.toString());prefs.edit().putString(KEY,rows.toString()).apply() }
}
