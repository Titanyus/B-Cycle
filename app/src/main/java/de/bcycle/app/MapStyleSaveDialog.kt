package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.*

internal object MapStyleSaveDialog {
    fun show(context:Context,prefs:SharedPreferences,themes:ThemeManager,permitted:()->Boolean) = show(context,prefs,themes,permitted,null)
    private fun show(context:Context,prefs:SharedPreferences,themes:ThemeManager,permitted:()->Boolean,selection:String?) {
        val all=MapStyleLibrary.read(prefs)
        val styles=all
        val active=selection ?: prefs.getString(MapStyleLibrary.ACTIVE,null)
        val selected=styles.indexOfFirst { it.id==active }.let { if(it>=0) it+1 else if(selection!=null) 0 else styles.indexOfFirst { it.name.equals("Bright",true) }+1 }
        fun selectedStyle(position:Int)=styles.getOrNull(position-1)
        fun selectedName(position:Int)=if(position==0) "" else selectedStyle(position)?.name.orEmpty()
        val body=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL
            val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        }
        val choice=Spinner(context).apply {
            tag="map_style_save_choice"
            adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,
                listOf(context.getString(R.string.map_style_save_new))+styles.map { it.name })
            setSelection(selected)
        }
        val name=BicycleComponents.input(context,themes).apply {
            tag="map_style_save_name";setSingleLine();filters=arrayOf(android.text.InputFilter.LengthFilter(60))
            setText(selectedName(selected))
        }
        body.addView(choice,LinearLayout.LayoutParams(-1,-2))
        body.addView(BicycleComponents.fieldLabel(context,themes,context.getString(R.string.map_style_save_name)))
        body.addView(name,LinearLayout.LayoutParams(-1,-2))
        val management=mutableListOf<View>()
        var choicePosition=selected
        choice.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:AdapterView<*>?)=Unit
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long) {
                if(position!=choicePosition) { choicePosition=position;name.setText(selectedName(position));management.forEach { it.isEnabled=selectedStyle(position)!=null } }
            }
        }
        lateinit var dialog:android.app.AlertDialog
        body.addView(LinearLayout(context).apply {
            orientation=LinearLayout.HORIZONTAL
            fun manage(text:String,key:String,action:(String)->Unit) {
                addView(BicycleComponents.action(context,themes,text,false) {
                    if(permitted()) selectedStyle(choice.selectedItemPosition)?.let { action(it.id);dialog.dismiss();show(context,prefs,themes,permitted,it.id) }
                }.apply { tag=key;isEnabled=selectedStyle(selected)!=null;management+=this },LinearLayout.LayoutParams(0,-2,1f))
            }
            manage("↑","map_style_move_up") { MapStyleLibrary.move(prefs,it,-1) }
            manage("↓","map_style_move_down") { MapStyleLibrary.move(prefs,it,1) }
            manage(context.getString(R.string.route_action_delete),"map_style_delete") { MapStyleLibrary.delete(prefs,it) }
        },LinearLayout.LayoutParams(-1,-2))
        body.addView(LinearLayout(context).apply {
            orientation=LinearLayout.HORIZONTAL;setTag(R.id.inline_control_row,true)
            addView(BicycleComponents.action(context,themes,context.getString(R.string.route_cancel),false) { dialog.dismiss() }.apply {
                tag="map_style_save_cancel"
            },LinearLayout.LayoutParams(0,-2,1f))
            addView(BicycleComponents.action(context,themes,context.getString(R.string.route_action_save),false) {
                if(!permitted()) dialog.dismiss()
                else runCatching { MapStyleLibrary.save(prefs,name.text.toString(),selectedStyle(choice.selectedItemPosition)?.id) }
                    .onSuccess { BicycleFeedback.show(context,R.string.map_style_saved);dialog.dismiss() }
                    .onFailure { name.error=context.getString(R.string.map_style_save_error) }
            }.apply { tag="map_style_save_confirm" },LinearLayout.LayoutParams(0,-2,1f).apply {
                marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap)
            })
        },LinearLayout.LayoutParams(-1,-2))
        dialog=BicycleDialogBuilder(context,themes,DialogNavigation(),fullscreen=false)
            .setTitle(R.string.map_style_save).setView(ScrollView(context).apply { BicycleComponents.scrollStyle(this);addView(body) }).show()
    }
}
