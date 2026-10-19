package de.bcycle.app

import android.content.SharedPreferences

internal data class DrinkReminderConfig(val enabled:Boolean=false,val minutes:Int=30)
internal object DrinkReminderSettings {
    const val enabledKey="drinkReminder.enabled"
    const val intervalKey="drinkReminder.minutes"
    fun valid(minutes:Int)=minutes in 10..60 && minutes%5==0
    fun read(prefs:SharedPreferences)=DrinkReminderConfig(prefs.getBoolean(enabledKey,false),
        prefs.getInt(intervalKey,30).takeIf(::valid) ?: 30)
    fun save(prefs:SharedPreferences,config:DrinkReminderConfig) {
        require(valid(config.minutes))
        prefs.edit().putBoolean(enabledKey,config.enabled).putInt(intervalKey,config.minutes).apply()
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
        if(config!=normalized || nextAt==null || lastElapsed?.let { elapsed<it }==true) {
            config=normalized;nextAt=elapsed+interval
        }
        lastElapsed=elapsed
        val due=nextAt!!
        if(!visible || elapsed<due) return false
        nextAt=due+((elapsed-due)/interval+1)*interval
        return true
    }
}
