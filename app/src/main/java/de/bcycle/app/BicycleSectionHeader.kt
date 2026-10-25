package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout

/** Shared category heading. Natural and pinned states use exactly the same native view. */
internal class BicycleSectionHeader(context: Context, internal val manager: ThemeManager, title: String, private val inlineRules:Boolean=false) : LinearLayout(context) {
    /** Optional section boundary, e.g. an independent accordion after the last category. */
    var sectionEnd: View? = null
    var expanded:Boolean?=null;private set
    var cardContent:View?=null;private set
    var revealOnOpen=false
    val titleView = BicycleComponents.text(context, manager, title).apply {
        tag="bicycle_section_title";setTag(R.id.section_title_role,true);BicycleComponents.sectionTypography(this)
    }
    init {
        BicycleComponents.bindTheme(titleView,manager) { titleView.setTextColor(it.foreground) }
        orientation = if(inlineRules) HORIZONTAL else VERTICAL; gravity=Gravity.CENTER_VERTICAL; isAccessibilityHeading = true; contentDescription = title
        tag = "section_header_$title"
        BicycleComponents.bindTheme(this,manager,::paintHeader)
        // Keep the separation opaque even when this same header is pinned above controls.
        outlineProvider = null
        fun rule() = View(context).apply {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            BicycleComponents.bindTheme(this, manager) {
                setBackgroundColor(it.foreground)
                alpha=0f
            }
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
    private fun paintHeader(tokens:ThemeTokens) {
        val flat=BicycleAppearance.flat(context)
        val radius=if(flat) 0f else BicycleDesign.dp(context,BicycleDesign.sectionCardRadius).toFloat()
        background=object:android.graphics.drawable.GradientDrawable() {
            private val matte=android.graphics.Paint().apply { color=tokens.secondary }
            override fun draw(canvas:android.graphics.Canvas) {
                // Keep all four corners visible when this header floats above its section body.
                if(!flat) canvas.drawRect(bounds,matte)
                super.draw(canvas)
            }
        }.apply { setColor(tokens.tertiary);cornerRadii=FloatArray(8) { radius } }
    }
    /** Opt-in collapsing; the title remains centered by reserving equal icon space on each side. */
    fun accordion(content:View,open:Boolean=true,changed:(Boolean)->Unit={}) {
        isClickable=true;isFocusable=true
        cardContent=content
        BicycleSectionCards.content(content,this)
        val themesUpdate:(ThemeTokens)->Unit={ tokens ->
            (parent as? View)?.invalidate()
            val size=BicycleDesign.dp(context,BicycleDesign.iconSize)
            val left=android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT).apply { setBounds(0,0,size,size) }
            val right=BicycleIcons.drawable(context,BicycleIcon.Expand,tokens.foreground).apply { setBounds(0,0,size,size) }
            val icon=android.graphics.drawable.RotateDrawable().apply {
                drawable=right;fromDegrees=if(expanded==true) 180f else 0f;toDegrees=fromDegrees;level=10000
                setBounds(0,0,size,size)
            }
            titleView.setCompoundDrawablesRelative(left,null,icon,null)
            titleView.compoundDrawablePadding=BicycleDesign.dp(context,BicycleDesign.controlGap)
        }
        fun show(value:Boolean) {
            if(!value && content.hasFocus()) {
                content.findFocus()?.clearFocus()
                context.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(content.windowToken,0)
            }
            expanded=value;content.visibility=if(value) VISIBLE else GONE
            stateDescription=context.getString(if(value) R.string.zone_expanded else R.string.zone_collapsed)
            themesUpdate(manager.active.tokens)
        }
        show(open)
        setOnClickListener {
            val pinned=translationY>0f
            show(content.visibility!=VISIBLE);changed(expanded==true)
            if(revealOnOpen && expanded==true) BicycleContentSpacing.revealAfterLayout(this)
            else if(pinned) post {
                var y=top;var parent=this.parent
                while(parent is View && parent !is android.widget.ScrollView) { y+=parent.top;parent=parent.parent }
                (parent as? android.widget.ScrollView)?.scrollTo(0,y)
            }
        }
        BicycleComponents.bindTheme(this,manager,themesUpdate)
        accessibilityDelegate=object:View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host:View,info:android.view.accessibility.AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host,info);info.className="android.widget.Button"
            }
        }
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        if(inlineRules) titleView.maxWidth=((MeasureSpec.getSize(widthMeasureSpec)-paddingLeft-paddingRight)*BicycleDesign.profileSectionTitleFraction).toInt().coerceAtLeast(1)
        super.onMeasure(widthMeasureSpec,heightMeasureSpec)
    }
    // An opaque pinned header must not allow taps through to covered controls. ScrollView still
    // intercepts drag gestures normally; opt-in accordions and rename headers handle clicks.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }
    override fun performClick(): Boolean = super.performClick()
}
