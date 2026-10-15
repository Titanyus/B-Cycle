package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout

/** Compact shared distance scale: each background is 100%, each foreground its zone's share. */
internal class RouteZoneBars(context: Context, themes: ThemeManager, distribution: ZoneDistanceDistribution,
    colors: List<Int>, heading: Int) : LinearLayout(context) {
    init {
        orientation = VERTICAL; tag = "analysis_zones_$heading"
        val gap = BicycleDesign.dp(context, BicycleDesign.smallGap)
        setPadding(gap, 0, gap, gap)
        distribution.percentages.forEachIndexed { index, percent ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
                contentDescription = context.getString(R.string.analysis_zone_description, context.getString(heading), index + 1,
                    context.getString(R.string.analysis_zone_percent, percent))
            }
            fun label(text: String) = BicycleComponents.text(context, themes, text).apply {
                textSize = BicycleDesign.analysisUnitSp; maxLines = 1; includeFontPadding = false
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            row.addView(label(context.getString(R.string.analysis_zone_label, index + 1)), LayoutParams(-2, -2).apply { marginEnd = gap })
            row.addView(RouteZoneBar(context, colors[index], percent), LayoutParams(0, BicycleDesign.dp(context, BicycleDesign.analysisZoneBarHeight), 1f))
            row.addView(label(context.getString(R.string.analysis_zone_percent, percent)).apply { gravity = Gravity.END },
                LayoutParams(BicycleDesign.dp(context, 56), -2).apply { marginStart = gap })
            addView(row, LayoutParams(-1, BicycleDesign.dp(context, BicycleDesign.analysisZoneRowHeight)))
        }
    }
}
internal class RouteZoneBar(context: Context, val zoneColor: Int, val percent: Double) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = zoneColor }
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    internal fun foregroundWidth() = width * (percent / 100).coerceIn(0.0, 1.0).toFloat()
    override fun onDraw(canvas: Canvas) {
        val radius = height / 2f
        paint.alpha = (255 * BicycleDesign.analysisZoneTrackAlpha).toInt()
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
        paint.alpha = 255
        if (percent > 0) canvas.drawRoundRect(0f, 0f, foregroundWidth(), height.toFloat(), radius, radius, paint)
    }
}
