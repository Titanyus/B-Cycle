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
    private enum class Action { SELECT, ACCESSORY, DIRECT }
    private lateinit var connection: BoschConnection
    private val diagnostics = DiagnosticLog()
    private var pendingReport: String? = null
    private lateinit var status: TextView
    private lateinit var speed: TextView
    private lateinit var power: TextView
    private lateinit var packetAge: TextView
    private lateinit var devices: LinearLayout
    private lateinit var selectedBikeSummary: TextView
    private val seenAddresses = mutableSetOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private var lastPacketAt: Long? = null
    private var nextAction = Action.SELECT
    private var selectedBike: BluetoothDevice? = null
    private val permissions = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
    private val tick = object : Runnable {
        override fun run() {
            packetAge.text = lastPacketAt?.let {
                "Letztes Datenpaket vor ${(SystemClock.elapsedRealtime() - it) / 1000} s"
            } ?: getString(R.string.no_data)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingReport = savedInstanceState?.getString("pendingReport")
        nextAction = Action.entries.getOrNull(savedInstanceState?.getInt("nextAction") ?: 0) ?: Action.SELECT
        selectedBike = savedInstanceState?.getParcelable("selectedBike", BluetoothDevice::class.java)
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
        column.addView(label("B-CYCLE · BOSCH-TEST", 18f, Color.rgb(167, 235, 117)))
        column.addView(label("Geschwindigkeit · km/h", 18f).apply { setPadding(0, dp(24), 0, 0) })
        speed = label("—", 76f).apply { gravity = Gravity.CENTER }
        column.addView(speed)
        column.addView(label("Fahrerleistung · W", 18f))
        power = label("—", 64f).apply { gravity = Gravity.CENTER }
        column.addView(power)
        status = label("Nicht verbunden", 16f).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setPadding(0, dp(12), 0, dp(8))
        }
        packetAge = label(getString(R.string.no_data), 14f)
        column.addView(status)
        column.addView(packetAge)
        selectedBikeSummary = label(getString(if (selectedBike == null) R.string.no_bike_selected else R.string.bike_selected), 14f)
        column.addView(selectedBikeSummary)
        column.addView(Button(this).apply {
            setText(R.string.find_bike)
            setOnClickListener { requestAction(Action.SELECT) }
        })
        column.addView(Button(this).apply {
            setText(R.string.offer_accessory)
            setOnClickListener { requestAction(Action.ACCESSORY) }
        })
        column.addView(Button(this).apply {
            setText(R.string.stop)
            setOnClickListener { stopConnection() }
        })
        column.addView(Button(this).apply {
            setText(R.string.reread_data)
            setOnClickListener { connection.reread() }
        })
        column.addView(Button(this).apply {
            setText(R.string.save_diagnostic)
            setOnClickListener { exportDiagnostic() }
        })
        column.addView(Button(this).apply {
            setText(R.string.connect_direct)
            setOnClickListener { requestAction(Action.DIRECT) }
        })
        column.addView(label(
            "„Fahrrad auswählen“ öffnet eine Geräteliste und baut keine Verbindung auf. Nach der Auswahl „Als Zubehör anbieten“ antippen. B-Cycle trennt eine eigene frühere Verbindung automatisch und wartet danach auf eine neue Verbindung vom Rad. Ein Hinweis auf einen bestehenden Link sagt nicht, welche App ihn hält. Falls nötig, die Zubehörsuche am Rad bzw. in Flow starten.\n\n" +
                "Experimenteller Test: Smartphone-Unterstützung ist von Bosch nicht zugesichert. Die Live-Anzeige läuft im Vordergrund; der Zubehörtest darf bis zu zwei Minuten im Hintergrund bleiben. In Flow erscheint gegebenenfalls der Bluetooth-Gerätename des Handys, nicht „B-Cycle“.",
            14f,
        ).apply { setPadding(0, dp(12), 0, 0) })
        devices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(devices)
    }

    private fun hasPermissions() = permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    private fun requestAction(action: Action) {
        nextAction = action
        if (hasPermissions()) runRequestedAction() else requestPermissions(permissions, 1)
    }
    private fun runRequestedAction() {
        val bike = selectedBike
        if (nextAction == Action.SELECT || bike == null) { startSelection(); return }
        diagnostics.reset(diagnosticHeader())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (nextAction == Action.ACCESSORY) connection.offerAccessory(bike) else connection.connect(bike)
    }
    private fun startSelection() {
        diagnostics.reset(diagnosticHeader())
        selectedBike = null
        selectedBikeSummary.setText(R.string.no_bike_selected)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        seenAddresses.clear()
        devices.removeAllViews()
        connection.discover()
    }

    private fun diagnosticHeader(): String {
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        return "App=$version device=${Build.MANUFACTURER} ${Build.MODEL} Android=${Build.VERSION.RELEASE} API=${Build.VERSION.SDK_INT}\n" +
            "Session UTC=${Instant.now()}\nRequested mode=$nextAction"
    }

    private fun exportDiagnostic() {
        diagnostics.event("EXPORT requested; leaving foreground will stop the BLE session")
        pendingReport = diagnostics.report()
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
            Toast.makeText(this, R.string.export_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pendingReport", pendingReport)
        outState.putInt("nextAction", nextAction.ordinal)
        outState.putParcelable("selectedBike", selectedBike)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Platform document picker callback; no AndroidX dependency is needed")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 2) return
        val report = pendingReport
        pendingReport = null
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
            if (hasPermissions()) runRequestedAction() else onStatus("Bluetooth-Zugriff ist für die Verbindung erforderlich")
        }
    }
    override fun onStatus(message: String) { status.text = message }
    override fun onDevice(device: BluetoothDevice, name: String) {
        val address = try { device.address } catch (_: SecurityException) { return }
        if (!seenAddresses.add(address)) return
        devices.addView(Button(this).apply {
            text = getString(R.string.device_name_address, name, address)
            isAllCaps = false
            setOnClickListener {
                if (hasPermissions()) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    selectedBike = device
                    selectedBikeSummary.text = getString(R.string.selected_bike_name, name)
                    connection.stop()
                    devices.removeAllViews()
                    if (nextAction == Action.SELECT) onStatus(getString(R.string.bike_selected)) else runRequestedAction()
                } else onStatus("Bitte erneut suchen und Bluetooth-Zugriff erlauben")
            }
        })
    }
    override fun onData(data: LiveData, packetReceived: Boolean) {
        speed.text = data.speedHundredths?.let { String.format(Locale.GERMANY, "%.1f", it / 100.0) } ?: "—"
        power.text = data.riderPowerWatts?.toString() ?: "—"
        if (packetReceived) lastPacketAt = SystemClock.elapsedRealtime()
        else { lastPacketAt = null; packetAge.setText(R.string.no_data) }
    }
    override fun onStart() {
        super.onStart()
        connection.onForegroundChanged(true)
        if (connection.isActive) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handler.post(tick)
    }
    override fun onStop() {
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
