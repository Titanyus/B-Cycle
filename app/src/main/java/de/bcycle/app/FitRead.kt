package de.bcycle.app

import com.garmin.fit.Decode
import com.garmin.fit.MesgListener
import java.io.File

internal data class ReadFit(val details: RouteDetails, val route: GpxRoute?)

/** Garmin's official streaming decoder. Never accumulate sensor records in memory. */
internal object FitRead {
    const val MAX_BYTES = 100L * 1024 * 1024
    fun read(file: File): ReadFit {
        require(file.length() in 14..MAX_BYTES) { "FIT-Datei leer oder größer als 100 MB" }
        require(file.inputStream().buffered().use { Decode().checkFileIntegrity(it) }) { "FIT-Datei unvollständig oder Prüfsumme ungültig" }
        val track = MapTrack(); var count = 0; var positions = 0
        var date: Long? = null; var first: Long? = null; var last: Long? = null
        var distance: Double? = null; var duration: Double? = null
        var sessionDistance = 0.0; var sessionDuration = 0.0; var hasDistance = false; var hasDuration = false
        var sessionAscent = 0.0; var hasAscent = false
        val moving = MovingTime(); var sessionMoving = 0.0; var hasMoving = false
        val elevation = RouteElevation()
        val bikeTypes = mutableSetOf<BikeType>()
        val listener = MesgListener { message ->
            fun value(name: String) = message.getFieldDoubleValue(name)?.takeIf { it.isFinite() }
            if(message.num == 12 || message.num == 18) {
                if(value("sport") == 2.0) when(value("sub_sport")?.toInt()) {
                    0 -> bikeTypes.add(BikeType.BIKE)
                    6 -> bikeTypes.add(BikeType.INDOOR)
                    28 -> bikeTypes.add(BikeType.EBIKE)
                }
            }
            when (message.num) {
                0 -> value("time_created")?.let { date = (it * 1000 + 631065600000L).toLong() }
                20 -> {
                    count++; require(count <= 600_000) { "FIT enthält zu viele Messpunkte" }
                    val stamp = value("timestamp")?.let { (it * 1000 + 631065600000L).toLong() }
                    if (stamp != null) { first = minOf(first ?: stamp, stamp); last = maxOf(last ?: stamp, stamp) }
                    moving.add(stamp, value("enhanced_speed") ?: value("speed")
                        ?: FitDeveloperValues.number(message,"SPEED","m/s") ?: FitDeveloperValues.number(message,"GPS_SPEED","m/s"))
                    val lat = value("position_lat")?.times(180.0 / 2147483648.0)
                    val lon = value("position_long")?.times(180.0 / 2147483648.0)
                    val valid = lat != null && lon != null && lat in -85.05112878..85.05112878 && lon in -180.0..180.0
                    elevation.add(if (valid) value("enhanced_altitude") ?: value("altitude") else null)
                    if (valid) positions++
                    track.append(RideSample(stamp ?: 0, stamp ?: count * 1000L, if (valid) lat else null, if (valid) lon else null))
                    (value("distance") ?: FitDeveloperValues.number(message,"GPS_DISTANCE","m")
                        ?: FitDeveloperValues.number(message,"BIKE_DISTANCE","m"))?.takeIf { it >= 0 }?.let { distance = maxOf(distance ?: 0.0, it) }
                }
                18 -> {
                    value("total_moving_time")?.takeIf { it >= 0 }?.let { sessionMoving += it; hasMoving = true }
                    value("total_ascent")?.takeIf { it >= 0 }?.let { sessionAscent += it; hasAscent = true }
                    value("total_distance")?.takeIf { it >= 0 }?.let { sessionDistance += it; hasDistance = true }
                    value("total_timer_time")?.takeIf { it >= 0 }?.let { sessionDuration += it; hasDuration = true }
                }
            }
        }
        require(file.inputStream().buffered().use { Decode().read(it, listener) }) { "FIT-Datei kann nicht gelesen werden" }
        if (hasDistance) distance = sessionDistance
        if (hasDuration) duration = sessionDuration
        val points = track.points.toList()
        val usable = points.zipWithNext().any { !it.second.segmentStart }
        val details = RouteDetails(date ?: first ?: file.lastModified(), distance ?: points.takeIf { usable }?.let(::routeDistance),
            duration?.times(1000)?.toLong() ?: first?.let { start -> last?.minus(start)?.takeIf { it >= 0 } }, routePreview(points), if (hasAscent) sessionAscent else elevation.ascent,
            if (hasMoving) (sessionMoving * 1000).toLong() else moving.value, bikeTypes.singleOrNull())
        return ReadFit(details, if (usable) GpxRoute(points, positions, points.count { it.segmentStart }, details.distanceM, details.durationMs, details.ascentM, details.movingMs) else null)
    }
}

/** Only our declared application, units and scalar fields can supply missing standard values. */
internal object FitDeveloperValues {
    private val appId=java.util.UUID.fromString("a4d3c076-05cc-4b59-b1ab-c4963f348248")
    fun number(message:com.garmin.fit.Mesg,name:String,units:String):Double? = message.developerFields.firstOrNull {
        it.isDefined && it.appUUID==appId && it.name==name && it.units==units && it.numValues==1
    }?.doubleValue?.takeIf { it.isFinite() && it>=0 }
}
