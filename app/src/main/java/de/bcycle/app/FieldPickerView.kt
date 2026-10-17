package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

/** Source stays above the bounded field list; selection updates in place without closing. */
internal class FieldPickerView(context: Context, data: LiveData, gps: GpsValues,
    bikeDistanceM: Long?, pulse: PulseValues, selected: DashboardField?, allowEmpty: Boolean,
    private val themes: ThemeManager = ThemeManager(context.getSharedPreferences("bike", 0)),
    graphPicker: GraphOverlayPicker? = null,
    visualization: ZoneGlowPicker? = null,
    training: TrainingSnapshot = TrainingSnapshot(),
    dataVisible: Boolean = true,
    batteryChoose: ((DashboardField?)->Unit)? = null,
    trendEnabled:Boolean = true,
    trendChanged:((Boolean)->Unit)? = null,
    choose: (DashboardField?) -> Unit) : LinearLayout(context) {
    private fun dp(n: Int) = BicycleDesign.dp(context, n)
    private lateinit var fieldList: ScrollView
    override fun onMeasure(widthMeasureSpec: Int,heightMeasureSpec: Int) {
        val available=MeasureSpec.getSize(heightMeasureSpec)
        val limit=BicycleDesign.fieldListHeight(context,available)
        if(fieldList.layoutParams.height!=limit) fieldList.layoutParams.height=limit
        super.onMeasure(widthMeasureSpec,heightMeasureSpec)
    }
    init {
        orientation = VERTICAL; setPadding(dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.horizontalPadding), 0)
        BicycleComponents.background(this, themes, ThemeRole.SECONDARY)
        val panels=LinearLayout(context).apply { orientation=VERTICAL }
        val scroll=ScrollView(context).apply { tag="field_picker_scroll";BicycleComponents.scrollStyle(this);addView(panels) }
        addView(scroll,LayoutParams(-1,-1))
        val dataContent=LinearLayout(context).apply { orientation=VERTICAL;visibility=GONE;tag="field_data_content" }
        lateinit var dataToggle: android.widget.Button
        dataToggle=BicycleComponents.action(context,themes,context.getString(R.string.field_data_title)) {
            dataContent.visibility=if(dataContent.visibility==VISIBLE) GONE else VISIBLE
            dataToggle.stateDescription=context.getString(if(dataContent.visibility==VISIBLE) R.string.zone_expanded else R.string.zone_collapsed)
        }.apply { tag="field_data_toggle";isAccessibilityHeading=true;stateDescription=context.getString(R.string.zone_collapsed) }
        BicycleDialogStyle.accordion(dataToggle,themes)
        BicycleDialogStyle.actionIcon(dataToggle,themes,BicycleIcon.Expand)
        panels.addView(dataToggle);panels.addView(dataContent)
        var currentSelection=selected
        var pickerDataVisible=dataVisible
        val emptyChoice=RadioButton(context).apply {
            setText(R.string.field_no_data);tag=if(allowEmpty) "map_field_empty" else "dashboard_field_empty"
            isChecked=!dataVisible || selected==null
            BicycleDialogStyle.control(this,themes)
        }
        dataContent.addView(emptyChoice,LayoutParams(-1,dp(BicycleDesign.touchSize)))
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
        val trendToggle=android.widget.Switch(context).apply {
            tag="field_trend_indicator";setText(R.string.field_trend_indicator)
            isChecked=trendEnabled
            visibility=if(!allowEmpty && dataVisible && AverageTrends.supports(selected) && trendChanged!=null) VISIBLE else GONE
            BicycleDialogStyle.control(this,themes)
            setOnCheckedChangeListener { _,checked -> trendChanged?.invoke(checked) }
        }
        dataContent.addView(trendToggle,LayoutParams(-1,-2))
        val rows = LinearLayout(context).apply { orientation = VERTICAL }
        val list = ScrollView(context).apply { tag = "field_list"; addView(rows) }
        BicycleDialogStyle.content(list, themes)
        fieldList=list
        dataContent.addView(list, LayoutParams(-1, dp(BicycleDesign.fieldListInitialHeight)))
        if(graphPicker!=null) panels.addView(graphPicker)
        if(visualization!=null) panels.addView(visualization.apply { visibility=GONE })
        var shown = -1
        val availableBatteries=BatteryFields.choices(data)
        var batterySelection=if(dataVisible) (selected?.batterySources
            ?: BatteryFields.single(selected?.id.orEmpty())?.let(::listOf).orEmpty()).filter { it in availableBatteries } else emptyList()
        var combined=dataVisible && selected?.batterySources!=null
        var refreshBatteryChecks:()->Unit={}
        fun reflectSelection(field:DashboardField?) {
            currentSelection=field;pickerDataVisible=field!=null
            combined=field?.batterySources!=null
            batterySelection=field?.batterySources.orEmpty().filter { it in availableBatteries }
            emptyChoice.isChecked=field==null
            trendToggle.visibility=if(!allowEmpty && field!=null && AverageTrends.supports(field) && trendChanged!=null) VISIBLE else GONE
            for(i in 0 until rows.childCount) (rows.getChildAt(i) as? RadioButton)?.let { it.isChecked=field!=null && !combined && it.tag==field.id }
            refreshBatteryChecks()
        }
        emptyChoice.setOnClickListener { reflectSelection(null);choose(null) }
        fun populate(position: Int) {
            if (shown == position) return
            shown = position;refreshBatteryChecks={}; rows.removeAllViews(); list.scrollTo(0, 0)
            for (section in groups.getValue(sources[position])) {
                rows.addView(TextView(context).apply {
                    text = if(section.source=="Leistung") context.getString(R.string.training_category) else DashboardTextResources.section(context,section.title); isAccessibilityHeading=true; textSize = BicycleDesign.bodySp; BicycleDialogStyle.control(this, themes)
                    setPadding(0, dp(BicycleDesign.rowGap), 0, dp(BicycleDesign.smallGap)); isAccessibilityHeading = true
                })
                for (field in section.fields.filter { BatteryFields.single(it.id)==null }) {
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
                        rows.addView(BicycleComponents.text(context,themes,context.getString(R.string.battery_selection_help)))
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
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = populate(position)
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        val initial = sources.indexOf(if(BatteryFields.single(selected?.id.orEmpty())!=null) "Akku" else selected?.source(data)).coerceAtLeast(0)
        selector.setSelection(initial); populate(initial)
    }
}
