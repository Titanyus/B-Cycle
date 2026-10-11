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
    fun key(index: Int) = "mapDataField_$index"
    fun read(preferences: SharedPreferences, index: Int) =
        preferences.getString(key(index), null)?.let(DashboardField::fromId)
}

/** Three global value-only overlays. Non-interactive views let map gestures pass through. */
internal class MapDataFieldsView(context: Context) : LinearLayout(context) {
    private val directions = IntArray(3)
    private var wasRecording = false
    private var wasLightOn = false
    private val lightIcon = LightStatusSpan(context)
    private val recordingDot = RecordingDotSpan(resources.displayMetrics.density)
    private val trends = AverageTrends()
    private var fields = List<DashboardField?>(3) { null }
    private val values = List(3) { index -> TextView(context).apply {
        tag = "map_value_$index"
        gravity = Gravity.CENTER
        typeface = resources.getFont(R.font.bebas_neue)
        setTextColor(Color.BLACK); includeFontPadding = false; maxLines = 1
        setAutoSizeTextTypeUniformWithConfiguration(16, 42, 1, TypedValue.COMPLEX_UNIT_SP)
        val inset = (4 * resources.displayMetrics.density).toInt()
        setPadding(inset, 0, inset, 0)
    } }
    init {
        tag = "map_data_fields"; orientation = HORIZONTAL; isBaselineAligned = false
        values.forEach { addView(it, LayoutParams(0, -1, 1f)) }
        visibility = GONE
    }
    fun reload(preferences: SharedPreferences) {
        fields = List(3) { MapFieldSelection.read(preferences, it) }
    }
    fun show(data: LiveData, gps: GpsValues, bikeDistanceM: Long?, pulse: PulseValues, recording: Boolean = false, bikeLight: Boolean? = null) {
        fields.forEachIndexed { index, field ->
            val value = field?.value(data, gps, bikeDistanceM, pulse)
            val next = value?.takeIf { it.available }?.text.orEmpty()
            val view = values[index]
            val direction = field?.let { trends.direction(it.id, it.averageNumber(data, gps, pulse), android.os.SystemClock.elapsedRealtime()) } ?: 0
            val hasDot = index == 1 && recording
            val hasLight = index == 1 && bikeLight == true
            val hasValue = next.isNotEmpty()
            val prefix = if (hasDot) "\uFFFC" + if (hasValue || hasLight) " " else "" else ""
            val trendPrefix = if (hasValue && direction != 0) "\uFFFC " else ""
            val suffix = if (hasLight) (if (hasValue) " " else "") + "\uFFFC" else ""
            val displayed: CharSequence = if (hasDot || hasLight || trendPrefix.isNotEmpty()) android.text.SpannableString(prefix + trendPrefix + next + suffix).apply {
                if (hasLight) setSpan(lightIcon, length - 1, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (hasDot) setSpan(recordingDot, 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (trendPrefix.isNotEmpty()) setSpan(AverageChevronSpan(direction), prefix.length, prefix.length + 1,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else next
            if (view.text.toString() != displayed.toString() || directions[index] != direction || (index == 1 && (wasRecording != recording || wasLightOn != (bikeLight == true)))) {
                view.text = displayed; directions[index] = direction
            }
            val visible = hasValue || hasDot || hasLight
            view.visibility = if (visible) VISIBLE else INVISIBLE
            view.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_YES else View.IMPORTANT_FOR_ACCESSIBILITY_NO
            val description = if (hasValue) "${field!!.title(data, gps)}: $next ${value!!.unit}" else null
            view.contentDescription = listOfNotNull("Aufzeichnung läuft".takeIf { hasDot }, description, "Fahrradlicht an".takeIf { hasLight }).joinToString(" · ").takeIf { it.isNotEmpty() }
        }
        wasRecording = recording; wasLightOn = bikeLight == true
        visibility = if (values.any { it.visibility == VISIBLE }) VISIBLE else GONE
    }
}

/** A static status mark; no animation or extra timer, and no touch interception. */
private class RecordingDotSpan(density: Float) : android.text.style.ReplacementSpan() {
    private val radius = 6.5f * density
    private val dot = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
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
    private val icon = BicycleIcons.drawable(context, BicycleIcon.Light, Color.RED).apply { setBounds(0, 0, size, size) }
    override fun getSize(paint: android.graphics.Paint, text: CharSequence, start: Int, end: Int,
                         fm: android.graphics.Paint.FontMetricsInt?): Int {
        fm?.let { paint.getFontMetricsInt(it) }
        return size
    }
    override fun draw(canvas: android.graphics.Canvas, text: CharSequence, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, paint: android.graphics.Paint) {
        canvas.save(); canvas.translate(x, y + (paint.ascent() + paint.descent() - size) / 2)
        icon.draw(canvas); canvas.restore()
    }
}
