package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.*

internal enum class DataAction(val title: Int, val icon: BicycleIcon) {
    HRV_EXPORT(R.string.hrv_export, BicycleIcon.ExportTrack),
    BACKUP(R.string.data_backup, BicycleIcon.ExportTrack), RESTORE(R.string.data_restore, BicycleIcon.RestoreSettings),
    IMPORT(R.string.data_route_import, BicycleIcon.Import),
    RIDE_BACKUP(R.string.ride_backup_title,BicycleIcon.ExportTrack),
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
                val transfer=when(action) {
                    DataAction.IMPORT -> R.string.transfer_import_in to R.string.transfer_import_out
                    DataAction.LOCAL -> R.string.transfer_local_in to R.string.transfer_local_out
                    DataAction.CLOUD -> R.string.transfer_webdav_in to R.string.transfer_webdav_out
                    DataAction.INTERVALS -> R.string.transfer_intervals_in to R.string.transfer_service_out
                    DataAction.DREEVE -> R.string.transfer_dreeve_in to R.string.transfer_service_out
                    DataAction.STRAVA -> R.string.transfer_strava_in to R.string.transfer_service_out
                    DataAction.RIDE_BACKUP -> R.string.transfer_backup_in to R.string.transfer_backup_out
                    else -> null
                }
                transfer?.let { (incoming,outgoing) ->
                    body.addView(LinearLayout(context).apply {
                        orientation=VERTICAL;tag="data_transfer_${action.name}"
                        setPadding(dp(BicycleDesign.horizontalPadding),0,dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.controlGap))
                        addView(BicycleComponents.description(context,themes,
                            context.getString(R.string.transfer_in,context.getString(incoming))))
                        addView(BicycleComponents.description(context,themes,
                            context.getString(R.string.transfer_out,context.getString(outgoing))))
                    },LayoutParams(-1,-2))
                }
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
        section(R.string.data_routes,listOf(DataAction.RIDE_BACKUP,DataAction.IMPORT,DataAction.LOCAL,DataAction.CLOUD,DataAction.INTERVALS,DataAction.DREEVE,DataAction.STRAVA))
        BicycleForm.section(this,themes,R.string.hrv_data_title)
        addView(BicycleComponents.action(context,themes,context.getString(R.string.hrv_export)) { selected(DataAction.HRV_EXPORT) }.apply {
            tag="data_HRV_EXPORT";contentDescription=context.getString(R.string.hrv_export)
        },LayoutParams(-1,-2))
    }
    fun install(action:DataAction,content:View,enabled:Switch?=null) {
        val header=headers.getValue(action)
        if(enabled!=null) {
            (enabled.parent as? android.view.ViewGroup)?.removeView(enabled)
            val label=header.getChildAt(0) as TextView
            val expand=header.getChildAt(1)
            BicycleComponents.switchAccordion(header,enabled,expand,themes,label)
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
