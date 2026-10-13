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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33, 35, 36], shadows = [HeartRateConnectionTest.GattShadow::class])
class HeartRateConnectionTest {
    @Test fun subscribesToStandardHeartRateNotificationsAndIgnoresOldClientAfterDisconnect() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        val device = app.getSystemService(BluetoothManager::class.java).adapter.getRemoteDevice("00:11:22:33:44:55")
        val pulses = mutableListOf<Int?>()
        val messages = mutableListOf<String>()
        val connection = HeartRateConnection(app, { messages += it }, { pulses += it.bpm }, {})
        connection.connect(device)
        val gatt = shadowOf(device).bluetoothGatts.single()
        val shadow = Shadow.extract<GattShadow>(gatt)
        val service = BluetoothGattService(HeartRateConnection.SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val measurement = BluetoothGattCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"), BluetoothGattCharacteristic.PROPERTY_NOTIFY, 0)
        val cccd = BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BluetoothGattDescriptor.PERMISSION_WRITE)
        measurement.addDescriptor(cccd); service.addCharacteristic(measurement)
        shadow.addDiscoverableService(service); shadow.allowCharacteristicNotification(measurement)
        val callback = shadow.gattCallback
        callback.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(connection.active)
        assertFalse(connection.live) // Wait for the subscription acknowledgement.
        assertSame(cccd, shadow.writtenDescriptor)
        assertArrayEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, shadow.writtenValue)
        callback.onDescriptorWrite(gatt, cccd, BluetoothGatt.GATT_SUCCESS)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(connection.live)
        callback.onCharacteristicChanged(gatt, measurement, byteArrayOf(0, 145.toByte()))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(145), pulses)
        assertEquals("Pulsmesser · Live", messages.last())
        connection.stop()
        assertFalse(connection.active); assertTrue(shadow.isClosed)
        callback.onCharacteristicChanged(gatt, measurement, byteArrayOf(0, 160.toByte()))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(145), pulses)
    }
    /** Simulates the remote acknowledgement for Android 13's descriptor-write API. */
    @Implements(BluetoothGatt::class)
    class GattShadow : ShadowBluetoothGatt() {
        var writtenDescriptor: BluetoothGattDescriptor? = null
        var writtenValue: ByteArray? = null
        @Implementation(minSdk = 33)
        public override fun writeDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray): Int {
            writtenDescriptor = descriptor; writtenValue = value.copyOf()
            return BluetoothStatusCodes.SUCCESS
        }
    }
}
