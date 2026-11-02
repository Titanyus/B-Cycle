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
import kotlin.math.abs

enum class MapSource(val title: String, val host: String, val path: String, val attribution: String, val creditUrl: String, val maximumZoom: Int = 19) {
    CYCLOSM("CyclOSM · Fahrradkarte", "a.tile-cyclosm.openstreetmap.fr", "/cyclosm/", "CyclOSM · © OpenStreetMap", "https://www.cyclosm.org/legend.html"),
    OSM("OpenStreetMap · Standardkarte", "tile.openstreetmap.org", "/", "© OpenStreetMap", "https://www.openstreetmap.org/copyright"),
    OPENTOPOMAP("OpenTopoMap", "a.tile.opentopomap.org", "/", "© OpenStreetMap · SRTM · OpenTopoMap (CC-BY-SA)", "https://opentopomap.org/about",17),
    OPENCYCLEMAP("OpenCycleMap", "tile.thunderforest.com", "/cycle/", "© Thunderforest · © OpenStreetMap", "https://www.thunderforest.com/terms/"),
    VECTOR("OpenFreeMap · Liberty", "tiles.openfreemap.org", "/planet", "OpenFreeMap · © OpenMapTiles · © OpenStreetMap", "https://openfreemap.org/");
    fun label(context: Context) = context.getString(when(this) {
        CYCLOSM -> R.string.ui_map_source_cyclosm; OSM -> R.string.ui_map_source_osm
        OPENTOPOMAP -> R.string.ui_map_source_topo; OPENCYCLEMAP -> R.string.ui_map_source_cycle
        VECTOR -> R.string.ui_map_source_vector
    })
    fun tiles(apiKey:String=""):org.oscim.tiling.source.bitmap.BitmapTileSource {
        require(this!=VECTOR) { "Vector source uses MVT, not raster tiles" }
        if(this==OPENCYCLEMAP) require(MapProviderSettings.valid(apiKey)) { "Thunderforest API key required" }
        val suffix=if(this==OPENCYCLEMAP) "?apikey="+java.net.URLEncoder.encode(apiKey,"UTF-8") else ""
        return org.oscim.tiling.source.bitmap.BitmapTileSource("https://$host",path+"{Z}/{X}/{Y}.png"+suffix,0,maximumZoom).apply {
            setHttpRequestHeaders(mapOf("User-Agent" to "B-Cycle/${AppVersion.name} (https://github.com/Titanyus/B-Cycle)"))
        }
    }
    companion object { fun decode(id: String?) = entries.firstOrNull { it.name == id } ?: CYCLOSM }
}

/** One resizable map. Bike/GPS ownership stays in RideRuntime; phone heading is display-only. */
class MapPanel(context: Context, private val runtime: RideRuntime, private val fullscreen: () -> Unit,
               private val requestGps: () -> Unit,
               private val openCredit: (String) -> Unit) : FrameLayout(context) {
    private val preferences = runtime.preferences
    private val handler = Handler(Looper.getMainLooper())
    private val windRose=android.widget.ImageView(context).apply {
        tag="map_wind_rose";visibility=GONE
        val north=context.getDrawable(R.drawable.map_wind_rose_north)!!.mutate()
        setImageDrawable(android.graphics.drawable.LayerDrawable(arrayOf(context.getDrawable(R.drawable.map_wind_rose)!!,north)))
        BicycleComponents.bindTheme(this,runtime.themes) { north.setTint(it.foreground) }
        alpha=.70f;scaleX=MapControlDesign.windRoseScale;scaleY=MapControlDesign.windRoseScale
        scaleType=android.widget.ImageView.ScaleType.FIT_CENTER
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable=false;isFocusable=false
    }
    private val controls = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val zoomControls = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; tag = "map_zoom_controls" }
    private lateinit var routeButton: Button
    private var routeShown: Boolean? = null
    private var routePopup:MapRouteSlotPopup?=null
    private val credit = MapCreditView(context)
    private var observedZoom=Double.NaN
    private val updateZoom=Runnable {
        val zoom=mapView.zoom
        if(zoom!=observedZoom) { observedZoom=zoom;updateCamera();if(running && follow) { handler.removeCallbacks(animateCamera);handler.post(animateCamera) } }
    }
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
        if(value) routePopup?.dismiss()
        dataFields.setEditing(value, runtime.themes.active.tokens.primary)
        editorFinish.visibility = if (value) VISIBLE else GONE
        for (i in 0 until controls.childCount) controls.getChildAt(i).isEnabled = !value
        for (i in 0 until zoomControls.childCount) zoomControls.getChildAt(i).apply {
            isEnabled = !value
            alpha = if (value) .45f else 1f
        }
        directionButton.isEnabled = !value && follow
        directionButton.alpha=if(follow) 1f else .4f
        nightButton.isEnabled = false
        refreshRouteButton()
        if (value) handler.removeCallbacks(hideControls) else scheduleHide()
        refreshDataFields();updateWindRose()
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
    private var downloads: VtmBaseMap
    private var cycleways=preferences.getBoolean(VtmBaseMap.cyclewaysKey,true)
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
        if (!running || editing || !runtime.isMoving || routePopup?.isShowing==true) return@Runnable
        controls.visibility = INVISIBLE; zoomControls.visibility = GONE; routeButton.visibility = GONE; nightButton.visibility = GONE
        updateWindRose();controlsVisibilityChanged?.invoke(false)
    }
    val mapView: BicycleMapView
    private val pinchZoom:MapPinchZoom
    private val routes by lazy { RouteTrackLayer(context,mapView) }

    init {
        setBackgroundColor(Color.rgb(225,225,225))
        mapView=BicycleMapView(context).apply {
            setZoomLimits(MapZoom.maximum(source));setZoom(MapZoom.read(preferences,source))
            setCenter(preferences.getFloat("mapLatitude",51f).toDouble().coerceIn(-85.0,85.0),preferences.getFloat("mapLongitude",10f).toDouble().coerceIn(-180.0,180.0))
            pauseMap()
        }
        pinchZoom=MapPinchZoom(mapView) { MapZoom.maximum(source) }
        addView(mapView,LayoutParams(-1,-1))
        downloads=createDownloadLayer(source)
        addView(routes,LayoutParams(-1,-1))
        mapView.userMoved={ setFollow(false);revealControls() }
        mapView.zoomed={ revealControls() }
        mapView.cameraChanged={ handler.removeCallbacks(updateZoom);handler.post(updateZoom);updateWindRose() }
        val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean { revealControls(); return true }
            override fun onScroll(first:MotionEvent?,next:MotionEvent,dx:Float,dy:Float):Boolean {
                if(next.pointerCount==1) mapView.notifyUserMove()
                return false
            }
        })
        mapView.setOnTouchListener { _, event ->
            if (editing) return@setOnTouchListener true
            detector.onTouchEvent(event)
            // VTM handles one-finger pan/fling; the shared controller handles continuous pinch.
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
        addView(windRose,LayoutParams(dp(MapControlDesign.iconSize),dp(MapControlDesign.iconSize)))
        nightButton = icon("", context.getString(R.string.ui_map_night_enable), null) {
            night = !night; preferences.edit().putBoolean("mapNight", night).apply(); applyNight()
        }.apply { tag = "map_night" }
        addView(nightButton, LayoutParams(dp(MapControlDesign.dashboardWidth), dp(MapControlDesign.buttonHeight), Gravity.TOP or Gravity.START).apply { topMargin = dp(MapControlDesign.edgeGap); marginStart = dp(MapControlDesign.nightOffset) })
        routeButton = icon("", context.getString(R.string.ui_map_track_toggle), null) {
            runtime.mapState.setRouteVisible(runtime.mapState.visibleRouteIds().isEmpty())
        }.apply {
            setOnLongClickListener {
                if(!editing && !runtime.mapState.busy && runtime.mapState.mapRoutes.isNotEmpty()) {
                    holdControls()
                    routePopup?.dismiss()
                    routePopup=MapRouteSlotPopup(this,runtime) { scheduleHide() }.also { it.show() }
                }
                true
            }
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
        applyNight()
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateCamera() }
    }

    private fun createDownloadLayer(source:MapSource)=VtmBaseMap(context,mapView.map(),source,providerKey,cycleways)
    private fun applyNight() {
        // Saved preference remains compatible; inverted raster tiles are deferred during the VTM migration.
        nightButton.visibility=GONE;nightButton.isEnabled=false
        setBackgroundColor(Color.rgb(225,225,225));refreshRoutes()
    }
    fun resume() { motion.reset();ovalAnchor.hold(android.os.SystemClock.uptimeMillis());lastTrack=-1L;running=true;mapView.resumeMap();downloads.resume();revealControls();refresh() }
    fun pause() {
        routePopup?.dismiss();pinchZoom.cancel();running=false;phoneHeading.stop();stopMotion()
        handler.removeCallbacksAndMessages(null);downloads.pause();mapView.pauseMap();saveCamera()
    }
    fun destroy() { pause();routes.dispose();mapView.disposeMap() }
    internal fun refreshProfileSettings() {
        pinchZoom.cancel()
        dataFields.resetTrends()
        refreshSettings()
        mapView.setZoom(MapZoom.read(preferences,source))
        val lat=preferences.getFloat("mapLatitude",51f).toDouble().coerceIn(-85.0,85.0)
        val lon=preferences.getFloat("mapLongitude",10f).toDouble().coerceIn(-180.0,180.0)
        mapView.setCenter(lat,lon)
    }
    fun refreshSettings() {
        val next = MapProviderSettings.source(context,preferences.getString("mapSource", null))
        val nextKey=MapProviderSettings.read(context)
        val nextCycleways=preferences.getBoolean(VtmBaseMap.cyclewaysKey,true)
        if (next != source || (next==MapSource.OPENCYCLEMAP && nextKey!=providerKey) || (next==MapSource.VECTOR && nextCycleways!=cycleways)) {
            pinchZoom.cancel()
            downloads.onDetach()
            source = next; providerKey=nextKey;cycleways=nextCycleways
            mapView.setZoomLimits(MapZoom.maximum(source))
            if(mapView.zoom>MapZoom.maximum(source)) mapView.setZoom(MapZoom.maximum(source).toDouble())
            downloads = createDownloadLayer(source)
            if (running) downloads.resume()
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
        val available = runtime.mapState.mapRoutes.isNotEmpty()
        val shown = runtime.mapState.visibleRouteIds().isNotEmpty()
        routePopup?.takeIf { it.isShowing }?.refresh()
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
            MapRouteStyles.read(preferences,MapLineKind.RECORDED,night),MapRouteStyles.read(preferences,MapLineKind.SAVED,night),
            preferences.getBoolean("mapHeightVisible",true),
            trackGeneration=runtime.mapState.track.generation,profileGeneration=runtime.profileRevision,
            overlays=runtime.mapState.mapRoutes.filter { runtime.mapState.routeVisible(it.id) }.map { StyledMapRoute(it.route,MapRouteStyles.read(preferences,MapRouteStyles.saved(it.slot),night)) })
    }
    private fun refreshDataFields() {
        dataFields.show(runtime.dashboardBikeData, runtime.gpsValues, runtime.ride.bike.distanceM, runtime.pulseValues, runtime.recording, null,runtime.trendSnapshot)
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
    override fun onLayout(changed:Boolean,left:Int,top:Int,right:Int,bottom:Int) {
        super.onLayout(changed,left,top,right,bottom)
        // Retain the button row's geometry while invisible, including compact map layouts.
        val size=dp(MapControlDesign.iconSize)
        val x=controls.left+fullButton.left+(fullButton.width-size)/2
        val y=controls.top+fullButton.top+(fullButton.height-size)/2
        windRose.layout(x,y,x+size,y+size)
        updateWindRose()
    }
    private fun updateWindRose() {
        windRose.visibility=if(follow && !editing && controls.visibility!=VISIBLE && preferences.getBoolean("mapWindRoseVisible",false)) VISIBLE else GONE
        windRose.rotation=mapView.bearing
    }
    fun showFullscreen(value: Boolean) {
        MapControlDesign.icon(fullButton, if(value) BicycleIcon.Maximize else BicycleIcon.Minimize)
        fullButton.contentDescription = if (value) context.getString(R.string.ui_map_return_dashboard) else context.getString(R.string.ui_map_fullscreen); revealControls() }
    fun setFollow(value: Boolean) {
        if (follow == value) return
        if (!value) { ovalAnchor.hold(android.os.SystemClock.uptimeMillis());refreshRoutes() }
        follow = value; preferences.edit().putBoolean("mapFollow", value).apply()
        updateWindRose()
        if(::directionButton.isInitialized) { directionButton.isEnabled=value && !editing;directionButton.alpha=if(value) 1f else .4f }
        if (::centerButton.isInitialized) centerButton.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(if (value) Color.rgb(110, 230, 130) else Color.WHITE)
    }
    private fun updateCamera(updateControls: Boolean = true, forceRotation:Boolean=false) {
        if (width <= 0 || height <= 0) return
        if((follow || forceRotation) && !touchingMap) {
            val bearing=if(heading) displayedGps.bearingDegrees else null
            val now=android.os.SystemClock.uptimeMillis()
            val position=ovalAnchor.at(if(heading) 0.0 else displayedGps.bearingDegrees,runtime.isMoving && displayedGps.latitude!=null,
                now,width,height,mapView.zoom)
            if(follow) mapView.setAnchor(position.x,position.y)
            val rotation=-(bearing ?: 0.0).toFloat()
            if(mapView.bearing!=rotation) mapView.rotate(rotation)
            if(follow && displayedGps.latitude!=null && displayedGps.longitude!=null && abs(displayedGps.latitude!!)<=85.05112878) {
                if(mapView.center.latitude!=displayedGps.latitude || mapView.center.longitude!=displayedGps.longitude) mapView.setCenter(displayedGps.latitude!!,displayedGps.longitude!!)
            }
        }
        updateWindRose()
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
        val visible=runtime.mapState.mapRoutes.filter { runtime.mapState.routeVisible(it.id) }
        val points=(visible.ifEmpty { runtime.mapState.mapRoutes }).flatMap { it.route.points }
        if (points.isEmpty() || width <= 0 || height <= 0) return
        setFollow(false)
        mapView.setAnchor(.5f,.5f)
        val lats = points.map { it.latitude }; val lons = points.map { it.longitude }
        val minLat = lats.min(); val maxLat = lats.max(); val minLon = lons.min(); val maxLon = lons.max()
        mapView.setCenter((minLat+maxLat)/2,(minLon+maxLon)/2)
        mapView.rotate(0f)
        var level = MapZoom.maximum(source)
        while (level > MapZoom.minimum) {
            val size = (256L shl level)
            val dx = abs((maxLon-minLon)/360*size)
            val dy = abs((org.oscim.core.MercatorProjection.latitudeToY(maxLat)-org.oscim.core.MercatorProjection.latitudeToY(minLat))*size)
            if (dx < width * .8 && dy < height * .7) break
            level--
        }
        mapView.setZoom(level.toDouble()); revealControls(); refreshRoutes()
    }
    private fun zoom(delta: Int) {
        pinchZoom.stop()
        mapView.setZoom((mapView.zoom+delta*.5).coerceIn(MapZoom.minimum.toDouble(),MapZoom.maximum(source).toDouble()))
        saveCamera()
    }
    private fun saveCamera() { mapView.save(preferences) }
    fun revealControls() { controls.visibility = VISIBLE; zoomControls.visibility = VISIBLE; routeButton.visibility = VISIBLE; nightButton.visibility = GONE; updateWindRose();controlsVisibilityChanged?.invoke(true); scheduleHide() }
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
