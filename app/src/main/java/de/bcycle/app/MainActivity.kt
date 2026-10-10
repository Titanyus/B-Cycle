package de.bcycle.app

import android.Manifest
import android.app.Activity
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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.IOException
import java.time.Instant
import java.util.Locale

class MainActivity : Activity(), BoschConnection.Listener {
    private lateinit var connection: BoschConnection
    private val diagnostics = DiagnosticLog()
    private var pendingReport: String? = null
    private lateinit var status: TextView
    private lateinit var speed: TextView
    private lateinit var power: TextView
    private lateinit var packetAge: TextView
    private lateinit var devices: LinearLayout
    private lateinit var rereadButton: Button
    private lateinit var selectedBikeSummary: TextView
    private val seenAddresses = mutableSetOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private var lastPacketAt: Long? = null
    private val reconnect = AutoReconnect()
    private val metricViews = linkedMapOf<LiveMetric, TextView>()
    private lateinit var ranges: TextView
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
        diagnostics.reset(diagnosticHeader())
        connection = BoschConnection(this, this, diagnostics)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
            setBackgroundColor(Color.rgb(16, 22, 19))
        }
        val scroll = ScrollView(this).apply { addView(column) }
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(scroll)
        column.addView(label("B-CYCLE", 18f, Color.rgb(167, 235, 117)))
        status = label("Nicht verbunden", 16f).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setPadding(0, dp(20), 0, dp(8))
        }
        packetAge = label(getString(R.string.no_data), 14f)
        column.addView(status)
        column.addView(packetAge)
        selectedBikeSummary = label("", 14f)
        updateBikeSummary()
        column.addView(selectedBikeSummary)
        column.addView(label("Geschwindigkeit · km/h", 18f).apply { setPadding(0, dp(24), 0, 0) })
        speed = label("—", 76f).apply { gravity = Gravity.CENTER }
        column.addView(speed)
        column.addView(label("Fahrerleistung · W", 18f))
        power = label("—", 64f).apply { gravity = Gravity.CENTER }
        column.addView(power)
        for (group in 0..2) {
            column.addView(label(listOf("Fahrt", "Fahrrad & Akku", "Bosch-Aktivität")[group], 18f,
                Color.rgb(167, 235, 117)).apply { setPadding(0, dp(20), 0, dp(8)) })
            for (metric in LiveMetric.entries.filter { it.group == group && it != LiveMetric.SPEED && it != LiveMetric.RIDER_POWER }) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, dp(6)) }
                row.addView(label(metric.title, 16f), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                val value = label("—", 20f).apply { gravity = Gravity.END; setPadding(dp(12), 0, 0, 0) }
                row.addView(value, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                metricViews[metric] = value
                column.addView(row)
            }
            if (group == 1) {
                column.addView(label("Reichweite je Unterstützungsmodus", 16f).apply { setPadding(0, dp(8), 0, 0) })
                ranges = label("—", 18f)
                column.addView(ranges)
            }
        }
        column.addView(Button(this).apply {
            setText(R.string.find_bike)
            setOnClickListener { selectBike() }
        })
        column.addView(Button(this).apply {
            setText(R.string.start_live)
            setOnClickListener {
                reconnect.start()
                devices.removeAllViews()
                if (!connection.isLive) connection.stop()
                if (selectedAddress == null) selectBike() else ensurePermissions()
                tryAutoConnect()
            }
        })
        column.addView(Button(this).apply {
            setText(R.string.stop)
            setOnClickListener {
                reconnect.enabled = false; reconnect.selecting = false
                devices.removeAllViews(); stopConnection()
            }
        })
        rereadButton = Button(this).apply {
            setText(R.string.reread_data)
            isEnabled = false
            setOnClickListener { connection.reread() }
        }
        column.addView(rereadButton)
        column.addView(Button(this).apply {
            setText(R.string.save_diagnostic)
            setOnClickListener { exportDiagnostic() }
        })
        column.addView(label(
            "Fahrrad mit Flow entsperren. Das ausgewählte E-Bike wird gespeichert und beim Öffnen automatisch verbunden. Flow darf weiterlaufen.\n\n" +
                "Die Live-Anzeige läuft im Vordergrund. Nicht gelieferte Werte bleiben als — sichtbar. Aktivitätswerte stammen vom Fahrrad und können seltener aktualisiert werden.",
            14f,
        ).apply { setPadding(0, dp(12), 0, 0) })
        devices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(devices)
    }

    private fun hasPermissions() = permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        pendingReport = diagnostics.report()
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
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (hasPermissions()) {
                if (reconnect.selecting) startSelection() else tryAutoConnect()
            } else { reconnect.selecting = false; onStatus("Bluetooth-Zugriff ist für die Verbindung erforderlich") }
        }
    }
    override fun onStatus(message: String) {
        status.text = message
        rereadButton.isEnabled = connection.canReread
        if (connection.isLive) reconnect.connected()
        if (!connection.isActive) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                    selectedName = name
                    preferences.edit().putString("address", address).putString("name", name).apply()
                    updateBikeSummary()
                    connection.stop()
                    devices.removeAllViews()
                    reconnect.start()
                    tryAutoConnect()
                } else onStatus("Bitte erneut suchen und Bluetooth-Zugriff erlauben")
            }
        })
    }
    override fun onData(data: LiveData, packetReceived: Boolean) {
        speed.text = data.speedHundredths?.let { String.format(Locale.GERMANY, "%.1f", it / 100.0) } ?: "—"
        power.text = data.riderPowerWatts?.toString() ?: "—"
        for ((metric, view) in metricViews) view.text = data.display(metric)
        ranges.text = data.displayRanges()
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
        if (connection.isActive) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handler.post(tick)
    }
    override fun onStop() {
        reconnect.foreground = false
        devices.removeAllViews()
        connection.onForegroundChanged(false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handler.removeCallbacks(tick)
        super.onStop()
    }
    override fun onDestroy() { connection.close(); super.onDestroy() }
    private fun label(value: String, size: Float, color: Int = Color.rgb(230, 237, 232)) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
