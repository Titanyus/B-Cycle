package de.bcycle.app

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Global named snapshots; each bicycle keeps its own active ID and editable working copy. */
internal data class SavedMapStyle(val id:String,val name:String,val style:String)
internal object MapStyleLibrary {
    const val KEY="savedVectorStylesV1"
    const val ACTIVE="mapSavedStyleId"
    const val MAX=32
    fun validId(id:String)=runCatching { UUID.fromString(id).toString()==id }.getOrDefault(false)
    fun parse(raw:String):List<SavedMapStyle> {
        require(raw.length<=MAX*66000)
        val rows=JSONArray(raw);require(rows.length()<=MAX)
        val values=(0 until rows.length()).map { i ->
            val row=rows.getJSONObject(i);require(row.keys().asSequence().toSet()==setOf("id","name","style"))
            require(row.get("id") is String && row.get("name") is String)
            val id=row.getString("id");val name=row.getString("name")
            require(validId(id) && name==name.trim() && name.length in 1..60 && name.none { it.isISOControl() })
            val style=row.getJSONObject("style");MapStyleOverrides.parse(style)
            SavedMapStyle(id,name,style.toString())
        }
        require(values.map { it.id }.distinct().size==values.size)
        require(values.map { it.name.lowercase(java.util.Locale.ROOT) }.distinct().size==values.size)
        return values
    }
    fun read(prefs:SharedPreferences)=runCatching { parse(prefs.getString(KEY,"[]")!!) }.getOrDefault(emptyList())
    fun json(values:List<SavedMapStyle>)=JSONArray().apply {
        values.forEach { put(JSONObject().put("id",it.id).put("name",it.name).put("style",JSONObject(it.style))) }
    }
    fun save(prefs:SharedPreferences,name:String,replaceId:String?):SavedMapStyle {
        val rows=parse(prefs.getString(KEY,"[]")!!).toMutableList()
        val title=name.trim();require(title.length in 1..60 && title.none { it.isISOControl() })
        val existing=rows.firstOrNull { it.name.equals(title,true) } ?: rows.firstOrNull { it.id==replaceId }
        require(existing!=null || rows.size<MAX)
        val value=SavedMapStyle(existing?.id ?: UUID.randomUUID().toString(),title,MapStyleOverrides.read(prefs).export(prefs))
        if(existing==null) rows+=value else rows[rows.indexOf(existing)]=value
        val encoded=json(rows).toString();parse(encoded)
        prefs.edit().putString(KEY,encoded).putString(ACTIVE,value.id).apply()
        return value
    }
    fun delete(prefs:SharedPreferences,id:String) {
        val rows=parse(prefs.getString(KEY,"[]")!!).filterNot { it.id==id }
        val edit=prefs.edit().putString(KEY,json(rows).toString())
        if(prefs.getString(ACTIVE,null)==id) edit.remove(ACTIVE)
        edit.apply()
    }
    fun move(prefs:SharedPreferences,id:String,delta:Int) {
        val rows=parse(prefs.getString(KEY,"[]")!!).toMutableList();val index=rows.indexOfFirst { it.id==id }
        if(index<0 || index+delta !in rows.indices) return
        java.util.Collections.swap(rows,index,index+delta)
        prefs.edit().putString(KEY,json(rows).toString()).apply()
    }
    fun load(prefs:SharedPreferences,id:String?) {
        val json=if(id==null) MapStyleOverrides().json().put("visibility",JSONObject().apply {
            MapVectorLayer.entries.forEach { put(it.name,it.defaultVisible) }
        }).put("cycleways",true).put("cyclingStyles",JSONObject().apply {
            for(kind in listOf(MapLineKind.CYCLEWAYS,MapLineKind.CYCLE_ROUTES)) put(kind.id,MapRouteStyles.default(kind).json())
        }) else JSONObject(read(prefs).first { it.id==id }.style)
        val overrides=MapStyleOverrides.parse(json)
        val edit=prefs.edit().putString(MapStyleOverrides.KEY,overrides.json().toString()).putString("mapSource",MapSource.VECTOR.name)
        if(id==null) edit.remove(ACTIVE) else edit.putString(ACTIVE,id)
        MapVectorLayer.entries.forEach { edit.putBoolean(it.key,json.optJSONObject("visibility")?.optBoolean(it.name,it.defaultVisible) ?: it.defaultVisible) }
        edit.putBoolean(VtmBaseMap.cyclewaysKey,json.optBoolean("cycleways",true))
        val lines=runCatching { MapRouteStyles.validate(JSONObject(prefs.getString(MapRouteStyles.key,"{}")!!)) }.getOrDefault(JSONObject())
        for(kind in listOf(MapLineKind.CYCLEWAYS,MapLineKind.CYCLE_ROUTES))
            lines.put(kind.id,json.optJSONObject("cyclingStyles")?.optJSONObject(kind.id) ?: MapRouteStyles.default(kind).json())
        edit.putString(MapRouteStyles.key,lines.toString()).apply()
    }
}
