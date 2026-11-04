package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.*

internal enum class DataAction(val title: Int, val icon: BicycleIcon) {
    HRV_IMPORT(R.string.hrv4_import, BicycleIcon.Import),
    HRV_EXPORT(R.string.hrv_backup_title, BicycleIcon.ExportTrack),
    HRV_RESTORE(R.string.hrv_restore_title,BicycleIcon.RestoreSettings),
    HRV_RESTORE_GROUP(R.string.hrv_restore_title,BicycleIcon.Heart),
    RIDE_RESTORE(R.string.ride_restore_title,BicycleIcon.RestoreSettings),
    BACKUP(R.string.data_backup, BicycleIcon.ExportTrack), RESTORE(R.string.data_restore, BicycleIcon.RestoreSettings),
    IMPORT(R.string.data_route_import, BicycleIcon.Import),
    RIDE_BACKUP(R.string.ride_backup_title,BicycleIcon.ExportTrack),
    LOCAL(R.string.data_local, TransferSymbols.local), CLOUD(R.string.data_cloud, TransferSymbols.webdav),
    INTERVALS(R.string.intervals_title,TransferSymbols.intervals),
    DREEVE(R.string.dreeve_title,TransferSymbols.dreeve), STRAVA(R.string.strava_title,TransferSymbols.strava)
}
/** Inline service forms reuse their existing controls; expanding never enables an upload. */
internal class DataSettingsView(context: Context, private val themes: ThemeManager, selected: (DataAction)->Unit) : LinearLayout(context) {
    val backupStatus=BicycleFeedback.status(context,themes).apply { tag="backup_status" }
    private val frames=mutableMapOf<DataAction,BicycleFrameDropdown>()
    private val bodies=mutableMapOf<DataAction,LinearLayout>()
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    init {
        orientation=VERTICAL;tag="data_settings";setPadding(dp(BicycleDesign.horizontalPadding),0,dp(BicycleDesign.horizontalPadding),0);BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        fun section(title:Int, actions:List<DataAction>) {
            BicycleForm.section(this,themes,title).apply { if(title==R.string.data_routes) tag="data_routes_heading" }
            addView(BicycleComponents.description(context,themes,context.getString(
                if(title==R.string.data_global) R.string.backup_section_hint else R.string.cloud_section_hint)),LayoutParams(-1,-2))
            actions.forEach { action ->
                if(action!=DataAction.IMPORT) {
                    val dropdown=BicycleFrameDropdown(context,themes,context.getString(action.title),false,leadingIcon=action.icon).apply {
                        tag="data_frame_${action.name}"
                    }
                    dropdown.header.apply { tag="data_${action.name}";contentDescription=context.getString(action.title) }
                    dropdown.content.tag="data_details_${action.name}"
                    dropdown.expandControl.tag="data_expand_${action.name}"
                    frames[action]=dropdown
                    bodies[action]=dropdown.content
                    if(action in setOf(DataAction.LOCAL,DataAction.CLOUD,DataAction.DREEVE,DataAction.INTERVALS,DataAction.STRAVA)) {
                        dropdown.content.addView(LinearLayout(context).apply {
                            orientation=VERTICAL;tag="data_transfer_${action.name}"
                            fun transfer(icon:BicycleIcon,title:Int) {
                                addView(BicycleComponents.description(context,themes,context.getString(title)).apply {
                                    BicycleDialogStyle.actionIcon(this,themes,icon)
                                },LayoutParams(-1,-2))
                            }
                            transfer(BicycleIcon.TransferOut,if(action==DataAction.INTERVALS) R.string.intervals_out_data else R.string.backup_fit_data)
                            if(action==DataAction.DREEVE) transfer(BicycleIcon.TransferIn,R.string.dreeve_in_data)
                            if(action==DataAction.INTERVALS) transfer(BicycleIcon.TransferIn,R.string.intervals_in_fitness)
                        },LayoutParams(-1,-2))
                    }
                    addView(dropdown,LayoutParams(-1,-2))
                    return@forEach
                }
                val inline=action !in setOf(DataAction.RESTORE,DataAction.RIDE_RESTORE)
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
                if(action==DataAction.IMPORT) { header.isEnabled=false;label.isEnabled=false;expand.isEnabled=false;header.alpha=.5f }
                header.setOnClickListener {
                    if(action==DataAction.IMPORT) return@setOnClickListener
                    if(inline) { body.visibility=if(body.visibility==VISIBLE) GONE else VISIBLE;showState() }
                    else selected(action)
                }
                header.addView(label,LayoutParams(0,-1,1f))
                header.addView(expand,LayoutParams(dp(BicycleDesign.touchSize),dp(BicycleDesign.touchSize)).apply { marginStart=dp(BicycleDesign.controlGap) })
                // Same geometry as the sensor switch/chevron row.
                addView(header,LayoutParams(-1,dp(BicycleDesign.sectionHeaderMinHeight)+2*BicycleDesign.linePixels(context,BicycleDesign.sectionRuleDp)))
                if(inline) { bodies[action]=body;addView(body,LayoutParams(-1,-2));showState() }
            }
        }
        section(R.string.data_global,listOf(DataAction.BACKUP,DataAction.RIDE_BACKUP,DataAction.HRV_EXPORT))
        addView(backupStatus,LayoutParams(-1,-2))
        addView(BicycleComponents.sectionDivider(context,themes))
        listOf(DataAction.RESTORE,DataAction.RIDE_RESTORE,DataAction.HRV_RESTORE).forEach { action ->
            val frame=BicycleFrameDropdown(context,themes,context.getString(action.title),false,leadingIcon=action.icon).apply {
                tag=if(action==DataAction.HRV_RESTORE) "data_HRV_RESTORE_GROUP" else "data_frame_${action.name}"
                header.tag="data_${action.name}";content.tag="data_details_${action.name}"
            }
            frames[action]=frame;bodies[action]=frame.content
            if(action==DataAction.HRV_RESTORE) {
                frame.content.addView(BicycleComponents.sectionDivider(context,themes).apply { tag="hrv_restore_import_divider" })
                frame.content.addView(BicycleComponents.action(context,themes,context.getString(DataAction.HRV_IMPORT.title)) { selected(DataAction.HRV_IMPORT) }.apply {
                    tag="data_HRV_IMPORT";BicycleDialogStyle.actionIcon(this,themes,DataAction.HRV_IMPORT.icon)
                },LayoutParams(-1,-2))
            }
            addView(frame,LayoutParams(-1,-2))
        }
        section(R.string.data_routes,listOf(DataAction.IMPORT,DataAction.LOCAL,DataAction.CLOUD,DataAction.DREEVE,DataAction.INTERVALS,DataAction.STRAVA))
    }
    fun install(action:DataAction,content:View,enabled:Switch?=null) {
        if(enabled!=null) frames.getValue(action).setTrailing(enabled)
        // The frame owns the horizontal inset; service forms must not add it a second time.
        content.setPadding(0,0,0,0)
        fun compactStatuses(view:View) {
            if(view is TextView && view.accessibilityLiveRegion==View.ACCESSIBILITY_LIVE_REGION_POLITE) {
                fun refresh() { view.visibility=if(view.text.isNullOrEmpty()) View.GONE else View.VISIBLE }
                refresh()
                view.addTextChangedListener(object:android.text.TextWatcher {
                    override fun beforeTextChanged(text:CharSequence?,start:Int,count:Int,after:Int)=Unit
                    override fun onTextChanged(text:CharSequence?,start:Int,before:Int,count:Int)=Unit
                    override fun afterTextChanged(text:android.text.Editable?) { refresh() }
                })
            }
            if(view is android.view.ViewGroup) for(i in 0 until view.childCount) compactStatuses(view.getChildAt(i))
        }
        compactStatuses(content)
        val body=bodies.getValue(action)
        body.addView(content,if(action in setOf(DataAction.RESTORE,DataAction.RIDE_RESTORE,DataAction.HRV_RESTORE)) 0 else body.childCount,LayoutParams(-1,-2))
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
