package de.bcycle.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*

/** One profile page: tiles, editable heading, configuration, sensors, then FIT. No nested scrolling. */
internal class BikesView(context: Context, private val runtime: RideRuntime,
    private val search: () -> Unit, private val stopSearch: () -> Unit, private val enableGps: () -> Unit, private val recordingControls: () -> View) : LinearLayout(context) {
    private val themes = runtime.themes
    private var shownSlot = -1
    private var binding = false
    private var cardSignature = ""
    private val cards = mutableListOf<LinearLayout>()
    private val cardNames = mutableListOf<TextView>()
    private val cardImages = mutableListOf<ImageView>()
    private val statuses = mutableMapOf<BikeSensor, TextView>()
    private val batteries = mutableMapOf<BikeSensor, TextView>()
    private val switches = mutableMapOf<BikeSensor, Switch>()
    private val disconnects = mutableMapOf<BikeSensor, Button>()
    private val deviceNames = mutableMapOf<BikeSensor, TextView>()
    private val removes = mutableMapOf<BikeSensor, ImageButton>()
    private lateinit var nameHeading: TextView
    private lateinit var autoConnect: Switch
    private lateinit var searchButton: Button
    private lateinit var searchResults: LinearLayout
    private lateinit var searchStatus: TextView
    private fun dp(n: Int) = BicycleDesign.dp(context,n)
    init { orientation = VERTICAL; tag = "bikes_manager"; setPadding(dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.verticalPadding),dp(BicycleDesign.horizontalPadding),0); rebuild() }
    private fun text(id: Int, heading: Boolean = false) = BicycleComponents.text(context,themes,context.getString(id),heading)
    private fun section(id: Int) { addView(BicycleComponents.sectionDivider(context,themes));addView(text(id,true)) }
    private fun watcher(change: (String)->Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?,start: Int,count: Int,after: Int) = Unit
        override fun onTextChanged(s: CharSequence?,start: Int,before: Int,count: Int) { change(s.toString()) }
        override fun afterTextChanged(s: Editable?) = Unit
    }
    private fun rebuild() {
        shownSlot = runtime.bikes.activeSlot
        removeAllViews(); cardSignature="";cards.clear();cardNames.clear();cardImages.clear();statuses.clear();batteries.clear();switches.clear();disconnects.clear();deviceNames.clear();removes.clear()
        val profile = runtime.bikes.current(); val slot = profile.slot
        val tiles = LinearLayout(context).apply { orientation = HORIZONTAL;isBaselineAligned=false }
        repeat(BikeProfileStore.COUNT) { index ->
            val card = LinearLayout(context).apply {
                orientation = VERTICAL;gravity=Gravity.CENTER;tag="bike_profile_$index";isClickable=true;isFocusable=true
                setPadding(dp(BicycleDesign.smallGap),dp(BicycleDesign.verticalPadding),dp(BicycleDesign.smallGap),dp(BicycleDesign.verticalPadding))
                setOnClickListener {
                    stopSearch()
                    if (!runtime.switchBike(index)) BicycleFeedback.show(context,R.string.bikes_profile_locked,false)
                    refresh()
                }
            }
            val image = ImageView(context).apply { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
            val caption = BicycleComponents.text(context,themes,"").apply { gravity=Gravity.CENTER;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
            card.addView(image,LayoutParams(-1,dp(BicycleDesign.bikePreviewHeight)))
            card.addView(caption,LayoutParams(-1,-2))
            cards+=card;cardNames+=caption;cardImages+=image
            BicycleComponents.bindTheme(card,themes) { paintCards() }
            tiles.addView(card,LayoutParams(0,dp(BicycleDesign.bikeTileHeight),1f).apply { if(index>0) marginStart=dp(BicycleDesign.controlGap) })
        }
        addView(tiles,LayoutParams(-1,-2))
        nameHeading = BicycleComponents.text(context,themes,profile.name,true).apply {
            tag="bike_profile_name";gravity=Gravity.CENTER;minimumHeight=dp(BicycleDesign.touchSize);isClickable=true;isFocusable=true
            contentDescription=context.getString(R.string.bikes_name_edit)
        }
        val nameEdit = BicycleComponents.input(context,themes).apply {
            tag="bike_profile_name_edit";visibility=GONE;setSingleLine();setText(profile.name)
            hint=context.getString(R.string.bikes_name_hint);contentDescription=context.getString(R.string.bikes_name_edit)
            filters=arrayOf(android.text.InputFilter.LengthFilter(60));imeOptions=EditorInfo.IME_ACTION_DONE
        }
        fun finishNaming() {
            if(nameEdit.text.toString().trim().isEmpty()) return
            nameEdit.visibility=GONE;nameHeading.visibility=VISIBLE;nameEdit.clearFocus()
            context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(nameEdit.windowToken,0)
        }
        nameHeading.setOnClickListener {
            nameHeading.visibility=GONE;nameEdit.visibility=VISIBLE;nameEdit.requestFocus();nameEdit.selectAll()
            context.getSystemService(InputMethodManager::class.java).showSoftInput(nameEdit,InputMethodManager.SHOW_IMPLICIT)
        }
        nameEdit.addTextChangedListener(watcher { value ->
            if(runtime.bikes.activeSlot!=slot) return@watcher
            val name=value.trim()
            nameEdit.error=if(name.isEmpty() || name.any(Char::isISOControl)) context.getString(R.string.bikes_name_empty) else null
            if(nameEdit.error==null) { runtime.bikes.save(runtime.bikes.current().copy(name=name));nameHeading.text=name;paintCards() }
        })
        nameEdit.setOnEditorActionListener { _,action,_ -> if(action==EditorInfo.IME_ACTION_DONE) { finishNaming();true } else false }
        nameEdit.setOnFocusChangeListener { _,focused -> if(!focused && nameEdit.visibility==VISIBLE) finishNaming() }
        addView(nameHeading,LayoutParams(-1,-2));addView(nameEdit,LayoutParams(-1,-2))
        addView(text(R.string.bikes_type,true))
        addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;isBaselineAligned=false
            BikeType.entries.forEachIndexed { i,type ->
                addView(BicycleComponents.action(context,themes,context.getString(type.label)) {}.apply {
                    tag="bike_type_${type.name}";isEnabled=type.available;alpha=if(type.available) 1f else .4f;isSelected=profile.type==type
                    contentDescription=if(type.available) context.getString(type.label) else context.getString(R.string.bikes_type_unavailable,context.getString(type.label))
                    if(isSelected) stateDescription=context.getString(R.string.bikes_selected)
                },LayoutParams(0,-2,1f).apply { if(i>0) marginStart=dp(BicycleDesign.controlGap) })
            }
        },LayoutParams(-1,-2))
        addView(text(R.string.bikes_category),LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
        val category = Spinner(context).apply {
            tag="bike_category";contentDescription=context.getString(R.string.bikes_category)
            adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,BikeCategory.entries.map { context.getString(it.label) })
            setSelection(profile.category.ordinal)
        }
        BicycleDialogStyle.spinner(category,themes);addView(category,LayoutParams(-1,dp(BicycleDesign.touchSize)))
        category.onItemSelectedListener=object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?,v: View?,position: Int,id: Long) {
                if(runtime.bikes.activeSlot==slot && runtime.bikes.current().category!=BikeCategory.entries[position]) {
                    runtime.bikes.save(runtime.bikes.current().copy(category=BikeCategory.entries[position]));paintCards()
                }
            }
        }
        addView(text(R.string.bikes_wheel),LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
        val wheel=BicycleComponents.input(context,themes).apply {
            tag="bike_wheel_mm";setSingleLine();inputType=android.text.InputType.TYPE_CLASS_NUMBER
            setText(profile.wheelMm.toString());contentDescription=context.getString(R.string.bikes_wheel)
        }
        addView(LinearLayout(context).apply {
            gravity=Gravity.CENTER_VERTICAL
            addView(wheel,LayoutParams(0,-2,1f))
            addView(text(R.string.bikes_wheel_mm).apply { tag="bike_wheel_unit";setTextColor(BicycleDesign.dashboardUnitColor);gravity=Gravity.CENTER },LayoutParams(dp(BicycleDesign.touchSize),-2))
        },LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
        wheel.addTextChangedListener(watcher { value ->
            if(runtime.bikes.activeSlot!=slot) return@watcher
            val mm=value.toIntOrNull()
            wheel.error=if(mm==null || mm !in 500..4000) context.getString(R.string.bikes_wheel_error) else null
            if(wheel.error==null) runtime.bikes.save(runtime.bikes.current().copy(wheelMm=mm!!))
        })
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
        profile.type.sensors.forEach { sensor ->
            addView(BicycleComponents.sectionDivider(context,themes))
            val toggle=Switch(context).apply {
                tag="bike_sensor_${sensor.name}";setText(sensor.label);isEnabled=sensor.supported
                isChecked=when(sensor) { BikeSensor.GPS->runtime.gpsEnabled;BikeSensor.EBIKE->runtime.bikeEnabled;BikeSensor.HEART->runtime.heartEnabled;else->false }
                setOnCheckedChangeListener { _,enabled ->
                    if(binding || runtime.bikes.activeSlot!=slot) return@setOnCheckedChangeListener
                    when(sensor) {
                        BikeSensor.GPS -> if(enabled) enableGps() else runtime.setGpsEnabled(false)
                        BikeSensor.EBIKE -> runtime.setBikeEnabled(enabled)
                        BikeSensor.HEART -> runtime.setHeartEnabled(enabled)
                        BikeSensor.POWER, BikeSensor.TRAINER -> Unit
                    }
                    refresh()
                }
            }
            BicycleDialogStyle.control(toggle,themes);switches[sensor]=toggle;addView(toggle,LayoutParams(-1,-2))
            if(sensor!=BikeSensor.GPS) {
                val device=BicycleComponents.text(context,themes,"");deviceNames[sensor]=device;addView(device,LayoutParams(-1,-2))
                val battery=BicycleComponents.text(context,themes,"");batteries[sensor]=battery;addView(battery,LayoutParams(-1,-2))
            }
            val state=BicycleFeedback.status(context,themes).apply {
                tag=when(sensor) { BikeSensor.GPS->"gps_state";BikeSensor.EBIKE->"bike_state";BikeSensor.HEART->"heart_state";else->"power_state" }
                textSize=BicycleDesign.sensorStatusSp
            }
            statuses[sensor]=state;addView(state,LayoutParams(-1,-2))
            if(sensor.supported) addView(LinearLayout(context).apply {
                orientation=HORIZONTAL
                val disconnect=BicycleComponents.action(context,themes,context.getString(R.string.bikes_disconnect)) { runtime.disconnectSensor(sensor) }.apply { tag="bike_disconnect_${sensor.name}" }
                BicycleDialogStyle.actionIcon(disconnect,themes,BicycleIcon.Disconnect)
                disconnects[sensor]=disconnect;addView(disconnect,LayoutParams(0,-2,1f))
                if(sensor!=BikeSensor.GPS) addView(BicycleComponents.icon(context,themes,BicycleIcon.DeleteTrack,context.getString(R.string.bikes_remove)) { runtime.removeSensor(sensor);refresh() }.apply { tag="bike_remove_${sensor.name}";removes[sensor]=this },LayoutParams(dp(BicycleDesign.touchSize),dp(BicycleDesign.touchSize)).apply { marginStart=dp(BicycleDesign.controlGap);topMargin=dp(BicycleDesign.controlGap) })
            },LayoutParams(-1,-2))
        }
        section(R.string.ui_recording_title)
        addView(recordingControls(),LayoutParams(-1,-2))
        refresh()
    }
    fun beginSearch() { searchResults.removeAllViews();searchStatus.visibility=VISIBLE }
    fun scanStatus(message: String) { BicycleFeedback.update(searchStatus,message,hideWhenEmpty=true) }
    fun addCandidate(name: String, address: String, sensor: BikeSensor, choose: () -> Unit) {
        searchResults.addView(BicycleComponents.action(context,themes,context.getString(R.string.bikes_found_sensor,name,context.getString(sensor.label))) { choose() }.apply {
            tag="bike_found_$address";contentDescription=context.getString(R.string.bikes_found_sensor,name,context.getString(sensor.label))
        },LayoutParams(-1,-2))
    }
    fun clearSearch() { searchResults.removeAllViews();searchStatus.visibility=GONE }
    private fun paintCards() {
        if(cards.isEmpty()) return
        val tokens=themes.active.tokens
        val profiles=runtime.bikes.all()
        val signature="${cards.size}:${runtime.bikes.activeSlot}:${tokens.hashCode()}:"+profiles.joinToString { "${it.name}:${it.category}" }
        if(signature==cardSignature) return
        cardSignature=signature
        profiles.forEachIndexed { index,p ->
            if(index>=cards.size) return@forEachIndexed
            val selected=index==runtime.bikes.activeSlot
            cards[index].isSelected=selected;cards[index].contentDescription=p.name
            cards[index].stateDescription=if(selected) context.getString(R.string.bikes_selected) else context.getString(R.string.ui_not_selected)
            cards[index].background=GradientDrawable().apply { setColor(if(selected) tokens.tertiary else tokens.secondary);cornerRadius=dp(BicycleDesign.inputRadius).toFloat();setStroke(dp(if(selected) 2 else 1),tokens.foreground) }
            cardNames[index].text=p.name
            cardImages[index].setImageDrawable(BikePreview(p.category).apply { setTint(tokens.foreground) })
        }
    }
    fun refresh() {
        if(shownSlot!=runtime.bikes.activeSlot) { rebuild();return }
        binding=true
        try {
            val p=runtime.bikes.current();paintCards();autoConnect.isChecked=p.autoConnect
            searchButton.isEnabled=!runtime.recording && !runtime.saving
            cards.forEach { it.isEnabled=!runtime.recording && !runtime.saving }
            switches.forEach { (s,v) -> v.isChecked=when(s) { BikeSensor.GPS->runtime.gpsEnabled;BikeSensor.EBIKE->runtime.bikeEnabled;BikeSensor.HEART->runtime.heartEnabled;else->false } }
            statuses.forEach { (sensor,view) ->
                val value=if(!sensor.supported) context.getString(R.string.bikes_not_available) else if(runtime.disconnected(sensor)) context.getString(R.string.bikes_disconnected) else when(sensor) {
                    BikeSensor.GPS->runtime.gpsStatus;BikeSensor.EBIKE->runtime.bikeStatus;BikeSensor.HEART->runtime.heartStatus;else->""
                }
                BicycleFeedback.update(view,value)
                deviceNames[sensor]?.let { BicycleFeedback.update(it,when(sensor) { BikeSensor.EBIKE->p.bikeName;BikeSensor.HEART->p.heartName;else->null } ?: context.getString(R.string.bikes_not_assigned)) }
                val battery=when(sensor) { BikeSensor.EBIKE->runtime.currentBikeBattery?.toInt();BikeSensor.HEART->runtime.pulseValues.battery.takeIf { runtime.heart.live };else->null }
                batteries[sensor]?.let { BicycleFeedback.update(it,context.getString(R.string.bikes_battery, battery?.let { value -> context.getString(R.string.bikes_battery_value,value) } ?: context.getString(R.string.bikes_battery_missing))) }
                disconnects[sensor]?.apply {
                    isEnabled=when(sensor) { BikeSensor.GPS->runtime.gpsSensor.active;BikeSensor.EBIKE->runtime.connection.isActive;BikeSensor.HEART->runtime.heart.active;else->false }
                    alpha=if(isEnabled) 1f else .4f
                }
                removes[sensor]?.apply {
                    isEnabled=when(sensor) { BikeSensor.EBIKE->p.bikeAddress!=null;BikeSensor.HEART->p.heartAddress!=null;else->false }
                    alpha=if(isEnabled) 1f else .4f
                }
            }
        } finally { binding=false }
    }
}
