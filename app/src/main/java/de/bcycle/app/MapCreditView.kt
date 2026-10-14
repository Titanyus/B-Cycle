package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView

/** Real rotated text layout with matching bounds/touch area; wraps instead of clipping on short maps. */
internal class MapCreditView(context: Context) : ViewGroup(context) {
    internal val label = TextView(context).apply {
        textSize = BicycleDesign.mapAttributionSp; setTextColor(Color.BLACK)
        setPadding(dp(5), dp(3), dp(5), dp(3))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        rotation = -90f
    }
    var text: CharSequence
        get() = label.text
        set(value) { label.text = value; requestLayout() }
    init {
        addView(label); setBackgroundColor(Color.argb(220, 255, 255, 255))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    private fun dp(n: Int) = BicycleDesign.dp(context, n)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val length = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) dp(240) else MeasureSpec.getSize(heightMeasureSpec)
        val thickness = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) dp(240) else MeasureSpec.getSize(widthMeasureSpec)
        label.measure(MeasureSpec.makeMeasureSpec(length, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(thickness, MeasureSpec.AT_MOST))
        setMeasuredDimension(resolveSize(label.measuredHeight, widthMeasureSpec), resolveSize(label.measuredWidth, heightMeasureSpec))
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val w = label.measuredWidth; val h = label.measuredHeight
        label.layout((width - w) / 2, (height - h) / 2, (width - w) / 2 + w, (height - h) / 2 + h)
        label.pivotX = w / 2f; label.pivotY = h / 2f
    }
}
