package de.bcycle.app

import android.graphics.Paint
import android.graphics.Path
import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.Layer
import kotlin.math.abs
import kotlin.math.roundToInt

/** Routes share the map framebuffer, including its live pinch scale and zoom pivot. */
internal class RouteTrackLayer(private val density: Float) : Layer() {
    private data class State(val route: GpxRoute?, val track: List<MapPoint>, val gps: GpsValues,
        val showRoute: Boolean, val showTrack: Boolean, val heightLabel: String? = null)
    @Volatile private var state = State(null, emptyList(), GpsValues(), false, false)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
    private val path = Path()
    private val arrow = Path()
    private val heightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(14); typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private fun dp(n: Int) = n * density
    fun update(route: GpxRoute?, track: List<MapPoint>, gps: GpsValues, showRoute: Boolean, showTrack: Boolean) {
        // Publish an immutable snapshot; drawing occurs on Mapsforge's layer thread.
        state = State(route, track, gps, showRoute, showTrack,
            gps.altitudeM?.takeIf { it.isFinite() }?.let { "${it.roundToInt()} m" }); requestRedraw()
    }
    override fun draw(boundingBox: BoundingBox, zoomLevel: Byte, canvas: Canvas, topLeftPoint: Point, rotation: Rotation) {
        val snapshot = state
        val target = AndroidGraphicFactory.getCanvas(canvas)
        val mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.tileSize)
        fun x(lon: Double): Float {
            var offset = MercatorProjection.longitudeToPixelX(lon, mapSize) - topLeftPoint.x
            val distance = offset - canvas.width / 2.0
            if (distance > mapSize / 2.0) offset -= mapSize else if (distance < -mapSize / 2.0) offset += mapSize
            return offset.toFloat()
        }
        fun y(lat: Double) = (MercatorProjection.latitudeToPixelY(lat, mapSize) - topLeftPoint.y).toFloat()
        fun line(points: List<MapPoint>, color: Int, planned: Boolean) {
            path.reset(); var lastX = 0f
            points.forEachIndexed { index, point ->
                val px = x(point.longitude); val py = y(point.latitude)
                if (index == 0 || point.segmentStart || abs(px - lastX) > mapSize / 2f) path.moveTo(px, py) else path.lineTo(px, py)
                lastX = px
            }
            paint.style = Paint.Style.STROKE
            if (!planned) {
                paint.color = android.graphics.Color.WHITE; paint.strokeWidth = dp(5); target.drawPath(path, paint)
            }
            paint.color = color; paint.strokeWidth = dp(if (planned) 6 else 3); target.drawPath(path, paint)
        }
        val route = snapshot.route
        if (snapshot.showRoute && route != null && route.points.isNotEmpty()) {
            line(route.points, android.graphics.Color.argb(89, 18, 59, 117), true)
            paint.style = Paint.Style.FILL
            fun endpoint(point: MapPoint, color: Int) {
                paint.color = android.graphics.Color.WHITE; target.drawCircle(x(point.longitude), y(point.latitude), dp(7), paint)
                paint.color = color; target.drawCircle(x(point.longitude), y(point.latitude), dp(5), paint)
            }
            endpoint(route.points.first(), android.graphics.Color.rgb(0, 150, 70))
            endpoint(route.points.last(), android.graphics.Color.rgb(220, 40, 30))
        }
        if (snapshot.showTrack) line(snapshot.track, android.graphics.Color.rgb(220, 45, 30), false)
        val gps = snapshot.gps
        val latitude = gps.latitude; val longitude = gps.longitude
        if (latitude != null && longitude != null && abs(latitude) <= 85.05112878) {
            target.save(); target.translate(x(longitude), y(latitude)); paint.style = Paint.Style.FILL
            if (gps.bearingDegrees == null) {
                paint.color = android.graphics.Color.WHITE; target.drawCircle(0f, 0f, dp(11), paint)
                paint.color = android.graphics.Color.rgb(0, 100, 245); target.drawCircle(0f, 0f, dp(8), paint)
            } else {
                target.rotate(gps.bearingDegrees.toFloat())
                arrow.reset(); arrow.moveTo(0f, -dp(16)); arrow.lineTo(dp(11), dp(12))
                arrow.lineTo(0f, dp(6)); arrow.lineTo(-dp(11), dp(12)); arrow.close()
                paint.color = android.graphics.Color.WHITE; paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(5); target.drawPath(arrow, paint)
                paint.color = android.graphics.Color.rgb(0, 100, 245); paint.style = Paint.Style.FILL; target.drawPath(arrow, paint)
            }
            target.restore()
            snapshot.heightLabel?.let { text ->
                target.save(); target.translate(x(longitude), y(latitude))
                // Mapsforge rotates the whole framebuffer; keep the attached label upright.
                target.rotate(-rotation.degrees)
                val left = dp(18); val inset = dp(5)
                val metrics = heightPaint.fontMetrics
                val halfHeight = (metrics.descent - metrics.ascent) / 2 + dp(3)
                heightPaint.color = android.graphics.Color.argb(220, 255, 255, 255)
                target.drawRoundRect(left, -halfHeight, left + heightPaint.measureText(text) + inset * 2,
                    halfHeight, dp(4), dp(4), heightPaint)
                heightPaint.color = android.graphics.Color.BLACK
                target.drawText(text, left + inset, -(metrics.ascent + metrics.descent) / 2, heightPaint)
                target.restore()
            }
        }
    }
}
