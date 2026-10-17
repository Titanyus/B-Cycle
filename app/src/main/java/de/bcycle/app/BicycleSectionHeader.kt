package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout

/** Shared category heading. Natural and pinned states use exactly the same native view. */
internal class BicycleSectionHeader(context: Context, themes: ThemeManager, title: String, private val inlineRules:Boolean=false) : LinearLayout(context) {
    /** Optional section boundary, e.g. an independent accordion after the last category. */
    var sectionEnd: View? = null
    val titleView = BicycleComponents.text(context, themes, title, true)
    init {
        BicycleComponents.bindTheme(titleView,themes) { titleView.setTextColor(it.accent) }
        orientation = if(inlineRules) HORIZONTAL else VERTICAL; gravity=Gravity.CENTER_VERTICAL; isAccessibilityHeading = true; contentDescription = title
        tag = "section_header_$title"
        BicycleComponents.background(this, themes, ThemeRole.SECONDARY)
        // Keep the separation opaque even when this same header is pinned above controls.
        outlineProvider = null
        fun rule() = View(context).apply {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            BicycleComponents.bindTheme(this, themes) { setBackgroundColor(it.foreground) }
        }
        fun addRule() {
            val thickness=BicycleDesign.linePixels(context,BicycleDesign.sectionRuleDp)
            addView(rule(),if(inlineRules) LayoutParams(0,thickness,1f) else LayoutParams(-1,thickness))
        }
        addRule()
        addView(titleView.apply {
            gravity = Gravity.CENTER
            minimumHeight = BicycleDesign.dp(context, BicycleDesign.sectionHeaderMinHeight)
            val horizontal = BicycleDesign.dp(context, if(inlineRules) BicycleDesign.controlGap else BicycleDesign.horizontalPadding)
            val vertical = BicycleDesign.dp(context, BicycleDesign.sectionHeaderPadding)
            setPadding(horizontal, vertical, horizontal, vertical)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LayoutParams(if(inlineRules) -2 else -1, -2))
        addRule()
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        if(inlineRules) titleView.maxWidth=((MeasureSpec.getSize(widthMeasureSpec)-paddingLeft-paddingRight)*BicycleDesign.profileSectionTitleFraction).toInt().coerceAtLeast(1)
        super.onMeasure(widthMeasureSpec,heightMeasureSpec)
    }
    // An opaque pinned header must not allow taps through to covered controls. ScrollView still
    // intercepts drag gestures normally; this heading adds no click action or alternate navigation.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }
    override fun performClick(): Boolean = super.performClick()
}
