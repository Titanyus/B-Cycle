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
    private fun lines(available:Int):List<List<android.view.View>> {
        val rows=mutableListOf<MutableList<android.view.View>>()
        var used=0
        for(i in 0 until childCount) {
            val child=getChildAt(i)
            val current=rows.lastOrNull()
            val gap=current?.lastOrNull()?.let { (it.layoutParams as LayoutParams).marginEnd } ?: 0
            if(current==null || used+gap+child.measuredWidth>available) {
                rows.add(mutableListOf(child));used=child.measuredWidth
            } else { current.add(child);used+=gap+child.measuredWidth }
        }
        return rows
    }
    private fun lineWidth(row:List<android.view.View>)=row.sumOf { it.measuredWidth }+
        row.dropLast(1).sumOf { (it.layoutParams as LayoutParams).marginEnd }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        val available=(MeasureSpec.getSize(widthMeasureSpec)-paddingLeft-paddingRight).coerceAtLeast(1)
        for(i in 0 until childCount) getChildAt(i).measure(MeasureSpec.makeMeasureSpec(available,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED))
        val rows=lines(available)
        val height=rows.sumOf { row-> row.maxOf { it.measuredHeight } }+
            (rows.size-1).coerceAtLeast(0)*BicycleDesign.dp(context,BicycleDesign.smallGap)
        setMeasuredDimension(resolveSize((rows.maxOfOrNull(::lineWidth) ?: 0)+paddingLeft+paddingRight,widthMeasureSpec),resolveSize(height+paddingTop+paddingBottom,heightMeasureSpec))
    }
    override fun onLayout(changed:Boolean,left:Int,top:Int,right:Int,bottom:Int) {
        val available=(width-paddingLeft-paddingRight).coerceAtLeast(1)
        var y=paddingTop
        for(row in lines(available)) {
            val height=row.maxOf { it.measuredHeight }
            var x=paddingLeft+(available-lineWidth(row))/2
            for(child in row) {
                val cy=y+(height-child.measuredHeight)/2
                child.layout(x,cy,x+child.measuredWidth,cy+child.measuredHeight)
                x+=child.measuredWidth+(child.layoutParams as LayoutParams).marginEnd
            }
            y+=height+BicycleDesign.dp(context,BicycleDesign.smallGap)
        }
    }
    init {
        orientation=HORIZONTAL;gravity=Gravity.CENTER;tag="analysis_weather"
        fun part(text:String,icon:BicycleIcon) {
            if(text.isEmpty()) return
            val separator=childCount>0
            addView(LinearLayout(context).apply {
                gravity=Gravity.CENTER_VERTICAL
                if(separator) addView(BicycleComponents.text(context,themes,context.getString(R.string.weather_group_separator)).apply {
                    textSize=BicycleDesign.rowTitleSp;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                    tag="weather_group_separator"
                },LayoutParams(-2,-2).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
                addView(android.widget.ImageView(context).apply {
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                    BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,icon,it.foreground)) }
                },LayoutParams(BicycleDesign.dp(context,18),BicycleDesign.dp(context,18)).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.smallGap) })
                addView(BicycleComponents.text(context,themes,text).apply {
                    textSize=BicycleDesign.rowTitleSp;typeface=BicycleDesign.bodyTypeface
                },LayoutParams(-2,-2))
            },LayoutParams(-2,-2).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        }
        val temperature=data.temperature?.let { context.getString(R.string.weather_temperature,it) }
        part(listOfNotNull(data.code?.let { context.getString(WeatherSky.label(it)) },temperature).joinToString(" "),WeatherSky.icon(data.code))
        part(data.humidity?.let { context.getString(R.string.weather_humidity,it.toInt()) }.orEmpty(),BicycleIcon.Humidity)
        val direction=data.direction?.let { context.resources.getStringArray(R.array.weather_directions)[((it+11.25)/22.5).toInt()%16] }
        part(listOfNotNull(data.wind?.let { context.getString(R.string.weather_wind,it) },direction).joinToString(" "),BicycleIcon.Wind)
        contentDescription=context.getString(R.string.weather_accessibility,
            listOfNotNull(data.code?.let { context.getString(WeatherSky.label(it)) },temperature,
                data.humidity?.let { context.getString(R.string.weather_humidity_accessibility,it.toInt()) },
                data.wind?.let { context.getString(R.string.weather_wind,it) },direction).joinToString(", "))
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
    }
}
