package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** Same vertical system-font labeling as charts; group labels retain the shared Black weight. */
internal class AnalysisSideLabel(context: Context, themes: ThemeManager, resource: Int) : View(context) {
    val title=context.getString(resource)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=BicycleDesign.strongTypeface; textAlign=Paint.Align.CENTER }
    private val textSize=android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,BicycleDesign.analysisSideLabelSp,resources.displayMetrics)
    init {
        tag="analysis_side_$resource";contentDescription=title;isAccessibilityHeading=true
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
        BicycleComponents.bindTheme(this,themes) { paint.color=it.foreground;invalidate() }
    }
    override fun onDraw(canvas: Canvas) {
        paint.textSize=textSize
        val available=(height-BicycleDesign.dp(context,BicycleDesign.verticalPadding)*2).coerceAtLeast(1)
        val measured=paint.measureText(title)
        if(measured>available) paint.textSize*=available/measured
        val save=canvas.save();canvas.translate(width/2f,height/2f);canvas.rotate(-90f)
        canvas.drawText(title,0f,-(paint.ascent()+paint.descent())/2,paint);canvas.restoreToCount(save)
    }
}
