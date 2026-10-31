package de.bcycle.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import java.io.InterruptedIOException
import java.util.concurrent.atomic.AtomicBoolean

/** A lease spans actual backup IO, never a password prompt or document picker. */
internal object BackupWork {
    private val lock=Any()
    private val leases=linkedSetOf<Lease>()
    private var idle:(()->Unit)?=null
    internal fun connect(callback:()->Unit)=synchronized(lock) { idle=callback }
    internal fun disconnect(callback:()->Unit)=synchronized(lock) { if(idle===callback) idle=null }
    internal val active get()=synchronized(lock) { leases.isNotEmpty() }
    fun begin(context:Context):Lease=synchronized(lock) {
        val app=context.applicationContext
        val lease=Lease();leases.add(lease)
        try { app.startForegroundService(Intent(app,BackupWorkService::class.java)) }
        catch(error:Exception) { leases.remove(lease);throw error }
        lease
    }
    internal fun interruptAll()=synchronized(lock) { leases.forEach { it.interrupt() } }
    fun start(context:Context,name:String,failed:(Exception)->Unit={ BicycleFeedback.show(context,R.string.backup_background_failed,true) },work:()->Unit) {
        val lease=try { begin(context) } catch(error:Exception) { failed(error);return }
        try { Thread({ lease.use {
            try { it.enter();work() }
            catch(error:Exception) { Handler(Looper.getMainLooper()).post { failed(error) } }
        } },name).start() }
        catch(error:Exception) { lease.close();failed(error) }
    }
    class Lease internal constructor():AutoCloseable {
        private val closed=AtomicBoolean(false)
        private var interrupted=false
        private var thread:Thread?=null
        fun enter()=synchronized(lock) {
            if(interrupted || closed.get()) throw InterruptedIOException()
            thread=Thread.currentThread()
        }
        internal fun interrupt() { interrupted=true;thread?.interrupt() }
        override fun close() {
            if(!closed.compareAndSet(false,true)) return
            synchronized(lock) {
                thread=null;leases.remove(this)
                if(leases.isEmpty()) idle?.invoke()
            }
        }
    }
}

/** Foreground data-sync priority and CPU wakefulness while user-requested backup work runs. */
class BackupWorkService:Service() {
    private lateinit var wake:PowerManager.WakeLock
    private val handler=Handler(Looper.getMainLooper())
    private var normalStop=false
    private val stopIfIdle=Runnable {
        if(!BackupWork.active) { normalStop=true;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf() }
    }
    private val idleCallback:()->Unit={ handler.post(stopIfIdle);Unit }
    private val renew=object:Runnable {
        override fun run() {
            if(!BackupWork.active) return
            wake.acquire(60*60*1000L)
            handler.postDelayed(this,30_000)
        }
    }
    override fun onCreate() {
        super.onCreate()
        BackupWork.connect(idleCallback)
        wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"B-Cycle:Backup").apply { setReferenceCounted(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("backups",getString(R.string.backup_background_channel),NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        val open=PendingIntent.getActivity(this,83,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(this,"backups").setSmallIcon(R.drawable.ic_bike)
            .setContentTitle(getString(R.string.backup_background_title)).setContentText(getString(R.string.backup_processing))
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setProgress(0,0,true).build()
        try {
            startForeground(83,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            if(BackupWork.active) { normalStop=false;handler.removeCallbacks(renew);renew.run() }
            else stopIfIdle.run()
        } catch(error:Exception) { BackupWork.interruptAll();stopSelf() }
        return START_NOT_STICKY
    }
    override fun onTimeout(startId:Int,fgsType:Int) {
        BackupWork.interruptAll();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
    }
    override fun onDestroy() {
        BackupWork.disconnect(idleCallback)
        handler.removeCallbacksAndMessages(null)
        if(::wake.isInitialized && wake.isHeld) wake.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if(BackupWork.active && !normalStop) BackupWork.interruptAll()
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null
}
