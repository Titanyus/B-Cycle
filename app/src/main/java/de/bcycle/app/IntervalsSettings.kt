package de.bcycle.app

import android.content.Context
import javax.crypto.SecretKey

internal data class IntervalsConfig(val enabled:Boolean=false,val athlete:String="",val wifiOnly:Boolean=true)
internal class IntervalsSettings(context:Context,key:(()->SecretKey)?=null) {
    private val prefs=context.getSharedPreferences("intervals-settings",Context.MODE_PRIVATE)
    private val secret=StoredSecret(prefs,"bcycle-intervals",key)
    fun read()=IntervalsConfig(prefs.getBoolean("enabled",false),prefs.getString("athlete","")!!,prefs.getBoolean("wifi",true))
    fun hasKey()=prefs.contains("password")
    fun apiKey()=secret.read()
    fun write(config:IntervalsConfig,apiKey:String?=null) {
        require(config.athlete.isEmpty() || config.athlete.matches(Regex("i?[0-9]{1,15}")))
        require(apiKey==null || (apiKey.length in 1..256 && apiKey.none { it.isWhitespace() || it.isISOControl() }))
        require(!config.enabled || (config.athlete.isNotEmpty() && (apiKey!=null || hasKey())))
        val edit=prefs.edit().putBoolean("enabled",config.enabled).putString("athlete",config.athlete).putBoolean("wifi",config.wifiOnly)
        if(apiKey!=null) edit.putString("password",secret.encode(apiKey))
        check(edit.commit())
    }
}
