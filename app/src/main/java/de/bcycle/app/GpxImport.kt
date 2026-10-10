package de.bcycle.app

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FilterInputStream
import java.io.InputStream

data class GpxRoute(val points: List<MapPoint>, val originalPoints: Int, val segments: Int,
                    val distanceM: Double? = null, val durationMs: Long? = null)

/** Streaming parser: bounded input, points, nesting and segments; no external entities. */
object GpxImport {
    const val MAX_BYTES = 20L * 1024 * 1024
    const val MAX_POINTS = 200_000
    fun read(input: InputStream): GpxRoute {
        val limited = object : FilterInputStream(input) {
            var bytes = 0L
            fun count(n: Int) { if (n > 0) { bytes += n; require(bytes <= MAX_BYTES) { "GPX ist größer als 20 MB" } } }
            override fun read(): Int = super.read().also { if (it >= 0) count(1) }
            override fun read(b: ByteArray, off: Int, len: Int): Int = `in`.read(b, off, len).also { count(it) }
        }
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        parser.setInput(limited, null)
        val points = ArrayList<MapPoint>()
        var total = 0; var segments = 0; var start = true
        var root = false; var trackDepth = -1; var segmentDepth = -1; var routeDepth = -1
        var pointDepth = -1; var firstTime: Long? = null; var lastTime: Long? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            require(parser.depth <= 32) { "GPX zu stark verschachtelt" }
            if (event == XmlPullParser.DOCDECL) error("GPX-DTD wird nicht unterstützt")
            if (event == XmlPullParser.START_TAG) {
                val name = parser.name
                if (parser.depth == 1) {
                    require(name == "gpx" && parser.namespace in listOf("", "http://www.topografix.com/GPX/1/0", "http://www.topografix.com/GPX/1/1")) { "Keine GPX-Datei" }; root = true
                }
                if (parser.depth == 2 && name == "trk") trackDepth = parser.depth
                if (parser.depth == 2 && name == "rte") { routeDepth = parser.depth; start = true }
                if (name == "trkseg" && trackDepth == 2 && parser.depth == 3) { segmentDepth = parser.depth; start = true }
                val point = (name == "trkpt" && segmentDepth == 3 && parser.depth == 4) ||
                    (name == "rtept" && routeDepth == 2 && parser.depth == 3)
                if (point) {
                    val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                    val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                    require(lat != null && lat.isFinite() && lat in -85.05112878..85.05112878 &&
                        lon != null && lon.isFinite() && lon in -180.0..180.0) { "Ungültige GPX-Koordinaten" }
                    total++; require(total <= MAX_POINTS) { "GPX enthält mehr als 200.000 Punkte" }
                    if (start) { segments++; require(segments <= 1_000) { "GPX enthält zu viele Abschnitte" } }
                    points.add(MapPoint(lat, lon, start)); start = false
                    pointDepth = parser.depth
                } else if (name == "time" && pointDepth > 0 && parser.depth == pointDepth + 1) {
                    val time = runCatching { java.time.Instant.parse(parser.nextText()).toEpochMilli() }.getOrNull()
                    if (time != null) { firstTime = minOf(firstTime ?: time, time); lastTime = maxOf(lastTime ?: time, time) }
                }
            } else if (event == XmlPullParser.END_TAG) {
                when (parser.name) {
                    "trkpt", "rtept" -> pointDepth = -1
                    "trk" -> if (parser.depth == trackDepth) trackDepth = -1
                    "trkseg" -> if (parser.depth == segmentDepth) { segmentDepth = -1; start = true }
                    "rte" -> if (parser.depth == routeDepth) { routeDepth = -1; start = true }
                }
            }
            event = parser.nextToken()
        }
        require(root && total >= 2 && points.zipWithNext().any { !it.second.segmentStart }) { "GPX enthält keine darstellbare Strecke" }
        // Keep original GPX on disk, but bound display work. Preserve both ends of every segment.
        val step = ((total + 19_999) / 20_000).coerceAtLeast(1)
        val display = points.filterIndexed { index, p -> p.segmentStart || index % step == 0 ||
            index == points.lastIndex || points[index + 1].segmentStart }
        return GpxRoute(display, total, segments, routeDistance(points),
            firstTime?.let { first -> lastTime?.minus(first)?.takeIf { it > 0 } })
    }
}
