package de.bcycle.app

import android.Manifest
import android.bluetooth.*
import android.os.Looper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowBluetoothGatt
import org.robolectric.shadow.api.Shadow
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33,36],shadows=[TrainerConnectionTest.GattShadow::class])
class TrainerConnectionTest {
    @Test fun subscriptionPrecedesBatteryReadAndLiveNeedsMeasurementStaleAndOldCallbacksAreHandled() {
        val app=RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN)
        val device=app.getSystemService(BluetoothManager::class.java).adapter.getRemoteDevice("00:11:22:33:44:55")
        var frames=0
        val connection=TrainerConnection(app,{}, { _,_->frames++ })
        connection.connect(device)
        val gatt=shadowOf(device).bluetoothGatts.single();val shadow=Shadow.extract<GattShadow>(gatt)
        val service=BluetoothGattService(TrainerConnection.SERVICE,0)
        val data=BluetoothGattCharacteristic(TrainerConnection.MEASUREMENT,BluetoothGattCharacteristic.PROPERTY_NOTIFY,0)
        val cccd=BluetoothGattDescriptor(TrainerConnection.CCCD,BluetoothGattDescriptor.PERMISSION_WRITE)
        data.addDescriptor(cccd);service.addCharacteristic(data)
        val batteryService=BluetoothGattService(TrainerConnection.BATTERY_SERVICE,0)
        val battery=BluetoothGattCharacteristic(TrainerConnection.BATTERY,BluetoothGattCharacteristic.PROPERTY_READ,0)
        batteryService.addCharacteristic(battery)
        shadow.addDiscoverableService(service);shadow.addDiscoverableService(batteryService);shadow.allowCharacteristicNotification(data)
        val callback=shadow.gattCallback
        callback.onConnectionStateChange(gatt,0,BluetoothProfile.STATE_CONNECTED);shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("CCCD"),shadow.operations);assertFalse(connection.live)
        callback.onDescriptorWrite(gatt,cccd,0);shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("CCCD","battery"),shadow.operations);assertTrue(connection.subscribed);assertFalse(connection.live)
        callback.onCharacteristicChanged(gatt,data,ftmsFrame(4 or 64,232,3,180,0,200,0));shadowOf(Looper.getMainLooper()).idle()
        assertTrue(connection.live);assertEquals(3,connection.receivedValueCount);assertEquals(1,frames)
        callback.onCharacteristicRead(gatt,battery,byteArrayOf(82),0);shadowOf(Looper.getMainLooper()).idle()
        assertEquals(82,connection.battery);assertEquals(4,connection.receivedValueCount)
        callback.onCharacteristicChanged(gatt,data,ftmsFrame(64,1,0));shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1,frames) // incomplete packet does not count as data
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5001))
        assertFalse(connection.live);assertEquals(app.getString(R.string.trainer_stale),connection.status)
        callback.onCharacteristicChanged(gatt,data,ftmsFrame(1 or 64,0,0));shadowOf(Looper.getMainLooper()).idle()
        assertTrue(connection.live);assertNull(connection.readings.value(TrainerField.SPEED,android.os.SystemClock.elapsedRealtime()))
        connection.stop();assertTrue(shadow.isClosed);assertEquals(0,connection.receivedValueCount)
        callback.onCharacteristicChanged(gatt,data,ftmsFrame(1 or 64,200,0));shadowOf(Looper.getMainLooper()).idle()
        assertEquals(2,frames);assertFalse(connection.live)
        assertEquals(listOf("CCCD","battery"),shadow.operations) // no trainer control or other characteristic write
    }
    @Test fun failedSubscriptionClosesGattAndNeverClaimsLive() {
        val app=RuntimeEnvironment.getApplication();shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT)
        val device=app.getSystemService(BluetoothManager::class.java).adapter.getRemoteDevice("00:11:22:33:44:66")
        val c=TrainerConnection(app,{}, { _,_->fail("No data expected") });c.connect(device)
        val gatt=shadowOf(device).bluetoothGatts.single();val shadow=Shadow.extract<GattShadow>(gatt)
        val s=BluetoothGattService(TrainerConnection.SERVICE,0)
        val data=BluetoothGattCharacteristic(TrainerConnection.MEASUREMENT,BluetoothGattCharacteristic.PROPERTY_NOTIFY,0)
        val descriptor=BluetoothGattDescriptor(TrainerConnection.CCCD,16);data.addDescriptor(descriptor);s.addCharacteristic(data)
        shadow.addDiscoverableService(s);shadow.allowCharacteristicNotification(data)
        shadow.gattCallback.onConnectionStateChange(gatt,0,2);shadowOf(Looper.getMainLooper()).idle()
        shadow.gattCallback.onDescriptorWrite(gatt,descriptor,257);shadowOf(Looper.getMainLooper()).idle()
        assertFalse(c.active);assertFalse(c.live);assertTrue(shadow.isClosed)
    }
    @Implements(BluetoothGatt::class)
    class GattShadow:ShadowBluetoothGatt() {
        val operations=mutableListOf<String>()
        @Implementation(minSdk=33)
        public override fun writeDescriptor(d:BluetoothGattDescriptor,value:ByteArray):Int {
            assertEquals(TrainerConnection.CCCD,d.uuid);assertArrayEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,value)
            operations+="CCCD";return BluetoothStatusCodes.SUCCESS
        }
        @Implementation fun readCharacteristic(c:BluetoothGattCharacteristic):Boolean { operations+="battery";return true }
    }
}
