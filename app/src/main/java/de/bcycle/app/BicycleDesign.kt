package de.bcycle.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.GridLayout
import android.widget.ScrollView
import android.widget.LinearLayout
import android.widget.TextView

/** Global geometry and typography, deliberately independent of every palette. DP unless noted. */
object BicycleDesign {
    // Android's sans-serif family uses Roboto on stock Android; request Black without bundling a font.
    val strongTypeface: android.graphics.Typeface by lazy {
        android.graphics.Typeface.create(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL),900,false)
    }
    // Secondary metric text composites with its actual surface instead of using opaque gray.
    val dashboardUnitColor = android.graphics.Color.argb(166, 255, 255, 255) // 65% opacity (8-bit alpha)
    const val dashboardCaptionSp = 13.5f
    const val tripleValueSp = 45f
    const val tripleUnitSp = 10f
    const val tripleHorizontalPadding = 3
    const val dashboardLargeCompactRatio = .75f / .85f
    const val titleSp = 23f
    const val bodySp = 14f
    const val inputSp = 16f
    const val headerHeight = 64
    const val headingSp = 32f
    const val rowTitleSp = 16f
    const val routeMetricSp = 20f
    const val riderZoneRangeWidth = 116
    const val formNumberWidth = 116
    const val formDateWidth = 148
    const val routeMetricHeight = 24
    const val routeMetricTopOffset = 3
    const val routeMetricLabelSp = 10f
    const val formatSp = 12f
    const val metadataHeight = 28
    const val metadataSpacingTrim = 4
    const val previewWidth = 56
    const val previewHeight = 50
    const val rowGap = 12
    const val popupRowHeight = 56
    const val popupWidth = 300
    const val managerMaxHeight = 680
    const val managerDefaultHeight = 580
    const val analysisTileMinHeight = 120
    const val analysisMetricIconSize = 32
    const val analysisMetricSp = 28f
    const val analysisUnitSp = 13f
    const val analysisUnitMinSp = 8
    const val analysisLabelSp = 11f
    const val sensorStatusSp = 13f
    const val mapStatusSp = 11f
    const val mapAttributionSp = 11f
    const val analysisZoneRowHeight = 24
    const val analysisZoneBarHeight = 10
    const val analysisZoneTrackAlpha = .18f
    const val analysisChartFillAlpha = 255
    const val analysisChartHeight = 120
    const val analysisSideLabelWidth = 24
    const val analysisSideLabelSp = 12f
    const val bikeVisibleTiles = 3
    const val bikeQuickVisibleTiles = 6
    const val bikeQuickScale = .5f
    const val bikeTileHeight = 154
    const val bikePhotoHeight = 200
    const val bikePhotoEmptyHeight = 64
    const val bikePreviewHeight = 56
    const val sectionHeaderMinHeight = 56
    const val sectionHeaderPadding = 12
    const val sectionRuleDp = .75f
    const val pageContentGap = 16
    const val sectionContentGap = 16
    const val sectionHeaderRaisedZ = 16
    const val headerShadowHeight = 12
    const val headerShadowAlpha = 102
    const val graphChoiceDotDiameter = 10.5f
    const val settingsRowHeight = 72
    const val settingsIconSize = 32
    const val touchSize = 48
    const val compactActionWidth = 36
    const val iconSize = 24
    const val gridColumns = 3
    const val gridRows = 2
    const val scrollFillViewport = false
    const val scrollClipToPadding = false
    // Data source stays above this bounded inner list; other accordions scroll in the outer body.
    const val horizontalPadding = 16
    const val verticalPadding = 8
    const val controlGap = 8
    const val headerHorizontalPadding = 24
    const val headerVerticalPadding = 12
    const val dialogRadius = 18
    const val inputRadius = 6
    const val scrollbarSize = 2
    const val footerHeight = 64
    const val footerLabelSp = 11f
    const val dividerDp = .75f
    fun linePixels(context: Context, widthDp: Float = dividerDp) = Math.round(context.resources.displayMetrics.density * widthDp).coerceAtLeast(1)
    const val dividerAlpha = .55f
    const val sectionDividerGap = 12
    const val analysisDividerGap = 2
    const val inputHorizontalPadding = 12
    const val menuIconSize = 64
    const val menuTileHeight = 148
    const val menuHeaderHeight = 124
    const val menuTileHorizontalPadding = 3
    const val smallGap = 4
    const val presetPreviewWidth = 60
    const val presetPreviewHeight = 84
    const val presetTileHeight = 132
    const val presetTileGap = 2
    const val modeGlowWidth = 24
    const val modeGlowAlpha = 140
    const val ambientGlowPasses = 3
    const val themeColumns = 4
    const val themeTileMinimumWidth = 68
    const val themeTileHeight = 136
    const val themeNameHeight = 44
    const val themeNameSp = 14f
    const val themeSelectedBorder = 2
    fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()

}

enum class ThemeRole { PRIMARY, SECONDARY, TERTIARY, FOREGROUND, ACCENT }
fun ThemeTokens.color(role: ThemeRole) = when (role) {
    ThemeRole.PRIMARY -> primary
    ThemeRole.SECONDARY -> secondary
    ThemeRole.TERTIARY -> tertiary
    ThemeRole.FOREGROUND -> foreground
    ThemeRole.ACCENT -> accent
}

/** Contrast is contextual; no theme-name or header-specific color exception. */
fun ThemeTokens.iconColor(role: ThemeRole, backgroundRole: ThemeRole = ThemeRole.SECONDARY): Int =
    if (role == ThemeRole.ACCENT && ThemeValidator.contrast(accent, color(backgroundRole)) < ThemeValidator.minimumIconContrast)
        foreground else color(role)

/** Shared native components. Geometry is independent of the active palette. */
object BicycleComponents {
    private fun dp(context: Context, value: Int) = BicycleDesign.dp(context, value)
    /** Geometry only: keep children, adapters, gestures and the current scroll position intact. */
    internal fun scrollStyle(view: ViewGroup) {
        if (view !is ScrollView && view !is android.widget.AbsListView) return
        view.clipToPadding = BicycleDesign.scrollClipToPadding
        view.scrollBarSize = dp(view.context, BicycleDesign.scrollbarSize)
        if (view is ScrollView) view.isFillViewport = BicycleDesign.scrollFillViewport
    }
    private val spacedControls = java.util.WeakHashMap<View, Boolean>()
    /** Form controls only: preserve caller dimensions, weights and intentional larger margins. */
    internal fun controlSpacing(view: View) {
        fun apply() {
            val margins=view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
            val parent=view.parent as? LinearLayout
            val index=parent?.indexOfChild(view) ?: -1
            val preceding=if(parent!=null && index>0) (index-1 downTo 0).map(parent::getChildAt).firstOrNull { it.visibility!=View.GONE } else null
            val separated=parent?.orientation==LinearLayout.VERTICAL && preceding?.tag=="bicycle_section_divider"
            val gap=if(separated) 0 else dp(view.context,BicycleDesign.controlGap)
            var changed=false
            if((separated && margins.topMargin!=0) || margins.topMargin<gap) { margins.topMargin=gap;changed=true }
            if(parent?.orientation==LinearLayout.HORIZONTAL) {
                val index=parent.indexOfChild(view)
                val previous=(index-1 downTo 0).firstOrNull { parent.getChildAt(it).visibility!=View.GONE }?.let(parent::getChildAt)
                if(previous is android.widget.Button || previous is android.widget.Spinner) {
                    val previousEnd=(previous.layoutParams as? ViewGroup.MarginLayoutParams)?.marginEnd ?: 0
                    if(previousEnd+margins.marginStart<gap) {
                        margins.marginStart=gap-previousEnd;changed=true
                    }
                }
            }
            if(changed) view.requestLayout()
        }
        apply()
        if(spacedControls.put(view,true)!=null) return
        // Constructors often style before addView assigns/replaces the final LayoutParams.
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = apply()
            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
    internal fun bindTheme(view: View, manager: ThemeManager, apply: (ThemeTokens) -> Unit) {
        apply(manager.active.tokens)
        var subscription: ThemeSubscription? = null
        fun subscribe() { if (subscription == null) subscription = manager.observe { apply(it.tokens) } }
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = subscribe()
            override fun onViewDetachedFromWindow(view: View) { subscription?.close(); subscription = null }
        })
        if (view.isAttachedToWindow) subscribe()
    }
    private fun surface(view: View, manager: ThemeManager, role: ThemeRole, radius: Int, header: Boolean = false) {
        val background = GradientDrawable().apply {
            val r = dp(view.context, radius).toFloat()
            if (header) cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f) else cornerRadius = r
        }
        view.background = background
        bindTheme(view, manager) { background.setColor(it.color(role)) }
    }
    internal fun heading(context: Context, manager: ThemeManager, value: String) = text(context, manager, value).apply {
        typeface = context.resources.getFont(R.font.bebas_neue); textSize = BicycleDesign.headingSp
        gravity = Gravity.CENTER; isAccessibilityHeading = true; maxLines = 1
    }
    internal fun icon(context: Context, manager: ThemeManager, icon: BicycleIcon, description: String,
                      click: () -> Unit) = icon(context, manager,
        BicycleIcons.drawable(context, icon, manager.active.tokens), description, icon.colorRole, click)
    internal fun background(view: View, manager: ThemeManager, role: ThemeRole, radius: Int = 0, header: Boolean = false) = surface(view, manager, role, radius, header)
    internal fun selectionBorder(tokens: ThemeTokens, selected: Boolean) = if (selected) tokens.accent else tokens.foreground
    fun header(context: Context, manager: ThemeManager) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        layoutParams = ViewGroup.LayoutParams(-1, dp(context, BicycleDesign.headerHeight))
        minimumHeight = dp(context, BicycleDesign.headerHeight)
        setPadding(dp(context, BicycleDesign.headerHorizontalPadding), dp(context, BicycleDesign.headerVerticalPadding),
            dp(context, BicycleDesign.headerHorizontalPadding), dp(context, BicycleDesign.headerVerticalPadding))
        surface(this, manager, ThemeRole.PRIMARY, BicycleDesign.dialogRadius, header = true)
    }
    fun dialogBody(context: Context, manager: ThemeManager) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(context, BicycleDesign.horizontalPadding), dp(context, BicycleDesign.verticalPadding),
            dp(context, BicycleDesign.horizontalPadding), dp(context, BicycleDesign.verticalPadding))
        surface(this, manager, ThemeRole.SECONDARY, BicycleDesign.dialogRadius)
    }
    fun listRow(context: Context, manager: ThemeManager) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(context, BicycleDesign.touchSize)
        setPadding(dp(context, BicycleDesign.horizontalPadding), dp(context, BicycleDesign.verticalPadding),
            dp(context, BicycleDesign.horizontalPadding), dp(context, BicycleDesign.verticalPadding))
        surface(this, manager, ThemeRole.SECONDARY, 0)
    }
    fun menuGrid(context: Context, manager: ThemeManager) = GridLayout(context).apply {
        columnCount = BicycleDesign.gridColumns; rowCount = BicycleDesign.gridRows
        alignmentMode = GridLayout.ALIGN_MARGINS; useDefaultMargins = false
        surface(this, manager, ThemeRole.SECONDARY, 0)
    }
    fun scrollBody(context: Context, manager: ThemeManager) = ScrollView(context).apply {
        scrollStyle(this)
        addView(dialogBody(context, manager))
    }
    fun text(context: Context, manager: ThemeManager, value: String, title: Boolean = false) = TextView(context).apply {
        text = value; textSize = if (title) BicycleDesign.titleSp else BicycleDesign.bodySp
        if (title) { typeface=BicycleDesign.strongTypeface;isAccessibilityHeading=true }
        bindTheme(this, manager) { setTextColor(it.foreground) }
    }
    /** Explanatory form copy keeps the shared gap; labels and metric text keep their own geometry. */
    fun description(context: Context, manager: ThemeManager, value: String) = text(context,manager,value).apply {
        layoutParams=LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(context,BicycleDesign.controlGap) }
        controlSpacing(this)
    }
    fun input(context: Context, manager: ThemeManager) = EditText(context).apply {
        textSize = BicycleDesign.inputSp; minimumHeight = dp(context, BicycleDesign.touchSize)
        setPadding(dp(context, 12), dp(context, BicycleDesign.verticalPadding), dp(context, 12), dp(context, BicycleDesign.verticalPadding))
        surface(this, manager, ThemeRole.TERTIARY, BicycleDesign.inputRadius)
        bindTheme(this, manager) {
            setTextColor(it.foreground); setHintTextColor(it.foreground); backgroundTintList = null
            textCursorDrawable?.mutate()?.setTint(it.foreground)
            textSelectHandle?.mutate()?.setTint(it.foreground); textSelectHandleLeft?.mutate()?.setTint(it.foreground); textSelectHandleRight?.mutate()?.setTint(it.foreground)
        }
    }
    fun action(context: Context, manager: ThemeManager, label: String, click: () -> Unit) =
        action(context, manager, label, true, click)
    internal fun action(context: Context, manager: ThemeManager, label: String, formSpacing: Boolean, click: () -> Unit) = Button(context).apply {
        text = label; textSize = BicycleDesign.bodySp; isAllCaps = false
        minimumWidth = dp(context, BicycleDesign.touchSize); minimumHeight = dp(context, BicycleDesign.touchSize)
        surface(this, manager, ThemeRole.TERTIARY, BicycleDesign.inputRadius)
        bindTheme(this, manager) { setTextColor(it.foreground); backgroundTintList = null }
        setOnClickListener { click() }
        if (formSpacing) controlSpacing(this)
    }
    fun icon(context: Context, manager: ThemeManager, drawable: android.graphics.drawable.Drawable,
             description: String, colorRole: ThemeRole = ThemeRole.FOREGROUND, click: () -> Unit) = ImageButton(context).apply {
        setImageDrawable(drawable); contentDescription = description
        layoutParams = ViewGroup.LayoutParams(dp(context, BicycleDesign.touchSize), dp(context, BicycleDesign.touchSize))
        val inset = dp(context, (BicycleDesign.touchSize - BicycleDesign.iconSize) / 2)
        setPadding(inset, inset, inset, inset); background = null
        bindTheme(this, manager) { setColorFilter(it.iconColor(colorRole)) }
        setOnClickListener { click() }
    }
    fun divider(context: Context, manager: ThemeManager) = View(context).apply {
        layoutParams = ViewGroup.LayoutParams(-1, BicycleDesign.linePixels(context))
        bindTheme(this, manager) { setBackgroundColor(it.foreground) }
    }
    fun sectionDivider(context: Context, manager: ThemeManager, gap: Int = BicycleDesign.sectionDividerGap) = divider(context, manager).apply {
        tag="bicycle_section_divider"
        alpha = BicycleDesign.dividerAlpha
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(-1, BicycleDesign.linePixels(context)).apply {
            topMargin = dp(context, gap); bottomMargin = dp(context, gap)
        }
    }
}
