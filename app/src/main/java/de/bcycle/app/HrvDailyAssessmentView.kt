package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import java.text.NumberFormat
import kotlin.math.cos
import kotlin.math.sin

internal class HrvDailyAssessmentView(context:Context,themes:ThemeManager,val assessment:HrvDailyAssessment):LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="hrv_daily_assessment"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding)
        setPadding(pad,0,pad,pad)
        addView(HrvDayCircle(context,themes,assessment),LayoutParams(-1,-2))
        addView(HrvIndexView(context,themes,context.getString(R.string.hrva_pns),assessment.indices.pns,assessment.indices.pnsReferenceDays).apply { tag="hrva_pns" },
            LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.sectionCardGap) })
        addView(HrvIndexView(context,themes,context.getString(R.string.hrva_activation),assessment.indices.activation,assessment.indices.activationReferenceDays).apply { tag="hrva_activation" },
            LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(LinearLayout(context).apply {
            orientation=VERTICAL;tag="hrva_advice_text"
            val reason=when(assessment.position) {
                HrvDayPosition.UNAVAILABLE->R.string.hrva_hrv_missing;HrvDayPosition.COLLECTING->R.string.hrva_collecting
                HrvDayPosition.LOW->R.string.hrva_low;HrvDayPosition.NORMAL->R.string.hrva_normal;HrvDayPosition.HIGH->R.string.hrva_high
            }
            val feeling=when(assessment.subjectiveState) {
                HrvSubjectiveState.UNKNOWN->R.string.hrva_subjective_unknown;HrvSubjectiveState.GOOD->R.string.hrva_subjective_good
                HrvSubjectiveState.MIXED->R.string.hrva_subjective_mixed;HrvSubjectiveState.POOR->R.string.hrva_subjective_poor
            }
            val advice=context.getString(assessment.advice.label)
            val paragraph=android.text.SpannableStringBuilder("$advice. ${context.getString(reason)} ${context.getString(feeling)}").apply {
                setSpan(android.text.style.TypefaceSpan(BicycleDesign.strongTypeface),0,advice.length,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.ForegroundColorSpan(assessment.advice.color),0,advice.length,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            addView(BicycleComponents.text(context,themes,"").apply { tag="hrva_advice";text=paragraph },LayoutParams(-1,-2))
        },LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,24) })
    }
}

/** Fixed corridor placement: 25% / 75%. Today's outliers never move the reference markers. */
internal fun hrvCorridorFraction(value:Double?,lower:Double?,upper:Double?):Float? {
    if(value==null || lower==null || upper==null || !value.isFinite() || !lower.isFinite() || !upper.isFinite() || upper<=lower) return null
    return (.25+.5*(value-lower)/(upper-lower)).coerceIn(0.0,1.0).toFloat()
}

internal class HrvDayCircle(context:Context,themes:ThemeManager,private val data:HrvDailyAssessment):View(context) {
    private var tokens=themes.active.tokens
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow=Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds=Rect()
    private val number=NumberFormat.getNumberInstance().apply { minimumFractionDigits=1;maximumFractionDigits=1 }
    private val limit=NumberFormat.getNumberInstance().apply { maximumFractionDigits=1 }
    private fun formatted(v:Double?)=v?.takeIf(Double::isFinite)?.let(number::format) ?: context.getString(R.string.hrvt_unavailable)
    private val score=formatted(data.score)
    private val pulse=data.pulse?.value?.takeIf { it.isFinite() && it>0 }?.let { kotlin.math.round(it).toInt().toString() } ?: context.getString(R.string.hrvt_unavailable)
    init {
        tag="hrva_circle";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
        fun range(low:Double?,high:Double?)=if(low==null || high==null) context.getString(R.string.hrva_range_missing)
            else context.getString(R.string.hrva_range,limit.format(low),limit.format(high))
        contentDescription=context.getString(R.string.hrva_metric_accessibility,context.getString(R.string.hrva_hrv),score,range(data.lower,data.upper))+". "+
            context.getString(R.string.hrva_metric_accessibility,context.getString(R.string.hrva_pulse),context.getString(R.string.hrva_pulse_value,pulse),range(data.pulse?.lower,data.pulse?.upper))
        BicycleComponents.bindTheme(this,themes) { tokens=it;glow.shader=null;invalidate() }
    }
    override fun onMeasure(w:Int,h:Int) {
        val side=MeasureSpec.getSize(w).coerceAtMost(BicycleDesign.dp(context,400))
        setMeasuredDimension(resolveSize(side,w),resolveSize(side,h))
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) { glow.shader=null }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val d=resources.displayMetrics.density;val sp=resources.displayMetrics.scaledDensity
        val x=width/2f;val y=height/2f
        val radius=minOf(width,height)/2f-24*d
        if(radius<=0) return
        val ring=BicycleDesign.hrvRingWidth*d
        val glowRadius=(radius-ring).coerceAtLeast(1f)
        if(glow.shader==null) glow.shader=RadialGradient(x,y,glowRadius,intArrayOf(data.advice.color,data.advice.color,Color.TRANSPARENT),floatArrayOf(0f,.25f,1f),Shader.TileMode.CLAMP)
        glow.alpha=(255*HrvPulseAnimation.PEAK_OPACITY).toInt();canvas.drawCircle(x,y,glowRadius,glow)
        fun arc(start:Float,sweep:Float,stroke:Float,value:Double?,low:Double?,high:Double?,color:Int) {
            paint.style=Paint.Style.STROKE;paint.strokeCap=Paint.Cap.ROUND;paint.strokeWidth=stroke;paint.color=tokens.tertiary;paint.alpha=255
            canvas.drawArc(x-radius,y-radius,x+radius,y+radius,start,sweep,false,paint)
            hrvCorridorFraction(value,low,high)?.takeIf { it>0 }?.let { fraction ->
                paint.color=color;canvas.drawArc(x-radius,y-radius,x+radius,y+radius,start,sweep*fraction,false,paint)
            }
            if(low!=null && high!=null && high>low) listOf(.25f to low,.75f to high).forEach { (fraction,v) ->
                val angle=Math.toRadians((start+sweep*fraction).toDouble())
                val c=cos(angle).toFloat();val s=sin(angle).toFloat()
                val inside=radius-stroke/2-3*d;val outside=radius+stroke/2+3*d
                paint.color=tokens.foreground;paint.strokeWidth=2*d;paint.strokeCap=Paint.Cap.BUTT
                canvas.drawLine(x+c*inside,y+s*inside,x+c*outside,y+s*outside,paint)
                paint.style=Paint.Style.FILL;paint.typeface=BicycleDesign.bodyTypeface;paint.textSize=BicycleDesign.analysisLabelSp*sp
                paint.textAlign=if(c<0) Paint.Align.RIGHT else Paint.Align.LEFT
                val tx=x+c*(outside+4*d);val ty=y+s*(outside+4*d)+(if(sweep>0) -3 else 3)*d
                canvas.drawText(limit.format(v),tx,ty-(paint.ascent()+paint.descent())/2,paint)
                paint.style=Paint.Style.STROKE
            }
        }
        arc(195f,150f,ring*2,data.score,data.lower,data.upper,data.advice.color)
        arc(165f,-150f,ring,data.pulse?.value,data.pulse?.lower,data.pulse?.upper,Color.WHITE)
        paint.style=Paint.Style.STROKE;paint.strokeCap=Paint.Cap.BUTT;paint.strokeWidth=2*d;paint.color=tokens.foreground;paint.alpha=64
        canvas.drawLine(x-radius*.7f,y,x+radius*.7f,y,paint)
        paint.alpha=255;paint.style=Paint.Style.FILL
        // Values retain their own center; bpm is placed to their right, not part of the centering width.
        fun value(text:String,cy:Float,color:Int,unit:String?=null) {
            paint.color=color;paint.typeface=BicycleDesign.narrowTypeface(context);paint.textSize=BicycleDesign.hrvDayValueSp*sp
            // Respect narrow devices and accessibility font sizes without crossing the circle.
            val maxWidth=radius*1.1f
            if(paint.measureText(text)>maxWidth) paint.textSize*=maxWidth/paint.measureText(text)
            paint.getTextBounds(text,0,text.length,bounds)
            val baseline=cy-(bounds.top+bounds.bottom)/2f
            val left=x-(bounds.left+bounds.right)/2f
            paint.textAlign=Paint.Align.LEFT;canvas.drawText(text,left,baseline,paint)
            if(unit!=null) {
                val right=left+paint.measureText(text)
                paint.typeface=BicycleDesign.compactTypeface;paint.textSize=BicycleDesign.analysisLabelSp*sp
                canvas.drawText(unit,right+4*d,cy-(paint.ascent()+paint.descent())/2,paint)
            }
        }
        value(score,y-radius*.55f,data.advice.color)
        value(pulse,y+radius*.55f,Color.WHITE,context.getString(R.string.hrv_bpm))
        paint.typeface=BicycleDesign.bodyTypeface;paint.textSize=BicycleDesign.bodySp*sp;paint.textAlign=Paint.Align.CENTER;paint.color=tokens.foreground
        val label=context.getString(R.string.hrva_hrv)
        if(paint.measureText(label)>radius*1.7f) paint.textSize*=radius*1.7f/paint.measureText(label)
        canvas.drawText(label,x,y-radius*sin(Math.toRadians(15.0)).toFloat()-(paint.ascent()+paint.descent())/2,paint)
        canvas.drawText(context.getString(R.string.hrva_pulse),x,y+radius*sin(Math.toRadians(15.0)).toFloat()-(paint.ascent()+paint.descent())/2,paint)
    }
}

/** Signed fraction from the bar centre: ±1 takes the middle half, outer quarters extend to ±4. */
internal fun hrvIndexOffsetFraction(value:Double):Float {
    if(!value.isFinite()) return 0f
    val magnitude=kotlin.math.abs(value)
    val distance=if(magnitude<=1) magnitude*.25 else .25+((magnitude-1)/3).coerceAtMost(1.0)*.25
    return (kotlin.math.sign(value)*distance).toFloat()
}

/** Signed deviation: square at zero, rounded only at its outward end. No diagnostic colors. */
internal class HrvIndexView(context:Context,themes:ThemeManager,label:String,private val value:Double?,referenceDays:Int?=null):LinearLayout(context) {
    init {
        orientation=VERTICAL
        val formatted=value?.let { NumberFormat.getNumberInstance().apply { minimumFractionDigits=1;maximumFractionDigits=1 }.format(if(kotlin.math.abs(it)<.05) 0.0 else it) } ?: when {
            referenceDays==null->context.getString(R.string.hrva_index_missing)
            referenceDays<HrvTrends.MIN_NORMAL->(HrvTrends.MIN_NORMAL-referenceDays).let { context.resources.getQuantityString(R.plurals.hrva_index_waiting,it,it) }
            else->context.getString(R.string.hrva_index_constant)
        }
        addView(LinearLayout(context).apply {
            gravity=Gravity.CENTER_VERTICAL
            addView(BicycleComponents.text(context,themes,label),LayoutParams(0,-2,1f))
            addView(BicycleComponents.text(context,themes,formatted).apply {
                tag="hrva_index_value"
                if(value==null) { textSize=BicycleDesign.analysisLabelSp;maxWidth=BicycleDesign.dp(context,200) }
            },LayoutParams(-2,-2))
        },LayoutParams(-1,-2))
        addView(object:View(context) {
            private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
            private var foreground=themes.active.tokens.foreground
            private var track=themes.active.tokens.tertiary
            init { BicycleComponents.bindTheme(this,themes) { foreground=it.foreground;track=it.tertiary;invalidate() } }
            override fun onDraw(canvas:Canvas) {
                val d=resources.displayMetrics.density;val bar=BicycleDesign.hrvRingWidth*d
                val center=width/2f;val y=bar/2+4*d
                paint.style=Paint.Style.FILL;paint.color=track
                canvas.drawRoundRect(0f,y-bar/2,width.toFloat(),y+bar/2,bar/2,bar/2,paint)
                value?.takeIf { it.isFinite() && it!=0.0 }?.let {
                    val end=center+hrvIndexOffsetFraction(it)*width
                    val save=canvas.save();canvas.clipRect(minOf(center,end),y-bar/2,maxOf(center,end),y+bar/2)
                    paint.color=foreground
                    canvas.drawRoundRect(minOf(center,end)-if(it>0) bar/2 else 0f,y-bar/2,
                        maxOf(center,end)+if(it<0) bar/2 else 0f,y+bar/2,bar/2,bar/2,paint)
                    canvas.restoreToCount(save)
                }
                paint.color=foreground;paint.strokeWidth=2*d
                canvas.drawLine(center,y-bar/2-3*d,center,y+bar/2+3*d,paint)
                paint.typeface=BicycleDesign.bodyTypeface;paint.textSize=BicycleDesign.analysisLabelSp*resources.displayMetrics.scaledDensity;paint.textAlign=Paint.Align.CENTER
                canvas.drawText("0",center,y+bar/2+6*d-paint.ascent(),paint)
            }
        },LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.hrvDayIndexHeight)))
    }
}
