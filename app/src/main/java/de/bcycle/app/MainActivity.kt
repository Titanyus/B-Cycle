package de.bcycle.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import java.io.IOException

/** Native Views remain the UI; the runtime also survives while the recording service is active. */
class MainActivity : Activity() {
    private lateinit var runtime: RideRuntime
    private lateinit var dashboard: DashboardView
    private lateinit var summary: TextView
    private lateinit var status: TextView
    private lateinit var packetAge: TextView
    private lateinit var sensorsStatus: TextView
    private lateinit var recordingStatus: TextView
    private lateinit var startButton: Button
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
    private val observer: () -> Unit = { render(); refreshSensorRows() }
    private val preferences get() = runtime.preferences
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(text: String, size: Float = 14f, color: Int = Color.WHITE) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
    private fun button(text: String, action: () -> Unit) = Button(this).apply { this.text = text; isAllCaps = false; setOnClickListener { action() } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = RideRuntime.get(this)
        pendingReport = savedInstanceState?.getString("pendingReport")
        dashboardLayout = DashboardLayout.decode(preferences.getString("dashboard", null))
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        setContentView(root)
        val header = column()
        summary = label("", 12f).apply { isSingleLine = true }
        status = label("", 12f, Color.LTGRAY).apply { maxLines = 2; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        packetAge = label("", 11f, Color.GRAY)
        sensorsStatus = label("", 11f, Color.LTGRAY).apply { maxLines = 2 }
        recordingStatus = label("", 11f, Color.LTGRAY).apply { maxLines = 2 }
        listOf(summary, status, packetAge, sensorsStatus, recordingStatus).forEach { header.addView(it) }
        root.addView(header)
        dashboard = DashboardView(this) { chooseField(it) }
        root.addView(dashboard, LinearLayout.LayoutParams(-1, 0, 1f))
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun toolbarButton(title: String, action: () -> Unit) = button(title, action).apply {
            textSize = 20f; setTextColor(Color.WHITE); minWidth = 0; minHeight = 0
            setPadding(dp(4), 0, dp(4), 0)
            background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(208, 0, 0)); setStroke(1, Color.rgb(70, 0, 0)) }
        }
        startButton = toolbarButton("START") {
            if (runtime.recording) startService(Intent(this, RecordingService::class.java).setAction(RecordingService.STOP)) else startRide()
        }
        toolbar.addView(startButton, LinearLayout.LayoutParams(0, dp(58), 3f))
        toolbar.addView(toolbarButton("☰") { showMenu() }.apply { contentDescription = "Menü öffnen" }, LinearLayout.LayoutParams(0, dp(58), 1f))
        root.addView(toolbar)
        render()
    }
    private fun render() {
        if (!::dashboard.isInitialized) return
        summary.text = runtime.bikeAddress?.let { runtime.bikeName } ?: "Noch kein Fahrrad ausgewählt"
        status.text = runtime.bikeStatus
        packetAge.text = runtime.lastPacketAt?.let { "Letztes Fahrradpaket vor ${(SystemClock.elapsedRealtime() - it) / 1000} s" } ?: "Noch keine Fahrraddaten"
        val gps = runtime.gpsValues
        val gpsStatus = if (runtime.gpsStatus == "GPS · Live" && gps.speedKmh == null) "GPS · Signal unterbrochen" else runtime.gpsStatus
        sensorsStatus.text = "$gpsStatus · ${runtime.heartStatus}"
        recordingStatus.text = if (runtime.recording && runtime.recordingStatus == "Aufzeichnung läuft")
            "● Aufzeichnung · ${runtime.elapsedMs / 60000} min" else runtime.recordingStatus
        startButton.text = if (runtime.recording) "STOPP · SPEICHERN" else if (runtime.saving) "SPEICHERN …" else "START"
        startButton.isEnabled = !runtime.saving || runtime.recording
        dashboard.show(dashboardLayout, runtime.bikeData, gps, runtime.ride.bike.distanceM, runtime.pulseValues)
        if (runtime.visible && (runtime.connection.isActive || runtime.gpsSensor.active || runtime.heart.active)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    /** Starts with three collapsed source categories, builds only the opened source's rows. */
    private fun chooseField(index: Int) {
        picker?.dismiss()
        val root = column()
        val groups = DashboardField.groups(runtime.bikeData, dashboardLayout.fields[index]).groupBy { it.source }
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
                            inner.addView(RadioButton(this).apply {
                                tag = field.id; text = "$title${if (value.unit.isNotEmpty()) " · ${value.unit}" else ""}"
                                textSize = 16f; setTextColor(Color.WHITE); minHeight = dp(48)
                                isChecked = dashboardLayout.fields[index] == field
                                contentDescription = "$source · $text"
                                setOnClickListener {
                                    dashboardLayout = dashboardLayout.replace(index, field); preferences.edit().putString("dashboard", dashboardLayout.encode()).apply()
                                    render(); picker?.dismiss(); if (source == "GPS") enableGps()
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
        picker = AlertDialog.Builder(this).setTitle("Feld ${index + 1} belegen").setView(ScrollView(this).apply { addView(root) }).setNegativeButton("Abbrechen", null).show()
    }
    private fun showMenu() {
        val choices = arrayOf("Sensoren verwalten", "Fahrrad auswählen", "Aufzeichnung einstellen", "Ungespeicherte Fahrten speichern", "GPS-Messung erklärt", "Diagnose speichern", "Standardbelegung herstellen", "Schrift & Lizenz")
        menu = AlertDialog.Builder(this).setTitle("B-Cycle").setItems(choices) { _, item -> when (item) {
            0 -> showSensorManager()
            1 -> selectSensor("bike")
            2 -> recordingSettings()
            3 -> runtime.retryPending()
            4 -> menu = AlertDialog.Builder(this).setTitle("GPS-Messung").setMessage("Während einer gestarteten Aufzeichnung laufen GPS und Sensoren auch bei ausgeschaltetem Display weiter. Außerhalb einer Aufzeichnung misst GPS bei sichtbarer App.\n\nSTART setzt die Fahrtzähler zurück. Nur angehakte Speicherfelder werden in FIT aufgezeichnet. Die FIT-Hauptquellen für Geschwindigkeit und Distanz werden getrennt gewählt und bleiben während der Fahrt fest.\n\nUngenaue Positionen und Sprünge werden verworfen. Höhe wird geglättet, Steigung benötigt etwa 30–40 m Strecke. Höhe und Höhenmeter sind Näherungen. MSL bezeichnet Meereshöhe, Ellipsoid die andere GPS-Höhenreferenz.").setPositiveButton("Schließen", null).show()
            5 -> exportDiagnostic()
            6 -> { dashboardLayout = DashboardLayout.defaults; preferences.edit().putString("dashboard", dashboardLayout.encode()).apply(); render() }
            7 -> menu = AlertDialog.Builder(this).setTitle("Bebas Neue · Dharma Type").setMessage(assets.open("Bebas-Neue-OFL.txt").bufferedReader().use { it.readText() }).setPositiveButton("Schließen", null).show()
        } }.show()
    }
    private fun showSensorManager() {
        sensorManager?.dismiss()
        val root = column(); sensorRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(sensorRows)
        root.addView(button("Sensor hinzufügen") {
            menu = AlertDialog.Builder(this).setTitle("Sensor hinzufügen").setItems(arrayOf("Bosch E-Bike", "BLE-Pulsmesser")) { _, index -> selectSensor(if (index == 0) "bike" else "heart") }.show()
        })
        refreshSensorRows()
        sensorManager = AlertDialog.Builder(this).setTitle("Sensoren").setView(ScrollView(this).apply { addView(root) }).setPositiveButton("Schließen", null).create().apply {
            setOnDismissListener { sensorRows = null }; show()
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
        fun sensor(title: String, state: String, tag: String, enabled: Boolean, toggle: () -> Unit) {
            rows.addView(label(title, 18f).apply { setPadding(0, dp(12), 0, dp(4)) })
            rows.addView(label(state, 13f, Color.LTGRAY).apply { this.tag = tag })
            rows.addView(button(if (enabled) "Trennen" else "Verbinden") { toggle(); rows.removeAllViews(); refreshSensorRows() })
        }
        sensor("GPS", runtime.gpsStatus, "gps_state", runtime.gpsEnabled) { if (runtime.gpsEnabled) runtime.setGpsEnabled(false) else enableGps() }
        sensor("E-Bike · ${if (runtime.bikeAddress == null) "Kein Rad gespeichert" else runtime.bikeName}", runtime.bikeStatus, "bike_state", runtime.bikeEnabled && runtime.bikeAddress != null) {
            if (runtime.bikeAddress == null) selectSensor("bike") else if (!runtime.hasBluetooth()) requestSensorPermission("bikeConnect") else runtime.setBikeEnabled(!runtime.bikeEnabled)
        }
        sensor("Pulsmesser · ${if (runtime.heartAddress == null) "Kein Sensor gespeichert" else runtime.heartName}", runtime.heartStatus, "heart_state", runtime.heartEnabled && runtime.heartAddress != null) {
            if (runtime.heartAddress == null) selectSensor("heart") else if (!runtime.hasBluetooth()) requestSensorPermission("heartConnect") else runtime.setHeartEnabled(!runtime.heartEnabled)
        }
    }
    private fun selectSensor(kind: String) {
        sensorManager?.dismiss()
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
        picker = AlertDialog.Builder(this).setTitle(if (kind == "bike") "Fahrrad auswählen" else "Pulsmesser auswählen")
            .setView(ScrollView(this).apply { addView(root) }).setNegativeButton("Abbrechen", null).create().apply { setOnDismissListener { stopScan() }; show() }
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
            return Spinner(this).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("E-Bike", "GPS"))
                setSelection(if (chosen == bikeId) 0 else 1); root.addView(this)
            }
        }
        val speed = source("FIT-Hauptquelle · Geschwindigkeit", current.speedSource, "SPEED")
        val distance = source("FIT-Hauptquelle · Strecke", current.distanceSource, "BIKE_DISTANCE")
        for ((source, fields) in RecordField.options.groupBy { it.source }) {
            root.addView(label(source, 20f).apply { setPadding(0, dp(18), 0, dp(6)); isAccessibilityHeading = true })
            for (field in fields) root.addView(CheckBox(this).apply {
                text = field.title; tag = "record_${field.id}"; isChecked = field.id in selected; minHeight = dp(48)
                setOnCheckedChangeListener { _, checked -> if (checked) selected.add(field.id) else selected.remove(field.id) }
            })
        }
        menu = AlertDialog.Builder(this).setTitle("Aufzeichnungsmanager").setView(ScrollView(this).apply { addView(root) })
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
            .putExtra(Intent.EXTRA_TITLE, "B-Cycle-0.4.0-diagnose.txt"), 2) }
        catch (_: android.content.ActivityNotFoundException) { pendingReport = null; Toast.makeText(this, R.string.export_unavailable, Toast.LENGTH_LONG).show() }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("pendingReport", pendingReport); super.onSaveInstanceState(outState) }
    @Deprecated("Native document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 2) return
        val report = pendingReport; pendingReport = null
        val uri = data?.data ?: return
        if (resultCode != RESULT_OK || report == null) return
        Thread({
            val ok = try { contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(report) } ?: throw IOException(); true }
            catch (_: IOException) { false } catch (_: SecurityException) { false }
            runOnUiThread { Toast.makeText(applicationContext, if (ok) R.string.export_saved else R.string.export_failed, Toast.LENGTH_LONG).show() }
        }, "B-Cycle-diagnostic-export").start()
    }
    override fun onStart() { super.onStart(); runtime.observers.add(observer); runtime.setVisible(true); render() }
    override fun onStop() {
        picker?.dismiss(); menu?.dismiss(); sensorManager?.dismiss(); stopScan()
        runtime.observers.remove(observer); runtime.setVisible(false); window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); super.onStop()
    }
}
