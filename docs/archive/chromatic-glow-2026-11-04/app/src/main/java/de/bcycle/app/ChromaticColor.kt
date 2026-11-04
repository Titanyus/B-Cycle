package de.bcycle.app

import kotlin.math.*

/** D65 OKLab/OKLCH, identical to docs/previews/chromatic-color.js. No Android dependency. */
internal object ChromaticColor {
    data class Palette(val base:Int,val light:Int,val dark:Int)
    private fun clamp(x:Double)=x.coerceIn(0.0,1.0)
    private fun linear(x:Double)=if(x<=.04045) x/12.92 else ((x+.055)/1.055).pow(2.4)
    private fun encoded(x:Double)=if(x<=.0031308) 12.92*x else 1.055*x.pow(1/2.4)-.055
    private fun lab(r:Double,g:Double,b:Double):DoubleArray {
        val l=Math.cbrt(.4122214708*r+.5363325363*g+.0514459929*b)
        val m=Math.cbrt(.2119034982*r+.6806995451*g+.1073969566*b)
        val s=Math.cbrt(.0883024619*r+.2817188376*g+.6299787005*b)
        return doubleArrayOf(.2104542553*l+.793617785*m-.0040720468*s,
            1.9779984951*l-2.428592205*m+.4505937099*s,.0259040371*l+.7827717662*m-.808675766*s)
    }
    private fun lab(color:Int)=lab(linear((color ushr 16 and 255)/255.0),linear((color ushr 8 and 255)/255.0),linear((color and 255)/255.0))
    private fun rgb(L:Double,C:Double,h:Double):DoubleArray {
        val a=C*cos(h);val b=C*sin(h)
        val l=(L+.3963377774*a+.2158037573*b).pow(3)
        val m=(L-.1055613458*a-.0638541728*b).pow(3)
        val s=(L-.0894841775*a-1.291485548*b).pow(3)
        return doubleArrayOf(4.0767416621*l-3.3077115913*m+.2309699292*s,
            -1.2684380046*l+2.6097574011*m-.3413193965*s,-.0041960863*l-.7034186147*m+1.707614701*s)
    }
    private fun acceptable(channels:DoubleArray,L:Double,C:Double,h:Double):Boolean {
        if(channels.all { it>=-1e-7 && it<=1+1e-7 }) return true
        val clipped=lab(clamp(channels[0]),clamp(channels[1]),clamp(channels[2]))
        return sqrt((clipped[0]-L).pow(2)+(clipped[1]-C*cos(h)).pow(2)+(clipped[2]-C*sin(h)).pow(2))<=.01
    }
    private fun color(L:Double,C:Double,h:Double):Int {
        var channels=rgb(L,C,h)
        if(!acceptable(channels,L,C,h)) {
            var lo=0.0;var hi=C
            repeat(20) { val mid=(lo+hi)/2;if(acceptable(rgb(L,mid,h),L,mid,h)) lo=mid else hi=mid }
            channels=rgb(L,lo,h)
        }
        fun channel(i:Int)=(clamp(encoded(clamp(channels[i])))*255).roundToInt()
        return (255 shl 24) or (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
    }
    fun tone(base:Int,strength:Double,chromatic:Double,highlight:Boolean):Int {
        val t=clamp(strength);val k=chromatic.coerceIn(0.0,3.0)
        if(t==0.0) return base
        if(t==1.0) return if(highlight) -1 else 0xff000000.toInt()
        val lab=lab(base);val L=lab[0];val C=hypot(lab[1],lab[2]);val h=atan2(lab[2],lab[1])
        val shift=k*sin(t*PI/2)*min(1.0,C/.04)*sin((if(highlight) 100 else 300)*PI/180-h)*(if(highlight) 32 else 45)*PI/180
        return color(if(highlight) L+(1-L)*t else L*(1-t),if(C<1e-7) 0.0 else C*(1-t).pow(if(highlight) 1.5 else 1.15),h+shift)
    }
    fun palette(base:Int,lighten:Double,darken:Double,chromatic:Double)=Palette(base,tone(base,lighten,chromatic,true),tone(base,darken,chromatic,false))
}
