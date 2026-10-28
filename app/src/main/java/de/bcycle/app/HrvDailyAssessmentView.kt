package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.time.format.DateTimeFormatter
import java.text.NumberFormat

/** HRV page surface, normal app cards/typography, deliberately no trend or baseline row. */
internal class HrvDailyAssessmentView(context:Context,private val themes:ThemeManager,val assessment:HrvDailyAssessment):LinearLayout(context) {
    private val format=NumberFormat.getNumberInstance().apply { maximumFractionDigits=1;minimumFractionDigits=1 }
    private fun number(value:Double?)=value?.let(format::format) ?: context.getString(R.string.hrvt_unavailable)
    private fun gap(value:Int)=BicycleDesign.dp(context,value)
    private fun card()=LinearLayout(context).apply {
        orientation=VERTICAL;val pad=gap(BicycleDesign.sectionCardInset);setPadding(pad,pad,pad,pad)
        BicycleComponents.bindTheme(this,themes) { tokens -> background=GradientDrawable().apply { setColor(tokens.tertiary);cornerRadius=gap(BicycleDesign.sectionCardRadius).toFloat() } }
    }
    init {
        orientation=VERTICAL;tag="hrv_daily_assessment"
        val pad=gap(BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        addView(BicycleComponents.text(context,themes,number(assessment.score)).apply {
            tag="hrva_score";gravity=Gravity.CENTER;typeface=BicycleDesign.narrowTypeface(context);textSize=BicycleDesign.hrvAssessmentScoreSp;includeFontPadding=false
            BicycleComponents.bindTheme(this,themes) { setTextColor(assessment.advice.color) }
        },LayoutParams(-1,-2))
        addView(BicycleComponents.text(context,themes,context.getString(R.string.hrva_hrv)).apply { gravity=Gravity.CENTER },LayoutParams(-1,-2))
        addView(HrvAssessmentGauge(context,themes,assessment.score,assessment.lower,assessment.upper,assessment.advice.color,true).apply { tag="hrva_hrv_gauge" },
            LayoutParams(-1,gap(BicycleDesign.hrvAssessmentGaugeHeight)).apply { topMargin=gap(BicycleDesign.controlGap) })
        addView(card().apply {
            tag="hrva_advice_card"
            addView(BicycleComponents.contentHeading(context,themes,context.getString(assessment.advice.label)).apply {
                tag="hrva_advice";BicycleComponents.bindTheme(this,themes) { setTextColor(assessment.advice.color) }
            },LayoutParams(-1,-2))
            val reason=when(assessment.position) {
                HrvDayPosition.UNAVAILABLE->R.string.hrva_hrv_missing;HrvDayPosition.COLLECTING->R.string.hrva_collecting
                HrvDayPosition.LOW->R.string.hrva_low;HrvDayPosition.NORMAL->R.string.hrva_normal;HrvDayPosition.HIGH->R.string.hrva_high
            }
            val feeling=when(assessment.subjectiveState) {
                HrvSubjectiveState.UNKNOWN->R.string.hrva_subjective_unknown;HrvSubjectiveState.GOOD->R.string.hrva_subjective_good
                HrvSubjectiveState.MIXED->R.string.hrva_subjective_mixed;HrvSubjectiveState.POOR->R.string.hrva_subjective_poor
            }
            addView(BicycleComponents.text(context,themes,context.getString(reason)+"\n\n"+context.getString(feeling)).apply { tag="hrva_reason" },
                LayoutParams(-1,-2).apply { topMargin=gap(BicycleDesign.controlGap) })
            val dates=DateTimeFormatter.ofPattern("dd.MM.yyyy")
            addView(BicycleComponents.text(context,themes,context.getString(R.string.hrva_subjective_period,assessment.day.minusDays(3).format(dates),assessment.day.format(dates),assessment.subjectiveDays)).apply {
                tag="hrva_subjective_period";textSize=BicycleDesign.analysisLabelSp
                BicycleComponents.bindTheme(this,themes) { setTextColor(it.secondaryTextColor) }
            },LayoutParams(-1,-2).apply { topMargin=gap(BicycleDesign.controlGap) })
        },LayoutParams(-1,-2).apply { topMargin=gap(BicycleDesign.sectionCardGap) })
        addView(card().apply {
            tag="hrva_pulse_card"
            addView(LinearLayout(context).apply {
                gravity=Gravity.CENTER_VERTICAL
                addView(BicycleComponents.text(context,themes,context.getString(R.string.hrva_pulse)).apply {
                    val icon=BicycleIcons.drawable(context,BicycleIcon.Heart,themes.active.tokens.foreground)
                    val size=gap(BicycleDesign.iconSize);icon.setBounds(0,0,size,size);setCompoundDrawablesRelative(icon,null,null,null);compoundDrawablePadding=gap(BicycleDesign.controlGap)
                    BicycleComponents.bindTheme(this,themes) { icon.setTint(it.foreground) }
                },LayoutParams(0,-2,1f))
                val pulse=assessment.pulse?.value?.takeIf { it>0 }?.let { kotlin.math.round(it).toInt().toString() } ?: context.getString(R.string.hrvt_unavailable)
                addView(BicycleComponents.text(context,themes,context.getString(R.string.hrva_pulse_value,pulse)).apply {
                    tag="hrva_pulse";typeface=BicycleDesign.narrowTypeface(context);textSize=BicycleDesign.analysisMetricSp;gravity=Gravity.END
                },LayoutParams(-2,-2))
            },LayoutParams(-1,-2))
            addView(HrvAssessmentGauge(context,themes,assessment.pulse?.value?.takeIf { it>0 },assessment.pulse?.lower,assessment.pulse?.upper,themes.active.tokens.foreground,false).apply { tag="hrva_pulse_gauge" },
                LayoutParams(-1,gap(BicycleDesign.hrvAssessmentPulseGaugeHeight)).apply { topMargin=gap(BicycleDesign.controlGap) })
        },LayoutParams(-1,-2).apply { topMargin=gap(BicycleDesign.sectionCardGap) })
    }
}

/** Finite display range includes today's value and known normal bounds; labels denote real bounds. */
internal class HrvAssessmentGauge(context:Context,themes:ThemeManager,private val value:Double?,private val lower:Double?,private val upper:Double?,
    private val color:Int,private val filled:Boolean):View(context) {
    private val density=resources.displayMetrics.density
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=BicycleDesign.bodyTypeface }
    private var foreground=themes.active.tokens.foreground
    private val rect=RectF()
    private val fillRect=RectF()
    private val number=NumberFormat.getNumberInstance().apply { maximumFractionDigits=1 }
    private val labels=listOfNotNull(lower,upper).filter(Double::isFinite).distinct().map { it to number.format(it) }
    private val values=listOfNotNull(value,lower,upper).filter(Double::isFinite)
    private val low=values.minOrNull() ?: 0.0
    private val high=values.maxOrNull() ?: 1.0
    private val padding=maxOf((high-low)*.5,if(filled) .2 else 2.0)
    private val minimum=low-padding
    private val maximum=high+padding
    private val dots=DashPathEffect(floatArrayOf(.1f*density,BicycleDesign.hrvTrendBandDotGapDp*density),0f)
    init {
        val range=if(labels.isEmpty()) context.getString(R.string.hrva_range_missing) else context.getString(R.string.hrva_range,lower?.let(number::format) ?: context.getString(R.string.hrvt_unavailable),upper?.let(number::format) ?: context.getString(R.string.hrvt_unavailable))
        contentDescription=(value?.let(number::format) ?: context.getString(R.string.hrvt_unavailable))+". "+range
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
        BicycleComponents.bindTheme(this,themes) { foreground=it.foreground;invalidate() }
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val edge=BicycleDesign.controlGap*density
        val trackHeight=(if(filled) BicycleDesign.hrvAssessmentTrackHeight else BicycleDesign.hrvAssessmentPulseTrackHeight)*density
        rect.set(edge,edge,width-edge,edge+trackHeight)
        fun x(v:Double)=rect.left+((v-minimum)/(maximum-minimum)).toFloat()*rect.width()
        paint.style=Paint.Style.FILL;paint.color=foreground;paint.alpha=40;paint.pathEffect=null
        canvas.drawRoundRect(rect,trackHeight/2,trackHeight/2,paint);paint.alpha=255
        if(value!=null && value.isFinite()) {
            if(filled) {
                fillRect.set(rect.left,rect.top,x(value),rect.bottom)
                paint.color=color;canvas.drawRoundRect(fillRect,trackHeight/2,trackHeight/2,paint)
            } else { paint.color=foreground;canvas.drawCircle(x(value),rect.centerY(),trackHeight/2,paint) }
        }
        paint.color=foreground;paint.alpha=180;paint.strokeWidth=BicycleDesign.hrvTrendBandLineDp*density;paint.pathEffect=dots;paint.strokeCap=Paint.Cap.ROUND
        labels.forEach { (v,_) -> canvas.drawLine(x(v),rect.centerY()-maxOf(trackHeight/2-4*density,6*density),x(v),rect.centerY()+maxOf(trackHeight/2-4*density,6*density),paint) }
        paint.pathEffect=null;paint.strokeCap=Paint.Cap.BUTT;paint.alpha=255
        paint.textSize=BicycleDesign.analysisLabelSp*resources.displayMetrics.scaledDensity;paint.textAlign=Paint.Align.CENTER
        val baseline=rect.bottom+BicycleDesign.horizontalPadding*density-paint.ascent()/2
        if(labels.isEmpty()) canvas.drawText(context.getString(R.string.hrva_range_missing),width/2f,baseline,paint)
        else labels.forEachIndexed { index,(v,text) ->
            val half=paint.measureText(text)/2
            var at=x(v).coerceIn(half,(width-half).coerceAtLeast(half))
            if(labels.size==2 && kotlin.math.abs(x(labels[1].first)-x(labels[0].first))<paint.measureText(labels[0].second)+4*density)
                at=if(index==0) width/2f-half-4*density else width/2f+half+4*density
            canvas.drawText(text,at,baseline,paint)
        }
    }
}
