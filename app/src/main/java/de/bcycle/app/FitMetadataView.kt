package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch

/** Same native accordion and switches as the remaining profile settings. */
internal class FitMetadataView(context:Context,themes:ThemeManager,profile:BikeProfile,selected:Set<FitMetadataField>,
    expanded:Boolean=false,onExpand:(Boolean)->Unit={},private val currentProfile:()->BikeProfile={profile},changed:(Set<FitMetadataField>)->Unit):LinearLayout(context) {
    private val profileSlot=profile.slot
    private val switches=linkedMapOf<FitMetadataField,Switch>()
    private val preferences=context.getSharedPreferences("bike",0)
    private val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,key ->
        if(key==BikeProfileStore.KEY) post { refreshValues() }
    }
    init {
        orientation=VERTICAL;tag="fit_metadata"
        val chosen=selected.toMutableSet()
        val frame=BicycleFrameDropdown(context,themes,context.getString(R.string.fit_metadata_title),open=expanded,
            leadingIcon=BicycleIcon.Info,changed=onExpand)
        frame.header.apply { tag="fit_metadata_toggle";isAccessibilityHeading=true }
        val content=frame.content.apply { tag="fit_metadata_content" }
        addView(frame,LayoutParams(-1,-2))
        FitMetadataField.entries.forEach { field ->
            content.addView(Switch(context).apply {
                setTag(R.id.compact_choice_role,true)
                tag="fit_metadata_${field.name}";switches[field]=this
                minHeight=BicycleDesign.dp(context,BicycleDesign.touchSize);isChecked=field in chosen
                setOnCheckedChangeListener { _,enabled -> if(enabled) chosen.add(field) else chosen.remove(field);changed(chosen.toSet()) }
            })
        }
        content.addView(BicycleComponents.description(context,themes,context.getString(R.string.fit_metadata_hint)))
        refreshValues()
        BicycleDialogStyle.content(this,themes)
    }
    private fun refreshValues() {
        val profile=currentProfile().takeIf { it.slot==profileSlot } ?: return
        val snapshot=FitMetadata(profile.name,profile.type,profile.slot)
        switches.forEach { (field,control) ->
            val label=when(field) {
                FitMetadataField.BIKE_NAME->R.string.fit_metadata_bike_name
                FitMetadataField.SPORT->R.string.fit_metadata_sport
                FitMetadataField.SUB_SPORT->R.string.fit_metadata_type
                FitMetadataField.PROFILE_ID->R.string.fit_metadata_id
                FitMetadataField.PROFILE_INDEX->R.string.fit_metadata_index
                FitMetadataField.RECORDER->R.string.fit_metadata_recorder
            }
            val value=when(field) {
                FitMetadataField.BIKE_NAME->profile.name
                FitMetadataField.SPORT->context.getString(R.string.fit_metadata_cycling)
                FitMetadataField.SUB_SPORT->context.getString(profile.type.label)
                FitMetadataField.PROFILE_ID->snapshot.profileId?.toString() ?: context.getString(R.string.analysis_missing)
                FitMetadataField.PROFILE_INDEX->snapshot.profileIndex?.toString() ?: context.getString(R.string.analysis_missing)
                FitMetadataField.RECORDER->"B|CYCLE"
            }
            val labelText=context.getString(R.string.fit_metadata_value,context.getString(label),value)
            if(control.text.toString()!=labelText) control.text=labelText
        }
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow();preferences.registerOnSharedPreferenceChangeListener(listener);refreshValues()
    }
    override fun onDetachedFromWindow() {
        preferences.unregisterOnSharedPreferenceChangeListener(listener);super.onDetachedFromWindow()
    }

}
