package de.bcycle.app

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*

/** One profile page: tiles, editable heading, configuration, sensors, then FIT. No nested scrolling. */
internal class BikesView(context: Context, private val runtime: RideRuntime,
    private val search: () -> Unit, private val stopSearch: () -> Unit, private val enableGps: () -> Unit, private val recordingControls: () -> View, private val choosePhoto:(Int)->Unit = {},
    private val confirmDelete:(Int,String,()->Unit)->Unit = { _,_,_ -> },
    private val additionalSections:(()->List<Pair<Int,View>>)?=null, private val editDashboard:()->Unit={},private val assignProfiles:()->Unit={}) : LinearLayout(context) {
    private val themes = runtime.themes.settings
    private val collapsedSections=mutableSetOf<String>()
    private val expandedSensors=mutableSetOf<BikeSensor>()
    var dashboardActions:LinearLayout?=null;private set
    var designHeader:BicycleSectionHeader?=null;private set
    var footerChanged:()->Unit={}
    private var copyPopup:ListPopupWindow?=null
    private val copyAction:BicycleFooterAction=BicycleFooterAction(context,themes,BicycleIcon.Import,context.getString(R.string.profiles_copy)) {
        if(!runtime.recording && !runtime.saving) {
            val slot=runtime.bikes.activeSlot
            val sources=runtime.bikes.all().filter { it.slot!=slot }
            copyPopup?.dismiss()
            copyPopup=ListPopupWindow(context).apply {
                anchorView=footer;isModal=true
                setAdapter(ArrayAdapter(context,android.R.layout.simple_list_item_1,sources.map { it.name }))
                width=footer.width.coerceAtLeast(dp(200))
                setOnItemClickListener { _,_,position,_ ->
                    dismiss()
                    if(runtime.bikes.activeSlot==slot && runtime.copyProfile(sources[position].slot)) { stopSearch();rebuild() }
                }
                show()
                listView?.let { BicycleDialogStyle.content(it,themes) }
            }
        }
    }.apply { tag="profile_copy" }
    private var shownSources:Map<RecordingQuantity,List<RecordingSource>> = emptyMap()
    private var shownSlot = -1
    private var shownType:BikeType?=null
    private lateinit var typeChoice:Spinner
    private lateinit var indoorTypeChoice:Spinner
    private val correctionControls=mutableListOf<Pair<SeekBar,TextView>>()
    private var binding = false
    private var photo:BikePhotoView?=null
    private lateinit var strip:BikeProfileStrip
    private val assignmentButton=BicycleFooterAction(context,themes,BicycleIcon.Bikes,context.getString(R.string.profile_assignment_title),click=assignProfiles).apply { tag="profile_assignment_open" }
    private val deleteAction=BicycleFooterAction(context,themes,BicycleIcon.DeleteTrack,context.getString(if(additionalSections==null) R.string.bikes_delete else R.string.profiles_delete)) {
        val p=runtime.bikes.current()
        if(!runtime.recording && !runtime.saving && runtime.bikes.all().size>1) confirmDelete(p.slot,p.name) {
            stopSearch();runtime.deleteCurrentBike(p.slot);refresh()
        }
    }.apply { tag="bike_delete" }
    val footer:LinearLayout=bicycleFooter(context,themes).apply {
        addView(BicycleFooterAction(context,themes,BicycleIcon.RenameTrack,context.getString(R.string.profiles_rename)) {
            nameHeading.performClick()
        }.apply { tag="bike_rename" },LayoutParams(0,-2,1f))
        if(additionalSections!=null) {
            addView(copyAction,LayoutParams(0,-2,1f))
        }
        addView(assignmentButton,LayoutParams(0,-2,1f))
        addView(deleteAction,LayoutParams(0,-2,1f))
    }
    private val statuses = mutableMapOf<BikeSensor, TextView>()
    private val measurementDetails=mutableMapOf<BikeSensor,SensorMeasurementDetails>()
    private val batteries = mutableMapOf<BikeSensor, TextView>()
    private val switches = mutableMapOf<BikeSensor, Switch>()
    private val disconnects = mutableMapOf<BikeSensor, Button>()
    private val deviceNames = mutableMapOf<BikeSensor, TextView>()
    private val removes = mutableMapOf<BikeSensor, ImageButton>()
    private lateinit var nameHeading: TextView
    private lateinit var autoConnect: Switch
    private lateinit var autoStartRecording:Switch
    private lateinit var searchButton: Button
    private lateinit var searchResults: LinearLayout
    private lateinit var searchStatus: TextView
    private fun dp(n: Int) = BicycleDesign.dp(context,n)
    init {
        orientation = VERTICAL; clipToPadding = false; tag = "bikes_manager"
        setPadding(dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.verticalPadding),dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.sectionContentGap))
        rebuild()
    }
    private fun text(id: Int, heading: Boolean = false) = BicycleComponents.text(context,themes,context.getString(id),heading)
    private fun section(id:Int)=BicycleForm.section(this,themes,id)
    override fun onDetachedFromWindow() { copyPopup?.dismiss();super.onDetachedFromWindow() }
    private fun watcher(change: (String)->Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?,start: Int,count: Int,after: Int) = Unit
        override fun onTextChanged(s: CharSequence?,start: Int,before: Int,count: Int) { change(s.toString()) }
        override fun afterTextChanged(s: Editable?) = Unit
    }
    private fun rebuild() {
        shownSources=RecordingSources.available(runtime.bikes.current(),runtime.barometer.available)
        shownSlot = runtime.bikes.activeSlot;shownType=runtime.bikes.current().type;correctionControls.clear()
        dashboardActions=null;designHeader=null
        removeAllViews();statuses.clear();measurementDetails.clear();batteries.clear();switches.clear();disconnects.clear();deviceNames.clear();removes.clear()
        val profile = runtime.bikes.current(); val slot = profile.slot
        strip=BikeProfileStrip(context,themes,{ id ->
            stopSearch()
            if(!runtime.switchBike(id)) BicycleFeedback.show(context,R.string.bikes_profile_locked,false)
            refresh()
        }, {
            stopSearch()
            if(runtime.addBike()) { refresh();strip.revealActive() }
        }, { id, position -> runtime.moveBike(id,position).also { refresh() } })
        addView(strip,LayoutParams(-1,-2))
        strip.show(runtime.bikes.all(),slot,runtime.recording || runtime.saving)
        strip.revealActive()
        val nameHeader = BicycleSectionHeader(context,themes,profile.name).apply { tag="bike_name_header" }
        nameHeading = (nameHeader.titleView).apply {
            tag="bike_profile_name";gravity=Gravity.CENTER;minimumHeight=dp(BicycleDesign.sectionHeaderMinHeight);isClickable=true;isFocusable=true
            contentDescription=context.getString(R.string.bikes_name_edit)
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        val nameEdit = BicycleComponents.input(context,themes).apply {
            tag="bike_profile_name_edit";visibility=GONE;setSingleLine();setText(profile.name)
            hint=context.getString(R.string.bikes_name_hint);contentDescription=context.getString(R.string.bikes_name_edit)
            filters=arrayOf(android.text.InputFilter.LengthFilter(60));imeOptions=EditorInfo.IME_ACTION_DONE
        }
        var naming=false
        val titleLayout=nameHeading.layoutParams
        fun finishNaming() {
            if(!naming || nameEdit.text.toString().trim().isEmpty()) return
            naming=false;nameEdit.clearFocus()
            nameHeader.removeView(nameEdit);nameEdit.visibility=GONE
            nameHeader.addView(nameHeading,1,titleLayout)
            context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(nameHeader.windowToken,0)
        }
        nameHeading.setOnClickListener {
            if(!naming) {
                naming=true
                val height=nameHeading.height.coerceAtLeast(nameHeading.minimumHeight)
                nameHeader.removeView(nameHeading)
                nameEdit.visibility=VISIBLE
                nameHeader.addView(nameEdit,1,LayoutParams(titleLayout).apply { this.height=height })
                nameEdit.requestFocus();nameEdit.selectAll()
                context.getSystemService(InputMethodManager::class.java).showSoftInput(nameEdit,InputMethodManager.SHOW_IMPLICIT)
            }
        }
        // The title edits the name; its existing end-chevron still toggles the section.
        var toggleTouch=false
        nameHeading.setOnTouchListener { view,event ->
            when(event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    val end=if(view.layoutDirection==LAYOUT_DIRECTION_RTL) event.x<=dp(BicycleDesign.touchSize)
                        else event.x>=view.width-dp(BicycleDesign.touchSize)
                    toggleTouch=end;end
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val consumed=toggleTouch;toggleTouch=false
                    if(consumed) nameHeader.performClick()
                    consumed
                }
                android.view.MotionEvent.ACTION_CANCEL -> { val consumed=toggleTouch;toggleTouch=false;consumed }
                else -> toggleTouch
            }
        }
        nameEdit.addTextChangedListener(watcher { value ->
            if(runtime.bikes.activeSlot!=slot) return@watcher
            val name=value.trim()
            nameEdit.error=if(name.isEmpty() || name.any(Char::isISOControl)) context.getString(R.string.bikes_name_empty) else null
            if(nameEdit.error==null) { runtime.bikes.save(runtime.bikes.current().copy(name=name));nameHeading.text=name;nameHeader.contentDescription=name;paintCards() }
        })
        nameEdit.setOnEditorActionListener { _,action,_ -> if(action==EditorInfo.IME_ACTION_DONE) { finishNaming();true } else false }
        nameEdit.setOnFocusChangeListener { _,focused -> if(!focused && nameEdit.visibility==VISIBLE) finishNaming() }
        addView(nameHeader,LayoutParams(-1,-2).apply { marginStart=-paddingLeft;marginEnd=-paddingRight })
        photo=BikePhotoView(context,themes,slot,profile.type) { choosePhoto(slot) }.also { addView(it,LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.sectionContentGap) }) }
        if(additionalSections==null) section(R.string.bikes_type)
        typeChoice=Spinner(context).apply {
            tag="bike_type_choice";contentDescription=context.getString(R.string.bikes_type)
            adapter=object:ArrayAdapter<String>(context,android.R.layout.simple_spinner_dropdown_item,BikeType.entries.map { context.getString(it.label) }) {
                override fun areAllItemsEnabled()=false
                override fun isEnabled(position:Int)=BikeType.entries[position].available
            }
            setSelection(profile.type.ordinal)
        }
        BicycleDialogStyle.spinner(typeChoice,themes,BicycleDesign.profileDropdownSp,iconForLabel={ label ->
            BikeTypeIcons.forType(BikeType.entries.first { context.getString(it.label)==label })
        })
        addView(typeChoice,LayoutParams(-1,dp(BicycleDesign.touchSize)))
        val choice=typeChoice
        choice.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:AdapterView<*>?)=Unit
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long) {
                if(!choice.isAttachedToWindow || runtime.bikes.activeSlot!=slot || binding) return
                val type=BikeType.entries[position]
                if(type==runtime.bikes.current().type) return
                if(type.available) {
                    stopSearch()
                    if(!runtime.changeBikeType(type)) BicycleFeedback.show(context,R.string.bikes_profile_locked,false)
                }
                if(runtime.bikes.current().type!=type) choice.setSelection(runtime.bikes.current().type.ordinal)
                refresh()
            }
        }
        indoorTypeChoice=Spinner(context).apply {
            tag="bike_indoor_type_choice";contentDescription=context.getString(R.string.bikes_indoor_type)
            visibility=if(profile.type==BikeType.INDOOR) VISIBLE else GONE
            adapter=object:ArrayAdapter<String>(context,android.R.layout.simple_spinner_dropdown_item,IndoorBikeType.entries.map { context.getString(it.label) }) {
                override fun areAllItemsEnabled()=false
                override fun isEnabled(position:Int)=IndoorBikeType.entries[position].available
            }
            setSelection(profile.indoorType.ordinal)
            onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent:AdapterView<*>?)=Unit
                override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long) {
                    if(!isAttachedToWindow || runtime.bikes.activeSlot!=slot || binding) return
                    val type=IndoorBikeType.entries[position]
                    if(!runtime.setIndoorType(type)) setSelection(runtime.bikes.current().indoorType.ordinal)
                }
            }
        }
        BicycleDialogStyle.spinner(indoorTypeChoice,themes,BicycleDesign.profileDropdownSp)
        addView(indoorTypeChoice,LayoutParams(-1,dp(BicycleDesign.touchSize)))
        val category = Spinner(context).apply {
            visibility=if(profile.type==BikeType.INDOOR) GONE else VISIBLE
            tag="bike_category";contentDescription=context.getString(R.string.bikes_category)
            adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,BikeCategory.entries.map { context.getString(it.label) })
            setSelection(profile.category.ordinal)
        }
        BicycleDialogStyle.spinner(category,themes,BicycleDesign.profileDropdownSp);addView(category,LayoutParams(-1,dp(BicycleDesign.touchSize)))
        category.onItemSelectedListener=object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?,v: View?,position: Int,id: Long) {
                if(category.isAttachedToWindow && runtime.bikes.activeSlot==slot && runtime.bikes.current().category!=BikeCategory.entries[position]) {
                    runtime.bikes.save(runtime.bikes.current().copy(category=BikeCategory.entries[position]));paintCards()
                }
            }
        }
        val wheel=BicycleComponents.input(context,themes).apply {
            tag="bike_wheel_mm";setSingleLine();inputType=android.text.InputType.TYPE_CLASS_NUMBER
            setText(profile.wheelMm.toString());contentDescription=context.getString(R.string.bikes_wheel)
        }
        val wheelRow=BicycleForm.numberRow(wheel,themes,R.string.bikes_wheel,R.string.bikes_wheel_mm).apply {
            tag="bike_wheel_row"; getChildAt(0).tag="bike_wheel_label"
        }
        addView(wheelRow,LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
        wheel.addTextChangedListener(watcher { value ->
            if(runtime.bikes.activeSlot!=slot) return@watcher
            val mm=value.toIntOrNull()
            wheel.error=if(mm==null || mm !in 500..4000) context.getString(R.string.bikes_wheel_error) else null
            if(wheel.error==null) runtime.bikes.save(runtime.bikes.current().copy(wheelMm=mm!!))
        })
        addView(BicycleComponents.sectionDivider(context,themes,BicycleDesign.sectionDividerGap).apply { tag="bike_autostart_divider" })
        autoStartRecording=Switch(context).apply {
            tag="bike_auto_start_recording";setText(R.string.bikes_auto_start_recording);isChecked=profile.autoStartRecording
            setOnCheckedChangeListener { _,enabled -> if(!binding && runtime.bikes.activeSlot==slot)
                runtime.bikes.save(runtime.bikes.current().copy(autoStartRecording=enabled)) }
        }
        BicycleDialogStyle.control(autoStartRecording,themes);addView(autoStartRecording,LayoutParams(-1,-2))
        section(R.string.bikes_sensors)
        autoConnect=Switch(context).apply {
            tag="bike_auto_connect";setText(R.string.bikes_autoconnect);isChecked=profile.autoConnect
            setOnCheckedChangeListener { _,enabled -> if(!binding && runtime.bikes.activeSlot==slot) runtime.setAutoConnect(enabled) }
        }
        BicycleDialogStyle.control(autoConnect,themes);addView(autoConnect,LayoutParams(-1,-2))
        searchButton=BicycleComponents.action(context,themes,context.getString(R.string.bikes_search),search).apply { tag="bike_sensor_search" }
        BicycleDialogStyle.actionIcon(searchButton,themes,BicycleIcon.Bluetooth)
        addView(searchButton,LayoutParams(-1,-2))
        searchStatus=BicycleFeedback.status(context,themes).apply { tag="bike_scan_status";visibility=GONE }
        addView(searchStatus,LayoutParams(-1,-2))
        searchResults=LinearLayout(context).apply { orientation=VERTICAL;tag="bike_scan_results" }
        addView(searchResults,LayoutParams(-1,-2))
        val sensorList=LinearLayout(context).apply { orientation=VERTICAL;tag="bike_sensor_list" }
        addView(sensorList,LayoutParams(-1,-2))
        profile.type.sensors.forEach { sensor ->
            sensorList.addView(BicycleComponents.sectionDivider(context,themes,BicycleDesign.controlGap).apply {
                alpha=0f // Test the separator-free appearance while preserving existing row spacing.
            })
            val toggle=Switch(context).apply {
                tag="bike_sensor_${sensor.name}";setText(sensor.label);isEnabled=sensor.supported
                gravity=Gravity.START or Gravity.CENTER_VERTICAL
                isChecked=when(sensor) { BikeSensor.GPS->runtime.gpsEnabled;BikeSensor.EBIKE->runtime.bikeEnabled;BikeSensor.HEART->runtime.heartEnabled;BikeSensor.TRAINER->runtime.trainerEnabled;else->false }
                setOnCheckedChangeListener { _,enabled ->
                    if(binding || runtime.bikes.activeSlot!=slot) return@setOnCheckedChangeListener
                    when(sensor) {
                        BikeSensor.GPS -> if(enabled) enableGps() else runtime.setGpsEnabled(false)
                        BikeSensor.EBIKE -> runtime.setBikeEnabled(enabled)
                        BikeSensor.HEART -> runtime.setHeartEnabled(enabled)
                        BikeSensor.TRAINER -> runtime.setTrainerEnabled(enabled)
                        BikeSensor.POWER -> Unit
                    }
                    refresh()
                }
            }
            BicycleDialogStyle.control(toggle,themes);switches[sensor]=toggle
            val details=LinearLayout(context).apply {
                orientation=VERTICAL;tag="bike_sensor_details_${sensor.name}"
                visibility=if(sensor in expandedSensors) VISIBLE else GONE
            }
            val expand=BicycleComponents.icon(context,themes,BicycleIcon.Expand,context.getString(R.string.sensor_details,context.getString(sensor.label))) {
                val open=details.visibility!=VISIBLE
                details.visibility=if(open) VISIBLE else GONE
                if(open) expandedSensors.add(sensor) else expandedSensors.remove(sensor)
                val button=findViewWithTag<View>("bike_sensor_expand_${sensor.name}")
                button.rotation=if(open) 180f else 0f
                button.stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
            }.apply {
                tag="bike_sensor_expand_${sensor.name}";rotation=if(sensor in expandedSensors) 180f else 0f
                stateDescription=context.getString(if(sensor in expandedSensors) R.string.zone_expanded else R.string.zone_collapsed)
            }
            sensorList.addView(LinearLayout(context).apply {
                tag=BicycleDesign.inlineControlRowTag;gravity=Gravity.CENTER_VERTICAL
                BicycleComponents.switchAccordion(this,toggle,expand,themes,icon=SensorIcons.forSensor(sensor))
            },LayoutParams(-1,dp(BicycleDesign.sectionHeaderMinHeight)+2*BicycleDesign.linePixels(context,BicycleDesign.sectionRuleDp)))
            sensorList.addView(details,LayoutParams(-1,-2))
            if(sensor!=BikeSensor.GPS) {
                val device=BicycleComponents.text(context,themes,"");deviceNames[sensor]=device;details.addView(device,LayoutParams(-1,-2))
                val battery=BicycleComponents.text(context,themes,"");batteries[sensor]=battery;details.addView(battery,LayoutParams(-1,-2))
            }
            val state=BicycleFeedback.status(context,themes).apply {
                tag=when(sensor) { BikeSensor.GPS->"gps_state";BikeSensor.EBIKE->"bike_state";BikeSensor.HEART->"heart_state";else->"power_state" }
                textSize=BicycleDesign.sensorStatusSp
            }
            statuses[sensor]=state
            val received=SensorMeasurementDetails(context,themes,state).apply { tag="sensor_measurements_${sensor.name}" }
            measurementDetails[sensor]=received;details.addView(received,LayoutParams(-1,-2))
            if(sensor.supported) details.addView(LinearLayout(context).apply {
                orientation=HORIZONTAL
                val disconnect=BicycleComponents.action(context,themes,context.getString(R.string.bikes_disconnect)) { runtime.disconnectSensor(sensor) }.apply { tag="bike_disconnect_${sensor.name}" }
                BicycleDialogStyle.actionIcon(disconnect,themes,BicycleIcon.Disconnect)
                disconnects[sensor]=disconnect;addView(disconnect,LayoutParams(0,-2,1f))
                if(sensor!=BikeSensor.GPS) addView(BicycleComponents.icon(context,themes,BicycleIcon.DeleteTrack,context.getString(R.string.bikes_remove)) { runtime.removeSensor(sensor);refresh() }.apply { tag="bike_remove_${sensor.name}";removes[sensor]=this },LayoutParams(dp(BicycleDesign.touchSize),dp(BicycleDesign.touchSize)).apply { marginStart=dp(BicycleDesign.controlGap);topMargin=dp(BicycleDesign.controlGap) })
            },LayoutParams(-1,-2))
        }
        if (profile.type == BikeType.INDOOR) {
            section(R.string.indoor_correction_title)
            addView(text(R.string.indoor_correction_description),LayoutParams(-1,-2))
            correctionSlider(slot, R.string.indoor_correction_power, "power", profile.indoorCorrection.powerTenths)
            correctionSlider(slot, R.string.indoor_correction_speed, "speed", profile.indoorCorrection.speedTenths)
        }
        val recording=recordingControls()
        if(recording is LinearLayout && additionalSections!=null) {
            while(recording.childCount>0) {
                val child=recording.getChildAt(0);recording.removeViewAt(0)
                addView(child,child.layoutParams)
            }
        } else addView(recording,LayoutParams(-1,-2))
        additionalSections?.invoke()?.filter { (title,_) -> title!=R.string.ui_settings_map || BikeSensor.GPS in profile.type.sensors }?.forEach { (title,body) ->
            val heading=section(title)
            if(title==R.string.training_help_title) {
                heading.revealOnOpen=true
                (body as? TrainingHelpView)?.revealAnchor=heading
            }
            if(title==R.string.ui_design_title) {
                designHeader=heading
                dashboardActions=body.findViewWithTag<LinearLayout>("dashboard_inline_actions")?.also {
                    (it.parent as? android.view.ViewGroup)?.removeView(it)
                    it.tag="dashboard_footer_actions"
                }
            }
            addView(body,LayoutParams(-1,-2))
        }
        groupSections()
        refresh();footerChanged()
    }
    private fun groupSections() {
        val nodes=(0 until childCount).map(::getChildAt)
        removeAllViews()
        var body:LinearLayout?=null
        nodes.forEach { node ->
            if(node is BicycleSectionHeader) {
                body=null
                addView(node,LayoutParams(-1,-2).apply { marginStart=-paddingLeft;marginEnd=-paddingRight })
                run {
                    val key=if(node.tag=="bike_name_header") "bike_profile" else node.contentDescription.toString()
                    body=LinearLayout(context).apply {
                        orientation=VERTICAL
                        tag=when(key) {
                            "bike_profile"->"bike_profile_details"
                            context.getString(R.string.ui_recording_title)->"bike_fit_settings"
                            context.getString(R.string.ui_track_recording)->"bike_track_settings"
                            else->"profile_section_content_$key"
                        }
                    }
                    addView(body,LayoutParams(-1,-2))
                    node.accordion(body!!,key !in collapsedSections) { open ->
                        if(open) collapsedSections.remove(key) else collapsedSections.add(key)
                    }
                }
            } else if(body!=null) body!!.addView(node,node.layoutParams) else addView(node,node.layoutParams)
        }
    }
    private fun correctionSlider(slot: Int, title: Int, key: String, step: Int) {
        val value = BicycleComponents.text(context,themes,"").apply {
            tag="indoor_correction_${key}_value"; typeface=BicycleDesign.narrowTypeface(context)
            textSize=BicycleDesign.settingsMetricSp; gravity=Gravity.END or Gravity.CENTER_VERTICAL
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val slider = SeekBar(context).apply {
            id=View.generateViewId(); tag="indoor_correction_$key"
            min=0; max=30; progress=step; keyProgressIncrement=1
            minimumHeight=dp(BicycleDesign.touchSize); contentDescription=context.getString(title)
        }
        addView(LinearLayout(context).apply {
            gravity=Gravity.CENTER_VERTICAL
            addView(text(title).apply { labelFor=slider.id },LayoutParams(0,-2,1f))
            addView(value,LayoutParams(-2,-2).apply { marginStart=dp(BicycleDesign.controlGap) })
        },LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
        addView(slider,LayoutParams(-1,-2))
        BicycleComponents.controlSpacing(slider)
        BicycleComponents.bindTheme(slider,themes) { tokens ->
            val accent=android.content.res.ColorStateList.valueOf(tokens.primary)
            slider.thumbTintList=accent; slider.progressTintList=accent
            slider.progressBackgroundTintList=android.content.res.ColorStateList.valueOf((tokens.foreground and 0x00ffffff) or 0x55000000)
        }
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser || binding || runtime.bikes.activeSlot!=slot) return
                val old=runtime.bikes.current().indoorCorrection
                val next=if(key=="power") old.copy(powerTenths=progress) else old.copy(speedTenths=progress)
                runtime.setIndoorCorrection(next)
                refresh()
            }
        })
        correctionControls += slider to value
    }
    fun revealSensorSetup() {
        val header=(0 until childCount).map(::getChildAt).filterIsInstance<BicycleSectionHeader>()
            .firstOrNull { it.contentDescription==context.getString(R.string.bikes_sensors) } ?: return
        if(header.expanded==false) header.performClick()
        BicycleContentSpacing.revealAfterLayout(header);requestLayout()
    }
    fun beginSearch() { searchResults.removeAllViews();searchStatus.visibility=VISIBLE }
    fun scanStatus(message: String) { BicycleFeedback.update(searchStatus,message,hideWhenEmpty=true) }
    fun addCandidate(name: String, address: String, sensor: BikeSensor, choose: () -> Unit) {
        searchResults.addView(BicycleComponents.action(context,themes,context.getString(R.string.bikes_found_sensor,name,context.getString(sensor.label))) { choose() }.apply {
            tag="bike_found_$address";contentDescription=context.getString(R.string.bikes_found_sensor,name,context.getString(sensor.label))
        },LayoutParams(-1,-2))
    }
    fun clearSearch() { searchResults.removeAllViews();searchStatus.visibility=GONE }
    private fun paintCards() { strip.show(runtime.bikes.all(),runtime.bikes.activeSlot,runtime.recording || runtime.saving) }
    fun refreshPhoto() { photo?.refresh() }
    fun refresh() {
        assignmentButton.visibility=if(runtime.mapState.orphanProfiles().isNotEmpty()) VISIBLE else GONE
        assignmentButton.isEnabled=!runtime.mapState.busy
        if(shownSlot!=runtime.bikes.activeSlot || shownType!=runtime.bikes.current().type || shownSources!=RecordingSources.available(runtime.bikes.current(),runtime.barometer.available)) { rebuild();return }
        binding=true
        try {
            val p=runtime.bikes.current();paintCards();autoConnect.isChecked=p.autoConnect;autoStartRecording.isChecked=p.autoStartRecording
            indoorTypeChoice.setSelection(p.indoorType.ordinal)
            findViewWithTag<RideNamingView>("ride_naming")?.refreshLock()
            findViewWithTag<RecordingSourcesView>("recording_sources")?.refreshLock()
            searchButton.isEnabled=!runtime.recording && !runtime.saving
            searchButton.alpha=if(searchButton.isEnabled) 1f else .4f
            val searchLabel=when {
                runtime.recording->R.string.bikes_search_recording_locked
                runtime.saving->R.string.bikes_search_saving_locked
                else->R.string.bikes_search
            }
            searchButton.setText(searchLabel)
            searchButton.stateDescription=if(searchButton.isEnabled) null else context.getString(searchLabel)
            correctionControls.forEachIndexed { index,(slider,value) ->
                slider.isEnabled=!runtime.recording && !runtime.saving
                slider.alpha=if(slider.isEnabled) 1f else .4f
                val step=if(index==0) p.indoorCorrection.powerTenths else p.indoorCorrection.speedTenths
                slider.progress=step
                val factor=context.getString(R.string.indoor_correction_factor,step/10.0)
                value.text=factor; slider.stateDescription=factor
            }
            typeChoice.isEnabled=!runtime.recording && !runtime.saving
            typeChoice.alpha=if(typeChoice.isEnabled) 1f else .4f
            indoorTypeChoice.isEnabled=typeChoice.isEnabled
            indoorTypeChoice.alpha=typeChoice.alpha
            deleteAction.visibility=if(runtime.bikes.all().size>1) VISIBLE else GONE
            deleteAction.isEnabled=!runtime.recording && !runtime.saving
            copyAction.isEnabled=!runtime.recording && !runtime.saving && runtime.bikes.all().size>1
            switches.forEach { (s,v) -> v.isChecked=when(s) { BikeSensor.GPS->runtime.gpsEnabled;BikeSensor.EBIKE->runtime.bikeEnabled;BikeSensor.HEART->runtime.heartEnabled;BikeSensor.TRAINER->runtime.trainerEnabled;else->false } }
            statuses.forEach { (sensor,view) ->
                val value=if(!sensor.supported) context.getString(R.string.bikes_not_available) else if(runtime.disconnected(sensor)) context.getString(R.string.bikes_disconnected) else when(sensor) {
                    BikeSensor.GPS->runtime.gpsStatus;BikeSensor.EBIKE->runtime.bikeStatus;BikeSensor.HEART->runtime.heartStatus;BikeSensor.TRAINER->runtime.trainer.status;else->""
                }
                val count=runtime.receivedValueCount(sensor)
                BicycleFeedback.update(view,if(count!=null && count>0) context.getString(R.string.sensor_received_values,value,
                    resources.getQuantityString(R.plurals.sensor_value_count,count,count)) else value)
                measurementDetails[sensor]?.update(runtime.receivedValueNames(sensor))
                deviceNames[sensor]?.let { BicycleFeedback.update(it,when(sensor) { BikeSensor.EBIKE->p.bikeName;BikeSensor.HEART->p.heartName;BikeSensor.TRAINER->p.trainerName;else->null } ?: context.getString(R.string.bikes_not_assigned)) }
                val battery=when(sensor) { BikeSensor.EBIKE->runtime.currentBikeBattery?.toInt();BikeSensor.HEART->runtime.pulseValues.battery.takeIf { runtime.heart.live };BikeSensor.TRAINER->runtime.trainer.battery;else->null }
                batteries[sensor]?.let { BicycleFeedback.update(it,context.getString(R.string.bikes_battery, battery?.let { value -> context.getString(R.string.bikes_battery_value,value) } ?: context.getString(R.string.bikes_battery_missing))) }
                disconnects[sensor]?.apply {
                    isEnabled=when(sensor) { BikeSensor.GPS->runtime.gpsSensor.active;BikeSensor.EBIKE->runtime.connection.isActive;BikeSensor.HEART->runtime.heart.active;BikeSensor.TRAINER->runtime.trainer.active;else->false }
                    alpha=if(isEnabled) 1f else .4f
                }
                removes[sensor]?.apply {
                    isEnabled=when(sensor) { BikeSensor.EBIKE->p.bikeAddress!=null;BikeSensor.HEART->p.heartAddress!=null;BikeSensor.TRAINER->p.trainerAddress!=null;else->false }
                    alpha=if(isEnabled) 1f else .4f
                }
            }
        } finally { binding=false }
    }
}
