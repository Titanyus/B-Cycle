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
    val dashboardUnitColor = android.graphics.Color.rgb(163, 163, 163)
    const val titleSp = 23f
    const val bodySp = 14f
    const val inputSp = 16f
    const val headerHeight = 64
    const val headingSp = 32f
    const val rowTitleSp = 16f
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
    const val analysisTileMinHeight = 140
    const val analysisMetricIconSize = 32
    const val analysisMetricSp = 28f
    const val analysisChartHeight = 120
    const val analysisMapHeight = 180
    const val touchSize = 48
    const val compactActionWidth = 36
    const val iconSize = 24
    const val gridColumns = 3
    const val gridRows = 2
    const val scrollFillViewport = false
    const val scrollClipToPadding = false
    const val horizontalPadding = 16
    const val verticalPadding = 8
    const val headerHorizontalPadding = 24
    const val headerVerticalPadding = 12
    const val dialogRadius = 18
    const val inputRadius = 6
    const val scrollbarSize = 2
    const val dividerPixels = 1
    const val dividerAlpha = .55f
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
    fun header(context: Context, manager: ThemeManager) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        layoutParams = ViewGroup.LayoutParams(-1, dp(context, BicycleDesign.headerHeight))
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
        isFillViewport = BicycleDesign.scrollFillViewport; clipToPadding = BicycleDesign.scrollClipToPadding
        addView(dialogBody(context, manager))
    }
    fun text(context: Context, manager: ThemeManager, value: String, title: Boolean = false) = TextView(context).apply {
        text = value; textSize = if (title) BicycleDesign.titleSp else BicycleDesign.bodySp
        if (title) setTypeface(typeface, android.graphics.Typeface.BOLD)
        bindTheme(this, manager) { setTextColor(it.foreground) }
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
    fun action(context: Context, manager: ThemeManager, label: String, click: () -> Unit) = Button(context).apply {
        text = label; textSize = BicycleDesign.bodySp; isAllCaps = false
        minimumWidth = dp(context, BicycleDesign.touchSize); minimumHeight = dp(context, BicycleDesign.touchSize)
        surface(this, manager, ThemeRole.TERTIARY, BicycleDesign.inputRadius)
        bindTheme(this, manager) { setTextColor(it.foreground); backgroundTintList = null }
        setOnClickListener { click() }
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
        layoutParams = ViewGroup.LayoutParams(-1, BicycleDesign.dividerPixels)
        bindTheme(this, manager) { setBackgroundColor(it.foreground) }
    }
    fun sectionDivider(context: Context, manager: ThemeManager) = divider(context, manager).apply {
        alpha = BicycleDesign.dividerAlpha
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(-1, BicycleDesign.dividerPixels).apply {
            topMargin = dp(context, BicycleDesign.rowGap); bottomMargin = dp(context, BicycleDesign.verticalPadding)
        }
    }
}
