package de.bcycle.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.UUID

/** Read-only Cycling Power Service, serialized CCCD then optional battery read. */
@SuppressLint("MissingPermission")
internal class CyclingPowerConnection(private val context:Context,private val changed:()->Unit,
    private val measurement:(TrainerMeasurement,Long)->Unit) {
    companion object {
        val SERVICE:UUID=UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
        val MEASUREMENT:UUID=UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb")
        val CCCD:UUID=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val BATTERY_SERVICE:UUID=UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY:UUID=UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    }
    private val handler=Handler(Looper.getMainLooper())
    private var client:BluetoothGatt?=null
    private var epoch=0
    val readings=TrainerReadings()
    private val decoder=CyclingPowerMeasurements()
    var active=false;private set
    var subscribed=false;private set
    var battery:Int?=null;private set
    private var receivedBattery=false
    var status=context.getString(R.string.power_meter_disconnected);private set
    val live get()=subscribed && readings.lastAt?.let { SystemClock.elapsedRealtime()-it in 0..5000 }==true
    val receivedValueCount get()=readings.count+if(receivedBattery) 1 else 0
    private val timeout=Runnable { fail(R.string.power_meter_timeout) }
    private val stale=Runnable { if(subscribed) { status=context.getString(R.string.power_meter_stale);changed() } }
    fun connect(device:BluetoothDevice) {
        stop();active=true;val session=++epoch
        status=context.getString(R.string.power_meter_connecting);changed();handler.postDelayed(timeout,20_000)
        try {
            client=device.connectGatt(context,false,object:BluetoothGattCallback() {
                private fun dispatch(gatt:BluetoothGatt,action:()->Unit) { handler.post {
                    if(session==epoch && client===gatt && active) try { action() }
                    catch(_:SecurityException) { fail(R.string.power_meter_permission) }
                    catch(_:IllegalStateException) { fail(R.string.power_meter_unavailable) }
                } }
                override fun onConnectionStateChange(gatt:BluetoothGatt,result:Int,state:Int)=dispatch(gatt) {
                    if(result!=BluetoothGatt.GATT_SUCCESS || state==BluetoothProfile.STATE_DISCONNECTED) fail(R.string.power_meter_disconnected)
                    else if(state==BluetoothProfile.STATE_CONNECTED && !gatt.discoverServices()) fail(R.string.power_meter_discovery_failed)
                }
                override fun onServicesDiscovered(gatt:BluetoothGatt,result:Int)=dispatch(gatt) {
                    val characteristic=gatt.getService(SERVICE)?.getCharacteristic(MEASUREMENT)
                    val descriptor=characteristic?.getDescriptor(CCCD)
                    if(result!=BluetoothGatt.GATT_SUCCESS || descriptor==null || characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY==0) {
                        fail(R.string.power_meter_not_cps);return@dispatch
                    }
                    if(!gatt.setCharacteristicNotification(characteristic,true) || gatt.writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)!=BluetoothStatusCodes.SUCCESS) fail(R.string.power_meter_subscribe_failed)
                }
                override fun onDescriptorWrite(gatt:BluetoothGatt,descriptor:BluetoothGattDescriptor,result:Int)=dispatch(gatt) {
                    if(descriptor.uuid!=CCCD || descriptor.characteristic.uuid!=MEASUREMENT) return@dispatch
                    if(result!=BluetoothGatt.GATT_SUCCESS) { fail(R.string.power_meter_subscribe_failed);return@dispatch }
                    handler.removeCallbacks(timeout);subscribed=true;status=context.getString(R.string.power_meter_waiting);changed()
                    // Serialized after the CCCD completion. No other GATT write is issued.
                    gatt.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY)?.let { gatt.readCharacteristic(it) }
                }
                override fun onCharacteristicChanged(gatt:BluetoothGatt,characteristic:BluetoothGattCharacteristic,value:ByteArray) {
                    val copy=value.copyOf()
                    dispatch(gatt) {
                        if(characteristic.uuid!=MEASUREMENT || !subscribed) return@dispatch
                        val frame=decoder.decode(copy,SystemClock.elapsedRealtime()) ?: return@dispatch
                        val now=SystemClock.elapsedRealtime();readings.apply(frame,now)
                        measurement(frame,now)
                        if(live) {
                            status=context.getString(R.string.power_meter_live)
                            handler.removeCallbacks(stale);handler.postDelayed(stale,5001)
                        }
                        changed()
                    }
                }
                override fun onCharacteristicRead(gatt:BluetoothGatt,characteristic:BluetoothGattCharacteristic,value:ByteArray,result:Int) {
                    val copy=value.copyOf()
                    dispatch(gatt) { if(characteristic.uuid==BATTERY && result==BluetoothGatt.GATT_SUCCESS && copy.size==1) {
                        battery=(copy[0].toInt() and 255).takeIf { it<=100 };if(battery!=null) receivedBattery=true;changed()
                    } }
                }
            },BluetoothDevice.TRANSPORT_LE)
            if(client==null) fail(R.string.power_meter_unavailable)
        } catch(_:SecurityException) { fail(R.string.power_meter_permission) }
        catch(_:IllegalStateException) { fail(R.string.power_meter_unavailable) }
    }
    fun stop() {
        epoch++;active=false;subscribed=false;readings.clear();decoder.reset();battery=null;receivedBattery=false
        handler.removeCallbacks(timeout);handler.removeCallbacks(stale)
        val old=client;client=null
        try { old?.disconnect();old?.close() } catch(_:SecurityException) { }
        status=context.getString(R.string.power_meter_disconnected)
    }
    private fun fail(message:Int) { stop();status=context.getString(message);changed() }
}
