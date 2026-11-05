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
        for(i in 0 until all.length) {
            val element=all.item(i) as? Element ?: continue
            val id=element.getAttribute("id")
            val target=MapStyleTarget.entries.firstOrNull { id in it.ids || id.endsWith("-casing") && id.removeSuffix("-casing") in it.ids } ?: continue
            val style=overrides.objects[target] ?: continue
            if(!id.endsWith("-casing")) if(style.color!=null || style.alpha!=null) {
                val attribute=when(element.tagName) { "area","text","caption" -> "fill";"line" -> "stroke";else -> null }
                if(attribute!=null && element.hasAttribute(attribute)) {
                    val color=style.color ?: Color.parseColor(element.getAttribute(attribute))
                    val alpha=style.alpha ?: Color.alpha(Color.parseColor(element.getAttribute(attribute)))
                    element.setAttribute(attribute,hex((color and 0xffffff) or (alpha shl 24)))
                }
            }
            if(element.tagName=="line") style.widthPercent?.let { percent ->
                if(element.hasAttribute("width")) element.setAttribute("width",(element.getAttribute("width").toDouble()*percent/100).toString())
            }
            if(element.tagName=="line" && style.dash!=MapDash.ORIGINAL) {
                element.removeAttribute("dasharray")
                element.removeAttribute("stipple")
                if(style.dash!=MapDash.SOLID) element.setAttribute("dasharray",style.dash.pattern!!)
            }
        }
        return serialize(doc)
    }
    fun defaultColor(xml:String,target:MapStyleTarget):Int {
        val doc=document(xml)
        if(target==MapStyleTarget.BACKGROUND) return Color.parseColor(doc.documentElement.getAttribute("map-background"))
        val all=doc.getElementsByTagName("*")
        for(i in 0 until all.length) {
            val element=all.item(i) as? Element ?: continue
            if(element.getAttribute("id") !in target.ids) continue
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
