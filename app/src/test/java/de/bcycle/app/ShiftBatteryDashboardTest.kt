package de.bcycle.app

import android.os.Looper
import android.view.View
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
class ShiftBatteryDashboardTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val device=AxsDevice("00:11:22:33:44:55","SRAM Apex")
    private fun cached(address:String,percent:Int) {
        c.getSharedPreferences("axs-battery-cache",0).edit().putInt("$address:level",percent)
            .putLong("$address:at",123456789L).commit()
    }
    @Test fun firstConfiguredDerailleurFeedsIndividualAndCombinedFieldsWithoutOtherProfileLeaks() {
        val runtime=RideRuntime.get(c)
        val controller=AxsDevice("00:11:22:33:44:66","SRAM Controller")
        cached(device.address,36);cached(controller.address,5)
        runtime.bikes.save(runtime.bikes.current().copy(shiftDevices=listOf(device,controller)))
        assertEquals(36,runtime.dashboardBikeData.batteries.level(BatterySource.SHIFT))
        val field=DashboardField.fromId("SHIFT_BATTERY")!!
        assertEquals("Schaltgruppe",field.source)
        assertEquals("36",field.value(runtime.dashboardBikeData).text)
        assertEquals("%",field.value(runtime.dashboardBikeData).unit)
        val combined=BatteryFields.field(listOf(BatterySource.SHIFT,BatterySource.PHONE))
        assertEquals(combined,DashboardField.fromId(combined.id))
        assertEquals(combined,DashboardLayout.decode(DashboardLayout.defaults.replace(0,combined).encode()).fields[0])
        assertTrue(combined.value(runtime.dashboardBikeData).text.contains("36%"))
        runtime.bikes.save(runtime.bikes.current().copy(shiftEnabled=false))
        assertNull(runtime.dashboardBikeData.batteries.level(BatterySource.SHIFT))
        runtime.bikes.save(runtime.bikes.current().copy(shiftEnabled=true,type=BikeType.INDOOR))
        assertNull(runtime.dashboardBikeData.batteries.level(BatterySource.SHIFT))
        runtime.bikes.save(runtime.bikes.current().copy(type=BikeType.EBIKE,shiftDevices=emptyList()))
        assertNull(runtime.dashboardBikeData.batteries.level(BatterySource.SHIFT))
    }
    @Test fun rideSummaryKeepsStopBatteryWhenTheCacheChangesDuringSaving() {
        val runtime=RideRuntime.get(c);cached(device.address,20)
        runtime.bikes.save(runtime.bikes.current().copy(shiftDevices=listOf(device)))
        runtime.beginRecording()
        android.os.SystemClock.sleep(11_000)
        runtime.stopRecording()
        cached(device.address,70)
        val until=System.nanoTime()+10_000_000_000L
        while((runtime.saving || runtime.mapState.busy) && System.nanoTime()<until) {
            shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5)
        }
        assertEquals(70,runtime.derailleurBattery?.percent)
        assertEquals(20,runtime.takeRideSummary()?.shiftBatteryAtEnd)
    }
    @Test fun pickerOffersDerailleurUnderShiftGroupAndInBatteryMultiselect() {
        var chosen:DashboardField?=null
        val data=LiveData(batteries=BatterySnapshot(mapOf(BatterySource.SHIFT to 36,BatterySource.PHONE to 80)))
        val picker=FieldPickerView(c,data,GpsValues(),null,PulseValues(),DashboardField("SHIFT_BATTERY"),false,choose={chosen=it})
        val source=picker.findViewWithTag<Spinner>("field_source")
        assertEquals("Schaltgruppe",source.selectedItem)
        picker.findViewWithTag<View>("SHIFT_BATTERY").performClick()
        assertEquals("SHIFT_BATTERY",chosen!!.id)
        source.setSelection((0 until source.adapter.count).first { source.adapter.getItem(it)=="Akku" })
        shadowOf(Looper.getMainLooper()).idle()
        val shift=picker.findViewWithTag<CheckBox>("battery_select_SHIFT")
        assertEquals(c.getString(R.string.axs_derailleur),shift.text.toString())
        // Switch off/on selects a combined field using the same persisted source key.
        if(shift.isChecked) shift.performClick()
        shift.performClick();assertEquals(listOf(BatterySource.SHIFT),chosen!!.batterySources)
        assertEquals(BicycleIcon.Derailleur,BatteryFields.icon(BatterySource.SHIFT))
    }
    @Test fun warningThresholdIsStrictAndDeliveredOnlyOnceWhenDashboardIsVisible() {
        val warning=ShiftBatteryWarning()
        for(level in listOf(null,-1,20,36,100,101)) {
            warning.start(level,true);assertNull(warning.take(true,true,true))
        }
        for(level in listOf(0,19)) {
            warning.start(level,true);assertNull(warning.take(true,false,true))
            assertEquals(level,warning.take(true,true,true));assertNull(warning.take(true,true,true))
        }
        warning.start(5,false);assertNull(warning.take(true,true,true))
        warning.start(5,true);assertNull(warning.take(false,true,true));assertNull(warning.take(true,true,true))
        warning.start(5,true);assertNull(warning.take(true,true,false));assertNull(warning.take(true,true,true))
    }
    @Test fun actualRecordingStartShowsIndependentWarningAndStopClearsIt() {
        val runtime=RideRuntime.get(c);cached(device.address,19)
        runtime.bikes.save(runtime.bikes.current().copy(shiftDevices=listOf(device),bikeAddress="00:11:22:33:44:77"))
        DrinkReminderSettings.save(runtime.preferences,DrinkReminderConfig(enabled=false))
        val host=Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity=host.get()
        val render=MainActivity::class.java.getDeclaredMethod("render").apply { isAccessible=true }
        val panel=activity.window.decorView.findViewWithTag<ShiftBatteryWarningPanel>("shift_battery_warning_panel")
        try {
            assertEquals(View.GONE,panel.visibility)
            runtime.beginRecording();render.invoke(activity)
            assertTrue(runtime.recording);assertEquals(View.VISIBLE,panel.visibility)
            panel.performClick();render.invoke(activity);assertEquals(View.GONE,panel.visibility)
            runtime.stopRecording();render.invoke(activity);assertEquals(View.GONE,panel.visibility)
        } finally {
            if(runtime.recording) runtime.stopRecording()
            val until=System.nanoTime()+10_000_000_000L
            while((runtime.saving || runtime.mapState.busy) && System.nanoTime()<until) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(5) }
            host.pause().stop().destroy()
        }
    }
    @Test fun sensorWarningIsMandatoryAndUsesSameSurfaceInBothDesignModes() {
        val runtime=RideRuntime.get(c);cached(device.address,36)
        runtime.bikes.save(runtime.bikes.current().copy(shiftDevices=listOf(device)))
        val view=AxsGroupView(c,runtime)
        assertFalse(testDescendants(view).filterIsInstance<TextView>().any { it.text==c.getString(R.string.axs_hint) })
        assertNull(view.findViewWithTag<Switch>("axs_warning_enabled"))
        val host=Robolectric.buildActivity(android.app.Activity::class.java).setup()
        try {
            for(flat in listOf(false,true)) {
                runtime.preferences.edit().putBoolean(BicycleAppearance.flatKey,flat).commit()
                val panel=ShiftBatteryWarningPanel(host.get(),runtime.themes)
                host.get().setContentView(panel)
                panel.showBattery(19);assertEquals(View.VISIBLE,panel.visibility)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(9999));assertEquals(View.VISIBLE,panel.visibility)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));assertEquals(View.GONE,panel.visibility)
                panel.showBattery(0);panel.performClick();assertEquals(View.GONE,panel.visibility)
            }
        } finally { host.pause().stop().destroy() }
    }
}
