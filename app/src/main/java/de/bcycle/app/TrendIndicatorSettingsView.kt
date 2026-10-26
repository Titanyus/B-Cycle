package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.*

internal class TrendIndicatorSettingsView(context:Context,themes:ThemeManager,initial:TrendIndicatorConfig,
    changed:(TrendIndicatorConfig)->Unit):LinearLayout(context) {
    fun setDataAvailable(available:Boolean) {
        fun enable(view:View) {
            view.isEnabled=available
            if(view is android.view.ViewGroup) for(index in 0 until view.childCount) enable(view.getChildAt(index))
        }
        enable(this);alpha=if(available) 1f else .5f
        if(!available) {
            findViewWithTag<View>("field_trend_details").visibility=GONE
            findViewWithTag<View>("field_trend_expand").apply { rotation=0f;stateDescription=context.getString(R.string.zone_collapsed) }
        }
    }
    init {
        orientation=VERTICAL;tag="trend_indicator_settings"
        var config=initial
        val toggle=Switch(context).apply {
            tag="field_trend_indicator";setText(R.string.field_trend_indicator);isChecked=config.enabled
            BicycleDialogStyle.control(this,themes)
        }
        val details=LinearLayout(context).apply { orientation=VERTICAL;tag="field_trend_details";visibility=GONE }
        val expand=BicycleComponents.icon(context,themes,BicycleIcon.Expand,context.getString(R.string.field_trend_indicator)) {
            val open=details.visibility!=VISIBLE
            details.visibility=if(open) VISIBLE else GONE
            findViewWithTag<View>("field_trend_expand").apply {
                rotation=if(open) 180f else 0f
                stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
            }
        }.apply { tag="field_trend_expand";stateDescription=context.getString(R.string.zone_collapsed) }
        addView(LinearLayout(context).apply { BicycleComponents.switchAccordion(this,toggle,expand,themes) },
            LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.sectionHeaderMinHeight)))
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
        addView(details,LayoutParams(-1,-2))
    }
}
