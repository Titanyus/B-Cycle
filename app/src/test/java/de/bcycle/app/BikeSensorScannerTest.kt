package de.bcycle.app

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Looper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33,35,36])
@Suppress("DEPRECATION")
class BikeSensorScannerTest {
    private fun adapter(): BluetoothAdapter {
        val app=RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT)
        return app.getSystemService(BluetoothManager::class.java).adapter.also { shadowOf(it).setState(BluetoothAdapter.STATE_ON) }
    }
    @Test fun supportedAdvertisedDevicesAreDeduplicatedAssignedSensorsExcludedAndOldCallbacksIgnored() {
        val adapter=adapter();val app=RuntimeEnvironment.getApplication()
        val found=mutableListOf<BikeSensor>();val excluded="00:11:22:33:44:55"
        val scanner=BikeSensorScanner(app,setOf(excluded),{ _,_,kind->found+=kind },{})
        scanner.start()
        val callback=shadowOf(adapter.bluetoothLeScanner).scanCallbacks.single()
        val record=ReflectionHelpers.callStaticMethod<ScanRecord>(ScanRecord::class.java,"parseFromBytes",ReflectionHelpers.ClassParameter.from(ByteArray::class.java,byteArrayOf(3,3,0x0d,0x18,0)))
        val device=adapter.getRemoteDevice("00:11:22:33:44:66")
        repeat(2) { callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES,ScanResult(device,record,-50,100)) }
        callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES,ScanResult(adapter.getRemoteDevice(excluded),record,-50,100))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(BikeSensor.HEART),found)
        scanner.stop()
        callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES,ScanResult(adapter.getRemoteDevice("00:11:22:33:44:77"),record,-50,100))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11))
        assertEquals(1,found.size);assertTrue(shadowOf(adapter.bluetoothLeScanner).scanCallbacks.isEmpty())
    }
    @Test fun unadvertisedBikeIsOnlyListedAfterServiceDiscoveryAndProbeIsClosed() {
        val adapter=adapter();val app=RuntimeEnvironment.getApplication();val found=mutableListOf<BikeSensor>()
        val scanner=BikeSensorScanner(app,emptySet(),{ _,_,kind->found+=kind },{})
        scanner.start()
        val device=adapter.getRemoteDevice("00:11:22:33:44:88")
        shadowOf(adapter.bluetoothLeScanner).scanCallbacks.single().onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES,ScanResult(device,null,-50,100))
        shadowOf(Looper.getMainLooper()).idle();assertTrue(found.isEmpty())
        val gatt=shadowOf(device).bluetoothGatts.single();val shadow=shadowOf(gatt)
        shadow.addDiscoverableService(BluetoothGattService(BoschConnection.MCSP_SERVICE,BluetoothGattService.SERVICE_TYPE_PRIMARY))
        shadow.gattCallback.onConnectionStateChange(gatt,BluetoothGatt.GATT_SUCCESS,BluetoothProfile.STATE_CONNECTED)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf(BikeSensor.EBIKE),found);assertTrue(shadow.isClosed)
        scanner.stop()
    }
    @Test fun indoorDiscoveryOnlyListsTrainerAndHeartAndStopsAtTheScanDeadline() {
        val adapter=adapter();val app=RuntimeEnvironment.getApplication();val found=mutableListOf<BikeSensor>()
        val scanner=BikeSensorScanner(app,emptySet(),{ _,_,kind->found+=kind },{},setOf(BikeSensor.TRAINER,BikeSensor.HEART))
        scanner.start();val callback=shadowOf(adapter.bluetoothLeScanner).scanCallbacks.single()
        for((index,service) in listOf(0x1826,0x180d,0x1818).withIndex()) {
            val record=ReflectionHelpers.callStaticMethod<ScanRecord>(ScanRecord::class.java,"parseFromBytes",ReflectionHelpers.ClassParameter.from(ByteArray::class.java,byteArrayOf(3,3,service.toByte(),(service ushr 8).toByte(),0)))
            callback.onScanResult(1,ScanResult(adapter.getRemoteDevice("00:11:22:33:44:7$index"),record,-50,100))
        }
        shadowOf(Looper.getMainLooper()).idle();assertEquals(listOf(BikeSensor.TRAINER,BikeSensor.HEART),found)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11))
        assertTrue(shadowOf(adapter.bluetoothLeScanner).scanCallbacks.isEmpty())
    }

    @Test fun cyclingPowerAdvertisingIsProbedAndFtmsHasPriorityOverThePowerMeterRole() {
        val adapter=adapter();val app=RuntimeEnvironment.getApplication();val found=mutableListOf<BikeSensor>()
        val scanner=BikeSensorScanner(app,emptySet(),{ _,_,kind->found+=kind },{},setOf(BikeSensor.TRAINER,BikeSensor.POWER))
        scanner.start();val callback=shadowOf(adapter.bluetoothLeScanner).scanCallbacks.single()
        val record=ReflectionHelpers.callStaticMethod<ScanRecord>(ScanRecord::class.java,"parseFromBytes",ReflectionHelpers.ClassParameter.from(ByteArray::class.java,byteArrayOf(3,3,0x18,0x18,0)))
        for((index,trainer) in listOf(false,true).withIndex()) {
            val device=adapter.getRemoteDevice("00:11:22:33:44:8$index")
            callback.onScanResult(1,ScanResult(device,record,-50,100));shadowOf(Looper.getMainLooper()).idle()
            assertEquals(index,found.size)
            val gatt=shadowOf(device).bluetoothGatts.last();val shadow=shadowOf(gatt)
            shadow.addDiscoverableService(BluetoothGattService(CyclingPowerConnection.SERVICE,0))
            if(trainer) shadow.addDiscoverableService(BluetoothGattService(TrainerConnection.SERVICE,0))
            shadow.gattCallback.onConnectionStateChange(gatt,0,2);shadowOf(Looper.getMainLooper()).idle()
            assertTrue(shadow.isClosed)
        }
        assertEquals(listOf(BikeSensor.POWER,BikeSensor.TRAINER),found);scanner.stop()
    }

}
