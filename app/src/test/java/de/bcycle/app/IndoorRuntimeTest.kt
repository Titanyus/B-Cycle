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
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],shadows=[TrainerConnectionTest.GattShadow::class])
class IndoorRuntimeTest {
    private fun setup():Triple<RideRuntime,BluetoothDevice,Pair<BluetoothGatt,BluetoothGattCharacteristic>> {
        val app=RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN)
        val adapter=app.getSystemService(BluetoothManager::class.java).adapter
        shadowOf(adapter).setState(BluetoothAdapter.STATE_ON)
        val r=RideRuntime.get(app);assertTrue(r.changeBikeType(BikeType.INDOOR))
        r.bikes.save(r.bikes.current().copy(autoConnect=false))
        val device=adapter.getRemoteDevice("00:11:22:33:44:55")
        r.selectTrainer(device,"JFICcycle");r.setVisible(true)
        val gatt=shadowOf(device).bluetoothGatts.single();val s=Shadow.extract<TrainerConnectionTest.GattShadow>(gatt)
        val service=BluetoothGattService(TrainerConnection.SERVICE,0)
        val data=BluetoothGattCharacteristic(TrainerConnection.MEASUREMENT,BluetoothGattCharacteristic.PROPERTY_NOTIFY,0)
        val descriptor=BluetoothGattDescriptor(TrainerConnection.CCCD,16);data.addDescriptor(descriptor);service.addCharacteristic(data)
        s.addDiscoverableService(service);s.allowCharacteristicNotification(data)
        s.gattCallback.onConnectionStateChange(gatt,0,2);shadowOf(Looper.getMainLooper()).idle()
        s.gattCallback.onDescriptorWrite(gatt,descriptor,0);shadowOf(Looper.getMainLooper()).idle()
        return Triple(r,device,gatt to data)
    }
    private fun send(link:Pair<BluetoothGatt,BluetoothGattCharacteristic>,distance:Int,power:Int=200) {
        val (gatt,data)=link
        val bytes=ftmsFrame(4 or 16 or 64,232,3,180,0,distance and 255,(distance ushr 8) and 255,0,power and 255,power ushr 8)
        Shadow.extract<TrainerConnectionTest.GattShadow>(gatt).gattCallback.onCharacteristicChanged(gatt,data,bytes)
        shadowOf(Looper.getMainLooper()).idle()
    }
    @Test fun previewNeverStartsTripAndRecordingUsesTrainerThroughBackgroundAndFreezesAtStop() {
        val (r,_,link)=setup()
        try {
            send(link,120);assertEquals(0L,r.elapsedMs);assertNull(r.ride.bike.distanceM)
            assertEquals(10.0,r.currentSpeedKmh!!,.001);assertEquals(200,r.dashboardBikeData.riderPowerWatts)
            assertNull(r.dashboardBikeData.value(LiveMetric.AVERAGE_POWER));assertFalse(r.gpsSensor.active)
            r.saveConfig(RecordingConfig(setOf("SPEED","BIKE_DISTANCE","RIDER_POWER","CADENCE","GPS_ELAPSED_TIME","MECHANICAL_WORK")))
            r.beginRecording();assertEquals(0L,r.ride.bike.distanceM)
            r.setVisible(false);assertTrue(r.trainer.active);assertFalse(r.gpsSensor.active)
            repeat(12) { i ->
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));send(link,125+i*10)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));send(link,130+i*10)
            }
            assertEquals(12000L,r.elapsedMs);assertEquals(120L,r.ride.bike.distanceM)
            assertEquals(2.4,r.performanceValues.knownMechanicalWorkKj,.001)
            assertEquals(PowerSource.FTMS,r.powerMeasurement!!.source)
            r.stopRecording();val stopped=r.elapsedMs
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
            assertEquals(stopped,r.elapsedMs);assertFalse(r.trainer.active)
            val end=System.nanoTime()+10_000_000_000L
            while(r.saving && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
            assertFalse(r.saving)
            val files=java.io.File(r.context.filesDir,"fit-library").listFiles { f -> f.extension=="fit" }!!
            assertEquals(1,files.size)
            val fit=files.single().inputStream().use { com.garmin.fit.FitDecoder().decode(it) }
            assertEquals(com.garmin.fit.SubSport.INDOOR_CYCLING,fit.sessionMesgs.single().subSport)
            assertTrue(fit.recordMesgs.any { it.power?.toInt()==200 && it.speed!=null && it.cadence?.toInt()==90 })
            assertTrue(fit.recordMesgs.all { it.positionLat==null && it.positionLong==null })
        } finally { if(r.recording) r.stopRecording();r.setVisible(false) }
    }
    @Test fun correctionFeedsDashboardPerformanceAndFitOnceWhileRawReadingsRemainIntact() {
        val (r,_,link)=setup()
        try {
            send(link,120)
            assertTrue(r.setIndoorCorrection(IndoorCorrection(15,20)))
            assertEquals(20.0,r.currentSpeedKmh!!,.001)
            assertEquals(300,r.dashboardBikeData.riderPowerWatts)
            assertEquals(200.0,r.trainer.readings.value(TrainerField.POWER,android.os.SystemClock.elapsedRealtime())!!,.001)
            assertEquals(300.0,r.powerMeasurement!!.watts!!,.001)
            r.saveConfig(RecordingConfig(setOf("SPEED","BIKE_DISTANCE","RIDER_POWER","CADENCE","GPS_ELAPSED_TIME","MECHANICAL_WORK")))
            r.beginRecording()
            assertFalse(r.setIndoorCorrection(IndoorCorrection(0,0)))
            repeat(12) { i ->
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));send(link,125+i*10)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));send(link,130+i*10)
            }
            assertEquals(240L,r.ride.bike.distanceM)
            assertEquals(3.6,r.performanceValues.knownMechanicalWorkKj,.001)
            assertEquals(300L,r.dashboardBikeData.value(LiveMetric.AVERAGE_POWER))
            r.stopRecording()
            assertFalse(r.setIndoorCorrection(IndoorCorrection())) // Saving is also protected.
            val end=System.nanoTime()+10_000_000_000L
            while(r.saving && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
            assertFalse(r.saving)
            val file=java.io.File(r.context.filesDir,"fit-library").listFiles { f -> f.extension=="fit" }!!.single()
            val original=file.readBytes()
            val fit=file.inputStream().use { com.garmin.fit.FitDecoder().decode(it) }
            assertTrue(fit.recordMesgs.any { it.power?.toInt()==300 && kotlin.math.abs((it.speed ?: 0f)-20f/3.6f)<.01 })
            assertEquals(240.0,fit.sessionMesgs.single().totalDistance.toDouble(),.01)
            assertTrue(r.setIndoorCorrection(IndoorCorrection(0,0)))
            assertArrayEquals(original,file.readBytes()) // Existing rides are never rewritten/re-scaled.
            send(link,300)
            assertEquals(0.0,r.currentSpeedKmh!!,.001);assertEquals(0,r.dashboardBikeData.riderPowerWatts)
            assertEquals(240L,r.ride.bike.distanceM) // Finished trip stays frozen.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
            assertNull(r.currentSpeedKmh);assertNull(r.dashboardBikeData.riderPowerWatts)
        } finally { if(r.recording) r.stopRecording();r.setVisible(false) }
    }
    @Test fun speedOnlyTrainerRecordsFallbackDistanceAndListsOnlyActuallyReceivedFields() {
        val (r,_,link)=setup()
        try {
            assertTrue(r.setIndoorType(IndoorBikeType.ROLLER_TRAINER))
            assertTrue(r.setIndoorCorrection(IndoorCorrection(10,20)))
            fun speedOnly() {
                val (gatt,data)=link
                Shadow.extract<TrainerConnectionTest.GattShadow>(gatt).gattCallback.onCharacteristicChanged(gatt,data,
                    ftmsFrame(4 or 64,232,3,180,0,200,0))
                shadowOf(Looper.getMainLooper()).idle()
            }
            speedOnly()
            assertEquals(3,r.receivedValueCount(BikeSensor.TRAINER))
            assertEquals(3,r.receivedValueNames(BikeSensor.TRAINER)!!.size)
            assertFalse(r.receivedValueNames(BikeSensor.TRAINER)!!.any { it.contains("Distanz") })
            assertNull(r.ride.bike.distanceM)
            r.saveConfig(RecordingConfig(setOf("SPEED","BIKE_DISTANCE","RIDER_POWER","CADENCE")))
            r.beginRecording()
            assertFalse(r.setIndoorType(IndoorBikeType.EXERCISE_BIKE))
            repeat(12) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));speedOnly() }
            assertEquals(66L,r.ride.bike.distanceM)
            assertEquals(20.0,r.currentSpeedKmh!!,.001)
            val before=r.ride.bike.distanceM!!
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(8));speedOnly()
            assertEquals(before,r.ride.bike.distanceM!!)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));speedOnly()
            assertTrue(r.ride.bike.distanceM!!>before)
            r.stopRecording()
            val end=System.nanoTime()+10_000_000_000L
            while(r.saving && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
            assertFalse(r.saving)
            val file=java.io.File(r.context.filesDir,"fit-library").listFiles { f -> f.extension=="fit" }!!.single()
            val fit=file.inputStream().use { com.garmin.fit.FitDecoder().decode(it) }
            assertTrue(fit.recordMesgs.any { (it.distance ?: 0f)>0f })
            assertEquals(r.ride.bike.distanceM!!.toDouble(),fit.sessionMesgs.single().totalDistance.toDouble(),1.0)
            assertEquals(com.garmin.fit.SubSport.INDOOR_CYCLING,fit.sessionMesgs.single().subSport)
            assertTrue(fit.recordMesgs.all { it.positionLat==null && it.positionLong==null })
        } finally { if(r.recording) r.stopRecording();r.setVisible(false) }
    }
    @Test fun disconnectReconnectTypeSwitchAndPersistedTrainerAreIndependentOfBoschPairing() {
        val (r,device,link)=setup()
        try {
            send(link,100);assertTrue(r.trainer.live)
            r.disconnectSensor(BikeSensor.TRAINER)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));assertFalse(r.trainer.active)
            assertEquals("00:11:22:33:44:55",r.bikes.current().trainerAddress)
            assertNull(r.currentSpeedKmh)
            r.connectAssignedSensors();assertEquals(2,shadowOf(device).bluetoothGatts.size)
            val second=shadowOf(device).bluetoothGatts.last()
            Shadow.extract<TrainerConnectionTest.GattShadow>(second).gattCallback.onConnectionStateChange(second,133,0)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
            assertTrue(shadowOf(device).bluetoothGatts.size>=3)
            val remembered=BikeProfileStore(r.context,r.preferences).current();assertEquals("JFICcycle",remembered.trainerName)
            assertTrue(r.changeBikeType(BikeType.EBIKE));assertFalse(r.trainer.active)
            assertNull(r.bikeAddress);assertNull(r.dashboardBikeData.riderPowerWatts)
            assertTrue(r.changeBikeType(BikeType.INDOOR));assertEquals(remembered.trainerAddress,r.trainerAddress)
            r.removeSensor(BikeSensor.TRAINER);assertNull(r.trainerAddress);assertFalse(r.trainer.active)
        } finally { r.setVisible(false) }
    }
}
