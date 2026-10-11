package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.cache.FileSystemTileCache
import org.mapsforge.map.layer.cache.TwoLevelTileCache
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.download.tilesource.OnlineTileSource
import org.mapsforge.map.view.InputListener
import java.io.File
import kotlin.math.abs

enum class MapSource(val title: String, val host: String, val path: String, val attribution: String, val creditUrl: String) {
    CYCLOSM("CyclOSM · Fahrradkarte", "a.tile-cyclosm.openstreetmap.fr", "/cyclosm/", "CyclOSM · © OpenStreetMap", "https://www.cyclosm.org/legend.html"),
    OSM("OpenStreetMap · Standardkarte", "tile.openstreetmap.org", "/", "© OpenStreetMap", "https://www.openstreetmap.org/copyright");
    fun tiles() = object : OnlineTileSource(arrayOf(host), 443) {
        override fun getDefaultTimeToLive() = 7L * 24 * 60 * 60 * 1_000
    }.setProtocol("https").setBaseUrl(path)
        .setName(name).setParallelRequestsLimit(2).setZoomLevelMax(19).apply {
            setUserAgent("B-Cycle/0.6.9 (https://github.com/Titanyus/B-Cycle)")
        }
    companion object { fun decode(id: String?) = entries.firstOrNull { it.name == id } ?: CYCLOSM }
}

/** One resizable map. Sensor ownership remains entirely in RideRuntime. */
class MapPanel(context: Context, private val runtime: RideRuntime, private val fullscreen: () -> Unit,
               private val openRoutes: () -> Unit, private val requestGps: () -> Unit,
               private val openCredit: (String) -> Unit) : FrameLayout(context) {
    private val preferences = runtime.preferences
    private val handler = Handler(Looper.getMainLooper())
    private val controls = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val zoomControls = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; tag = "map_zoom_controls" }
    private lateinit var routeButton: Button
    private val credit = TextView(context)
    private val status = TextView(context)
    private val dataFields = MapDataFieldsView(context)
    private lateinit var directionButton: Button
    private lateinit var centerButton: Button
    private lateinit var fullButton: Button
    private var running = false
    private var source = MapSource.decode(preferences.getString("mapSource", null))
    private var downloads: TileDownloadLayer
    private var follow = preferences.getBoolean("mapFollow", true)
    private var heading = preferences.getBoolean("mapHeading", true)
    private var gps = GpsValues()
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var lastBearing: Double? = null
    private var lastTrack = -1L
    private var lastRoute = -1L
    private var layerTrackRevision = -1L
    private var layerTrack: List<MapPoint> = emptyList()
    var controlsVisibilityChanged: ((Boolean) -> Unit)? = null
    private val hideControls = Runnable {
        controls.visibility = GONE; zoomControls.visibility = GONE; routeButton.visibility = GONE
        controlsVisibilityChanged?.invoke(false)
    }
    val mapView: MapView
    private val routes = RouteTrackLayer(resources.displayMetrics.density)

    init {
        AndroidGraphicFactory.createInstance(context.applicationContext)
        setBackgroundColor(Color.rgb(225, 225, 225))
        mapView = MapView(context).apply {
            model.displayModel.setFixedTileSize(256)
            setBuiltInZoomControls(false); mapScaleBar?.isVisible = false
            touchGestureHandler.setRotationEnabled(false)
            setZoomLevelMin(2); setZoomLevelMax(19)
            setZoomLevel(preferences.getInt("mapZoom", 16).coerceIn(2, 19).toByte())
            val lat = preferences.getFloat("mapLatitude", 51f).toDouble().coerceIn(-85.0, 85.0)
            val lon = preferences.getFloat("mapLongitude", 10f).toDouble().coerceIn(-180.0, 180.0)
            setCenter(LatLong(lat, lon))
            layerManager.pause()
        }
        addView(mapView, LayoutParams(-1, -1))
        downloads = createDownloadLayer(source)
        mapView.layerManager.layers.add(downloads)
        mapView.layerManager.layers.add(routes)
        mapView.addInputListener(object : InputListener {
            override fun onMoveEvent() { setFollow(false); revealControls() }
            override fun onZoomEvent() { revealControls() }
        })
        val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean { revealControls(); return true }
        })
        mapView.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            // Native Mapsforge handles pan/zoom. A tap never resumes GPS following.
            if (event.actionMasked == MotionEvent.ACTION_DOWN) handler.removeCallbacks(hideControls)
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                if (controls.visibility == VISIBLE) scheduleHide()
            }
            false
        }
        fun icon(symbol: String, description: String, parent: LinearLayout? = controls, action: () -> Unit): Button = Button(context).apply {
            text = symbol; textSize = 20f; setTextColor(Color.WHITE); isAllCaps = false
            contentDescription = description; minWidth = 0; minHeight = 0; setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply { setColor(Color.argb(225, 25, 25, 25)); cornerRadius = dp(5).toFloat(); setStroke(dp(1), Color.DKGRAY) }
            setOnClickListener { action(); revealControls() }
            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) handler.removeCallbacks(hideControls)
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) scheduleHide()
                false
            }
            parent?.addView(this, LinearLayout.LayoutParams(dp(44), dp(48)).apply {
                if (parent.orientation == LinearLayout.VERTICAL) topMargin = dp(2) else marginStart = dp(2)
            })
        }
        centerButton = icon("◎", "GPS-Zentrierung aktivieren") {
            setFollow(true); requestGps(); updateCamera()
        }
        directionButton = icon("↑", "Kartenausrichtung ändern") {
            heading = !heading; preferences.edit().putBoolean("mapHeading", heading).apply(); updateCamera()
        }
        icon("+", "Karte vergrößern", zoomControls) { zoom(1) }
        icon("−", "Karte verkleinern", zoomControls) { zoom(-1) }
        fullButton = icon("⛶", "Karte im Vollbild") { fullscreen() }
        addView(controls, LayoutParams(-2, dp(48), Gravity.TOP or Gravity.END).apply { topMargin = dp(6); marginEnd = dp(6) })
        routeButton = icon("", "Streckenmanager öffnen", null) { openRoutes() }.apply {
            val image = context.getDrawable(R.drawable.ic_settings_routes)!!.apply { setBounds(0, 0, dp(28), dp(28)) }
            setCompoundDrawables(null, image, null, null); gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), 0)
        }
        addView(routeButton, LayoutParams(dp(44), dp(48), Gravity.TOP or Gravity.START).apply { topMargin = dp(6); marginStart = dp(6) })
        addView(zoomControls, LayoutParams(dp(44), -2, Gravity.BOTTOM or Gravity.END).apply { bottomMargin = dp(77); marginEnd = dp(6) })
        dataFields.reload(preferences)
        addView(dataFields, LayoutParams(-1, dp(48), Gravity.BOTTOM).apply { bottomMargin = dp(23) })
        credit.apply {
            text = source.attribution; textSize = 11f; setTextColor(Color.BLACK)
            setPadding(dp(5), dp(3), dp(5), dp(3)); setBackgroundColor(Color.argb(220, 255, 255, 255))
            contentDescription = "Kartenquelle, Urheber und Legende"
            setOnClickListener { openCredit(source.creditUrl) }
        }
        addView(credit, LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END))
        status.apply {
            textSize = 11f; setTextColor(Color.BLACK); setBackgroundColor(Color.argb(220, 255, 255, 255))
            setPadding(dp(5), dp(3), dp(5), dp(3)); maxLines = 1
            setOnClickListener { requestGps(); revealControls() }
        }
        addView(status, LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START).apply { bottomMargin = dp(23) })
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateCamera() }
    }

    private fun createDownloadLayer(source: MapSource): TileDownloadLayer {
        // A bounded normal cache, never a region prefetch/offline download.
        val memory = ViewportTileCache()
        val disk = FileSystemTileCache(512, File(context.cacheDir, "tiles-${source.name}").apply { mkdirs() }, AndroidGraphicFactory.INSTANCE, true)
        return TileDownloadLayer(TwoLevelTileCache(memory, disk), mapView.model.mapViewPosition, source.tiles(), AndroidGraphicFactory.INSTANCE)
    }
    fun resume() { running = true; mapView.layerManager.proceed(); downloads.onResume(); revealControls(); refresh() }
    fun pause() {
        running = false; handler.removeCallbacksAndMessages(null); downloads.onPause(); mapView.layerManager.pause(); saveCamera()
    }
    fun destroy() { pause(); mapView.destroyAll() }
    fun refreshSettings() {
        val next = MapSource.decode(preferences.getString("mapSource", null))
        if (next != source) {
            mapView.layerManager.layers.remove(downloads); downloads.onDestroy(); downloads.tileCache.destroy()
            source = next; downloads = createDownloadLayer(source); mapView.layerManager.layers.add(0, downloads)
            if (running) downloads.onResume()
            credit.text = source.attribution
        }
        dataFields.reload(preferences)
        refreshDataFields()
        heading = preferences.getBoolean("mapHeading", true)
        follow = preferences.getBoolean("mapFollow", true)
        updateCamera(); refreshRoutes()
    }
    fun refresh() {
        if (!running) return
        gps = runtime.gpsValues
        refreshDataFields()
        val track = runtime.mapState.track.revision; val route = runtime.mapState.routeRevision
        // BLE packets and elapsed-time changes must not redraw an unchanged map.
        if (gps.latitude != lastLatitude || gps.longitude != lastLongitude || gps.bearingDegrees != lastBearing ||
            track != lastTrack || route != lastRoute) {
            lastLatitude = gps.latitude; lastLongitude = gps.longitude; lastBearing = gps.bearingDegrees
            lastTrack = track; lastRoute = route; updateCamera(); refreshRoutes()
        }
        val next = if (!runtime.gpsEnabled) "GPS aus · antippen" else if (gps.latitude == null) "GPS · Warte auf Position"
            else if (runtime.recording && !runtime.mapTrackEnabled) "Track-Aufzeichnung aus" else ""
        if (status.text.toString() != next) status.text = next
        status.visibility = if (next.isEmpty()) GONE else VISIBLE
    }
    private fun refreshRoutes() {
        if (layerTrackRevision != runtime.mapState.track.revision) {
            layerTrackRevision = runtime.mapState.track.revision
            layerTrack = runtime.mapState.track.points.toList()
        }
        routes.update(runtime.mapState.route, layerTrack, runtime.gpsValues,
            preferences.getBoolean("mapGpxVisible", true), preferences.getBoolean("mapTrackVisible", true))
    }
    private fun refreshDataFields() {
        dataFields.show(runtime.bikeData, runtime.gpsValues, runtime.ride.bike.distanceM, runtime.pulseValues)
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Keep legal attribution and GPS status readable, including larger system fonts.
        val creditHeight = credit.measuredHeight
        val fieldMargin = dataFields.layoutParams as LayoutParams
        val statusMargin = status.layoutParams as LayoutParams
        val zoomMargin = zoomControls.layoutParams as LayoutParams
        val nextStatusMargin = creditHeight + if (dataFields.visibility == VISIBLE) dataFields.measuredHeight else 0
        val nextZoomMargin = creditHeight + if (dataFields.visibility == VISIBLE) dp(54) else dp(6)
        // Short map panels still need both zoom buttons; reserve their column if it meets the top row.
        val buttonHeight = ((measuredHeight - nextZoomMargin - dp(4)) / 2).coerceIn(dp(24), dp(48))
        val topMargin = controls.layoutParams as LayoutParams
        val nextTopEnd = if (measuredHeight - nextZoomMargin - 2 * buttonHeight - dp(4) < dp(60)) dp(56) else dp(6)
        var changed = false
        for (index in 0 until zoomControls.childCount) {
            val params = zoomControls.getChildAt(index).layoutParams
            if (params.height != buttonHeight) { params.height = buttonHeight; changed = true }
        }
        if (changed || fieldMargin.bottomMargin != creditHeight || statusMargin.bottomMargin != nextStatusMargin ||
            zoomMargin.bottomMargin != nextZoomMargin || topMargin.marginEnd != nextTopEnd) {
            fieldMargin.bottomMargin = creditHeight; statusMargin.bottomMargin = nextStatusMargin
            zoomMargin.bottomMargin = nextZoomMargin; topMargin.marginEnd = nextTopEnd
            controls.layoutParams = topMargin
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
    fun showFullscreen(value: Boolean) { fullButton.text = if (value) "↙" else "⛶"; fullButton.contentDescription = if (value) "Zurück zum Dashboard" else "Karte im Vollbild"; revealControls() }
    fun setFollow(value: Boolean) {
        if (follow == value) return
        follow = value; preferences.edit().putBoolean("mapFollow", value).apply()
        if (::centerButton.isInitialized) centerButton.setTextColor(if (value) Color.rgb(110, 230, 130) else Color.WHITE)
    }
    private fun updateCamera() {
        if (width <= 0 || height <= 0) return
        val bearing = if (heading) gps.bearingDegrees else null
        // Preserve the manual viewport when following is paused; avoid a jump on the next GPS tick.
        val centerY = if (!follow) mapView.mapViewCenterY else if (heading) .68f else .5f
        if (mapView.mapViewCenterY != centerY) mapView.setMapViewCenterY(centerY)
        val rotation = Rotation(-(bearing ?: 0.0).toFloat(), width / 2f, height / 2f)
        if (mapView.mapRotation != rotation) mapView.rotate(rotation)
        if (follow && gps.latitude != null && gps.longitude != null && abs(gps.latitude!!) <= 85.05112878) {
            val center = LatLong(gps.latitude!!, gps.longitude!!)
            if (mapView.model.mapViewPosition.center != center) mapView.setCenter(center)
        }
        directionButton.text = if (heading) "↑" else "N"
        directionButton.contentDescription = if (heading) "Fahrtrichtung oben · wechseln zu Norden" else "Norden oben · wechseln zur Fahrtrichtung"
        centerButton.setTextColor(if (follow) Color.rgb(110, 230, 130) else Color.WHITE)
    }
    fun fitRoute() {
        val points = runtime.mapState.route?.points ?: return
        if (points.isEmpty() || width <= 0 || height <= 0) return
        setFollow(false)
        mapView.setMapViewCenterY(.5f)
        val lats = points.map { it.latitude }; val lons = points.map { it.longitude }
        val minLat = lats.min(); val maxLat = lats.max(); val minLon = lons.min(); val maxLon = lons.max()
        mapView.setCenter(LatLong((minLat + maxLat) / 2, (minLon + maxLon) / 2))
        mapView.rotate(Rotation.NULL_ROTATION)
        var level = 19
        while (level > 2) {
            val size = MercatorProjection.getMapSize(level.toByte(), 256)
            val dx = abs(MercatorProjection.longitudeToPixelX(maxLon, size) - MercatorProjection.longitudeToPixelX(minLon, size))
            val dy = abs(MercatorProjection.latitudeToPixelY(maxLat, size) - MercatorProjection.latitudeToPixelY(minLat, size))
            if (dx < width * .8 && dy < height * .7) break
            level--
        }
        mapView.model.mapViewPosition.setZoomLevel(level.toByte(), false); revealControls(); refreshRoutes()
    }
    private fun zoom(delta: Int) {
        mapView.model.mapViewPosition.setZoomLevel((mapView.model.mapViewPosition.zoomLevel + delta).coerceIn(2, 19).toByte(), false)
        saveCamera()
    }
    private fun saveCamera() {
        val position = mapView.model.mapViewPosition
        preferences.edit().putInt("mapZoom", position.zoomLevel.toInt()).putFloat("mapLatitude", position.center.latitude.toFloat())
            .putFloat("mapLongitude", position.center.longitude.toFloat()).apply()
    }
    fun revealControls() { controls.visibility = VISIBLE; zoomControls.visibility = VISIBLE; routeButton.visibility = VISIBLE; controlsVisibilityChanged?.invoke(true); scheduleHide() }
    fun holdControls() { handler.removeCallbacks(hideControls) }
    private fun scheduleHide() {
        handler.removeCallbacks(hideControls)
        if (running && !context.getSystemService(AccessibilityManager::class.java).isTouchExplorationEnabled)
            handler.postDelayed(hideControls, 5_000)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

}
