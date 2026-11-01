package de.bcycle.app

import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.widget.TextView
import kotlin.math.ceil

/** Inline swatches use the actual chart palette and scale with the surrounding body text. */
internal fun TextView.showHrvContextLegend(mode:HrvContextMode) {
    val label=context.getString(mode.label)
    val legend=HrvContextColors.legend(mode)
    if(legend==null) { text=label;contentDescription=label;return }
    val low=context.getString(legend.low);val high=context.getString(legend.high)
    val line=SpannableStringBuilder("$label · $low ")
    legend.colors.forEach { color ->
        val start=line.length;line.append('\uFFFC')
        line.setSpan(HrvLegendSquare(color,4*resources.displayMetrics.density),start,line.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    line.append(" $high")
    text=line
    contentDescription="$label · $low – $high"
}

internal class HrvLegendSquare(val color:Int,private val gap:Float):ReplacementSpan() {
    override fun getSize(paint:Paint,text:CharSequence,start:Int,end:Int,fm:Paint.FontMetricsInt?)=ceil(paint.textSize*.8f+gap).toInt()
    override fun draw(canvas:Canvas,text:CharSequence,start:Int,end:Int,x:Float,top:Int,y:Int,bottom:Int,paint:Paint) {
        val side=paint.textSize*.8f;val center=y+(paint.ascent()+paint.descent())/2
        val oldColor=paint.color;val oldStyle=paint.style
        paint.color=color;paint.style=Paint.Style.FILL
        canvas.drawRect(x+gap/2,center-side/2,x+gap/2+side,center+side/2,paint)
        paint.color=oldColor;paint.style=oldStyle
    }
}
