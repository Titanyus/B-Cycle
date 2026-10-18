package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.*

internal enum class DataAction(val title: Int, val icon: BicycleIcon) {
    BACKUP(R.string.data_backup, BicycleIcon.ExportTrack), RESTORE(R.string.data_restore, BicycleIcon.RestoreSettings),
    LOCAL(R.string.data_local, BicycleIcon.Save), CLOUD(R.string.data_cloud, BicycleIcon.WebDav),
    INTERVALS(R.string.intervals_title,BicycleIcon.Intervals),
    DREEVE(R.string.dreeve_title,BicycleIcon.Dreeve), STRAVA(R.string.strava_title,BicycleIcon.Strava)
}
/** Inline service forms reuse their existing controls; expanding never enables an upload. */
internal class DataSettingsView(context: Context, private val themes: ThemeManager, selected: (DataAction)->Unit) : LinearLayout(context) {
    private val headers=mutableMapOf<DataAction,LinearLayout>()
    private val bodies=mutableMapOf<DataAction,LinearLayout>()
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    init {
        orientation=VERTICAL;tag="data_settings";setPadding(dp(BicycleDesign.horizontalPadding),0,dp(BicycleDesign.horizontalPadding),0);BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        fun section(title:Int, actions:List<DataAction>) {
            BicycleForm.section(this,themes,title).apply { if(title==R.string.data_routes) tag="data_routes_heading" }
            actions.forEach { action ->
                val inline=action!=DataAction.RESTORE
                val body=LinearLayout(context).apply { orientation=VERTICAL;tag="data_details_${action.name}";visibility=GONE }
                val label=BicycleComponents.text(context,themes,context.getString(action.title)).apply {
                    gravity=Gravity.CENTER_VERTICAL
                    BicycleDialogStyle.actionIcon(this,themes,action.icon)
                }
                val header=LinearLayout(context).apply {
                    tag="data_${action.name}";setTag(R.id.inline_control_row,true);gravity=Gravity.CENTER_VERTICAL
                    contentDescription=context.getString(action.title);isClickable=true;isFocusable=true
                }
                val expand=BicycleComponents.icon(context,themes,if(inline) BicycleIcon.Expand else BicycleIcon.Forward,context.getString(action.title)) { header.performClick() }.apply {
                    tag="data_expand_${action.name}"
                }
                fun showState() {
                    val open=body.visibility==VISIBLE
                    expand.rotation=if(open && inline) 180f else 0f
                    expand.stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
                    header.stateDescription=expand.stateDescription
                }
                header.setOnClickListener {
                    if(inline) { body.visibility=if(body.visibility==VISIBLE) GONE else VISIBLE;showState() }
                    else selected(action)
                }
                header.addView(label,LayoutParams(0,-1,1f))
                header.addView(expand,LayoutParams(dp(BicycleDesign.touchSize),dp(BicycleDesign.touchSize)).apply { marginStart=dp(BicycleDesign.controlGap) })
                // Same geometry as the sensor switch/chevron row.
                addView(header,LayoutParams(-1,dp(BicycleDesign.sectionHeaderMinHeight)+2*BicycleDesign.linePixels(context,BicycleDesign.sectionRuleDp)))
                if(inline) { headers[action]=header;bodies[action]=body;addView(body,LayoutParams(-1,-2));showState() }
            }
        }
        section(R.string.data_global,listOf(DataAction.BACKUP,DataAction.RESTORE))
        section(R.string.data_routes,listOf(DataAction.LOCAL,DataAction.CLOUD,DataAction.INTERVALS,DataAction.DREEVE,DataAction.STRAVA))
    }
    fun install(action:DataAction,content:View,enabled:Switch?=null) {
        val header=headers.getValue(action)
        if(enabled!=null) {
            (enabled.parent as? android.view.ViewGroup)?.removeView(enabled)
            header.removeViewAt(0)
            enabled.text=context.getString(action.title);enabled.gravity=Gravity.START or Gravity.CENTER_VERTICAL
            BicycleDialogStyle.actionIcon(enabled,themes,action.icon)
            header.addView(enabled,0,LayoutParams(0,-1,1f))
        }
        bodies.getValue(action).addView(content,LayoutParams(-1,-2))
    }
}
internal class BackupSelectionView(context:Context, themes:ThemeManager, available:Set<BackupSection>) : LinearLayout(context) {
    private val checks=linkedMapOf<BackupSection,CheckBox>()
    var changed:()->Unit={}
    fun selected()=checks.filterValues { it.isChecked }.keys.toSet()
    init {
        orientation=VERTICAL;tag="backup_selection"
        setPadding(BicycleDesign.dp(context,BicycleDesign.horizontalPadding),BicycleDesign.dp(context,BicycleDesign.verticalPadding),BicycleDesign.dp(context,BicycleDesign.horizontalPadding),0)
        val all=Button(context).apply { text=context.getString(R.string.backup_all);tag="backup_all";setOnClickListener { checks.values.forEach { it.isChecked=true } } }
        val none=Button(context).apply { text=context.getString(R.string.backup_none);tag="backup_none";setOnClickListener { checks.values.forEach { it.isChecked=false } } }
        addView(LinearLayout(context).apply {
            addView(all,LayoutParams(0,-2,1f).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.verticalPadding) })
            addView(none,LayoutParams(0,-2,1f))
        })
        BackupSection.entries.filter { it in available }.forEach { section ->
            val check=CheckBox(context).apply {
                text=context.getString(section.title);tag="backup_${section.id}";isChecked=true
                setOnCheckedChangeListener { _,_->changed() }
            };checks[section]=check;addView(check,LayoutParams(-1,-2))
        }
        BicycleDialogStyle.content(this,themes)
    }
}
