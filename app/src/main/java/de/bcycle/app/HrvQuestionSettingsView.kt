package de.bcycle.app

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.LinearLayout

/** Global person-level question selection. Hidden questions never delete historical answers. */
internal class HrvQuestionSettingsView(context:Context,themes:ThemeManager):LinearLayout(context) {
    private val settings=HrvQuestionSettings(context)
    private val riderPrefs=context.getSharedPreferences("bike",0)
    private var binding=false
    private val riderListener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,key->if(key==RiderProfileStore.key) refreshCycle() }
    private fun refreshCycle() {
        val cycle=findViewWithTag<CheckBox>("hrvq_enabled_CYCLE") ?: return
        binding=true
        cycle.isEnabled=settings.cycleAvailable()
        cycle.isChecked=cycle.isEnabled && HrvQuestion.CYCLE in settings.read().enabled
        binding=false
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();riderPrefs.registerOnSharedPreferenceChangeListener(riderListener);refreshCycle() }
    override fun onDetachedFromWindow() { riderPrefs.unregisterOnSharedPreferenceChangeListener(riderListener);super.onDetachedFromWindow() }
    init {
        orientation=VERTICAL;tag="hrvq_question_settings"
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrvq_settings_hint)))
        val groups=listOf(R.string.hrvq_sleep_group,R.string.hrvq_training_group,R.string.hrvq_overview_group,R.string.hrvq_lifestyle_group,R.string.hrvq_notes_group)
        groups.forEachIndexed { index,title ->
            addView(BicycleComponents.contentHeading(context,themes,context.getString(title)))
            HrvQuestion.entries.filter { it.group==index }.forEach { question ->
                addView(CheckBox(context).apply {
                    tag="hrvq_enabled_${question.name}";setText(question.label)
                    val available=question!=HrvQuestion.CYCLE || settings.cycleAvailable()
                    isChecked=available && question in settings.read().enabled;isEnabled=available
                    if(!available) contentDescription=context.getString(R.string.hrvq_cycle_locked)
                    BicycleDialogStyle.control(this,themes)
                    setOnCheckedChangeListener { _,on ->
                        if(binding) return@setOnCheckedChangeListener
                        val old=settings.read();settings.write(old.copy(enabled=if(on) old.enabled+question else old.enabled-question))
                    }
                },LayoutParams(-1,-2))
            }
        }
        addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.hrvq_custom_group)))
        addView(BicycleComponents.description(context,themes,context.getString(R.string.hrvq_custom_hint)))
        (0..2).forEach { index ->
            addView(BicycleComponents.input(context,themes).apply {
                tag="hrvq_custom_name_$index";hint=context.getString(R.string.hrvq_custom_name,index+1);contentDescription=hint
                typeface=android.graphics.Typeface.DEFAULT;textSize=BicycleDesign.bodySp;setSingleLine()
                filters=arrayOf(android.text.InputFilter.LengthFilter(40));setText(settings.read().customNames[index])
                addTextChangedListener(object:TextWatcher {
                    override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
                    override fun afterTextChanged(s:Editable?)=Unit
                    override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {
                        val text=s?.toString().orEmpty();if(text.any(Char::isISOControl)) return
                        val old=settings.read();settings.write(old.copy(customNames=old.customNames.toMutableList().also { it[index]=text.trim() }))
                    }
                })
            },LayoutParams(-1,-2))
        }
    }
}
