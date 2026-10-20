package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.view.View
import android.widget.*
import de.bcycle.hrv.Phase
import org.json.JSONObject

/** Native foreground measurement; colors follow the active dashboard palette. */
internal class HrvMeasurementView(context:Context,private val themes:ThemeManager,private val controller:HrvController,
    private val sensor:()->String,private val canStart:()->Boolean,private val history:()->Unit):LinearLayout(context) {
    private val phase=BicycleComponents.contentHeading(context,themes,context.getString(R.string.hrv_title))
    private val circle=HrvCircleView(context,themes).apply { tag="hrv_circle" }
    private val status=BicycleFeedback.status(context,themes).apply { tag="hrv_status" }
    private val result=LinearLayout(context).apply { orientation=VERTICAL;tag="hrv_result" }
    private val graph=HrvGraphView(context,themes).apply { tag="hrv_graph" }
    private val action=BicycleComponents.action(context,themes,context.getString(R.string.hrv_start)) {
        when { controller.state.saveError && controller.canRetrySave -> controller.retrySave()
            controller.busy -> controller.abort()
            canStart() -> controller.start(sensor()) }
    }.apply { tag="hrv_action" }
    private val historyButton=BicycleComponents.action(context,themes,context.getString(R.string.hrv_history),history).apply { tag="hrv_history" }
    private var subscription:AutoCloseable?=null
    private var shown:JSONObject?=null
    init {
        orientation=VERTICAL;tag="hrv_measurement"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        addView(phase);addView(circle,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.hrvCircleHeight)))
        addView(action,LayoutParams(-1,-2));addView(status,LayoutParams(-1,-2))
        addView(graph,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.hrvGraphHeight)))
        addView(result,LayoutParams(-1,-2));addView(historyButton,LayoutParams(-1,-2))
        show(controller.state)
    }
    private fun show(state:HrvState) {
        val running=controller.busy && !state.saveError
        keepScreenOn=running
        phase.setText(when(state.phase) { Phase.REST->R.string.hrv_rest;Phase.MEASURE->R.string.hrv_measuring;else->R.string.hrv_title })
        circle.show(state);graph.show(state.points)
        action.setText(when { state.saveError && controller.canRetrySave->R.string.hrv_retry_save;running->R.string.hrv_abort
            state.result!=null->R.string.hrv_repeat;else->R.string.hrv_start })
        action.isEnabled=!state.saving && (state.saveError && controller.canRetrySave || running || state.ready && canStart())
        historyButton.isEnabled=!running && !state.saving
        BicycleFeedback.update(status,context.getString(when {
            state.saveError->R.string.hrv_save_failed;state.saving->R.string.hrv_saving
            state.result!=null->R.string.hrv_saved;running->R.string.hrv_running_hint;!state.ready && !running->R.string.hrv_sensor_needed;else->R.string.hrv_ready }))
        if(shown!==state.result) {
            shown=state.result;result.removeAllViews()
            state.result?.let { result.addView(hrvResult(context,themes,it))
                post { (parent as? ScrollView)?.smoothScrollTo(0,result.top) }
            }
        }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();subscription=controller.observe(::show) }
    override fun onDetachedFromWindow() { subscription?.close();subscription=null;keepScreenOn=false;super.onDetachedFromWindow() }
}
internal fun hrvResult(context:Context,themes:ThemeManager,row:JSONObject):LinearLayout=LinearLayout(context).apply {
    orientation=LinearLayout.VERTICAL
    val accepted=row.optBoolean("acceptable")
    if(accepted) row.optJSONObject("corrected")?.let { values ->
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrv_metrics,
            values.getDouble("rmssdMs"),values.getDouble("lnRmssd"),values.getDouble("sdnnMs"),
            values.getDouble("pnn50Percent"),values.getDouble("restingHrBpm"))).apply { tag="hrv_metrics" })
    } else {
        val reasons=row.optJSONArray("reasons")
        val messages=(0 until (reasons?.length() ?: 0)).map { index -> context.getString(when(reasons!!.getString(index)) {
            "CONTACT_LOST","DISCONNECTED"->R.string.hrv_contact_lost
            "RR_GAP"->R.string.hrv_gap
            "APP_BACKGROUND","VIEW_CLOSED"->R.string.hrv_background
            "USER_ABORT"->R.string.hrv_user_abort
            "SHORT_SERIES","INVALID_RR"->R.string.hrv_too_short
            "HIGH_CORRECTION","RESIDUAL_ARTIFACTS","CORRECTION_FAILED"->R.string.hrv_artifacts
            "STORAGE_FAILED","PROCESS_INTERRUPTED","BUFFER_OVERFLOW"->R.string.hrv_capture_failed
            else->R.string.hrv_invalid_data
        }) }.distinct().joinToString("; ")
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrv_quality_failed,messages)))
    }
    if(row.optDouble("coverage",1.0)<.95) addView(BicycleComponents.description(context,themes,
        context.getString(R.string.hrv_coverage_warning,row.optDouble("coverage")*100)).apply {
        tag="hrv_coverage_warning"
        BicycleDialogStyle.control(this,themes)
        BicycleComponents.bindTheme(this,themes) { setTextColor(HrvVisuals.warningOrange) }
    })
    addView(BicycleComponents.description(context,themes,context.getString(R.string.hrv_record_info,
        row.optDouble("measurementElapsedMs")/1000,row.optDouble("rrDurationMs")/1000,row.optDouble("coverage")*100)))
}
internal object HrvVisuals { const val warningOrange=0xffff8a26.toInt() }

internal class HrvCircleView(context:Context,themes:ThemeManager):View(context) {
    private var tokens=themes.active.tokens
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var state=HrvState()
    init { BicycleComponents.bindTheme(this,themes) { tokens=it;invalidate() };importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES }
    fun show(value:HrvState) {
        state=value
        contentDescription=context.getString(R.string.hrv_circle_accessibility,context.getString(if(value.phase==Phase.REST) R.string.hrv_rest else R.string.hrv_measuring),value.bpm?.toString() ?: "–",time())
        invalidate()
    }
    private fun time():String { val seconds=(state.remainingMs+999)/1000;return "%02d:%02d".format(java.util.Locale.ROOT,seconds/60,seconds%60) }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val density=resources.displayMetrics.density
        val radius=minOf(width,height)/2f-12*density;val x=width/2f;val y=height/2f
        paint.style=Paint.Style.STROKE;paint.strokeWidth=BicycleDesign.hrvRingWidth*density;paint.strokeCap=Paint.Cap.ROUND
        paint.color=tokens.tertiary;canvas.drawCircle(x,y,radius,paint)
        val duration=if(state.phase==Phase.REST) 30000.0 else 180000.0
        val progress=when(state.phase) { Phase.COMPLETE->1f;Phase.REST,Phase.MEASURE->(1-state.remainingMs/duration).coerceIn(0.0,1.0).toFloat();else->0f }
        paint.color=tokens.primary;canvas.drawArc(x-radius,y-radius,x+radius,y+radius,-90f,360*progress,false,paint)
        paint.style=Paint.Style.FILL;paint.textAlign=Paint.Align.CENTER;paint.color=tokens.foreground
        paint.typeface=resources.getFont(R.font.bebas_neue);paint.textSize=BicycleDesign.hrvPulseSp*resources.displayMetrics.scaledDensity
        canvas.drawText(state.bpm?.toString() ?: "–",x,y+10*density,paint)
        paint.typeface=android.graphics.Typeface.DEFAULT;paint.textSize=14*resources.displayMetrics.scaledDensity
        canvas.drawText(context.getString(R.string.hrv_bpm),x,y-51*density,paint)
        paint.textSize=BicycleDesign.hrvTimeSp*resources.displayMetrics.scaledDensity
        canvas.drawText(time(),x,y+48*density,paint)
    }
}
internal class HrvGraphView(context:Context,themes:ThemeManager):View(context) {
    private var tokens=themes.active.tokens
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var points:List<HrvPoint> = emptyList()
    init { BicycleComponents.bindTheme(this,themes) { tokens=it;invalidate() };contentDescription=context.getString(R.string.hrv_rr_label) }
    fun show(value:List<HrvPoint>) { points=value;invalidate() }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val density=resources.displayMetrics.density
        val left=48*density;val right=width-8*density;val top=30*density;val bottom=height-32*density
        if(right<=left || bottom<=top) return
        val low=(points.minOfOrNull { it.rrMs } ?: 600.0)-50
        val high=(points.maxOfOrNull { it.rrMs } ?: 1200.0)+50
        paint.style=Paint.Style.FILL;paint.color=tokens.foreground;paint.typeface=android.graphics.Typeface.DEFAULT
        paint.textSize=12*resources.displayMetrics.scaledDensity;paint.textAlign=Paint.Align.LEFT
        canvas.drawText(context.getString(R.string.hrv_rr_label),0f,16*density,paint)
        for(index in 0..4) {
            val y=top+(bottom-top)*index/4
            paint.color=tokens.tertiary;paint.strokeWidth=density;canvas.drawLine(left,y,right,y,paint)
            paint.color=tokens.foreground;paint.textAlign=Paint.Align.RIGHT
            canvas.drawText("%.0f".format(java.util.Locale.ROOT,high-(high-low)*index/4),left-5*density,y+4*density,paint)
        }
        paint.textAlign=Paint.Align.LEFT;canvas.drawText("0",left,bottom+18*density,paint)
        paint.textAlign=Paint.Align.RIGHT;canvas.drawText("180",right,bottom+18*density,paint)
        paint.textAlign=Paint.Align.CENTER;canvas.drawText(context.getString(R.string.hrv_time_axis),(left+right)/2,bottom+18*density,paint)
        val path=Path()
        points.forEachIndexed { i,p ->
            val x=left+(right-left)*(p.atMs/180000f).coerceIn(0f,1f)
            val y=bottom-((p.rrMs-low)/(high-low)).toFloat()*(bottom-top)
            if(i==0 || p.gap) path.moveTo(x,y) else path.lineTo(x,y)
        }
        paint.style=Paint.Style.STROKE;paint.strokeWidth=2*density;paint.color=tokens.primary;canvas.drawPath(path,paint)
    }
}
