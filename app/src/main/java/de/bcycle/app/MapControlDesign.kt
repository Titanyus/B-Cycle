package de.bcycle.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button

/** Map-specific geometry. Preserve compact dashboard and full-size detail-map roles. DP unless noted. */
internal object MapControlDesign {
    const val dashboardWidth = 44
    const val buttonHeight = 48
    const val detailWidth = 48
    const val compactMinHeight = 24
    const val compactMinWidth = 24
    const val iconSize = 24
    const val trackIconSize = 28
    const val iconHorizontalPadding = 10
    const val trackHorizontalPadding = 8
    const val trackTopPadding = 10
    const val buttonGap = 2
    const val edgeGap = 6
    const val radius = 5
    const val strokeWidth = 1
    const val symbolSp = 20f
    const val topRowReserve = 58
    const val zoomClearance = 10
    const val collisionGap = 4
    const val nightOffset = edgeGap + dashboardWidth + buttonGap
    const val compactRowOffset = dashboardWidth + edgeGap * 2
    val foreground = Color.WHITE
    val dashboardBackground = Color.argb(225, 25, 25, 25)
    val dashboardBorder = Color.DKGRAY

    fun background(context: android.content.Context) = GradientDrawable().apply {
        setColor(dashboardBackground)
        cornerRadius = BicycleDesign.dp(context, radius).toFloat()
        setStroke(BicycleDesign.dp(context, strokeWidth), dashboardBorder)
    }
    fun icon(button: Button, kind: BicycleIcon, negative:Boolean=false) {
        val size = BicycleDesign.dp(button.context, iconSize)
        val padding = BicycleDesign.dp(button.context, iconHorizontalPadding)
        button.gravity = Gravity.CENTER
        button.setPadding(padding, 0, padding, 0)
        button.setCompoundDrawables(BicycleIcons.drawable(button.context, kind, foreground,negative).apply {
            setBounds(0, 0, size, size)
        }, null, null, null)
    }
}
