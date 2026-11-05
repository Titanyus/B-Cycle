package de.bcycle.app

import android.content.SharedPreferences
import android.graphics.Color
import org.json.JSONObject
import kotlin.math.roundToInt

internal enum class MapLineKind(val id:String,val title:Int) {
    RECORDED("recorded",R.string.map_line_recorded),SAVED("saved",R.string.map_line_saved),
    SAVED_TWO("saved2",R.string.map_line_saved_two),SAVED_THREE("saved3",R.string.map_line_saved_three),
    CYCLEWAYS("cycleways",R.string.map_cycling_ways),CYCLE_ROUTES("cycleRoutes",R.string.map_cycling_routes)
}
internal data class MapLineStyle(val color:Int,val widthDp:Float,val opacity:Int) {
    val argb get()=Color.argb((opacity*255/100f).roundToInt(),Color.red(color),Color.green(color),Color.blue(color))
    fun json()=JSONObject().put("color",color).put("width",widthDp.toDouble()).put("opacity",opacity)
}
internal object MapRouteStyles {
    const val key="mapRouteStylesV1"
    fun default(kind:MapLineKind,night:Boolean=false)=if(kind==MapLineKind.CYCLEWAYS) MapLineStyle(Color.rgb(36,120,196),2.5f,100)
        else if(kind==MapLineKind.CYCLE_ROUTES) MapLineStyle(Color.rgb(170,45,150),2.5f,85)
        else if(kind==MapLineKind.RECORDED)
        MapLineStyle(Color.rgb(220,45,30),3f,100)
        else MapLineStyle(when(kind) {
            MapLineKind.SAVED_TWO->if(night) Color.rgb(255,190,70) else Color.rgb(180,95,0)
            MapLineKind.SAVED_THREE->if(night) Color.rgb(190,120,255) else Color.rgb(115,40,165)
            else->if(night) Color.rgb(90,150,255) else Color.rgb(18,59,117)
        },6f,50)
    fun saved(slot:Int)=listOf(MapLineKind.SAVED,MapLineKind.SAVED_TWO,MapLineKind.SAVED_THREE)[slot]
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
        }.getOrNull() ?: if(kind==MapLineKind.CYCLEWAYS) {
            val old=MapStyleOverrides.read(prefs).objects[MapStyleTarget.CYCLE]
            val baseline=default(kind,night)
            baseline.copy(color=old?.color ?: baseline.color,widthDp=((baseline.widthDp*(old?.widthPercent ?: 100)/100*2).roundToInt()/2f).coerceIn(.5f,8f),opacity=((old?.alpha ?: 255)*100/255))
        } else default(kind,night)
    fun write(prefs:SharedPreferences,kind:MapLineKind,style:MapLineStyle) {
        val row=runCatching { validate(JSONObject(prefs.getString(key,"{}")!!)) }.getOrDefault(JSONObject())
        row.put(kind.id,style.json());validate(row);prefs.edit().putString(key,row.toString()).apply()
    }
}

/** Display-only cycling options share the track editor/storage; keep native zoom-dependent widths. */
internal object MapCyclingStyles {
    fun effective(prefs:SharedPreferences):MapStyleOverrides {
        var value=MapStyleOverrides.read(prefs)
        for((kind,target) in listOf(MapLineKind.CYCLEWAYS to MapStyleTarget.CYCLE,MapLineKind.CYCLE_ROUTES to MapStyleTarget.CYCLE_ROUTES)) {
            val line=MapRouteStyles.read(prefs,kind)
            value=value.update(target,MapObjectStyle(line.color,alpha=Color.alpha(line.argb),widthPercent=(line.widthDp/2.5f*100).roundToInt()))
        }
        return value
    }
}
