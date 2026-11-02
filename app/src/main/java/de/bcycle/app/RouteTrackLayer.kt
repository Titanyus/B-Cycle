package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.View
import org.oscim.core.MercatorProjection
import org.oscim.core.Point
import java.util.IdentityHashMap
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.*

/** GPS display and geometry publisher; all route lines/endpoints belong to VTM's GL frame. */
internal class RouteTrackLayer(context:Context,private val map:BicycleMapView):View(context) {
    private data class Geometry(val native:GlRouteGeometry)
    private data class Line(val points:List<MapPoint>,val style:MapLineStyle,val planned:Boolean)
    private val density=resources.displayMetrics.density
    private val main=Handler(Looper.getMainLooper())
    private val worker=ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,ArrayBlockingQueue(1),{ task -> Thread(task,"BicycleMapGeometry").apply { isDaemon=true } })
    private var generation=0
    private var disposed=false
    private var lines=emptyList<Line>()
    private var pointLists=emptyList<List<MapPoint>>()
    private var geometry=IdentityHashMap<List<MapPoint>,Geometry>()
    private var gps=GpsValues()
    private var phoneHeading:Double?=null
    private var showHeight=true
    private var showAccuracy=false
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeJoin=Paint.Join.ROUND;strokeCap=Paint.Cap.ROUND }
    private val heightPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize=14*density;typeface=BicycleDesign.strongTypeface }
    private val arrow=Path()
    private val nativeRoutes=VtmRouteLayer(map,density)
    private fun dp(n:Int)=n*density
    init { isClickable=false;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO;tag="map_route_overlay";map.map().layers().add(nativeRoutes) }
    fun update(route:GpxRoute?,track:List<MapPoint>,gps:GpsValues,showRoute:Boolean,showTrack:Boolean,night:Boolean=false,phoneHeading:Double?=null,
        recordedStyle:MapLineStyle=MapRouteStyles.default(MapLineKind.RECORDED),savedStyle:MapLineStyle=MapRouteStyles.default(MapLineKind.SAVED,night),showHeight:Boolean=true,overlays:List<StyledMapRoute>?=null) {
        if(disposed) return
        this.gps=gps;this.phoneHeading=phoneHeading;this.showHeight=showHeight
        val saved=overlays ?: listOfNotNull(route?.let { StyledMapRoute(it,savedStyle) })
        lines=buildList {
            if(showRoute) saved.forEach { add(Line(it.route.points,it.style,true)) }
            if(showTrack) add(Line(track,recordedStyle,false))
        }
        val next=lines.map { it.points }
        if(next.size!=pointLists.size || next.indices.any { next[it]!==pointLists[it] }) {
            pointLists=next;val token=++generation
            val previous=geometry
            // Immediately remove geometry belonging to a previous profile/visibility selection.
            geometry=IdentityHashMap<List<MapPoint>,Geometry>().apply { next.forEach { p -> previous[p]?.let { put(p,it) } } }
            worker.queue.clear()
            try { worker.execute {
                val prepared=IdentityHashMap<List<MapPoint>,Geometry>()
                next.forEach { points ->
                    if(points.isNotEmpty()) prepared[points]=previous[points] ?: prepare(points)
                }
                main.post { if(!disposed && generation==token) { geometry=prepared;publishRoutes();invalidate() } }
            } } catch(_:java.util.concurrent.RejectedExecutionException) { /* Disposed; never block the recording/UI thread. */ }
        }
        publishRoutes();invalidate()
    }
    private fun publishRoutes() {
        nativeRoutes.show(lines.mapNotNull { line -> geometry[line.points]?.let { GlRouteLine(it.native,line.style,line.planned) } })
    }
    private fun prepare(points:List<MapPoint>):Geometry {
        val xy=DoubleArray(points.size*2);val starts=BooleanArray(points.size)
        points.forEachIndexed { i,p ->
            xy[i*2]=MercatorProjection.longitudeToX(p.longitude);xy[i*2+1]=MercatorProjection.latitudeToY(p.latitude)
            starts[i]=i==0 || p.segmentStart
        }
        return Geometry(GlRouteGeometry(xy,starts))
    }
    private fun wrap(x:Double)=x-floor(x+.5)
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        if(width<=0 || height<=0) return
        val camera=map.camera;val viewport=map.map().viewport()
        val origin=Point();val east=Point();val south=Point()
        val step=100.0/(camera.scale*256)
        viewport.toScreenPoint(camera.x,camera.y,false,origin)
        viewport.toScreenPoint(camera.x+step,camera.y,false,east)
        viewport.toScreenPoint(camera.x,camera.y+step,false,south)
        val ax=(east.x-origin.x)/step;val ay=(east.y-origin.y)/step
        val bx=(south.x-origin.x)/step;val by=(south.y-origin.y)/step
        fun xy(x:Double,y:Double):Point {
            val dx=wrap(x-camera.x);val dy=y-camera.y
            return Point(origin.x+dx*ax+dy*bx,origin.y+dx*ay+dy*by)
        }
        val lat=gps.latitude;val lon=gps.longitude
        if(lat==null || lon==null || !lat.isFinite() || !lon.isFinite() || abs(lat)>85.05112878) { showAccuracy=false;return }
        val position=xy(MercatorProjection.longitudeToX(lon),MercatorProjection.latitudeToY(lat))
        val x=position.x.toFloat();val y=position.y.toFloat()
        val accuracy=gps.accuracyM?.takeIf { it.isFinite() && it>0 }
        showAccuracy=accuracy!=null && if(showAccuracy) accuracy>10 else accuracy>15
        if(showAccuracy) {
            val radius=(accuracy!!/(40075016.686*cos(Math.toRadians(lat))/(camera.scale*256))).toFloat()
            paint.style=Paint.Style.FILL;paint.color=Color.argb(30,0,100,245);canvas.drawCircle(x,y,radius,paint)
            paint.style=Paint.Style.STROKE;paint.strokeWidth=density;paint.color=Color.argb(100,0,100,245);canvas.drawCircle(x,y,radius,paint)
        }
        val save=canvas.save();canvas.translate(x,y)
        phoneHeading?.takeIf { it.isFinite() }?.let { heading ->
            val angle=Math.toRadians(heading);val vx=ax*sin(angle)-bx*cos(angle);val vy=ay*sin(angle)-by*cos(angle)
            val rotated=canvas.save();canvas.rotate(Math.toDegrees(atan2(vx,-vy)).toFloat())
            arrow.reset();arrow.moveTo(0f,-dp(21));arrow.lineTo(dp(5),-dp(10));arrow.lineTo(-dp(5),-dp(10));arrow.close()
            paint.color=Color.WHITE;paint.style=Paint.Style.STROKE;paint.strokeWidth=dp(3);canvas.drawPath(arrow,paint)
            paint.color=Color.rgb(0,100,245);paint.style=Paint.Style.FILL;canvas.drawPath(arrow,paint);canvas.restoreToCount(rotated)
        }
        paint.style=Paint.Style.FILL;paint.color=Color.WHITE;canvas.drawCircle(0f,0f,dp(11),paint)
        paint.color=Color.rgb(0,100,245);canvas.drawCircle(0f,0f,dp(8),paint);canvas.restoreToCount(save)
        gps.altitudeM?.takeIf { showHeight && it.isFinite() }?.let { altitude ->
            val text="${altitude.roundToInt()} m";val left=x+dp(18);val inset=dp(5);val metrics=heightPaint.fontMetrics
            val half=(metrics.descent-metrics.ascent)/2+dp(3)
            heightPaint.color=Color.argb(220,255,255,255);canvas.drawRoundRect(left,y-half,left+heightPaint.measureText(text)+inset*2,y+half,dp(4),dp(4),heightPaint)
            heightPaint.color=Color.BLACK;canvas.drawText(text,left+inset,y-(metrics.ascent+metrics.descent)/2,heightPaint)
        }
    }
    fun dispose() { disposed=true;generation++;worker.queue.clear();worker.shutdownNow();main.removeCallbacksAndMessages(null);geometry.clear();map.map().layers().remove(nativeRoutes);nativeRoutes.onDetach() }
}
