package de.bcycle.app

import android.content.Context
import org.json.JSONObject

/** Scoped by remote instance and stable gear ID. A deleted local profile is never reused. */
internal object DreeveBikeMappings {
    private fun prefs(c:Context)=c.getSharedPreferences("dreeve-bike-mappings",0)
    fun read(c:Context,account:String):Map<String,Int> {
        val known=BikeProfileStore(c,c.getSharedPreferences("bike",0)).all().map { it.slot }.toSet()
        val data=JSONObject(prefs(c).getString("entries","{}")!!).optJSONObject(account) ?: return emptyMap()
        return data.keys().asSequence().map { it to data.getInt(it) }.filter { it.second==-1 || it.second in known }.toMap()
    }
    fun save(c:Context,account:String,mappings:Map<String,Int>) {
        val all=JSONObject(prefs(c).getString("entries","{}")!!)
        all.put(account,JSONObject(mappings));check(prefs(c).edit().putString("entries",all.toString()).commit())
    }
}
