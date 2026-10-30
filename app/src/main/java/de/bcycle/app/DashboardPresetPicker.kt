package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.*

/** Named pages use stable slots; the ten geometry presets remain independent choices. */
internal class DashboardPresetPicker(context:Context, initialPage:Int,
    private val themes:ThemeManager=ThemeManager(context.getSharedPreferences("bike",0))) : LinearLayout(context) {
    private val prefs=context.getSharedPreferences("bike",0)
    private val bikeType=BikeProfileStore(context,prefs).current().type
    private val availableTemplates=DashboardTemplate.entries.filter { bikeType!=BikeType.INDOOR || !it.hasMap }
    val pages=DashboardPages(context,prefs)
    var changed:()->Unit={}
    val selection get()=pages.all().map { template(it.id) }
    private val row=LinearLayout(context).apply { orientation=HORIZONTAL;isBaselineAligned=false }
    private val scroller=object:HorizontalScrollView(context) {
        override fun onMeasure(w:Int,h:Int) {
            val gap=dp(BicycleDesign.controlGap)
            val width=((MeasureSpec.getSize(w)-gap*2)/3).coerceAtLeast(1)
            for(i in 0 until row.childCount) {
                val child=row.getChildAt(i);val lp=child.layoutParams as LinearLayout.LayoutParams
                // Assign parameters again so resolved native margins follow rebuilt/selected cards.
                val margin=if(i==0) 0 else gap
                if(lp.width!=width || lp.marginStart!=margin || lp.leftMargin!=margin) {
                    lp.width=width;lp.marginStart=margin;lp.marginEnd=0
                    lp.leftMargin=margin;lp.rightMargin=0;child.layoutParams=lp
                }
            }
            super.onMeasure(w,h)
        }
    }.apply { tag="dashboard_page_strip";isHorizontalScrollBarEnabled=false;addView(row,FrameLayout.LayoutParams(-2,-2)) }
    private val reorder=TileReorder(scroller,row,themes,{ pages.all().map { it.id } }) { id,index ->
        pages.move(id,index).also { if(it) { refresh();changed() } }
    }
    private val layoutBody=LinearLayout(context).apply { orientation=VERTICAL;tag="dashboard_layout_details";visibility=GONE;BicycleContentSpacing.accordionContent(this) }
    private val layoutChoice=BicycleComponents.action(context,themes,"") {
        layoutBody.visibility=if(layoutBody.visibility==GONE) VISIBLE else GONE
        refreshLayoutChoices()
    }.apply { tag="dashboard_layout_choice" }
    private fun dp(n:Int)=BicycleDesign.dp(context,n)
    private fun template(id:Int)=DashboardTemplate.decode(prefs.getString("dashboardTemplate_$id",DashboardTemplate.entries[id].name)).forBike(bikeType)
    init {
        orientation=VERTICAL
        BicycleComponents.controlSpacing(this)
        pages.select(initialPage)
        addView(scroller,LayoutParams(-1,-2))
        layoutChoice.gravity=Gravity.START or Gravity.CENTER_VERTICAL
        layoutChoice.includeFontPadding=false
        layoutChoice.setSingleLine();layoutChoice.ellipsize=android.text.TextUtils.TruncateAt.END
        layoutChoice.compoundDrawablePadding=dp(BicycleDesign.controlGap)
        BicycleDialogStyle.accordion(layoutChoice,themes)
        BicycleComponents.bindTheme(layoutChoice,themes) { tokens ->
            updateLayoutLabel(tokens.foreground)
        }
        addView(layoutChoice,LayoutParams(-1,dp(56)))
        addView(layoutBody,LayoutParams(-1,-2))
        refresh()
    }
    fun refresh() {
        val x=scroller.scrollX
        row.removeAllViews()
        fun card(page:DashboardPage?) {
            val active=page?.id==pages.selected()
            val tile=LinearLayout(context).apply {
                orientation=VERTICAL;gravity=Gravity.CENTER;isClickable=true;isFocusable=true
                tag=page?.let { "preset_page_${it.id}" } ?: "dashboard_page_add"
                setPadding(dp(BicycleDesign.smallGap),dp(BicycleDesign.verticalPadding),dp(BicycleDesign.smallGap),dp(BicycleDesign.verticalPadding))
                contentDescription=page?.name ?: context.getString(R.string.dashboard_new)
                isSelected=active;stateDescription=context.getString(if(active) R.string.ui_selected else R.string.ui_not_selected)
                addView(ImageView(context).apply {
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                    BicycleComponents.bindTheme(this,themes) { tokens ->
                        setImageDrawable(page?.let { DashboardPreview(template(it.id)) } ?: BicycleIcons.drawable(context,BicycleIcon.AddSensor,tokens.foreground));setColorFilter(tokens.foreground)
                    }
                },LayoutParams(dp(BicycleDesign.presetPreviewWidth),dp(BicycleDesign.presetPreviewHeight)))
                addView(BicycleComponents.text(context,themes,page?.name ?: context.getString(R.string.dashboard_new)).apply {
                    gravity=Gravity.CENTER;setLines(2);ellipsize=android.text.TextUtils.TruncateAt.END;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                },LayoutParams(-1,-2))
                BicycleComponents.bindTheme(this,themes) { tokens ->
                    BicycleSelectionCard.apply(this,tokens,active,add=page==null)
                }
                if(page!=null) reorder.bind(this,page.id)
                setOnClickListener {
                    if(page==null) pages.add() else pages.select(page.id)
                    refresh();changed()
                    val index=pages.all().indexOfFirst { it.id==pages.selected() }
                    scroller.post { row.getChildAt(index)?.let { scroller.smoothScrollTo(it.left,0) } }
                }
            }
            row.addView(tile,LayoutParams(0,dp(BicycleDesign.bikeTileHeight)))
        }
        pages.all().forEach(::card);if(pages.all().size<DashboardPages.MAX) card(null)
        refreshLayoutChoices()
        scroller.post { scroller.scrollTo(x,0) }
    }
    private fun refreshLayoutChoices() {
        layoutBody.removeAllViews()
        if(layoutBody.visibility==VISIBLE) layoutBody.addView(DashboardLayoutGrid(context,themes,template(pages.selected()),availableTemplates) { chosen ->
            prefs.edit().putString("dashboardTemplate_${pages.selected()}",chosen.name).apply()
            refresh();changed()
        },LayoutParams(-1,-2))
        updateLayoutLabel(themes.active.tokens.foreground)
    }
    private fun updateLayoutLabel(color:Int) {
        val selected=template(pages.selected())
        val preview=DashboardPreview(selected).apply { setBounds(0,0,dp(28),dp(40));setTint(color) }
        layoutChoice.contentDescription=selected.displayTitle(context)
        layoutChoice.text=selected.displayTitle(context)
        val icon=BicycleIcons.drawable(context,BicycleIcon.Expand,color)
        val arrow=android.graphics.drawable.RotateDrawable().apply {
            drawable=icon;fromDegrees=if(layoutBody.visibility==VISIBLE) 180f else 0f;toDegrees=fromDegrees;level=10000
            setBounds(0,0,dp(BicycleDesign.iconSize),dp(BicycleDesign.iconSize))
        }
        layoutChoice.stateDescription=context.getString(if(layoutBody.visibility==VISIBLE) R.string.zone_expanded else R.string.zone_collapsed)
        layoutChoice.setCompoundDrawablesRelative(preview,null,arrow,null)
    }
}
