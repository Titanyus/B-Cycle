package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.*

internal class TrendIndicatorSettingsView(context:Context,themes:ThemeManager,initial:TrendIndicatorConfig,
    changed:(TrendIndicatorConfig)->Unit):LinearLayout(context) {
    private val frame:BicycleFrameDropdown
    fun setDataAvailable(available:Boolean) {
        fun enable(view:View) {
            view.isEnabled=available
            if(view is android.view.ViewGroup) for(index in 0 until view.childCount) enable(view.getChildAt(index))
        }
        enable(this);alpha=if(available) 1f else .5f
        if(!available) frame.setExpanded(false)
    }
    init {
        orientation=VERTICAL;tag="trend_indicator_settings"
        var config=initial
        val toggle=Switch(context).apply {
            tag="field_trend_indicator";setText(R.string.field_trend_indicator);isChecked=config.enabled
            BicycleDialogStyle.control(this,themes)
        }
        frame=BicycleFrameDropdown(context,themes,context.getString(R.string.field_trend_indicator),open=false,
            trailing=toggle,leadingIcon=BicycleIcon.HeadingUp)
        frame.expandControl.tag="field_trend_expand"
        val details=frame.content.apply { tag="field_trend_details" }
        addView(frame,LayoutParams(-1,-2))
        listOf(R.string.trend_current_values to AverageTrends.currentFields,
            R.string.trend_average_values to AverageTrends.averageFields).forEach { (heading,fields) ->
            BicycleComponents.listCategory(details,themes,context.getString(heading))
            fields.forEach { id ->
                val field=DashboardField(id)
                details.addView(CheckBox(context).apply {
                    tag="field_trend_$id"
                    text=context.getString(R.string.ui_source_field_description,
                        DashboardTextResources.source(context,field.source(LiveData())),field.title(context,LiveData()))
                    isChecked=id !in config.disabled
                    BicycleDialogStyle.control(this,themes)
                    setOnCheckedChangeListener { _,on -> config=config.select(id,on);changed(config) }
                },LayoutParams(-1,-2))
            }
        }
        toggle.setOnCheckedChangeListener { _,on -> config=config.copy(enabled=on);changed(config) }
    }
}
