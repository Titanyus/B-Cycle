package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal data class DashboardPage(val id: Int, val name: String)

/** Stable slots preserve all existing field/graph keys. Deletion never renumbers another page. */
internal class DashboardPages(private val context: Context, private val prefs: SharedPreferences) {
    companion object {
        const val KEY="dashboardPages"
        const val MAX=6
        fun fieldKey(id:Int)=if(id==0) "dashboard" else "dashboard_$id"
        fun keys(id:Int)=listOf(fieldKey(id),"dashboardTemplate_$id",GraphOverlayConfig.key(id),
            GraphDisplaySettings.key(id),ZoneGlowConfig.key(id),DashboardDataVisibility.key(id),DashboardTrendIndicators.key(id),DashboardTrainingHelp.key(id))
    }
    fun all():List<DashboardPage> {
        val saved=runCatching { JSONArray(prefs.getString(KEY,null)) }.getOrNull()
        if(saved!=null) {
            val pages=(0 until saved.length()).mapNotNull { i ->
                val row=saved.optJSONObject(i) ?: return@mapNotNull null
                val id=row.optInt("id",-1);val name=row.optString("name").trim()
                if(id !in 0 until MAX || name.isEmpty() || name.length>60 || name.any(Char::isISOControl)) null else DashboardPage(id,name)
            }.distinctBy { it.id }.take(MAX)
            if(pages.isNotEmpty()) return pages
        }
        return (0..3).map { DashboardPage(it,context.getString(R.string.dashboard_default_name,it+1)) }
    }
    fun selected():Int=prefs.getInt("dashboardPage",0).takeIf { id -> all().any { it.id==id } } ?: all().first().id
    fun select(id:Int) { if(all().any { it.id==id }) prefs.edit().putInt("dashboardPage",id).apply() }
    private fun write(rows:List<DashboardPage>,edit:SharedPreferences.Editor=prefs.edit()) {
        edit.putString(KEY,JSONArray().apply { rows.forEach { put(JSONObject().put("id",it.id).put("name",it.name)) } }.toString()).apply()
    }
    fun add():Boolean {
        val rows=all();if(rows.size>=MAX) return false
        val id=(0 until MAX).first { candidate -> rows.none { it.id==candidate } }
        val edit=prefs.edit();keys(id).forEach(edit::remove)
        edit.putString("dashboardTemplate_$id",DashboardTemplate.DATA.name).putInt("dashboardPage",id)
        write(rows+DashboardPage(id,context.getString(R.string.dashboard_default_name,id+1)),edit);return true
    }
    fun rename(id:Int,name:String):Boolean {
        val value=name.trim();val rows=all()
        if(value.isEmpty() || value.length>60 || value.any(Char::isISOControl) || rows.none { it.id==id }) return false
        write(rows.map { if(it.id==id) it.copy(name=value) else it });return true
    }
    fun delete(id:Int):Boolean {
        val rows=all();if(rows.size<=1 || rows.none { it.id==id }) return false
        val remaining=rows.filter { it.id!=id };val edit=prefs.edit()
        keys(id).forEach(edit::remove)
        if(selected()==id) edit.putInt("dashboardPage",remaining.first().id)
        write(remaining,edit);return true
    }
}
