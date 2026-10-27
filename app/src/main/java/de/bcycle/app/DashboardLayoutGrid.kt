package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout

/** Three-column layout previews using the shared dashboard preview geometry. */
internal class DashboardLayoutGrid(context:Context,themes:ThemeManager,selected:DashboardTemplate,
    templates:List<DashboardTemplate> = DashboardTemplate.entries,
    choose:(DashboardTemplate)->Unit):LinearLayout(context) {
    private fun dp(n:Int)=BicycleDesign.dp(context,n)
    init {
        orientation=VERTICAL;tag="dashboard_layout_grid"
        listOf(true,false).forEach { hasMap ->
        val matching=templates.filter { it.hasMap==hasMap }
        if(matching.isEmpty()) return@forEach
        addView(BicycleComponents.contentHeading(context,themes,context.getString(if(hasMap) R.string.dashboard_with_map else R.string.dashboard_without_map)),LayoutParams(-1,-2).apply {
            topMargin=if(childCount==0) 0 else dp(BicycleDesign.sectionContentGap);bottomMargin=dp(BicycleDesign.controlGap)
        })
        matching.chunked(3).forEachIndexed { index,templates ->
            addView(LinearLayout(context).apply {
                orientation=HORIZONTAL;tag="preset_row_${if(hasMap) "map" else "data"}_$index"
                templates.forEach { template ->
                    val tile=LinearLayout(context).apply {
                        orientation=VERTICAL;gravity=Gravity.CENTER;isClickable=true;isFocusable=true
                        tag="preset_${template.name}";contentDescription=template.displayTitle(context);isSelected=template==selected
                        stateDescription=context.getString(if(isSelected) R.string.ui_selected else R.string.ui_not_selected)
                        setPadding(dp(BicycleDesign.inputRadius),dp(BicycleDesign.verticalPadding),dp(BicycleDesign.inputRadius),dp(BicycleDesign.verticalPadding))
                        addView(ImageView(context).apply {
                            setImageDrawable(DashboardPreview(template));importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                            BicycleComponents.bindTheme(this,themes) { setColorFilter(it.foreground) }
                        },LayoutParams(dp(BicycleDesign.presetPreviewWidth),dp(BicycleDesign.presetPreviewHeight)))
                        addView(BicycleComponents.text(context,themes,context.getString(R.string.dashboard_preset_values,template.slots.size)).apply {
                            gravity=Gravity.CENTER;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                        },LayoutParams(-1,dp(BicycleDesign.iconSize)))
                        BicycleComponents.bindTheme(this,themes) { tokens ->
                            BicycleSelectionCard.apply(this,tokens,isSelected)
                        }
                        setOnClickListener { choose(template) }
                    }
                    addView(tile,LayoutParams(0,dp(BicycleDesign.presetTileHeight),1f).apply { setMargins(dp(BicycleDesign.presetTileGap),dp(BicycleDesign.presetTileGap),dp(BicycleDesign.presetTileGap),dp(BicycleDesign.presetTileGap)) })
                }
                repeat(3-templates.size) { addView(View(context),LayoutParams(0,dp(BicycleDesign.presetTileHeight),1f).apply { setMargins(dp(BicycleDesign.presetTileGap),dp(BicycleDesign.presetTileGap),dp(BicycleDesign.presetTileGap),dp(BicycleDesign.presetTileGap)) }) }
            },LayoutParams(-1,-2))
        }
        }
    }
}
