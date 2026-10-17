package de.bcycle.app

import android.content.SharedPreferences
import org.json.JSONObject

internal enum class TrainingHelpMode { OFF, ALL_ZONES, ZONE_TWO }

/** Dashboard-wide presentation policy; no separate sensor processing or ride statistics. */
internal data class DashboardTrainingHelp(val mode:TrainingHelpMode=TrainingHelpMode.OFF,
    val current:Boolean=true,val average:Boolean=false) {
    fun sources(layout:DashboardLayout,visible:DashboardDataVisibility)=ZoneGlowConfig(layout.fields.mapIndexed { i,field ->
        if(mode==TrainingHelpMode.OFF || !visible.visible[i]) ZoneGlowSource.OFF else when {
            current && field.id in currentPower -> ZoneGlowSource.POWER_CURRENT
            current && field.id in currentHeart -> ZoneGlowSource.HEART_CURRENT
            average && field.id=="AVERAGE_POWER" -> ZoneGlowSource.POWER_AVERAGE
            average && field.id=="HEART_AVERAGE" -> ZoneGlowSource.HEART_AVERAGE
            else -> ZoneGlowSource.OFF
        }
    })
    fun encode()=JSONObject().put("mode",mode.name).put("current",current).put("average",average).toString()
    companion object {
        private val currentPower=setOf("RIDER_POWER","TRAINING_POWER_ZONE","TRAINING_FTP_PERCENT")
        private val currentHeart=setOf("HEART_RATE","TRAINING_HEART_ZONE","TRAINING_HRR_PERCENT")
        fun supports(field:DashboardField)=field.id in currentPower || field.id in currentHeart || field.id in setOf("AVERAGE_POWER","HEART_AVERAGE")
        fun key(page:Int)="dashboardTrainingHelp_$page"
        fun decode(text:String?):DashboardTrainingHelp?=runCatching {
            val json=JSONObject(text ?: return null)
            require(json.opt("mode") is String && json.opt("current") is Boolean && json.opt("average") is Boolean)
            DashboardTrainingHelp(TrainingHelpMode.valueOf(json.getString("mode")),json.getBoolean("current"),json.getBoolean("average"))
        }.getOrNull()
        fun read(prefs:SharedPreferences,page:Int):DashboardTrainingHelp {
            decode(prefs.getString(key(page),null))?.let { return it }
            val legacy=ZoneGlowConfig.read(prefs,page).sources
            val current=legacy.any { it==ZoneGlowSource.POWER_CURRENT || it==ZoneGlowSource.HEART_CURRENT }
            val average=legacy.any { it==ZoneGlowSource.POWER_AVERAGE || it==ZoneGlowSource.HEART_AVERAGE }
            return if(current || average) DashboardTrainingHelp(TrainingHelpMode.ALL_ZONES,current,average) else DashboardTrainingHelp()
        }
    }
}

/** Lower tolerance is intentionally much wider than the upper warning boundary. Percent points. */
internal object ZoneTwoHelpColors {
    val green=TrainingZoneColors.values[1]
    val red=TrainingZoneColors.values[4]
    fun color(percent:Double,lower:Double,upper:Double):Int {
        val lowerBlend=((percent-(lower-20))/10).coerceIn(0.0,1.0)
        val upperBlend=((upper+.5-percent)/1.0).coerceIn(0.0,1.0)
        val t=minOf(lowerBlend,upperBlend).toFloat()
        return ZoneGlowColors.mix(red,green,t*t*(3-2*t))
    }
}
