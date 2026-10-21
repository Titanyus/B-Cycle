package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.widget.EditText

/** The suffix is painted only: native editing, selection and persistence contain the base name. */
internal class RideNamingInput(context:Context):EditText(context) {
    var previewSuffix:String=""
        set(value) { if(field!=value) { field=value;requestLayout();invalidate() } }
    private fun previewLayout(width:Int):StaticLayout? {
        if(previewSuffix.isEmpty() || width<=0) return null
        val base=text?.toString().orEmpty()
        val example=SpannableString(base+previewSuffix).apply {
            setSpan(ForegroundColorSpan(android.graphics.Color.TRANSPARENT),0,base.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val ghost=TextPaint(paint).apply { color=(currentTextColor and 0x00ffffff) or (0x80 shl 24) }
        return StaticLayout.Builder.obtain(example,0,example.length,ghost,width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(includeFontPadding)
            .setBreakStrategy(breakStrategy).setHyphenationFrequency(hyphenationFrequency)
            .setLineSpacing(lineSpacingExtra,lineSpacingMultiplier).build()
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec)
        val example=previewLayout(measuredWidth-compoundPaddingLeft-compoundPaddingRight) ?: return
        setMeasuredDimension(measuredWidth,resolveSize(maxOf(measuredHeight,example.height+compoundPaddingTop+compoundPaddingBottom),heightMeasureSpec))
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val example=previewLayout(width-compoundPaddingLeft-compoundPaddingRight) ?: return
        val saved=canvas.save()
        canvas.clipRect(scrollX+compoundPaddingLeft,scrollY+compoundPaddingTop,scrollX+width-compoundPaddingRight,scrollY+height-compoundPaddingBottom)
        canvas.translate(compoundPaddingLeft.toFloat(),extendedPaddingTop.toFloat())
        example.draw(canvas);canvas.restoreToCount(saved)
    }
}

/** Examples reuse the real formatter; absent history/metrics remain explicit placeholders. */
internal fun rideNamingPreview(context:Context,naming:RideNaming,details:RouteDetails?,
    zone:java.time.ZoneId=java.time.ZoneId.systemDefault(),locale:java.util.Locale=java.util.Locale.getDefault()):String {
    fun part(part:RideNamePart):String {
        val available=details!=null && when(part) {
            RideNamePart.DATE,RideNamePart.TIME->details.dateMs>0
            RideNamePart.DISTANCE->details.distanceM!=null
            RideNamePart.MOVING->details.movingMs!=null
            RideNamePart.SPEED->details.movingAverageKmh!=null
        }
        return if(available) naming.copy(text="",parts=setOf(part)).format(details!!,zone,locale)
        else context.getString(when(part) {
            RideNamePart.DATE->R.string.ride_preview_date
            RideNamePart.TIME->R.string.ride_preview_time
            RideNamePart.DISTANCE->R.string.ride_preview_distance
            RideNamePart.MOVING->R.string.ride_preview_moving
            RideNamePart.SPEED->R.string.ride_preview_speed
        })
    }
    val chunks=mutableListOf<String>()
    listOf(RideNamePart.DATE,RideNamePart.TIME).filter { it in naming.parts }.joinToString(" ") { part(it) }.takeIf { it.isNotEmpty() }?.let(chunks::add)
    listOf(RideNamePart.DISTANCE,RideNamePart.MOVING,RideNamePart.SPEED).filter { it in naming.parts }.forEach { chunks+=part(it) }
    return if(chunks.isEmpty()) "" else (if(naming.text.isBlank()) "" else " · ")+chunks.joinToString(" · ")
}
