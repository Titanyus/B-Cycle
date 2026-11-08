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
    private val savedKinds=listOf(MapLineKind.SAVED,MapLineKind.SAVED_TWO,MapLineKind.SAVED_THREE)
    fun saved(slot:Int)=savedKinds[slot]
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
    private data class ReadKey(val styles:String?,val legacy:String?,val night:Boolean)
    private val snapshots=object:LinkedHashMap<ReadKey,Map<MapLineKind,MapLineStyle>>(8,.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<ReadKey,Map<MapLineKind,MapLineStyle>>?)=size>8
    }
    /** Raw values identify the snapshot, so profile switches, restore and direct edits cannot go stale. */
    @Synchronized fun read(prefs:SharedPreferences,kind:MapLineKind,night:Boolean=prefs.getBoolean("mapNight",false)):MapLineStyle {
        val key=ReadKey(prefs.getString(this.key,null),prefs.getString(MapStyleOverrides.KEY,null),night)
        val snapshot=snapshots.getOrPut(key) {
            val row=runCatching { validate(JSONObject(key.styles ?: "{}")) }.getOrNull()
            val legacy=runCatching { MapStyleOverrides.parse(JSONObject(key.legacy ?: "{}")) }.getOrDefault(MapStyleOverrides()).objects[MapStyleTarget.CYCLE]
            MapLineKind.entries.associateWith { entry ->
                val stored=row?.optJSONObject(entry.id)
                if(stored!=null) MapLineStyle(stored.getInt("color") or Color.BLACK,stored.getDouble("width").toFloat(),stored.getInt("opacity"))
                else if(entry==MapLineKind.CYCLEWAYS) {
                    val baseline=default(entry,night)
                    baseline.copy(color=legacy?.color ?: baseline.color,
                        widthDp=((baseline.widthDp*(legacy?.widthPercent ?: 100)/100*2).roundToInt()/2f).coerceIn(.5f,8f),
                        opacity=((legacy?.alpha ?: 255)*100/255))
                } else default(entry,night)
            }
        }
        return snapshot.getValue(kind)
    }
    fun write(prefs:SharedPreferences,kind:MapLineKind,style:MapLineStyle) {
        val row=runCatching { validate(JSONObject(prefs.getString(key,"{}")!!)) }.getOrDefault(JSONObject())
        row.put(kind.id,style.json());validate(row);prefs.edit().putString(key,row.toString()).apply()
    }
}

/** Display-only cycling options share the track editor/storage; keep native zoom-dependent widths. */
internal object MapCyclingStyles {
    private val targets=listOf(MapLineKind.CYCLEWAYS to MapStyleTarget.CYCLE,MapLineKind.CYCLE_ROUTES to MapStyleTarget.CYCLE_ROUTES)
    fun effective(prefs:SharedPreferences):MapStyleOverrides {
        var value=MapStyleOverrides.read(prefs)
        for((kind,target) in targets) {
            val line=MapRouteStyles.read(prefs,kind)
            value=value.update(target,MapObjectStyle(line.color,alpha=Color.alpha(line.argb),widthPercent=(line.widthDp/2.5f*100).roundToInt()))
        }
        return value
    }
}
