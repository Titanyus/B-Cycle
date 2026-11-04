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

/** Source filters remain exact; VTM owns drawing, resource disposal and its bounded style cache. */
internal class VtmBrightTheme private constructor(
    private val native:IRenderTheme,
    source:JSONObject
):IRenderTheme by native {
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
        when(tags.getValue("layer")) { "poi","aerodrome_label" -> return null }
        val candidates=layers[tags.getValue("layer")].orEmpty()
        val matched=TagSet()
        // The generated native instructions use these only for sprite selection/cycling overlay.
        for(key in nativeKeys)
            tags.getValue(key)?.let { matched.add(Tag(key,it)) }
        for(layer in candidates) if(layer.matches(type,tags)) matched.add(layer.flag)
        return native.matchElement(type,matched,zoomLevel)
    }
    companion object {
        private val nativeKeys=arrayOf("layer","class","subclass","bicycle","network","ref_length")
        fun load(cycleways:Boolean):IRenderTheme {
            val source=AssetAdapter.readFileAsStream("styles/bright.json").use { JSONObject(it.bufferedReader().readText()) }
            return VtmBrightTheme(ThemeLoader.load(VtmCycleTheme(cycleways)),source)
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
