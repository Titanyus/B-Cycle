package de.bcycle.app

import android.content.SharedPreferences
import android.graphics.Color
import org.json.JSONObject

internal enum class MapDash(val title:Int,val pattern:String?) {
    ORIGINAL(R.string.map_style_original,null),SOLID(R.string.map_style_solid,""),DASHED(R.string.map_style_dashed,"12,8"),
    DOTTED(R.string.map_style_dotted,"1,4"),DASH_DOT(R.string.map_style_dash_dot,"12,8,1,8")
}
internal enum class MapStyleTarget(val title:Int,val line:Boolean,val ids:Set<String>) {
    BACKGROUND(R.string.map_style_background,false,setOf("background")),
    FOREST(R.string.map_style_forest,false,setOf("landcover-wood")),
    GRASS(R.string.map_style_grass,false,setOf("landcover-grass")),
    PARK(R.string.map_style_park,false,setOf("landcover-grass-park")),
    RESIDENTIAL(R.string.map_style_residential,false,setOf("landuse-residential")),
    COMMERCIAL(R.string.map_style_commercial,false,setOf("landuse-commercial")),
    INDUSTRIAL(R.string.map_style_industrial,false,setOf("landuse-industrial")),
    CEMETERY(R.string.map_style_cemetery,false,setOf("landuse-cemetery")),
    HOSPITAL(R.string.map_style_hospital,false,setOf("landuse-hospital")),
    SCHOOL(R.string.map_style_school,false,setOf("landuse-school")),
    SAND(R.string.map_style_sand,false,setOf("landcover-sand")),
    ICE(R.string.map_style_ice,false,setOf("landcover-glacier","landcover-ice-shelf")),
    WATER(R.string.map_layer_water,false,setOf("water","water-intermittent")),
    RIVER(R.string.map_style_river,true,setOf("waterway-river","waterway-river-intermittent")),
    STREAM(R.string.map_style_stream,true,setOf("waterway-stream-canal","waterway-stream-canal-intermittent","waterway-other","waterway-other-intermittent","waterway_tunnel")),
    MOTORWAY(R.string.map_style_motorway,true,setOf("highway-motorway","highway-motorway-link","bridge-motorway","bridge-motorway-link","tunnel-motorway")),
    PRIMARY(R.string.map_style_primary,true,setOf("highway-primary","highway-trunk","bridge-trunk-primary","tunnel-trunk-primary")),
    SECONDARY(R.string.map_style_secondary,true,setOf("highway-secondary-tertiary","bridge-secondary-tertiary","tunnel-secondary-tertiary")),
    MINOR(R.string.map_layer_minor_roads,true,setOf("highway-minor","highway-link","bridge-link","tunnel-minor","tunnel-service-track")),
    PATH(R.string.map_style_paths,true,setOf("highway-path","bridge-path","tunnel-path")),
    CYCLE(R.string.map_vector_cycleways,true,setOf("bcycle-cycleway")),
    RAIL(R.string.map_style_rail,true,setOf("railway","railway-service","railway-transit","bridge-railway","tunnel-railway")),
    BUILDING(R.string.map_layer_buildings,false,setOf("building")),
    BOUNDARY(R.string.map_style_boundary,true,setOf("boundary-land-level-4","boundary-land-level-2","boundary-land-disputed","boundary-water")),
    PLACE_TEXT(R.string.map_layer_place_names,false,setOf("place-other","place-village","place-town","place-city","place-city-capital","place-country-other","place-country-3","place-country-2","place-country-1","place-continent")),
    ROAD_TEXT(R.string.map_layer_road_names,false,setOf("highway-name-path","highway-name-minor","highway-name-major","highway-shield","highway-shield-us-interstate","highway-shield-us-other")),
    WATER_TEXT(R.string.map_style_water_text,false,setOf("waterway-name","water-name-lakeline","water-name-ocean","water-name-other")),
    CONTOUR(R.string.map_layer_contours,true,setOf("contour-minor","contour-major")),
    TRACK(R.string.map_style_tracks,true,setOf("bcycle-track","bcycle-track-tunnel")),
    CYCLE_ROUTES(R.string.map_cycling_routes,true,setOf("bcycle-cycle-route"));
    val outlined get()=this==MOTORWAY || this==PRIMARY || this==SECONDARY || this==MINOR
}
internal data class MapOutlineStyle(val color:Int?=null,val alpha:Int?=null,val widthPercent:Int?=null)
internal data class MapObjectStyle(val color:Int?=null,val dash:MapDash=MapDash.ORIGINAL,val alpha:Int?=null,val widthPercent:Int?=null,val outline:MapOutlineStyle?=null)
internal data class MapStyleOverrides(val objects:Map<MapStyleTarget,MapObjectStyle> = emptyMap(),val placeLabels:Map<MapPlaceClass,MapPlaceTextStyle> = emptyMap()) {
    /** Text is baked into glyph atlases; pattern changes create native textures/buckets. */
    fun canUpdateColorsFrom(previous:MapStyleOverrides)=placeLabels==previous.placeLabels && MapStyleTarget.entries.all { target ->
        val before=previous.objects[target] ?: MapObjectStyle()
        val after=objects[target] ?: MapObjectStyle()
        before.dash==after.dash && before.widthPercent==after.widthPercent && before.outline?.widthPercent==after.outline?.widthPercent && (target !in textTargets || before.color==after.color && before.alpha==after.alpha)
    }
    /** Keep unmodified native geometry paints as exact per-zoom reset baselines. */
    fun compiledStyles()=copy(objects=objects.mapValues { (target,value) ->
        if(target in textTargets) value else value.copy(color=null,alpha=null,outline=value.outline?.copy(color=null,alpha=null)?.takeUnless { it==MapOutlineStyle() })
    }.filterValues { it!=MapObjectStyle() })
    fun json()=JSONObject().put("dashPattern",org.json.JSONArray().put(MapRoadSurface.dashLength).put(MapRoadSurface.gapLength))
        .put("placeTextHalo",JSONObject().put("widthScale",MapPlaceHalo.widthScale).put("opacity",MapPlaceHalo.opacity)).put("roadSurfaceStyle",MapRoadSurface.policy).put("format","bcycle-map-style").put("version",1).put("base","bright")
        .put("baseSha256",BASE_SHA).put("objects",JSONObject().apply {
            objects.toSortedMap(compareBy { it.name }).forEach { (target,style) -> put(target.name,JSONObject().apply {
                style.color?.let { put("color",String.format(java.util.Locale.ROOT,"#%06X",it and 0xffffff)) }
                style.alpha?.let { put("alpha",it) }
                style.widthPercent?.let { put("widthPercent",it) }
                style.outline?.let { outline -> put("outline",JSONObject().apply {
                    outline.color?.let { put("color",String.format(java.util.Locale.ROOT,"#%06X",it and 0xffffff)) }
                    outline.alpha?.let { put("alpha",it) };outline.widthPercent?.let { put("widthPercent",it) }
                }) }
                if(style.dash!=MapDash.ORIGINAL) put("lineStyle",style.dash.name)
            }) }
        })
        .apply { if(placeLabels.isNotEmpty()) put("placeLabels",JSONObject().apply {
            placeLabels.forEach { (type,value) -> put(type.name,JSONObject().put("weight",value.weight).put("sizePercent",value.sizePercent)) }
        }) }
    fun write(prefs:SharedPreferences) { prefs.edit().putString(KEY,json().toString()).apply() }
    fun update(target:MapStyleTarget,value:MapObjectStyle?)=copy(objects=objects.toMutableMap().apply {
        if(value==null || value==MapObjectStyle()) remove(target) else put(target,value)
    })
    fun export(prefs:SharedPreferences)=json().put("visibility",JSONObject().apply {
        MapVectorLayer.entries.forEach { put(it.name,prefs.getBoolean(it.key,it.defaultVisible)) }
    }).put("cycleways",prefs.getBoolean(VtmBaseMap.cyclewaysKey,true)).put("cyclingStyles",JSONObject().apply {
        for(kind in listOf(MapLineKind.CYCLEWAYS,MapLineKind.CYCLE_ROUTES)) put(kind.id,MapRouteStyles.read(prefs,kind).json())
    }).toString(2)
    companion object {
        private val textTargets=setOf(MapStyleTarget.PLACE_TEXT,MapStyleTarget.ROAD_TEXT,MapStyleTarget.WATER_TEXT)
        const val KEY="mapStyleOverridesV1"
        const val BASE_SHA="cda8931095c5101cbad5d2a60fa129600a1a0ad3dea27d802be341fb5eae071f"
        fun read(prefs:SharedPreferences)=runCatching { parse(JSONObject(prefs.getString(KEY,null) ?: return MapStyleOverrides())) }.getOrDefault(MapStyleOverrides())
        fun parse(json:JSONObject):MapStyleOverrides {
            require(json.toString().length<=65536)
            require(json.keys().asSequence().all { it in setOf("format","version","base","baseSha256","objects","visibility","cycleways","placeLabels","cyclingStyles","roadSurfaceStyle","dashPattern","placeTextHalo") })
            require(json.getString("format")=="bcycle-map-style" && json.get("version")==1 && json.getString("base")=="bright" && json.getString("baseSha256")==BASE_SHA)
            json.optJSONObject("visibility")?.let { v -> v.keys().forEach { require(MapVectorLayer.entries.any { layer -> layer.name==it } && v.get(it) is Boolean) } }
            if(json.has("visibility")) require(json.get("visibility") is JSONObject)
            if(json.has("cycleways")) require(json.get("cycleways") is Boolean)
            if(json.has("cyclingStyles")) {
                val cycling=json.getJSONObject("cyclingStyles")
                require(cycling.keys().asSequence().all { it in setOf("cycleways","cycleRoutes") })
                MapRouteStyles.validate(cycling)
            }
            if(json.has("roadSurfaceStyle")) require(json.get("roadSurfaceStyle")==MapRoadSurface.policy)
            if(json.has("dashPattern")) {
                val pattern=json.getJSONArray("dashPattern");require(pattern.length()==2 && pattern.get(0)==MapRoadSurface.dashLength && pattern.get(1)==MapRoadSurface.gapLength)
            }
            if(json.has("placeTextHalo")) {
                val halo=json.getJSONObject("placeTextHalo")
                require(halo.keys().asSequence().toSet()==setOf("widthScale","opacity"))
                require(halo.getDouble("widthScale")==MapPlaceHalo.widthScale.toDouble() && halo.getDouble("opacity")==MapPlaceHalo.opacity.toDouble())
            }
            val labels=buildMap<MapPlaceClass,MapPlaceTextStyle> {
                if(json.has("placeLabels")) {
                    val entries=json.getJSONObject("placeLabels")
                    entries.keys().forEach { key ->
                        val type=MapPlaceClass.valueOf(key);val row=entries.getJSONObject(key)
                        require(row.keys().asSequence().toSet()==setOf("weight","sizePercent"))
                        val weight=row.get("weight");val size=row.get("sizePercent")
                        require(weight is Int && weight in 100..900 && weight%100==0)
                        require(size is Int && size in 50..200)
                        put(type,MapPlaceTextStyle(weight,size))
                    }
                }
            }
            val rows=json.getJSONObject("objects")
            return MapStyleOverrides(buildMap {
                rows.keys().forEach { key ->
                    val target=MapStyleTarget.valueOf(key);val row=rows.getJSONObject(key)
                    require(row.keys().asSequence().all { it in setOf("color","lineStyle","alpha","widthPercent","outline") })
                    val color=if(row.has("color")) row.getString("color").also { require(it.matches(Regex("#[0-9a-fA-F]{6}"))) }.let(Color::parseColor) else null
                    val alpha=if(row.has("alpha")) row.get("alpha").also { require(it is Int && it in 0..255) } as Int else null
                    val dash=if(row.has("lineStyle")) MapDash.valueOf(row.getString("lineStyle")) else MapDash.ORIGINAL
                    val width=if(row.has("widthPercent")) row.get("widthPercent").also { require(it is Int && it in 25..300) } as Int else null
                    require(target.line || dash==MapDash.ORIGINAL && width==null)
                    val outline=if(row.has("outline")) {
                        require(target.outlined)
                        val border=row.getJSONObject("outline")
                        require(border.keys().asSequence().all { it in setOf("color","alpha","widthPercent") })
                        val rgb=if(border.has("color")) border.getString("color").also { require(it.matches(Regex("#[0-9a-fA-F]{6}"))) }.let(Color::parseColor) else null
                        val a=if(border.has("alpha")) border.get("alpha").also { require(it is Int && it in 0..255) } as Int else null
                        val w=if(border.has("widthPercent")) border.get("widthPercent").also { require(it is Int && it in 25..300) } as Int else null
                        MapOutlineStyle(rgb,a,w)
                    } else null
                    put(target,MapObjectStyle(color,dash,alpha,width,outline))
                }
            },labels)
        }
    }
}
