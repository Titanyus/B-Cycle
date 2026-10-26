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
internal class RouteTrackLayer(private val density: Float,private val position:org.mapsforge.map.model.MapViewPosition?=null) : Layer() {
    private var symbolScale=1f
    private data class State(val route: GpxRoute?, val track: List<MapPoint>, val gps: GpsValues,
        val showRoute: Boolean, val showTrack: Boolean, val heightLabel: String? = null, val night: Boolean = false, val phoneHeading:Double?=null,
        val recordedStyle:MapLineStyle=MapRouteStyles.default(MapLineKind.RECORDED),val savedStyle:MapLineStyle=MapRouteStyles.default(MapLineKind.SAVED))
    @Volatile private var state = State(null, emptyList(), GpsValues(), false, false)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
    private val path = Path()
    private val lineBounds=android.graphics.RectF()
    private val arrow = Path()
    private var showAccuracy=false
    private val heightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(14); typeface = BicycleDesign.strongTypeface
    }
    private fun dp(n: Int) = n * density / symbolScale
    fun update(route: GpxRoute?, track: List<MapPoint>, gps: GpsValues, showRoute: Boolean, showTrack: Boolean, night: Boolean = false, phoneHeading:Double?=null,
        recordedStyle:MapLineStyle=MapRouteStyles.default(MapLineKind.RECORDED),savedStyle:MapLineStyle=MapRouteStyles.default(MapLineKind.SAVED,night)) {
        // Publish an immutable snapshot; drawing occurs on Mapsforge's layer thread.
        state = State(route, track, gps, showRoute, showTrack,
            gps.altitudeM?.takeIf { it.isFinite() }?.let { "${it.roundToInt()} m" }, night,phoneHeading?.takeIf { it.isFinite() }?.let(PhoneHeadingFilter::normalize),recordedStyle,savedStyle); requestRedraw()
    }
    override fun draw(boundingBox: BoundingBox, zoomLevel: Byte, canvas: Canvas, topLeftPoint: Point, rotation: Rotation) {
        val snapshot = state
        // Geographic coordinates stay in the integer tile frame. Only screen-sized artwork
        // compensates the fractional scale applied later by Mapsforge's framebuffer.
        symbolScale=if(org.mapsforge.core.util.Parameters.FRACTIONAL_ZOOM && position!=null)
            (position.scaleFactor/Math.pow(2.0,zoomLevel.toDouble())).toFloat().coerceAtLeast(.001f) else 1f
        heightPaint.textSize=dp(14)
        val target = AndroidGraphicFactory.getCanvas(canvas)
        val mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.tileSize)
        fun x(lon: Double): Float {
            var offset = MercatorProjection.longitudeToPixelX(lon, mapSize) - topLeftPoint.x
            val distance = offset - canvas.width / 2.0
            if (distance > mapSize / 2.0) offset -= mapSize else if (distance < -mapSize / 2.0) offset += mapSize
            return offset.toFloat()
        }
        fun y(lat: Double) = (MercatorProjection.latitudeToPixelY(lat, mapSize) - topLeftPoint.y).toFloat()
        fun line(points: List<MapPoint>, style: MapLineStyle, planned: Boolean) {
            if(style.opacity==0) return
            path.reset(); var lastX = 0f
            points.forEachIndexed { index, point ->
                val px = x(point.longitude); val py = y(point.latitude)
                if (index == 0 || point.segmentStart || abs(px - lastX) > mapSize / 2f) path.moveTo(px, py) else path.lineTo(px, py)
                lastX = px
            }
            paint.style = Paint.Style.STROKE
            val group=if(!planned && style.opacity<100) {
                path.computeBounds(lineBounds,true);lineBounds.inset(-(style.widthDp+2)*density/symbolScale,-(style.widthDp+2)*density/symbolScale)
                target.saveLayerAlpha(lineBounds,android.graphics.Color.alpha(style.argb))
            } else -1
            if (!planned) {
                paint.color = android.graphics.Color.WHITE; paint.strokeWidth = (style.widthDp+2)*density/symbolScale; target.drawPath(path, paint)
            }
            paint.color = if(group>=0) style.color else style.argb; paint.strokeWidth = style.widthDp*density/symbolScale; target.drawPath(path, paint)
            if(group>=0) target.restoreToCount(group)
        }
        val route = snapshot.route
        if (snapshot.showRoute && route != null && route.points.isNotEmpty()) {
            line(route.points, snapshot.savedStyle, true)
            paint.style = Paint.Style.FILL
            fun endpoint(point: MapPoint, color: Int) {
                paint.color = android.graphics.Color.WHITE; target.drawCircle(x(point.longitude), y(point.latitude), dp(7), paint)
                paint.color = color; target.drawCircle(x(point.longitude), y(point.latitude), dp(5), paint)
            }
            endpoint(route.points.first(), android.graphics.Color.rgb(0, 150, 70))
            endpoint(route.points.last(), android.graphics.Color.rgb(220, 40, 30))
        }
        if (snapshot.showTrack) line(snapshot.track, snapshot.recordedStyle, false)
        val gps = snapshot.gps
        val latitude = gps.latitude; val longitude = gps.longitude
        if (latitude != null && longitude != null && abs(latitude) <= 85.05112878) {
            val accuracy=gps.accuracyM?.takeIf { it.isFinite() && it>0 }
            showAccuracy=accuracy!=null && if(showAccuracy) accuracy>10 else accuracy>15
            if(showAccuracy) {
                val metresPerPixel=40075016.686*kotlin.math.cos(Math.toRadians(latitude))/mapSize
                val radius=(accuracy!!/metresPerPixel).toFloat()
                paint.style=Paint.Style.FILL;paint.color=android.graphics.Color.argb(30,0,100,245)
                target.drawCircle(x(longitude),y(latitude),radius,paint)
                paint.style=Paint.Style.STROKE;paint.strokeWidth=dp(1);paint.color=android.graphics.Color.argb(100,0,100,245)
                target.drawCircle(x(longitude),y(latitude),radius,paint)
            }
            target.save(); target.translate(x(longitude), y(latitude)); paint.style = Paint.Style.FILL
            snapshot.phoneHeading?.let { direction ->
                val saved=target.save();target.rotate(direction.toFloat())
                arrow.reset();arrow.moveTo(0f,-dp(21));arrow.lineTo(dp(5),-dp(10));arrow.lineTo(-dp(5),-dp(10));arrow.close()
                paint.color=android.graphics.Color.WHITE;paint.style=Paint.Style.STROKE;paint.strokeWidth=dp(3);target.drawPath(arrow,paint)
                paint.color=android.graphics.Color.rgb(0,100,245);paint.style=Paint.Style.FILL;target.drawPath(arrow,paint)
                target.restoreToCount(saved)
            }
            paint.style=Paint.Style.FILL
            paint.color=android.graphics.Color.WHITE;target.drawCircle(0f,0f,dp(11),paint)
            paint.color=android.graphics.Color.rgb(0,100,245);target.drawCircle(0f,0f,dp(8),paint)
            target.restore()
            snapshot.heightLabel?.let { text ->
                target.save(); target.translate(x(longitude), y(latitude))
                // Mapsforge rotates the whole framebuffer; keep the attached label upright.
                target.rotate(-rotation.degrees)
                target.scale(1f/symbolScale,1f/symbolScale)
                // Keep font metrics at their intended size too; shrinking textSize before
                // framebuffer scaling causes different glyph hinting at fractional zoom.
                heightPaint.textSize=14*density
                val left = 18*density; val inset = 5*density
                val metrics = heightPaint.fontMetrics
                val halfHeight = (metrics.descent - metrics.ascent) / 2 + 3*density
                heightPaint.color = android.graphics.Color.argb(220, 255, 255, 255)
                target.drawRoundRect(left, -halfHeight, left + heightPaint.measureText(text) + inset * 2,
                    halfHeight, 4*density, 4*density, heightPaint)
                heightPaint.color = android.graphics.Color.BLACK
                target.drawText(text, left + inset, -(metrics.ascent + metrics.descent) / 2, heightPaint)
                target.restore()
            }
        } else showAccuracy=false
    }
}
