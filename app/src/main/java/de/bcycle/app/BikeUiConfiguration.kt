package de.bcycle.app

import android.content.SharedPreferences

/** Future per-bike payload. Not applied on bike changes yet; current global behavior is preserved. */
internal data class BikeUiConfiguration(val version:Int=1,val design:Map<String,Any?>,val maps:Map<String,Any?>,val selectedGpxRouteId:String?=null)
internal object BikeUiConfigurationScope {
    private val designKeys=setOf(ThemeManager.activeKey,ThemeManager.customsKey,ModeGlowSettings.key,
        ModeGlowSettings.placementKey,DashboardFullscreen.key,DashboardPages.KEY,"dashboardPage",GraphDisplaySettings.key,
        MapFieldSelection.key(0),MapFieldSelection.key(1),MapFieldSelection.key(2))
    private val mapKeys=setOf("mapSource","mapNight","mapFollow","mapHeading",MapOvalAnchor.preference,"mapTrackVisible","mapGpxVisible",
        "mapZoom","mapLatitude","mapLongitude","displayFitId",FitElevationCorrection.preference)
    fun designKey(key:String)=key in designKeys || (0 until DashboardPages.MAX).any { key in DashboardPages.keys(it) }
    fun mapKey(key:String)=key in mapKeys
    fun capture(prefs:SharedPreferences,selectedGpxRouteId:String?=null)=BikeUiConfiguration(design=prefs.all.filterKeys(::designKey).toMap(),maps=prefs.all.filterKeys(::mapKey).toMap(),selectedGpxRouteId=selectedGpxRouteId)
}
