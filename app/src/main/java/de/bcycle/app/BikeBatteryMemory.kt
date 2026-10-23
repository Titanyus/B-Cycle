package de.bcycle.app

import android.content.SharedPreferences

/** Display-only last known charge, isolated by portable profile identity; never a ride sample. */
internal class BikeBatteryMemory(private val prefs:SharedPreferences) {
    fun read(id:String):Long?=if(prefs.contains(id)) prefs.getLong(id,-1).takeIf { it in 0..100 } else null
    fun remember(id:String,value:Long?) {
        if(value==null || value !in 0..100 || read(id)==value) return
        prefs.edit().putLong(id,value).apply()
    }
}
