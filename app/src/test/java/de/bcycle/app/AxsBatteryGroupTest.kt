package de.bcycle.app

import android.Manifest
import android.bluetooth.*
import android.os.Looper
import android.widget.Switch
import android.widget.TextView
import org.json.JSONArray
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
class AxsBatteryGroupTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val a=AxsDevice("00:11:22:33:44:11","SRAM Schaltwerk")
    private val b=AxsDevice("00:11:22:33:44:22","SRAM Controller")
    private fun adapter():BluetoothAdapter {
        shadowOf(c).grantPermissions(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN)
        return c.getSystemService(BluetoothManager::class.java).adapter.also { shadowOf(it).setState(BluetoothAdapter.STATE_ON) }
    }
    private fun link(adapter:BluetoothAdapter,device:AxsDevice,battery:Boolean=true):Pair<BluetoothGatt,BluetoothGattCharacteristic> {
        val gatt=shadowOf(adapter.getRemoteDevice(device.address)).bluetoothGatts.last()
        val shadow=Shadow.extract<TrainerConnectionTest.GattShadow>(gatt)
        val value=BluetoothGattCharacteristic(AxsBatteryGroup.BATTERY,BluetoothGattCharacteristic.PROPERTY_READ,0)
        shadow.addDiscoverableService(BluetoothGattService(AxsBatteryGroup.SRAM_SERVICE,0))
        if(battery) shadow.addDiscoverableService(BluetoothGattService(AxsBatteryGroup.BATTERY_SERVICE,0).apply { addCharacteristic(value) })
        shadow.gattCallback.onConnectionStateChange(gatt,0,BluetoothProfile.STATE_CONNECTED);shadowOf(Looper.getMainLooper()).idle()
        return gatt to value
    }
    private fun read(link:Pair<BluetoothGatt,BluetoothGattCharacteristic>,value:Int) {
        Shadow.extract<TrainerConnectionTest.GattShadow>(link.first).gattCallback.onCharacteristicRead(link.first,link.second,byteArrayOf(value.toByte()),0)
        shadowOf(Looper.getMainLooper()).idle()
    }
    @Test fun serialReadsPersistZeroAndHundredAndAreDebouncedWithoutAnyWrites() {
        val adapter=adapter();val group=AxsBatteryGroup(c,{})
        group.configure(listOf(a,b));group.refresh()
        assertTrue(shadowOf(adapter.getRemoteDevice(b.address)).bluetoothGatts.isEmpty())
        val first=link(adapter,a);val s=Shadow.extract<TrainerConnectionTest.GattShadow>(first.first)
        assertEquals(listOf("battery"),s.operations);read(first,0);assertTrue(s.isClosed);assertTrue(group.receivedBattery(a.address))
        val second=link(adapter,b);read(second,100)
        assertFalse(group.active);assertEquals(0,group.battery(a.address)!!.percent);assertEquals(100,group.battery(b.address)!!.percent)
        assertEquals(100,AxsBatteryGroup(c,{}).battery(b.address)!!.percent)
        group.refresh();assertFalse(group.active)
        assertEquals(1,shadowOf(adapter.getRemoteDevice(a.address)).bluetoothGatts.size)
        group.refresh(force=true);assertTrue(group.active);group.stop()
    }
    @Test fun failedMissingAndSleepingDevicesDoNotEraseTheLastReadingOrBlockTheQueue() {
        val adapter=adapter();val group=AxsBatteryGroup(c,{})
        group.configure(listOf(a));group.refresh();read(link(adapter,a),74)
        group.refresh(force=true);read(link(adapter,a),255)
        assertEquals(74,group.battery(a.address)!!.percent);assertEquals(R.string.axs_failed,group.state(a.address))
        val missing=AxsDevice("00:11:22:33:44:33","SRAM ohne Akkudienst")
        group.configure(listOf(missing,b));group.refresh()
        link(adapter,missing,battery=false);assertEquals(R.string.axs_no_battery,group.state(missing.address))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11))
        assertFalse(group.active);assertNull(group.battery(b.address));assertEquals(R.string.axs_timeout,group.state(b.address))
        assertEquals(74,group.battery(a.address)!!.percent)
    }
    @Test fun profileChangeAndStopIgnoreLateCallbacksAndNeverMisattributeBattery() {
        val adapter=adapter();val group=AxsBatteryGroup(c,{})
        group.configure(listOf(a));group.refresh();val old=link(adapter,a)
        group.configure(listOf(b));group.refresh();read(old,99)
        assertNull(group.battery(a.address));assertNull(group.battery(b.address))
        val current=link(adapter,b);group.stop();read(current,23)
        assertFalse(group.active);assertNull(group.battery(b.address))
        assertTrue(Shadow.extract<TrainerConnectionTest.GattShadow>(current.first).isClosed)
    }
    @Test fun unsupportedValuesAndInvalidBackupAddressesCannotBecomePercentages() {
        listOf(byteArrayOf(),byteArrayOf(-1),byteArrayOf(101),byteArrayOf(22,33)).forEach { assertNull(AxsBatteryGroup.percent(it)) }
        assertEquals(listOf(a,b),AxsDevice.decode(AxsDevice.encode(listOf(a,b))))
        assertThrows(IllegalArgumentException::class.java) { AxsDevice.decode(AxsDevice.encode(listOf(a,a))) }
        assertThrows(IllegalArgumentException::class.java) { AxsDevice.decode(JSONArray("[{\"address\":\"invalid\",\"name\":\"SRAM\"}]")) }
        assertTrue(AxsBatteryGroup.isSram("SRAM 123456",emptyList()))
        assertFalse(AxsBatteryGroup.isSram("Battery device",listOf(AxsBatteryGroup.BATTERY_SERVICE)))
    }
    @Test fun ebikeGroupAssignsMultipleComponentsAndSwitchesProfilesWithoutChangingRecordingSources() {
        val adapter=adapter();val runtime=RideRuntime.get(c)
        val original=runtime.bikes.current().recording
        runtime.setVisible(true)
        try {
            runtime.selectShiftComponent(adapter.getRemoteDevice(a.address),a.name)
            runtime.selectShiftComponent(adapter.getRemoteDevice(b.address),b.name)
            assertEquals(listOf(a,b),runtime.bikes.current().shiftDevices)
            assertEquals(original,runtime.bikes.current().recording)
            val view=BikesView(c,runtime,{},{},{},{android.view.View(c)})
            val toggle=view.findViewWithTag<Switch>("bike_sensor_SHIFT")
            assertTrue(toggle.isChecked)
            assertNotNull(view.findViewWithTag<android.view.View>("axs_components"))
            read(link(adapter,a),66);runtime.shiftGroup.stop();view.refresh()
            assertTrue(view.findViewWithTag<TextView>("axs_battery_${a.address}").text.contains("66%"))
            assertTrue(view.findViewWithTag<TextView>("axs_status_${a.address}").text.contains("1"))
            assertTrue(view.findViewWithTag<TextView>("axs_status_${a.address}").isClickable)
            toggle.performClick();assertFalse(runtime.bikes.current().shiftEnabled);assertFalse(runtime.shiftGroup.active)
            runtime.removeShiftComponent(b.address);assertEquals(listOf(a),runtime.bikes.current().shiftDevices)
            val second=runtime.bikes.add()!!;runtime.switchBike(second.slot)
            assertTrue(runtime.bikes.current().shiftDevices.isEmpty());assertFalse(runtime.shiftGroup.active)
            runtime.changeBikeType(BikeType.INDOOR)
            assertFalse(BikeSensor.SHIFT in runtime.bikes.current().type.sensors)
        } finally {
            runtime.setVisible(false)
            // MapState initializes on its own worker; let it finish before Robolectric tears down.
            val until=System.nanoTime()+10_000_000_000L
            while(runtime.mapState.busy && System.nanoTime()<until) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5) }
        }
    }
}
