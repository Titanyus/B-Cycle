package de.bcycle.app

import android.content.SharedPreferences

/** Derived estimate, refreshed only when the ride inputs change, never while rendering rows. */
internal class RecentRidePace(private val preferences:SharedPreferences) {
    @Volatile var value:Double?=preferences.getString(KEY,null)?.toDoubleOrNull()?.takeIf { it.isFinite() && it>0 }
        private set
    private var fingerprint=preferences.getString(INPUTS,null)
    fun update(entries:List<ManagedRoute>,now:Long=System.currentTimeMillis()):Double? {
        val rides=entries.filter { RouteKind.of(it)==RouteKind.RECORDED && !it.details.virtualRide && it.details.bikeType!=BikeType.INDOOR }
        val inputs=rides.sortedBy { it.id }.joinToString("|") { "${it.id}:${it.details.dateMs}:${it.details.movingAverageKmh}" }
        val digest=java.security.MessageDigest.getInstance("SHA-256").digest(inputs.toByteArray()).joinToString("") { "%02x".format(it) }
        if(digest==fingerprint) return value
        val from=now-28L*24*60*60*1000
        val speeds=rides.filter { it.details.dateMs in from..now }.mapNotNull { it.details.movingAverageKmh?.takeIf { n -> n.isFinite() && n>0 } }
        value=speeds.takeIf { it.isNotEmpty() }?.average()
        fingerprint=digest
        preferences.edit().putString(INPUTS,digest).apply { if(value==null) remove(KEY) else putString(KEY,value.toString()) }.apply()
        return value
    }
    companion object {
        private const val KEY="templatePace.v1"
        private const val INPUTS="templatePace.inputs.v1"
        fun duration(distanceM:Double?,paceKmh:Double?):Long? {
            if(distanceM==null || !distanceM.isFinite() || distanceM<0 || paceKmh==null || !paceKmh.isFinite() || paceKmh<=0) return null
            return (distanceM*3600/paceKmh).takeIf { it.isFinite() && it>=0 && it<Long.MAX_VALUE }?.toLong()
        }
    }
}
