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
internal class DataSettingsView(context: Context, themes: ThemeManager, selected: (DataAction)->Unit) : LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="data_settings";BicycleComponents.background(this,themes,ThemeRole.SECONDARY)
        fun section(title:Int, actions:List<DataAction>) {
            BicycleForm.section(this,themes,title)
            actions.forEach { action -> addView(BicycleComponents.listRow(context,themes).apply {
                tag="data_${action.name}";contentDescription=context.getString(action.title);isClickable=true;isFocusable=true
                setOnClickListener { selected(action) }
                addView(ImageView(context).apply {
                    BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,action.icon,it.foreground)) }
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                },LayoutParams(BicycleDesign.dp(context,BicycleDesign.iconSize),BicycleDesign.dp(context,BicycleDesign.iconSize)))
                addView(BicycleComponents.text(context,themes,context.getString(action.title)).apply {
                    textSize=BicycleDesign.inputSp;gravity=Gravity.CENTER_VERTICAL
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                },LayoutParams(0,-2,1f).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.verticalPadding) })
                addView(ImageView(context).apply {
                    BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,BicycleIcon.Forward,it.foreground)) }
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                },LayoutParams(BicycleDesign.dp(context,BicycleDesign.iconSize),BicycleDesign.dp(context,BicycleDesign.iconSize)))
            },LayoutParams(-1,-2)) }
        }
        section(R.string.data_global,listOf(DataAction.BACKUP,DataAction.RESTORE))
        section(R.string.data_routes,listOf(DataAction.LOCAL,DataAction.CLOUD,DataAction.INTERVALS,DataAction.DREEVE,DataAction.STRAVA))
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
