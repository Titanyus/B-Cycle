package de.bcycle.app

import android.graphics.Color
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.io.StringWriter

/** Modify native paint instructions before VTM builds its textures. Bundled Bright assets stay untouched. */
internal object MapStyleXml {
    private fun document(xml:String)=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
    /** Split only track geometry from Bright's shared minor/service/track paints. Source stays intact. */
    fun trackBase(xml:String):String {
        val doc=document(xml)
        val rules=doc.getElementsByTagName("m")
        val originals=(0 until rules.length).map { rules.item(it) as Element }.filter {
            it.getAttribute("k") in setOf("bcycle:bright:highway-minor","bcycle:bright:tunnel-service-track")
        }
        for(rule in originals) {
            val clone=rule.cloneNode(true) as Element
            clone.setAttribute("k",rule.getAttribute("k")+"-track")
            val lines=clone.getElementsByTagName("line")
            for(i in 0 until lines.length) {
                val line=lines.item(i) as Element
                line.setAttribute("id",if(rule.getAttribute("k").contains("tunnel")) "bcycle-track-tunnel" else "bcycle-track")
                line.setAttribute("stroke","#ffccbbaa")
                line.setAttribute("width",(line.getAttribute("width").toDouble()*.5).toString())
                line.setAttribute("dasharray","3,2")
                line.setAttribute("cap","butt")
            }
            rule.parentNode.insertBefore(clone,rule.nextSibling)
        }
        return serialize(doc)
    }
    fun apply(xml:String,overrides:MapStyleOverrides):String {
        if(overrides.objects.isEmpty()) return xml
        val doc=document(xml)
        overrides.objects[MapStyleTarget.BACKGROUND]?.let { style ->
            val original=Color.parseColor(doc.documentElement.getAttribute("map-background"))
            val color=style.color ?: original
            doc.documentElement.setAttribute("map-background",hex((color and 0xffffff) or ((style.alpha ?: Color.alpha(original)) shl 24)))
        }
        val all=doc.getElementsByTagName("*")
        fun zoomKey(element:Element)=generateSequence(element.parentNode) { it.parentNode }.filterIsInstance<Element>()
            .map { it.getAttribute("zoom-min")+":"+it.getAttribute("zoom-max") }.filter { it!=":" }.joinToString("/")
        val innerWidths=(0 until all.length).mapNotNull { all.item(it) as? Element }.filter {
            it.tagName=="line" && !it.getAttribute("id").endsWith("-casing") && it.hasAttribute("width")
        }.associate { (it.getAttribute("id") to zoomKey(it)) to it.getAttribute("width").toDouble() }
        for(i in 0 until all.length) {
            val element=all.item(i) as? Element ?: continue
            val id=element.getAttribute("id")
            val target=MapStyleTarget.entries.firstOrNull { id in it.ids || id.endsWith("-casing") && id.removeSuffix("-casing") in it.ids } ?: continue
            val style=overrides.objects[target] ?: continue
            val casing=id.endsWith("-casing")
            val colorOverride=if(casing) style.outline?.color else style.color
            val alphaOverride=if(casing) style.outline?.alpha else style.alpha
            if(colorOverride!=null || alphaOverride!=null) {
                val attribute=when(element.tagName) { "area","text","caption" -> "fill";"line" -> "stroke";else -> null }
                if(attribute!=null && element.hasAttribute(attribute)) {
                    val color=colorOverride ?: Color.parseColor(element.getAttribute(attribute))
                    val alpha=alphaOverride ?: Color.alpha(Color.parseColor(element.getAttribute(attribute)))
                    element.setAttribute(attribute,hex((color and 0xffffff) or (alpha shl 24)))
                }
            }
            if(element.tagName=="line" && element.hasAttribute("width")) {
                val original=element.getAttribute("width").toDouble()
                val innerScale=(style.widthPercent ?: 100)/100.0
                val borderScale=(style.outline?.widthPercent ?: style.widthPercent ?: 100)/100.0
                val inner=innerWidths[id.removeSuffix("-casing") to zoomKey(element)]
                val width=if(casing && style.outline!=null && inner!=null && original>=inner)
                    inner*innerScale+(original-inner)*borderScale
                else original*(if(casing) borderScale else innerScale)
                element.setAttribute("width",width.toString())
            }
            if(element.tagName=="line" && style.dash!=MapDash.ORIGINAL) {
                element.removeAttribute("dasharray")
                element.removeAttribute("stipple")
                if(style.dash!=MapDash.SOLID) element.setAttribute("dasharray",style.dash.pattern!!)
            }
        }
        return serialize(doc)
    }
    /** Enforce semantic road dashes after legacy/manual patterns, including casings and bike highlights. */
    fun roadSurfaces(xml:String):String {
        val doc=document(xml)
        val nodes=doc.getElementsByTagName("line")
        val roads=(0 until nodes.length).map { nodes.item(it) as Element }.filter { MapRoadSurface.roadId(it.getAttribute("id")) }
        for(line in roads) {
            for(unpaved in listOf(false,true)) {
                val rule=doc.createElement("m").apply { setAttribute("k","bcycle:surface");setAttribute("v",if(unpaved) "unpaved" else "other") }
                val paint=line.cloneNode(true) as Element
                for(key in listOf("dasharray","stroke-dasharray","stipple","stipple-width","stipple-ratio","stipple-color")) paint.removeAttribute(key)
                if(unpaved) { paint.setAttribute("dasharray","${MapRoadSurface.dashLength},${MapRoadSurface.gapLength}");paint.setAttribute("cap","butt") }
                rule.appendChild(paint);line.parentNode.insertBefore(rule,line)
            }
            line.parentNode.removeChild(line)
        }
        // All dashed map lines share the chosen 12/8 rhythm. Keep pure dots and
        // structural railway cross-hatching as their distinct symbol types.
        val all=doc.getElementsByTagName("*")
        for(i in 0 until all.length) {
            val e=all.item(i) as? Element ?: continue
            val id=e.getAttribute("id")
            if(e.tagName=="line" && e.hasAttribute("dasharray") && !id.contains("hatching")) {
                if(e.getAttribute("dasharray")!=MapDash.DOTTED.pattern) {
                    val pattern=e.getAttribute("dasharray").split(',').mapNotNull { it.trim().toDoubleOrNull() }
                    if(pattern.size<=2) e.setAttribute("dasharray","${MapRoadSurface.dashLength},${MapRoadSurface.gapLength}")
                    else e.setAttribute("dasharray",pattern.mapIndexed { index,value -> if(index%2==1) MapRoadSurface.gapLength.toString() else if(value<=1) value.toString() else MapRoadSurface.dashLength.toString() }.joinToString(","))
                }
            }
            if(e.tagName in setOf("text","caption") && id.startsWith("place-")) {
                if(e.hasAttribute("stroke")) e.setAttribute("stroke","#ffffffff")
                if(e.hasAttribute("stroke-width")) e.setAttribute("stroke-width",(e.getAttribute("stroke-width").toDouble()*MapPlaceHalo.widthScale).toString())
            }
        }
        return serialize(doc)
    }
    fun defaultColor(xml:String,target:MapStyleTarget,outline:Boolean=false):Int {
        val doc=document(xml)
        if(target==MapStyleTarget.BACKGROUND) return Color.parseColor(doc.documentElement.getAttribute("map-background"))
        val all=doc.getElementsByTagName("*")
        for(i in 0 until all.length) {
            val element=all.item(i) as? Element ?: continue
            if(if(outline) element.getAttribute("id").removeSuffix("-casing") !in target.ids || !element.getAttribute("id").endsWith("-casing") else element.getAttribute("id") !in target.ids) continue
            val attribute=if(element.tagName=="line") "stroke" else "fill"
            if(element.hasAttribute(attribute)) return Color.parseColor(element.getAttribute(attribute))
        }
        return when(target) { MapStyleTarget.TRACK -> Color.rgb(204,187,170);MapStyleTarget.CYCLE -> Color.rgb(36,120,196);MapStyleTarget.CONTOUR -> Color.argb(102,139,115,85);else -> Color.GRAY }
    }
    private fun serialize(doc:org.w3c.dom.Document):String {
        val writer=StringWriter();TransformerFactory.newInstance().newTransformer().transform(DOMSource(doc),StreamResult(writer));return writer.toString()
    }
    private fun hex(color:Int)=String.format(java.util.Locale.ROOT,"#%08X",color)
}
