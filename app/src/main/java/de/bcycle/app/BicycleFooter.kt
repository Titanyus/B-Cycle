package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** One accessible action: icon above its short label. Shared by detail and import footers. */
internal class BicycleFooterAction(context: Context, themes: ThemeManager, icon: BicycleIcon,
    label: String, description: String = label, click: () -> Unit) : LinearLayout(context) {
    internal val image = ImageView(context)
    private var currentIcon = icon
    init {
        orientation = VERTICAL; gravity = Gravity.CENTER
        minimumHeight = BicycleDesign.dp(context, BicycleDesign.footerHeight)
        minimumWidth = BicycleDesign.dp(context, BicycleDesign.touchSize)
        contentDescription = description; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
        image.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        addView(image, LayoutParams(BicycleDesign.dp(context, BicycleDesign.iconSize), BicycleDesign.dp(context, BicycleDesign.iconSize)))
        val caption = TextView(context).apply {
            text = label; gravity = Gravity.CENTER; maxLines = 1
            textSize = BicycleDesign.footerLabelSp
            setAutoSizeTextTypeUniformWithConfiguration(8, BicycleDesign.footerLabelSp.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(caption, LayoutParams(-1, -2).apply { topMargin = BicycleDesign.dp(context, BicycleDesign.smallGap) })
        BicycleComponents.bindTheme(this, themes) { tokens ->
            image.setImageDrawable(BicycleIcons.drawable(context, currentIcon, tokens.foreground))
            caption.setTextColor(tokens.foreground)
        }
        setOnClickListener { if (isEnabled) click() }
    }
    fun setIcon(icon: BicycleIcon, foreground: Int) {
        currentIcon = icon
        image.setImageDrawable(BicycleIcons.drawable(context, icon, foreground))
    }
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else .5f
    }
}

internal fun bicycleFooter(context: Context, themes: ThemeManager) = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL; tag = "bicycle_action_footer"
    minimumHeight = BicycleDesign.dp(context, BicycleDesign.footerHeight)
    BicycleComponents.background(this, themes, ThemeRole.SECONDARY)
}
