package de.bcycle.app

import android.content.SharedPreferences

internal data class DrinkReminderConfig(val enabled:Boolean=false,val minutes:Int=30,val durationSeconds:Int=15,val position:MessagePosition=MessagePosition.CENTER)
internal object DrinkReminderSettings {
    const val enabledKey="drinkReminder.enabled"
    const val intervalKey="drinkReminder.minutes"
    const val durationKey="drinkReminder.durationSeconds"
    const val positionKey="drinkReminder.position"
    fun validDuration(seconds:Int)=seconds in 5..60 && seconds%5==0
    fun valid(minutes:Int)=minutes in 10..60 && minutes%5==0
    fun read(prefs:SharedPreferences)=DrinkReminderConfig(prefs.getBoolean(enabledKey,false),
        prefs.getInt(intervalKey,30).takeIf(::valid) ?: 30,
        prefs.getInt(durationKey,15).takeIf(::validDuration) ?: 15,
        runCatching { MessagePosition.valueOf(prefs.getString(positionKey,"CENTER")!!) }.getOrDefault(MessagePosition.CENTER))
    fun save(prefs:SharedPreferences,config:DrinkReminderConfig) {
        require(valid(config.minutes) && validDuration(config.durationSeconds))
        prefs.edit().putBoolean(enabledKey,config.enabled).putInt(intervalKey,config.minutes).putInt(durationKey,config.durationSeconds).putString(positionKey,config.position.name).apply()
    }
}

/** Recording elapsed time only. Hidden dashboards defer one reminder, never build up a queue. */
internal class DrinkReminder {
    private var config:DrinkReminderConfig?=null
    private var nextAt:Long?=null
    private var lastElapsed:Long?=null
    fun reset() { config=null;nextAt=null;lastElapsed=null }
    fun update(recording:Boolean,elapsed:Long,settings:DrinkReminderConfig,visible:Boolean):Boolean {
        if(!recording || !settings.enabled || elapsed<0) { reset();return false }
        val minutes=settings.minutes.takeIf(DrinkReminderSettings::valid) ?: 30
        val normalized=settings.copy(minutes=minutes)
        val interval=minutes*60_000L
        if(config?.minutes!=normalized.minutes || nextAt==null || lastElapsed?.let { elapsed<it }==true) {
            config=normalized;nextAt=elapsed+interval
        }
        lastElapsed=elapsed
        val due=nextAt!!
        if(!visible || elapsed<due) return false
        nextAt=due+((elapsed-due)/interval+1)*interval
        return true
    }
}
