package de.bcycle.app

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
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
import java.util.Locale

class MainActivity : Activity(), BoschConnection.Listener {
    private lateinit var connection: BoschConnection
    private lateinit var status: TextView
    private lateinit var speed: TextView
    private lateinit var power: TextView
    private lateinit var packetAge: TextView
    private lateinit var devices: LinearLayout
    private val seenAddresses = mutableSetOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private var lastPacketAt: Long? = null
    private val permissions = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
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
        connection = BoschConnection(this, this)
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
        column.addView(Button(this).apply {
            setText(R.string.find_bike)
            setOnClickListener {
                if (hasPermissions()) startConnection() else requestPermissions(permissions, 1)
            }
        })
        column.addView(Button(this).apply {
            setText(R.string.stop)
            setOnClickListener { stopConnection() }
        })
        column.addView(label(
            "Fahrrad mit Flow entsperren, dann hier suchen und auswählen. Die Liste enthält auch andere BLE-Geräte. Android kann eine Kopplungsbestätigung anzeigen.\n\n" +
                "Experimenteller Test: Smartphone-Unterstützung ist von Bosch nicht zugesichert. Die App arbeitet nur im Vordergrund.",
            14f,
        ).apply { setPadding(0, dp(12), 0, 0) })
        devices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(devices)
    }

    private fun hasPermissions() = permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    private fun startConnection() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        seenAddresses.clear()
        devices.removeAllViews()
        connection.discover()
    }
    private fun stopConnection() {
        connection.stop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (hasPermissions()) startConnection() else onStatus("Bluetooth-Zugriff ist für die Verbindung erforderlich")
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
                    connection.connect(device)
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
    override fun onStart() { super.onStart(); handler.post(tick) }
    override fun onStop() {
        stopConnection()
        handler.removeCallbacks(tick)
        super.onStop()
    }
    override fun onDestroy() { connection.close(); super.onDestroy() }
    private fun label(value: String, size: Float, color: Int = Color.rgb(230, 237, 232)) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
