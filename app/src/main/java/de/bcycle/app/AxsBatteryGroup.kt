package de.bcycle.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

internal data class AxsDevice(val address:String,val name:String) {
    companion object {
        const val MAX_COUNT=8
        fun decode(rows:JSONArray):List<AxsDevice> {
            require(rows.length()<=MAX_COUNT)
            val result=(0 until rows.length()).map { i ->
                val row=rows.getJSONObject(i)
                require(row.keys().asSequence().all { it in setOf("address","name") })
                val address=row.getString("address").uppercase(Locale.ROOT);val name=row.getString("name")
                require(address.matches(Regex("([0-9A-F]{2}:){5}[0-9A-F]{2}")))
                require(name.isNotBlank() && name.length<=248 && name.none(Char::isISOControl))
                AxsDevice(address,name)
            }
            require(result.map { it.address }.distinct().size==result.size)
            return result
        }
        fun encode(devices:List<AxsDevice>)=JSONArray().apply { devices.forEach { put(JSONObject().put("address",it.address).put("name",it.name)) } }
    }
}

internal data class AxsBattery(val percent:Int,val readAt:Long)

/** Short, serial, read-only GATT sessions. No SRAMBond, control writes, or continuous scan. */
@SuppressLint("MissingPermission")
internal class AxsBatteryGroup(private val context:Context,private val changed:()->Unit) {
    companion object {
        val SRAM_SERVICE=UUID.fromString("0000fe51-0000-1000-8000-00805f9b34fb")
        val BATTERY_SERVICE=UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY=UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        fun isSram(name:String,services:List<UUID>,manufacturer:Boolean=false)=
            manufacturer || name.startsWith("SRAM ",ignoreCase=true) || services.any {
                it==SRAM_SERVICE || it.toString().startsWith("d905") && it.toString().endsWith("-90aa-4c7c-b036-1e01fb8eb7ee")
            }
        fun percent(bytes:ByteArray)=bytes.takeIf { it.size==1 }?.first()?.toInt()?.and(255)?.takeIf { it in 0..100 }
    }
    private val handler=Handler(Looper.getMainLooper())
    private val cache=context.getSharedPreferences("axs-battery-cache",Context.MODE_PRIVATE)
    private val attempted=mutableMapOf<String,Long>()
    private val states=mutableMapOf<String,Int>()
    private var devices:List<AxsDevice> = emptyList()
    private val pending=ArrayDeque<AxsDevice>()
    private var client:BluetoothGatt?=null
    private var current:AxsDevice?=null
    private var epoch=0
    val active get()=client!=null || pending.isNotEmpty()
    private val timeout=Runnable { finish(R.string.axs_timeout) }
    fun battery(address:String):AxsBattery? {
        val level=cache.getInt("$address:level",-1);val at=cache.getLong("$address:at",0)
        return if(level in 0..100 && at>0) AxsBattery(level,at) else null
    }
    fun state(address:String)=states[address] ?: R.string.axs_waiting
    fun configure(selected:List<AxsDevice>) {
        if(devices==selected) return
        stop();devices=selected.toList();attempted.clear();states.clear()
    }
    fun refresh(force:Boolean=false) {
        if(active) return
        val now=SystemClock.elapsedRealtime()
        pending.addAll(devices.filter { force || attempted[it.address]?.let { last -> now-last>=300_000 }!=false })
        if(pending.isNotEmpty()) next()
    }
    private fun next() {
        if(pending.isEmpty()) { changed();return }
        val device=pending.removeFirst();current=device
        attempted[device.address]=SystemClock.elapsedRealtime();states[device.address]=R.string.axs_reading
        val session=++epoch
        handler.postDelayed(timeout,10_000)
        try {
            val adapter=context.getSystemService(BluetoothManager::class.java).adapter
            if(adapter?.isEnabled!=true) { finish(R.string.bikes_scan_bluetooth);return }
            client=adapter.getRemoteDevice(device.address).connectGatt(context,false,object:BluetoothGattCallback() {
                private fun dispatch(gatt:BluetoothGatt,action:()->Unit) { handler.post {
                    if(session!=epoch || client!==gatt) return@post
                    try { action() } catch(_:SecurityException) { finish(R.string.bikes_scan_permission) }
                    catch(_:IllegalStateException) { finish(R.string.axs_failed) }
                } }
                override fun onConnectionStateChange(gatt:BluetoothGatt,status:Int,state:Int)=dispatch(gatt) {
                    if(status!=BluetoothGatt.GATT_SUCCESS || state==BluetoothProfile.STATE_DISCONNECTED) finish(R.string.axs_unreachable)
                    else if(state==BluetoothProfile.STATE_CONNECTED && !gatt.discoverServices()) finish(R.string.axs_failed)
                }
                override fun onServicesDiscovered(gatt:BluetoothGatt,status:Int)=dispatch(gatt) {
                    if(status!=BluetoothGatt.GATT_SUCCESS) { finish(R.string.axs_failed);return@dispatch }
                    val value=gatt.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY)
                    if(value==null || value.properties and BluetoothGattCharacteristic.PROPERTY_READ==0) finish(R.string.axs_no_battery)
                    else if(!gatt.readCharacteristic(value)) finish(R.string.axs_failed)
                }
                override fun onCharacteristicRead(gatt:BluetoothGatt,characteristic:BluetoothGattCharacteristic,value:ByteArray,status:Int) {
                    val copy=value.copyOf()
                    dispatch(gatt) {
                        if(characteristic.uuid!=BATTERY) return@dispatch
                        val percent=percent(copy)
                        if(status!=BluetoothGatt.GATT_SUCCESS || percent==null) { finish(R.string.axs_failed);return@dispatch }
                        cache.edit().putInt("${device.address}:level",percent).putLong("${device.address}:at",System.currentTimeMillis()).apply()
                        finish(R.string.axs_read_ok)
                    }
                }
            },BluetoothDevice.TRANSPORT_LE)
            if(client==null) finish(R.string.axs_failed) else changed()
        } catch(_:SecurityException) { finish(R.string.bikes_scan_permission) }
        catch(_:IllegalArgumentException) { finish(R.string.axs_failed) }
        catch(_:IllegalStateException) { finish(R.string.axs_failed) }
    }
    private fun close() {
        handler.removeCallbacks(timeout);epoch++
        val old=client;client=null;current=null
        try { old?.disconnect();old?.close() } catch(_:SecurityException) { }
    }
    private fun finish(state:Int) {
        current?.let { states[it.address]=state }
        close();next()
    }
    fun stop() { pending.clear();current?.let { states[it.address]=R.string.bikes_disconnected };close() }
}
