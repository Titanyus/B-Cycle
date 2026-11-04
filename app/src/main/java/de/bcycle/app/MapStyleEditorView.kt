package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*

/** Small on-map editor. Persist per bike, throttle live colours and debounce atlas/pattern changes, export a portable delta. */
internal class MapStyleEditorView(context:Context,private val prefs:SharedPreferences,private val themes:ThemeManager,
    private val changed:()->Unit,private val export:(String)->Unit):LinearLayout(context) {
    private val profile=prefs.getInt(BikeProfileStore.ACTIVE,0)
    private val base=context.assets.open("styles/bright-vtm.xml").bufferedReader().use { it.readText() }
    private val handler=Handler(Looper.getMainLooper())
    private val preview=Runnable { if(currentProfile()) changed() }
    private val target=Spinner(context).apply { tag="map_style_target" }
    private val swatch=View(context)
    private val hex=BicycleComponents.text(context,themes,"").apply { tag="map_style_rgb_hex" }
    private val rgb=IntArray(3)
    private val channels=Array(3) { index -> BicycleSettingSlider(context,themes,Int::toString).apply {
        tag="map_style_rgb_$index";max=255;min=0
        contentDescription=context.getString(listOf(R.string.map_style_red,R.string.map_style_green,R.string.map_style_blue)[index])
    } }
    private val line=Spinner(context).apply { tag="map_style_dash" }
    private val lineLabel=BicycleComponents.fieldLabel(context,themes,context.getString(R.string.map_style_line))
    private var selected=MapStyleTarget.BACKGROUND
    private var updating=false
    init {
        orientation=VERTICAL;tag="map_style_editor"
        addView(BicycleComponents.description(context,themes,context.getString(R.string.map_style_hint)))
        addView(BicycleComponents.fieldLabel(context,themes,context.getString(R.string.map_style_object)))
        target.adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,MapStyleTarget.entries.map { context.getString(it.title) })
        target.contentDescription=context.getString(R.string.map_style_object);addView(target,LayoutParams(-1,-2))
        addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;tag=BicycleDesign.inlineControlRowTag
            addView(swatch,LayoutParams(BicycleDesign.dp(context,32),BicycleDesign.dp(context,32)))
            addView(hex,LayoutParams(0,-2,1f).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        },LayoutParams(-1,-2))
        channels.forEachIndexed { index,slider ->
            val label=context.getString(listOf(R.string.map_style_red,R.string.map_style_green,R.string.map_style_blue)[index])
            addView(settingSliderRow(context,themes,label,slider),LayoutParams(-1,-2))
            slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,value:Int,fromUser:Boolean) {
                    if(!updating && currentProfile()) { rgb[index]=value;val chosen=Color.rgb(rgb[0],rgb[1],rgb[2]);save(current().copy(color=chosen));displayColor(chosen) }
                }
            })
        }
        addView(lineLabel)
        line.adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,MapDash.entries.map { context.getString(it.title) })
        line.contentDescription=context.getString(R.string.map_style_line);addView(line,LayoutParams(-1,-2))
        fun selection(spinner:Spinner,action:(Int)->Unit) { spinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:AdapterView<*>?)=Unit
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long) { action(position) }
        } }
        selection(target) { selected=MapStyleTarget.entries[it];render() }
        selection(line) { if(!updating && selected.line) {
            val value=current();val dash=MapDash.entries[it]
            if(value.dash!=dash) save(value.copy(dash=dash))
        } }
        addView(BicycleComponents.action(context,themes,context.getString(R.string.map_style_reset_object)) {
            if(currentProfile()) { MapStyleOverrides.read(prefs).update(selected,null).write(prefs);render();schedule() }
        }.apply { tag="map_style_reset_object" })
        addView(BicycleComponents.action(context,themes,context.getString(R.string.map_style_reset_all)) {
            if(currentProfile()) { MapStyleOverrides().write(prefs);render();schedule() }
        }.apply { tag="map_style_reset_all" })
        addView(BicycleComponents.action(context,themes,context.getString(R.string.map_style_export)) {
            if(currentProfile()) export(MapStyleOverrides.read(prefs).export(prefs))
        }.apply { tag="map_style_export" })
        BicycleDialogStyle.content(this,themes);render()
    }
    private fun currentProfile()=prefs.getInt(BikeProfileStore.ACTIVE,0)==profile
    private fun current()=MapStyleOverrides.read(prefs).objects[selected] ?: MapObjectStyle()
    private fun schedule(live:Boolean=false) {
        if(live) { if(!handler.hasCallbacks(preview)) handler.postDelayed(preview,48) }
        else { handler.removeCallbacks(preview);handler.postDelayed(preview,180) }
    }
    private fun save(value:MapObjectStyle) {
        if(!currentProfile()) return
        val previous=MapStyleOverrides.read(prefs)
        val next=previous.update(selected,value)
        next.write(prefs);schedule(live=next.canUpdateColorsFrom(previous))
    }
    private fun displayColor(value:Int) {
        hex.text=String.format(java.util.Locale.ROOT,"#%06X",value and 0xffffff)
        swatch.background=BicycleComponents.controlBackground(context,themes.active.tokens,value)
    }
    private fun render() {
        updating=true
        val value=current();val initial=value.color ?: MapStyleXml.defaultColor(base,selected)
        rgb[0]=Color.red(initial);rgb[1]=Color.green(initial);rgb[2]=Color.blue(initial)
        channels.forEachIndexed { index,slider -> slider.progress=rgb[index] }
        displayColor(initial)
        lineLabel.visibility=if(selected.line) VISIBLE else GONE;line.visibility=lineLabel.visibility
        line.setSelection(value.dash.ordinal);updating=false
    }
    override fun onDetachedFromWindow() {
        if(handler.hasCallbacks(preview)) { handler.removeCallbacks(preview);if(currentProfile()) changed() }
        super.onDetachedFromWindow()
    }
}
