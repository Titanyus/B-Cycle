package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Battery-only first step; each physical component keeps its own reading and timestamp. */
internal class AxsGroupView(context:Context,private val runtime:RideRuntime,search:()->Unit):LinearLayout(context) {
    private val themes=runtime.themes.settings
    private val slot=runtime.bikes.activeSlot
    private var shown:List<AxsDevice>?=null
    private val rows=LinearLayout(context).apply { orientation=VERTICAL }
    private val labels=mutableMapOf<String,Pair<TextView,TextView>>()
    private val removes=mutableListOf<View>()
    private val measurements=mutableMapOf<String,SensorMeasurementDetails>()
    private var binding=false
    private val warning=android.widget.Switch(context).apply {
        tag="axs_warning_enabled";setText(R.string.axs_warning_enabled)
        isChecked=runtime.bikes.current().shiftBatteryWarning
        setOnCheckedChangeListener { _,enabled ->
            if(!binding && runtime.bikes.activeSlot==slot) {
                runtime.bikes.save(runtime.bikes.current().copy(shiftBatteryWarning=enabled))
                if(!enabled) runtime.shiftBatteryWarning.clear()
            }
        }
    }
    private val read=BicycleComponents.action(context,themes,context.getString(R.string.axs_refresh)) {
        if(runtime.bikes.activeSlot==slot) if(runtime.hasBluetooth()) runtime.refreshShiftGroup() else search()
    }.apply { tag="axs_refresh" }
    private val add=BicycleComponents.action(context,themes,context.getString(R.string.axs_add),search).apply { tag="axs_add" }
    init {
        orientation=VERTICAL;tag="axs_components"
        addView(rows,LayoutParams(-1,-2))
        addView(LinearLayout(context).apply {
            orientation=HORIZONTAL
            addView(add,LayoutParams(0,-2,1f));addView(read,LayoutParams(0,-2,1f))
        },LayoutParams(-1,-2))
        addView(warning,LayoutParams(-1,-2))
        BicycleDialogStyle.content(this,themes);refresh()
    }
    fun refresh() {
        if(runtime.bikes.activeSlot!=slot) return
        val p=runtime.bikes.current()
        if(shown!=p.shiftDevices) {
            shown=p.shiftDevices;rows.removeAllViews();labels.clear();removes.clear();measurements.clear()
            p.shiftDevices.forEach { device ->
                val title=BicycleComponents.text(context,themes,device.name)
                val value=BicycleComponents.text(context,themes,"").apply { tag="axs_battery_${device.address}" }
                val state=BicycleFeedback.status(context,themes).apply { tag="axs_status_${device.address}";textSize=BicycleDesign.sensorStatusSp }
                labels[device.address]=value to state
                rows.addView(LinearLayout(context).apply {
                    orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
                    addView(title,LayoutParams(0,-2,1f))
                    addView(BicycleComponents.icon(context,themes,BicycleIcon.DeleteTrack,context.getString(R.string.axs_remove,device.name)) {
                        if(runtime.bikes.activeSlot==slot) runtime.removeShiftComponent(device.address)
                    }.also { removes.add(it) },LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize)))
                },LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
                rows.addView(value,LayoutParams(-1,-2))
                val received=SensorMeasurementDetails(context,themes,state).apply { tag="axs_measurements_${device.address}" }
                measurements[device.address]=received;rows.addView(received,LayoutParams(-1,-2))
            }
            BicycleDialogStyle.content(rows,themes)
        }
        p.shiftDevices.forEach { device ->
            val battery=runtime.shiftGroup.battery(device.address)
            val (value,state)=labels.getValue(device.address)
            BicycleFeedback.update(value,battery?.let {
                context.getString(R.string.axs_battery_value,it.percent,android.text.format.DateFormat.getDateFormat(context).format(java.util.Date(it.readAt))+" "+android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(it.readAt)))
            } ?: context.getString(R.string.bikes_battery,context.getString(R.string.bikes_battery_missing)))
            val connected=p.shiftEnabled && !runtime.disconnected(BikeSensor.SHIFT)
            val count=if(connected && runtime.shiftGroup.receivedBattery(device.address)) 1 else 0
            val status=context.getString(if(!connected) R.string.bikes_disconnected else runtime.shiftGroup.state(device.address))
            BicycleFeedback.update(state,if(count>0) context.getString(R.string.sensor_received_values,status,
                resources.getQuantityString(R.plurals.sensor_value_count,count,count)) else status)
            measurements[device.address]?.update(if(count>0) listOf(context.getString(R.string.sensor_values_battery)) else emptyList())
        }
        binding=true;warning.isChecked=p.shiftBatteryWarning;binding=false
        add.isEnabled=!runtime.recording && !runtime.saving && p.shiftDevices.size<AxsDevice.MAX_COUNT
        read.isEnabled=p.shiftEnabled && p.shiftDevices.isNotEmpty() && !runtime.shiftGroup.active
        removes.forEach { it.isEnabled=!runtime.recording && !runtime.saving;it.alpha=if(it.isEnabled) 1f else .4f }
        add.alpha=if(add.isEnabled) 1f else .4f;read.alpha=if(read.isEnabled) 1f else .4f
    }
}
