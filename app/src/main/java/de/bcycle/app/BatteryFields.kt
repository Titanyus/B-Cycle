package de.bcycle.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** Stable source IDs are UI metadata, not new FIT fields or sensor measurements. */
enum class BatterySource(val label: Int) {
    EBIKE(R.string.ui_source_bike), HEART(R.string.ui_source_heart),
    TRAINER(R.string.bikes_trainer), PHONE(R.string.battery_phone),
    POWER(R.string.bikes_power_meter), SHIFT(R.string.axs_derailleur)
}

data class BatterySnapshot(val levels: Map<BatterySource,Int?> = emptyMap()) {
    fun level(source: BatterySource)=levels[source]?.takeIf { it in 0..100 }
}

internal object BatteryFields {
    const val COMBINED="BATTERIES"
    fun sources(id: String): List<BatterySource>? {
        if(id==COMBINED) return listOf(BatterySource.PHONE)
        if(!id.startsWith("$COMBINED:")) return null
        val raw=id.substringAfter(':').split(',')
        val parsed=raw.map { name -> BatterySource.entries.find { it.name==name } ?: return null }
        return parsed.takeIf { it.size in 1..4 && it.distinct().size==it.size }
    }
    fun field(sources: List<BatterySource>): DashboardField {
        require(sources.size in 1..4 && sources.distinct().size==sources.size)
        return DashboardField("$COMBINED:"+sources.sortedBy { it.ordinal }.joinToString(",") { it.name })
    }
    fun choices(data:LiveData)=listOf(if(data.indoor) BatterySource.TRAINER else BatterySource.EBIKE,BatterySource.HEART,BatterySource.POWER,BatterySource.PHONE,BatterySource.SHIFT)
        .filter { data.batteries.level(it)!=null }
    fun icon(source: BatterySource)=when(source) {
        BatterySource.EBIKE,BatterySource.TRAINER->SensorIcons.forSensor(if(source==BatterySource.EBIKE) BikeSensor.EBIKE else BikeSensor.TRAINER)
        BatterySource.HEART->SensorIcons.forSensor(BikeSensor.HEART)
        BatterySource.POWER->SensorIcons.forSensor(BikeSensor.POWER)
        BatterySource.PHONE->BicycleIcon.Phone
        BatterySource.SHIFT->BicycleIcon.Derailleur
    }
    fun single(id: String)=when(id) {
        "BATTERY"->BatterySource.EBIKE;"HEART_BATTERY"->BatterySource.HEART
        "SHIFT_BATTERY"->BatterySource.SHIFT;"TRAINER_BATTERY"->BatterySource.TRAINER;"PHONE_BATTERY"->BatterySource.PHONE;else->null
    }
    fun warning(level: Int?)=when { level==null->0;level<10->2;level<20->1;else->0 }
    fun color(level:Int?,foreground:Int,inclusive:Boolean=false):Int {
        val warning=if(inclusive) when { level==null->0;level<=10->2;level<=20->1;else->0 } else warning(level)
        return when(warning) {
            2->if(foreground==android.graphics.Color.BLACK) 0xffa51c1c.toInt() else 0xffff3030.toInt()
            1->if(foreground==android.graphics.Color.BLACK) 0xff8c5700.toInt() else 0xffffb000.toInt()
            else->foreground
        }
    }
    fun description(context: Context, sources: List<BatterySource>, snapshot: BatterySnapshot)=sources.joinToString(" · ") { source ->
        context.getString(R.string.battery_reading,context.getString(source.label),snapshot.level(source)?.let {
            context.getString(R.string.battery_percent,it)
        } ?: context.getString(R.string.ui_field_missing_value))
    }
}

/** One sticky system broadcast subscription while the existing runtime is active; no polling timer. */
internal class PhoneBattery(private val context: Context, private val changed: ()->Unit) {
    var level: Int?=null;private set
    private var registered=false
    private val receiver=object:BroadcastReceiver() {
        override fun onReceive(context: Context,intent: Intent) { update(intent) }
    }
    private fun update(intent: Intent?) {
        val raw=intent?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1) ?: -1
        val scale=intent?.getIntExtra(BatteryManager.EXTRA_SCALE,-1) ?: -1
        val next=if(raw>=0 && scale>0 && raw<=scale) (raw*100L/scale).toInt() else null
        if(level!=next) { level=next;changed() }
    }
    fun start() {
        if(registered) return
        registered=true
        update(context.registerReceiver(receiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED),Context.RECEIVER_NOT_EXPORTED))
    }
    fun stop() { if(registered) { context.unregisterReceiver(receiver);registered=false };level=null }
}
