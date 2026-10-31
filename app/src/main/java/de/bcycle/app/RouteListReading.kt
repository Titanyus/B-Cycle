package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.text.TextUtils
import android.widget.LinearLayout
import android.widget.TextView

/** Common value/unit/label geometry for ride rows and their group summaries. */
internal class RouteListReading(context:Context,themes:ThemeManager,labelId: Int? = null, unitId: Int? = null) : LinearLayout(context) {
    val number = BicycleComponents.text(context, themes, "").apply {
        typeface = BicycleDesign.narrowTypeface(context); textSize = BicycleDesign.routeMetricSp
        gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 1
        setAutoSizeTextTypeUniformWithConfiguration(if (labelId == null) 10 else 12, BicycleDesign.routeMetricSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
    }
    val unit = TextView(context).apply {
        text = unitId?.let(context::getString).orEmpty(); typeface = BicycleDesign.narrowTypeface(context)
        textSize = BicycleDesign.formatSp; gravity = Gravity.CENTER; includeFontPadding = false; maxLines = 1
        BicycleComponents.secondaryText(this,themes)
        setAutoSizeTextTypeUniformWithConfiguration(8, BicycleDesign.formatSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
    }
    val label = BicycleComponents.text(context, themes, labelId?.let(context::getString).orEmpty()).apply {
        textSize = BicycleDesign.routeMetricLabelSp; gravity = Gravity.CENTER; includeFontPadding = false
        maxLines = 1; ellipsize = TextUtils.TruncateAt.END
    }
    init {
        orientation = VERTICAL
        addView(AnalysisValueRow(context, number, unit), LayoutParams(-1, BicycleDesign.dp(context,BicycleDesign.routeMetricHeight)))
        addView(label, LayoutParams(-1, -2))
    }
    fun bind(value: String?, id: String, kind: String) {
        number.text = value.orEmpty(); number.tag = "route_${kind}_$id"
        unit.visibility = if (value == null || unit.text.isEmpty()) GONE else VISIBLE
    }
}
