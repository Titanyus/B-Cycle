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
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],shadows=[HeartRateConnectionTest.GattShadow::class])
class HeartProfileReuseTest {
    private fun connected(r:RideRuntime):Pair<BluetoothDevice,HeartRateConnectionTest.GattShadow> {
        val c=RuntimeEnvironment.getApplication()
        shadowOf(c).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN)
        val device=c.getSystemService(BluetoothManager::class.java).adapter.getRemoteDevice("00:11:22:33:44:66")
        r.setVisible(true)
        r.bikes.save(r.bikes.current().copy(heartAddress=device.address,heartName="H10"))
        r.heart.connect(device)
        val gatt=shadowOf(device).bluetoothGatts.last()
        val shadow=Shadow.extract<HeartRateConnectionTest.GattShadow>(gatt)
        shadow.gattCallback.onDescriptorWrite(gatt,BluetoothGattDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),BluetoothGattDescriptor.PERMISSION_WRITE),BluetoothGatt.GATT_SUCCESS)
        shadowOf(Looper.getMainLooper()).idle()
        shadow.gattCallback.onCharacteristicChanged(gatt,BluetoothGattCharacteristic(java.util.UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"),BluetoothGattCharacteristic.PROPERTY_NOTIFY,0),byteArrayOf(0,75))
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(r.heart.live)
        return device to shadow
    }
    private fun configure(r:RideRuntime,profile:BikeProfile) {
        val previous=r.bikes.activeSlot
        r.bikes.select(profile.slot);r.bikes.save(profile);r.bikes.select(previous)
    }
    @Test fun emptyProfileSharesLiveHeartWithoutOpeningAnotherGattButOwnAssignmentWins() {
        val r=RideRuntime.get(RuntimeEnvironment.getApplication())
        try {
            val (device,gatt)=connected(r);val clients=shadowOf(device).bluetoothGatts.size
            val target=r.bikes.add()!!
            assertTrue(r.switchBike(target.slot))
            assertEquals(device.address,r.bikes.current().heartAddress);assertEquals("H10",r.heartName)
            assertTrue(r.heart.live);assertFalse(gatt.isClosed)
            assertEquals(clients,shadowOf(device).bluetoothGatts.size)
            assertEquals(75,r.pulseValues.bpm);assertNull(r.pulseValues.average)
            val own=r.bikes.add()!!.copy(heartAddress="00:11:22:33:44:77",heartName="Eigener Sensor")
            configure(r,own)
            assertTrue(r.switchBike(own.slot));assertEquals(own.heartAddress,r.heartAddress)
            assertTrue(gatt.isClosed)
        } finally { r.setVisible(false) }
    }
    @Test fun disabledHeartInTargetDoesNotInheritOrKeepThePreviousConnection() {
        val r=RideRuntime.get(RuntimeEnvironment.getApplication())
        try {
            val (_,gatt)=connected(r)
            val target=r.bikes.add()!!.copy(heartEnabled=false);configure(r,target)
            assertTrue(r.switchBike(target.slot));assertNull(r.heartAddress)
            assertFalse(r.heart.live);assertTrue(gatt.isClosed);assertNull(r.pulseValues.bpm)
        } finally { r.setVisible(false) }
    }
}
