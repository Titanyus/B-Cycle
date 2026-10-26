package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/** Presentation only: due flags stay server-owned, no locally invented severity thresholds. */
internal object MaintenancePalette {
    val green get()=TrainingZoneColors.green
    val red get()=TrainingZoneColors.red
    fun normalLimit(foreground:Int)=ZoneGlowColors.mix(red,foreground,.35f)
    fun exceeded()=ZoneGlowColors.mix(red,Color.BLACK,.30f)
}

internal class MaintenanceProgress(context:Context,themes:ThemeManager,private val task:DreeveMaintenanceTask):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect=RectF()
    private val clip=Path()
    private var foreground=Color.WHITE
    internal val ratio:Double? get()=task.elapsed?.div(task.interval)
        ?: task.remaining?.let { (task.interval-it)/task.interval } ?: task.percentage?.div(100)
    internal val targetFraction get()=ratio?.takeIf { it>1 }?.let { 1.0/it } ?: 1.0
    init {
        tag="maintenance_progress_${task.id}";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        BicycleComponents.bindTheme(this,themes) { foreground=it.foreground;invalidate() }
    }
    override fun onDraw(canvas:Canvas) {
        val inset=BicycleDesign.dp(context,BicycleDesign.smallGap).toFloat()
        val bar=BicycleDesign.dp(context,BicycleDesign.analysisZoneBarHeight).toFloat()
        rect.set(0f,inset,width.toFloat(),inset+bar)
        paint.style=Paint.Style.FILL;paint.color=foreground;paint.alpha=(255*BicycleDesign.analysisZoneTrackAlpha).toInt()
        canvas.drawRoundRect(rect,bar/2,bar/2,paint)
        val value=ratio?.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: return
        clip.rewind();clip.addRoundRect(rect,bar/2,bar/2,Path.Direction.CW)
        val save=canvas.save();canvas.clipPath(clip);paint.alpha=255
        if(task.overdue && value>1) {
            val end=(width*targetFraction).toFloat()
            paint.color=MaintenancePalette.normalLimit(foreground);canvas.drawRect(0f,inset,end,inset+bar,paint)
            paint.color=MaintenancePalette.exceeded();canvas.drawRect(end,inset,width.toFloat(),inset+bar,paint)
        } else {
            paint.color=if(task.overdue) MaintenancePalette.red else MaintenancePalette.green
            canvas.drawRect(0f,inset,(width*value.coerceAtMost(1.0)).toFloat(),inset+bar,paint)
        }
        canvas.restoreToCount(save)
        if(task.overdue && value>1) {
            paint.color=foreground;paint.strokeWidth=BicycleDesign.linePixels(context).toFloat()
            val x=(width*targetFraction).toFloat();canvas.drawLine(x,0f,x,inset*2+bar,paint)
        }
    }
}
