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
    private var routeRows: LinearLayout? = null
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
    private fun dialog() = object : AlertDialog.Builder(this, R.style.SettingsDialogTheme) {
        override fun create(): AlertDialog = super.create().also { dialogs.track(it) }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(text: String, size: Float = 14f, color: Int = Color.WHITE) = TextView(settingsContext).apply {
        this.text = text; textSize = size; setTextColor(color)
    }
    private fun column() = LinearLayout(settingsContext).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
    private fun button(text: String, action: () -> Unit) = Button(settingsContext).apply { this.text = text; isAllCaps = false; setOnClickListener { action() } }
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
        mapPanel = MapPanel(this, runtime, { setMapFullscreen(!mapFullscreen) }, ::changeDashboard, ::enableGps, ::openMapLink)
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
        fun toolbarButton(title: String, color: Int = getColor(R.color.start_red), action: () -> Unit) = button(title, action).apply {
            textSize = 20f; setTextColor(Color.WHITE); minWidth = 0; minHeight = 0
            setPadding(dp(4), 0, dp(4), 0)
            background = android.graphics.drawable.GradientDrawable().apply { setColor(color); setStroke(1, Color.rgb(70, 0, 0)) }
        }
        startButton = toolbarButton("START") {
            if (runtime.recording) startService(Intent(this, RecordingService::class.java).setAction(RecordingService.STOP)) else startRide()
        }
        toolbar.addView(startButton, LinearLayout.LayoutParams(0, dp(58), 3f))
        toolbar.addView(toolbarButton("☰", getColor(R.color.settings_red)) { showMenu() }.apply { contentDescription = "Menü öffnen" }, LinearLayout.LayoutParams(0, dp(58), 1f))
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
            if (message.startsWith("Gespeichert:") || message.startsWith("FIT nicht gespeichert") ||
                message.contains("Fehler") || message.contains("überlastet") ||
                message.startsWith("Aufzeichnung konnte") || message.contains("Fahrten gespeichert"))
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        dashboard.show(dashboardLayout, runtime.bikeData, gps, runtime.ride.bike.distanceM, runtime.pulseValues)
        if (::mapPanel.isInitialized) mapPanel.refresh()
        val mapMessage = runtime.mapState.message
        if (mapMessage != displayedMapMessage) {
            displayedMapMessage = mapMessage
            if (mapMessage.isNotEmpty()) Toast.makeText(this, mapMessage, Toast.LENGTH_LONG).show()
        }
        if (runtime.visible && (runtime.connection.isActive || runtime.gpsSensor.active || runtime.heart.active)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    /** Starts with three collapsed source categories, builds only the opened source's rows. */
    private fun chooseField(index: Int, mapField: Boolean = false, changed: (() -> Unit)? = null) {
        picker?.dismiss()
        val root = column()
        val selected = if (mapField) MapFieldSelection.read(preferences, index) else dashboardLayout.fields[index]
        fun select(field: DashboardField?) {
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
            if (field?.source == "GPS") enableGps()
        }
        if (mapField) root.addView(RadioButton(settingsContext).apply {
            text = "Leer"; tag = "map_field_empty"; isChecked = selected == null
            setOnClickListener { select(null) }
        })
        val groups = DashboardField.groups(runtime.bikeData, selected).groupBy { it.source }
        for ((source, sections) in groups) {
            val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
            val heading = button("▸ $source") {}.apply { tag = "category_$source"; isAccessibilityHeading = true; stateDescription = "Eingeklappt" }
            var built = false
            heading.setOnClickListener {
                val expand = inner.visibility != View.VISIBLE
                if (expand && !built) {
                    for (section in sections) {
                        inner.addView(label(section.title, 14f, Color.LTGRAY).apply { setPadding(0, dp(10), 0, dp(4)); isAccessibilityHeading = true })
                        for (field in section.fields) {
                            val value = field.value(runtime.bikeData, runtime.gpsValues, runtime.ride.bike.distanceM, runtime.pulseValues)
                            val title = if (field.id == "RIDER_POWER") "Fahrerleistung (eigene)" else field.title(runtime.bikeData, runtime.gpsValues)
                            inner.addView(RadioButton(settingsContext).apply {
                                tag = field.id; text = "$title${if (value.unit.isNotEmpty()) " · ${value.unit}" else ""}"
                                textSize = 16f; setTextColor(Color.WHITE); minHeight = dp(48)
                                isChecked = selected == field
                                contentDescription = if (field.showSource) "$source · $text" else text
                                setOnClickListener {
                                    select(field)
                                }
                            })
                        }
                    }
                    built = true
                }
                inner.visibility = if (expand) View.VISIBLE else View.GONE
                heading.text = "${if (expand) "▾" else "▸"} $source"
                heading.stateDescription = if (expand) "Ausgeklappt" else "Eingeklappt"
            }
            root.addView(heading); root.addView(inner)
        }
        picker = dialog().setTitle(if (mapField) "Kartenfeld · ${MapFieldSelection.positions[index]}" else "Feld ${index + 1} belegen").setView(ScrollView(this).apply { addView(root) }).setNegativeButton("Abbrechen", null).show()
    }
    private fun showMenu() {
        val content = SettingsMenuView(settingsContext) { action ->
            when (action) {
                SettingsAction.SENSORS -> showSensorManager()
                SettingsAction.RECORDING -> recordingSettings()
                SettingsAction.MAP -> mapSettings()
                SettingsAction.DASHBOARDS -> dashboardSettings()
                SettingsAction.ROUTES -> routeSettings()
                SettingsAction.ADVANCED -> Unit // Expanded within SettingsMenuView.
                SettingsAction.RECOVERY -> recoverySettings()
                SettingsAction.GPS_HELP -> menu = dialog().setTitle("GPS-Messung").setMessage("Während einer gestarteten Aufzeichnung laufen GPS und Sensoren auch bei ausgeschaltetem Display weiter. Außerhalb einer Aufzeichnung misst GPS bei sichtbarer App.\n\nSTART setzt die Fahrtzähler zurück. Nur angehakte Speicherfelder werden in FIT aufgezeichnet. Die FIT-Hauptquellen für Geschwindigkeit und Distanz werden getrennt gewählt und bleiben während der Fahrt fest.\n\nUngenaue Positionen und Sprünge werden verworfen. Höhe wird geglättet, Steigung benötigt etwa 30–40 m Strecke. Höhe und Höhenmeter sind Näherungen. MSL bezeichnet Meereshöhe, Ellipsoid die andere GPS-Höhenreferenz.").setPositiveButton("Schließen", null).show()
                SettingsAction.DIAGNOSTIC -> diagnosticSettings()
                SettingsAction.RESET -> resetDashboardSettings()
                SettingsAction.FONT_LICENSE -> menu = dialog().setTitle("Bebas Neue · Dharma Type").setMessage(assets.open("Bebas-Neue-OFL.txt").bufferedReader().use { it.readText() }).setPositiveButton("Schließen", null).show()
                SettingsAction.MAP_LICENSE -> menu = dialog().setTitle("Mapsforge · LGPL-3.0 mit Ausnahme").setMessage(assets.open("Mapsforge-LICENSE.txt").bufferedReader().use { it.readText() }).setPositiveButton("Schließen", null).show()
            }
        }
        menu = dialog().setNegativeButton("Schließen", null).create().apply {
            setView(ScrollView(settingsContext).apply { addView(content) }, 0, 0, 0, 0)
            show()
        }
    }
    private fun recoverySettings() {
        val root = column()
        root.addView(label("Vollständige Messpunkte aus noch nicht gespeicherten Fahrten werden erneut als FIT-Dateien nach Download/B-Cycle exportiert.", 15f))
        root.addView(label(runtime.recordingStatus, 14f, Color.LTGRAY).apply { setPadding(0, dp(12), 0, dp(12)) })
        root.addView(button("Speichern erneut versuchen") { menu?.dismiss(); runtime.retryPending() }.apply {
            tag = "recovery_execute"; isEnabled = !runtime.recording && !runtime.saving
        })
        menu = dialog().setTitle("Ungespeicherte Fahrten").setView(ScrollView(settingsContext).apply { addView(root) }).setPositiveButton("Schließen", null).show()
    }
    private fun diagnosticSettings() {
        val root = column()
        root.addView(label("Exportiert die aktuelle Diagnose als Textdatei. Sie enthält Verbindungsprotokolle, Sensorstatus und GPS-Messwerte. Den Speicherort wählst du im nächsten Schritt.", 15f))
        root.addView(button("Diagnose als Datei speichern") { menu?.dismiss(); exportDiagnostic() }.apply { tag = "diagnostic_execute" })
        menu = dialog().setTitle("Diagnose").setView(ScrollView(settingsContext).apply { addView(root) }).setPositiveButton("Schließen", null).show()
    }
    private fun resetDashboardSettings() {
        val root = column()
        root.addView(label("Stellt die Standard-Messwertbelegung für Dashboard ${dashboardPage + 1} wieder her. Die anderen Dashboard-Seiten, Vorlagen, Sensoren und gespeicherten Fahrten bleiben erhalten.", 15f))
        root.addView(button("Belegung zurücksetzen") {
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
        root.addView(button("Sensor hinzufügen") {
            menu = dialog().setTitle("Sensor hinzufügen").setItems(arrayOf("Bosch E-Bike", "BLE-Pulsmesser")) { _, index -> selectSensor(if (index == 0) "bike" else "heart") }.show()
        })
        refreshSensorRows()
        sensorManager = dialog().setTitle("Sensoren").setView(ScrollView(this).apply { addView(root) }).setPositiveButton("Schließen", null).create().apply {
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
            rows.addView(label(title, 18f).apply { setPadding(0, dp(12), 0, dp(4)) })
            rows.addView(label(state, 13f, Color.LTGRAY).apply { this.tag = tag })
            if (choose != null && kind != null) rows.addView(button(choose) { selectSensor(kind) })
            rows.addView(button(if (enabled) "Trennen" else "Verbinden") { toggle(); rows.removeAllViews(); refreshSensorRows() })
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
            devices.addView(button("$name\n$address") {
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
    private fun recordingSettings() {
        val current = runtime.config; val selected = current.fields.toMutableSet(); val root = column()
        root.addView(label("Gilt für die nächste Fahrt. Nur angehakte Werte werden gespeichert. Fehlende Sensorwerte bleiben unbekannt.", 14f, Color.LTGRAY))
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
            root.addView(label(source, 20f).apply { setPadding(0, dp(18), 0, dp(6)); isAccessibilityHeading = true })
            for (field in fields) root.addView(CheckBox(settingsContext).apply {
                text = field.title; tag = "record_${field.id}"; isChecked = field.id in selected; minHeight = dp(48)
                setOnCheckedChangeListener { _, checked -> if (checked) selected.add(field.id) else selected.remove(field.id) }
            })
        }
        menu = dialog().setTitle("Aufzeichnungsmanager").setView(ScrollView(this).apply { addView(root) })
            .setPositiveButton("Speichern") { _, _ ->
                runtime.saveConfig(RecordingConfig(selected.toSet(), if (speed.selectedItemPosition == 0) "SPEED" else "GPS_SPEED", if (distance.selectedItemPosition == 0) "BIKE_DISTANCE" else "GPS_DISTANCE"))
                Toast.makeText(this, "Auswahl für die nächste Fahrt gespeichert", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Abbrechen", null).show()
    }
    private fun startRide() {
        if (runtime.recording || runtime.saving) return
        val permissions = mutableListOf<String>()
        if (!runtime.hasBluetooth()) permissions += listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        if (!preferences.getBoolean("recordPermissionsAsked", false)) {
            if (runtime.config.fields.any { it.startsWith("GPS_") } && !runtime.hasGps()) permissions += listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
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
                    if (runtime.config.fields.any { it.startsWith("GPS_") } && !runtime.hasGps()) Toast.makeText(this, "Aufzeichnung startet ohne GPS; präzisen Standort später im Sensormanager erlauben.", Toast.LENGTH_LONG).show()
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
            .putExtra(Intent.EXTRA_TITLE, "B-Cycle-0.6.6-diagnose.txt"), 2) }
        catch (_: android.content.ActivityNotFoundException) { pendingReport = null; Toast.makeText(this, R.string.export_unavailable, Toast.LENGTH_LONG).show() }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("pendingReport", pendingReport); outState.putBoolean("mapFullscreen", mapFullscreen); super.onSaveInstanceState(outState) }
    @Deprecated("Native document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 8) {
            if (resultCode == RESULT_OK) data?.data?.let { runtime.mapState.import(it) }
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
        Toast.makeText(this, "Dashboard ${dashboardPage + 1}/4 · ${template.title}", Toast.LENGTH_SHORT).show()
    }
    private fun dashboardSettings() {
        val root = column()
        root.addView(label("Vier Seiten mit festen Vorlagen. Die Messwerte jeder Seite sind unabhängig frei belegbar. Wechsel über ‹/› auf der Karte.", 14f, Color.LTGRAY))
        val presets = DashboardPresetPicker(settingsContext, (0 until 4).map { page ->
            DashboardTemplate.decode(preferences.getString("dashboardTemplate_$page", DashboardTemplate.entries[page].name))
        }, dashboardPage)
        root.addView(presets)
        menu = dialog().setTitle("Dashboards").setView(ScrollView(this).apply { addView(root) })
            .setPositiveButton("Speichern") { _, _ ->
                val edit = preferences.edit(); presets.selection.forEachIndexed { page, template -> edit.putString("dashboardTemplate_$page", template.name) }; edit.apply()
                changeDashboard(0)
            }.setNegativeButton("Abbrechen", null).show()
    }
    private fun mapSettings() {
        val root = column()
        root.addView(label("Online-Karten benötigen Internet. Der Kartenserver erhält die angefragten Kartenausschnitte. GPS und FIT bleiben lokal. Bei CyclOSM sind Radwege, Fahrradrouten und Belagsangaben Teil der fertigen Karte; sie können nicht einzeln ausgeblendet werden.", 14f, Color.LTGRAY))
        root.addView(button("Karten-Datenfelder") { mapDataFieldSettings() }.apply { tag = "map_fields_manage" })
        root.addView(label("Kartenquelle", 18f))
        val source = Spinner(settingsContext).apply {
            adapter = ArrayAdapter(settingsContext, android.R.layout.simple_spinner_dropdown_item, MapSource.entries.map { it.title })
            setSelection(MapSource.decode(preferences.getString("mapSource", null)).ordinal); root.addView(this)
        }
        fun check(title: String, key: String, fallback: Boolean) = CheckBox(settingsContext).apply { text = title; isChecked = preferences.getBoolean(key, fallback); root.addView(this) }
        val heading = check("Fahrtrichtung oben (sonst Norden)", "mapHeading", true)
        val follow = check("GPS automatisch folgen", "mapFollow", true)
        val track = check("Gefahrenen Track anzeigen", "mapTrackVisible", true)
        val gpx = check("Importierte GPX-Strecke anzeigen", "mapGpxVisible", true)
        root.addView(label("Live-Track nur nach START und bei aktivierter GPS-Route im Aufzeichnungsmanager. Befestigt bedeutet nicht automatisch rennradtauglich; fehlende OSM-Angaben bleiben unbekannt.", 14f, Color.LTGRAY))
        root.addView(button("GPS aktivieren / Berechtigung erlauben") { enableGps() })
        root.addView(button("CyclOSM-Legende öffnen") { openMapLink(MapSource.CYCLOSM.creditUrl) })
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
        root.addView(label("Drei transparente Messwerte unten auf der Karte, gemeinsam für alle Dashboards und das Vollbild. Wähle links, Mitte oder rechts. Leer entfernt einen Wert; ohne empfangene Daten bleibt das Feld unsichtbar. Die Belegung wird beim Auswählen übernommen.", 14f, Color.LTGRAY))
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
    private fun importGpx() {
        if (runtime.mapState.busy) { Toast.makeText(this, "GPX wird noch verarbeitet", Toast.LENGTH_SHORT).show(); return }
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), 8)
        } catch (_: android.content.ActivityNotFoundException) { Toast.makeText(this, "Dateiauswahl nicht verfügbar", Toast.LENGTH_LONG).show() }
    }
    private fun routeSettings() {
        val root = column()
        root.addView(label("Gespeicherte GPX-Strecken. Das Auge zeigt eine Strecke an oder blendet sie aus. Keine Routenberechnung oder Abbiegehinweise.", 14f, Color.LTGRAY))
        root.addView(button("GPX-Strecke importieren") { importGpx() })
        routeRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(routeRows)
        refreshRouteRows()
        menu = dialog().setTitle("Streckenmanager").setView(ScrollView(this).apply { addView(root) })
            .setPositiveButton("Schließen", null).create().apply { dialogs.onDismiss(this) { routeRows = null }; show() }
    }
    private fun refreshRouteRows() {
        val rows = routeRows ?: return
        val state = runtime.mapState
        val signature = "${state.routeRevision}:${state.busy}"
        if (rows.tag == signature) return
        rows.tag = signature; rows.removeAllViews()
        if (state.busy) rows.addView(label("GPX wird verarbeitet …", 14f, Color.LTGRAY))
        if (state.routes.isEmpty()) rows.addView(label("Noch keine GPX-Strecke gespeichert", 16f))
        for (entry in state.routes) {
            val selected = state.selectedRoute == entry.id
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
            row.addView(label("${entry.name}\n${entry.points} Punkte · ${entry.segments} Abschnitte", 15f).apply { tag = "route_name_${entry.id}" }, LinearLayout.LayoutParams(0, -2, 1f))
            val eye = ImageButton(settingsContext).apply {
                tag = "route_eye_${entry.id}"; setPadding(dp(12), dp(12), dp(12), dp(12)); setImageDrawable(RouteActionIcon(false, selected))
                contentDescription = if (selected) "${entry.name} ausblenden" else "${entry.name} anzeigen"
                isEnabled = !state.busy; setOnClickListener {
                    preferences.edit().putBoolean("mapGpxVisible", true).apply()
                    state.selectRoute(if (selected) null else entry.id)
                }
            }
            row.addView(eye, LinearLayout.LayoutParams(dp(48), dp(48)))
            row.addView(ImageButton(settingsContext).apply {
                tag = "route_delete_${entry.id}"; setPadding(dp(12), dp(12), dp(12), dp(12)); setImageDrawable(RouteActionIcon(true))
                contentDescription = "${entry.name} löschen"; isEnabled = !state.busy
                setOnClickListener {
                    dialog().setTitle("GPX-Strecke löschen?").setMessage(entry.name)
                        .setPositiveButton("Löschen") { _, _ -> state.deleteRoute(entry.id) }.setNegativeButton("Abbrechen", null).show()
                }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            rows.addView(row)
        }
        if (state.route != null) rows.addView(button("Angezeigte Strecke vollständig zeigen") { dialogs.closeAll(); mapPanel.fitRoute() }.apply { isEnabled = !state.busy })
    }
    private fun openMapLink(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
        catch (_: android.content.ActivityNotFoundException) { Toast.makeText(this, "Kein Browser verfügbar", Toast.LENGTH_SHORT).show() }
    }
}
