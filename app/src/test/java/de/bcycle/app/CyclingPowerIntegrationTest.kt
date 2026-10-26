package de.bcycle.app

import android.Manifest
import android.bluetooth.*
import android.os.Looper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],shadows=[TrainerConnectionTest.GattShadow::class])
class CyclingPowerIntegrationTest {
    private data class Link(val gatt:BluetoothGatt,val data:BluetoothGattCharacteristic) {
        val shadow get()=Shadow.extract<TrainerConnectionTest.GattShadow>(gatt)
        fun send(bytes:ByteArray) { shadow.gattCallback.onCharacteristicChanged(gatt,data,bytes);shadowOf(Looper.getMainLooper()).idle() }
    }
    private fun link(device:BluetoothDevice,service:java.util.UUID,measurement:java.util.UUID):Link {
        val gatt=shadowOf(device).bluetoothGatts.last();val shadow=Shadow.extract<TrainerConnectionTest.GattShadow>(gatt)
        val data=BluetoothGattCharacteristic(measurement,BluetoothGattCharacteristic.PROPERTY_NOTIFY,0)
        val cccd=BluetoothGattDescriptor(TrainerConnection.CCCD,16);data.addDescriptor(cccd)
        if(service==CyclingPowerConnection.SERVICE) shadow.addDiscoverableService(BluetoothGattService(CyclingPowerConnection.BATTERY_SERVICE,0).apply {
            addCharacteristic(BluetoothGattCharacteristic(CyclingPowerConnection.BATTERY,BluetoothGattCharacteristic.PROPERTY_READ,0))
        })
        shadow.addDiscoverableService(BluetoothGattService(service,0).apply { addCharacteristic(data) });shadow.allowCharacteristicNotification(data)
        shadow.gattCallback.onConnectionStateChange(gatt,0,2);shadowOf(Looper.getMainLooper()).idle()
        shadow.gattCallback.onDescriptorWrite(gatt,cccd,0);shadowOf(Looper.getMainLooper()).idle()
        return Link(gatt,data)
    }
    @Test fun independentPowerAndCadenceSelectionFallbackAndRecoveryReachDashboardPerformanceAndFit() {
        val app=RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN)
        val adapter=app.getSystemService(BluetoothManager::class.java).adapter;shadowOf(adapter).setState(BluetoothAdapter.STATE_ON)
        val r=RideRuntime.get(app);r.changeBikeType(BikeType.INDOOR)
        val trainer=adapter.getRemoteDevice("00:11:22:33:44:11");val meter=adapter.getRemoteDevice("00:11:22:33:44:22")
        r.selectTrainer(trainer,"KICKR test");r.selectPowerMeter(meter,"Pedale test");r.setVisible(true)
        try {
            val t=link(trainer,TrainerConnection.SERVICE,TrainerConnection.MEASUREMENT)
            val p=link(meter,CyclingPowerConnection.SERVICE,CyclingPowerConnection.MEASUREMENT)
            val battery=p.gatt.getService(CyclingPowerConnection.BATTERY_SERVICE).getCharacteristic(CyclingPowerConnection.BATTERY)
            p.shadow.gattCallback.onCharacteristicRead(p.gatt,battery,byteArrayOf(81),0);shadowOf(Looper.getMainLooper()).idle()
            var turns=100
            fun trainerFrame()=t.send(ftmsFrame(4 or 64,232,3,180,0,200,0))
            fun powerFrame() { turns++;p.send(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).apply {
                putShort(32);putShort(350);putShort(turns.toShort());putShort((turns*1024).toShort())
            }.array()) }
            trainerFrame();powerFrame()
            assertEquals(200,r.dashboardBikeData.riderPowerWatts) // Existing default unchanged.
            assertTrue(r.setIndoorCorrection(IndoorCorrection(15,20)))
            val prefs=RecordingSources.defaults(r.config,true).let { RecordingSources(it.preferred+(RecordingQuantity.POWER to RecordingSource.POWER)+(RecordingQuantity.CADENCE to RecordingSource.POWER)) }
            r.saveConfig(RecordingConfig(setOf("RIDER_POWER","CADENCE","SPEED","BIKE_DISTANCE","MECHANICAL_WORK"),sourcePreferences=prefs))
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));trainerFrame();powerFrame()
            assertEquals(350,r.dashboardBikeData.riderPowerWatts);assertEquals(120L,r.dashboardBikeData.value(LiveMetric.CADENCE))
            assertEquals(PowerSource.CYCLING_POWER_BLE,r.powerMeasurement!!.source)
            assertEquals(3,r.receivedValueCount(BikeSensor.POWER))
            assertEquals(81,r.batterySnapshot.level(BatterySource.POWER))
            assertTrue(BatterySource.POWER in BatteryFields.choices(r.dashboardBikeData))
            r.beginRecording()
            repeat(12) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));trainerFrame();powerFrame() }
            repeat(8) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));trainerFrame() }
            assertEquals(300,r.dashboardBikeData.riderPowerWatts)
            assertEquals(180L,r.dashboardBikeData.value(LiveMetric.CADENCE))
            assertEquals(PowerSource.FTMS,r.powerMeasurement!!.source)
            powerFrame();assertEquals(350,r.dashboardBikeData.riderPowerWatts)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));trainerFrame();powerFrame()
            assertEquals(120L,r.dashboardBikeData.value(LiveMetric.CADENCE))
            assertEquals(RecordingSource.POWER,r.config.sourcePreferences!!.preferred[RecordingQuantity.POWER])
            r.stopRecording()
            val end=System.nanoTime()+10_000_000_000L
            while(r.saving && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
            assertFalse(r.saving)
            val file=java.io.File(app.filesDir,"fit-library").listFiles { f->f.extension=="fit" }!!.single()
            val fit=file.inputStream().use { com.garmin.fit.FitDecoder().decode(it) }
            assertTrue(fit.recordMesgs.any { it.power?.toInt()==350 && it.cadence?.toInt()==60 })
            assertTrue(fit.recordMesgs.any { it.power?.toInt()==300 && it.cadence?.toInt()==90 })
            assertEquals(r.performanceValues.knownMechanicalWorkKj,fit.sessionMesgs.single().totalWork.toDouble()/1000.0,.002)
            assertEquals(listOf("CCCD","battery"),p.shadow.operations)
            val callback=p.shadow.gattCallback;r.disconnectSensor(BikeSensor.POWER)
            callback.onCharacteristicChanged(p.gatt,p.data,byteArrayOf(0,0,1,0));shadowOf(Looper.getMainLooper()).idle()
            assertNull(r.receivedValueNames(BikeSensor.POWER));assertTrue(p.shadow.isClosed)
        } finally { if(r.recording) r.stopRecording();r.setVisible(false) }
    }
}
