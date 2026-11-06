package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

/** Complete provider fields, kept distinct from the normalized steps used by our chart. */
internal class TrainingDetailsView(context:Context,themes:ThemeManager,val workout:TrainingWorkout,profile:RiderProfile,original:JSONObject?):LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="training_detail_view"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        BicycleForm.section(this,themes,R.string.training_details_overview)
        addView(TrainingWorkoutView(context,themes,detailAxes=true).apply { bind(workout,profile,false) })
        fun text(value:String)=BicycleComponents.text(context,themes,value).apply { setTextIsSelectable(true) }
        addView(text(context.getString(R.string.training_details_structure,workout.structureSource.ifBlank { workout.format })))
        if(workout.description.isNotEmpty()) addView(text(workout.description))
        BicycleForm.section(this,themes,R.string.training_details_steps)
        val steps=LinearLayout(context).apply { orientation=VERTICAL;tag="training_detail_steps" }
        var index=0;var elapsed:Double?=0.0
        lateinit var more:android.widget.Button
        fun appendSteps() {
            more.parent?.let { (it as android.view.ViewGroup).removeView(more) }
            val end=minOf(index+32,workout.steps.size)
            while(index<end) {
                val step=workout.steps[index]
                steps.addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.training_details_step,index+1)+if(step.name.isBlank()) "" else " · ${step.name}"))
                val duration=step.seconds?.let { seconds ->
                    val start=elapsed;elapsed=start?.plus(seconds)
                    if(start==null) trainingClock(seconds) else "${trainingClock(start)}–${trainingClock(start+seconds)} · ${trainingClock(seconds)}"
                } ?: run { elapsed=null
                    val condition=when(step.durationType) {
                        "DISTANCE_METERS"->step.durationValue?.let { "${trainingNumber(it)} m" }
                        "DISTANCE"->step.durationValue?.let { "${trainingNumber(it/100)} m" }
                        else->null
                    }
                    condition ?: step.durationType+(step.durationValue?.let { " · ${trainingNumber(it)}" } ?: "")
                }
                steps.addView(text(duration))
                if(step.targets.isEmpty()) steps.addView(text(context.getString(R.string.training_target_free)))
                for(target in step.targets) steps.addView(text(trainingTargetText(context,target)))
                steps.addView(BicycleComponents.sectionDivider(context,themes))
                index++
            }
            if(index<workout.steps.size) steps.addView(more)
        }
        more=BicycleComponents.action(context,themes,context.getString(R.string.training_details_more_steps)) { appendSteps() }
        appendSteps();addView(steps)
        BicycleForm.section(this,themes,R.string.training_details_original)
        if(original==null) addView(text(context.getString(if(workout.source=="Intervals.icu") R.string.training_details_original_missing else R.string.training_details_original_file)))
        else addView(TrainingJsonView(context,themes,original))
    }
}

private fun trainingNumber(value:Double)=NumberFormat.getNumberInstance().apply { maximumFractionDigits=2 }.format(value)
internal fun trainingClock(seconds:Double):String {
    val value=kotlin.math.round(seconds).toLong()
    return if(value>=3600) String.format(Locale.ROOT,"%d:%02d:%02d",value/3600,value/60%60,value%60) else String.format(Locale.ROOT,"%d:%02d",value/60,value%60)
}
internal fun trainingTargetText(context:Context,target:TrainingTarget):String {
    val scale=if(target.unit==TrainingUnit.FTP) 100.0 else 1.0
    val values=trainingNumber(target.low*scale)+(if(target.ramp) " → ${trainingNumber(target.high*scale)}" else if(target.low!=target.high) "–${trainingNumber(target.high*scale)}" else "")
    val unit=when(target.unit) {
        TrainingUnit.FTP->"% FTP";TrainingUnit.WATTS->"W";TrainingUnit.HR_BPM->"bpm"
        TrainingUnit.HR_MAX_PERCENT->"% HFmax";TrainingUnit.HR_THRESHOLD_PERCENT->"% LTHR";TrainingUnit.HR_RESERVE_PERCENT->"% HF-Reserve"
        TrainingUnit.POWER_ZONE->context.getString(R.string.training_power_zone);TrainingUnit.HR_ZONE->context.getString(R.string.training_heart_zone)
        TrainingUnit.CADENCE->"rpm";TrainingUnit.SPEED->"m/s";TrainingUnit.OTHER->target.label
    }
    val kind=context.getString(if(target.ramp) R.string.training_details_ramp else if(target.low!=target.high) R.string.training_details_range else R.string.training_details_target)
    return "$kind: $values $unit"
}

/** Expand nested original fields on demand; every entry stays accessible, including unknown fields. */
internal class TrainingJsonView(context:Context,private val themes:ThemeManager,private val value:Any):LinearLayout(context) {
    private var cursor=0
    private val keys=(value as? JSONObject)?.keys()?.asSequence()?.toList().orEmpty()
    private val size=if(value is JSONArray) value.length() else keys.size
    private val more=BicycleComponents.action(context,themes,context.getString(R.string.training_details_more_fields)) { append() }
    init { orientation=VERTICAL;tag="training_original_fields";append() }
    private fun append() {
        removeView(more)
        repeat(minOf(32,size-cursor)) {
            val index=cursor++;val key=if(value is JSONArray) "[$index]" else keys[index]
            val entry=if(value is JSONArray) value.get(index) else (value as JSONObject).get(key)
            val nested=entry is JSONObject || entry is JSONArray
            if(nested) {
                var loaded=false
                lateinit var frame:BicycleFrameDropdown
                frame=BicycleFrameDropdown(context,themes,key,false,leadingIcon=BicycleIcon.Info) { open ->
                    if(open && !loaded) { loaded=true;frame.content.addView(TrainingJsonView(context,themes,entry)) }
                }
                frame.tag="training_original_$key";addView(frame)
            } else {
                addView(BicycleComponents.text(context,themes,key).apply { typeface=BicycleDesign.strongTypeface })
                val text=if(entry==JSONObject.NULL) "null" else entry.toString()
                // Large strings (e.g. descriptions or attachments) are revealed completely in chunks.
                val content=LinearLayout(context).apply { orientation=VERTICAL }
                var from=0
                lateinit var next:android.widget.Button
                fun chunk() {
                    content.removeView(next)
                    var end=minOf(from+4096,text.length)
                    if(end<text.length && end>from && text[end-1].isHighSurrogate()) end--
                    content.addView(BicycleComponents.text(context,themes,text.substring(from,end)).apply { setTextIsSelectable(true) })
                    from=end;if(from<text.length) content.addView(next)
                }
                next=BicycleComponents.action(context,themes,context.getString(R.string.training_details_more_text)) { chunk() }
                chunk();addView(content)
                addView(BicycleComponents.sectionDivider(context,themes))
            }
        }
        if(cursor<size) addView(more)
        if(size==0) addView(BicycleComponents.text(context,themes,if(value is JSONArray) "[]" else "{}"))
    }
}
