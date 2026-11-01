package de.bcycle.app

import java.time.LocalDate
import java.time.ZoneId

internal enum class RouteKind(val label:Int) {
    IMPORTED(R.string.ui_routes_filter_imported),RECORDED(R.string.ui_routes_filter_recorded);
    companion object {
        fun of(route:ManagedRoute)=if(route.origin==TrackOrigin.IMPORTED) IMPORTED else RECORDED
    }
}
internal enum class RouteSort(val label:Int) {
    DATE(R.string.route_filter_date),DISTANCE(R.string.route_filter_distance),MOVING(R.string.route_filter_moving),SPEED(R.string.route_filter_speed);
    fun value(route:ManagedRoute):Double?=when(this) {
        DATE->route.details.dateMs.takeIf { it>0 }?.toDouble()
        DISTANCE->route.details.distanceM
        MOVING->route.details.movingMs?.toDouble()
        SPEED->route.details.movingAverageKmh
    }?.takeIf { it.isFinite() && it>=0 }
}
internal data class RouteRange(val from:Double?=null,val to:Double?=null) {
    val active get()=from!=null || to!=null
    fun accepts(value:Double?):Boolean = !active || (value!=null && value.isFinite() &&
        (from==null || value>=from) && (to==null || value<=to))
}
internal data class RouteFilterAvailability(val kinds:Set<RouteKind>,val bikeTypes:Set<BikeType>,val bikes:Set<Int>)
internal class RouteQuery(val scope:RouteKind?=null) {
    var estimateKmh:Double?=null
    val templates get()=scope==RouteKind.IMPORTED
    fun value(sort:RouteSort,route:ManagedRoute):Double? = if(templates && sort==RouteSort.MOVING)
        RecentRidePace.duration(route.details.distanceM,estimateKmh)?.toDouble() else sort.value(route)
    fun save(preferences:android.content.SharedPreferences) {
        val collection=scope ?: return
        val json=org.json.JSONObject().put("displayed",displayedOnly).put("from",fromDate?.toString()).put("to",toDate?.toString())
            .put("types",org.json.JSONArray(bikeTypes.map { it.name })).put("bikes",org.json.JSONArray(bikes.toList()))
            .put("sort",sort.name).put("descending",descending)
        fun range(key:String,r:RouteRange) { json.put(key,org.json.JSONObject().put("from",r.from).put("to",r.to)) }
        range("distance",distance);range("moving",moving);range("speed",speed)
        preferences.edit().putString("routeQuery.${collection.name}.v1",json.toString()).apply()
    }
    fun restore(preferences:android.content.SharedPreferences) {
        val collection=scope ?: return
        runCatching {
            val json=org.json.JSONObject(preferences.getString("routeQuery.${collection.name}.v1",null) ?: return)
            displayedOnly=json.optBoolean("displayed",false)
            fromDate=runCatching { LocalDate.parse(json.optString("from")) }.getOrNull()
            toDate=runCatching { LocalDate.parse(json.optString("to")) }.getOrNull()
            bikeTypes=buildSet { val a=json.optJSONArray("types");if(a!=null) repeat(a.length()) { i -> BikeType.entries.firstOrNull { it.name==a.optString(i) }?.let(::add) } }
            bikes=buildSet { val a=json.optJSONArray("bikes");if(a!=null) repeat(a.length()) { i -> a.optInt(i,-2).takeIf { it>=-1 }?.let(::add) } }
            sort=RouteSort.entries.firstOrNull { it.name==json.optString("sort") } ?: RouteSort.DATE
            if(templates && sort==RouteSort.SPEED) sort=RouteSort.DATE
            descending=json.optBoolean("descending",true)
            fun range(key:String):RouteRange {
                val row=json.optJSONObject(key) ?: return RouteRange()
                val from=row.optDouble("from").takeIf { it.isFinite() && it>=0 };val to=row.optDouble("to").takeIf { it.isFinite() && it>=0 }
                return if(from!=null && to!=null && from>to) RouteRange() else RouteRange(from,to)
            }
            distance=range("distance");moving=range("moving");speed=if(templates) RouteRange() else range("speed")
        }
    }
    private enum class Facet { ORIGIN,TYPE,PROFILE }
    fun availability(entries:List<ManagedRoute>,profile:(String)->Int?,zone:ZoneId=ZoneId.systemDefault(),displayedId:String?=null,displayedIds:Set<String> = setOfNotNull(displayedId))=RouteFilterAvailability(
        filtered(entries,profile,zone,Facet.ORIGIN,displayedIds).map { RouteKind.of(it) }.toSet(),
        filtered(entries,profile,zone,Facet.TYPE,displayedIds).mapNotNull { it.details.bikeType }.toSet(),
        filtered(entries,profile,zone,Facet.PROFILE,displayedIds).map { profile(it.id) ?: -1 }.toSet())
    var kinds=setOf<RouteKind>();var bikeTypes=setOf<BikeType>();var bikes=setOf<Int>() // -1 = no recorded profile assignment
    var displayedOnly=false
    var fromDate:LocalDate?=null;var toDate:LocalDate?=null
    var distance=RouteRange();var moving=RouteRange();var speed=RouteRange()
    var sort=RouteSort.DATE;var descending=true
    val activeCount get()=listOf(displayedOnly,(scope==null && kinds.isNotEmpty()),bikeTypes.isNotEmpty(),bikes.isNotEmpty(),fromDate!=null || toDate!=null,distance.active,moving.active,(!templates && speed.active)).count { it }
    fun clearFilters() { displayedOnly=false;kinds=emptySet();bikeTypes=emptySet();bikes=emptySet();fromDate=null;toDate=null;distance=RouteRange();moving=RouteRange();speed=RouteRange() }
    private fun filtered(entries:List<ManagedRoute>,profile:(String)->Int?,zone:ZoneId,ignore:Facet?=null,displayedIds:Set<String> = emptySet()):List<ManagedRoute> {
        val from=fromDate?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
        val until=toDate?.plusDays(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
        return entries.filter { route ->
            (scope==null || RouteKind.of(route)==scope) &&
                (!displayedOnly || route.id in displayedIds) &&
                (scope!=null || ignore==Facet.ORIGIN || kinds.isEmpty() || RouteKind.of(route) in kinds) &&
                (ignore==Facet.TYPE || bikeTypes.isEmpty() || route.details.bikeType in bikeTypes) &&
                (ignore==Facet.PROFILE || bikes.isEmpty() || (profile(route.id) ?: -1) in bikes) &&
                ((from==null && until==null) || (route.details.dateMs>0 && (from==null || route.details.dateMs>=from) && (until==null || route.details.dateMs<until))) &&
                distance.accepts(route.details.distanceM?.div(1000)) && moving.accepts(value(RouteSort.MOVING,route)?.div(3600000.0)) && (templates || speed.accepts(route.details.movingAverageKmh))
        }
    }
    fun apply(entries:List<ManagedRoute>,profile:(String)->Int?,zone:ZoneId=ZoneId.systemDefault(),displayedId:String?=null,displayedIds:Set<String> = setOfNotNull(displayedId)):List<ManagedRoute> =
        filtered(entries,profile,zone,displayedIds=displayedIds).sortedWith { a,b ->
            val av=value(sort,a);val bv=value(sort,b)
            val order=when { av==null && bv==null->0;av==null->1;bv==null->-1;else->av.compareTo(bv)*(if(descending) -1 else 1) }
            if(order!=0) order else compareValues(b.details.dateMs,a.details.dateMs).takeIf { it!=0 } ?: a.id.compareTo(b.id)
        }
    companion object {
        fun validateStored(text:String) {
            val json=BackupJson.objectValue(text.toByteArray(),8192)
            require(json.keys().asSequence().all { it in setOf("displayed","from","to","types","bikes","sort","descending","distance","moving","speed") })
            for(key in listOf("displayed","descending")) if(json.has(key)) require(json.get(key) is Boolean)
            for(key in listOf("from","to")) if(json.has(key)) LocalDate.parse(json.getString(key))
            require(RouteSort.entries.any { it.name==json.getString("sort") })
            val types=json.getJSONArray("types");require(types.length()<=BikeType.entries.size)
            repeat(types.length()) { at -> require(BikeType.entries.any { it.name==types.getString(at) }) }
            val bikes=json.getJSONArray("bikes");require(bikes.length()<=128)
            repeat(bikes.length()) { at -> require(bikes.get(at) is Int && bikes.getInt(at)>=-1) }
            for(key in listOf("distance","moving","speed")) {
                val row=json.getJSONObject(key)
                require(row.keys().asSequence().all { it in setOf("from","to") })
                for(edge in listOf("from","to")) if(row.has(edge)) require(row.get(edge) is Number && row.getDouble(edge).let { it.isFinite() && it>=0 })
                if(row.has("from") && row.has("to")) require(row.getDouble("from")<=row.getDouble("to"))
            }
        }
    }

}
