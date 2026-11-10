package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*

/** Compact on-map editor: fill left, road outline right. No extra geometry work for colour drags. */
internal class MapStyleEditorView(context:Context,private val prefs:SharedPreferences,private val themes:ThemeManager,
    private val changed:()->Unit,private val export:(String)->Unit,private val close:()->Unit={}):LinearLayout(context) {
    private val profile=prefs.getInt(BikeProfileStore.ACTIVE,0)
    private val base=context.assets.open("styles/bright-vtm.xml").bufferedReader().use { it.readText() }
    private val originals=mutableMapOf<Pair<MapStyleTarget,Boolean>,Int>()
    private val handler=Handler(Looper.getMainLooper())
    private val preview=Runnable { if(currentProfile()) changed() }
    private val targets=MapStyleTarget.entries.filterNot { it in setOf(MapStyleTarget.CYCLE,MapStyleTarget.CYCLE_ROUTES) }
    private val target=Spinner(context).apply { tag="map_style_target" }
    private val swatch=View(context).apply { tag="map_style_swatch" }
    private val outlineSwatch=View(context).apply { tag="map_style_outline_swatch" }
    private val rgb=IntArray(4)
    private val outlineRgb=IntArray(4)
    private val channelNames=listOf(R.string.map_style_red,R.string.map_style_green,R.string.map_style_blue,R.string.map_style_alpha)
    private val channelColors=listOf(Color.RED,Color.GREEN,Color.BLUE,Color.WHITE)
    private fun channels(outline:Boolean)=Array(4) { index -> BicycleSettingSlider(context,themes,Int::toString).apply {
        tag=if(outline) "map_style_outline_rgb_$index" else "map_style_rgb_$index";max=255;min=0;accentColor=channelColors[index]
        contentDescription=context.getString(channelNames[index])+(if(outline) " · Outline" else "")
    } }
    private val channels=channels(false)
    private val outlineChannels=channels(true)
    private fun width(outline:Boolean)=BicycleSettingSlider(context,themes) { "$it %" }.apply {
        tag=if(outline) "map_style_outline_width" else "map_style_width";min=25;max=300
        contentDescription=context.getString(R.string.map_style_width)+(if(outline) " · Outline" else "")
    }
    private val widthSlider=width(false)
    private val outlineWidth=width(true)
    private val outlineColumn=LinearLayout(context).apply { orientation=VERTICAL;tag="map_style_outline_column" }
    private val placeText=MapPlaceTextEditor(context,prefs,themes) { schedule() }
    private val line=Spinner(context).apply { tag="map_style_dash" }
    private val lineLabel=BicycleComponents.fieldLabel(context,themes,context.getString(R.string.map_style_line))
    private var selected=runCatching { MapStyleTarget.valueOf(prefs.getString(LAST_TARGET,null) ?: "BACKGROUND") }.getOrDefault(MapStyleTarget.BACKGROUND).takeIf { it in targets } ?: MapStyleTarget.BACKGROUND
    private var updating=false
    init {
        orientation=VERTICAL;tag="map_style_editor"
        target.adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,targets.map { context.getString(it.title) })
        target.setSelection(targets.indexOf(selected));target.contentDescription=context.getString(R.string.map_style_object)
        addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setTag(R.id.inline_control_row,true)
            addView(target,LayoutParams(0,-2,1f))
            for(field in listOf(swatch,outlineSwatch)) addView(field,LayoutParams(BicycleDesign.dp(context,32),BicycleDesign.dp(context,32)).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        },LayoutParams(-1,-2))
        val pitch=BicycleDesign.dp(context,(BicycleDesign.touchSize*.8f).toInt())
        val fillColumn=LinearLayout(context).apply { orientation=VERTICAL;tag="map_style_fill_column" }
        fun column(body:LinearLayout,sliders:Array<BicycleSettingSlider>,width:BicycleSettingSlider,outline:Boolean) {
            sliders.forEachIndexed { index,slider ->
                body.addView(slider,LayoutParams(-1,pitch))
                slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(bar:SeekBar)=Unit
                    override fun onStopTrackingTouch(bar:SeekBar)=Unit
                    override fun onProgressChanged(bar:SeekBar,value:Int,fromUser:Boolean) {
                        if(updating || !currentProfile()) return
                        val values=if(outline) outlineRgb else rgb;values[index]=value
                        val color=Color.rgb(values[0],values[1],values[2])
                        val next=if(outline) current().copy(outline=(current().outline ?: MapOutlineStyle()).copy(color=color,alpha=values[3]))
                            else current().copy(color=color,alpha=values[3])
                        save(next);displayColor(Color.argb(values[3],values[0],values[1],values[2]),outline)
                    }
                })
            }
            body.addView(width,LayoutParams(-1,pitch))
            width.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,value:Int,fromUser:Boolean) {
                    if(updating || (!selected.line && !(outline && selected.outlined)) || !currentProfile()) return
                    val old=current()
                    val next=if(outline) old.copy(outline=(old.outline ?: MapOutlineStyle()).copy(widthPercent=value))
                        else old.copy(widthPercent=value,outline=if(selected.outlined)
                            (old.outline ?: MapOutlineStyle()).let { it.copy(widthPercent=it.widthPercent ?: old.widthPercent ?: 100) } else old.outline)
                    save(next)
                }
            })
        }
        column(fillColumn,channels,widthSlider,false);column(outlineColumn,outlineChannels,outlineWidth,true)
        addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;isBaselineAligned=false;tag="map_style_columns"
            addView(fillColumn,LayoutParams(0,-2,1f))
            addView(outlineColumn,LayoutParams(0,-2,1f).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        },LayoutParams(-1,-2))
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
            button(BicycleIcon.Save,R.string.map_style_save,"map_style_save") {
                if(currentProfile()) MapStyleSaveDialog.show(context,prefs,themes,::currentProfile)
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
        val previous=MapStyleOverrides.read(prefs);val next=previous.update(selected,value)
        next.write(prefs);schedule(live=next.canUpdateColorsFrom(previous))
    }
    private fun displayColor(value:Int,outline:Boolean) {
        val field=if(outline) outlineSwatch else swatch
        field.background=BicycleComponents.controlBackground(context,themes.active.tokens,value)
        field.contentDescription=String.format(java.util.Locale.ROOT,"#%08X",value)
    }
    private fun render() {
        updating=true;placeText.render()
        val value=current()
        fun colour(outline:Boolean) {
            val original=originals.getOrPut(selected to outline) { MapStyleXml.defaultColor(base,selected,outline) }
            val color=if(outline) value.outline?.color else value.color
            val alpha=if(outline) value.outline?.alpha else value.alpha
            val initial=((color ?: original) and 0xffffff) or ((alpha ?: Color.alpha(original)) shl 24)
            val values=if(outline) outlineRgb else rgb;val sliders=if(outline) outlineChannels else channels
            values[0]=Color.red(initial);values[1]=Color.green(initial);values[2]=Color.blue(initial);values[3]=Color.alpha(initial)
            sliders.forEachIndexed { i,slider -> slider.progress=values[i] };displayColor(initial,outline)
        }
        colour(false)
        outlineColumn.visibility=if(selected.outlined) VISIBLE else GONE;outlineSwatch.visibility=outlineColumn.visibility
        if(selected.outlined) colour(true)
        widthSlider.visibility=if(selected.line) VISIBLE else GONE;widthSlider.progress=value.widthPercent ?: 100
        outlineWidth.progress=value.outline?.widthPercent ?: value.widthPercent ?: 100
        lineLabel.visibility=if(selected.line && !MapRoadSurface.target(selected)) VISIBLE else GONE;line.visibility=lineLabel.visibility
        line.setSelection(value.dash.ordinal);updating=false
    }
    companion object { private const val LAST_TARGET="mapStyleEditorLastTarget" }
    override fun onDetachedFromWindow() {
        if(handler.hasCallbacks(preview)) { handler.removeCallbacks(preview);if(currentProfile()) changed() }
        super.onDetachedFromWindow()
    }
}
