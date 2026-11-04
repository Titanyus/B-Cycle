package de.bcycle.app

import android.content.SharedPreferences
import org.oscim.core.TagSet

/** Only visual source layers; recorded routes, GPS and sensor data are independent. */
internal enum class MapVectorLayer(val title:Int,val defaultVisible:Boolean=true) {
    MAIN_ROADS(R.string.map_layer_main_roads),
    MINOR_ROADS(R.string.map_layer_minor_roads),
    PATHS(R.string.map_layer_paths),
    BUILDINGS(R.string.map_layer_buildings),
    NATURE(R.string.map_layer_nature),
    WATER(R.string.map_layer_water),
    PLACE_NAMES(R.string.map_layer_place_names),
    ROAD_NAMES(R.string.map_layer_road_names),
    POIS(R.string.map_layer_pois,false),
    LOCAL_BOUNDARIES(R.string.map_layer_local_boundaries,false),
    CONTOURS(R.string.map_layer_contours,false),
    ADMIN_AREAS(R.string.map_layer_admin_areas);
    val key get()="mapVectorLayer_$name"
}
internal data class MapVectorLayers(val visible:Set<MapVectorLayer> = MapVectorLayer.entries.filter { it.defaultVisible }.toSet()) {
    operator fun contains(layer:MapVectorLayer)=layer in visible
    fun accepts(tags:TagSet):Boolean {
        val source=tags.getValue("layer")
        val category=when(source) {
            "poi","aerodrome_label" -> MapVectorLayer.POIS
            "building" -> MapVectorLayer.BUILDINGS
            "landcover","landuse","park" -> MapVectorLayer.NATURE
            "water","waterway","water_name" -> MapVectorLayer.WATER
            "place" -> MapVectorLayer.PLACE_NAMES
            "boundary" -> when(tags.getValue("admin_level")?.toIntOrNull()) {
                7 -> MapVectorLayer.ADMIN_AREAS
                in 8..99 -> MapVectorLayer.LOCAL_BOUNDARIES
                else -> null
            }
            else -> null
        }
        if(category!=null && category !in this) return false
        if(source=="transportation" || source=="transportation_name") {
            if(source=="transportation_name" && MapVectorLayer.ROAD_NAMES !in this) return false
            val road=when(tags.getValue("class")?.removeSuffix("_link")) {
                "motorway","trunk","primary","secondary","tertiary" -> MapVectorLayer.MAIN_ROADS
                "minor","minor_road","residential","unclassified","service" -> MapVectorLayer.MINOR_ROADS
                "track","path","footway","cycleway","pedestrian","steps","bridleway" -> MapVectorLayer.PATHS
                else -> null
            }
            if(road!=null && road !in this) return false
        }
        return true
    }
    companion object {
        fun read(prefs:SharedPreferences)=MapVectorLayers(MapVectorLayer.entries.filter { prefs.getBoolean(it.key,it.defaultVisible) }.toSet())
    }
}
