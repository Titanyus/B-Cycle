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

enum class MapSource(val title: String, val host: String, val path: String, val attribution: String, val creditUrl: String, val maximumZoom: Int = 19) {
    CYCLOSM("CyclOSM · Fahrradkarte", "a.tile-cyclosm.openstreetmap.fr", "/cyclosm/", "CyclOSM · © OpenStreetMap", "https://www.cyclosm.org/legend.html"),
    OSM("OpenStreetMap · Standardkarte", "tile.openstreetmap.org", "/", "© OpenStreetMap", "https://www.openstreetmap.org/copyright"),
    OPENTOPOMAP("OpenTopoMap", "a.tile.opentopomap.org", "/", "© OpenStreetMap · SRTM · OpenTopoMap (CC-BY-SA)", "https://opentopomap.org/about",17),
    OPENCYCLEMAP("OpenCycleMap", "tile.thunderforest.com", "/cycle/", "© Thunderforest · © OpenStreetMap", "https://www.thunderforest.com/terms/");
    fun label(context: Context) = context.getString(when(this) {
        CYCLOSM -> R.string.ui_map_source_cyclosm; OSM -> R.string.ui_map_source_osm
        OPENTOPOMAP -> R.string.ui_map_source_topo; OPENCYCLEMAP -> R.string.ui_map_source_cycle
    })
    fun tiles(apiKey: String = "") = object : OnlineTileSource(arrayOf(host), 443) {
        override fun getTileUrl(tile: org.mapsforge.core.model.Tile): java.net.URL {
            val base=super.getTileUrl(tile)
            if(this@MapSource!=OPENCYCLEMAP) return base
            require(MapProviderSettings.valid(apiKey)) { "Thunderforest API key required" }
            return java.net.URL(base.toString()+"?apikey="+java.net.URLEncoder.encode(apiKey,"UTF-8"))
        }
        override fun getDefaultTimeToLive() = 7L * 24 * 60 * 60 * 1_000
    }.setProtocol("https").setBaseUrl(path)
        .setName(name).setParallelRequestsLimit(2).setZoomLevelMax(maximumZoom.toByte()).apply {
            setUserAgent("B-Cycle/${AppVersion.name} (https://github.com/Titanyus/B-Cycle)")
        }
    companion object { fun decode(id: String?) = entries.firstOrNull { it.name == id } ?: CYCLOSM }
}

/** One resizable map. Bike/GPS ownership stays in RideRuntime; phone heading is display-only. */
class MapPanel(context: Context, private val runtime: RideRuntime, private val fullscreen: () -> Unit,
               private val requestGps: () -> Unit,
               private val openCredit: (String) -> Unit) : FrameLayout(context) {
    private val preferences = runtime.preferences
    private val handler = Handler(Looper.getMainLooper())
    private val controls = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val zoomControls = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; tag = "map_zoom_controls" }
    private lateinit var routeButton: Button
    private var routeShown: Boolean? = null
    private val credit = MapCreditView(context)
    private var observedZoom=Double.NaN
    private val updateZoom=Runnable {
        val zoom=mapView.model.mapViewPosition.zoom
        if(zoom!=observedZoom) { observedZoom=zoom;updateCamera();if(running && follow) { handler.removeCallbacks(animateCamera);handler.post(animateCamera) } }
    }
    private val zoomObserver=org.mapsforge.map.model.common.Observer { handler.removeCallbacks(updateZoom);handler.post(updateZoom) }
    private val status = TextView(context)
    private val dataFields = MapDataFieldsView(context)
    private var editing = false
    var finishEditor: (() -> Unit)? = null
    var selectMapField: ((Int) -> Unit)? = null
    private val editorFinish = BicycleComponents.action(context, runtime.themes, context.getString(R.string.dashboard_editor_finish), false) { finishEditor?.invoke() }.apply {
        tag = "dashboard_editor_finish"; visibility = GONE
        setPadding(BicycleDesign.dp(context,BicycleDesign.horizontalPadding),paddingTop,BicycleDesign.dp(context,BicycleDesign.horizontalPadding),paddingBottom)
    }
    fun setEditing(value: Boolean) {
        editing = value
        dataFields.setEditing(value, runtime.themes.active.tokens.primary)
        editorFinish.visibility = if (value) VISIBLE else GONE
        for (i in 0 until controls.childCount) controls.getChildAt(i).isEnabled = !value
        for (i in 0 until zoomControls.childCount) zoomControls.getChildAt(i).apply {
            isEnabled = !value
            alpha = if (value) .45f else 1f
        }
        directionButton.isEnabled = !value && follow
        directionButton.alpha=if(follow) 1f else .4f
        nightButton.isEnabled = !value
        refreshRouteButton()
        if (value) handler.removeCallbacks(hideControls) else scheduleHide()
        refreshDataFields()
    }
    private var directionIconHeading: Boolean? = null
    private lateinit var directionButton: Button
    private lateinit var centerButton: Button
    private lateinit var fullButton: Button
    private lateinit var nightButton: Button
    private var night = preferences.getBoolean("mapNight", false)
    private var running = false
    private var moving = false
    private var source = MapProviderSettings.source(context,preferences.getString("mapSource", null))
    private var providerKey=MapProviderSettings.read(context)
    private var downloads: MapNightLayer
    private var follow = preferences.getBoolean("mapFollow", true)
    private var heading = preferences.getBoolean("mapHeading",true) && !preferences.getBoolean(MapOvalAnchor.preference,false)
    private var gps = GpsValues()
    private var displayedGps = GpsValues()
    private val motion = MapMotion()
    private val ovalAnchor=MapOvalAnchor()
    private var touchingMap = false
    private var displayedPhoneHeading:Double?=null
    private val phoneHeading:PhoneHeading=PhoneHeading(context,{ display?.rotation ?: android.view.Surface.ROTATION_0 }) {
        if(!headingVisible()) phoneHeading.stop()
        refreshPhoneHeading()
    }
    private fun refreshPhoneHeading() {
        val value=phoneHeading.current()
        if(value!=displayedPhoneHeading) { displayedPhoneHeading=value;refreshRoutes() }
    }
    private fun headingVisible()=running && isShown && cameraVisible() && runtime.gpsEnabled && gps.latitude!=null

    var cameraVisible: () -> Boolean = { true }
    private val animateCamera = object : Runnable {
        override fun run() {
            if (!running || !isShown || !cameraVisible()) {
                stopMotion()
                // Finish the shared pose once when occluded/stopped, without a frame loop.
                if (running && follow && !touchingMap) { updateCamera(updateControls = false); refreshRoutes() }
                return
            }
            displayedGps = motion.at(android.os.SystemClock.uptimeMillis())
            updateCamera(updateControls = false); refreshRoutes()
            if (motion.active || ovalAnchor.active) handler.postDelayed(this, MapMotion.frameMs)
        }
    }
    private fun stopMotion() {
        handler.removeCallbacks(animateCamera)
        displayedGps = motion.snap(gps)
        ovalAnchor.hold(android.os.SystemClock.uptimeMillis())
    }
    private fun receivePosition() {
        handler.removeCallbacks(animateCamera)
        displayedGps = motion.move(gps, android.os.SystemClock.uptimeMillis(), running && runtime.isMoving && isShown && cameraVisible())
        updateCamera(); refreshRoutes()
        if (motion.active || ovalAnchor.active) handler.postDelayed(animateCamera, MapMotion.frameMs)
    }
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var lastBearing: Double? = null
    private var lastAltitude: Double? = null
    private var lastTrack = -1L
    private var lastRoute = -1L
    private var layerTrackRevision = -1L
    private var layerTrack: List<MapPoint> = emptyList()
    var controlsVisibilityChanged: ((Boolean) -> Unit)? = null
    private val hideControls = Runnable {
        if (!running || editing || !runtime.isMoving) return@Runnable
        controls.visibility = GONE; zoomControls.visibility = GONE; routeButton.visibility = GONE; nightButton.visibility = GONE
        controlsVisibilityChanged?.invoke(false)
    }
    val mapView: MapView
    private val pinchZoom:MapPinchZoom
    private val routes by lazy { RouteTrackLayer(resources.displayMetrics.density,mapView.model.mapViewPosition) }

    init {
        MapZoom.enableFractional()
        AndroidGraphicFactory.createInstance(context.applicationContext)
        setBackgroundColor(Color.rgb(225, 225, 225))
        mapView = MapView(context).apply {
            model.displayModel.setFixedTileSize(256)
            setBuiltInZoomControls(false); mapScaleBar?.isVisible = false
            touchGestureHandler.setRotationEnabled(false)
            setZoomLevelMin(MapZoom.minimum.toByte()); setZoomLevelMax(MapZoom.maximum(source).toByte())
            model.mapViewPosition.setZoom(MapZoom.read(preferences,source),false)
            val lat = preferences.getFloat("mapLatitude", 51f).toDouble().coerceIn(-85.0, 85.0)
            val lon = preferences.getFloat("mapLongitude", 10f).toDouble().coerceIn(-180.0, 180.0)
            setCenter(LatLong(lat, lon))
            layerManager.pause()
        }
        pinchZoom=MapPinchZoom(mapView) { MapZoom.maximum(source) }
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
            if (editing) return@setOnTouchListener true
            detector.onTouchEvent(event)
            // Native Mapsforge handles pan/zoom. A tap never resumes GPS following.
            if (event.actionMasked == MotionEvent.ACTION_DOWN) { touchingMap = true; refreshRoutes(); handler.removeCallbacks(hideControls) }
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                touchingMap = false
                if (controls.visibility == VISIBLE) scheduleHide()
            }
            pinchZoom.onTouch(event)
        }
        fun icon(symbol: String, description: String, parent: LinearLayout? = controls, action: () -> Unit): Button = Button(context).apply {
            text = symbol; textSize = MapControlDesign.symbolSp; setTextColor(MapControlDesign.foreground); isAllCaps = false
            contentDescription = description; minWidth = 0; minHeight = 0; setPadding(0, 0, 0, 0)
            background = MapControlDesign.background(context)
            setOnClickListener { pinchZoom.stop();action(); revealControls() }
            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) handler.removeCallbacks(hideControls)
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) scheduleHide()
                false
            }
            parent?.addView(this, LinearLayout.LayoutParams(dp(MapControlDesign.dashboardWidth), dp(MapControlDesign.buttonHeight)).apply {
                if (parent.orientation == LinearLayout.VERTICAL) topMargin = dp(MapControlDesign.buttonGap) else marginStart = dp(MapControlDesign.buttonGap)
            })
        }
        centerButton = icon("", context.getString(R.string.ui_map_center)) {
            setFollow(true); requestGps(); stopMotion(); receivePosition()
        }
        directionButton = icon("", context.getString(R.string.ui_map_orientation)) {
            heading=!heading
            preferences.edit().putBoolean("mapHeading",heading).putBoolean(MapOvalAnchor.preference,false).apply()
            ovalAnchor.reset();setFollow(true);requestGps();stopMotion();receivePosition()
        }.apply { tag="map_follow_mode" }
        icon("+", context.getString(R.string.ui_map_zoom_in), zoomControls) { zoom(1) }
        icon("−", context.getString(R.string.ui_map_zoom_out), zoomControls) { zoom(-1) }
        fullButton = icon("", context.getString(R.string.ui_map_fullscreen)) { fullscreen() }
        fun graphic(button: Button, symbol: BicycleIcon) = MapControlDesign.icon(button, symbol)
        graphic(centerButton, BicycleIcon.Locate)
        graphic(directionButton, BicycleIcon.Compass)
        graphic(fullButton, BicycleIcon.Maximize)
        addView(controls, LayoutParams(-2, dp(MapControlDesign.buttonHeight), Gravity.TOP or Gravity.END).apply { topMargin = dp(MapControlDesign.edgeGap); marginEnd = dp(MapControlDesign.edgeGap) })
        nightButton = icon("", context.getString(R.string.ui_map_night_enable), null) {
            night = !night; preferences.edit().putBoolean("mapNight", night).apply(); applyNight()
        }.apply { tag = "map_night" }
        addView(nightButton, LayoutParams(dp(MapControlDesign.dashboardWidth), dp(MapControlDesign.buttonHeight), Gravity.TOP or Gravity.START).apply { topMargin = dp(MapControlDesign.edgeGap); marginStart = dp(MapControlDesign.nightOffset) })
        routeButton = icon("", context.getString(R.string.ui_map_track_toggle), null) {
            runtime.mapState.setRouteVisible(!preferences.getBoolean("mapGpxVisible", true))
        }.apply {
            tag="map_route_toggle"; gravity = Gravity.CENTER; setPadding(dp(MapControlDesign.trackHorizontalPadding), dp(MapControlDesign.trackTopPadding), dp(MapControlDesign.trackHorizontalPadding), 0)
        }
        refreshRouteButton()
        addView(routeButton, LayoutParams(dp(MapControlDesign.dashboardWidth), dp(MapControlDesign.buttonHeight), Gravity.TOP or Gravity.START).apply { topMargin = dp(MapControlDesign.edgeGap); marginStart = dp(MapControlDesign.edgeGap) })
        addView(zoomControls, LayoutParams(dp(MapControlDesign.dashboardWidth), -2, Gravity.BOTTOM or Gravity.END).apply { bottomMargin = 0; marginEnd = dp(MapControlDesign.edgeGap) })
        dataFields.reload(preferences)
        dataFields.bindEditorTheme(runtime.themes)
        dataFields.selectField = { selectMapField?.invoke(it) }
        addView(editorFinish, LayoutParams(-2, -2, Gravity.CENTER))
        addView(dataFields, LayoutParams(-1, dp(62), Gravity.BOTTOM).apply { bottomMargin = 0 })
        credit.apply {
            text = source.attribution
            contentDescription = context.getString(R.string.ui_map_attribution)
            setOnClickListener { openCredit(source.creditUrl) }
        }
        addView(credit, LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START))
        status.apply {
            textSize = BicycleDesign.mapStatusSp; setTextColor(Color.BLACK); setBackgroundColor(Color.argb(220, 255, 255, 255))
            setPadding(dp(5), dp(3), dp(5), dp(3)); maxLines = 1
            setOnClickListener { requestGps(); revealControls() }
        }
        addView(status, LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START).apply { bottomMargin = dp(23) })
        mapView.model.mapViewPosition.addObserver(zoomObserver)
        applyNight()
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateCamera() }
    }

    private fun createDownloadLayer(source: MapSource): MapNightLayer {
        // A bounded normal cache, never a region prefetch/offline download.
        val memory = ViewportTileCache()
        val disk = FileSystemTileCache(512, File(context.cacheDir, "tiles-${source.name}").apply { mkdirs() }, AndroidGraphicFactory.INSTANCE, true)
        return MapNightLayer(TwoLevelTileCache(memory, disk), mapView.model.mapViewPosition, source.tiles(providerKey)).apply { night = this@MapPanel.night }
    }
    private fun applyNight() {
        downloads.night = night
        val color = if (night) MapNightPalette.background else Color.rgb(225, 225, 225)
        setBackgroundColor(color); mapView.model.displayModel.backgroundColor = color
        val icon = if (night) BicycleIcon.Moon else BicycleIcon.Sun
        nightButton.contentDescription = if (night) context.getString(R.string.ui_map_day_enable) else context.getString(R.string.ui_map_night_enable)
        MapControlDesign.icon(nightButton, icon)
        downloads.requestRedraw(); refreshRoutes()
    }
    fun resume() { motion.reset();ovalAnchor.hold(android.os.SystemClock.uptimeMillis()); lastTrack = -1L; running = true; mapView.layerManager.proceed(); downloads.onResume(); revealControls(); refresh() }
    fun pause() {
        pinchZoom.cancel()
        running = false; phoneHeading.stop(); stopMotion(); handler.removeCallbacksAndMessages(null); downloads.onPause(); mapView.layerManager.pause(); saveCamera()
    }
    fun destroy() { mapView.model.mapViewPosition.removeObserver(zoomObserver);pause(); mapView.destroyAll() }
    internal fun refreshProfileSettings() {
        pinchZoom.cancel()
        dataFields.resetTrends()
        refreshSettings()
        mapView.model.mapViewPosition.setZoom(MapZoom.read(preferences,source),false)
        val lat=preferences.getFloat("mapLatitude",51f).toDouble().coerceIn(-85.0,85.0)
        val lon=preferences.getFloat("mapLongitude",10f).toDouble().coerceIn(-180.0,180.0)
        mapView.setCenter(LatLong(lat,lon))
    }
    fun refreshSettings() {
        val next = MapProviderSettings.source(context,preferences.getString("mapSource", null))
        val nextKey=MapProviderSettings.read(context)
        if (next != source || (next==MapSource.OPENCYCLEMAP && nextKey!=providerKey)) {
            pinchZoom.cancel()
            mapView.layerManager.layers.remove(downloads); downloads.onDestroy(); downloads.tileCache.destroy()
            source = next; providerKey=nextKey
            mapView.setZoomLevelMax(MapZoom.maximum(source).toByte())
            if(mapView.model.mapViewPosition.zoom>MapZoom.maximum(source)) mapView.model.mapViewPosition.setZoom(MapZoom.maximum(source).toDouble(),false)
            downloads = createDownloadLayer(source); mapView.layerManager.layers.add(0, downloads)
            if (running) downloads.onResume()
            credit.text = source.attribution
        }
        night = preferences.getBoolean("mapNight", false); applyNight()
        dataFields.reload(preferences)
        refreshRouteButton()
        refreshDataFields()
        if(preferences.getBoolean(MapOvalAnchor.preference,false)) {
            preferences.edit().putBoolean("mapHeading",false).putBoolean(MapOvalAnchor.preference,false).apply()
        }
        heading = preferences.getBoolean("mapHeading",true)
        follow = preferences.getBoolean("mapFollow", true)
        stopMotion(); updateCamera(); refreshRoutes()
    }
    fun refresh() {
        if (!running) return
        gps = runtime.mapGpsValues
        phoneHeading.updateLocation(gps);phoneHeading.setEnabled(headingVisible());refreshPhoneHeading()
        if (motion.active && !runtime.isMoving) { stopMotion(); updateCamera(); refreshRoutes() }
        updateMotionControls()
        refreshRouteButton()
        refreshDataFields()
        val track = runtime.mapState.track.revision; val route = runtime.mapState.routeRevision
        // BLE packets and elapsed-time changes must not redraw an unchanged map.
        if (gps.latitude != lastLatitude || gps.longitude != lastLongitude || gps.bearingDegrees != lastBearing || gps.altitudeM != lastAltitude ||
            track != lastTrack || route != lastRoute) {
            lastLatitude = gps.latitude; lastLongitude = gps.longitude; lastBearing = gps.bearingDegrees; lastAltitude = gps.altitudeM
            lastTrack = track; lastRoute = route; receivePosition()
        }
        val next = if (!runtime.gpsEnabled) "GPS aus · antippen" else if (gps.latitude == null) "GPS · Warte auf Position"
            else if (runtime.recording && !runtime.mapTrackEnabled) "Track-Aufzeichnung aus" else ""
        if (status.text.toString() != next) status.text = next
        status.visibility = if (next.isEmpty()) GONE else VISIBLE
    }
    private fun refreshRouteButton() {
        val available = runtime.mapState.route != null
        val shown = available && preferences.getBoolean("mapGpxVisible", true)
        routeButton.isEnabled = available && !runtime.mapState.busy && !editing
        routeButton.alpha = if (routeButton.isEnabled) 1f else .45f
        routeButton.contentDescription = if (!available) context.getString(R.string.ui_map_route_missing) else if (shown) context.getString(R.string.ui_map_route_hide) else context.getString(R.string.ui_map_route_show)
        if (routeShown != shown) {
            val image = BicycleIcons.drawable(context, BicycleIcon.ShowOnMap, Color.WHITE, negative = !shown)
                .apply { setBounds(0, 0, dp(MapControlDesign.trackIconSize), dp(MapControlDesign.trackIconSize)) }
            routeButton.setCompoundDrawables(null, image, null, null)
            routeShown = shown
        }
    }
    private fun refreshRoutes() {
        if (layerTrackRevision != runtime.mapState.track.revision) {
            layerTrackRevision = runtime.mapState.track.revision
            layerTrack = runtime.mapState.track.points.toList()
        }
        routes.update(runtime.mapState.route, layerTrack, displayedGps,
            preferences.getBoolean("mapGpxVisible", true), preferences.getBoolean("mapTrackVisible", true), night, displayedPhoneHeading,
            MapRouteStyles.read(preferences,MapLineKind.RECORDED,night),MapRouteStyles.read(preferences,MapLineKind.SAVED,night))
    }
    private fun refreshDataFields() {
        dataFields.show(runtime.dashboardBikeData, runtime.gpsValues, runtime.ride.bike.distanceM, runtime.pulseValues, runtime.recording, runtime.currentBikeLight,runtime.trendBikeData)
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dataLayout=dataFields.layoutParams
        val neededHeight=dp(dataFields.preferredHeightDp)
        if(dataLayout.height!=neededHeight) { dataLayout.height=neededHeight;dataFields.layoutParams=dataLayout }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Reserve a narrow left strip for the rotated attribution, including wrapped text on short maps.
        val creditInset = credit.measuredWidth + dp(4)
        val fieldMargin = dataFields.layoutParams as LayoutParams
        val statusMargin = status.layoutParams as LayoutParams
        val zoomMargin = zoomControls.layoutParams as LayoutParams
        val nextFieldMargin = 0
        val nextStatusMargin = nextFieldMargin + if (dataFields.visibility == VISIBLE) dataFields.measuredHeight else 0
        // Keep zoom above the complete value row so the right-hand field remains unobstructed.
        val nextZoomMargin = nextFieldMargin+dataFields.measuredHeight+dp(BicycleDesign.smallGap)
        // Short map panels still need both zoom buttons; reserve their column if it meets the top row.
        val topHeight = (measuredHeight-nextZoomMargin-dp(MapControlDesign.topRowReserve)).coerceIn(dp(MapControlDesign.compactMinHeight),dp(MapControlDesign.buttonHeight))
        val buttonHeight = ((measuredHeight-nextZoomMargin-topHeight-dp(MapControlDesign.zoomClearance))/2).coerceIn(dp(MapControlDesign.compactMinHeight),dp(MapControlDesign.buttonHeight))
        val topMargin = controls.layoutParams as LayoutParams
        val topChanged = controls.layoutParams.height != topHeight || nightButton.layoutParams.height != topHeight ||
            routeButton.layoutParams.height != topHeight
        controls.layoutParams.height = topHeight
        for (i in 0 until controls.childCount) controls.getChildAt(i).layoutParams.height = topHeight
        val nightMargin = nightButton.layoutParams as LayoutParams
        nightMargin.height = topHeight
        routeButton.layoutParams.height=topHeight
        val nextTopEnd = if (measuredHeight - nextZoomMargin - 2 * buttonHeight - dp(MapControlDesign.collisionGap) < topHeight + dp(MapControlDesign.edgeGap)) dp(MapControlDesign.compactRowOffset) else dp(MapControlDesign.edgeGap)
        var changed = topChanged
        val leftEdge=dp(MapControlDesign.nightOffset+MapControlDesign.dashboardWidth+MapControlDesign.collisionGap)
        val controlWidth=((measuredWidth-nextTopEnd-leftEdge)/controls.childCount-dp(MapControlDesign.buttonGap))
            .coerceIn(dp(MapControlDesign.compactMinWidth),dp(MapControlDesign.dashboardWidth))
        for(index in 0 until controls.childCount) {
            val child=controls.getChildAt(index)
            if(child.layoutParams.width!=controlWidth) {
                child.layoutParams.width=controlWidth
                val padding=((controlWidth-dp(MapControlDesign.iconSize))/2).coerceAtLeast(0)
                child.setPadding(padding,0,padding,0);changed=true
            }
        }
        for (index in 0 until zoomControls.childCount) {
            val params = zoomControls.getChildAt(index).layoutParams
            if (params.height != buttonHeight) { params.height = buttonHeight; changed = true }
        }
        if (changed || fieldMargin.marginStart != creditInset || statusMargin.marginStart != creditInset || fieldMargin.bottomMargin != nextFieldMargin || statusMargin.bottomMargin != nextStatusMargin ||
            zoomMargin.bottomMargin != nextZoomMargin || topMargin.marginEnd != nextTopEnd) {
            fieldMargin.bottomMargin = nextFieldMargin; fieldMargin.marginStart = creditInset
            statusMargin.bottomMargin = nextStatusMargin; statusMargin.marginStart = creditInset
            dataFields.layoutParams = fieldMargin; status.layoutParams = statusMargin
            zoomMargin.bottomMargin = nextZoomMargin; topMargin.marginEnd = nextTopEnd
            controls.layoutParams = topMargin
            nightButton.layoutParams = nightMargin
            routeButton.layoutParams=routeButton.layoutParams
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
    fun showFullscreen(value: Boolean) {
        MapControlDesign.icon(fullButton, if(value) BicycleIcon.Maximize else BicycleIcon.Minimize)
        fullButton.contentDescription = if (value) context.getString(R.string.ui_map_return_dashboard) else context.getString(R.string.ui_map_fullscreen); revealControls() }
    fun setFollow(value: Boolean) {
        if (follow == value) return
        if (!value) { ovalAnchor.hold(android.os.SystemClock.uptimeMillis());refreshRoutes() }
        follow = value; preferences.edit().putBoolean("mapFollow", value).apply()
        if(::directionButton.isInitialized) { directionButton.isEnabled=value && !editing;directionButton.alpha=if(value) 1f else .4f }
        if (::centerButton.isInitialized) centerButton.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(if (value) Color.rgb(110, 230, 130) else Color.WHITE)
    }
    private fun updateCamera(updateControls: Boolean = true, forceRotation:Boolean=false) {
        if (width <= 0 || height <= 0) return
        if((follow || forceRotation) && !touchingMap) {
            val bearing=if(heading) displayedGps.bearingDegrees else null
            val now=android.os.SystemClock.uptimeMillis()
            val position=ovalAnchor.at(if(heading) 0.0 else displayedGps.bearingDegrees,runtime.isMoving && displayedGps.latitude!=null,
                now,width,height,mapView.model.mapViewPosition.zoom)
            if(follow && mapView.mapViewCenterX!=position.x) mapView.setMapViewCenterX(position.x)
            if(follow && mapView.mapViewCenterY!=position.y) mapView.setMapViewCenterY(position.y)
            val rotation=Rotation(-(bearing ?: 0.0).toFloat(),width/2f,height/2f)
            if(mapView.mapRotation!=rotation) mapView.rotate(rotation)
            if(follow && displayedGps.latitude!=null && displayedGps.longitude!=null && abs(displayedGps.latitude!!)<=85.05112878) {
                val center=LatLong(displayedGps.latitude!!,displayedGps.longitude!!)
                if(mapView.model.mapViewPosition.center!=center) mapView.setCenter(center)
            }
        }
        if (!updateControls) return
        val effectiveHeading=heading
        if(directionIconHeading!=effectiveHeading) {
            directionIconHeading=effectiveHeading
            MapControlDesign.icon(directionButton, if(effectiveHeading) BicycleIcon.HeadingUp else BicycleIcon.Compass)
        }
        directionButton.isEnabled=follow && !editing
        directionButton.alpha=if(follow) 1f else .4f
        directionButton.stateDescription=context.getString(if(follow) R.string.ui_selected else R.string.ui_not_selected)
        directionButton.contentDescription = context.getString(if(effectiveHeading) R.string.map_follow_heading else R.string.map_follow_north)
        centerButton.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(if (follow) Color.rgb(110, 230, 130) else Color.WHITE)
    }
    fun fitRoute() {
        val points = runtime.mapState.route?.points ?: return
        if (points.isEmpty() || width <= 0 || height <= 0) return
        setFollow(false)
        mapView.setMapViewCenterX(.5f);mapView.setMapViewCenterY(.5f)
        val lats = points.map { it.latitude }; val lons = points.map { it.longitude }
        val minLat = lats.min(); val maxLat = lats.max(); val minLon = lons.min(); val maxLon = lons.max()
        mapView.setCenter(LatLong((minLat + maxLat) / 2, (minLon + maxLon) / 2))
        mapView.rotate(Rotation.NULL_ROTATION)
        var level = MapZoom.maximum(source)
        while (level > MapZoom.minimum) {
            val size = MercatorProjection.getMapSize(level.toByte(), 256)
            val dx = abs(MercatorProjection.longitudeToPixelX(maxLon, size) - MercatorProjection.longitudeToPixelX(minLon, size))
            val dy = abs(MercatorProjection.latitudeToPixelY(maxLat, size) - MercatorProjection.latitudeToPixelY(minLat, size))
            if (dx < width * .8 && dy < height * .7) break
            level--
        }
        mapView.model.mapViewPosition.setZoomLevel(level.toByte(), false); revealControls(); refreshRoutes()
    }
    private fun zoom(delta: Int) {
        pinchZoom.stop()
        mapView.model.mapViewPosition.setZoom((mapView.model.mapViewPosition.zoom + delta*.5).coerceIn(MapZoom.minimum.toDouble(),MapZoom.maximum(source).toDouble()), false)
        saveCamera()
    }
    private fun saveCamera() {
        val position = mapView.model.mapViewPosition
        preferences.edit().putInt("mapZoom", position.zoomLevel.toInt()).putFloat(MapZoom.preference,position.zoom.toFloat()).putFloat("mapLatitude", position.center.latitude.toFloat())
            .putFloat("mapLongitude", position.center.longitude.toFloat()).apply()
    }
    fun revealControls() { controls.visibility = VISIBLE; zoomControls.visibility = VISIBLE; routeButton.visibility = VISIBLE; nightButton.visibility = VISIBLE; controlsVisibilityChanged?.invoke(true); scheduleHide() }
    fun holdControls() { handler.removeCallbacks(hideControls) }
    private fun updateMotionControls() {
        val next = runtime.isMoving
        if (next == moving) {
            if (!next && controls.visibility != VISIBLE) revealControls()
            return
        }
        moving = next
        if (next) scheduleHide() else revealControls()
    }
    private fun scheduleHide() {
        handler.removeCallbacks(hideControls)
        if (running && !editing && runtime.isMoving && !context.getSystemService(AccessibilityManager::class.java).isTouchExplorationEnabled)
            handler.postDelayed(hideControls, 5_000)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

}
