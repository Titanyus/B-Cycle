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
internal class DataSettingsView(context: Context, private val themes: ThemeManager,private val importSelected:(RouteImportAction)->Unit={},private val canImport:(RouteImportAction)->Boolean={true},private val trainingSelected:(TrainingImportAction)->Unit={},selected: (DataAction)->Unit) : LinearLayout(context) {
    val backupStatus=BicycleFeedback.status(context,themes).apply { tag="backup_status" }
    private val importRows=linkedMapOf<RouteImportAction,View>()
    private val frames=mutableMapOf<DataAction,BicycleFrameDropdown>()
    private val bodies=mutableMapOf<DataAction,LinearLayout>()
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    init {
        orientation=VERTICAL;tag="data_settings";setPadding(dp(BicycleDesign.horizontalPadding),0,dp(BicycleDesign.horizontalPadding),0);BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        fun section(title:Int, actions:List<DataAction>) {
            BicycleForm.section(this,themes,title).apply { if(title==R.string.data_routes) tag="data_routes_heading" }
            if(title==R.string.data_global) addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.data_group_save)).apply { tag="data_group_save" })
            addView(BicycleComponents.description(context,themes,context.getString(
                if(title==R.string.data_global) R.string.backup_section_hint else R.string.cloud_section_hint)),LayoutParams(-1,-2))
            actions.forEach { action ->
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
            }
        }
        section(R.string.data_global,listOf(DataAction.BACKUP,DataAction.RIDE_BACKUP,DataAction.HRV_EXPORT))
        addView(backupStatus,LayoutParams(-1,-2))
        addView(BicycleComponents.sectionDivider(context,themes))
        addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.data_group_restore)).apply { tag="data_group_restore" })
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
        addView(BicycleComponents.sectionDivider(context,themes).apply { tag="data_import_divider" })
        addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.data_group_import)).apply { tag="data_group_import" })
        val routesImport=BicycleFrameDropdown(context,themes,context.getString(R.string.data_route_import),false,leadingIcon=BicycleIcon.Import).apply { tag="data_frame_IMPORT";header.tag="data_IMPORT" }
        addView(routesImport,LayoutParams(-1,-2))
        RouteImportAction.entries.forEach { action ->
            val row=LinearLayout(context).apply {
                orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;minimumHeight=dp(BicycleDesign.touchSize)
                tag=action.viewTag;setTag(R.id.inline_control_row,true)
                contentDescription=context.getString(action.title);isClickable=true;isFocusable=true
                setOnClickListener { if(canImport(action)) importSelected(action) else refreshImportAvailability() }
                addView(BicycleComponents.text(context,themes,context.getString(action.title)).apply {
                    gravity=Gravity.CENTER_VERTICAL;BicycleDialogStyle.actionIcon(this,themes,action.icon)
                },LayoutParams(0,-2,1f))
                addView(ImageView(context).apply {
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                    BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,BicycleIcon.Forward,it.foreground)) }
                },LayoutParams(dp(BicycleDesign.iconSize),dp(BicycleDesign.iconSize)))
            }
            importRows[action]=row;routesImport.content.addView(row,LayoutParams(-1,-2))
        }
        val trainingImport=BicycleFrameDropdown(context,themes,context.getString(R.string.training_import_title),false,leadingIcon=BicycleIcon.Training).apply { tag="data_training_import" }
        for(action in TrainingImportAction.entries) trainingImport.content.addView(BicycleComponents.action(context,themes,context.getString(action.title)) { trainingSelected(action) }.apply {
            tag=action.viewTag;BicycleDialogStyle.actionIcon(this,themes,action.icon)
        },LayoutParams(-1,-2))
        addView(trainingImport,LayoutParams(-1,-2))
        refreshImportAvailability()
        section(R.string.data_routes,listOf(DataAction.LOCAL,DataAction.CLOUD,DataAction.DREEVE,DataAction.INTERVALS,DataAction.STRAVA))
    }
    fun refreshImportAvailability() {
        importRows.forEach { (action,row) -> row.isEnabled=canImport(action);row.alpha=if(row.isEnabled) 1f else .4f }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();refreshImportAvailability() }
    override fun onWindowFocusChanged(hasWindowFocus:Boolean) { super.onWindowFocusChanged(hasWindowFocus);if(hasWindowFocus) refreshImportAvailability() }
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
