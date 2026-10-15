package de.bcycle.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import java.io.IOException

/** Native Views remain the UI; the runtime also survives while the recording service is active. */
class MainActivity : Activity() {
    private lateinit var runtime: RideRuntime
    private lateinit var dashboard: DashboardView
    private lateinit var startButton: Button
    private lateinit var mapPanel: MapPanel
    private lateinit var dashboardArea: LinearLayout
    private var routeManager: RouteManagerView? = null
    private var routeImportButton: BicycleFooterAction? = null
    private var routePopup: ListPopupWindow? = null
    private var routeSignature = ""
    private val settingsWriter = java.util.concurrent.Executors.newSingleThreadExecutor { task -> Thread(task,"Bicycle-settings") }
    private var exportDraft: RouteExportConfig? = null
    private var exportPasswordDraft: String? = null
    private var pendingBackupExport: ByteArray? = null
    private var dataSettingsScreen: AlertDialog? = null
    private var pendingMapRoute: String? = null
    private var dashboardPage = 0
    private var mapFullscreen = false
    private var controlsVisible = true
    private var displayedMapMessage = ""
    private val dashboardKey get() = if (dashboardPage == 0) "dashboard" else "dashboard_$dashboardPage"
    private val templateKey get() = "dashboardTemplate_$dashboardPage"
    private fun currentTemplate() = DashboardTemplate.decode(preferences.getString(templateKey, DashboardTemplate.entries[dashboardPage].name))
    private var dashboardLayout = DashboardLayout.defaults
    private var dashboardDataVisibility = DashboardDataVisibility()
    private var dashboardGraphs = GraphOverlayConfig()
    private var dashboardZoneGlow = ZoneGlowConfig()
    private val trainingPresentation = TrainingPresentation()
    private var trainingSnapshot = TrainingSnapshot()
    private val mapBack = android.window.OnBackInvokedCallback { setMapFullscreen(false) }
    private var mapBackRegistered = false
    private fun updateMapBack() {
        if (mapFullscreen == mapBackRegistered) return
        if (mapFullscreen) onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,mapBack)
        else onBackInvokedDispatcher.unregisterOnBackInvokedCallback(mapBack)
        mapBackRegistered=mapFullscreen
    }
    internal var dashboardEditing = false; private set
    internal fun setDashboardEditing(value: Boolean) {
        dashboardEditing = value
        dashboard.setEditing(value); mapPanel.setEditing(value)
        if (value && mapFullscreen) setMapFullscreen(false)
    }
    private fun finishDashboardEditor() {
        // commit waits for the existing field/graph apply writes; retain EDIT on a reported disk failure.
        if (!preferences.edit().commit()) {
            BicycleFeedback.show(this, R.string.dashboard_editor_save_failed, long = true); return
        }
        setDashboardEditing(false)
    }
    private fun enterDashboardEditor() {
        dialogs.closeAll(); setDashboardEditing(true)
    }
    private var picker: AlertDialog? = null
    private var menu: AlertDialog? = null
    private data class DetailSession(val id: String, val screen: AlertDialog, val content: RouteAnalysisView, val footer: LinearLayout, var analysis: RouteAnalysis? = null, var signature: String = "")
    private var routeDetail: DetailSession? = null
    private var sensorManager: AlertDialog? = null
    private var bikesManager: BikesView? = null
    private val expandedFitSources = mutableSetOf<String>()
    private var scan: BikeSensorScanner? = null
    private var pendingSensor: String? = null
    private var pendingStart = false
    private var pendingReport: String? = null
    private var displayedRecordingStatus: String? = null
    private val observer: () -> Unit = { render(); refreshSensorRows(); refreshRouteRows() }
    private val preferences get() = runtime.preferences
    private val settingsContext by lazy { android.view.ContextThemeWrapper(this, R.style.SettingsDialogTheme) }
    private val dialogs = DialogNavigation()
    private fun dialog(fullscreen: Boolean = true) = BicycleDialogBuilder(this, runtime.themes, dialogs, fullscreen)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(text: String, size: Float = BicycleDesign.bodySp) = BicycleComponents.text(settingsContext, runtime.themes, text).apply {
        isAccessibilityHeading = size >= BicycleDesign.inputSp
        BicycleDialogStyle.control(this, runtime.themes)
    }
    private fun column() = LinearLayout(settingsContext).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding))
    }
    private fun button(text: String, icon: BicycleIcon? = null, action: () -> Unit) = BicycleComponents.action(settingsContext, runtime.themes, text, action).apply {
        BicycleDialogStyle.control(this, runtime.themes)
        icon?.let { BicycleDialogStyle.actionIcon(this, runtime.themes, it) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = RideRuntime.get(this)
        pendingReport = savedInstanceState?.getString("pendingReport")
        pendingBackupExport = savedInstanceState?.getByteArray("settingsBackupPendingExport")
        dashboardPage = preferences.getInt("dashboardPage", 0).coerceIn(0, 3)
        dashboardLayout = DashboardLayout.decode(preferences.getString(dashboardKey, null))
        dashboardGraphs=GraphOverlayConfig.read(preferences,dashboardPage); dashboardZoneGlow=ZoneGlowConfig.read(preferences,dashboardPage); dashboardDataVisibility=DashboardDataVisibility.read(preferences,dashboardPage)
        mapFullscreen = savedInstanceState?.getBoolean("mapFullscreen", false) ?: false
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        setContentView(root)
        dashboard = DashboardView(this) { chooseField(it) }
        dashboard.bindEditorTheme(runtime.themes)
        dashboard.activateEditor = { setDashboardEditing(true) }
        dashboard.graphWidthChanged={ runtime.liveGraphs.width=it }
        updateMapBack()
        dashboard.changePage = ::changeDashboard
        dashboard.setTemplate(currentTemplate())
        mapPanel = MapPanel(this, runtime, { setMapFullscreen(!mapFullscreen) }, ::enableGps, ::openMapLink)
        mapPanel.cameraVisible = { dialogs.empty }
        mapPanel.finishEditor = ::finishDashboardEditor
        mapPanel.selectMapField = { chooseField(it, mapField = true) }
        dashboardArea = object : LinearLayout(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val available = View.MeasureSpec.getSize(heightMeasureSpec) - if (controlsVisible) 0 else dp(58)
                (dashboard.layoutParams as? LayoutParams)?.let {
                    it.height = dashboard.heightForViewport(View.MeasureSpec.getSize(widthMeasureSpec), available.coerceAtLeast(0)); it.weight = 0f
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }.apply { orientation = LinearLayout.VERTICAL }
        dashboardArea.addView(dashboard, LinearLayout.LayoutParams(-1, 0, currentTemplate().dataFraction))
        dashboardArea.addView(mapPanel, LinearLayout.LayoutParams(-1, 0, 1f - currentTemplate().dataFraction))
        dashboard.visibility = if (mapFullscreen) View.GONE else View.VISIBLE
        mapPanel.showFullscreen(mapFullscreen)
        root.addView(dashboardArea, LinearLayout.LayoutParams(-1, 0, 1f))
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; tag = "ride_toolbar" }
        fun toolbarButton(title: String, role: ThemeRole = ThemeRole.PRIMARY, action: () -> Unit) = Button(this).apply {
            text = title; isAllCaps = false; setOnClickListener { action() }
            textSize = 20f; minWidth = 0; minHeight = 0
            setPadding(dp(4), 0, dp(4), 0)
            BicycleComponents.bindTheme(this, runtime.themes) { tokens ->
                setTextColor(tokens.foreground)
                backgroundTintList = null
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(tokens.color(role)); setStroke(1, tokens.tertiary)
                }
            }
        }
        startButton = toolbarButton(getString(R.string.ui_record_start)) {
            if (runtime.recording) startService(Intent(this, RecordingService::class.java).setAction(RecordingService.STOP)) else startRide()
        }
        toolbar.addView(startButton, LinearLayout.LayoutParams(0, dp(58), 3f))
        toolbar.addView(android.widget.ImageButton(this).apply {
            contentDescription = getString(R.string.ui_routes_open); tag = "dashboard_routes"
            setImageResource(BicycleIcon.Route.resource)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setPadding(dp(17), dp(17), dp(17), dp(17))
            setOnClickListener { routeSettings() }
            BicycleComponents.bindTheme(this, runtime.themes) { tokens ->
                imageTintList = android.content.res.ColorStateList.valueOf(tokens.foreground)
                backgroundTintList = null
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(tokens.secondary); setStroke(1, tokens.tertiary)
                }
            }
        }, LinearLayout.LayoutParams(0, dp(58), 1f))
        toolbar.addView(toolbarButton("☰", ThemeRole.SECONDARY) { showMenu() }.apply { contentDescription = getString(R.string.ui_menu_open) }, LinearLayout.LayoutParams(0, dp(58), 1f))
        root.addView(toolbar)
        // Keep the data area height stable; all reclaimed toolbar space goes to the map.
        mapPanel.controlsVisibilityChanged = { visible ->
            controlsVisible = visible
            toolbar.visibility = if (visible) View.VISIBLE else View.GONE
            dashboardArea.requestLayout()
        }
        for (i in 0 until toolbar.childCount) toolbar.getChildAt(i).setOnTouchListener { _, event ->
            if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) mapPanel.holdControls()
            if (event.actionMasked == android.view.MotionEvent.ACTION_UP || event.actionMasked == android.view.MotionEvent.ACTION_CANCEL) mapPanel.revealControls()
            false
        }
        render()
    }
    private fun render() {
        if (!::dashboard.isInitialized) return
        val gps = runtime.gpsValues
        startButton.text = if (runtime.recording) getString(R.string.ui_record_stop) else if (runtime.saving) getString(R.string.ui_record_saving) else getString(R.string.ui_record_start)
        startButton.isEnabled = !runtime.saving || runtime.recording
        val message = runtime.recordingStatus
        if (message != displayedRecordingStatus) {
            displayedRecordingStatus = message
            if (message.startsWith("Gespeichert:") || message.startsWith("FIT nicht gespeichert") || message.startsWith("FIT lokal gespeichert") ||
                message.contains("Fehler") || message.contains("überlastet") ||
                message.startsWith("Aufzeichnung konnte") || message.contains("Fahrten gespeichert"))
                BicycleFeedback.show(this, message, long = true)
        }
        val trainingNow=android.os.SystemClock.elapsedRealtime()
        trainingSnapshot=trainingPresentation.update(runtime.trainingInputs(trainingNow),runtime.riderProfiles.read(),trainingNow)
        dashboard.showTraining(dashboardZoneGlow,trainingSnapshot)
        dashboard.showDataVisibility(dashboardDataVisibility)
        dashboard.showGraphs(dashboardGraphs,runtime.liveGraphs.frame)
        dashboard.show(dashboardLayout, runtime.dashboardBikeData, gps, runtime.ride.bike.distanceM, runtime.pulseValues)
        dashboard.showModeGlow(ModeGlowSettings.enabled(preferences), runtime.currentAssistMode, runtime.bikeData.assistNames, runtime.currentBikeBattery)
        if (::mapPanel.isInitialized) mapPanel.refresh()
        val mapMessage = runtime.mapState.message
        if (mapMessage != displayedMapMessage) {
            displayedMapMessage = mapMessage
            if (mapMessage.isNotEmpty()) BicycleFeedback.show(this, mapMessage, long = true)
        }
        if (runtime.visible && (runtime.connection.isActive || runtime.gpsSensor.active || runtime.heart.active)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    /** Source selector stays outside the scrollable list for dashboard and map fields. */
    private fun chooseField(index: Int, mapField: Boolean = false) {
        fun permitted(): Boolean = dashboardEditing
        if (!permitted()) return
        picker?.dismiss()
        val selected = if (mapField) MapFieldSelection.read(preferences, index) else dashboardLayout.fields[index]
        val graphs=if(!mapField && dashboard.template.supportsGraph(index))
            GraphOverlayPicker(settingsContext,runtime.themes,dashboardGraphs.slots[index]) else null
        val visualization=if(!mapField) ZoneGlowPicker(settingsContext,runtime.themes,dashboardZoneGlow.sources[index]) else null
        visualization?.changed = {
            if(permitted()) {
                dashboardZoneGlow=dashboardZoneGlow.replace(index,visualization.selected)
                preferences.edit().putString(ZoneGlowConfig.key(dashboardPage),dashboardZoneGlow.encode()).apply();render()
            }
        }
        fun saveGraphs() {
            if(graphs!=null) {
                dashboardGraphs=dashboardGraphs.replace(index,graphs.selected)
                preferences.edit().putString(GraphOverlayConfig.key(dashboardPage),dashboardGraphs.encode())
                    .putString(DashboardDataVisibility.key(dashboardPage),dashboardDataVisibility.encode()).apply()
            }
        }
        graphs?.changed = { saveGraphs(); render() }
        fun select(field: DashboardField?) {
            // Ignore stale selections after the dashboard editor has been exited.
            if (!permitted()) { picker?.dismiss(); return }
            if (mapField) {
                preferences.edit().apply {
                    if (field == null) remove(MapFieldSelection.key(index)) else putString(MapFieldSelection.key(index), field.id)
                }.apply()
                mapPanel.refreshSettings()
            } else {
                if(field!=null) dashboardLayout = dashboardLayout.replace(index, field)
                dashboardDataVisibility=dashboardDataVisibility.replace(index,field!=null)
                // Write the old display-mode key as compatibility metadata; visibility is authoritative.
                dashboardGraphs=dashboardGraphs.withDisplay(index,if(field==null) GraphDisplayMode.GRAPH_ONLY else GraphDisplayMode.WITH_DATA)
                preferences.edit().putString(dashboardKey,dashboardLayout.encode())
                    .putString(DashboardDataVisibility.key(dashboardPage),dashboardDataVisibility.encode())
                    .putString(GraphDisplaySettings.key(dashboardPage),dashboardGraphs.encodeModes()).apply()
            }
            saveGraphs(); render(); picker?.dismiss()
            if (field?.requiresGps == true) enableGps()
        }
        val content = FieldPickerView(settingsContext, runtime.dashboardBikeData, runtime.gpsValues,
            runtime.ride.bike.distanceM, runtime.pulseValues, selected, mapField, runtime.themes, graphs, visualization, trainingSnapshot, if(mapField) selected!=null else dashboardDataVisibility.visible[index], ::select)
        val builder = dialog().setTitle(if (mapField) getString(R.string.ui_map_field_title, MapFieldSelection.positionLabel(this, index)) else getString(R.string.ui_dashboard_field_title, index + 1))
            .setView(content)
        picker=builder.show()
    }
    private fun showMenu(advanced: Boolean = false) {
        setDashboardEditing(false)
        val content = SettingsMenuView(settingsContext, runtime.themes, advanced) { action ->
            when (action) {
                SettingsAction.SENSORS -> showSensorManager()
                SettingsAction.DATA -> dataSettings()
                SettingsAction.PROFILE -> { menu = showRiderProfile(settingsContext, runtime.themes, dialogs, runtime.riderProfiles) }
                SettingsAction.MAP -> mapSettings()
                SettingsAction.DASHBOARDS -> dashboardSettings()
                SettingsAction.ROUTES -> routeSettings()
                SettingsAction.ADVANCED -> showMenu(advanced = true)
                SettingsAction.RECOVERY -> recoverySettings()
                SettingsAction.GPS_HELP -> menu = dialog().setTitle(getString(R.string.ui_gps_help_title)).setMessage(R.string.ui_gps_help_body).show()
                SettingsAction.DIAGNOSTIC -> diagnosticSettings()
                SettingsAction.RESET -> resetDashboardSettings()
                SettingsAction.SOURCES -> sourcesSettings()
                SettingsAction.FONT_LICENSE -> menu = dialog().setTitle(getString(R.string.ui_font_license_title)).setMessage(assets.open("Bebas-Neue-OFL.txt").bufferedReader().use { it.readText() }).show()
                SettingsAction.MAP_LICENSE -> menu = dialog().setTitle(getString(R.string.ui_library_license_title)).setMessage(
                    "Mapsforge · LGPL-3.0 mit Ausnahme\n\n" + assets.open("Mapsforge-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nGarmin FIT SDK 21.218.0\n\n" + assets.open("Garmin-FIT-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nOkHttp 4.12.0 · Apache-2.0\n\n" + assets.open("OkHttp-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nOkio 3.6.0 · Apache-2.0\n\n" + assets.open("Okio-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nLucide 0.468.0 · ISC / Feather MIT\n\n" + assets.open("Lucide-LICENSE.txt").bufferedReader().use { it.readText() })
                    .show()
            }
        }
        menu = dialog().setTitle(if(advanced) R.string.ui_advanced else R.string.ui_settings).create().apply {
            setView(ScrollView(settingsContext).apply { addView(content) }, 0, 0, 0, 0)
            show()
        }
    }
    private fun recoverySettings() {
        val root = column()
        root.addView(label(runtime.recordingStatus, BicycleDesign.bodySp).apply { setPadding(0, dp(12), 0, dp(12)) })
        root.addView(button(getString(R.string.ui_recovery_retry), BicycleIcon.ExportTrack) { menu?.dismiss(); runtime.retryPending() }.apply {
            tag = "recovery_execute"; isEnabled = !runtime.recording && !runtime.saving
        })
        menu = dialog().setTitle(getString(R.string.ui_recovery_title)).setView(ScrollView(settingsContext).apply { addView(root) }).show()
    }
    private fun diagnosticSettings() {
        val root = column()
        root.addView(button(getString(R.string.ui_diagnostic_export), BicycleIcon.ExportTrack) { menu?.dismiss(); exportDiagnostic() }.apply { tag = "diagnostic_execute" })
        menu = dialog().setTitle(getString(R.string.ui_diagnostic_title)).setView(ScrollView(settingsContext).apply { addView(root) }).show()
    }
    private fun resetDashboardSettings() {
        val root = column()
        root.addView(button(getString(R.string.ui_reset_occupancy), BicycleIcon.Reset) {
            dialog(fullscreen=false).setTitle(R.string.ui_reset_title).setMessage(R.string.ui_reset_confirm)
                .setPositiveButton(R.string.ui_reset_action) { _, _ ->
                    dashboardLayout = DashboardLayout.defaults
                    preferences.edit().putString(dashboardKey, dashboardLayout.encode()).apply()
                    render()
                    BicycleFeedback.show(this, R.string.ui_reset_done, long = false)
                }.show()
        }.apply { tag = "reset_execute" })
        menu = dialog().setTitle(getString(R.string.ui_reset_defaults_title)).setView(ScrollView(settingsContext).apply { addView(root) }).show()
    }
    private fun showSensorManager() {
        val content = BikesView(settingsContext, runtime, ::searchSensors, ::stopScan, ::enableGps, ::recordingControls)
        bikesManager = content
        sensorManager = dialog().setTitle(R.string.bikes_title).setView(BicycleStickyScrollView(settingsContext).apply { addView(content) }).create().apply {
            dialogs.onDismiss(this) { stopScan();if(bikesManager===content) bikesManager=null };show()
        }
    }
    private fun refreshSensorRows() { bikesManager?.refresh() }
    private fun searchSensors() {
        if(runtime.recording || runtime.saving) return
        if(!runtime.hasBluetooth()) { requestSensorPermission("all");return }
        stopScan();runtime.connectAssignedSensors()
        val manager=bikesManager ?: return
        val slot=runtime.bikes.activeSlot
        val profile=runtime.bikes.current()
        manager.beginSearch()
        scan=BikeSensorScanner(this,setOfNotNull(profile.bikeAddress,profile.heartAddress),{ device,name,category ->
            if(runtime.bikes.activeSlot!=slot || bikesManager!==manager || category !in profile.type.sensors || !category.supported) return@BikeSensorScanner
            manager.addCandidate(name,device.address,category) {
                if(runtime.bikes.activeSlot==slot && !runtime.recording && !runtime.saving) {
                    stopScan()
                    when(category) {
                        BikeSensor.EBIKE->runtime.selectBike(device,name)
                        BikeSensor.HEART->runtime.selectHeart(device,name)
                        else->Unit
                    }
                    manager.clearSearch();manager.refresh()
                }
            }
        },{ status -> if(runtime.bikes.activeSlot==slot && bikesManager===manager) manager.scanStatus(status) }).also { it.start() }
    }
    private fun stopScan() { scan?.stop();scan=null }
    private fun requestSensorPermission(kind: String) {
        pendingSensor = kind
        requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 1)
    }
    private fun enableGps() {
        if (!runtime.hasGps()) requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 3)
        else {
            runtime.setGpsEnabled(true)
            if (runtime.recording) startForegroundService(Intent(this, RecordingService::class.java).setAction(RecordingService.START))
        }
    }
    private fun recordingControls(): LinearLayout {
        val slot = runtime.bikes.activeSlot
        val current = runtime.config; val selected = current.fields.toMutableSet(); val root = column().apply { tag="bike_fit_settings";setPadding(0, 0, 0, 0) }
        root.addView(label(getString(R.string.bikes_fit_explanation)))
        fun source(title: String, chosen: String, bikeId: String): Spinner {
            root.addView(label(title, 16f).apply { setPadding(0, dp(12), 0, 0) })
            return Spinner(settingsContext).apply {
                adapter = ArrayAdapter(settingsContext, android.R.layout.simple_spinner_dropdown_item, listOf(getString(R.string.ui_source_bike), getString(R.string.ui_source_gps)))
                setSelection(if (chosen == bikeId) 0 else 1); root.addView(this)
            }
        }
        val speed = source(getString(R.string.bikes_fit_primary_speed), current.speedSource, "SPEED")
        val distance = source(getString(R.string.bikes_fit_primary_distance), current.distanceSource, "BIKE_DISTANCE")
        fun saveFields() { if(runtime.bikes.activeSlot!=slot || root.parent==null) return;runtime.saveConfig(RecordingConfig(selected.toSet(),if(speed.selectedItemPosition==0) "SPEED" else "GPS_SPEED",if(distance.selectedItemPosition==0) "BIKE_DISTANCE" else "GPS_DISTANCE")) }
        observeSettings(speed,::saveFields);observeSettings(distance,::saveFields)
        root.addView(label(getString(R.string.bikes_fit_additional), BicycleDesign.titleSp).apply { isAccessibilityHeading=true })
        for ((source, fields) in RecordField.options.groupBy { it.source }) {
            val content=LinearLayout(settingsContext).apply {
                orientation=LinearLayout.VERTICAL;tag="fit_source_content_$source"
                visibility=if(source in expandedFitSources) View.VISIBLE else View.GONE
            }
            lateinit var toggle: Button
            toggle=BicycleComponents.action(settingsContext,runtime.themes,DashboardTextResources.source(this,source)) {
                val open=content.visibility!=View.VISIBLE
                content.visibility=if(open) View.VISIBLE else View.GONE
                if(open) expandedFitSources.add(source) else expandedFitSources.remove(source)
                toggle.stateDescription=getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
            }.apply {
                tag="fit_source_$source";isAccessibilityHeading=true
                stateDescription=getString(if(source in expandedFitSources) R.string.zone_expanded else R.string.zone_collapsed)
            }
            BicycleDialogStyle.accordion(toggle,runtime.themes)
            BicycleDialogStyle.actionIcon(toggle,runtime.themes,BicycleIcon.Expand)
            root.addView(toggle);root.addView(content)
            for (field in fields) content.addView(CheckBox(settingsContext).apply {
                text = field.titleResource?.let(::getString) ?: DashboardTextResources.recordTitle(this@MainActivity, field.id) ?: DashboardField(field.id).title(this@MainActivity, LiveData()); tag = "record_${field.id}"; isChecked = field.id in selected; minHeight = dp(48)
                setOnCheckedChangeListener { _, checked -> if (checked) selected.add(field.id) else selected.remove(field.id);saveFields() }
            })
        }
        // Style all source accordions and their existing field controls.
        BicycleDialogStyle.content(root, runtime.themes)
        return root
    }

    private fun startRide() {
        if (runtime.recording || runtime.saving) return
        val permissions = mutableListOf<String>()
        if (!runtime.hasBluetooth()) permissions += listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        if (!preferences.getBoolean("recordPermissionsAsked", false)) {
            if (runtime.gpsEnabled && runtime.config.needsGps && !runtime.hasGps()) permissions += listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (permissions.isNotEmpty()) {
            pendingStart = true; preferences.edit().putBoolean("recordPermissionsAsked", true).apply(); requestPermissions(permissions.toTypedArray(), 5); return
        }
        try { startForegroundService(Intent(this, RecordingService::class.java).setAction(RecordingService.START)) }
        catch (_: SecurityException) { BicycleFeedback.show(this, getString(R.string.ui_record_permission_error), long = true) }
        catch (_: IllegalStateException) { BicycleFeedback.show(this, getString(R.string.ui_record_visible_error), long = true) }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                val kind = pendingSensor; pendingSensor = null
                if (runtime.hasBluetooth()) when (kind) { "bikeConnect" -> runtime.setBikeEnabled(true); "heartConnect" -> runtime.setHeartEnabled(true); "bike", "heart", "all" -> searchSensors() }
                else BicycleFeedback.show(this, getString(R.string.ui_bluetooth_missing), long = false)
            }
            3 -> if (runtime.hasGps()) enableGps() else BicycleFeedback.show(this, getString(R.string.ui_gps_permission), long = false)
            5 -> if (pendingStart) {
                pendingStart = false
                if (runtime.hasBluetooth()) {
                    if (runtime.gpsEnabled && runtime.config.needsGps && !runtime.hasGps()) BicycleFeedback.show(this, getString(R.string.ui_record_without_gps), long = true)
                    startRide()
                } else BicycleFeedback.show(this, getString(R.string.ui_record_bluetooth_required), long = true)
            }
        }
        runtime.reconnectSensors(); render()
    }
    private fun exportDiagnostic() {
        val gps = runtime.gpsValues
        pendingReport = runtime.diagnostics.report() + "\nRecording=${runtime.recording} GPS accepted=${runtime.ride.gps.accepted} rejected=${runtime.ride.gps.rejected}\n" +
            GpsMetric.entries.joinToString("\n") { "${it.name}=${it.value(gps).text} ${it.unit}" }
        try { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "B-Cycle-${AppVersion.name}-diagnose.txt"), 2) }
        catch (_: android.content.ActivityNotFoundException) { pendingReport = null; BicycleFeedback.show(this, R.string.export_unavailable, long = true) }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putByteArray("settingsBackupPendingExport", pendingBackupExport); outState.putString("pendingReport", pendingReport); outState.putBoolean("mapFullscreen", mapFullscreen); super.onSaveInstanceState(outState) }
    @Deprecated("Native document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 12 || requestCode == 13) { backupResult(requestCode,resultCode,data);return }
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) data?.data?.let { uri ->
                try {
                    val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    if (flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION == 0) error("Keine Schreibberechtigung")
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    val settings=RouteExportSettings(this)
                    settings.write(settings.read().copy(tree=uri.toString()),null)
                    exportDraft = settings.read()
                } catch (_: Exception) { BicycleFeedback.show(this, getString(R.string.ui_folder_permission_error), long = true) }
            }
            if (dialogs.empty) { showMenu(); dataSettings(); routeExportSettings(RouteExportPart.LOCAL) }
            return
        }
        if (requestCode == 8 || requestCode == 9) {
            if (resultCode == RESULT_OK) data?.data?.let { runtime.mapState.import(it, recorded = requestCode == 9) }
            if (dialogs.empty) { showMenu(); routeSettings() }
            return
        }
        if (requestCode != 2) return
        if (dialogs.empty) { showMenu(); diagnosticSettings() }
        val report = pendingReport; pendingReport = null
        val uri = data?.data ?: return
        if (resultCode != RESULT_OK || report == null) return
        Thread({
            val ok = try { contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(report) } ?: throw IOException(); true }
            catch (_: IOException) { false } catch (_: SecurityException) { false }
            runOnUiThread { BicycleFeedback.show(applicationContext, if (ok) R.string.export_saved else R.string.export_failed, long = true) }
        }, "B-Cycle-diagnostic-export").start()
    }
    override fun onStart() { super.onStart(); mapPanel.resume(); runtime.observers.add(observer); runtime.setVisible(true); render(); refreshRouteRows() }
    override fun onStop() {
        dialogs.closeAll(); stopScan()
        mapPanel.pause(); runtime.observers.remove(observer); runtime.setVisible(false); window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); super.onStop()
    }
    override fun onDestroy() {
        if(mapBackRegistered) onBackInvokedDispatcher.unregisterOnBackInvokedCallback(mapBack)
        dialogs.closeAll(); settingsWriter.shutdown(); mapPanel.destroy(); super.onDestroy()
    }

    private fun setMapFullscreen(full: Boolean) {
        mapFullscreen = full; updateMapBack(); dashboard.visibility = if (full) View.GONE else View.VISIBLE
        mapPanel.showFullscreen(full); dashboardArea.requestLayout()
    }
    private fun changeDashboard(delta: Int) {
        dashboardPage = (dashboardPage + delta + 4) % 4
        preferences.edit().putInt("dashboardPage", dashboardPage).apply()
        dashboardLayout = DashboardLayout.decode(preferences.getString(dashboardKey, null))
        dashboardGraphs=GraphOverlayConfig.read(preferences,dashboardPage); dashboardZoneGlow=ZoneGlowConfig.read(preferences,dashboardPage); dashboardDataVisibility=DashboardDataVisibility.read(preferences,dashboardPage)
        val template = currentTemplate(); dashboard.setTemplate(template)
        dashboardArea.requestLayout()
        (mapPanel.layoutParams as LinearLayout.LayoutParams).weight = 1f - template.dataFraction
        setMapFullscreen(false); render()
    }
    private fun dashboardSettings() {
        val root = column()
        BicycleForm.section(root,runtime.themes,R.string.ui_theme)
        root.addView(ThemePickerView(settingsContext, runtime.themes))
        BicycleForm.section(root,runtime.themes,R.string.ui_dashboard_page_choose)
        val presets = DashboardPresetPicker(settingsContext, (0 until 4).map { page ->
            DashboardTemplate.decode(preferences.getString("dashboardTemplate_$page", DashboardTemplate.entries[page].name))
        }, dashboardPage, runtime.themes)
        presets.changed = {
            val edit=preferences.edit(); presets.selection.forEachIndexed { page,template -> edit.putString("dashboardTemplate_$page",template.name) };edit.apply();changeDashboard(0)
        }
        root.addView(presets)
        BicycleForm.section(root,runtime.themes,R.string.ui_mode_glow)
        root.addView(label(getString(R.string.ui_mode_glow_description)))
        val glow = Switch(settingsContext).apply {
            text = getString(R.string.ui_mode_glow); tag = "mode_glow_enabled"
            isChecked = ModeGlowSettings.enabled(preferences)
        }
        root.addView(glow)
        root.addView(BicycleComponents.sectionDivider(settingsContext, runtime.themes))
        root.addView(button(getString(R.string.dashboard_editor_open), BicycleIcon.EditDashboard) { enterDashboardEditor() }.apply { tag = "dashboard_editor_open" })
        glow.setOnCheckedChangeListener { _,checked -> ModeGlowSettings.save(preferences,checked); render() }
        menu = dialog().setTitle(getString(R.string.ui_design_title)).setView(BicycleStickyScrollView(settingsContext).apply { tag="design_ui_scroll";addView(root) }).show()
    }
    private fun mapSettings() {
        val root = column()
        root.addView(label(getString(R.string.ui_map_source), BicycleDesign.inputSp))
        val source = Spinner(settingsContext).apply {
            adapter = ArrayAdapter(settingsContext, android.R.layout.simple_spinner_dropdown_item, MapSource.entries.map { it.label(this@MainActivity) })
            setSelection(MapSource.decode(preferences.getString("mapSource", null)).ordinal); root.addView(this)
        }
        val keyRow=LinearLayout(settingsContext).apply { orientation=LinearLayout.VERTICAL;tag="map_provider_key_row" }
        keyRow.addView(label(getString(R.string.map_thunderforest_key)))
        val providerKey=BicycleComponents.input(settingsContext,runtime.themes).apply {
            tag="map_thunderforest_key";setSingleLine();contentDescription=getString(R.string.map_thunderforest_key)
            inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS;isSaveEnabled=false
            setText(MapProviderSettings.read(this@MainActivity))
        }
        BicycleForm.outlinedInput(providerKey,runtime.themes)
        keyRow.addView(providerKey);root.addView(keyRow)
        val providerStatus=BicycleFeedback.status(settingsContext,runtime.themes).apply { tag="map_provider_status" }
        root.addView(providerStatus)
        keyRow.visibility=if(MapSource.entries[source.selectedItemPosition]==MapSource.OPENCYCLEMAP) View.VISIBLE else View.GONE
        fun toggle(title: String, key: String, fallback: Boolean) = Switch(settingsContext).apply { text = title; tag = key; isChecked = preferences.getBoolean(key, fallback); root.addView(this) }
        root.addView(BicycleComponents.sectionDivider(settingsContext, runtime.themes))
        val heading = toggle(getString(R.string.ui_map_heading), "mapHeading", true)
        val follow = toggle(getString(R.string.ui_map_follow), "mapFollow", true)
        val track = toggle(getString(R.string.ui_map_track), "mapTrackVisible", true)
        val gpx = toggle(getString(R.string.ui_map_gpx), "mapGpxVisible", true)
        val elevation = toggle(getString(R.string.elevation_enable), FitElevationCorrection.preference, false).apply { tag="map_elevation_correction" }
        root.addView(BicycleComponents.sectionDivider(settingsContext, runtime.themes))
        root.addView(button(getString(R.string.ui_map_legend), BicycleIcon.Info) { openMapLink(MapProviderSettings.source(this,preferences.getString("mapSource",null)).creditUrl) })
        observeSettings(root) {
            val chosen=MapSource.entries[source.selectedItemPosition]
            keyRow.visibility=if(chosen==MapSource.OPENCYCLEMAP) View.VISIBLE else View.GONE
            val entered=providerKey.text.toString().trim()
            val validKey=entered.isEmpty() || MapProviderSettings.valid(entered)
            providerKey.error=if(!validKey && chosen==MapSource.OPENCYCLEMAP) getString(R.string.map_thunderforest_invalid) else null
            if(validKey) MapProviderSettings.write(this,entered)
            val available=chosen!=MapSource.OPENCYCLEMAP || MapProviderSettings.read(this).isNotEmpty()
            BicycleFeedback.update(providerStatus,if(!available) getString(R.string.map_thunderforest_required) else "",hideWhenEmpty=true)
            val edit=preferences.edit()
            if(available) edit.putString("mapSource",chosen.name)
            edit.putBoolean("mapHeading",heading.isChecked).putBoolean("mapFollow",follow.isChecked)
                .putBoolean("mapTrackVisible",track.isChecked).putBoolean("mapGpxVisible",gpx.isChecked)
                .putBoolean(FitElevationCorrection.preference,elevation.isChecked).apply()
            mapPanel.refreshSettings()
        }
        menu=dialog().setTitle(getString(R.string.ui_map_manager_title)).setView(ScrollView(this).apply { addView(root) }).show()
    }
    private fun importRoute(recorded: Boolean = false) {
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, if (recorded)
                    arrayOf("application/vnd.ant.fit", "application/octet-stream") else
                    arrayOf("application/gpx+xml", "application/xml", "text/xml", "application/vnd.ant.fit", "application/octet-stream")),
                if (recorded) 9 else 8)
        } catch (_: android.content.ActivityNotFoundException) {
            BicycleFeedback.show(this, getString(R.string.ui_file_picker_missing), long = true)
        }
    }
    private fun routeAnalysis(entry: ManagedRoute) {
        val content = RouteAnalysisView(settingsContext, runtime.themes, entry.id, ::routeAnalysisMap)
        val footer = bicycleFooter(settingsContext, runtime.themes)
        val root = LinearLayout(settingsContext).apply {
            orientation = LinearLayout.VERTICAL
            addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(footer, LinearLayout.LayoutParams(-1, dp(BicycleDesign.footerHeight)))
        }
        val screen = dialog().setView(root).create()
        val detail = DetailSession(entry.id, screen, content, footer)
        val labels = listOf(R.string.route_action_map, R.string.elevation_correct_action, R.string.route_action_cloud, R.string.route_action_save, R.string.route_action_delete)
        val captions = listOf(R.string.footer_show, R.string.elevation_correct_action, R.string.footer_cloud_upload, R.string.route_action_save, R.string.route_action_delete)
        val icons = listOf(BicycleIcon.ShowOnMap, BicycleIcon.CorrectElevation, BicycleIcon.CloudNotUploaded, BicycleIcon.Save, BicycleIcon.DeleteTrack)
        val tags = listOf("detail_map", "detail_elevation", "detail_cloud", "detail_save", "detail_delete")
        labels.indices.forEach { index ->
            footer.addView(BicycleFooterAction(settingsContext, runtime.themes, icons[index], getString(captions[index]), getString(labels[index])) {
                executeDetailAction(detail, index)
            }.apply { tag = tags[index] }, LinearLayout.LayoutParams(0, dp(BicycleDesign.footerHeight), 1f))
        }
        routeDetail = detail
        screen.setCustomTitle(analysisHeader(settingsContext, runtime.themes, entry, rename = { if(!runtime.mapState.busy) detailEntry(detail)?.let(::renameRoute) }, back = { screen.dismiss() }))
        dialogs.onDismiss(screen) {
            content.dispose()
            if (routeDetail === detail) { routePopup?.dismiss(); routePopup = null; routeDetail = null }
        }
        screen.show()
        refreshDetails()
        fun load() {
            content.loading()
            content.cancellation(runtime.mapState.loadAnalysis(entry.id) { result ->
                result.fold({ analysis ->
                    detail.analysis = analysis
                    content.show(analysis)
                    refreshDetails()
                    screen.window?.decorView?.findViewWithTag<TextView>("analysis_date")?.let { date ->
                        date.text = java.time.format.DateTimeFormatter.ofPattern(getString(R.string.analysis_date_format))
                            .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(analysis.entry.details.dateMs))
                    }
                }) { content.failed { load() } }
            })
        }
        content.correctElevation = {
            if(!runtime.recording && !runtime.saving && !runtime.mapState.busy && entry.format=="FIT" && entry.origin==TrackOrigin.RECORDED && entry.hasRoute) {
                content.correctionStatus(true)
                content.cancellation(runtime.mapState.correctElevation(entry.id) { result ->
                    if(routeDetail===detail && screen.isShowing) result.fold({ load() },{ content.correctionStatus(false,getString(R.string.elevation_failed)) })
                })
            }
        }
        load()
    }
    private fun routeAnalysisMap(analysis: RouteAnalysis) {
        val route = analysis.route ?: return
        val map = RouteAnalysisMap(settingsContext, route, true)
        val screen = dialog().setTitle(analysis.entry.name).setView(map)
            .create()
        dialogs.onDismiss(screen, map::dispose)
        screen.show()
    }
    private fun routeSettings() {
        setDashboardEditing(false)
        val state = runtime.mapState
        routeSignature = ""
        val content = RouteManagerView(settingsContext, { entry ->
            val displayed = state.selectedRoute == entry.id && state.route != null && preferences.getBoolean("mapGpxVisible", true)
            state.setRouteVisible(!displayed)
            if (!displayed && state.selectedRoute != entry.id) state.selectRoute(entry.id)
        }, themes = runtime.themes, details = ::routeAnalysis)
        routeManager = content
        refreshRouteRows()
        val footer = bicycleFooter(settingsContext, runtime.themes)
        val import = BicycleFooterAction(settingsContext, runtime.themes, BicycleIcon.Import, getString(R.string.footer_import_route)) {
            if (!state.busy) routeImportChoice()
        }.apply { tag = "route_import"; isEnabled = !state.busy }
        footer.addView(import, LinearLayout.LayoutParams(dp(BicycleDesign.footerHeight * 2), dp(BicycleDesign.footerHeight)))
        val root = LinearLayout(settingsContext).apply {
            orientation = LinearLayout.VERTICAL
            addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(footer, LinearLayout.LayoutParams(-1, dp(BicycleDesign.footerHeight)))
        }
        routeImportButton = import
        menu = dialog().setView(root).create().apply {
            setView(root, 0, 0, 0, 0)
            dialogs.onDismiss(this) { if (routeManager === content) { routePopup?.dismiss(); routePopup = null; routeManager = null; routeImportButton = null } }; show()
        }
        state.refreshLibrary()
    }
    private fun routeImportChoice() {
        val anchor = routeImportButton ?: return
        routeDropdown(anchor, listOf(getString(R.string.ui_import_planned), getString(R.string.ui_import_recorded)),
            listOf(BicycleIcon.TrackImported, BicycleIcon.TrackRecorded), listOf("route_import_planned", "route_import_recorded")) {
            importRoute(it == 1)
        }
    }
    /** The same anchored native dropdown serves imports and each route's actions. */
    private fun routeDropdown(anchor: View, choices: List<String>, icons: List<BicycleIcon> = emptyList(),
                              tags: List<String> = emptyList(), selected: (Int) -> Unit) {
        routePopup?.dismiss()
        val popup = ListPopupWindow(settingsContext).apply {
            anchorView = anchor; isModal = true
            width = minOf(dp(BicycleDesign.popupWidth), resources.displayMetrics.widthPixels - dp(BicycleDesign.horizontalPadding * 2))
            setBackgroundDrawable(android.graphics.drawable.GradientDrawable().apply {
                setColor(runtime.themes.active.tokens.tertiary); cornerRadius = dp(BicycleDesign.inputRadius).toFloat()
            })
            setAdapter(object : ArrayAdapter<String>(settingsContext, android.R.layout.simple_list_item_1, choices) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                    ((convertView as? TextView) ?: TextView(settingsContext).apply {
                        BicycleComponents.bindTheme(this, runtime.themes) { tokens ->
                            setTextColor(tokens.foreground)
                            compoundDrawablesRelative.filterNotNull().forEach { it.setTint(tokens.foreground) }
                        }
                    }).apply {
                        text = getItem(position); textSize = BicycleDesign.bodySp
                        gravity = android.view.Gravity.CENTER_VERTICAL; minHeight = dp(BicycleDesign.popupRowHeight)
                        setPadding(dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding))
                        tag = tags.getOrNull(position)
                        setCompoundDrawablesRelative(icons.getOrNull(position)?.let { kind -> BicycleIcons.drawable(settingsContext, kind, runtime.themes.active.tokens.foreground)
                            .apply { setBounds(0, 0, dp(BicycleDesign.iconSize), dp(BicycleDesign.iconSize)) } }, null, null, null)
                        compoundDrawablePadding = dp(BicycleDesign.verticalPadding)
                    }
            })
            setOnItemClickListener { _, _, position, _ ->
                dismiss()
                if (!runtime.mapState.busy) selected(position)
            }
            setOnDismissListener { if (routePopup === this) routePopup = null }
        }
        routePopup = popup
        popup.show()
        popup.listView?.let { list -> BicycleComponents.bindTheme(list, runtime.themes) { tokens ->
            popup.setBackgroundDrawable(android.graphics.drawable.GradientDrawable().apply {
                setColor(tokens.tertiary); cornerRadius = dp(BicycleDesign.inputRadius).toFloat()
            })
            list.selector = android.graphics.drawable.GradientDrawable().apply {
                setColor(tokens.tertiary); setStroke(BicycleDesign.dividerPixels, tokens.foreground)
                cornerRadius = dp(BicycleDesign.inputRadius).toFloat()
            }
        } }
    }
    private fun dataDialog(title: Int, body: View, action: Int? = null): AlertDialog {
        val builder = dialog().setTitle(title).setView(body)
        action?.takeIf { it!=R.string.route_action_save }?.let { builder.setPositiveButton(it, null) }
        val screen = builder.create()
        screen.setCustomTitle(BicycleComponents.header(settingsContext, runtime.themes).apply {
            tag = "bicycle_dialog_header"; setPadding(dp(BicycleDesign.smallGap),0,dp(BicycleDesign.verticalPadding),0)
            addView(BicycleComponents.icon(settingsContext,runtime.themes,BicycleIcon.Back,getString(R.string.data_back)) { screen.dismiss() })
            addView(BicycleComponents.heading(settingsContext,runtime.themes,getString(title)).apply {
                tag="bicycle_dialog_title";setAutoSizeTextTypeUniformWithConfiguration(18,BicycleDesign.headingSp.toInt(),1,android.util.TypedValue.COMPLEX_UNIT_SP)
            },LinearLayout.LayoutParams(0,dp(BicycleDesign.headerHeight),1f))
        })
        screen.show(); return screen
    }
    private fun dataSettings() {
        val content = DataSettingsView(settingsContext,runtime.themes) { action -> when(action) {
            DataAction.BACKUP -> backupSettings()
            DataAction.RESTORE -> try {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
                    .putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("application/json","text/json","text/plain")),13)
            } catch(_:android.content.ActivityNotFoundException) { BicycleFeedback.show(this, R.string.backup_picker_error, long = true) }
            DataAction.LOCAL -> routeExportSettings(RouteExportPart.LOCAL)
            DataAction.CLOUD -> routeExportSettings(RouteExportPart.CLOUD)
            DataAction.INTERVALS -> intervalsSettings()
        } }
        val screen=dataDialog(R.string.data_title,BicycleStickyScrollView(settingsContext).apply { addView(content) })
        dataSettingsScreen=screen;menu=screen
        dialogs.onDismiss(screen) { if(dataSettingsScreen===screen) dataSettingsScreen=null }
    }
    private fun backupError(error: Throwable): Int = when((error as? SettingsBackupFailure)?.problem) {
        BackupProblem.FORMAT->R.string.backup_format_error;BackupProblem.VERSION->R.string.backup_version_error
        BackupProblem.TOO_LARGE->R.string.backup_size_error;BackupProblem.SELECTION->R.string.backup_selection_error
        BackupProblem.INVALID->R.string.backup_invalid_error;BackupProblem.ROLLBACK->R.string.backup_rollback_error
        else->R.string.backup_io_error
    }
    private fun backupSettings(document: SettingsBackupDocument? = null) {
        val backup=SettingsBackup(this,runtime.themes)
        val choices=BackupSelectionView(settingsContext,runtime.themes,document?.sections?.keys ?: BackupSection.entries.toSet())
        val screen=dataDialog(if(document==null) R.string.data_backup else R.string.data_restore,
            ScrollView(settingsContext).apply { addView(choices) },if(document==null) R.string.backup_execute else R.string.backup_restore_execute)
        val button=screen.getButton(AlertDialog.BUTTON_POSITIVE)
        BicycleDialogStyle.actionIcon(button,runtime.themes,if(document==null) BicycleIcon.ExportTrack else BicycleIcon.RestoreSettings)
        choices.changed={ button.isEnabled=choices.selected().isNotEmpty() }
        button.setOnClickListener {
            val selected=choices.selected()
            if(document==null) {
                try {
                    pendingBackupExport=backup.export(selected)
                    startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
                        .putExtra(Intent.EXTRA_TITLE,getString(R.string.backup_filename,java.time.LocalDate.now().toString())),12)
                } catch(e:Exception) { pendingBackupExport=null;BicycleFeedback.show(this, if(e is android.content.ActivityNotFoundException) R.string.backup_picker_error else backupError(e), long = true) }
            } else {
                dialog(fullscreen=false).setTitle(R.string.backup_confirm_title)
                    .setMessage(getString(R.string.backup_confirm_message,selected.joinToString(", ") { getString(it.title) }))
                    .setPositiveButton(R.string.backup_restore_execute) { _,_->
                        button.isEnabled=false
                        Thread({
                            val result=runCatching {
                                val plan=backup.plan(document,selected);backup.apply(plan)
                                if(BackupSection.DESIGN in selected) runtime.themes.reloadFromPreferences()
                                if(BackupSection.CLOUD in selected) RouteUploads.settingsChanged(applicationContext)
                                plan
                            }
                            runOnUiThread {
                                if(!isDestroyed) {
                                    button.isEnabled=choices.selected().isNotEmpty()
                                    result.fold({ plan ->
                                        if(BackupSection.DASHBOARD in selected) {
                                            dashboardLayout=DashboardLayout.decode(preferences.getString(dashboardKey,null))
                                            dashboardGraphs=GraphOverlayConfig.read(preferences,dashboardPage); dashboardZoneGlow=ZoneGlowConfig.read(preferences,dashboardPage); dashboardDataVisibility=DashboardDataVisibility.read(preferences,dashboardPage)
                                            dashboard.setTemplate(currentTemplate());dashboardArea.requestLayout()
                                            (mapPanel.layoutParams as LinearLayout.LayoutParams).weight=1f-currentTemplate().dataFraction
                                        }
                                        if(BackupSection.MAPS in selected || BackupSection.DASHBOARD in selected) mapPanel.refreshSettings()
                                        render();screen.dismiss()
                                        BicycleFeedback.show(this, if(plan.cloudNeedsPassword) R.string.backup_cloud_secret else R.string.backup_restored, long = true)
                                    },{ BicycleFeedback.show(this, backupError(it), long = true) })
                                }
                            }
                        },"B-Cycle-settings-restore").start()
                    }.show()
            }
        }
    }
    private fun backupResult(requestCode:Int,resultCode:Int,data:Intent?) {
        if(dialogs.empty) { showMenu();dataSettings() }
        val bytes=if(requestCode==12) pendingBackupExport.also { pendingBackupExport=null } else null
        val uri=data?.data ?: return
        if(resultCode!=RESULT_OK) return
        val origin=dataSettingsScreen
        Thread({
            val result=runCatching {
                val backup=SettingsBackup(applicationContext,runtime.themes)
                if(requestCode==12) {
                    val payload=bytes ?: throw SettingsBackupFailure(BackupProblem.IO)
                    val output=contentResolver.openOutputStream(uri,"wt") ?: throw IOException()
                    backup.write(output,payload);null
                } else backup.read(contentResolver.openInputStream(uri) ?: throw IOException())
            }
            runOnUiThread {
                if(!isDestroyed && dataSettingsScreen===origin && origin?.isShowing==true) result.fold({ document ->
                    if(document==null) BicycleFeedback.show(this, R.string.backup_written, long = true) else backupSettings(document)
                },{ BicycleFeedback.show(this, backupError(it), long = true) })
            }
        },"B-Cycle-settings-document").start()
    }
    private fun sourcesSettings() {
        val body=BicycleComponents.dialogBody(settingsContext,runtime.themes)
        for((title,description) in listOf(R.string.sources_maps to R.string.sources_maps_text,
            R.string.sources_elevation to R.string.sources_elevation_text,R.string.sources_weather to R.string.sources_weather_text,
            R.string.sources_uploads to R.string.sources_uploads_text)) {
            body.addView(BicycleComponents.text(settingsContext,runtime.themes,getString(title),true))
            body.addView(BicycleComponents.text(settingsContext,runtime.themes,getString(description)).apply {
                android.text.util.Linkify.addLinks(this,android.text.util.Linkify.WEB_URLS)
                movementMethod=android.text.method.LinkMovementMethod.getInstance()
                BicycleComponents.bindTheme(this,runtime.themes) { setLinkTextColor(it.foreground) }
            },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap);bottomMargin=dp(BicycleDesign.controlGap) })
        }
        dataDialog(R.string.sources_title,ScrollView(settingsContext).apply { addView(body) })
    }
    private fun intervalsSettings() {
        val settings=IntervalsSettings(this)
        val worker=java.util.concurrent.Executors.newSingleThreadExecutor()
        lateinit var content:IntervalsView
        content=IntervalsView(settingsContext,runtime.themes,settings.read(),settings.hasKey()) {
            val config=content.config();val typedKey=content.newKey()
            content.testing(true)
            worker.execute {
                val result=runCatching { IntervalsClient(config,typedKey ?: settings.apiKey()).test() }
                runOnUiThread {
                    content.testing(false)
                    BicycleFeedback.update(content.result,getString(if(result.isSuccess) R.string.route_export_test_ok
                        else (result.exceptionOrNull() as? IntervalsFailure)?.problem ?: R.string.intervals_connection_failed))
                }
            }
        }
        val screen=dataDialog(R.string.intervals_title,ScrollView(settingsContext).apply { addView(content) })
        val revision=java.util.concurrent.atomic.AtomicLong()
        observeSettings(content) {
            val config=content.config();val key=content.newKey();val token=revision.incrementAndGet()
            worker.execute {
                if(token==revision.get()) {
                    val result=runCatching { settings.write(config,key);IntervalsUploads.settingsChanged(applicationContext) }
                    runOnUiThread {
                        if(token==revision.get()) {
                            val error=if(result.isFailure) getString(R.string.intervals_check_settings) else null
                            content.athlete.error=error
                        }
                    }
                }
            }
        }
        dialogs.onDismiss(screen) { worker.shutdown() }
    }
    private fun routeExportSettings(part: RouteExportPart) {
        val settings = RouteExportSettings(this)
        lateinit var content: RouteExportView
        content = RouteExportView(settingsContext, exportDraft ?: settings.read(), settings.hasPassword(), {
            exportDraft = content.read(); exportPasswordDraft = content.password.text.toString().takeIf { it.isNotEmpty() }
            try {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), 10)
            } catch (_: android.content.ActivityNotFoundException) {
                BicycleFeedback.show(this, getString(R.string.route_export_no_picker), long = true)
            }
        }, {
            val config = content.read()
            val newPassword = content.password.text.toString().takeIf { it.isNotEmpty() }
            content.testing(true)
            Thread({
                val message = try {
                    WebDavClient(config, newPassword ?: settings.password()).test(); getString(R.string.route_export_test_ok)
                } catch (e: Exception) {
                    when (e) {
                        is DavFailure -> e.message ?: getString(R.string.ui_connection_failed)
                        is IllegalArgumentException -> e.message ?: getString(R.string.ui_check_settings)
                        else -> getString(R.string.route_export_test_failed)
                    }
                }
                runOnUiThread { content.testing(false); BicycleFeedback.update(content.result, message) }
            }, "B-Cycle-WebDAV-Test").start()
        }, runtime.themes, storedPassword = {
            runCatching { settings.password() }.getOrElse {
                BicycleFeedback.update(content.result, getString(R.string.route_export_password_failed))
                ""
            }
        }, part = part)
        BicycleDialogStyle.content(content, runtime.themes)
        exportPasswordDraft?.let { content.password.setText(it) }
        exportDraft = null; exportPasswordDraft = null
        val screen = dataDialog(if(part == RouteExportPart.LOCAL) R.string.data_local else R.string.data_cloud,
            ScrollView(settingsContext).apply { addView(content) }, R.string.route_action_save)
        var saveRevision=0L
        observeSettings(content) {
            if(content.restoringPassword) return@observeSettings
            val revision=++saveRevision
            try {
                val draft = content.read(); val current = settings.read()
                val config = if(part == RouteExportPart.LOCAL) current.copy(save = draft.save, tree = draft.tree)
                    else current.copy(webdav = draft.webdav, server = draft.server, user = draft.user, folder = draft.folder, wifiOnly = draft.wifiOnly)
                val password = if(part == RouteExportPart.LOCAL) null else content.password.text.toString().takeIf { it.isNotEmpty() }
                if (part == RouteExportPart.CLOUD && config.webdav) {
                    WebDavClient.directory(config)
                    require(password != null || settings.hasPassword()) { getString(R.string.route_export_need_password) }
                }
                val appContext = applicationContext
                settingsWriter.execute {
                    val success = runCatching {
                        val latest=settings.read()
                        val merged=if(part==RouteExportPart.LOCAL) latest.copy(save=draft.save,tree=draft.tree)
                            else latest.copy(webdav=draft.webdav,server=draft.server,user=draft.user,folder=draft.folder,wifiOnly=draft.wifiOnly)
                        settings.write(merged,password);RouteUploads.settingsChanged(appContext)
                    }.isSuccess
                    runOnUiThread {
                        if (!isDestroyed && revision==saveRevision) {
                            if (success) { routeSignature = ""; refreshRouteRows(); BicycleFeedback.update(content.result, "") }
                            else BicycleFeedback.update(content.result, getString(R.string.route_export_save_failed))
                        }
                    }
                }
            } catch (e: Exception) {
                BicycleFeedback.update(content.result, if (e is IllegalArgumentException) e.message else getString(R.string.route_export_save_failed))
            }
        }
    }
    private fun refreshRouteRows() {
        refreshDetails()
        val state = runtime.mapState
        if (!state.busy && pendingMapRoute != null) {
            if (pendingMapRoute == state.selectedRoute && state.route != null) {
                dialogs.closeAll(); mapPanel.fitRoute()
            }
            pendingMapRoute = null
        }
        val manager = routeManager ?: return
        routeImportButton?.isEnabled = !state.busy
        val displayed = state.selectedRoute.takeIf { state.route != null && preferences.getBoolean("mapGpxVisible", true) }
        val signature = "${state.routeRevision}:${state.busy}:${RouteCloudUploads.revision(this)}:$displayed"
        if (routeSignature == signature) return
        routeSignature = signature
        manager.submit(state.entries, displayed, state.busy)
    }
    private fun renameRoute(entry: ManagedRoute) {
        val input = EditText(settingsContext).apply { setText(entry.name); setSingleLine(); selectAll()
            filters = arrayOf(android.text.InputFilter.LengthFilter(120)); tag = "route_rename" }
        val rename = dialog().setTitle(getString(R.string.route_rename_title)).setView(input)
            .create()
        rename.show()
        val handler=android.os.Handler(android.os.Looper.getMainLooper())
        var pending: String?=null
        val save=object : Runnable {
            override fun run() {
                val name=pending ?: return
                if(runtime.mapState.busy) { handler.postDelayed(this,50);return }
                pending=null;runtime.mapState.renameRoute(entry.id,name)
            }
        }
        input.onTextChanged {
            val name=input.text.toString().trim()
            if(name.isEmpty()) input.error=getString(R.string.route_name_required)
            else { input.error=null;pending=name;handler.removeCallbacks(save);handler.postDelayed(save,200) }
        }
    }
    private fun deleteRoute(entry: ManagedRoute) {
        dialog(fullscreen = false).setTitle(getString(R.string.route_delete_title))
            .setMessage(getString(R.string.route_delete_message, entry.name))
            .setPositiveButton(getString(R.string.route_action_delete)) { _, _ -> runtime.mapState.deleteRoute(entry.id) }
            .show()
    }
    private fun detailEntry(detail: DetailSession): ManagedRoute? {
        val current = runtime.mapState.entries.firstOrNull { it.id == detail.id } ?: return null
        val verified = detail.analysis?.entry?.fileStamp
        val stamp = if (verified != null && verified.length == current.fileStamp?.length && verified.modified == current.fileStamp.modified) verified else current.fileStamp
        return current.copy(fileStamp = stamp)
    }
    private fun refreshDetails() {
        val detail = routeDetail ?: return
        val entry = detailEntry(detail)
        if (!runtime.mapState.busy) {
            if (entry == null) { detail.screen.dismiss(); return }
            detail.screen.window?.decorView?.findViewWithTag<TextView>("analysis_title")?.apply {
                if (text.toString() != entry.name) text = entry.name
                contentDescription = entry.name
            }
            detail.analysis = detail.analysis?.let { it.copy(entry = it.entry.copy(name = entry.name, fileStamp = entry.fileStamp)) }
        }
        val signature = "${runtime.mapState.routeRevision}:${runtime.mapState.busy}:${runtime.recording}:${runtime.saving}:${RouteCloudUploads.revision(this)}:${RouteLocalSaves.revision(this)}:${runtime.themes.active.tokens.hashCode()}:${detail.analysis?.route != null}:${entry?.fileStamp}"
        if (signature == detail.signature) return
        detail.signature = signature
        val cloud = if (entry != null && RouteCloudUploads.state(this, entry.id, entry.fileStamp).state == UploadState.UPLOADED) BicycleIcon.CloudConfirmed else BicycleIcon.CloudNotUploaded
        val local = if (entry != null && RouteLocalSaves.completed(this, entry.id, entry.fileStamp)) BicycleIcon.SaveConfirmed else BicycleIcon.Save
        val kinds = listOf(BicycleIcon.ShowOnMap, BicycleIcon.CorrectElevation, cloud, local, BicycleIcon.DeleteTrack)
        kinds.indices.forEach { index ->
            val button = detail.footer.getChildAt(index) as BicycleFooterAction
            button.setIcon(kinds[index], runtime.themes.active.tokens.foreground)
            if (index == 2) button.contentDescription = getString(if (cloud == BicycleIcon.CloudConfirmed) R.string.route_cloud_confirmed else R.string.route_action_cloud)
            if (index == 3) button.contentDescription = getString(if (local == BicycleIcon.SaveConfirmed) R.string.route_save_confirmed else R.string.route_action_save)
            button.isEnabled = !runtime.mapState.busy && entry != null && when (index) {
                0 -> detail.analysis?.route != null
                1 -> !runtime.recording && !runtime.saving && entry.format == "FIT" && entry.origin == TrackOrigin.RECORDED && entry.hasRoute
                2 -> entry.format == "FIT" && entry.origin == TrackOrigin.RECORDED && entry.locallySaved
                else -> true
            }
        }
    }
    private fun executeDetailAction(detail: DetailSession, index: Int) {
        if (runtime.mapState.busy) return
        val entry = detailEntry(detail) ?: return
        when (index) {
            0 -> detail.analysis?.let(::routeAnalysisMap)
            1 -> detail.content.correctElevation?.invoke()
            2 -> if (entry.format == "FIT" && entry.origin == TrackOrigin.RECORDED) runtime.mapState.uploadRoute(entry.id)
            3 -> runtime.mapState.exportRoute(entry.id)
            4 -> deleteRoute(entry)
        }
    }
    private fun openMapLink(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
        catch (_: android.content.ActivityNotFoundException) { BicycleFeedback.show(this, getString(R.string.ui_browser_missing), long = false) }
    }
}
