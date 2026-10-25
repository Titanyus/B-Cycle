package de.bcycle.app

import android.content.Context
import java.time.LocalDate

/** Local launch bookkeeping, not profile configuration and deliberately not a portable setting. */
internal class SensorSetupLaunch(context:Context) {
    private val profiles=context.getSharedPreferences("bike",0)
    private val state=context.getSharedPreferences("sensor-setup-launch-state",0)
    companion object {
        fun missing(profile:BikeProfile)=when(profile.type) {
            BikeType.EBIKE->profile.bikeAddress.isNullOrBlank()
            BikeType.INDOOR->profile.trainerAddress.isNullOrBlank()
            BikeType.BIKE->false
        }
    }
    fun identity(profile:BikeProfile)=BikeIdentities.id(profiles,profile.slot)
    fun shouldShow(profile:BikeProfile,day:LocalDate=LocalDate.now()):Boolean =
        missing(profile) && state.getString(identity(profile),null)!=day.toString()
    fun later(profile:BikeProfile,day:LocalDate=LocalDate.now()) {
        state.edit().putString(identity(profile),day.toString()).apply()
    }
}
