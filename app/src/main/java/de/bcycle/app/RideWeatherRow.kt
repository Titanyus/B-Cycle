package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

internal object WeatherSky {
    fun label(code:Int?)=when(code) {
        0->R.string.weather_clear;1->R.string.weather_fair;2->R.string.weather_partly_cloudy;3->R.string.weather_cloudy
        45,48->R.string.weather_fog;51,53,55,56,57->R.string.weather_drizzle;61,63,65,66,67->R.string.weather_rain
        71,73,75,77,85,86->R.string.weather_snow;80,81,82->R.string.weather_showers;95,96,99->R.string.weather_storm;else->R.string.weather_unknown
    }
    fun icon(code:Int?)=when(code) { 0,1->BicycleIcon.Sun;2->BicycleIcon.Weather;51,53,55,56,57,61,63,65,66,67,80,81,82,95,96,99->BicycleIcon.Rain;else->BicycleIcon.Cloud }
}
internal class RideWeatherRow(context:Context,themes:ThemeManager,data:RideWeatherData):LinearLayout(context) {
    init {
        orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;tag="analysis_weather"
        fun part(text:String,icon:BicycleIcon,weight:Float) {
            if(text.isEmpty()) return
            addView(TextView(context).apply {
                this.text=text;contentDescription=text;gravity=Gravity.CENTER;maxLines=1
                textSize=BicycleDesign.analysisLabelSp
                setAutoSizeTextTypeUniformWithConfiguration(8,BicycleDesign.analysisLabelSp.toInt(),1,android.util.TypedValue.COMPLEX_UNIT_SP)
                compoundDrawablePadding=BicycleDesign.dp(context,2)
                BicycleComponents.bindTheme(this,themes) { tokens ->
                    setTextColor(tokens.foreground)
                    setCompoundDrawablesRelative(BicycleIcons.drawable(context,icon,tokens.foreground).apply {
                        val size=BicycleDesign.dp(context,14);setBounds(0,0,size,size)
                    },null,null,null)
                }
            },LayoutParams(0,-2,weight))
        }
        val temperature=data.temperature?.let { context.getString(R.string.weather_temperature,it) }
        part(listOfNotNull(data.code?.let { context.getString(WeatherSky.label(it)) },temperature).joinToString(" · "),WeatherSky.icon(data.code),1.5f)
        part(data.humidity?.let { context.getString(R.string.weather_humidity,it.toInt()) }.orEmpty(),BicycleIcon.Humidity,.8f)
        val direction=data.direction?.let { context.resources.getStringArray(R.array.weather_directions)[((it+11.25)/22.5).toInt()%16] }
        part(listOfNotNull(data.wind?.let { context.getString(R.string.weather_wind,it) },direction).joinToString(" "),BicycleIcon.Wind,1.2f)
        contentDescription=context.getString(R.string.weather_accessibility,
            listOfNotNull(data.code?.let { context.getString(WeatherSky.label(it)) },temperature,
                data.humidity?.let { context.getString(R.string.weather_humidity_accessibility,it.toInt()) },
                data.wind?.let { context.getString(R.string.weather_wind,it) },direction).joinToString(", "))
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
    }
}
