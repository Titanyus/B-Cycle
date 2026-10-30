package de.bcycle.app

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Three stable style/number slots. FIFO age changes only when a new route is assigned. */
internal data class MapRouteSlots(val ids:List<String?> = List(COUNT) { null },val visible:List<Boolean> = List(COUNT) { true },val order:List<Int> = emptyList()) {
    init {
        require(ids.size==COUNT && visible.size==COUNT)
        require(ids.filterNotNull().let { it.distinct().size==it.size && it.all(::validId) })
        require(order.distinct().size==order.size && order.all { it in 0 until COUNT } && order.toSet()==ids.indices.filter { ids[it]!=null }.toSet())
    }
    fun slot(id:String)=ids.indexOf(id).takeIf { it>=0 }
    fun add(id:String):MapRouteSlots {
        require(validId(id))
        slot(id)?.let { return copy(visible=visible.mapIndexed { index,on -> on || index==it }) }
        val index=ids.indexOf(null).takeIf { it>=0 } ?: order.first()
        return copy(ids=ids.mapIndexed { i,old -> if(i==index) id else old },visible=visible.mapIndexed { i,on -> on || i==index },order=order.filterNot { it==index }+index)
    }
    fun assign(id:String,index:Int):MapRouteSlots {
        require(validId(id) && index in 0 until COUNT)
        if(slot(id)==index) return show(index,true)
        val free=remove(id)
        return free.copy(ids=free.ids.mapIndexed { i,old -> if(i==index) id else old },
            visible=free.visible.mapIndexed { i,on -> on || i==index },order=free.order.filterNot { it==index }+index)
    }
    fun remove(id:String):MapRouteSlots {
        val index=slot(id) ?: return this
        return copy(ids=ids.mapIndexed { i,old -> if(i==index) null else old },order=order.filterNot { it==index })
    }
    fun show(index:Int,on:Boolean):MapRouteSlots { require(index in 0 until COUNT);return copy(visible=visible.mapIndexed { i,old -> if(i==index) on else old }) }
    fun all(on:Boolean)=copy(visible=List(COUNT) { on })
    fun json()=JSONObject().put("version",1).put("ids",JSONArray(ids.map { it ?: JSONObject.NULL })).put("visible",JSONArray(visible)).put("order",JSONArray(order))
    fun write(prefs:SharedPreferences) { prefs.edit().putString(KEY,json().toString()).apply() }
    companion object {
        const val COUNT=3
        const val KEY="mapRouteSlotsV1"
        fun validId(id:String)=id.matches(Regex("(recorded|imported)-[a-f0-9]{64}|[a-f0-9-]{36}"))
        fun parse(row:JSONObject):MapRouteSlots {
            require(row.get("version") is Number && row.getDouble("version")==1.0)
            val ids=row.getJSONArray("ids");val shown=row.getJSONArray("visible");val order=row.getJSONArray("order")
            require(ids.length()==COUNT && shown.length()==COUNT && order.length()<=COUNT)
            return MapRouteSlots((0 until COUNT).map { if(ids.isNull(it)) null else ids.getString(it) },
                (0 until COUNT).map { require(shown.get(it) is Boolean);shown.getBoolean(it) },
                (0 until order.length()).map { require(order.get(it) is Number && order.getDouble(it)==order.getInt(it).toDouble());order.getInt(it) })
        }
        fun read(prefs:SharedPreferences)=runCatching { parse(JSONObject(prefs.getString(KEY,"{}")!!)) }.getOrDefault(MapRouteSlots())
    }
}
internal data class DisplayedMapRoute(val slot:Int,val id:String,val route:GpxRoute)
internal data class StyledMapRoute(val route:GpxRoute,val style:MapLineStyle)
