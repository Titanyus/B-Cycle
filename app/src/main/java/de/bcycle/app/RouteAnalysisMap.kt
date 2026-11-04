package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import kotlin.math.abs

/** Same VTM base source and route overlay as the dashboard, without live GPS or camera preferences. */
internal class RouteAnalysisMap(context: Context, private val route: GpxRoute, private val interactive: Boolean) : FrameLayout(context) {
    private val prefs = context.getSharedPreferences("bike", 0)
    private val night = prefs.getBoolean("mapNight",false)
    private val source = MapProviderSettings.source(context,prefs.getString("mapSource",null))
    internal lateinit var map: BicycleMapView
        private set
    private lateinit var layer: VtmBaseMap
    private lateinit var overlay:RouteTrackLayer
    private var pinchZoom:MapPinchZoom?=null
    private var destroyed = false; private var fitted = false
    init {
        tag = "analysis_map"; contentDescription = context.getString(R.string.analysis_map)
        map=BicycleMapView(context).apply {
            setZoomLimits(MapZoom.maximum(source));setZoom(MapZoom.minimum.toDouble());pauseMap()
            if(!interactive) setOnTouchListener { _,_ -> true }
        }
        if(interactive) {
            pinchZoom=MapPinchZoom(map) { MapZoom.maximum(source) }
            map.setOnTouchListener { _,event -> pinchZoom!!.onTouch(event) }
        }
        layer=VtmBaseMap(context,map.map(),source,MapProviderSettings.read(context),prefs.getBoolean(VtmBaseMap.cyclewaysKey,true),map::queueEvent,MapVectorLayers.read(prefs),MapStyleOverrides.read(prefs))
        overlay=RouteTrackLayer(context,map).apply { update(route,emptyList(),GpsValues(),true,false,night,savedStyle=MapRouteStyles.read(prefs,MapLineKind.SAVED,night)) }
        addView(map,LayoutParams(-1,-1));addView(overlay,LayoutParams(-1,-1))
        if (interactive) {
            val buttons = android.widget.LinearLayout(context).apply { orientation = android.widget.LinearLayout.VERTICAL }
            val themes = ThemeManager(prefs)
            fun control(icon: BicycleIcon, description: Int, action: () -> Unit) {
                buttons.addView(BicycleComponents.icon(context, themes, icon, context.getString(description)) { pinchZoom?.stop();action() }, android.widget.LinearLayout.LayoutParams(BicycleDesign.dp(context, MapControlDesign.detailWidth), BicycleDesign.dp(context, MapControlDesign.buttonHeight)))
            }
            control(BicycleIcon.ZoomIn, R.string.analysis_zoom_in) { map.setZoom((map.zoom+.5).coerceAtMost(MapZoom.maximum(source).toDouble())) }
            control(BicycleIcon.ZoomOut, R.string.analysis_zoom_out) { map.setZoom((map.zoom-.5).coerceAtLeast(MapZoom.minimum.toDouble())) }
            control(BicycleIcon.Locate, R.string.analysis_fit_route) { fit(width, height) }
            BicycleComponents.background(buttons, themes, ThemeRole.TERTIARY, BicycleDesign.inputRadius)
            addView(buttons, LayoutParams(-2, -2, android.view.Gravity.TOP or android.view.Gravity.END))
        }
        addView(android.widget.TextView(context).apply {
            text = source.attribution+if(source==MapSource.VECTOR && MapVectorLayer.CONTOURS in MapVectorLayers.read(prefs)) " · Mapzen Terrain" else ""; textSize = BicycleDesign.mapAttributionSp; setTextColor(Color.BLACK)
            setBackgroundColor(Color.argb(220, 255, 255, 255)); setPadding(4, 2, 4, 2)
        }, LayoutParams(-2, -2, android.view.Gravity.BOTTOM or android.view.Gravity.END))
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!fitted && w > 0 && h > 0) { fit(w, h); fitted = true }
    }
    private fun fit(w: Int, h: Int) {
        val points = route.points
        val minLat = points.minOf { it.latitude }; val maxLat = points.maxOf { it.latitude }
        // Unwrap longitudes to fit tracks crossing the antimeridian into their actual small extent.
        val origin = points.first().longitude
        fun lon(p: MapPoint) = origin + ((p.longitude - origin + 540) % 360) - 180
        val minLon = points.minOf(::lon); val maxLon = points.maxOf(::lon)
        val centerLon = (((minLon + maxLon) / 2 + 540) % 360) - 180
        map.setCenter((minLat+maxLat)/2,centerLon)
        var zoom = MapZoom.maximum(source)
        while (zoom > MapZoom.minimum) {
            val size = (256L shl zoom)
            val dx = (maxLon - minLon) / 360 * size
            val dy = abs((org.oscim.core.MercatorProjection.latitudeToY(maxLat)-org.oscim.core.MercatorProjection.latitudeToY(minLat))*size)
            if (dx <= w * .8 && dy <= h * .75) break
            zoom--
        }
        map.setZoom(zoom.toDouble())
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); resume() }
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (!::map.isInitialized || !::layer.isInitialized || destroyed) return
        if (visibility == View.VISIBLE) resume() else { layer.pause();map.pauseMap() }
    }
    private fun resume() { if (!destroyed) { map.resumeMap();layer.resume() } }
    fun dispose() { if (!destroyed) { destroyed = true; layer.pause();map.pauseMap(); pinchZoom?.cancel();overlay.dispose();map.disposeMap() } }
    override fun onDetachedFromWindow() {
        // Fullscreen host rehouses views after showing: detach pauses, the owning screen disposes on close.
        if (!destroyed) { pinchZoom?.cancel();layer.pause();map.pauseMap() }
        super.onDetachedFromWindow()
    }
}
