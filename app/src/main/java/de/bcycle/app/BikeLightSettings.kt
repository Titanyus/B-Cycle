package de.bcycle.app

import android.content.SharedPreferences

internal enum class BikeLightPlacement(val label:Int) { BOTTOM(R.string.bike_light_bottom),TOP(R.string.mode_glow_top) }

/** Presentation only: never changes the physical light or its sensor value. */
internal object BikeLightSettings {
    const val key="bikeLightPlacement"
    fun placement(prefs:SharedPreferences)=runCatching { BikeLightPlacement.valueOf(prefs.getString(key,BikeLightPlacement.BOTTOM.name)!!) }.getOrDefault(BikeLightPlacement.BOTTOM)
    fun top(prefs:SharedPreferences)=DashboardFullscreen.enabled(prefs)
}
