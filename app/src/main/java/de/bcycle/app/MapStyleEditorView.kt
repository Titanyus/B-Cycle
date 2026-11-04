package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*

/** Small on-map editor. Persist drafts per bike, preview with a bounded debounce, export a portable delta. */
internal class MapStyleEditorView(context:Context,private val prefs:SharedPreferences,private val themes:ThemeManager,
    private val changed:()->Unit,private val export:(String)->Unit):LinearLayout(context) {
    private val profile=prefs.getInt(BikeProfileStore.ACTIVE,0)
    private val base=context.assets.open("styles/bright-vtm.xml").bufferedReader().use { it.readText() }
    private val handler=Handler(Looper.getMainLooper())
    private val preview=Runnable { if(currentProfile()) changed() }
    private val target=Spinner(context).apply { tag="map_style_target" }
    private val color=BicycleFrameDropdown(context,themes,context.getString(R.string.map_style_color),open=false,leadingIcon=BicycleIcon.EditDashboard)
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
        addView(color,LayoutParams(-1,-2));addView(lineLabel)
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
    private fun schedule() { handler.removeCallbacks(preview);handler.postDelayed(preview,180) }
    private fun save(value:MapObjectStyle) {
        if(!currentProfile()) return
        MapStyleOverrides.read(prefs).update(selected,value).write(prefs);schedule()
    }
    private fun render() {
        updating=true
        val value=current();val initial=value.color ?: MapStyleXml.defaultColor(base,selected)
        color.header.text=context.getString(R.string.map_style_color)+" · "+String.format(java.util.Locale.ROOT,"#%06X",initial and 0xffffff)
        color.leadingIconColor=initial;color.content.removeAllViews()
        val editing=selected
        color.content.addView(FixedColorPalette(context,themes,initial) { chosen ->
            if(selected==editing && currentProfile()) {
                save(current().copy(color=chosen));color.leadingIconColor=chosen
                color.header.text=context.getString(R.string.map_style_color)+" · "+String.format(java.util.Locale.ROOT,"#%06X",chosen and 0xffffff)
            }
        },LayoutParams(-1,-2))
        lineLabel.visibility=if(selected.line) VISIBLE else GONE;line.visibility=lineLabel.visibility
        line.setSelection(value.dash.ordinal);updating=false
    }
    override fun onDetachedFromWindow() {
        if(handler.hasCallbacks(preview)) { handler.removeCallbacks(preview);if(currentProfile()) changed() }
        super.onDetachedFromWindow()
    }
}
