package de.bcycle.app

import android.content.Context
import android.widget.LinearLayout
import java.util.Locale

/** Compact workout overview; original import data stays in storage. */
internal class TrainingDetailsView(context:Context,themes:ThemeManager,val workout:TrainingWorkout,profile:RiderProfile):LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="training_detail_view"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        BicycleForm.section(this,themes,R.string.training_details_overview)
        addView(TrainingWorkoutView(context,themes,detailAxes=true).apply { bind(workout,profile,false) })
        fun text(value:String)=BicycleComponents.text(context,themes,value).apply { setTextIsSelectable(true) }
        addView(text(context.getString(R.string.training_metrics_explanation)))
        addView(text(context.getString(R.string.training_details_structure,workout.structureSource.ifBlank { workout.format })))
        if(workout.description.isNotEmpty()) addView(text(workout.description))
    }
}

internal fun trainingClock(seconds:Double):String {
    val value=kotlin.math.round(seconds).toLong()
    return if(value>=3600) String.format(Locale.ROOT,"%d:%02d:%02d",value/3600,value/60%60,value%60) else String.format(Locale.ROOT,"%d:%02d",value/60,value%60)
}
