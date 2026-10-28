package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.util.TypedValue

internal object MapFieldSelection {
    val positions = listOf("Links", "Mitte", "Rechts")
    fun positionLabel(context: Context, index: Int) = context.getString(listOf(R.string.ui_map_position_left, R.string.ui_map_position_center, R.string.ui_map_position_right)[index])
    fun key(index: Int) = "mapDataField_$index"
    fun read(preferences: SharedPreferences, index: Int) =
        preferences.getString(key(index), null)?.let(DashboardField::fromId)
}

/** Three global value-only overlays. Non-interactive views let map gestures pass through. */
internal class MapDataFieldsView(context: Context) : LinearLayout(context) {
    private val directions = IntArray(3)
    private var trendIndicators=TrendIndicatorConfig()
    private val occupied = BooleanArray(3)
    var editing = false; private set
    var selectField: ((Int) -> Unit)? = null
    internal fun bindEditorTheme(themes: ThemeManager) {
        values.forEach { view -> BicycleComponents.bindTheme(view, themes) { tokens ->
            (view.foreground as? DashboardEditOutline)?.color = tokens.primary
        } }
    }
    fun setEditing(value: Boolean, primary: Int) {
        editing = value
        values.forEach { view ->
            view.foreground = if (value) DashboardEditOutline(resources.displayMetrics.density, primary) else null
            view.isClickable = value; view.isFocusable = value
        }
        updateVisibility()
    }
    private fun updateVisibility() {
        values.forEachIndexed { index, view ->
            view.visibility = if (editing || occupied[index]) VISIBLE else INVISIBLE
            view.importantForAccessibility = if (editing || occupied[index]) IMPORTANT_FOR_ACCESSIBILITY_YES else IMPORTANT_FOR_ACCESSIBILITY_NO
            if (editing && !occupied[index]) view.contentDescription = resources.getString(R.string.dashboard_editor_map_slot, index + 1)
        }
        visibility = if (editing || occupied.any { it }) VISIBLE else GONE
    }
    private var wasRecording = false
    private var wasLightOn = false
    private val lightIcon = LightStatusSpan(context)
    private val recordingDot = RecordingDotSpan(resources.displayMetrics.density)
    private var fields = List<DashboardField?>(3) { null }
    val preferredHeightDp get()=if(fields.any { (it?.batterySources?.size ?: 0)>2 }) 76 else 62
    private val values = List(3) { index -> BatteryMapTextView(context).apply {
        tag = "map_value_$index"
        setOnClickListener { if (editing) selectField?.invoke(index) }
        isClickable = false; isFocusable = false
        gravity = Gravity.CENTER
        typeface = BicycleDesign.narrowTypeface(context)
        setTextColor(Color.BLACK); includeFontPadding = false; maxLines = 1
        setAutoSizeTextTypeUniformWithConfiguration(16, 42, 1, TypedValue.COMPLEX_UNIT_SP)
        val inset = (4 * resources.displayMetrics.density).toInt()
        setPadding(inset, 0, inset, 0)
    } }
    private val chevrons=values.map { TrendChevrons(it) }
    private var previousElapsed:Long?=null
    internal fun resetTrends() { chevrons.forEach { it.clear() };directions.fill(0);previousElapsed=null }
    init {
        tag = "map_data_fields"; orientation = HORIZONTAL; isBaselineAligned = false
        values.forEach { addView(it, LayoutParams(0, -1, 1f)) }
        visibility = GONE
    }
    fun reload(preferences: SharedPreferences) {
        trendIndicators=TrendIndicatorSettings.read(preferences)
        val next=List(3) { MapFieldSelection.read(preferences, it) }
        if(next!=fields) { chevrons.forEach { it.clear() };directions.fill(0) }
        fields = next
    }
    fun show(data: LiveData, gps: GpsValues, bikeDistanceM: Long?, pulse: PulseValues, recording: Boolean = false, bikeLight: Boolean? = null,trendSnapshot:RideTrendSnapshot=RideTrendSnapshot()) {
        if(gps.elapsedMs!=null && previousElapsed?.let { gps.elapsedMs<it }==true) { chevrons.forEach { it.clear() } }
        previousElapsed=gps.elapsedMs
        fields.forEachIndexed { index, field ->
            val batterySources=field?.batterySources
            val view=values[index]
            val hasDot=index==1 && recording
            val hasLight=index==1 && bikeLight==true
            view.batteries(batterySources.orEmpty(),data.batteries,hasDot || hasLight)
            if(batterySources!=null) {
                if(view.autoSizeTextType!=TextView.AUTO_SIZE_TEXT_TYPE_NONE) {
                    view.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);view.textSize=22f
                }
                view.gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL
                val marks=if(hasDot && hasLight) "\uFFFC  \uFFFC" else if(hasDot || hasLight) "\uFFFC" else ""
                if(view.text.toString()!=marks || (index==1 && (wasRecording!=recording || wasLightOn!=(bikeLight==true)))) view.text=android.text.SpannableString(marks).apply {
                    if(hasDot) setSpan(recordingDot,0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    if(hasLight) setSpan(lightIcon,length-1,length,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                occupied[index]=true
                view.contentDescription=listOfNotNull(context.getString(R.string.ui_recording_running).takeIf { hasDot },
                    BatteryFields.description(context,batterySources,data.batteries),context.getString(R.string.ui_bike_light_on).takeIf { hasLight }).joinToString(" · ")
                return@forEachIndexed
            }
            if(view.autoSizeTextType==TextView.AUTO_SIZE_TEXT_TYPE_NONE) view.setAutoSizeTextTypeUniformWithConfiguration(16,42,1,TypedValue.COMPLEX_UNIT_SP)
            view.gravity=Gravity.CENTER
            val value = field?.value(data, gps, bikeDistanceM, pulse)
            val next = value?.takeIf { it.available }?.text.orEmpty()
            val calculated = field?.let { trendSnapshot.indicator(it) } ?: 0
            val direction = if(field!=null && trendIndicators.shows(field)) calculated else 0
            chevrons[index].update(direction)
            val hasValue = next.isNotEmpty()
            val prefix = if (hasDot) "\uFFFC" + if (hasValue || hasLight) " " else "" else ""
            val trendPrefix = if (hasValue && chevrons[index].visible) "\uFFFC " else ""
            val suffix = if (hasLight) (if (hasValue) " " else "") + "\uFFFC" else ""
            val displayed: CharSequence = if (hasDot || hasLight || trendPrefix.isNotEmpty()) android.text.SpannableString(prefix + trendPrefix + next + suffix).apply {
                if (hasLight) setSpan(lightIcon, length - 1, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (hasDot) setSpan(recordingDot, 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (trendPrefix.isNotEmpty()) setSpan(AverageChevronSpan(chevrons[index]), prefix.length, prefix.length + 1,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else next
            if (view.text.toString() != displayed.toString() || directions[index] != direction || (index == 1 && (wasRecording != recording || wasLightOn != (bikeLight == true)))) {
                view.text = displayed; directions[index] = direction
            }
            val visible = hasValue || hasDot || hasLight
            occupied[index] = visible
            view.visibility = if (editing || visible) VISIBLE else INVISIBLE
            view.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_YES else View.IMPORTANT_FOR_ACCESSIBILITY_NO
            val description = if (hasValue) context.getString(R.string.ui_field_reading, field!!.title(context, data, gps), next, value!!.unit) else null
            view.contentDescription = listOfNotNull(context.getString(R.string.ui_recording_running).takeIf { hasDot }, description, context.getString(R.string.ui_bike_light_on).takeIf { hasLight }).joinToString(" · ").takeIf { it.isNotEmpty() }
        }
        wasRecording = recording; wasLightOn = bikeLight == true
        updateVisibility()
    }
}

/** A static status mark; no animation or extra timer, and no touch interception. */
internal class RecordingDotSpan(density: Float, color: Int = Color.RED, diameterDp: Float = 13f) : android.text.style.ReplacementSpan() {
    private val radius = diameterDp / 2 * density
    private val dot = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    override fun getSize(paint: android.graphics.Paint, text: CharSequence, start: Int, end: Int,
                         fm: android.graphics.Paint.FontMetricsInt?): Int {
        // A dot-only TextView still needs line metrics, otherwise Android may skip drawing it.
        fm?.let { paint.getFontMetricsInt(it) }
        return kotlin.math.ceil(radius * 2).toInt()
    }
    override fun draw(canvas: android.graphics.Canvas, text: CharSequence, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, paint: android.graphics.Paint) {
        canvas.drawCircle(x + radius, y + (paint.ascent() + paint.descent()) / 2, radius, dot)
    }
}

/** Same static red as the recording dot; cached drawable, no extra layout or touch target. */
private class LightStatusSpan(context: Context) : android.text.style.ReplacementSpan() {
    private val size = (21 * context.resources.displayMetrics.density).toInt()
    private val width = size*4/3
    private val icon = BicycleIcons.drawable(context, BicycleIcon.Light, Color.RED).apply { setBounds(0, 0, width, size) }
    override fun getSize(paint: android.graphics.Paint, text: CharSequence, start: Int, end: Int,
                         fm: android.graphics.Paint.FontMetricsInt?): Int {
        fm?.let { paint.getFontMetricsInt(it) }
        return width
    }
    override fun draw(canvas: android.graphics.Canvas, text: CharSequence, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, paint: android.graphics.Paint) {
        canvas.save(); canvas.translate(x, y + (paint.ascent() + paint.descent() - size) / 2)
        icon.draw(canvas); canvas.restore()
    }
}
