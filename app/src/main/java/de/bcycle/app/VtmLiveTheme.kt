package de.bcycle.app

import android.graphics.Color
import org.oscim.theme.IRenderTheme
import org.oscim.theme.RenderTheme
import org.oscim.theme.rule.Rule
import org.oscim.theme.styles.AreaStyle
import org.oscim.theme.styles.LineStyle
import org.oscim.theme.styles.RenderStyle
import java.util.Collections
import java.util.IdentityHashMap

/** Original style handles stay in existing GPU buckets. Replace their current paint on the GL queue. */
internal class VtmLiveTheme(private val native:RenderTheme,initial:MapStyleOverrides,
    private val allowBackground:Boolean=true):IRenderTheme by native {
    private data class Paint(val target:MapStyleTarget,val original:RenderStyle<*>)
    private val paints=ArrayList<Paint>()
    private var appliedColors:Map<MapStyleTarget,MapObjectStyle> = emptyMap()
    @Volatile private var background=native.mapBackground
    init {
        val seen=Collections.newSetFromMap(IdentityHashMap<RenderStyle<*>,Boolean>())
        native.traverseRules(object:Rule.RuleVisitor() {
            override fun apply(rule:Rule) {
                for(style in rule.styles) if(seen.add(style)) {
                    val id=when(style) { is LineStyle -> style.style;is AreaStyle -> style.style;else -> null }
                    val target=MapStyleTarget.entries.firstOrNull { id in it.ids }
                    if(target!=null) paints.add(Paint(target,style))
                }
                super.apply(rule)
            }
        })
        applyColors(initial)
    }
    override fun getMapBackground()=background
    /** At construction before use, otherwise exclusively on this map's GL queue. No bitmap/VBO allocation. */
    fun applyColors(style:MapStyleOverrides) {
        val colors=style.objects.filterValues { it.color!=null || it.alpha!=null }
        val changed=MapStyleTarget.entries.filter { appliedColors[it]!=colors[it] }.toSet()
        var touched=false
        for((target,original) in paints) {
            if(target !in changed) continue
            touched=true
            val override=colors[target]
            val rgb=override?.color ?: original.let { when(it) { is LineStyle -> it.color;is AreaStyle -> it.color;else -> 0 } }
            when(original) {
                is LineStyle -> {
                    if(override==null) original.set(original)
                    else {
                        val color=withAlpha(rgb,original.color,override.alpha)
                        val replacement=LineStyle.builder().set(original).color(color)
                        // Dash masks are white textures; both shader paint uniforms must change.
                        if(original.dashArray!=null || original.stippleColor==original.color)
                            replacement.stippleColor(withAlpha(rgb,original.stippleColor,override.alpha))
                        original.set(replacement.build())
                    }
                }
                is AreaStyle -> if(override==null) original.set(original)
                    else original.set(AreaStyle.builder().set(original).color(withAlpha(rgb,original.color,override.alpha)).build())
            }
        }
        background=if(allowBackground) style.objects[MapStyleTarget.BACKGROUND]?.let { withAlpha(it.color ?: native.mapBackground,native.mapBackground,it.alpha) } ?: native.mapBackground else native.mapBackground
        appliedColors=colors
        if(touched) native.updateStyles()
    }
    private fun withAlpha(rgb:Int,original:Int,alpha:Int?)=(rgb and 0xffffff) or ((alpha ?: Color.alpha(original)) shl 24)
}
