package de.bcycle.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

/** Foreground-only, selected sources only. No polling, network probes or recording side effects. */
internal class DashboardStatusMonitor(private val context:Context,private val changed:(DashboardStatusSnapshot)->Unit) {
    private val handler=Handler(Looper.getMainLooper())
    private var generation=0
    private var signalGeneration=0
    private var config=DashboardStatusConfig()
    private var networks:ConnectivityManager.NetworkCallback?=null
    private val vpns=mutableSetOf<Network>()
    private var telephony:TelephonyManager?=null
    private var signal:TelephonyCallback?=null
    private var strength:Int?=null
    private var signalSubscription=SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private var battery:Int?=null
    private var receiver:BroadcastReceiver?=null
    private val messages:()->Unit={ emit() }
    fun battery(value:Int?) { if(battery!=value) { battery=value;emit() } }
    fun configure(next:DashboardStatusConfig) {
        if(next==config) {
            if(next.uses(DashboardStatusItem.CELLULAR) && signalSubscription!=SubscriptionManager.getDefaultDataSubscriptionId()) startSignal(generation)
            emit();return
        }
        stop();config=next
        if(!next.populated) { emit();return }
        val session=generation
        if(next.uses(DashboardStatusItem.NOTIFICATIONS)) MessageStatusNotifications.observers.add(messages)
        val filter=IntentFilter()
        if(next.uses(DashboardStatusItem.TIME)) {
            filter.addAction(Intent.ACTION_TIME_TICK);filter.addAction(Intent.ACTION_TIME_CHANGED);filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        if(next.uses(DashboardStatusItem.CELLULAR)) { filter.addAction(SubscriptionManager.ACTION_DEFAULT_SUBSCRIPTION_CHANGED);filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED) }
        if(filter.countActions()>0) {
            receiver=object:BroadcastReceiver() {
                override fun onReceive(c:Context,intent:Intent) {
                    if(session!=generation) return
                    if(intent.action==SubscriptionManager.ACTION_DEFAULT_SUBSCRIPTION_CHANGED || intent.action==Intent.ACTION_AIRPLANE_MODE_CHANGED) startSignal(session)
                    emit()
                }
            }
            context.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED)
        }
        if(next.uses(DashboardStatusItem.VPN)) {
            val manager=context.getSystemService(ConnectivityManager::class.java)
            if(manager!=null) {
                runCatching { manager.allNetworks.filterTo(vpns) { manager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)==true } }
                val callback=object:ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network:Network) { if(session==generation) { vpns+=network;emit() } }
                    override fun onLost(network:Network) { if(session==generation) { vpns-=network;emit() } }
                    override fun onCapabilitiesChanged(network:Network,caps:NetworkCapabilities) {
                        if(session==generation) { if(caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) vpns+=network else vpns-=network;emit() }
                    }
                }
                runCatching { manager.registerNetworkCallback(NetworkRequest.Builder().clearCapabilities().addTransportType(NetworkCapabilities.TRANSPORT_VPN).build(),callback,handler);networks=callback }
            }
        }
        if(next.uses(DashboardStatusItem.CELLULAR)) startSignal(session)
        emit()
    }
    private fun startSignal(session:Int) {
        val signalSession=++signalGeneration
        signal?.let { callback -> runCatching { telephony?.unregisterTelephonyCallback(callback) } }
        signal=null;telephony=null;strength=null
        signalSubscription=SubscriptionManager.getDefaultDataSubscriptionId()
        if(android.provider.Settings.Global.getInt(context.contentResolver,android.provider.Settings.Global.AIRPLANE_MODE_ON,0)!=0) return
        if(!context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY_RADIO_ACCESS)) return
        val manager=context.getSystemService(TelephonyManager::class.java) ?: return
        val id=signalSubscription
        val selected=if(SubscriptionManager.isValidSubscriptionId(id)) manager.createForSubscriptionId(id) else manager
        val callback=object:TelephonyCallback(),TelephonyCallback.SignalStrengthsListener {
            override fun onSignalStrengthsChanged(value:SignalStrength) { if(session==generation && signalSession==signalGeneration) { strength=value.level.takeIf { it in 0..4 };emit() } }
        }
        runCatching {
            strength=selected.signalStrength?.level?.takeIf { it in 0..4 }
            selected.registerTelephonyCallback(context.mainExecutor,callback);telephony=selected;signal=callback
        }.onFailure { strength=null }
    }
    private fun emit() {
        changed(DashboardStatusSnapshot(android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date()),vpns.isNotEmpty(),battery,
            if(config.uses(DashboardStatusItem.NOTIFICATIONS)) MessageStatusNotifications.count(context) else 0,strength))
    }
    fun stop() {
        generation++;signalGeneration++
        networks?.let { runCatching { context.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it) } };networks=null;vpns.clear()
        signal?.let { callback -> runCatching { telephony?.unregisterTelephonyCallback(callback) } };signal=null;telephony=null;strength=null
        receiver?.let { runCatching { context.unregisterReceiver(it) } };receiver=null
        MessageStatusNotifications.observers.remove(messages);config=DashboardStatusConfig()
    }
}
