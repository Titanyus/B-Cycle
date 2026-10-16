package de.bcycle.app

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

data class RouteDetails(val dateMs: Long = 0, val distanceM: Double? = null, val durationMs: Long? = null,
                        val preview: List<MapPoint> = emptyList(), val ascentM: Double? = null, val movingMs: Long? = null, val bikeType: BikeType? = null) {
    val movingAverageKmh: Double? get() {
        val moving = movingMs?.takeIf { it > 0 && (durationMs == null || it <= durationMs) } ?: return null
        val distance = distanceM?.takeIf { it.isFinite() && it >= 0 } ?: return null
        return (distance * 3600 / moving).takeIf { it.isFinite() }
    }
    internal fun json() = JSONObject().put("date", dateMs).put("distance", distanceM)
        .put("bikeType", bikeType?.name ?: JSONObject.NULL).put("moving", movingMs ?: JSONObject.NULL).put("duration", durationMs).put("ascent", ascentM ?: JSONObject.NULL).put("preview", JSONArray().also { array ->
            preview.forEach { array.put(JSONArray().put(it.latitude).put(it.longitude).put(it.segmentStart)) }
        })
    companion object {
        internal fun from(json: JSONObject?) = if (json == null) RouteDetails() else RouteDetails(
            json.optLong("date"), json.optDouble("distance").takeIf { it.isFinite() && it >= 0 },
            json.optLong("duration", -1).takeIf { it >= 0 },
            json.optJSONArray("preview")?.let { array -> List(array.length().coerceAtMost(128)) { i ->
                val p = array.getJSONArray(i); MapPoint(p.getDouble(0), p.getDouble(1), p.optBoolean(2))
            } } ?: emptyList(), json.optDouble("ascent").takeIf { it.isFinite() && it >= 0 },
            json.optLong("moving", -1).takeIf { it >= 0 }, BikeType.entries.find { it.name == json.optString("bikeType") })
    }
}

internal fun routeDistance(points: List<MapPoint>): Double {
    var total = 0.0
    for (i in 1 until points.size) {
        val a = points[i - 1]; val b = points[i]
        if (!b.segmentStart) {
            val dlat = Math.toRadians(b.latitude - a.latitude); val dlon = Math.toRadians(b.longitude - a.longitude)
            val h = sin(dlat / 2).pow(2) + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dlon / 2).pow(2)
            total += 12_742_000 * asin(sqrt(h.coerceIn(0.0, 1.0)))
        }
    }
    return total
}

/** A small persistent preview; dropped segments are never connected to neighbours. */
internal fun routePreview(points: List<MapPoint>): List<MapPoint> {
    val step = ((points.size + 95) / 96).coerceAtLeast(1)
    val result = ArrayList<MapPoint>(); var gap = true
    points.forEachIndexed { i, point ->
        gap = gap || point.segmentStart
        if (i % step == 0 || i == points.lastIndex) { result += point.copy(segmentStart = gap); gap = false }
    }
    return result
}
