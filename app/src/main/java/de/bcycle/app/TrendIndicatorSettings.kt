package de.bcycle.app

import android.content.SharedPreferences

/** Global presentation choices; never stop the shared trend calculation. */
internal data class TrendIndicatorConfig(val enabled:Boolean=true,val disabled:Set<String> = emptySet()) {
    fun shows(field:DashboardField)=enabled && AverageTrends.supports(field) && field.id !in disabled
    fun select(id:String,on:Boolean)=copy(disabled=if(on) disabled-id else disabled+id)
}
internal object TrendIndicatorSettings {
    const val enabledKey="trendIndicatorsEnabled"
    const val disabledKey="trendIndicatorsDisabledFields"
    val fields get()=AverageTrends.currentFields+AverageTrends.averageFields
    fun read(prefs:SharedPreferences):TrendIndicatorConfig {
        val disabled=if(prefs.contains(disabledKey)) prefs.getStringSet(disabledKey,emptySet()).orEmpty().intersect(fields.toSet()) else {
            // Retain old choices by field. If any previous occurrence was on, keep it on globally.
            val occurrences=mutableMapOf<String,MutableList<Boolean>>()
            for(page in 0 until DashboardPages.MAX) {
                if(!prefs.contains(DashboardTrendIndicators.key(page))) continue
                val layout=DashboardLayout.decode(prefs.getString(DashboardPages.fieldKey(page),null))
                val flags=DashboardTrendIndicators.read(prefs,page)
                layout.fields.forEachIndexed { slot,field -> if(AverageTrends.supports(field))
                    occurrences.getOrPut(field.id) { mutableListOf() }.add(flags.enabled[slot]) }
            }
            occurrences.filterValues { values -> values.none { it } }.keys.toSet()
        }
        return TrendIndicatorConfig(prefs.getBoolean(enabledKey,true),disabled)
    }
    fun save(prefs:SharedPreferences,value:TrendIndicatorConfig) {
        prefs.edit().putBoolean(enabledKey,value.enabled).putStringSet(disabledKey,value.disabled).apply()
    }
}
