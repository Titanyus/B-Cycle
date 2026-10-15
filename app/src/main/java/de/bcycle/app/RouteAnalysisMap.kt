package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.view.MapView
import kotlin.math.abs

/** Same Mapsforge layers and viewport cache as the dashboard, without live GPS or camera preferences. */
internal class RouteAnalysisMap(context: Context, private val route: GpxRoute, private val interactive: Boolean) : FrameLayout(context) {
    private val prefs = context.getSharedPreferences("bike", 0)
    private val night = prefs.getBoolean("mapNight", false)
    private val source = MapProviderSettings.source(context,prefs.getString("mapSource",null))
    internal lateinit var map: MapView
        private set
    private lateinit var layer: MapNightLayer
    private var destroyed = false; private var fitted = false
    init {
        tag = "analysis_map"; contentDescription = context.getString(R.string.analysis_map)
        AndroidGraphicFactory.createInstance(context.applicationContext)
        map = MapView(context).apply {
            model.displayModel.setFixedTileSize(256)
            model.displayModel.backgroundColor = if (night) MapNightPalette.background else Color.rgb(225, 225, 225)
            setBuiltInZoomControls(false); mapScaleBar?.isVisible = false
            setZoomLevelMin(2); setZoomLevelMax(source.maximumZoom.toByte()); setZoomLevel(2)
            touchGestureHandler.setRotationEnabled(false); layerManager.pause()
            // Preview never consumes scroll gestures. The dedicated fullscreen view supports native pan/zoom.
            if (!interactive) setOnTouchListener { _, _ -> true }
        }
        layer = MapNightLayer(ViewportTileCache(), map.model.mapViewPosition, source.tiles(MapProviderSettings.read(context))).apply { this.night = this@RouteAnalysisMap.night }
        map.layerManager.layers.add(layer)
        map.layerManager.layers.add(RouteTrackLayer(resources.displayMetrics.density).apply { update(route, emptyList(), GpsValues(), true, false, night) })
        addView(map, LayoutParams(-1, -1))
        if (interactive) {
            val buttons = android.widget.LinearLayout(context).apply { orientation = android.widget.LinearLayout.VERTICAL }
            val themes = ThemeManager(prefs)
            fun control(icon: BicycleIcon, description: Int, action: () -> Unit) {
                buttons.addView(BicycleComponents.icon(context, themes, icon, context.getString(description), action), android.widget.LinearLayout.LayoutParams(BicycleDesign.dp(context, MapControlDesign.detailWidth), BicycleDesign.dp(context, MapControlDesign.buttonHeight)))
            }
            control(BicycleIcon.ZoomIn, R.string.analysis_zoom_in) { map.model.mapViewPosition.zoomIn() }
            control(BicycleIcon.ZoomOut, R.string.analysis_zoom_out) { map.model.mapViewPosition.zoomOut() }
            control(BicycleIcon.Locate, R.string.analysis_fit_route) { fit(width, height) }
            BicycleComponents.background(buttons, themes, ThemeRole.TERTIARY, BicycleDesign.inputRadius)
            addView(buttons, LayoutParams(-2, -2, android.view.Gravity.TOP or android.view.Gravity.END))
        }
        addView(android.widget.TextView(context).apply {
            text = source.attribution; textSize = BicycleDesign.mapAttributionSp; setTextColor(Color.BLACK)
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
        map.setCenter(LatLong((minLat + maxLat) / 2, centerLon))
        var zoom = source.maximumZoom
        while (zoom > 2) {
            val size = MercatorProjection.getMapSize(zoom.toByte(), 256)
            val dx = (maxLon - minLon) / 360 * size
            val dy = abs(MercatorProjection.latitudeToPixelY(maxLat, size) - MercatorProjection.latitudeToPixelY(minLat, size))
            if (dx <= w * .8 && dy <= h * .75) break
            zoom--
        }
        map.setZoomLevel(zoom.toByte())
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); resume() }
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (!::map.isInitialized || !::layer.isInitialized || destroyed) return
        if (visibility == View.VISIBLE) resume() else { layer.onPause(); map.layerManager.pause() }
    }
    private fun resume() { if (!destroyed) { map.layerManager.proceed(); layer.onResume() } }
    fun dispose() { if (!destroyed) { destroyed = true; layer.onPause(); map.layerManager.pause(); map.destroyAll() } }
    override fun onDetachedFromWindow() {
        // Fullscreen host rehouses views after showing: detach pauses, the owning screen disposes on close.
        if (!destroyed) { layer.onPause(); map.layerManager.pause() }
        super.onDetachedFromWindow()
    }
}
