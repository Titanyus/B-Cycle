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
    private var routeImportButton: Button? = null
    private var routePopup: ListPopupWindow? = null
    private var routeSignature = ""
    private var exportDraft: RouteExportConfig? = null
    private var exportPasswordDraft: String? = null
    private var pendingMapRoute: String? = null
    private var dashboardPage = 0
    private var mapFullscreen = false
    private var controlsVisible = true
    private var displayedMapMessage = ""
    private val dashboardKey get() = if (dashboardPage == 0) "dashboard" else "dashboard_$dashboardPage"
    private val templateKey get() = "dashboardTemplate_$dashboardPage"
    private fun currentTemplate() = DashboardTemplate.decode(preferences.getString(templateKey, DashboardTemplate.entries[dashboardPage].name))
    private var dashboardLayout = DashboardLayout.defaults
    private var picker: AlertDialog? = null
    private var menu: AlertDialog? = null
    private var sensorManager: AlertDialog? = null
    private var sensorRows: LinearLayout? = null
    private var scan: HeartRateScan? = null
    private var scanningBike = false
    private var pendingSensor: String? = null
    private var pendingStart = false
    private var pendingReport: String? = null
    private var displayedRecordingStatus: String? = null
    private val observer: () -> Unit = { render(); refreshSensorRows(); refreshRouteRows() }
    private val preferences get() = runtime.preferences
    private val settingsContext by lazy { android.view.ContextThemeWrapper(this, R.style.SettingsDialogTheme) }
    private val dialogs = DialogNavigation()
    private fun dialog() = BicycleDialogBuilder(this, runtime.themes, dialogs)
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
        dashboardPage = preferences.getInt("dashboardPage", 0).coerceIn(0, 3)
        dashboardLayout = DashboardLayout.decode(preferences.getString(dashboardKey, null))
        mapFullscreen = savedInstanceState?.getBoolean("mapFullscreen", false) ?: false
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        setContentView(root)
        dashboard = DashboardView(this) { chooseField(it) }
        dashboard.changePage = ::changeDashboard
        dashboard.setTemplate(currentTemplate())
        mapPanel = MapPanel(this, runtime, { setMapFullscreen(!mapFullscreen) }, ::enableGps, ::openMapLink)
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
        startButton = toolbarButton("START") {
            if (runtime.recording) startService(Intent(this, RecordingService::class.java).setAction(RecordingService.STOP)) else startRide()
        }
        toolbar.addView(startButton, LinearLayout.LayoutParams(0, dp(58), 3f))
        toolbar.addView(android.widget.ImageButton(this).apply {
            contentDescription = "Streckenmanager öffnen"; tag = "dashboard_routes"
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
        toolbar.addView(toolbarButton("☰", ThemeRole.SECONDARY) { showMenu() }.apply { contentDescription = "Menü öffnen" }, LinearLayout.LayoutParams(0, dp(58), 1f))
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
        startButton.text = if (runtime.recording) "STOPP · SPEICHERN" else if (runtime.saving) "SPEICHERN …" else "START"
        startButton.isEnabled = !runtime.saving || runtime.recording
        val message = runtime.recordingStatus
        if (message != displayedRecordingStatus) {
            displayedRecordingStatus = message
            if (message.startsWith("Gespeichert:") || message.startsWith("FIT nicht gespeichert") || message.startsWith("FIT lokal gespeichert") ||
                message.contains("Fehler") || message.contains("überlastet") ||
                message.startsWith("Aufzeichnung konnte") || message.contains("Fahrten gespeichert"))
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        dashboard.show(dashboardLayout, runtime.dashboardBikeData, gps, runtime.ride.bike.distanceM, runtime.pulseValues)
        dashboard.showModeGlow(ModeGlowSettings.enabled(preferences), runtime.currentAssistMode, runtime.bikeData.assistNames)
        if (::mapPanel.isInitialized) mapPanel.refresh()
        val mapMessage = runtime.mapState.message
        if (mapMessage != displayedMapMessage) {
            displayedMapMessage = mapMessage
            if (mapMessage.isNotEmpty()) Toast.makeText(this, mapMessage, Toast.LENGTH_LONG).show()
        }
        if (runtime.visible && (runtime.connection.isActive || runtime.gpsSensor.active || runtime.heart.active)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    /** Source selector stays outside the scrollable list for dashboard and map fields. */
    private fun chooseField(index: Int, mapField: Boolean = false, changed: (() -> Unit)? = null) {
        fun permitted(): Boolean = runtime.canEditDataFields
        if (!permitted()) return
        picker?.dismiss()
        val selected = if (mapField) MapFieldSelection.read(preferences, index) else dashboardLayout.fields[index]
        fun select(field: DashboardField?) {
            // The bike may have started moving while the selection was open.
            if (!permitted()) { picker?.dismiss(); return }
            if (mapField) {
                preferences.edit().apply {
                    if (field == null) remove(MapFieldSelection.key(index)) else putString(MapFieldSelection.key(index), field.id)
                }.apply()
                mapPanel.refreshSettings(); changed?.invoke()
            } else if (field != null) {
                dashboardLayout = dashboardLayout.replace(index, field)
                preferences.edit().putString(dashboardKey, dashboardLayout.encode()).apply()
            }
            render(); picker?.dismiss()
            if (field?.requiresGps == true) enableGps()
        }
        val content = FieldPickerView(settingsContext, runtime.dashboardBikeData, runtime.gpsValues,
            runtime.ride.bike.distanceM, runtime.pulseValues, selected, mapField, runtime.themes, ::select)
        picker = dialog().setTitle(if (mapField) "Kartenfeld · ${MapFieldSelection.positions[index]}" else "Feld ${index + 1} belegen")
            .setView(content).setNegativeButton("Abbrechen", null).show()
    }
    private fun showMenu() {
        val content = SettingsMenuView(settingsContext, runtime.themes) { action ->
            when (action) {
                SettingsAction.SENSORS -> showSensorManager()
                SettingsAction.MAP -> mapSettings()
                SettingsAction.DASHBOARDS -> dashboardSettings()
                SettingsAction.ROUTES -> routeSettings()
                SettingsAction.ADVANCED -> Unit // Expanded within SettingsMenuView.
                SettingsAction.RECOVERY -> recoverySettings()
                SettingsAction.GPS_HELP -> menu = dialog().setTitle("GPS-Messung").setMessage("Während einer gestarteten Aufzeichnung laufen GPS und Sensoren auch bei ausgeschaltetem Display weiter. Außerhalb einer Aufzeichnung misst GPS bei sichtbarer App.\n\nSTART setzt die Fahrtzähler zurück. Nur angehakte Speicherfelder werden in FIT aufgezeichnet. Die FIT-Hauptquellen für Geschwindigkeit und Distanz werden getrennt gewählt und bleiben während der Fahrt fest.\n\nUngenaue Positionen und Sprünge werden verworfen. Höhe wird geglättet, Steigung benötigt etwa 30–40 m Strecke. Höhe und Höhenmeter sind Näherungen. MSL bezeichnet Meereshöhe, Ellipsoid die andere GPS-Höhenreferenz.").setPositiveButton("Schließen", null).show()
                SettingsAction.DIAGNOSTIC -> diagnosticSettings()
                SettingsAction.RESET -> resetDashboardSettings()
                SettingsAction.FONT_LICENSE -> menu = dialog().setTitle("Bebas Neue · Dharma Type").setMessage(assets.open("Bebas-Neue-OFL.txt").bufferedReader().use { it.readText() }).setPositiveButton("Schließen", null).show()
                SettingsAction.MAP_LICENSE -> menu = dialog().setTitle("Bibliotheken · Lizenzen").setMessage(
                    "Mapsforge · LGPL-3.0 mit Ausnahme\n\n" + assets.open("Mapsforge-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nGarmin FIT SDK 21.218.0\n\n" + assets.open("Garmin-FIT-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nOkHttp 4.12.0 · Apache-2.0\n\n" + assets.open("OkHttp-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nOkio 3.6.0 · Apache-2.0\n\n" + assets.open("Okio-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nLucide 0.468.0 · ISC / Feather MIT\n\n" + assets.open("Lucide-LICENSE.txt").bufferedReader().use { it.readText() })
                    .setPositiveButton("Schließen", null).show()
            }
        }
        menu = dialog().setNegativeButton("Schließen", null).create().apply {
            setView(ScrollView(settingsContext).apply { addView(content) }, 0, 0, 0, 0)
            show()
        }
    }
    private fun recoverySettings() {
        val root = column()
        root.addView(label(runtime.recordingStatus, 14f).apply { setPadding(0, dp(12), 0, dp(12)) })
        root.addView(button("Speichern erneut versuchen", BicycleIcon.ExportTrack) { menu?.dismiss(); runtime.retryPending() }.apply {
            tag = "recovery_execute"; isEnabled = !runtime.recording && !runtime.saving
        })
        menu = dialog().setTitle("Ungespeicherte Fahrten").setView(ScrollView(settingsContext).apply { addView(root) }).setPositiveButton("Schließen", null).show()
    }
    private fun diagnosticSettings() {
        val root = column()
        root.addView(button("Diagnose als Datei speichern", BicycleIcon.ExportTrack) { menu?.dismiss(); exportDiagnostic() }.apply { tag = "diagnostic_execute" })
        menu = dialog().setTitle("Diagnose").setView(ScrollView(settingsContext).apply { addView(root) }).setPositiveButton("Schließen", null).show()
    }
    private fun resetDashboardSettings() {
        val root = column()
        root.addView(button("Belegung zurücksetzen", BicycleIcon.Reset) {
            dashboardLayout = DashboardLayout.defaults
            preferences.edit().putString(dashboardKey, dashboardLayout.encode()).apply()
            render(); menu?.dismiss()
            Toast.makeText(this, "Standardbelegung hergestellt", Toast.LENGTH_SHORT).show()
        }.apply { tag = "reset_execute" })
        menu = dialog().setTitle("Standardbelegung").setView(ScrollView(settingsContext).apply { addView(root) }).setPositiveButton("Schließen", null).show()
    }
    private fun showSensorManager() {
        val root = column(); sensorRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(sensorRows)
        root.addView(button("Sensor hinzufügen", BicycleIcon.AddSensor) {
            menu = dialog().setTitle("Sensor hinzufügen").setItems(arrayOf("Bosch E-Bike", "BLE-Pulsmesser")) { _, index -> selectSensor(if (index == 0) "bike" else "heart") }.show()
        })
        root.addView(BicycleComponents.sectionDivider(settingsContext, runtime.themes))
        val recordingContent = LinearLayout(settingsContext).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE; tag = "sensor_recording_content"
        }
        var recordingBuilt = false
        root.addView(button("FIT-Aufzeichnung", BicycleIcon.Expand) {
            val expand = recordingContent.visibility != View.VISIBLE
            if (expand && !recordingBuilt) {
                recordingContent.addView(recordingControls()); recordingBuilt = true
            }
            recordingContent.visibility = if (expand) View.VISIBLE else View.GONE
            root.findViewWithTag<View>("sensor_recording_toggle").stateDescription = if (expand) "Ausgeklappt" else "Eingeklappt"
        }.apply { tag = "sensor_recording_toggle"; stateDescription = "Eingeklappt" })
        root.addView(recordingContent)
        refreshSensorRows()
        sensorManager = dialog().setTitle("Sensoren & Aufzeichnung").setView(ScrollView(this).apply { addView(root) }).setPositiveButton("Schließen", null).create().apply {
            dialogs.onDismiss(this) { sensorRows = null }; show()
        }
    }
    private fun refreshSensorRows() {
        val rows = sensorRows ?: return
        // Keep focused controls stable when only live status text changes.
        if (rows.childCount > 0) {
            rows.findViewWithTag<TextView>("bike_state")?.text = runtime.bikeStatus
            rows.findViewWithTag<TextView>("heart_state")?.text = runtime.heartStatus
            rows.findViewWithTag<TextView>("gps_state")?.text = runtime.gpsStatus
            return
        }
        fun sensor(title: String, state: String, tag: String, enabled: Boolean, choose: String? = null, kind: String? = null, toggle: () -> Unit) {
            if (rows.childCount > 0) rows.addView(BicycleComponents.sectionDivider(settingsContext, runtime.themes))
            rows.addView(label(title, BicycleDesign.inputSp).apply { setPadding(0, dp(12), 0, dp(4)) })
            rows.addView(label(state, 13f).apply { this.tag = tag })
            if (choose != null && kind != null) rows.addView(button(choose, BicycleIcon.Bluetooth) { selectSensor(kind) })
            rows.addView(button(if (enabled) "Trennen" else "Verbinden", if (enabled) BicycleIcon.Disconnect else BicycleIcon.Bluetooth) { toggle(); rows.removeAllViews(); refreshSensorRows() })
        }
        sensor("GPS", runtime.gpsStatus, "gps_state", runtime.gpsEnabled) { if (runtime.gpsEnabled) runtime.setGpsEnabled(false) else enableGps() }
        sensor("E-Bike · ${if (runtime.bikeAddress == null) "Kein Rad gespeichert" else runtime.bikeName}", runtime.bikeStatus, "bike_state", runtime.bikeEnabled && runtime.bikeAddress != null, "Fahrrad auswählen", "bike") {
            if (runtime.bikeAddress == null) selectSensor("bike") else if (!runtime.hasBluetooth()) requestSensorPermission("bikeConnect") else runtime.setBikeEnabled(!runtime.bikeEnabled)
        }
        sensor("Pulsmesser · ${if (runtime.heartAddress == null) "Kein Sensor gespeichert" else runtime.heartName}", runtime.heartStatus, "heart_state", runtime.heartEnabled && runtime.heartAddress != null, "Sensor auswählen", "heart") {
            if (runtime.heartAddress == null) selectSensor("heart") else if (!runtime.hasBluetooth()) requestSensorPermission("heartConnect") else runtime.setHeartEnabled(!runtime.heartEnabled)
        }
    }
    private fun selectSensor(kind: String) {
        if (!runtime.hasBluetooth()) { requestSensorPermission(kind); return }
        picker?.dismiss(); stopScan()
        val root = column(); val scanLabel = label("Suche …"); root.addView(scanLabel)
        val devices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(devices)
        val seen = mutableSetOf<String>()
        fun found(device: BluetoothDevice, name: String) {
            val address = try { device.address } catch (_: SecurityException) { return }
            if (!seen.add(address)) return
            devices.addView(button("$name\n$address", BicycleIcon.Bluetooth) {
                if (kind == "bike") { scanningBike = false; runtime.selectBike(device, name) } else runtime.selectHeart(device, name)
                picker?.dismiss(); stopScan(); render()
            })
        }
        picker = dialog().setTitle(if (kind == "bike") "Fahrrad auswählen" else "Pulsmesser auswählen")
            .setView(ScrollView(this).apply { addView(root) }).setNegativeButton("Abbrechen", null).create().apply { dialogs.onDismiss(this) { stopScan() }; show() }
        if (kind == "bike") {
            scanningBike = true; runtime.scanFound = ::found; runtime.scanStatus = { scanLabel.text = it }; runtime.beginBikeScan()
        } else {
            scan = HeartRateScan(this, ::found) { scanLabel.text = it }.also { it.start() }
        }
    }
    private fun stopScan() {
        scan?.stop(); scan = null; runtime.scanFound = null; runtime.scanStatus = null
        if (scanningBike) { scanningBike = false; runtime.endBikeScan() }
    }
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
        val current = runtime.config; val selected = current.fields.toMutableSet(); val root = column().apply { setPadding(0, 0, 0, 0) }
        fun source(title: String, chosen: String, bikeId: String): Spinner {
            root.addView(label(title, 16f).apply { setPadding(0, dp(12), 0, 0) })
            return Spinner(settingsContext).apply {
                adapter = ArrayAdapter(settingsContext, android.R.layout.simple_spinner_dropdown_item, listOf("E-Bike", "GPS"))
                setSelection(if (chosen == bikeId) 0 else 1); root.addView(this)
            }
        }
        val speed = source("FIT-Hauptquelle · Geschwindigkeit", current.speedSource, "SPEED")
        val distance = source("FIT-Hauptquelle · Strecke", current.distanceSource, "BIKE_DISTANCE")
        for ((source, fields) in RecordField.options.groupBy { it.source }) {
            root.addView(BicycleComponents.sectionDivider(settingsContext, runtime.themes))
            root.addView(label(source, BicycleDesign.inputSp).apply { setPadding(0, dp(BicycleDesign.verticalPadding), 0, dp(BicycleDesign.smallGap)); isAccessibilityHeading = true })
            for (field in fields) root.addView(CheckBox(settingsContext).apply {
                text = field.title; tag = "record_${field.id}"; isChecked = field.id in selected; minHeight = dp(48)
                setOnCheckedChangeListener { _, checked -> if (checked) selected.add(field.id) else selected.remove(field.id) }
            })
        }
        root.addView(button("Speichern", BicycleIcon.Save) {
            runtime.saveConfig(RecordingConfig(selected.toSet(), if (speed.selectedItemPosition == 0) "SPEED" else "GPS_SPEED", if (distance.selectedItemPosition == 0) "BIKE_DISTANCE" else "GPS_DISTANCE"))
            Toast.makeText(this, "Auswahl für die nächste Fahrt gespeichert", Toast.LENGTH_SHORT).show()
        }.apply { tag = "record_save" })
        // The accordion is built after the dialog opens; apply the same central styles to its children.
        BicycleDialogStyle.content(root, runtime.themes)
        return root
    }

    private fun startRide() {
        if (runtime.recording || runtime.saving) return
        val permissions = mutableListOf<String>()
        if (!runtime.hasBluetooth()) permissions += listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        if (!preferences.getBoolean("recordPermissionsAsked", false)) {
            if (runtime.config.needsGps && !runtime.hasGps()) permissions += listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (permissions.isNotEmpty()) {
            pendingStart = true; preferences.edit().putBoolean("recordPermissionsAsked", true).apply(); requestPermissions(permissions.toTypedArray(), 5); return
        }
        try { startForegroundService(Intent(this, RecordingService::class.java).setAction(RecordingService.START)) }
        catch (_: SecurityException) { Toast.makeText(this, "Aufzeichnung konnte nicht starten. Sensorberechtigungen prüfen.", Toast.LENGTH_LONG).show() }
        catch (_: IllegalStateException) { Toast.makeText(this, "Aufzeichnung bitte bei sichtbarer App starten.", Toast.LENGTH_LONG).show() }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                val kind = pendingSensor; pendingSensor = null
                if (runtime.hasBluetooth()) when (kind) { "bikeConnect" -> runtime.setBikeEnabled(true); "heartConnect" -> runtime.setHeartEnabled(true); "bike", "heart" -> selectSensor(kind) }
                else Toast.makeText(this, "Bluetooth-Zugriff fehlt", Toast.LENGTH_SHORT).show()
            }
            3 -> if (runtime.hasGps()) enableGps() else Toast.makeText(this, "GPS braucht präzisen Standort", Toast.LENGTH_SHORT).show()
            5 -> if (pendingStart) {
                pendingStart = false
                if (runtime.hasBluetooth()) {
                    if (runtime.config.needsGps && !runtime.hasGps()) Toast.makeText(this, "Aufzeichnung startet ohne GPS; präzisen Standort später im Sensormanager erlauben.", Toast.LENGTH_LONG).show()
                    startRide()
                } else Toast.makeText(this, "Bluetooth-Zugriff ist für die Aufzeichnung erforderlich", Toast.LENGTH_LONG).show()
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
        catch (_: android.content.ActivityNotFoundException) { pendingReport = null; Toast.makeText(this, R.string.export_unavailable, Toast.LENGTH_LONG).show() }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("pendingReport", pendingReport); outState.putBoolean("mapFullscreen", mapFullscreen); super.onSaveInstanceState(outState) }
    @Deprecated("Native document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) data?.data?.let { uri ->
                try {
                    val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    if (flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION == 0) error("Keine Schreibberechtigung")
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    exportDraft = (exportDraft ?: RouteExportSettings(this).read()).copy(tree = uri.toString())
                } catch (_: Exception) { Toast.makeText(this, "Ordnerzugriff konnte nicht gespeichert werden", Toast.LENGTH_LONG).show() }
            }
            if (dialogs.empty) { showMenu(); routeSettings(); routeExportSettings() }
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
            runOnUiThread { Toast.makeText(applicationContext, if (ok) R.string.export_saved else R.string.export_failed, Toast.LENGTH_LONG).show() }
        }, "B-Cycle-diagnostic-export").start()
    }
    override fun onStart() { super.onStart(); mapPanel.resume(); runtime.observers.add(observer); runtime.setVisible(true); render(); refreshRouteRows() }
    override fun onStop() {
        dialogs.closeAll(); stopScan()
        mapPanel.pause(); runtime.observers.remove(observer); runtime.setVisible(false); window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); super.onStop()
    }
    override fun onDestroy() { mapPanel.destroy(); super.onDestroy() }
    @Deprecated("Native Activity back handling")
    override fun onBackPressed() { if (mapFullscreen) setMapFullscreen(false) else super.onBackPressed() }

    private fun setMapFullscreen(full: Boolean) {
        mapFullscreen = full; dashboard.visibility = if (full) View.GONE else View.VISIBLE
        mapPanel.showFullscreen(full); dashboardArea.requestLayout()
    }
    private fun changeDashboard(delta: Int) {
        dashboardPage = (dashboardPage + delta + 4) % 4
        preferences.edit().putInt("dashboardPage", dashboardPage).apply()
        dashboardLayout = DashboardLayout.decode(preferences.getString(dashboardKey, null))
        val template = currentTemplate(); dashboard.setTemplate(template)
        dashboardArea.requestLayout()
        (mapPanel.layoutParams as LinearLayout.LayoutParams).weight = 1f - template.dataFraction
        setMapFullscreen(false); render()
    }
    private fun dashboardSettings() {
        val root = column()
        root.addView(ThemePickerView(settingsContext, runtime.themes))
        val presets = DashboardPresetPicker(settingsContext, (0 until 4).map { page ->
            DashboardTemplate.decode(preferences.getString("dashboardTemplate_$page", DashboardTemplate.entries[page].name))
        }, dashboardPage, runtime.themes)
        root.addView(presets)
        root.addView(BicycleComponents.sectionDivider(settingsContext, runtime.themes))
        root.addView(label("Fahrmodus-Glow", BicycleDesign.inputSp))
        root.addView(label("Zeigt den aktiven Bosch-Fahrmodus durch einen farbigen Lichtschein am Rand der Datenfelder."))
        val glow = Switch(settingsContext).apply {
            text = "Fahrmodus-Glow"; tag = "mode_glow_enabled"
            isChecked = ModeGlowSettings.enabled(preferences)
        }
        root.addView(glow)
        root.addView(BicycleComponents.sectionDivider(settingsContext, runtime.themes))
        root.addView(button("Karten-Datenfelder", BicycleIcon.ShowOnMap) { mapDataFieldSettings() }.apply { tag = "map_fields_manage" })
        menu = dialog().setTitle("Design & UI").setView(ScrollView(this).apply { tag = "design_ui_scroll"; addView(root) })
            .setPositiveButton("Speichern") { _, _ ->
                val edit = preferences.edit(); presets.selection.forEachIndexed { page, template -> edit.putString("dashboardTemplate_$page", template.name) }; edit.apply()
                ModeGlowSettings.save(preferences, glow.isChecked)
                changeDashboard(0)
            }.setNegativeButton("Abbrechen", null).show()
    }
    private fun mapSettings() {
        val root = column()
        root.addView(label("Kartenquelle", BicycleDesign.inputSp))
        val source = Spinner(settingsContext).apply {
            adapter = ArrayAdapter(settingsContext, android.R.layout.simple_spinner_dropdown_item, MapSource.entries.map { it.title })
            setSelection(MapSource.decode(preferences.getString("mapSource", null)).ordinal); root.addView(this)
        }
        fun check(title: String, key: String, fallback: Boolean) = CheckBox(settingsContext).apply { text = title; isChecked = preferences.getBoolean(key, fallback); root.addView(this) }
        root.addView(BicycleComponents.sectionDivider(settingsContext, runtime.themes))
        val heading = check("Fahrtrichtung oben (sonst Norden)", "mapHeading", true)
        val follow = check("GPS automatisch folgen", "mapFollow", true)
        val track = check("Gefahrenen Track anzeigen", "mapTrackVisible", true)
        val gpx = check("Importierte GPX-Strecke anzeigen", "mapGpxVisible", true)
        root.addView(BicycleComponents.sectionDivider(settingsContext, runtime.themes))
        root.addView(button("GPS aktivieren / Berechtigung erlauben", BicycleIcon.Map) { enableGps() })
        root.addView(button("CyclOSM-Legende öffnen", BicycleIcon.Info) { openMapLink(MapSource.CYCLOSM.creditUrl) })
        menu = dialog().setTitle("Kartenmanager").setView(ScrollView(this).apply { addView(root) })
            .setPositiveButton("Speichern") { _, _ ->
                preferences.edit().putString("mapSource", MapSource.entries[source.selectedItemPosition].name)
                    .putBoolean("mapHeading", heading.isChecked).putBoolean("mapFollow", follow.isChecked)
                    .putBoolean("mapTrackVisible", track.isChecked).putBoolean("mapGpxVisible", gpx.isChecked).apply()
                mapPanel.refreshSettings()
            }.setNegativeButton("Abbrechen", null).show()
    }
    private fun mapDataFieldSettings() {
        val root = column()
        for (index in MapFieldSelection.positions.indices) {
            val slot = button("") {}.apply { tag = "map_field_slot_$index" }
            fun updateTitle() {
                val field = MapFieldSelection.read(preferences, index)
                val title = field?.let {
                    val name = it.title(runtime.bikeData, runtime.gpsValues)
                    if (it.showSource) "${it.source} · $name" else name
                } ?: "Leer"
                slot.text = "${MapFieldSelection.positions[index]} · $title"
            }
            updateTitle()
            slot.setOnClickListener { chooseField(index, mapField = true, changed = ::updateTitle) }
            root.addView(slot)
        }
        menu = dialog().setTitle("Karten-Datenfelder").setView(ScrollView(settingsContext).apply { addView(root) })
            .setPositiveButton("Schließen", null).show()
    }
    private fun importRoute(recorded: Boolean = false) {
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, if (recorded)
                    arrayOf("application/vnd.ant.fit", "application/octet-stream") else
                    arrayOf("application/gpx+xml", "application/xml", "text/xml", "application/vnd.ant.fit", "application/octet-stream")),
                if (recorded) 9 else 8)
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Keine Dateiauswahl verfügbar", Toast.LENGTH_LONG).show()
        }
    }
    private fun routeSettings() {
        val state = runtime.mapState
        routeSignature = ""
        val content = RouteManagerView(settingsContext, { entry ->
            val displayed = state.selectedRoute == entry.id && state.route != null && preferences.getBoolean("mapGpxVisible", true)
            state.setRouteVisible(!displayed)
            if (!displayed && state.selectedRoute != entry.id) state.selectRoute(entry.id)
        }, ::routeActions, ::routeExportSettings, themes = runtime.themes,
            uploadState = { RouteUploads.state(this, it.id, it.fileStamp) },
            upload = { state.uploadRoute(it.id) }, local = { state.saveRouteLocally(it.id) })
        routeManager = content
        refreshRouteRows()
        menu = dialog().setView(content).setNeutralButton("Importieren", null).setPositiveButton("Schließen", null).create().apply {
            setView(content, 0, 0, 0, 0)
            dialogs.onDismiss(this) { if (routeManager === content) { routePopup?.dismiss(); routePopup = null; routeManager = null; routeImportButton = null } }; show()
            routeImportButton = getButton(AlertDialog.BUTTON_NEUTRAL).apply {
                tag = "route_import"; isEnabled = !state.busy
                setCompoundDrawablesRelative(BicycleIcons.drawable(this@MainActivity, BicycleIcon.Import, runtime.themes.active.tokens.foreground)
                    .apply { setBounds(0, 0, dp(BicycleDesign.iconSize), dp(BicycleDesign.iconSize)) }, null, null, null)
                compoundDrawablePadding = dp(BicycleDesign.verticalPadding); setOnClickListener { if (!state.busy) routeImportChoice() }
            }
        }
        val screen = menu!!
        val decor = screen.window!!.decorView
        BicycleComponents.bindTheme(decor, runtime.themes) { tokens ->
            screen.window!!.setBackgroundDrawable(android.graphics.drawable.GradientDrawable().apply {
                setColor(tokens.secondary); cornerRadius = dp(BicycleDesign.dialogRadius).toFloat()
            })
            decor.clipToOutline = true
            listOf(AlertDialog.BUTTON_NEUTRAL, AlertDialog.BUTTON_POSITIVE).forEach { which ->
                screen.getButton(which).apply {
                    setTextColor(tokens.foreground); typeface = android.graphics.Typeface.DEFAULT; textSize = BicycleDesign.bodySp
                    backgroundTintList = null; background = null
                    (parent as? View)?.setBackgroundColor(tokens.secondary)
                }
            }
            screen.getButton(AlertDialog.BUTTON_NEUTRAL).compoundDrawablesRelative[0]?.setTint(tokens.foreground)
        }
        BicycleDialogStyle.actionIcon(screen.getButton(AlertDialog.BUTTON_POSITIVE), runtime.themes, BicycleIcon.Close)
        state.refreshLibrary()
    }
    private fun routeImportChoice() {
        val anchor = routeImportButton ?: return
        routeDropdown(anchor, listOf("Geplante Strecke (GPX / FIT)", "Aufgezeichnete Fahrt (FIT)"),
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
    private fun routeExportSettings() {
        val settings = RouteExportSettings(this)
        lateinit var content: RouteExportView
        content = RouteExportView(settingsContext, exportDraft ?: settings.read(), settings.hasPassword(), {
            exportDraft = content.read(); exportPasswordDraft = content.password.text.toString().takeIf { it.isNotEmpty() }
            try {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), 10)
            } catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(this, "Keine Ordnerauswahl verfügbar", Toast.LENGTH_LONG).show()
            }
        }, {
            val config = content.read()
            val newPassword = content.password.text.toString().takeIf { it.isNotEmpty() }
            content.testing(true)
            Thread({
                val message = try {
                    WebDavClient(config, newPassword ?: settings.password()).test(); "Verbindung erfolgreich · Zielverzeichnis erreichbar"
                } catch (e: Exception) {
                    when (e) {
                        is DavFailure -> e.message ?: "Verbindung fehlgeschlagen"
                        is IllegalArgumentException -> e.message ?: "Einstellungen prüfen"
                        else -> "Verbindung fehlgeschlagen · Adresse, Netzwerk und App-Passwort prüfen"
                    }
                }
                runOnUiThread { content.testing(false); content.result.text = message }
            }, "B-Cycle-WebDAV-Test").start()
        }, runtime.themes, storedPassword = {
            runCatching { settings.password() }.getOrElse {
                content.result.text = "Gespeichertes Passwort konnte nicht gelesen werden"
                ""
            }
        })
        BicycleDialogStyle.content(content, runtime.themes)
        exportPasswordDraft?.let { content.password.setText(it) }
        exportDraft = null; exportPasswordDraft = null
        val screen = dialog().setTitle("Streckeneinstellungen").setView(ScrollView(settingsContext).apply { addView(content) })
            .setPositiveButton("Speichern", null).setNegativeButton("Abbrechen", null).create()
        screen.show()
        screen.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            try {
                val config = content.read()
                val password = content.password.text.toString().takeIf { it.isNotEmpty() }
                if (config.webdav) {
                    WebDavClient.directory(config)
                    require(password != null || settings.hasPassword()) { "Bitte ein Nextcloud-App-Passwort eingeben" }
                }
                screen.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                val appContext = applicationContext
                Thread({
                    val success = runCatching { settings.write(config, password); RouteUploads.settingsChanged(appContext) }.isSuccess
                    runOnUiThread {
                        if (!isDestroyed) {
                            screen.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            if (success) { routeSignature = ""; refreshRouteRows(); screen.dismiss() }
                            else content.result.text = "Einstellungen konnten nicht gespeichert werden · App-Passwort prüfen"
                        }
                    }
                }, "B-Cycle-Streckeneinstellungen").start()
            } catch (e: Exception) {
                content.result.text = if (e is IllegalArgumentException) e.message else "Einstellungen konnten nicht gespeichert werden · App-Passwort prüfen"
            }
        }
    }
    private fun refreshRouteRows() {
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
        val signature = "${state.routeRevision}:${state.busy}:${RouteUploads.revision(this)}:$displayed"
        if (routeSignature == signature) return
        routeSignature = signature
        manager.submit(state.entries, displayed, state.busy)
    }
    private fun routeActions(entry: ManagedRoute) {
        val state = runtime.mapState
        if (state.busy) return
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        if (entry.hasRoute) {
            actions += "Auf Karte ansehen" to {
                preferences.edit().putBoolean("mapGpxVisible", true).apply()
                pendingMapRoute = entry.id; state.selectRoute(entry.id)
            }
            if (!entry.recorded) actions += "Als geplante Strecke auswählen" to {
                preferences.edit().putBoolean("mapGpxVisible", true).apply(); state.selectRoute(entry.id)
            }
        }
        actions += "Umbenennen" to {
            val input = EditText(settingsContext).apply { setText(entry.name); setSingleLine(); selectAll()
                filters = arrayOf(android.text.InputFilter.LengthFilter(120)); tag = "route_rename" }
            val rename = dialog().setTitle("Strecke umbenennen").setView(input)
                .setPositiveButton("Speichern", null).setNegativeButton("Abbrechen", null).create()
            rename.show()
            rename.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isEmpty()) input.error = "Bitte einen Namen eingeben"
                else { state.renameRoute(entry.id, name); rename.dismiss() }
            }
        }
        if (entry.format == "FIT" && entry.origin == TrackOrigin.RECORDED) actions += "Zu Nextcloud hochladen / erneut versuchen" to {
            state.uploadRoute(entry.id)
        }
        actions += "Nach Downloads exportieren" to { state.exportRoute(entry.id) }
        actions += "Aus Sammlung löschen" to {
            dialog().setTitle("Strecke aus Sammlung löschen?")
                .setMessage("${entry.name}\n\nDie appinterne Datei wird gelöscht. Vorhandene Dateien in Downloads oder an der Importquelle bleiben erhalten.")
                .setPositiveButton("Löschen") { _, _ -> state.deleteRoute(entry.id) }
                .setNegativeButton("Abbrechen", null).show()
        }
        val anchor = routeManager?.findViewWithTag<View>("route_more_${entry.id}") ?: return
        routeDropdown(anchor, actions.map { it.first }, actions.map { when (it.first) {
            "Auf Karte ansehen", "Als geplante Strecke auswählen" -> BicycleIcon.ShowOnMap
            "Umbenennen" -> BicycleIcon.RenameTrack
            "Zu Nextcloud hochladen / erneut versuchen" -> BicycleIcon.CloudUploaded
            "Nach Downloads exportieren" -> BicycleIcon.ExportTrack
            else -> BicycleIcon.DeleteTrack
        } }) { which -> actions[which].second() }
    }
    private fun openMapLink(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
        catch (_: android.content.ActivityNotFoundException) { Toast.makeText(this, "Kein Browser verfügbar", Toast.LENGTH_SHORT).show() }
    }
}
