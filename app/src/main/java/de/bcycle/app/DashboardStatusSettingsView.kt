package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.*

internal class DashboardStatusSettingsView(context:Context,private val themes:ThemeManager,private val prefs:android.content.SharedPreferences,
    private val choose:(Int,DashboardStatusConfig,(DashboardStatusItem)->Unit)->Unit,private val changed:()->Unit):LinearLayout(context) {
    private var config=DashboardStatusConfig.read(prefs)
    private val preview=DashboardStatusStrip(context,themes,preview=true)
    private val body=LinearLayout(context).apply { orientation=VERTICAL;visibility=GONE;tag="dashboard_status_details";BicycleContentSpacing.accordionContent(this) }
    private val rows=mutableListOf<LinearLayout>()
    private val orders=listOf((0..2).toMutableList(),(3..5).toMutableList())
    private fun dp(n:Int)=BicycleDesign.dp(context,n)
    init {
        orientation=VERTICAL;tag="dashboard_fullscreen_settings"
        val enabled=Switch(context).apply {
            tag="dashboard_fullscreen";setText(R.string.dashboard_fullscreen);isChecked=DashboardFullscreen.enabled(prefs)
            BicycleDialogStyle.control(this,themes)
            setOnCheckedChangeListener { _,value -> prefs.edit().putBoolean(DashboardFullscreen.key,value).apply();changed() }
        }
        addView(LinearLayout(context).apply {
            tag=BicycleDesign.inlineControlRowTag;gravity=Gravity.CENTER_VERTICAL
            val expand=BicycleComponents.icon(context,themes,BicycleIcon.Expand,context.getString(R.string.dashboard_fullscreen)) {}.apply {
                tag="dashboard_status_expand";stateDescription=context.getString(R.string.zone_collapsed)
                setOnClickListener {
                    val open=body.visibility==GONE;body.visibility=if(open) VISIBLE else GONE;rotation=if(open) 180f else 0f
                    stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
                }
            }
            BicycleComponents.switchAccordion(this,enabled,expand,themes)
        },LayoutParams(-1,dp(BicycleDesign.sectionHeaderMinHeight)))
        body.addView(preview,LayoutParams(-1,dp(BicycleDesign.dashboardStatusPreviewHeight)))
        body.addView(BicycleComponents.description(context,themes,context.getString(R.string.dashboard_status_preview_note)))
        for(side in 0..1) {
            body.addView(BicycleComponents.contentHeading(context,themes,context.getString(if(side==0) R.string.dashboard_status_left else R.string.dashboard_status_right)))
            val row=LinearLayout(context).apply { orientation=HORIZONTAL;isBaselineAligned=false;tag="dashboard_status_side_$side";setTag(R.id.inline_control_row,true) }
            val scroll=HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled=false;addView(row,FrameLayout.LayoutParams(-2,-2)) }
            val reorder=TileReorder(scroll,row,themes,{ orders[side].toList() }) { id,to ->
                val from=orders[side].indexOf(id)
                if(from<0 || from==to || to !in 0..2) false else {
                    config=config.move(side*3+from,side*3+to)
                    orders[side].add(to,orders[side].removeAt(from))
                    val moved=row.getChildAt(from);row.removeViewAt(from);row.addView(moved,to)
                    save();true
                }
            }
            rows+=row
            repeat(3) { offset ->
                val slot=side*3+offset
                val card=BicycleSegmentButton(context,themes,multiple=true,standalone=true).apply {
                    tag="dashboard_status_slot_$slot"
                    textSize=BicycleDesign.analysisLabelSp
                    setOnClickListener {
                        val index=side*3+orders[side].indexOf(slot)
                        isChecked=config.slots[index]!=DashboardStatusItem.EMPTY
                        choose(index,config) { item -> config=config.assign(index,item);save() }
                    }
                }
                row.addView(card,LayoutParams(dp(BicycleDesign.rideNamePartWidth),dp(BicycleDesign.rideNamePartHeight)).apply { marginStart=if(offset==0) 0 else dp(BicycleDesign.controlGap) })
                reorder.bind(card,slot)
            }
            body.addView(scroll,LayoutParams(-1,-2))
        }
        body.addView(BicycleComponents.description(context,themes,context.getString(R.string.dashboard_status_hint)))
        addView(body,LayoutParams(-1,-2));render()
    }
    private fun save() { DashboardStatusConfig.write(prefs,config);render();changed() }
    private fun render() {
        preview.config=config;preview.reloadMessenger();preview.show(DashboardStatusSnapshot("10:28",true,72,2,3))
        rows.forEachIndexed { side,row -> for(offset in 0..2) {
            val item=config.slots[side*3+offset];val card=row.getChildAt(offset) as BicycleSegmentButton
            card.tag="dashboard_status_slot_${side*3+offset}"
            (card.layoutParams as LayoutParams).marginStart=if(offset==0) 0 else dp(BicycleDesign.controlGap)
            card.isChecked=item!=DashboardStatusItem.EMPTY
            statusSlotAppearance(card,item,themes)
        } }
    }
}

internal fun statusSlotAppearance(card:BicycleSegmentButton,item:DashboardStatusItem,themes:ThemeManager) {
    val context=card.context;val size=BicycleDesign.dp(context,BicycleDesign.iconSize)
    card.contentDescription=context.getString(item.label)
    card.text=when(item) { DashboardStatusItem.EMPTY->context.getString(R.string.hrvt_unavailable);DashboardStatusItem.TIME->"10:28";DashboardStatusItem.VPN->"VPN";else->"" }
    card.setCompoundDrawables(null,null,null,null)
    val icon=when(item) {
        DashboardStatusItem.EMPTY,DashboardStatusItem.TIME,DashboardStatusItem.VPN->null
        DashboardStatusItem.NOTIFICATIONS->MessageNotificationSettings(context).orderedPackages.firstOrNull()?.let { runCatching { context.packageManager.getApplicationIcon(it).mutate() }.getOrNull() }
            ?: BicycleIcons.drawable(context,item.icon,card.currentTextColor)
        else->BicycleIcons.drawable(context,item.icon,card.currentTextColor)
    }
    icon?.let {
        val centered=StatusSlotIcon(card,it,item==DashboardStatusItem.NOTIFICATIONS).apply { setBounds(0,0,size,size) }
        card.setCompoundDrawables(null,centered,null,null)
    }
}

/** A symbol-only card has no label block: keep the compound icon at the card's vertical centre. */
private class StatusSlotIcon(private val view:android.widget.TextView,private val icon:android.graphics.drawable.Drawable,
    private val originalColors:Boolean):android.graphics.drawable.Drawable() {
    override fun draw(canvas:android.graphics.Canvas) {
        val save=canvas.save();canvas.translate(0f,(view.height-bounds.height())/2f-view.paddingTop)
        icon.draw(canvas);canvas.restoreToCount(save)
    }
    override fun onBoundsChange(bounds:android.graphics.Rect) { icon.setBounds(0,0,bounds.width(),bounds.height()) }
    override fun setAlpha(alpha:Int) { icon.alpha=alpha }
    override fun setColorFilter(filter:android.graphics.ColorFilter?) { if(!originalColors) icon.colorFilter=filter }
    override fun setTint(color:Int) { if(!originalColors) icon.setTint(color) }
    @Deprecated("Deprecated in Android") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}
