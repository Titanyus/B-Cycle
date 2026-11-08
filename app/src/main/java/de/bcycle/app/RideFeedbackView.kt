package de.bcycle.app

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*

internal object RideRpe {
    fun label(value:Int)=when(value) { 0->R.string.ride_rpe_none;1->R.string.ride_rpe_very_easy;2->R.string.ride_rpe_easy;3->R.string.ride_rpe_moderate;4->R.string.ride_rpe_somewhat_hard;5,6->R.string.ride_rpe_hard;7,8,9->R.string.ride_rpe_very_hard;else->R.string.ride_rpe_maximum }
    fun slider(context:Context,themes:ThemeManager,id:String,changed:(Int)->Unit={}):LinearLayout = LinearLayout(context).apply {
        orientation=LinearLayout.VERTICAL;tag="ride_rpe_control"
        addView(BicycleComponents.fieldLabel(context,themes,context.getString(R.string.ride_rpe_question)))
        val value=BicycleComponents.text(context,themes,"").apply { tag="ride_rpe_label";gravity=Gravity.CENTER }
        val initial=RideAnnotations.read(context,id).rpe
        val slider=BicycleRatingSlider(context,themes,0,10).apply { tag="ride_rpe_slider";progress=initial ?: 0;contentDescription=context.getString(R.string.ride_rpe_question) }
        fun label(rpe:Int?) { value.text=rpe?.let { "$it · ${context.getString(RideRpe.label(it))}" } ?: context.getString(R.string.ride_rpe_unrated);slider.stateDescription=value.text }
        label(initial)
        slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar:SeekBar)=Unit
            override fun onStopTrackingTouch(bar:SeekBar) { RideAnnotations.rpe(context,id,bar.progress);label(bar.progress);changed(bar.progress) }
            override fun onProgressChanged(bar:SeekBar,progress:Int,fromUser:Boolean) { if(fromUser) { RideAnnotations.rpe(context,id,progress);label(progress);changed(progress) } }
        })
        addView(slider,LinearLayout.LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.touchSize)))
        addView(value)
    }
}
/** Text is saved on every change; leaving/focus loss never requires a confirmation button. */
internal class RideNoteEditor(context:Context,themes:ThemeManager,private val rideId:String):EditText(context) {
    private val preferences=context.getSharedPreferences("ride-annotations",0)
    private fun status() { error=if(RideAnnotations.problem(context,rideId)) context.getString(R.string.ride_note_external_failed) else null }
    private val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,key -> if(key=="problem:$rideId") post { status() } }
    init {
        setTag(R.id.input_body_typography,true)
        tag="ride_note_editor";hint=context.getString(R.string.ride_note_hint)
        inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        gravity=Gravity.TOP or Gravity.START;setLines(3);setHorizontallyScrolling(false)
        filters=arrayOf(InputFilter.LengthFilter(RideAnnotation.MAX_NOTE))
        typeface=BicycleDesign.bodyTypeface;textSize=BicycleDesign.rowTitleSp
        setText(RideAnnotations.read(context,rideId).note)
        BicycleForm.outlinedInput(this,themes)
        onTextChanged { RideAnnotations.note(context,rideId,text.toString()) }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();preferences.registerOnSharedPreferenceChangeListener(listener);status() }
    override fun onDetachedFromWindow() { preferences.unregisterOnSharedPreferenceChangeListener(listener);RideAnnotations.flushAsync(context,rideId);super.onDetachedFromWindow() }
}
