package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import java.util.Locale

/** Independent, non-selectable group row; never part of a ride card. */
internal class RouteListGroupHeader(context:Context,themes:ThemeManager):LinearLayout(context) {
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    private val title=BicycleComponents.text(context,themes,"").apply {
        BicycleComponents.headerTypography(this)
        gravity=Gravity.CENTER;isAccessibilityHeading=true;includeFontPadding=false;maxLines=1
        setAutoSizeTextTypeUniformWithConfiguration(16,BicycleDesign.headingSp.toInt(),1,android.util.TypedValue.COMPLEX_UNIT_SP)
        tag="route_group_title"
    }
    private val count=RouteListReading(context,themes,R.string.route_group_activities)
    private val distance=RouteListReading(context,themes,R.string.route_list_distance,R.string.analysis_km)
    private val moving=RouteListReading(context,themes,R.string.route_group_time,R.string.analysis_hours)
    private val average=RouteListReading(context,themes,R.string.route_list_average,R.string.analysis_kmh)
    init {
        orientation=VERTICAL;visibility=GONE
        BicycleComponents.background(this,themes,ThemeRole.TERTIARY)
        setPadding(dp(BicycleDesign.verticalPadding),dp(BicycleDesign.rowGap),dp(BicycleDesign.verticalPadding),dp(BicycleDesign.sectionDividerGap))
        // A heading is not a ride action. ListView can still intercept a vertical scroll here.
        setOnTouchListener { _,_->true }
        addView(title,LayoutParams(-1,-2))
        addView(LinearLayout(context).apply {
            orientation=HORIZONTAL;isBaselineAligned=false
            listOf(count,distance,moving,average).forEachIndexed { index,reading ->
                if(index>0) addView(BicycleComponents.divider(context,themes),LayoutParams(BicycleDesign.linePixels(context),-1))
                addView(reading,LayoutParams(0,-2,1f))
            }
        },LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
    }
    fun bind(group:RouteListGroup?) {
        visibility=if(group==null) GONE else VISIBLE
        tag=group?.let { "route_group_header_${it.first}" }
        if(group==null) {
            listOf(count,distance,moving,average).forEach { it.number.tag=null }
            return
        }
        title.text=group.label
        val key=group.first.toString()
        val missing=context.getString(R.string.analysis_missing)
        count.bind(group.count.toString(),key,"group_count")
        distance.bind(group.distanceM?.let { String.format(Locale.GERMANY,"%.1f",it/1000) } ?: missing,key,"group_distance")
        moving.bind(group.movingMs?.let {
            val minutes=(it/60000).toLong();String.format(Locale.GERMANY,"%d:%02d",minutes/60,minutes%60)
        } ?: missing,key,"group_moving")
        average.bind(group.averageKmh?.let { String.format(Locale.GERMANY,"%.1f",it) } ?: missing,key,"group_average")
        distance.unit.visibility=if(group.distanceM==null) GONE else VISIBLE
        moving.unit.visibility=if(group.movingMs==null) GONE else VISIBLE
        average.unit.visibility=if(group.averageKmh==null) GONE else VISIBLE
    }
}
