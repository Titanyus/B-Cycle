package de.bcycle.app

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout

/** Profile-scoped, autosaved name template; native multi-select segments share the zone style. */
internal class RideNamingView(context:Context,themes:ThemeManager,private val latestRide:()->RouteDetails?={null},private val canEdit:()->Boolean):LinearLayout(context) {
    private var refreshPreview:()->Unit={}
    fun refreshLock() {
        if(findViewWithTag<View>("ride_naming_content")?.visibility==VISIBLE) refreshPreview()
        val enabled=canEdit()
        findViewWithTag<android.view.View>("ride_naming_text")?.isEnabled=enabled
        RideNamePart.entries.forEach { findViewWithTag<android.view.View>("ride_name_${it.name}")?.isEnabled=enabled }
    }
    init {
        orientation=VERTICAL;tag="ride_naming"
        val settings=RideNamingSettings(context);var current=settings.read()
        val content=LinearLayout(context).apply { orientation=VERTICAL;visibility=GONE;tag="ride_naming_content" }
        lateinit var toggle:android.widget.Button
        toggle=BicycleComponents.action(context,themes,context.getString(R.string.ride_name_title)) {
            content.visibility=if(content.visibility==VISIBLE) GONE else VISIBLE
            if(content.visibility==VISIBLE) refreshPreview()
            toggle.stateDescription=context.getString(if(content.visibility==VISIBLE) R.string.zone_expanded else R.string.zone_collapsed)
        }.apply { tag="ride_naming_toggle";stateDescription=context.getString(R.string.zone_collapsed) }
        BicycleDialogStyle.accordion(toggle,themes);BicycleDialogStyle.actionIcon(toggle,themes,BicycleIcon.Expand)
        addView(toggle);addView(content)
        val input=BicycleComponents.input(RideNamingInput(context),themes).apply {
            BicycleDialogStyle.control(this,themes);typeface=android.graphics.Typeface.DEFAULT;textSize=BicycleDesign.bodySp
            tag="ride_naming_text";hint=context.getString(R.string.ride_name_text);contentDescription=hint
            setSingleLine(false);setHorizontallyScrolling(false);gravity=android.view.Gravity.TOP or android.view.Gravity.START
            setRawInputType(android.text.InputType.TYPE_CLASS_TEXT);imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            filters=arrayOf(android.text.InputFilter.LengthFilter(60));setText(current.text);isEnabled=canEdit()
        }
        refreshPreview={
            input.previewSuffix=rideNamingPreview(context,current,latestRide())
            input.hint=if(input.previewSuffix.isEmpty()) context.getString(R.string.ride_name_text) else null
            input.stateDescription=context.getString(R.string.ride_preview_accessibility,current.text+input.previewSuffix)
        }
        refreshPreview()
        content.addView(input,LayoutParams(-1,-2))
        input.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {
                val value=s?.toString().orEmpty()
                if(canEdit() && value.none(Char::isISOControl)) { current=current.copy(text=value);settings.write(current);refreshPreview() }
            }
            override fun afterTextChanged(s:Editable?)=Unit
        })
        val choices=LinearLayout(context).apply { orientation=HORIZONTAL;tag="ride_naming_parts";BicycleComponents.segmentedControlRow(this,themes) }
        RideNamePart.entries.forEach { part ->
            val button=BicycleSegmentButton(context,themes,multiple=true).apply {
                tag="ride_name_${part.name}";setText(part.label);textSize=BicycleDesign.analysisLabelSp
                isChecked=part in current.parts;isEnabled=canEdit()
                contentDescription=context.getString(part.label)
                stackedIcon(part.icon)
                setOnClickListener { if(canEdit()) {
                    current=current.copy(parts=if(isChecked) current.parts+part else current.parts-part);settings.write(current);refreshPreview()
                } }
            }
            choices.addView(button,LayoutParams(0,BicycleDesign.dp(context,72),1f))
        }
        content.addView(choices,LayoutParams(-1,-2))
    }
}
