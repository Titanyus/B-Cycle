package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.view.View
import kotlin.math.roundToInt

/** Approved designer export (docs/designs/input-design.json), compiled once; no runtime JSON. */
internal object BicycleInputStyle {
    const val defaultChromatic=.5
    private val strengths=mapOf("hellfire-color" to .5,"hellfire-dark" to 1.5,"solar-flare-color" to 2.0,"solar-flare-dark" to 2.0,
        "sunstruck-color" to 2.6,"sunstruck-dark" to 2.6,"acid-pop-color" to 1.4,"acid-pop-dark" to 1.4,
        "deep-dive-color" to 1.8,"deep-dive-dark" to 1.8,"blue-riot-color" to .8,"blue-riot-dark" to .7,
        "purple-haze-color" to 1.7,"purple-haze-dark" to 1.7)
    fun strength(themeId:String?)=strengths[themeId?.removePrefix("preset:")] ?: defaultChromatic
    fun strength(context:Context)=strength(context.getSharedPreferences("bike",0).getString(ThemeManager.activeKey,null))
    fun strength(view:View)= (view.getTag(R.id.input_theme_id) as? String)?.let(::strength) ?: strength(view.context)
    fun radius(context:Context)=BicycleDesign.dp(context,if(BicycleAppearance.flat(context)) BicycleDesign.inputRadius else BicycleDesign.inputGlowRadius).toFloat()
    enum class Type { BASE,LIGHT,DARK }
    data class Stop(val position:Float,val type:Type,val opacity:Float=1f)
    val outline=listOf(Stop(.0031545258456678014f,Type.LIGHT),Stop(.16088327301298447f,Type.DARK),
        Stop(.38801259961955076f,Type.BASE),Stop(.47397469245719587f,Type.LIGHT),Stop(.7326499995451244f,Type.BASE),Stop(1f,Type.DARK))
    val fill=listOf(Stop(.29731864581093553f,Type.BASE),Stop(1f,Type.DARK))
    fun resolve(stops:List<Stop>,palette:ChromaticColor.Palette,dark:Int=palette.dark)=stops.map { stop ->
        val color=when(stop.type) { Type.BASE->palette.base;Type.LIGHT->palette.light;Type.DARK->dark }
        (color and 0xffffff) or ((Color.alpha(color)*stop.opacity).roundToInt() shl 24)
    }.toIntArray()
}
