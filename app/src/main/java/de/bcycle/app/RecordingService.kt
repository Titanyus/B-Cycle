package de.bcycle.app

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*

/** Exists only between explicit START and STOPP. Owns foreground priority and CPU wakefulness. */
class RecordingService : Service() {
    private lateinit var runtime: RideRuntime
    private lateinit var wakeLock: PowerManager.WakeLock
    private val handler = Handler(Looper.getMainLooper())
    private val observer: () -> Unit = {
        if (!runtime.recording && !runtime.saving) {
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
        }
    }
    private val refresh = object : Runnable {
        override fun run() {
            if (!runtime.recording && !runtime.saving) return
            wakeLock.acquire(60 * 60 * 1000L)
            getSystemService(NotificationManager::class.java).notify(1, notification())
            handler.postDelayed(this, 30_000)
        }
    }
    override fun onCreate() {
        super.onCreate(); runtime = RideRuntime.get(this)
        runtime.observers.add(observer)
        wakeLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "B-Cycle:Recording").apply { setReferenceCounted(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("ride", "Fahrtaufzeichnung", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            runtime.stopRecording()
            if (!runtime.saving) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
            return START_NOT_STICKY
        }
        if (!runtime.hasBluetooth()) { stopSelf(); return START_NOT_STICKY }
        var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        if (runtime.hasGps() && runtime.gpsEnabled) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        try {
            startForeground(1, notification(), types)
            runtime.beginRecording(); handler.removeCallbacks(refresh); handler.post(refresh)
        } catch (_: SecurityException) { runtime.stopRecording(); stopSelf() }
        return START_NOT_STICKY
    }
    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, RecordingService::class.java).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val seconds = runtime.elapsedMs / 1000
        return Notification.Builder(this, "ride").setSmallIcon(R.drawable.ic_bike).setContentTitle(if (runtime.saving) "B-Cycle · FIT speichern" else "B-Cycle · Aufzeichnung")
            .setContentText(if (runtime.saving) runtime.recordingStatus else "${seconds / 60} min · ${runtime.gpsStatus}").setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, "STOPP · FIT speichern", stop).build()).build()
    }
    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        runtime.observers.remove(observer)
        if (runtime.recording) runtime.stopRecording()
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
    companion object { const val START = "de.bcycle.app.START_RIDE"; const val STOP = "de.bcycle.app.STOP_RIDE" }
}
