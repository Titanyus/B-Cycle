package de.bcycle.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import org.oscim.core.GeoPoint
import org.oscim.core.MapPosition
import org.oscim.core.Tile
import org.oscim.utils.Parameters
import kotlin.math.floor
import kotlin.math.sqrt

/** VTM surface shared by dashboard and route previews. Camera values retain the existing preference units. */
class BicycleMapView(context:Context):org.oscim.android.MapView(configure(context)) {
    companion object {
        private fun configure(context:Context):Context {
            Parameters.CUSTOM_TILE_SIZE=true;Tile.SIZE=256
            return context
        }
    }
    val camera:MapPosition get()=map().mapPosition
    val zoom:Double get()=camera.zoom
    data class Center(val latitude:Double,val longitude:Double)
    val center:Center get()=camera.let { Center(org.oscim.core.MercatorProjection.toLatitude(it.y),org.oscim.core.MercatorProjection.toLongitude(it.x)) }
    fun screenPoint(latitude:Double,longitude:Double)=org.oscim.core.Point().also {
        map().viewport().toScreenPoint(org.oscim.core.MercatorProjection.longitudeToX(longitude),org.oscim.core.MercatorProjection.latitudeToY(latitude),false,it)
    }
    val bearing:Float get()=camera.bearing
    var anchorX=.5f;private set
    var anchorY=.5f;private set
    var cameraChanged:(()->Unit)?=null
    var userMoved:(()->Unit)?=null
    var zoomed:(()->Unit)?=null
    private var snapshot:Bitmap?=null
    private var snapshotGeneration=0
    private var disposed=false
    private var lastEventZoom=Double.NaN
    init {
        // VTM disables View drawing for its GL surface. Parent snapshots would then bypass
        // draw(Canvas) entirely and omit our PixelCopy image from the outgoing dashboard.
        setWillNotDraw(false)
        org.oscim.backend.AssetAdapter.init(object:org.oscim.backend.AssetAdapter() {
            override fun openFileAsStream(name:String):java.io.InputStream? =
                try { context.applicationContext.assets.open(name) }
                catch(_:java.io.IOException) { BicycleMapView::class.java.getResourceAsStream("/assets/$name") }
        })
        map().eventLayer.apply { enableRotation(false);enableTilt(false);enableZoom(true) }
        // All map hosts stay in plan view, including native camera animations and gestures.
        map().viewport().apply { minTilt=0f;maxTilt=0f }
        map().events.bind { _,position ->
            val next=position.zoom
            if(lastEventZoom.isFinite() && kotlin.math.abs(next-lastEventZoom)>.0001) zoomed?.invoke()
            lastEventZoom=next;cameraChanged?.invoke()
        }
    }
    fun setZoomLimits(maximum:Int) { map().viewport().apply { minZoomLevel=MapZoom.minimum;maxZoomLevel=maximum } }
    fun setZoom(value:Double) { setCamera(camera.apply { setZoom(value) });zoomed?.invoke() }
    fun setCenter(latitude:Double,longitude:Double) { setCamera(camera.apply { setPosition(latitude,longitude) }) }
    fun rotate(degrees:Float) { setCamera(camera.apply { setBearing(degrees) }) }
    fun setCamera(position:MapPosition) {
        if(disposed) return
        position.tilt=0f;position.roll=0f
        map().setMapPosition(position);cameraChanged?.invoke()
    }
    fun setAnchor(x:Float,y:Float) {
        if(anchorX==x && anchorY==y) return
        anchorX=x;anchorY=y
        map().viewport().setMapViewCenter((x-.5f)*2,(y-.5f)*2)
        setCamera(camera)
    }
    fun panBy(x:Float,y:Float) { map().viewport().moveMap(x,y);map().updateMap(true);cameraChanged?.invoke() }
    fun notifyUserMove() { userMoved?.invoke() }
    fun cancelMotion() { map().animator().cancel() }
    fun resumeMap() { if(!disposed) { onResume();map().updateMap(true) } }
    fun pauseMap() { cancelMotion();onPause() }
    fun disposeMap() { if(!disposed) { pauseMap();disposed=true;clearSnapshot();cameraChanged=null;userMoved=null;zoomed=null;onDestroy() } }
    /** Capture the separate GL surface before the outgoing dashboard is drawn into its bounded bitmap. */
    fun prepareSnapshot(done:(Boolean)->Unit) {
        clearSnapshot()
        val token=snapshotGeneration
        captureImage { image ->
            if(token!=snapshotGeneration || disposed) { image?.recycle();done(false) }
            else { snapshot=image;done(image!=null) }
        }
    }
    /** Independent ownership for adjacent-page previews; never replaces the outgoing slide image. */
    internal fun captureImage(done:(Bitmap?)->Unit) {
        if(disposed || !isAttachedToWindow || holder.surface?.isValid!=true || width<=0 || height<=0) { done(null);return }
        val factor=sqrt(1_000_000.0/(width.toDouble()*height)).coerceAtMost(1.0)
        val image=try { Bitmap.createBitmap((width*factor).toInt().coerceAtLeast(1),(height*factor).toInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888) }
            catch(_:OutOfMemoryError) { done(null);return }
        val handler=Handler(Looper.getMainLooper());var completed=false
        val timeout=Runnable { if(!completed) { completed=true;done(null) } }
        handler.postDelayed(timeout,200)
        try {
            PixelCopy.request(this,image,{ result ->
                handler.removeCallbacks(timeout)
                if(completed || disposed) { image.recycle();if(!completed) { completed=true;done(null) };return@request }
                completed=true
                if(result==PixelCopy.SUCCESS) done(image) else { image.recycle();done(null) }
            },handler)
        } catch(_:IllegalArgumentException) { handler.removeCallbacks(timeout);image.recycle();if(!completed) { completed=true;done(null) } }
    }
    fun clearSnapshot() { snapshotGeneration++;snapshot?.recycle();snapshot=null }
    override fun draw(canvas:Canvas) {
        val shot=snapshot
        if(!canvas.isHardwareAccelerated && shot!=null && !shot.isRecycled) canvas.drawBitmap(shot,null,Rect(0,0,width,height),null)
        else super.draw(canvas)
    }
    fun save(prefs:android.content.SharedPreferences) {
        val position=camera
        prefs.edit().putInt("mapZoom",floor(position.zoom).toInt()).putFloat(MapZoom.preference,position.zoom.toFloat())
            .putFloat("mapLatitude",position.geoPoint.latitude.toFloat()).putFloat("mapLongitude",position.geoPoint.longitude.toFloat()).apply()
    }
}
