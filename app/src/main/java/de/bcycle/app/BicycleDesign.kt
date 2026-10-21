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
    const val dashboardToolbarMainWeight=2.3f
    const val dashboardToolbarActionWeight=.9f
    const val hrvCircleHeight=320
    const val hrvGraphHeight=286
    const val hrvRingWidth=9f*1.35f
    const val hrvPulseSp=72f
    const val hrvTimeSp=18f
    // Endpoint grows 40% from its previous size, independently of the thicker ring.
    const val hrvRingEndScale=1.3f*1.4f/1.35f
    val hrvAxisColor=0x80ffffff.toInt()
    const val hrvGlowFadeMs=1500L
    const val hrvGraphWindowMs=60000L
    const val hrvActionOffset=54
    const val fastScrollThumbHeight=40f
    const val fastScrollTouchWidth=24f
    const val fastScrollPreviewMaxWidth=180f
    const val fastScrollPreviewHeight=40f
    const val fastScrollPreviewPadding=12f
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
    // Normal content headings, as introduced in grouped settings help.
    const val contentHeadingSp = 14f
    const val inputSp = 16f
    const val choiceSp = bodySp
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
    const val settingsNarrowScale = .75f
    const val settingsMetricSp = 28f * settingsNarrowScale
    const val settingsUnitSp = formatSp * settingsNarrowScale
    const val themeDropdownHeight = 48
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
    const val bikeQuickVisibleTiles = 3
    const val bikeQuickScale = 1f
    const val bikeTileHeight = 154
    const val bikePhotoAspectRatio = 16f / 9f
    const val bikePhotoPlaceholderIconSize = 72
    const val bikePreviewHeight = 56
    const val sectionHeaderMinHeight = 56
    const val sectionHeaderPadding = 12
    const val sectionRuleDp = .75f
    const val pageContentGap = 16
    const val sectionContentGap = 16
    const val sectionEndGap = 32
    const val inlineControlRowTag = "bicycle_inline_control_row"
    const val profileDropdownSp = choiceSp
    const val profileAccordionRadius = 10
    const val profileAccordionHeaderHeight = 64
    const val profileAccordionLeadingWidth = 3
    const val profileAccordionBodyInset = 4
    const val profileSectionTitleFraction = .78f
    const val sectionHeaderRaisedZ = 16
    const val headerShadowHeight = 12
    const val headerShadowAlpha = 102
    const val graphChoiceDotDiameter = 10.5f
    const val settingsRowHeight = 72
    const val settingsIconSize = 32
    const val switchWidth = 48
    const val switchHeight = 24
    const val switchThumbDiameter = 18
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
    const val controlBorder = 1
    const val controlBorderAlpha = .5f
    const val inputRadius = 6
    const val scrollbarSize = 2
    const val tileDropSettleMs = 260L
    const val tileDropOvershoot = 1.4f
    const val sectionDividerGap = 16
    const val footerRuleDp = 1f
    const val footerHeight = 64
    const val modeFooterTransitionMs = 180L
    const val modeFooterHysteresis = 24
    const val footerLabelSp = 11f
    const val dividerDp = .75f
    fun linePixels(context: Context, widthDp: Float = dividerDp) = Math.round(context.resources.displayMetrics.density * widthDp).coerceAtLeast(1)
    const val dividerAlpha = .55f
    const val analysisDividerGap = 2
    const val inputHorizontalPadding = 12
    const val menuIconSize = 64
    const val menuTileHeight = 148
    const val menuHeaderHeight = 124
    const val menuTileHorizontalPadding = 3
    const val smallGap = 4
    const val segmentIconTextGap = 2
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
    const val settingsThemeNameSp = settingsMetricSp
    const val themeSelectedBorder = 2
    fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()

}

/** Borderless text action with native button semantics and touch target. */
internal class BicycleTextAction(context:Context):Button(context)

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

internal val ThemeTokens.secondaryTextColor: Int get() = (foreground and 0x00ffffff) or (166 shl 24)

/** Shared native components. Geometry is independent of the active palette. */
object BicycleComponents {
    private fun dp(context: Context, value: Int) = BicycleDesign.dp(context, value)
    /** Label and chevron expand details; the isolated outer switch alone changes activation. */
    internal fun switchAccordion(row:LinearLayout,toggle:android.widget.Switch,expand:View,manager:ThemeManager,
        existingLabel:TextView?=null,icon:BicycleIcon?=null) {
        val title=existingLabel?.text ?: toggle.text
        val label=existingLabel ?: text(row.context,manager,title.toString())
        label.tag="${toggle.tag}_label";label.gravity=Gravity.START or Gravity.CENTER_VERTICAL
        label.isClickable=true;label.isFocusable=true
        label.setOnClickListener { expand.performClick() }
        if(icon!=null) BicycleDialogStyle.actionIcon(label,manager,icon)
        toggle.contentDescription=title;toggle.text=""
        toggle.setCompoundDrawablesRelative(null,null,null,null)
        toggle.setPadding(0,0,0,0);toggle.switchPadding=0
        row.removeAllViews()
        row.setTag(R.id.inline_control_row,true);row.gravity=Gravity.CENTER_VERTICAL
        row.addView(label,LinearLayout.LayoutParams(0,-1,1f))
        row.addView(expand,LinearLayout.LayoutParams(dp(row.context,BicycleDesign.touchSize),dp(row.context,BicycleDesign.touchSize)))
        row.addView(toggle,LinearLayout.LayoutParams(dp(row.context,BicycleDesign.switchWidth),-1).apply { marginStart=dp(row.context,BicycleDesign.controlGap) })
    }
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
            if(parent?.tag==BicycleDesign.inlineControlRowTag || parent?.getTag(R.id.inline_control_row)==true) {
                if(margins.topMargin!=0 || margins.bottomMargin!=0) {
                    margins.topMargin=0;margins.bottomMargin=0;view.requestLayout()
                }
                return
            }
            val index=parent?.indexOfChild(view) ?: -1
            val preceding=if(parent!=null && index>0) (index-1 downTo 0).map(parent::getChildAt).firstOrNull { it.visibility!=View.GONE } else null
            val separated=parent?.orientation==LinearLayout.VERTICAL && preceding?.tag=="bicycle_section_divider"
            val choice=view is android.widget.CheckBox || view is android.widget.RadioButton
            val choiceList=choice && (preceding is android.widget.CheckBox || preceding is android.widget.RadioButton)
            val gap=if(separated || choiceList) 0 else dp(view.context,BicycleDesign.controlGap)
            var changed=false
            if(((separated || choiceList) && margins.topMargin!=0) || margins.topMargin<gap) { margins.topMargin=gap;changed=true }
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
    internal fun textColor(view:TextView,manager:ThemeManager,tokens:ThemeTokens):Int {
        val pageHeader=when(view.tag) { "bicycle_page_heading","bicycle_dialog_title","analysis_title"->true;else->false }
        return if(pageHeader && manager.isSettingsPalette && manager.settingsLight) android.graphics.Color.WHITE else tokens.foreground
    }
    internal fun heading(context: Context, manager: ThemeManager, value: String) = text(context, manager, value).apply {
        typeface = context.resources.getFont(R.font.bebas_neue); textSize = BicycleDesign.headingSp
        gravity = Gravity.CENTER; isAccessibilityHeading = true; maxLines = 1;tag="bicycle_page_heading"
        bindTheme(this,manager) { setTextColor(textColor(this,manager,it)) }
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
        bindTheme(this, manager) { setTextColor(textColor(this,manager,it)) }
    }
    /** Content hierarchy below page/section headers; does not resize sticky headers or menu titles. */
    fun contentHeading(context:Context,manager:ThemeManager,value:String)=text(context,manager,value,true).apply {
        tag="bicycle_content_heading"
        textSize=BicycleDesign.contentHeadingSp
        gravity=Gravity.START
    }
    /** Explanatory form copy keeps the shared gap; labels and metric text keep their own geometry. */
    fun description(context: Context, manager: ThemeManager, value: String) = text(context,manager,value).apply {
        layoutParams=LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(context,BicycleDesign.controlGap) }
        controlSpacing(this)
    }
    fun input(context: Context, manager: ThemeManager) = input(EditText(context),manager)
    internal fun <T:EditText> input(view:T,manager:ThemeManager):T = view.apply {
        typeface=resources.getFont(R.font.bebas_neue)
        textSize = BicycleDesign.settingsMetricSp; minimumHeight = dp(context, BicycleDesign.touchSize)
        setPadding(dp(context, 12), dp(context, BicycleDesign.verticalPadding), dp(context, 12), dp(context, BicycleDesign.verticalPadding))
        bindTheme(this, manager) {
            background=controlBackground(context,it,android.graphics.Color.TRANSPARENT)
            setTextColor(it.foreground); setHintTextColor(it.foreground); backgroundTintList = null
            textCursorDrawable?.mutate()?.setTint(it.foreground)
            textSelectHandle?.mutate()?.setTint(it.foreground); textSelectHandleLeft?.mutate()?.setTint(it.foreground); textSelectHandleRight?.mutate()?.setTint(it.foreground)
        }
        controlSpacing(this)
    }
    internal fun controlBackground(context:Context,tokens:ThemeTokens,fill:Int=tokens.secondary)=GradientDrawable().apply {
        setColor(fill);cornerRadius=dp(context,BicycleDesign.inputRadius).toFloat()
        setStroke(dp(context,BicycleDesign.controlBorder),android.graphics.Color.argb(
            (android.graphics.Color.alpha(tokens.foreground)*BicycleDesign.controlBorderAlpha).toInt(),
            android.graphics.Color.red(tokens.foreground),android.graphics.Color.green(tokens.foreground),android.graphics.Color.blue(tokens.foreground)))
    }
    /** Shared popup shell: reserve its outline so list contents cannot paint over the border. */
    internal fun dropdownBackground(context:Context,tokens:ThemeTokens)=controlBackground(context,tokens,tokens.tertiary).apply {
        val inset=BicycleDesign.dp(context,BicycleDesign.controlBorder)
        setPadding(inset,inset,inset,inset)
    }
    /** Shared frame for adjacent selection segments, whether single or multiple selection. */
    private val segmentedRows = java.util.WeakHashMap<LinearLayout, Boolean>()
    internal fun segmentedControlRow(row:LinearLayout,manager:ThemeManager) {
        row.orientation=LinearLayout.HORIZONTAL
        row.setTag(R.id.inline_control_row,true)
        controlSpacing(row)
        if (segmentedRows.put(row,true)!=null) return
        outlinedControl(row,manager)
        val border=dp(row.context,BicycleDesign.controlBorder).coerceAtLeast(1)
        row.setPadding(border,border,border,border)
    }
    internal fun outlinedControl(view:View,manager:ThemeManager) {
        bindTheme(view,manager) { view.background=controlBackground(view.context,it);view.backgroundTintList=null }
    }
    fun action(context: Context, manager: ThemeManager, label: String, click: () -> Unit) =
        action(context, manager, label, true, click)
    internal fun textAction(context:Context,manager:ThemeManager,label:String,click:()->Unit)=BicycleTextAction(context).apply {
        text=label;textSize=BicycleDesign.bodySp;isAllCaps=false;typeface=android.graphics.Typeface.DEFAULT
        minimumWidth=dp(context,BicycleDesign.touchSize);minimumHeight=dp(context,BicycleDesign.touchSize)
        bindTheme(this,manager) { tokens ->
            background=null;backgroundTintList=null
            setTextColor(android.content.res.ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled),intArrayOf()),
                intArrayOf(tokens.accent,android.graphics.Color.argb(128,android.graphics.Color.red(tokens.accent),android.graphics.Color.green(tokens.accent),android.graphics.Color.blue(tokens.accent)))))
        }
        setOnClickListener { click() }
    }
    internal fun action(context: Context, manager: ThemeManager, label: String, formSpacing: Boolean, click: () -> Unit) = Button(context).apply {
        text = label; textSize = BicycleDesign.bodySp; isAllCaps = false
        minimumWidth = dp(context, BicycleDesign.touchSize); minimumHeight = dp(context, BicycleDesign.touchSize)
        outlinedControl(this,manager)
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
        bindTheme(this, manager) {
            val header=(parent as? View)?.tag=="bicycle_navigation_header"
            setColorFilter(if(header && manager.isSettingsPalette && manager.settingsLight) android.graphics.Color.WHITE else it.iconColor(colorRole))
        }
        setOnClickListener { click() }
    }
    internal fun secondaryText(view:TextView,manager:ThemeManager) {
        bindTheme(view,manager) { view.setTextColor(it.secondaryTextColor) }
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
