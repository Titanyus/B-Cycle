package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.*

internal object MapStyleSaveDialog {
    fun show(context:Context,prefs:SharedPreferences,themes:ThemeManager,permitted:()->Boolean) {
        val styles=MapStyleLibrary.read(prefs)
        val selected=styles.indexOfFirst { it.id==prefs.getString(MapStyleLibrary.ACTIVE,null) }+1
        val body=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL
            val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        }
        val choice=Spinner(context).apply {
            tag="map_style_save_choice"
            adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,listOf(context.getString(R.string.map_style_save_new))+styles.map { it.name })
            setSelection(selected)
        }
        val name=BicycleComponents.input(context,themes).apply {
            tag="map_style_save_name";setSingleLine();filters=arrayOf(android.text.InputFilter.LengthFilter(60))
            setText(styles.getOrNull(selected-1)?.name.orEmpty())
        }
        body.addView(choice,LinearLayout.LayoutParams(-1,-2))
        body.addView(BicycleComponents.fieldLabel(context,themes,context.getString(R.string.map_style_save_name)))
        body.addView(name,LinearLayout.LayoutParams(-1,-2))
        choice.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:AdapterView<*>?)=Unit
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long) { name.setText(styles.getOrNull(position-1)?.name.orEmpty()) }
        }
        val dialog=BicycleDialogBuilder(context,themes,DialogNavigation(),fullscreen=false)
            .setTitle(R.string.map_style_save).setView(body).setNegativeButton(R.string.route_cancel,null).setPositiveButton(R.string.route_action_save,null).show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if(!permitted()) { dialog.dismiss();return@setOnClickListener }
            runCatching { MapStyleLibrary.save(prefs,name.text.toString(),styles.getOrNull(choice.selectedItemPosition-1)?.id) }
                .onSuccess { BicycleFeedback.show(context,R.string.map_style_saved);dialog.dismiss() }
                .onFailure { name.error=context.getString(R.string.map_style_save_error) }
        }
    }
}
