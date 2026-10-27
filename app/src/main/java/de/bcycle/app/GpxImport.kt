package de.bcycle.app

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FilterInputStream
import java.io.InputStream

data class GpxRoute(val points: List<MapPoint>, val originalPoints: Int, val segments: Int,
                    val distanceM: Double? = null, val durationMs: Long? = null, val ascentM: Double? = null, val movingMs: Long? = null)

internal data class GpxMeasurement(val point:MapPoint,val time:Long?,val height:Double?,val speed:Double?,
    val power:Double?,val cadence:Double?,val heart:Double?,val temperature:Double?,val intervalSpeed:Double?=speed)

/** Streaming parser: bounded input, points, nesting and segments; no external entities. */
object GpxImport {
    const val MAX_BYTES = 20L * 1024 * 1024
    const val MAX_POINTS = 200_000
    fun read(input: InputStream): GpxRoute = read(input, null)
    internal fun read(input: InputStream, observed: ((MapPoint, Long?, Double?, Double?) -> Unit)?): GpxRoute = parse(input,observed,null)
    internal fun samples(input:InputStream,observed:(GpxMeasurement)->Unit):GpxRoute = parse(input,null,observed)
    private fun parse(input:InputStream,observed:((MapPoint,Long?,Double?,Double?)->Unit)?,samples:((GpxMeasurement)->Unit)?):GpxRoute {
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
        val moving = MovingTime();val motion=GpxMotion(); var nativeSpeed:Double?=null; var pointTime: Long? = null; var previousPointTime: Long? = null
        var previousPoint: MapPoint? = null
        val elevation = RouteElevation(); var pointHeight: Double? = null; var heightStart = false
        var extensionDepth=-1;var garminDepth=-1
        var power:Double?=null;var cadence:Double?=null;var heart:Double?=null;var temperature:Double?=null
        val gpxNamespaces=setOf("","http://www.topografix.com/GPX/1/1","http://www.topografix.com/GPX/1/0")
        val garminFields=setOf("hr","cad","atemp")
        val garminNamespaces=setOf("http://www.garmin.com/xmlschemas/TrackPointExtension/v1","http://www.garmin.com/xmlschemas/TrackPointExtension/v2")
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
                    points.add(MapPoint(lat, lon, start)); heightStart = start; pointHeight = null; pointTime = null; start = false
                    pointDepth = parser.depth;extensionDepth=-1;garminDepth=-1
                    power=null;cadence=null;heart=null;temperature=null;nativeSpeed=null
                } else if(name=="extensions" && pointDepth>0 && parser.depth==pointDepth+1) {
                    extensionDepth=parser.depth
                } else if(name=="TrackPointExtension" && extensionDepth>0 && parser.depth==extensionDepth+1 && parser.namespace in garminNamespaces) {
                    garminDepth=parser.depth
                } else if(name=="power" && extensionDepth>0 && parser.depth==extensionDepth+1 && parser.namespace in gpxNamespaces) {
                    power=parser.nextText().toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..65534.0 }
                } else if(garminDepth>0 && parser.depth==garminDepth+1 && parser.namespace in garminNamespaces && name in garminFields) {
                    val value=parser.nextText().toDoubleOrNull()?.takeIf { it.isFinite() }
                    when(name) {
                        "hr"->heart=value?.takeIf { it in 1.0..254.0 }
                        "cad"->cadence=value?.takeIf { it in 0.0..254.0 }
                        "atemp"->temperature=value?.takeIf { it in -100.0..100.0 }
                    }
                } else if(name=="speed" && pointDepth>0 && parser.depth==pointDepth+1 && parser.namespace in setOf("","http://www.topografix.com/GPX/1/0")) {
                    nativeSpeed=parser.nextText().toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..35.0 }
                } else if (name == "ele" && pointDepth > 0 && parser.depth == pointDepth + 1) {
                    pointHeight = parser.nextText().toDoubleOrNull()
                } else if (name == "time" && pointDepth > 0 && parser.depth == pointDepth + 1) {
                    val time = runCatching { java.time.Instant.parse(parser.nextText()).toEpochMilli() }.getOrNull()
                    pointTime = time
                    if (time != null) { firstTime = minOf(firstTime ?: time, time); lastTime = maxOf(lastTime ?: time, time) }
                }
            } else if (event == XmlPullParser.END_TAG) {
                if(parser.depth==extensionDepth) extensionDepth=-1
                if(parser.depth==garminDepth) garminDepth=-1
                when (parser.name) {
                    "trkpt", "rtept" -> if (parser.depth == pointDepth) {
                        elevation.add(pointHeight, heightStart)
                        val current = points.last()
                        val dt = pointTime?.let { stamp -> previousPointTime?.let { stamp - it } }
                        val intervalSpeed = if (!heightStart && dt != null && dt > 0 && previousPoint != null)
                            routeDistance(listOf(previousPoint!!, current)) * 1000 / dt else null
                        val sample=GpxMeasurement(current,pointTime,pointHeight,nativeSpeed,power,cadence,heart,temperature,intervalSpeed)
                        val speed=motion.speed(sample)
                        // A segment's first point establishes time only; no cross-segment movement.
                        if (heightStart) moving.add(null, null)
                        moving.add(pointTime, speed ?: if (heightStart && pointTime != null) 0.0 else null)
                        observed?.invoke(current, pointTime, pointHeight, speed)
                        samples?.invoke(sample.copy(speed=speed))
                        previousPointTime = pointTime; previousPoint = current; pointDepth = -1
                    }
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
            firstTime?.let { first -> lastTime?.minus(first)?.takeIf { it > 0 } }, elevation.ascent, moving.value)
    }
}
