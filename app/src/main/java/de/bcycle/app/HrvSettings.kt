package de.bcycle.app

import android.content.Context
import de.bcycle.hrv.Protocol

internal data class HrvMeasurementConfig(val restSeconds:Int=30,val measureSeconds:Int=180,
    val showOnFirstStart:Boolean=false,val vibrateAtEnd:Boolean=false,val soundAtEnd:Boolean=false) {
    val protocol get()=Protocol(restSeconds*1000L,measureSeconds*1000L)
    val valid get()=restSeconds in 15..60 && measureSeconds in 60..300 && restSeconds%15==0 && measureSeconds%15==0
}
internal class HrvSettings(context:Context) {
    companion object { const val STORE="hrv-settings" }
    val preferences=context.getSharedPreferences(STORE,0)
    fun read()=HrvMeasurementConfig(preferences.getInt("restSeconds",30),preferences.getInt("measureSeconds",180),
        preferences.getBoolean("showOnFirstStart",false),preferences.getBoolean("vibrateAtEnd",false),preferences.getBoolean("soundAtEnd",false))
        .takeIf { it.valid } ?: HrvMeasurementConfig()
    fun write(config:HrvMeasurementConfig) {
        require(config.valid)
        preferences.edit().putInt("restSeconds",config.restSeconds).putInt("measureSeconds",config.measureSeconds)
            .putBoolean("showOnFirstStart",config.showOnFirstStart).putBoolean("vibrateAtEnd",config.vibrateAtEnd).putBoolean("soundAtEnd",config.soundAtEnd).apply()
    }
}

/** Local launch bookkeeping, deliberately excluded from portable settings backups. */
internal class HrvDailyLaunch(context:Context) {
    private val settings=HrvSettings(context)
    private val launches=context.getSharedPreferences("hrv-launch-state",0)
    fun claim(day:java.time.LocalDate=java.time.LocalDate.now()):Boolean {
        val today=day.toString()
        if(launches.getString("lastDay",null)==today) return false
        if(!launches.edit().putString("lastDay",today).commit()) return false
        return settings.read().showOnFirstStart
    }
}
