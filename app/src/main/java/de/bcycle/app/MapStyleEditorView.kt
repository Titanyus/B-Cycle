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
    private val changed:()->Unit,private val export:(String)->Unit,private val close:()->Unit={}):LinearLayout(context) {
    private val profile=prefs.getInt(BikeProfileStore.ACTIVE,0)
    private val base=context.assets.open("styles/bright-vtm.xml").bufferedReader().use { it.readText() }
    private val handler=Handler(Looper.getMainLooper())
    private val preview=Runnable { if(currentProfile()) changed() }
    private val targets=MapStyleTarget.entries.filterNot { it in setOf(MapStyleTarget.CYCLE,MapStyleTarget.CYCLE_ROUTES) }
    private val target=Spinner(context).apply { tag="map_style_target" }
    private val swatch=View(context).apply { tag="map_style_swatch" }
    private val hex=BicycleComponents.text(context,themes,"").apply { tag="map_style_rgb_hex" }
    private val rgb=IntArray(4)
    private val channels=Array(4) { index -> BicycleSettingSlider(context,themes,Int::toString).apply {
        tag="map_style_rgb_$index";max=255;min=0
        contentDescription=context.getString(listOf(R.string.map_style_red,R.string.map_style_green,R.string.map_style_blue,R.string.map_style_alpha)[index])
    } }
    private val widthSlider=BicycleSettingSlider(context,themes) { "$it %" }.apply {
        tag="map_style_width";min=25;max=300;contentDescription=context.getString(R.string.map_style_width)
    }
    private val widthRow=settingSliderRow(context,themes,context.getString(R.string.map_style_width),widthSlider).apply {
        minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize);setPadding(0,0,0,0)
    }
    private val placeText=MapPlaceTextEditor(context,prefs,themes) { schedule() }
    private val line=Spinner(context).apply { tag="map_style_dash" }
    private val surfaceHint=BicycleComponents.description(context,themes,context.getString(R.string.map_style_surface_hint))
    private val lineLabel=BicycleComponents.fieldLabel(context,themes,context.getString(R.string.map_style_line))
    private var selected=runCatching { MapStyleTarget.valueOf(prefs.getString(LAST_TARGET,null) ?: "BACKGROUND") }.getOrDefault(MapStyleTarget.BACKGROUND).takeIf { it in targets } ?: MapStyleTarget.BACKGROUND
    private var updating=false
    init {
        orientation=VERTICAL;tag="map_style_editor"
        addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setTag(R.id.inline_control_row,true)
            addView(BicycleComponents.fieldLabel(context,themes,context.getString(R.string.map_style_object)),LayoutParams(0,-2,1f))
            addView(hex,LayoutParams(-2,-2))
        },LayoutParams(-1,-2))
        target.adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,targets.map { context.getString(it.title) })
        target.setSelection(targets.indexOf(selected))
        target.contentDescription=context.getString(R.string.map_style_object)
        addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setTag(R.id.inline_control_row,true)
            addView(target,LayoutParams(0,-2,1f))
            addView(swatch,LayoutParams(BicycleDesign.dp(context,32),BicycleDesign.dp(context,32)).apply {
                marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap)
            })
        },LayoutParams(-1,-2))
        channels.forEachIndexed { index,slider ->
            val label=context.getString(listOf(R.string.map_style_red,R.string.map_style_green,R.string.map_style_blue,R.string.map_style_alpha)[index])
            addView(settingSliderRow(context,themes,label,slider).apply {
                minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
                setPadding(0,0,0,0)
            },LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.touchSize)))
            slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,value:Int,fromUser:Boolean) {
                    if(!updating && currentProfile()) { rgb[index]=value;val chosen=Color.rgb(rgb[0],rgb[1],rgb[2]);save(current().copy(color=chosen,alpha=rgb[3]));displayColor((chosen and 0xffffff) or (rgb[3] shl 24)) }
                }
            })
        }
        addView(widthRow,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.touchSize)))
        widthSlider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar:SeekBar)=Unit
            override fun onStopTrackingTouch(bar:SeekBar)=Unit
            override fun onProgressChanged(bar:SeekBar,value:Int,fromUser:Boolean) {
                if(!updating && selected.line && currentProfile()) save(current().copy(widthPercent=value))
            }
        })
        addView(surfaceHint)
        addView(lineLabel)
        line.adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,MapDash.entries.map { context.getString(it.title) })
        line.contentDescription=context.getString(R.string.map_style_line);addView(line,LayoutParams(-1,-2))
        fun selection(spinner:Spinner,action:(Int)->Unit) { spinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:AdapterView<*>?)=Unit
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long) { action(position) }
        } }
        selection(target) {
            selected=targets[it]
            if(currentProfile()) prefs.edit().putString(LAST_TARGET,selected.name).apply()
            render()
        }
        selection(line) { if(!updating && selected.line && !MapRoadSurface.target(selected)) {
            val value=current();val dash=MapDash.entries[it]
            if(value.dash!=dash) save(value.copy(dash=dash))
        } }
        addView(placeText,LayoutParams(-1,-2))
        addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;tag="map_style_actions";setTag(R.id.inline_control_row,true)
            fun button(icon:BicycleIcon,label:Int,key:String,click:()->Unit) {
                addView(BicycleComponents.icon(context,themes,BicycleIcons.drawable(context,icon,themes.active.tokens.foreground),context.getString(label),click=click).apply {
                    tag=key;tooltipText=contentDescription
                },LayoutParams(0,BicycleDesign.dp(context,BicycleDesign.touchSize),1f))
            }
            button(BicycleIcon.Reset,R.string.map_style_reset_object,"map_style_reset_object") {
                if(currentProfile()) { MapStyleOverrides.read(prefs).update(selected,null).write(prefs);render();schedule() }
            }
            button(BicycleIcon.DeleteTrack,R.string.map_style_reset_all,"map_style_reset_all") {
                if(currentProfile()) { MapStyleOverrides().write(prefs);render();schedule() }
            }
            button(BicycleIcon.ExportTrack,R.string.map_style_export,"map_style_export") {
                if(currentProfile()) export(MapStyleOverrides.read(prefs).export(prefs))
            }
            button(BicycleIcon.Cancel,R.string.analysis_close,"map_style_close",close)
        },LayoutParams(-1,-2))
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
        hex.text=if(Color.alpha(value)==255) String.format(java.util.Locale.ROOT,"#%06X",value and 0xffffff) else String.format(java.util.Locale.ROOT,"#%08X",value)
        swatch.background=BicycleComponents.controlBackground(context,themes.active.tokens,value)
    }
    private fun render() {
        updating=true
        placeText.render()
        val value=current();val original=MapStyleXml.defaultColor(base,selected)
        val initial=((value.color ?: original) and 0xffffff) or ((value.alpha ?: Color.alpha(original)) shl 24)
        rgb[0]=Color.red(initial);rgb[1]=Color.green(initial);rgb[2]=Color.blue(initial);rgb[3]=Color.alpha(initial)
        channels.forEachIndexed { index,slider -> slider.progress=rgb[index] }
        displayColor(initial)
        widthRow.visibility=if(selected.line) VISIBLE else GONE
        widthSlider.progress=value.widthPercent ?: 100
        surfaceHint.visibility=if(MapRoadSurface.target(selected)) VISIBLE else GONE
        lineLabel.visibility=if(selected.line && !MapRoadSurface.target(selected)) VISIBLE else GONE;line.visibility=lineLabel.visibility
        line.setSelection(value.dash.ordinal);updating=false
    }
    companion object { private const val LAST_TARGET="mapStyleEditorLastTarget" }
    override fun onDetachedFromWindow() {
        if(handler.hasCallbacks(preview)) { handler.removeCallbacks(preview);if(currentProfile()) changed() }
        super.onDetachedFromWindow()
    }
}
