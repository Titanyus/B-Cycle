package de.bcycle.app

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup

/** Shared HRV-card chrome. Shadows deliberately remain visible in both design modes. */
internal object BicycleDialogSurface {
    val normal = ThemeRole.TERTIARY
    val alert = ThemeRole.PRIMARY

    fun background(view: View, tokens: ThemeTokens, role: ThemeRole = normal) = GradientDrawable().apply {
        cornerRadius = BicycleDesign.dp(view.context, BicycleDesign.dialogRadius).toFloat()
        setColor(tokens.color(role))
        setStroke(BicycleDesign.dp(view.context, BicycleDesign.hrvPromptBorder).coerceAtLeast(1), tokens.color(role))
    }
    fun bind(view: View, themes: ThemeManager, role: ThemeRole = normal) {
        view.clipToOutline = true
        view.outlineAmbientShadowColor = Color.BLACK
        view.outlineSpotShadowColor = Color.BLACK
        BicycleComponents.bindTheme(view, themes) {
            view.background = background(view, it, role)
            view.elevation = BicycleDesign.dp(view.context, BicycleDesign.dialogShadow).toFloat()
        }
    }
    fun window(screen: AlertDialog, themes: ThemeManager, role: ThemeRole) {
        val window = screen.window ?: return
        val decor = window.decorView
        bind(decor, themes, role)
        BicycleComponents.bindTheme(decor, themes) {
            window.setBackgroundDrawable(background(decor, it, role))
            window.setElevation(BicycleDesign.dp(decor.context, BicycleDesign.dialogShadow).toFloat())
        }
    }
    /** Recolour page surfaces only: controls, maps, artwork and semantic fills retain their style.
     * Bind after existing page styling so theme changes cannot restore the old SECONDARY surface. */
    fun content(view: View, themes: ThemeManager, role: ThemeRole = normal) {
        if (view !is ViewGroup) return
        fun surfaceColor(): Int? = when (val background = view.background) {
            is ColorDrawable -> background.color
            is GradientDrawable -> background.color?.defaultColor
            else -> null
        }
        if (surfaceColor() == themes.active.tokens.secondary) {
            BicycleComponents.bindTheme(view, themes) { tokens ->
                when (val background = view.background) {
                    is ColorDrawable -> background.mutate().also { (it as ColorDrawable).color = tokens.color(role) }
                    is GradientDrawable -> background.mutate().also { (it as GradientDrawable).setColor(tokens.color(role)) }
                }
                view.invalidate()
            }
        }
        for (i in 0 until view.childCount) content(view.getChildAt(i), themes, role)
    }
}
