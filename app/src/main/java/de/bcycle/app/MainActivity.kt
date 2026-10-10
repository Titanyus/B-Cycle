package de.bcycle.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.IOException
import java.time.Instant

class MainActivity : Activity(), BoschConnection.Listener {
    private lateinit var connection: BoschConnection
    private val diagnostics = DiagnosticLog()
    private var pendingReport: String? = null
    private lateinit var status: TextView
    private lateinit var packetAge: TextView
    private lateinit var devices: LinearLayout
    private lateinit var selectedBikeSummary: TextView
    private val seenAddresses = mutableSetOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private var lastPacketAt: Long? = null
    private val reconnect = AutoReconnect()
    private lateinit var dashboard: DashboardView
    private lateinit var connectionButton: Button
    private var dashboardLayout = DashboardLayout.defaults
    private var liveData = LiveData()
    private lateinit var ride: RideSession
    private lateinit var gpsSensor: GpsSensor
    private lateinit var gpsStatusView: TextView
    private var gpsEnabled = false
    private var gpsMessage = "GPS · Aus"
    private var deviceDialog: AlertDialog? = null
    private var fieldDialog: AlertDialog? = null
    private var menuDialog: AlertDialog? = null
    private var scanStatus: TextView? = null
    private var selectedAddress: String? = null
    private var selectedName = "E-Bike"
    private var permissionAsked = false
    private val preferences by lazy { getSharedPreferences("bike", MODE_PRIVATE) }
    private val permissions = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    private val tick = object : Runnable {
        override fun run() {
            packetAge.text = lastPacketAt?.let {
                "Letztes Datenpaket vor ${(SystemClock.elapsedRealtime() - it) / 1000} s"
            } ?: getString(R.string.no_data)
            refreshDashboard()
            tryAutoConnect()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingReport = savedInstanceState?.getString("pendingReport")
        selectedAddress = preferences.getString("address", null)?.takeIf { android.bluetooth.BluetoothAdapter.checkBluetoothAddress(it) }
        selectedName = preferences.getString("name", "E-Bike") ?: "E-Bike"
        reconnect.enabled = savedInstanceState?.getBoolean("autoConnect", true) ?: true
        reconnect.exporting = pendingReport != null
        reconnect.selecting = savedInstanceState?.getBoolean("selecting", false) ?: false
        permissionAsked = savedInstanceState?.getBoolean("permissionAsked", false) ?: false
        dashboardLayout = DashboardLayout.decode(preferences.getString("dashboard", null))
        ride = lastNonConfigurationInstance as? RideSession ?: RideSession()
        gpsEnabled = preferences.getBoolean("gpsEnabled", false)
        gpsSensor = GpsSensor(this, ride.gps) { message ->
            gpsMessage = message
            refreshDashboard()
            updateScreenAwake()
        }
        diagnostics.reset(diagnosticHeader())
        connection = BoschConnection(this, this, diagnostics)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        column.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(column)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(6))
        }
        selectedBikeSummary = label("", 12f)
        selectedBikeSummary.isSingleLine = true
        updateBikeSummary()
        status = label("Nicht verbunden", 12f, Color.rgb(170, 170, 170)).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            maxLines = 2
        }
        packetAge = label(getString(R.string.no_data), 11f, Color.rgb(119, 119, 119))
        header.addView(selectedBikeSummary)
        header.addView(status)
        header.addView(packetAge)
        gpsStatusView = label(gpsMessage, 11f, Color.rgb(163, 163, 163))
        header.addView(gpsStatusView)
        column.addView(header)
        dashboard = DashboardView(this) { index -> chooseField(index) }
        dashboard.show(dashboardLayout, liveData)
        column.addView(dashboard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        connectionButton = toolbarButton("VERBINDEN", Color.rgb(208, 0, 0)) {
            if (connection.isActive || (reconnect.enabled && selectedAddress != null)) {
                reconnect.enabled = false; reconnect.selecting = false
                deviceDialog?.dismiss(); stopConnection()
            } else {
                reconnect.start()
                if (selectedAddress == null) selectBike() else ensurePermissions()
                tryAutoConnect()
            }
        }
        toolbar.addView(connectionButton, LinearLayout.LayoutParams(0, dp(58), 2f))
        toolbar.addView(toolbarButton("RAD", Color.rgb(158, 0, 0)) { selectBike() }, LinearLayout.LayoutParams(0, dp(58), 1f))
        toolbar.addView(toolbarButton("MENÜ", Color.rgb(158, 0, 0)) { showMenu() }, LinearLayout.LayoutParams(0, dp(58), 1f))
        column.addView(toolbar)
        devices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    }

    private fun toolbarButton(title: String, color: Int, action: () -> Unit) = Button(this).apply {
        text = title; textSize = 16f
        setTextColor(Color.WHITE)
        setPadding(dp(4), 0, dp(4), 0)
        minWidth = 0; minHeight = 0
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(color); setStroke(1, Color.rgb(70, 0, 0))
        }
        setOnClickListener { action() }
    }

    private fun chooseField(index: Int) {
        val selected = dashboardLayout.fields[index]
        fieldDialog?.dismiss()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val gps = ride.gps.snapshot(SystemClock.elapsedRealtime())
        var source: String? = null
        for (group in DashboardField.groups(liveData, selected)) {
            if (group.source != source) {
                source = group.source
                content.addView(label(source, 22f).apply {
                    setPadding(0, dp(18), 0, dp(4)); isAccessibilityHeading = true
                })
            }
            content.addView(label(group.title, 14f, Color.rgb(190, 190, 190)).apply {
                setPadding(0, dp(12), 0, dp(4)); isAccessibilityHeading = true
            })
            for (field in group.fields) {
                val value = field.value(liveData, gps, ride.bike.distanceM)
                val title = if (field.id == "RIDER_POWER") "Fahrerleistung (eigene)" else field.title(liveData, gps)
                content.addView(RadioButton(this).apply {
                    tag = field.id
                    text = "$title${if (value.unit.isNotEmpty()) " · ${value.unit}" else ""}"
                    textSize = 16f; setTextColor(Color.WHITE)
                    isChecked = field == selected
                    minHeight = dp(48)
                    contentDescription = "${field.source} · $text"
                    setOnClickListener {
                        dashboardLayout = dashboardLayout.replace(index, field)
                        preferences.edit().putString("dashboard", dashboardLayout.encode()).apply()
                        refreshDashboard()
                        fieldDialog?.dismiss()
                        if (field.source == "GPS") enableGps()
                    }
                })
            }
        }
        fieldDialog = AlertDialog.Builder(this)
            .setTitle("Feld ${index + 1} belegen${if (index == 2) " · großes Feld" else ""}")
            .setView(ScrollView(this).apply { addView(content) })
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun showMenu() {
        val gpsAction = if (gpsEnabled && hasGpsPermission()) "GPS pausieren" else "GPS aktivieren"
        menuDialog = AlertDialog.Builder(this).setTitle("B-Cycle")
            .setItems(arrayOf(gpsAction, "Neue Fahrt · Zähler zurücksetzen", "GPS-Messung erklärt", "Werte erneut lesen", "Diagnose speichern", "Standardbelegung herstellen", "Schrift & Lizenz")) { _, item ->
                when (item) {
                    0 -> if (gpsEnabled && hasGpsPermission()) {
                        gpsEnabled = false; preferences.edit().putBoolean("gpsEnabled", false).apply()
                        gpsSensor.stop(); gpsMessage = "GPS · Aus"; refreshDashboard(); updateScreenAwake()
                    } else enableGps()
                    1 -> {
                        ride.gps.reset()
                        ride.bike.reset(if (connection.isLive) liveData.value(LiveMetric.ODOMETER) else null)
                        gpsMessage = if (gpsSensor.active) "GPS · Suche Position" else "GPS · Aus"
                        refreshDashboard()
                        Toast.makeText(this, "GPS- und E-Bike-Fahrtzähler zurückgesetzt", Toast.LENGTH_SHORT).show()
                    }
                    2 -> menuDialog = AlertDialog.Builder(this).setTitle("GPS-Messung")
                        .setMessage("GPS misst bei sichtbarer App; das Display bleibt an. Beim Verlassen pausiert die Messung. Neue Fahrt setzt beide Strecken und alle GPS-Zähler zurück. Nach vollständigem Beenden beginnt eine neue Fahrt.\n\nBewegungsdurchschnitt zählt nur Bewegungszeit; Gesamtdurchschnitt enthält auch Pausen und Wartezeiten nach der ersten Position. Standzeit zählt nur bestätigten Stillstand. Empfangslücken werden nicht als gefahrene Strecke ergänzt.\n\nUngenaue Positionen und Sprünge werden verworfen. Bewegung unter etwa 2,5 km/h wird als Stillstand behandelt. Höhe wird geglättet, Höhenmeter zählen ab 3 m Änderung. Steigung braucht etwa 30–40 m Strecke und reagiert verzögert.\n\nFalls das Handy Meereshöhe (MSL) liefert, wird diese genutzt; sonst GPS-Ellipsoidhöhe. Höhe, Höhenmeter und Steigung sind Näherungen. Es wird noch keine Route gespeichert.")
                        .setPositiveButton("Schließen", null).show()
                    3 -> if (connection.canReread) connection.reread() else Toast.makeText(this, "Aktuell keine erneute Datenanfrage möglich", Toast.LENGTH_SHORT).show()
                    4 -> exportDiagnostic()
                    5 -> {
                        dashboardLayout = DashboardLayout.defaults
                        preferences.edit().putString("dashboard", dashboardLayout.encode()).apply()
                        refreshDashboard()
                    }
                    6 -> {
                        val license = assets.open("Bebas-Neue-OFL.txt").bufferedReader().use { it.readText() }
                        menuDialog = AlertDialog.Builder(this).setTitle("Bebas Neue · Dharma Type")
                            .setMessage(license).setPositiveButton("Schließen", null).show()
                    }
                }
            }.show()
    }

    private fun hasPermissions() = permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    private fun hasGpsPermission() = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private fun enableGps() {
        gpsEnabled = true
        preferences.edit().putBoolean("gpsEnabled", true).apply()
        if (hasGpsPermission()) {
            if (reconnect.foreground) gpsSensor.start()
        } else {
            gpsMessage = "GPS · Präzisen Standort erlauben"; refreshDashboard()
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 3)
        }
    }

    private fun refreshDashboard() {
        val gps = ride.gps.snapshot(SystemClock.elapsedRealtime())
        dashboard.show(dashboardLayout, liveData, gps, ride.bike.distanceM)
        gpsStatusView.text = if (gpsMessage == "GPS · Live" && gps.speedKmh == null) "GPS · Signal unterbrochen" else gpsMessage
    }

    private fun updateScreenAwake() {
        if (reconnect.foreground && (connection.isActive || gpsSensor.active)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private fun updateBikeSummary() {
        selectedBikeSummary.text = if (selectedAddress == null) getString(R.string.no_bike_selected) else
            getString(R.string.selected_bike_name, selectedName)
    }

    private fun ensurePermissions() {
        if (!hasPermissions()) {
            permissionAsked = true
            requestPermissions(permissions, 1)
        }
    }

    private fun selectBike() {
        reconnect.selecting = true
        if (hasPermissions()) startSelection() else ensurePermissions()
    }

    private fun startSelection() {
        diagnostics.reset(diagnosticHeader())
        seenAddresses.clear()
        devices.removeAllViews()
        (devices.parent as? android.view.ViewGroup)?.removeView(devices)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // One native picker keeps the riding screen limited to the seven chosen fields.
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        scanStatus = label("Suche nach BLE-Geräten", 14f)
        content.addView(scanStatus)
        content.addView(devices)
        val scroll = ScrollView(this).apply { addView(content) }
        val dialog = AlertDialog.Builder(this).setTitle("Fahrrad auswählen")
            .setView(scroll).setNegativeButton("Abbrechen", null).create()
        deviceDialog?.setOnDismissListener(null)
        deviceDialog?.dismiss()
        deviceDialog = dialog
        dialog.setOnDismissListener {
            if (deviceDialog === dialog) {
                deviceDialog = null; scanStatus = null
                (devices.parent as? android.view.ViewGroup)?.removeView(devices)
                if (reconnect.selecting) {
                    reconnect.selecting = false
                    connection.stop()
                    tryAutoConnect()
                }
            }
        }
        dialog.show()
        connection.discover()
    }

    private fun tryAutoConnect() {
        val address = selectedAddress ?: return
        if (!hasPermissions() || !reconnect.due(SystemClock.elapsedRealtime(), connection.isActive)) return
        reconnect.attempted(SystemClock.elapsedRealtime())
        diagnostics.reset(diagnosticHeader())
        try {
            val manager = getSystemService(android.bluetooth.BluetoothManager::class.java)
            val adapter = manager.adapter
            if (adapter == null || !adapter.isEnabled) { onStatus("Bitte Bluetooth einschalten"); return }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            connection.connect(adapter.getRemoteDevice(address))
        } catch (_: SecurityException) { onStatus("Bluetooth-Zugriff ist für die Verbindung erforderlich") }
        catch (_: IllegalStateException) { onStatus("Bluetooth ist nicht verfügbar") }
    }

    private fun diagnosticHeader(): String {
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        return "App=$version device=${Build.MANUFACTURER} ${Build.MODEL} Android=${Build.VERSION.RELEASE} API=${Build.VERSION.SDK_INT}\n" +
            "Session UTC=${Instant.now()}\nMode=MCSP"
    }

    private fun exportDiagnostic() {
        diagnostics.event("EXPORT requested; leaving foreground will stop the BLE session")
        val gps = ride.gps.snapshot(SystemClock.elapsedRealtime())
        pendingReport = diagnostics.report() + "\nGPS foreground: enabled=$gpsEnabled accepted=${ride.gps.accepted} rejected=${ride.gps.rejected}\n" +
            GpsMetric.entries.joinToString("\n") { "${it.name}=${it.value(gps).text} ${it.unit}" } +
            "\nBIKE_DISTANCE=${ride.bike.distanceM ?: "unavailable"} m\n"
        reconnect.exporting = true
        stopConnection()
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            val version = packageManager.getPackageInfo(packageName, 0).versionName
            putExtra(Intent.EXTRA_TITLE, "B-Cycle-$version-diagnose.txt")
        }
        try { startActivityForResult(intent, 2) }
        catch (_: android.content.ActivityNotFoundException) {
            pendingReport = null
            reconnect.exporting = false
            Toast.makeText(this, R.string.export_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pendingReport", pendingReport)
        outState.putBoolean("autoConnect", reconnect.enabled)
        outState.putBoolean("selecting", reconnect.selecting)
        outState.putBoolean("permissionAsked", permissionAsked)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Platform document picker callback; no AndroidX dependency is needed")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 2) return
        val report = pendingReport
        pendingReport = null
        reconnect.exporting = false
        val uri = data?.data
        if (resultCode != RESULT_OK || report == null || uri == null) return
        Thread({
            val success = try {
                val stream = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(report) }
                true
            } catch (_: IOException) { false }
            catch (_: SecurityException) { false }
            runOnUiThread {
                Toast.makeText(applicationContext, if (success) R.string.export_saved else R.string.export_failed, Toast.LENGTH_LONG).show()
            }
        }, "B-Cycle-diagnostic-export").start()
    }
    private fun stopConnection() {
        connection.stop()
        updateScreenAwake()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (hasPermissions()) {
                if (reconnect.selecting) startSelection() else tryAutoConnect()
            } else { reconnect.selecting = false; onStatus("Bluetooth-Zugriff ist für die Verbindung erforderlich") }
        }
        if (requestCode == 3) {
            if (hasGpsPermission() && gpsEnabled && reconnect.foreground) gpsSensor.start()
            else { gpsMessage = "GPS · Präzisen Standort erlauben"; refreshDashboard() }
        }
    }
    override fun onStatus(message: String) {
        status.text = message
        scanStatus?.text = message
        connectionButton.text = if (connection.isActive || (reconnect.enabled && selectedAddress != null)) "STOPPEN" else "VERBINDEN"
        if (connection.isLive) reconnect.connected()
        updateScreenAwake()
    }
    override fun onDevice(device: BluetoothDevice, name: String) {
        val address = try { device.address } catch (_: SecurityException) { return }
        if (!seenAddresses.add(address)) return
        devices.addView(Button(this).apply {
            text = getString(R.string.device_name_address, name, address)
            isAllCaps = false
            setOnClickListener {
                if (hasPermissions()) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    selectedAddress = address
                    if (preferences.getString("address", null) != address) ride.bike.reset()
                    selectedName = name
                    preferences.edit().putString("address", address).putString("name", name).apply()
                    updateBikeSummary()
                    connection.stop()
                    devices.removeAllViews()
                    reconnect.start()
                    deviceDialog?.dismiss()
                    tryAutoConnect()
                } else onStatus("Bitte erneut suchen und Bluetooth-Zugriff erlauben")
            }
        })
    }
    override fun onData(data: LiveData, packetReceived: Boolean) {
        liveData = data
        if (packetReceived) ride.bike.update(data.value(LiveMetric.ODOMETER)) else ride.bike.pause()
        refreshDashboard()
        if (packetReceived) lastPacketAt = SystemClock.elapsedRealtime()
        else { lastPacketAt = null; packetAge.setText(R.string.no_data) }
    }
    override fun onStart() {
        super.onStart()
        reconnect.foreground = true
        connection.onForegroundChanged(true)
        if (selectedAddress != null && !hasPermissions() && !permissionAsked) ensurePermissions()
        if (reconnect.selecting && hasPermissions()) startSelection()
        tryAutoConnect()
        if (gpsEnabled) gpsSensor.start()
        updateScreenAwake()
        handler.post(tick)
    }
    override fun onStop() {
        reconnect.foreground = false
        ride.gps.snapshot(SystemClock.elapsedRealtime())
        gpsSensor.stop(); ride.bike.pause()
        deviceDialog?.dismiss(); fieldDialog?.dismiss(); menuDialog?.dismiss()
        devices.removeAllViews()
        connection.onForegroundChanged(false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handler.removeCallbacks(tick)
        super.onStop()
    }
    override fun onDestroy() { connection.close(); super.onDestroy() }
    @Deprecated("Retain the small in-memory trip counters across screen rotation without a UI dependency")
    override fun onRetainNonConfigurationInstance(): Any = ride
    private fun label(value: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
