package de.bcycle.app

import android.graphics.Bitmap
import kotlin.math.roundToInt

/** Cached static-field blur: three separable box passes approximate a Gaussian in linear time.
 * Blur premultiplied channels together, so transparent edges cannot create dark color fringes. */
internal object BicycleContourBlur {
    fun radius(spread:Float)=(spread/3).roundToInt().coerceAtLeast(1)
    fun blur(source:Bitmap,spread:Float):Bitmap {
        if(spread<=0) return source.copy(Bitmap.Config.ARGB_8888,false)
        val width=source.width;val height=source.height;val r=radius(spread)
        var pixels=IntArray(width*height);var scratch=IntArray(pixels.size)
        source.getPixels(pixels,0,width,0,0,width,height)
        for(i in pixels.indices) {
            val c=pixels[i];val a=c ushr 24
            pixels[i]=(a shl 24) or ((((c ushr 16) and 255)*a/255) shl 16) or
                ((((c ushr 8) and 255)*a/255) shl 8) or ((c and 255)*a/255)
        }
        fun pass(horizontal:Boolean) {
            val lines=if(horizontal) height else width;val length=if(horizontal) width else height
            val step=if(horizontal) 1 else width;val divisor=2*r+1
            for(line in 0 until lines) {
                val start=if(horizontal) line*width else line
                var a=0;var red=0;var green=0;var blue=0
                fun add(index:Int,sign:Int) {
                    if(index !in 0 until length) return
                    val c=pixels[start+index*step]
                    a+=(c ushr 24)*sign;red+=((c ushr 16) and 255)*sign
                    green+=((c ushr 8) and 255)*sign;blue+=(c and 255)*sign
                }
                for(i in 0..r) add(i,1)
                for(i in 0 until length) {
                    scratch[start+i*step]=((a/divisor) shl 24) or ((red/divisor) shl 16) or ((green/divisor) shl 8) or (blue/divisor)
                    add(i-r,-1);add(i+r+1,1)
                }
            }
            val old=pixels;pixels=scratch;scratch=old
        }
        repeat(3) { pass(true);pass(false) }
        for(i in pixels.indices) {
            val c=pixels[i];val a=c ushr 24
            pixels[i]=if(a==0) 0 else (a shl 24) or (((((c ushr 16) and 255)*255/a).coerceAtMost(255)) shl 16) or
                (((((c ushr 8) and 255)*255/a).coerceAtMost(255)) shl 8) or (((c and 255)*255/a).coerceAtMost(255))
        }
        return Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888)
    }
}
