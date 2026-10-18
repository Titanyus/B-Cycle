package de.bcycle.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.SystemClock

/** Native GNSS only, one update/second, no network SDK or satellite display. */
class GpsSensor(private val context: Context, private val ride: GpsRide, private val changed: (String) -> Unit) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private var mapObservation:GpsFix?=null
    internal fun mapFix(now:Long):GpsFix?=mapObservation?.takeIf {
        active && now-it.timeMs in 0..5000 && it.latitude.isFinite() && it.latitude in -85.05112878..85.05112878 &&
            it.longitude.isFinite() && it.longitude in -180.0..180.0 && it.accuracyM.isFinite() && it.accuracyM in 0.0..1000.0
    }
    var active = false; private set
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (!active) return
            val msl = Build.VERSION.SDK_INT >= 34 && location.hasMslAltitude() && location.hasMslAltitudeAccuracy()
            val altitude = if (msl) location.mslAltitudeMeters else if (location.hasAltitude()) location.altitude else null
            val vertical = if (msl) location.mslAltitudeAccuracyMeters.toDouble() else
                if (location.hasVerticalAccuracy()) location.verticalAccuracyMeters.toDouble() else null
            val fix = GpsFix(location.elapsedRealtimeNanos / 1_000_000, location.latitude, location.longitude,
                if (location.hasAccuracy()) location.accuracy.toDouble() else Double.POSITIVE_INFINITY,
                if (location.hasSpeed()) location.speed.toDouble() else null,
                if (location.hasSpeedAccuracy()) location.speedAccuracyMetersPerSecond.toDouble() else null,
                altitude, vertical, msl,
                if (location.hasBearing()) location.bearing.toDouble() else null,
                if (location.hasBearingAccuracy()) location.bearingAccuracyDegrees.toDouble() else null)
            val accepted = ride.accept(fix, SystemClock.elapsedRealtime())
            // Preserve all existing plausibility filters for precise fixes; only a coarse
            // accuracy estimate may bypass recording rejection for the display-only circle.
            if((accepted || fix.accuracyM>20) && mapObservation?.timeMs?.let { fix.timeMs>it }!=false) mapObservation=fix
            changed(if (accepted) "GPS · Live" else "GPS · Warte auf brauchbare Messung")
        }
        override fun onProviderEnabled(provider: String) { if (active) changed("GPS · Suche Position") }
        override fun onProviderDisabled(provider: String) {
            if (active) { mapObservation=null;ride.pause(); changed("GPS · Standort am Handy einschalten") }
        }
    }

    fun start() {
        if (active) return
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            changed("GPS · Präzisen Standort erlauben"); return
        }
        if (!manager.hasProvider(LocationManager.GPS_PROVIDER)) { changed("GPS · Nicht verfügbar"); return }
        try {
            active = true
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                LocationRequest.Builder(1000).setMinUpdateIntervalMillis(1000).setMinUpdateDistanceMeters(0f).build(),
                context.mainExecutor, listener)
            changed(if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) "GPS · Suche Position" else "GPS · Standort am Handy einschalten")
        } catch (_: SecurityException) { active = false; changed("GPS · Präzisen Standort erlauben") }
        catch (_: IllegalArgumentException) { active = false; changed("GPS · Nicht verfügbar") }
    }

    fun stop() {
        active = false;mapObservation=null
        manager.removeUpdates(listener)
        ride.pause()
    }
}
