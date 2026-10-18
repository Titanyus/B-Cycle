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
    private lateinit var labelView:TextView
    fun setLabel(text:String) { labelView.text=text;contentDescription=text }
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
        labelView=caption
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
    styleBicycleFooter(this, themes)
}

/** Draw above action children; nested action rows share their host separator. */
internal fun styleBicycleFooter(view:android.view.View,themes:ThemeManager) {
    BicycleComponents.background(view,themes,ThemeRole.SECONDARY)
    BicycleComponents.bindTheme(view,themes) { tokens ->
        view.foreground=object:android.graphics.drawable.Drawable() {
            private val paint=android.graphics.Paint().apply { color=tokens.foreground }
            override fun draw(canvas:android.graphics.Canvas) {
                if((view.parent as? android.view.View)?.tag=="bicycle_action_footer") return
                canvas.drawRect(bounds.left.toFloat(),bounds.top.toFloat(),bounds.right.toFloat(),
                    bounds.top+BicycleDesign.linePixels(view.context,BicycleDesign.footerRuleDp).toFloat(),paint)
            }
            override fun setAlpha(alpha:Int) { paint.alpha=alpha;invalidateSelf() }
            override fun setColorFilter(filter:android.graphics.ColorFilter?) { paint.colorFilter=filter;invalidateSelf() }
            @Deprecated("Deprecated in Java") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
        }
    }
}
