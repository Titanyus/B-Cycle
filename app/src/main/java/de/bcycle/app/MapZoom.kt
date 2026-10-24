package de.bcycle.app

import android.content.SharedPreferences

/** Fractional camera zoom; retain the legacy integer preference for old settings readers. */
internal object MapZoom {
    const val minimum=6
    const val maximum=19
    const val preference="mapZoomFractional"
    fun maximum(source:MapSource)=minOf(maximum,source.maximumZoom)
    fun read(prefs:SharedPreferences,source:MapSource):Double =
        prefs.getFloat(preference,prefs.getInt("mapZoom",16).toFloat()).toDouble().let { if(it.isFinite()) it.coerceIn(minimum.toDouble(),maximum(source).toDouble()) else 16.0.coerceAtMost(maximum(source).toDouble()) }
    fun enableFractional() { org.mapsforge.core.util.Parameters.FRACTIONAL_ZOOM=true }
    fun margin(zoom:Double)=when {
        zoom<=11 -> .5
        zoom<12 -> .5-(zoom-11)*.1
        zoom<17 -> .4-(zoom-12)*.03
        else -> .25
    }
}
