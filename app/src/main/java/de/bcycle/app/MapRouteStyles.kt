package de.bcycle.app

import android.content.SharedPreferences
import android.graphics.Color
import org.json.JSONObject
import kotlin.math.roundToInt

internal enum class MapLineKind(val id:String,val title:Int) {
    RECORDED("recorded",R.string.map_line_recorded),SAVED("saved",R.string.map_line_saved)
}
internal data class MapLineStyle(val color:Int,val widthDp:Float,val opacity:Int) {
    val argb get()=Color.argb((opacity*255/100f).roundToInt(),Color.red(color),Color.green(color),Color.blue(color))
    fun json()=JSONObject().put("color",color).put("width",widthDp.toDouble()).put("opacity",opacity)
}
internal object MapRouteStyles {
    const val key="mapRouteStylesV1"
    fun default(kind:MapLineKind,night:Boolean=false)=if(kind==MapLineKind.RECORDED)
        MapLineStyle(Color.rgb(220,45,30),3f,100)
        else MapLineStyle(if(night) Color.rgb(90,150,255) else Color.rgb(18,59,117),6f,50)
    fun validate(row:JSONObject):JSONObject {
        MapLineKind.entries.forEach { kind -> if(row.has(kind.id)) {
            val v=row.getJSONObject(kind.id)
            val width=v.getDouble("width");val color=v.getLong("color");val opacity=v.getDouble("opacity")
            require(width.isFinite() && width in .5..8.0 && width*2==(width*2).toInt().toDouble())
            require(color in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
            require(opacity in 0.0..100.0 && opacity==opacity.toInt().toDouble())
        } }
        return row
    }
    fun read(prefs:SharedPreferences,kind:MapLineKind,night:Boolean=prefs.getBoolean("mapNight",false)):MapLineStyle =
        runCatching {
            val row=JSONObject(prefs.getString(key,"{}")!!);validate(row)
            row.optJSONObject(kind.id)?.let { MapLineStyle(it.getInt("color") or Color.BLACK,it.getDouble("width").toFloat(),it.getInt("opacity")) }
        }.getOrNull() ?: default(kind,night)
    fun write(prefs:SharedPreferences,kind:MapLineKind,style:MapLineStyle) {
        val row=runCatching { validate(JSONObject(prefs.getString(key,"{}")!!)) }.getOrDefault(JSONObject())
        row.put(kind.id,style.json());validate(row);prefs.edit().putString(key,row.toString()).apply()
    }
}
