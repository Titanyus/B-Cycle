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
    private val circle=HrvCircleView(context,themes).apply { tag="hrv_circle" }
    private val status=BicycleFeedback.status(context,themes).apply { tag="hrv_status" }
    private val result=LinearLayout(context).apply { orientation=VERTICAL;tag="hrv_result" }
    private val graph=HrvGraphView(context,themes).apply { tag="hrv_graph" }
    private val action=BicycleComponents.textAction(context,themes,context.getString(R.string.hrv_start)) {
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
        addView(FrameLayout(context).apply {
            addView(circle,FrameLayout.LayoutParams(-1,-1))
            addView(action,FrameLayout.LayoutParams(-2,BicycleDesign.dp(context,BicycleDesign.touchSize),android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL).apply {
                topMargin=BicycleDesign.dp(context,BicycleDesign.hrvCircleHeight/2+BicycleDesign.hrvActionOffset)
            })
        },LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.hrvCircleHeight)))
        addView(status,LayoutParams(-1,-2))
        addView(graph,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.hrvGraphHeight)))
        addView(result,LayoutParams(-1,-2));addView(historyButton,LayoutParams(-1,-2))
        show(controller.state)
    }
    private fun show(state:HrvState) {
        val running=controller.busy && !state.saveError
        keepScreenOn=running
        circle.show(state);graph.show(state.points,state.elapsedMs,state.phase==Phase.MEASURE && running,state.measureMs)
        action.setText(when { state.saveError && controller.canRetrySave->R.string.hrv_retry_save;running->R.string.hrv_abort
            state.result!=null->R.string.hrv_repeat;else->R.string.hrv_start })
        action.isEnabled=!state.saving && (state.saveError && controller.canRetrySave || running || state.ready && canStart())
        historyButton.isEnabled=!running && !state.saving
        historyButton.visibility=if(running) GONE else VISIBLE
        status.visibility=if(state.saveError) VISIBLE else GONE
        graph.visibility=if(state.result==null) VISIBLE else GONE
        BicycleFeedback.update(status,context.getString(when {
            state.saveError->R.string.hrv_save_failed;state.saving->R.string.hrv_saving
            state.result!=null->R.string.hrv_saved;running->R.string.hrv_running_hint;!state.ready && !running->R.string.hrv_sensor_needed;else->R.string.hrv_ready },
            state.restMs/1000,state.measureMs/1000))
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
internal fun hrvResult(context:Context,themes:ThemeManager,row:JSONObject,includeSensor:Boolean=true):LinearLayout=LinearLayout(context).apply {
    orientation=LinearLayout.VERTICAL
    fun heading(id:Int) { addView(BicycleComponents.text(context,themes,context.getString(id)).apply {
        textSize=BicycleDesign.titleSp;typeface=BicycleDesign.strongTypeface
        setPadding(0,BicycleDesign.dp(context,BicycleDesign.controlGap),0,BicycleDesign.dp(context,BicycleDesign.controlGap))
    }) }
    heading(R.string.hrv_result)
    if(includeSensor) row.optString("sensor").takeIf { it.isNotBlank() }?.let { name ->
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrv_sensor_used,name)))
    }
    val accepted=row.optBoolean("acceptable")
    if(accepted) { heading(R.string.hrv_time_metrics) }; if(accepted) row.optJSONObject("corrected")?.let { values ->
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrv_metrics,
            values.getDouble("rmssdMs"),values.getDouble("lnRmssd"),values.getDouble("sdnnMs"),
            values.getDouble("pnn50Percent"),values.getDouble("restingHrBpm"))).apply { tag="hrv_metrics" })
        if(values.has("meanNnMs")) addView(BicycleComponents.description(context,themes,
            context.getString(R.string.hrv_nn_info,values.getDouble("meanNnMs"),values.optInt("count"))))
    } else {
        val reasons=row.optJSONArray("reasons")
        val messages=(0 until (reasons?.length() ?: 0)).map { index -> context.getString(when(reasons!!.getString(index)) {
            "CONTACT_LOST","DISCONNECTED"->R.string.hrv_contact_lost
            "RR_GAP"->R.string.hrv_gap
            "APP_BACKGROUND","VIEW_CLOSED"->R.string.hrv_background
            "USER_ABORT"->R.string.hrv_user_abort
            "SHORT_SERIES","INVALID_RR"->R.string.hrv_too_short
            "HIGH_CORRECTION"->R.string.hrv_legacy_high_correction
            "RESIDUAL_ARTIFACTS"->R.string.hrv_residual_artifacts
            "CORRECTION_FAILED"->R.string.hrv_correction_failed
            "STORAGE_FAILED","PROCESS_INTERRUPTED","BUFFER_OVERFLOW"->R.string.hrv_capture_failed
            else->R.string.hrv_invalid_data
        }) }.distinct().joinToString("; ")
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrv_quality_failed,messages)))
    }
    if(accepted) {
        heading(R.string.hrv_frequency_title)
        val f=row.optJSONObject("frequency")
        addView(BicycleComponents.description(context,themes,if(f==null) context.getString(R.string.hrv_frequency_missing)
            else context.getString(R.string.hrv_frequency_values,f.getDouble("lfMs2"),f.getDouble("hfMs2"),
                if(f.isNull("lfHf")) "–" else "%.2f".format(java.util.Locale.getDefault(),f.getDouble("lfHf")))).apply { tag="hrv_frequency" })
        if(f!=null) addView(BicycleComponents.description(context,themes,context.getString(R.string.hrv_frequency_note)).apply {
            BicycleComponents.bindTheme(this,themes) { setTextColor(HrvVisuals.warningOrange) }
        })
    }
    heading(R.string.hrv_quality_title)
    val warnings=row.optJSONArray("warnings")
    if((0 until (warnings?.length() ?: 0)).any { warnings!!.optString(it)=="HIGH_CORRECTION" })
        addView(BicycleComponents.description(context,themes,
            context.getString(R.string.hrv_correction_warning,row.optDouble("affectedFraction")*100)).apply {
            tag="hrv_correction_warning"
            BicycleDialogStyle.control(this,themes)
            BicycleComponents.bindTheme(this,themes) { setTextColor(HrvVisuals.warningOrange) }
        })
    row.optJSONObject("residualArtifacts")?.let { residual ->
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrv_correction_info,
            row.optDouble("affectedFraction")*100,residual.optInt("count"))))
    }
    if(row.optDouble("coverage",1.0)<.95) addView(BicycleComponents.description(context,themes,
        context.getString(R.string.hrv_coverage_warning,row.optDouble("coverage")*100)).apply {
        tag="hrv_coverage_warning"
        BicycleDialogStyle.control(this,themes)
        BicycleComponents.bindTheme(this,themes) { setTextColor(HrvVisuals.warningOrange) }
    })
    if(row.has("restMs") && row.has("measureMs")) addView(BicycleComponents.description(context,themes,
        context.getString(R.string.hrv_protocol_info,row.getLong("restMs")/1000,row.getLong("measureMs")/1000,row.optInt("packetCount"),row.optJSONArray("rrTicks")?.length() ?: 0)))
    addView(BicycleComponents.description(context,themes,context.getString(R.string.hrv_record_info,
        row.optDouble("measurementElapsedMs")/1000,row.optDouble("rrDurationMs")/1000,row.optDouble("coverage")*100)))
}
internal object HrvVisuals { const val warningOrange=0xffff8a26.toInt() }

/** Animation only: BLE receipt anchors the rhythm; interpolation never changes RR measurements. */
internal object HrvPulseAnimation {
    fun opacity(nowMs:Long,beatAtMs:Long?,intervalMs:Double?,running:Boolean):Float {
        if(!running || beatAtMs==null || intervalMs==null || intervalMs<=0) return .1f
        val age=(nowMs-beatAtMs).coerceAtLeast(0).toDouble()
        val sinceBeat=if(age<=5000) age%intervalMs else age
        return (.3-.2*(sinceBeat/BicycleDesign.hrvGlowFadeMs).coerceIn(0.0,1.0)).toFloat()
    }
}

internal class HrvCircleView(context:Context,themes:ThemeManager):View(context) {
    private var tokens=themes.active.tokens
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow=Paint(Paint.ANTI_ALIAS_FLAG)
    private val numbers=resources.getFont(R.font.bebas_neue)
    private val units=Typeface.create("sans-serif-condensed",Typeface.NORMAL)
    private val pulseBounds=Rect()
    private val timeBounds=Rect()
    private val phaseBounds=Rect()
    private var state=HrvState()
    private var phaseDeadlineMs=0L
    init {
        BicycleComponents.bindTheme(this,themes) { tokens=it;glow.shader=null;invalidate() }
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    fun show(value:HrvState) {
        // Establish one continuous monotonic timeline per phase; packet/tick updates
        // change live values without restarting the progress animation.
        if(value.phase!=state.phase) phaseDeadlineMs=android.os.SystemClock.elapsedRealtime()+value.remainingMs
        state=value
        contentDescription=context.getString(R.string.hrv_circle_accessibility,context.getString(if(value.phase==Phase.REST) R.string.hrv_rest else R.string.hrv_measuring),value.bpm?.toString() ?: "–",time())
        invalidate()
    }
    private fun remaining():Long=if((state.phase==Phase.REST || state.phase==Phase.MEASURE) && !state.saving && !state.saveError)
        (phaseDeadlineMs-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0) else state.remainingMs
    private fun time():String { val seconds=(remaining()+999)/1000;return "%02d:%02d".format(java.util.Locale.ROOT,seconds/60,seconds%60) }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { glow.shader=null }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val density=resources.displayMetrics.density;val scaled=resources.displayMetrics.scaledDensity
        val radius=minOf(width,height)/2f-12*density;val x=width/2f;val y=height/2f
        if(radius<=0) return
        val running=state.phase==Phase.REST || state.phase==Phase.MEASURE
        val now=android.os.SystemClock.elapsedRealtime()
        val glowRadius=(radius-BicycleDesign.hrvRingWidth*density).coerceAtLeast(1f)
        if(glow.shader==null) glow.shader=RadialGradient(x,y,glowRadius,
            intArrayOf(tokens.accent,tokens.accent,Color.TRANSPARENT),floatArrayOf(0f,.25f,1f),Shader.TileMode.CLAMP)
        glow.alpha=(255*HrvPulseAnimation.opacity(now,state.lastBeatAtMs,state.beatIntervalMs,running && state.ready)).toInt()
        canvas.drawCircle(x,y,glowRadius,glow)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=BicycleDesign.hrvRingWidth*density;paint.strokeCap=Paint.Cap.ROUND
        paint.color=tokens.tertiary;canvas.drawCircle(x,y,radius,paint)
        val duration=if(state.phase==Phase.REST) state.restMs.toDouble() else state.measureMs.toDouble()
        val progress=when(state.phase) { Phase.COMPLETE->1f;Phase.REST,Phase.MEASURE->(1-remaining()/duration).coerceIn(0.0,1.0).toFloat();else->0f }
        val measuring=state.phase==Phase.MEASURE || state.phase==Phase.COMPLETE
        paint.color=if(measuring) tokens.accent else tokens.foreground
        canvas.drawArc(x-radius,y-radius,x+radius,y+radius,-90f,360*progress,false,paint)
        paint.style=Paint.Style.FILL
        paint.color=if(measuring) tokens.foreground else tokens.accent
        val angle=Math.toRadians((-90+360*progress).toDouble())
        canvas.drawCircle(x+radius*kotlin.math.cos(angle).toFloat(),y+radius*kotlin.math.sin(angle).toFloat(),
            BicycleDesign.hrvRingWidth*density*BicycleDesign.hrvRingEndScale/2,paint)
        val pulse=state.bpm?.toString() ?: "–";val unit=context.getString(R.string.hrv_bpm)
        paint.typeface=numbers;paint.textSize=BicycleDesign.hrvPulseSp*scaled
        val pulseWidth=paint.measureText(pulse)
        paint.typeface=units;paint.textSize=DashboardTileStyle.ONE.unitMaxSp*scaled
        val unitWidth=paint.measureText(unit);val gap=4*density
        val left=x-(pulseWidth+gap+unitWidth)/2
        paint.textAlign=Paint.Align.LEFT;paint.color=tokens.foreground
        paint.typeface=numbers;paint.textSize=BicycleDesign.hrvPulseSp*scaled
        paint.getTextBounds(pulse,0,pulse.length,pulseBounds)
        val pulseTop=y+10*density+pulseBounds.top
        val pulseBottom=y+10*density+pulseBounds.bottom
        canvas.drawText(pulse,left,y+10*density,paint)
        paint.typeface=units;paint.textSize=DashboardTileStyle.ONE.unitMaxSp*scaled
        paint.color=BicycleDesign.dashboardUnitColor
        canvas.drawText(unit,left+pulseWidth+gap,y+10*density,paint)
        paint.typeface=Typeface.DEFAULT;paint.color=tokens.foreground;paint.textAlign=Paint.Align.CENTER
        paint.textSize=BicycleDesign.hrvTimeSp*scaled
        val clock=time();paint.getTextBounds(clock,0,clock.length,timeBounds)
        canvas.drawText(clock,x,y+48*density,paint)
        val gapAbove=y+48*density+timeBounds.top-pulseBottom
        val phase=context.getString(when(state.phase) {
            Phase.REST->R.string.hrv_rest;Phase.MEASURE->R.string.hrv_measuring
            Phase.COMPLETE,Phase.ABORTED->R.string.hrv_result;else->R.string.hrv_idle
        })
        paint.textSize=BicycleDesign.bodySp*scaled
        paint.getTextBounds(phase,0,phase.length,phaseBounds)
        canvas.drawText(phase,x,pulseTop-gapAbove-phaseBounds.bottom,paint)
        if(running && !state.saving && !state.saveError && remaining()>0 && isAttachedToWindow && isShown)
            postInvalidateOnAnimation()
    }
}

internal data class HrvGraphWindow(val startMs:Long,val endMs:Long) {
    val ticks:List<Long> get() {
        val first=((startMs+9999)/10000)*10000
        return (first..endMs step 10000).toList()
    }
    companion object {
        fun at(elapsedMs:Long,measureMs:Long=180000):HrvGraphWindow {
            val end=elapsedMs.coerceIn(BicycleDesign.hrvGraphWindowMs,measureMs.coerceAtLeast(BicycleDesign.hrvGraphWindowMs))
            return HrvGraphWindow(end-BicycleDesign.hrvGraphWindowMs,end)
        }
    }
}
internal class HrvGraphView(context:Context,themes:ThemeManager):View(context) {
    private var tokens=themes.active.tokens
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val path=Path()
    private val units=Typeface.create("sans-serif-condensed",Typeface.NORMAL)
    private var points:List<HrvPoint> = emptyList()
    private var elapsedMs=0L
    private var updatedAtMs=0L
    private var running=false
    private var measureMs=180000L
    private var low=550.0;private var high=1250.0
    init { BicycleComponents.bindTheme(this,themes) { tokens=it;invalidate() };contentDescription=context.getString(R.string.hrv_rr_label) }
    fun show(value:List<HrvPoint>,elapsed:Long,running:Boolean,measureMs:Long=180000) {
        this.measureMs=measureMs
        points=value;elapsedMs=elapsed;updatedAtMs=android.os.SystemClock.elapsedRealtime();this.running=running
        low=(points.minOfOrNull { it.rrMs } ?: 600.0)-50
        high=(points.maxOfOrNull { it.rrMs } ?: 1200.0)+50
        invalidate()
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val density=resources.displayMetrics.density
        val left=48*density;val right=width-12*density;val top=30*density;val bottom=height-44*density
        if(right<=left || bottom<=top) return
        val elapsed=elapsedMs+if(running) (android.os.SystemClock.elapsedRealtime()-updatedAtMs).coerceAtLeast(0) else 0
        val window=HrvGraphWindow.at(elapsed,measureMs)
        fun position(at:Long)=left+(right-left)*((at-window.startMs).toDouble()/BicycleDesign.hrvGraphWindowMs).toFloat()
        paint.style=Paint.Style.FILL;paint.color=BicycleDesign.dashboardUnitColor;paint.typeface=units
        paint.textSize=12*resources.displayMetrics.scaledDensity;paint.textAlign=Paint.Align.LEFT
        canvas.drawText(context.getString(R.string.hrv_rr_label),0f,16*density,paint)
        for(index in 0..4) {
            val y=top+(bottom-top)*index/4
            paint.color=tokens.tertiary;paint.strokeWidth=density;canvas.drawLine(left,y,right,y,paint)
            paint.color=BicycleDesign.dashboardUnitColor;paint.textAlign=Paint.Align.RIGHT
            canvas.drawText("%.0f".format(java.util.Locale.ROOT,high-(high-low)*index/4),left-5*density,y+4*density,paint)
        }
        paint.textAlign=Paint.Align.CENTER;paint.color=BicycleDesign.dashboardUnitColor
        for(at in window.ticks) {
            val x=position(at)
            canvas.drawLine(x,bottom,x,bottom+4*density,paint)
            val label=(at/1000).toString();val half=paint.measureText(label)/2
            canvas.drawText(label,x.coerceIn(left+half,right-half),bottom+18*density,paint)
        }
        canvas.drawText(context.getString(R.string.hrv_time_axis),(left+right)/2,bottom+36*density,paint)
        path.rewind()
        // Retain one point before the clip for a continuous entry at the left edge.
        val first=(points.indexOfFirst { it.atMs>=window.startMs }.let { if(it<0) points.size else it }-1).coerceAtLeast(0)
        var started=false
        for(index in first until points.size) {
            val p=points[index];if(p.atMs>window.endMs) break
            val x=position(p.atMs);val y=bottom-((p.rrMs-low)/(high-low)).toFloat()*(bottom-top)
            if(!started || p.gap) path.moveTo(x,y) else path.lineTo(x,y)
            started=true
        }
        val save=canvas.save();canvas.clipRect(left,top,right,bottom)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=2*density;paint.color=tokens.accent;canvas.drawPath(path,paint)
        canvas.restoreToCount(save)
        if(running && elapsed<measureMs && isAttachedToWindow && isShown) postInvalidateOnAnimation()
    }
}
