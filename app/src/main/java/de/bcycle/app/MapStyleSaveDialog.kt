package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.*

internal object MapStyleSaveDialog {
    fun show(context:Context,prefs:SharedPreferences,themes:ThemeManager,permitted:()->Boolean) {
        val all=MapStyleLibrary.read(prefs)
        val bright=all.firstOrNull { it.name.equals("Bright",true) }
        val styles=all.filterNot { it===bright }
        val active=prefs.getString(MapStyleLibrary.ACTIVE,null)
        val selected=styles.indexOfFirst { it.id==active }.let { if(it>=0) it+2 else 1 }
        fun selectedStyle(position:Int)=if(position==1) bright else styles.getOrNull(position-2)
        fun selectedName(position:Int)=if(position==1) context.getString(R.string.map_source_bright) else styles.getOrNull(position-2)?.name.orEmpty()
        val body=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL
            val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        }
        val choice=Spinner(context).apply {
            tag="map_style_save_choice"
            adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,
                listOf(context.getString(R.string.map_style_save_new),context.getString(R.string.map_source_bright))+styles.map { it.name })
            setSelection(selected)
        }
        val name=BicycleComponents.input(context,themes).apply {
            tag="map_style_save_name";setSingleLine();filters=arrayOf(android.text.InputFilter.LengthFilter(60))
            setText(selectedName(selected))
        }
        body.addView(choice,LinearLayout.LayoutParams(-1,-2))
        body.addView(BicycleComponents.fieldLabel(context,themes,context.getString(R.string.map_style_save_name)))
        body.addView(name,LinearLayout.LayoutParams(-1,-2))
        var choicePosition=selected
        choice.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:AdapterView<*>?)=Unit
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long) {
                if(position!=choicePosition) { choicePosition=position;name.setText(selectedName(position)) }
            }
        }
        lateinit var dialog:android.app.AlertDialog
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
            .setTitle(R.string.map_style_save).setView(body).show()
    }
}
