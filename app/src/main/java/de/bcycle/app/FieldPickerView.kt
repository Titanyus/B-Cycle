package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Spinner

/** One scroll host keeps every data choice and the following training help reachable. */
internal class FieldPickerView(context: Context, data: LiveData, gps: GpsValues,
    bikeDistanceM: Long?, pulse: PulseValues, selected: DashboardField?, allowEmpty: Boolean,
    private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)),
    graphPicker: GraphOverlayPicker? = null,
    visualization: ZoneGlowPicker? = null,
    training: TrainingSnapshot = TrainingSnapshot(),
    dataVisible: Boolean = true,
    batteryChoose: ((DashboardField?)->Unit)? = null,
    trendConfig:TrendIndicatorConfig = TrendIndicatorConfig(),
    trendChanged:((TrendIndicatorConfig)->Unit)? = null,
    trainingHelp:TrainingHelpView? = null,
    drinkReminder:DrinkReminderSettingsView? = null,
    choose: (DashboardField?) -> Unit) : LinearLayout(context) {
    var interaction:()->Unit={}
    private fun dp(n: Int) = BicycleDesign.dp(context, n)
    init {
        orientation = VERTICAL; setPadding(0, dp(BicycleDesign.verticalPadding), 0, 0)
        BicycleComponents.background(this, themes, ThemeRole.SECONDARY)
        val panels=LinearLayout(context).apply {
            orientation=VERTICAL;clipToPadding=false
            setPadding(dp(BicycleDesign.horizontalPadding),0,dp(BicycleDesign.horizontalPadding),0)
        }
        val scroll=BicycleStickyScrollView(context).apply { tag="field_picker_scroll";addView(panels) }
        addView(scroll,LayoutParams(-1,-1))
        val dataContent=LinearLayout(context).apply { orientation=VERTICAL;visibility=GONE;tag="field_data_content" }
        var currentSelection=selected
        var pickerDataVisible=dataVisible && selected!=null
        var reflecting=false
        val dataEnabled=android.widget.Switch(context).apply {
            tag="field_data_enabled";setText(R.string.field_data_title);isChecked=pickerDataVisible
            BicycleDialogStyle.control(this,themes)
        }
        val dataToggle=BicycleComponents.icon(context,themes,BicycleIcon.Expand,context.getString(R.string.field_data_title)) {
            val open=dataContent.visibility!=VISIBLE
            dataContent.visibility=if(open) VISIBLE else GONE
            findViewWithTag<View>("field_data_toggle").apply {
                rotation=if(open) 180f else 0f
                stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
            }
        }.apply { tag="field_data_toggle";stateDescription=context.getString(R.string.zone_collapsed) }
        panels.addView(LinearLayout(context).apply { BicycleComponents.switchAccordion(this,dataEnabled,dataToggle,themes) },LayoutParams(-1,dp(BicycleDesign.sectionHeaderMinHeight)))
        panels.addView(dataContent)
        val groups = DashboardField.groups(data, selected, includeTraining = !allowEmpty).groupBy { it.source }
        val sources = groups.keys.toList()
        val selector = Spinner(context).apply {
            tag = "field_source"; contentDescription = context.getString(R.string.ui_source_choose)
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, sources)
        }
        BicycleDialogStyle.spinner(selector, themes) { source, foreground ->
            val title=DashboardTextResources.source(context,source)
            android.text.SpannableString("\uFFFC $title").apply {
                setSpan(SourceIconSpan(source,context,foreground),0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        dataContent.addView(selector, LayoutParams(-1, dp(BicycleDesign.touchSize)))
        val trend=trendChanged?.let { TrendIndicatorSettingsView(context,themes,trendConfig,it).apply { setDataAvailable(pickerDataVisible) } }
        if(trend!=null) panels.addView(trend,LayoutParams(-1,-2))
        val rows = LinearLayout(context).apply { orientation = VERTICAL;tag="field_list" }
        dataContent.addView(rows, LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.sectionContentGap) })
        if(graphPicker!=null) panels.addView(graphPicker)
        if(visualization!=null) panels.addView(visualization.apply { visibility=GONE })
        if(trainingHelp!=null) {
            if(drinkReminder!=null) trainingHelp.addDrinkReminder(drinkReminder)
            BicycleForm.section(panels,themes,R.string.training_help_title)
            panels.addView(trainingHelp,LayoutParams(-1,-2))
        }
        if(drinkReminder!=null && trainingHelp==null) {
            panels.addView(BicycleComponents.sectionDivider(context,themes))
            panels.addView(drinkReminder,LayoutParams(-1,-2))
        }
        var shown = -1
        val availableBatteries=BatteryFields.choices(data)
        var batterySelection=if(dataVisible) (selected?.batterySources
            ?: BatteryFields.single(selected?.id.orEmpty())?.let(::listOf).orEmpty()).filter { it in availableBatteries } else emptyList()
        var combined=dataVisible && selected?.batterySources!=null
        var refreshBatteryChecks:()->Unit={}
        fun reflectSelection(field:DashboardField?) {
            if(field!=null) currentSelection=field
            pickerDataVisible=field!=null
            trend?.setDataAvailable(pickerDataVisible)
            reflecting=true;dataEnabled.isChecked=field!=null;reflecting=false
            combined=field?.batterySources!=null
            batterySelection=field?.batterySources.orEmpty().filter { it in availableBatteries }
            for(i in 0 until rows.childCount) (rows.getChildAt(i) as? RadioButton)?.let { it.isChecked=field!=null && !combined && it.tag==field.id }
            refreshBatteryChecks()
        }
        dataEnabled.setOnCheckedChangeListener { _,enabled ->
            if(!reflecting) {
                val field=if(enabled) currentSelection ?: groups.values.flatten().flatMap { it.fields }.firstOrNull { it.id!=BatteryFields.COMBINED } else null
                reflectSelection(field);choose(field)
            }
        }
        fun populate(position: Int) {
            if (shown == position) return
            shown = position;refreshBatteryChecks={}; rows.removeAllViews()
            for (section in groups.getValue(sources[position])) {
                BicycleComponents.listCategory(rows,themes,
                    if(section.source=="Leistung") context.getString(R.string.training_category) else DashboardTextResources.section(context,section.title))
                for (field in section.fields.filter { BatteryFields.single(it.id)==null || section.source=="Schaltgruppe" }) {
                    val value = field.value(data, gps, bikeDistanceM, pulse, training)
                    val title = if (field.id == "RIDER_POWER") context.getString(R.string.ui_rider_power_own) else field.title(context,data, gps)
                    if(field.id==BatteryFields.COMBINED) {
                        var updating=false
                        val checks=mutableMapOf<BatterySource,android.widget.CheckBox>()
                        fun updateChecks() {
                            updating=true
                            checks.forEach { (source,check) ->
                                check.isChecked=source in batterySelection
                                check.isEnabled=source in batterySelection || batterySelection.size<4
                            }
                            updating=false
                        }
                        fun saveCombination() {
                            val field=if(combined) BatteryFields.field(batterySelection) else null
                            reflectSelection(field)
                            (batteryChoose ?: choose).invoke(field)
                        }
                        refreshBatteryChecks=::updateChecks
                        rows.addView(BicycleComponents.text(context,themes,context.getString(R.string.battery_selection)))
                        rows.addView(BicycleComponents.description(context,themes,context.getString(R.string.battery_selection_help)))
                        availableBatteries.forEach { source ->
                            val check=android.widget.CheckBox(context).apply {
                                tag="battery_select_${source.name}";text=context.getString(source.label);isChecked=source in batterySelection
                                BicycleDialogStyle.control(this,themes);BicycleDialogStyle.actionIcon(this,themes,BatteryFields.icon(source))
                                setOnCheckedChangeListener { _,checked ->
                                    if(!updating) {
                                        batterySelection=if(checked) (batterySelection+source).distinct() else batterySelection-source
                                        combined=batterySelection.isNotEmpty()
                                        saveCombination()
                                    }
                                }
                            }
                            checks[source]=check;rows.addView(check)
                        }
                        updateChecks()
                    } else {
                        val radio=RadioButton(context).apply {
                            tag=field.id
                            text=if(value.unit.isNotEmpty()) context.getString(R.string.ui_field_with_unit,title,value.unit) else title
                            BicycleDialogStyle.control(this,themes);minHeight=dp(BicycleDesign.touchSize)
                            isChecked=pickerDataVisible && !combined && currentSelection==field
                            contentDescription=if(field.showSource) context.getString(R.string.ui_source_field_description,DashboardTextResources.source(context,field.source(data)),text) else text
                            setOnClickListener { reflectSelection(field);choose(field) }
                        }
                        rows.addView(radio,LayoutParams(-1,-2))
                    }
                }
            }
        }
        selector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { if(shown!=position) interaction();populate(position) }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        val initial = sources.indexOf(if(BatteryFields.single(selected?.id.orEmpty())!=null && selected?.id!="SHIFT_BATTERY") "Akku" else selected?.source(data)).coerceAtLeast(0)
        selector.setSelection(initial); populate(initial)
    }
}
