package de.bcycle.app

import android.content.Context
import de.bcycle.hrv.Protocol

internal data class HrvMeasurementConfig(val restSeconds:Int=30,val measureSeconds:Int=180) {
    val protocol get()=Protocol(restSeconds*1000L,measureSeconds*1000L)
    val valid get()=restSeconds in 15..60 && measureSeconds in 60..300 && restSeconds%15==0 && measureSeconds%15==0
}
internal class HrvSettings(context:Context) {
    companion object { const val STORE="hrv-settings" }
    val preferences=context.getSharedPreferences(STORE,0)
    fun read()=HrvMeasurementConfig(preferences.getInt("restSeconds",30),preferences.getInt("measureSeconds",180))
        .takeIf { it.valid } ?: HrvMeasurementConfig()
    fun write(config:HrvMeasurementConfig) {
        require(config.valid)
        preferences.edit().putInt("restSeconds",config.restSeconds).putInt("measureSeconds",config.measureSeconds).apply()
    }
}
