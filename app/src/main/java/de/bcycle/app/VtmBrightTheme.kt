package de.bcycle.app

import org.json.JSONArray
import org.json.JSONObject
import org.oscim.backend.AssetAdapter
import org.oscim.core.GeometryBuffer.GeometryType
import org.oscim.core.Tag
import org.oscim.core.TagSet
import org.oscim.theme.IRenderTheme
import org.oscim.theme.ThemeLoader
import org.oscim.theme.styles.RenderStyle
import org.oscim.theme.styles.TextStyle

/** Source filters remain exact; VTM owns drawing, resource disposal and its bounded style cache. */
internal enum class VtmMapPass { ALL,GROUND,FOREGROUND }
internal class VtmBrightTheme private constructor(
    private val native:VtmLiveTheme,
    source:JSONObject,
    private val options:MapVectorLayers,
    private val pass:VtmMapPass
):IRenderTheme by native {
    private data class PriorityKey(val styles:Array<RenderStyle<*>>,val priority:Int)
    private val priorityCache=android.util.LruCache<PriorityKey,Array<RenderStyle<*>>>(256)
    private data class Layer(val flag:Tag,val matches:(GeometryType,TagSet)->Boolean)
    private val layers:Map<String,List<Layer>> = buildMap {
        val all=source.getJSONArray("layers")
        for(i in 0 until all.length()) {
            val layer=all.getJSONObject(i)
            if(!layer.has("source-layer") || layer.getString("id") in setOf("building-top","water-offset")) continue
            val key=layer.getString("source-layer")
            val predicate=BrightFilter.compile(layer.optJSONArray("filter"))
            val item=Layer(Tag("bcycle:bright:"+layer.getString("id"),"1"),predicate)
            put(key,get(key).orEmpty()+item)
        }
    }
    override fun matchElement(type:GeometryType,tags:TagSet,zoomLevel:Int):Array<RenderStyle<*>>? {
        if(!options.accepts(tags)) return null
        val ground=tags.getValue("layer") in setOf("landcover","landuse","park","water")
        if(pass==VtmMapPass.GROUND && !ground || pass==VtmMapPass.FOREGROUND && ground) return null
        val candidates=layers[tags.getValue("layer")].orEmpty()
        val matched=TagSet()
        // The generated native instructions use these only for sprite selection/cycling overlay.
        for(key in nativeKeys)
            tags.getValue(key)?.let { matched.add(Tag(key,it)) }
        for(layer in candidates) if(layer.matches(type,tags)) {
            val track=tags.getValue("layer")=="transportation" && tags.getValue("class")=="track" &&
                layer.flag.key in setOf("bcycle:bright:highway-minor","bcycle:bright:highway-minor-casing","bcycle:bright:tunnel-service-track","bcycle:bright:tunnel-service-track-casing")
            matched.add(if(track) Tag(layer.flag.key+"-track","1") else layer.flag)
        }
        val styles=native.matchElement(type,matched,zoomLevel) ?: return null
        val largePlace=tags.getValue("layer")=="place" && tags.getValue("class") in setOf("town","city")
        val priority=when(tags.getValue("layer")) {
            "place" -> when(tags.getValue("class")) {
                "continent"->0;"country"->10;"state","province"->20;"city"->30;"town"->40
                "village"->50;"hamlet"->60;"suburb","quarter","neighbourhood","isolated_dwelling"->70
                else->80
            }*100+(tags.getValue("rank")?.toIntOrNull()?.coerceIn(0,99) ?: 99)
            "transportation_name" -> when(tags.getValue("class")) {
                "motorway"->100;"trunk"->110;"primary"->120;"secondary"->130;"tertiary"->140
                "minor","minor_road","residential","unclassified"->150;"service"->160
                "track","path","footway","cycleway","pedestrian","steps","bridleway"->170
                else->180
            }*100
            else -> return styles
        }
        if(styles.none { it is TextStyle }) return styles
        // Two tile-loader threads share the theme. Bound native-style variants;
        // copied text styles borrow the original symbol bitmap, never dispose it twice.
        synchronized(priorityCache) {
            val key=PriorityKey(styles,priority)
            return priorityCache.get(key) ?: styles.map { style ->
                if(style is TextStyle) TextStyle.builder().set(style).priority(priority).apply {
                    if(largePlace) {
                        fontFamily(org.oscim.backend.canvas.Paint.FontFamily.BLACK)
                        fontStyle(org.oscim.backend.canvas.Paint.FontStyle.NORMAL)
                        fontSize(style.fontSize*.9f)
                    }
                }.build() else style
            }.toTypedArray().also { priorityCache.put(key,it) }
        }
    }
    fun applyColors(style:MapStyleOverrides) { native.applyColors(style) }
    override fun getMapBackground()=if(pass==VtmMapPass.FOREGROUND) android.graphics.Color.TRANSPARENT else native.mapBackground
    override fun dispose() { priorityCache.evictAll();native.dispose() }
    override fun scaleTextSize(scale:Float) { priorityCache.evictAll();native.scaleTextSize(scale) }
    companion object {
        private val nativeKeys=arrayOf("layer","class","subclass","bicycle","network","ref_length")
        fun load(cycleways:Boolean,options:MapVectorLayers=MapVectorLayers(),style:MapStyleOverrides=MapStyleOverrides(),pass:VtmMapPass=VtmMapPass.ALL):IRenderTheme {
            val source=AssetAdapter.readFileAsStream("styles/bright.json").use { JSONObject(it.bufferedReader().readText()) }
            return VtmBrightTheme(VtmLiveTheme(ThemeLoader.load(VtmCycleTheme(cycleways,style.compiledStyles())) as org.oscim.theme.RenderTheme,style),source,options,pass)
        }
    }
}

/** Compile once at theme load; no JSON parsing, HTTP or geometry mutation on tile matching. */
internal object BrightFilter {
    fun compile(filter:JSONArray?):(GeometryType,TagSet)->Boolean {
        if(filter==null) return { _,_ -> true }
        val op=filter.getString(0)
        if(op in setOf("all","any","none")) {
            val parts=(1 until filter.length()).map { compile(filter.getJSONArray(it)) }
            return when(op) {
                "all" -> { type,tags -> parts.all { it(type,tags) } }
                "any" -> { type,tags -> parts.any { it(type,tags) } }
                else -> { type,tags -> parts.none { it(type,tags) } }
            }
        }
        val key=filter.getString(1)
        val values=(2 until filter.length()).map { filter.get(it) }
        val expected=if(key=="class") values.map { if(it=="minor_road") "minor" else it } else values
        require(op in setOf("==","!=","in","!in","has","!has",">=","<=",">","<")) { "Unsupported Bright filter: $op" }
        return { type,tags ->
            val value=if(key=="\$type") when(type) {
                GeometryType.POLY -> "Polygon"
                GeometryType.LINE -> "LineString"
                GeometryType.POINT -> "Point"
                else -> null
            } else if(key=="class" && tags.getValue("layer")=="transportation") {
                val raw=tags.getValue(key)
                // Current OpenMapTiles puts ramps on the base road class, old Bright uses *_link.
                if(raw in setOf("motorway","trunk","primary","secondary","tertiary") && tags.getValue("ramp")=="1") "${raw}_link"
                else if(raw=="minor_road") "minor" else raw
            } else tags.getValue(key)
            // This one old Bright tunnel filter says minor_road; current schema says minor.
            val included=expected.any { if(it is Number) value?.toDoubleOrNull()==it.toDouble() else value==it.toString() }
            when(op) {
                "has" -> value!=null
                "!has" -> value==null
                "==","in" -> value!=null && included
                "!=","!in" -> value==null || !included
                else -> {
                    val a=value?.toDoubleOrNull();val b=expected.firstOrNull()?.toString()?.toDoubleOrNull()
                    if(a==null || b==null) false else when(op) {
                        ">=" -> a>=b;"<=" -> a<=b;">" -> a>b;else -> a<b
                    }
                }
            }
        }
    }
}
