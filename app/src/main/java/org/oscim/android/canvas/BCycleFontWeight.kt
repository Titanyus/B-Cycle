package org.oscim.android.canvas

import android.graphics.Typeface
import org.oscim.backend.canvas.Paint

/** Same-package bridge to VTM's Android paint: exact Android weights, no reflection or new atlas. */
internal object BCycleFontWeight {
    private val faces=Array(9) { Typeface.create(Typeface.SANS_SERIF,(it+1)*100,false) }
    fun apply(paint:Paint,weight:Int) {
        (paint as AndroidPaint).mPaint.typeface=faces[weight/100-1]
    }
}
