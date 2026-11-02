package de.bcycle.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import org.oscim.core.MercatorProjection
import java.util.IdentityHashMap
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Geometry/GPS publisher; every map-attached element belongs to VTM's GL frame. */
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
    private val nativeRoutes=VtmRouteLayer(map,density)
    private val nativeGps=VtmGpsLayer(map,context)
    private var liveEpoch:Pair<Int,Long>?=null
    private var liveFallback:GlRouteLine?=null
    init {
        isClickable=false;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO;tag="map_route_overlay"
        setWillNotDraw(true)
        map.map().layers().add(nativeRoutes);map.map().layers().add(nativeGps)
    }
    fun update(route:GpxRoute?,track:List<MapPoint>,gps:GpsValues,showRoute:Boolean,showTrack:Boolean,night:Boolean=false,phoneHeading:Double?=null,
        recordedStyle:MapLineStyle=MapRouteStyles.default(MapLineKind.RECORDED),savedStyle:MapLineStyle=MapRouteStyles.default(MapLineKind.SAVED,night),showHeight:Boolean=true,overlays:List<StyledMapRoute>?=null,trackGeneration:Long=0L,profileGeneration:Int=0) {
        if(disposed) return
        nativeGps.show(gps,phoneHeading,showHeight)
        val epoch=profileGeneration to trackGeneration
        if(liveEpoch!=epoch || !showTrack || track.isEmpty()) liveFallback=null
        liveEpoch=epoch
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
                main.post { if(!disposed && generation==token) { geometry=prepared;publishRoutes() } }
            } } catch(_:java.util.concurrent.RejectedExecutionException) { /* Disposed; never block the recording/UI thread. */ }
        }
        publishRoutes()
    }
    private fun publishRoutes() {
        nativeRoutes.show(lines.mapNotNull { line ->
            val prepared=geometry[line.points]?.let { GlRouteLine(it.native,line.style,line.planned) }
            if(line.planned) prepared else {
                if(prepared!=null) liveFallback=prepared
                prepared ?: liveFallback?.takeIf { line.points.isNotEmpty() }?.copy(style=line.style)
            }
        })
    }
    private fun prepare(points:List<MapPoint>):Geometry {
        val xy=DoubleArray(points.size*2);val starts=BooleanArray(points.size)
        points.forEachIndexed { i,p ->
            xy[i*2]=MercatorProjection.longitudeToX(p.longitude);xy[i*2+1]=MercatorProjection.latitudeToY(p.latitude)
            starts[i]=i==0 || p.segmentStart
        }
        return Geometry(GlRouteGeometry(xy,starts))
    }
    fun dispose() { disposed=true;generation++;worker.queue.clear();worker.shutdownNow();main.removeCallbacksAndMessages(null);geometry.clear();liveFallback=null;map.map().layers().remove(nativeRoutes);nativeRoutes.onDetach();map.map().layers().remove(nativeGps);nativeGps.onDetach() }
}
