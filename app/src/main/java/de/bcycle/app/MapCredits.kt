package de.bcycle.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout

/** Provider/overlay metadata shared by every map's disclosure and the complete sources screen. */
internal data class MapCredit(val title:String,val url:String,val licenceUrl:String?=null)
internal object MapCredits {
    val osm=MapCredit("© OpenStreetMap-Mitwirkende · ODbL","https://www.openstreetmap.org/copyright")
    val openFreeMap=MapCredit("OpenFreeMap","https://openfreemap.org/")
    val openMapTiles=MapCredit("© OpenMapTiles · CC BY 4.0","https://openmaptiles.org/","https://creativecommons.org/licenses/by/4.0/")
    val terrain=MapCredit("© Mapterhorn · Höhendaten","https://mapterhorn.com/attribution")
    val cyclingRoutes=MapCredit("Waymarked Trails · Radrouten aus OpenStreetMap","https://cycling.waymarkedtrails.org/")
    fun base(source:MapSource):List<MapCredit> = when(source) {
        MapSource.VECTOR->listOf(osm,openFreeMap,openMapTiles)
        MapSource.OSM->listOf(osm)
        else->listOf(osm,MapCredit(source.attribution,source.creditUrl))
    }
    fun active(source:MapSource,layers:MapVectorLayers):List<MapCredit> =
        (base(source)+if(source==MapSource.VECTOR) MapVectorLayer.entries.filter { it in layers }.mapNotNull { it.credit } else emptyList()).distinctBy { it.url }
    fun all():List<MapCredit> = (MapSource.entries.flatMap(::base)+MapVectorLayer.entries.mapNotNull { it.credit }).distinctBy { it.url }
    fun append(body:LinearLayout,themes:ThemeManager,credits:List<MapCredit>) {
        val context=body.context
        for(credit in credits) body.addView(BicycleComponents.text(context,themes,listOfNotNull(credit.title,credit.url,credit.licenceUrl).joinToString("\n")).apply {
            android.text.util.Linkify.addLinks(this,android.text.util.Linkify.WEB_URLS)
            movementMethod=android.text.method.LinkMovementMethod.getInstance()
            BicycleComponents.bindTheme(this,themes) { setLinkTextColor(it.foreground) }
        },LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
    }
}

/** Native, bounded disclosure: no extra map/GL instance or persistent overlay is involved. */
internal class MapCreditsPopup(private val anchor:View,themes:ThemeManager,credits:List<MapCredit>):android.widget.PopupWindow() {
    init {
        val context=anchor.context
        isFocusable=true;isOutsideTouchable=true;inputMethodMode=INPUT_METHOD_NOT_NEEDED
        setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        val body=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL;tag="map_credits_popup"
            val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
            addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.sources_maps)))
        }
        MapCredits.append(body,themes,credits)
        BicycleComponents.bindTheme(body,themes) { body.background=BicycleComponents.controlBackground(context,it,it.tertiary) }
        contentView=android.widget.ScrollView(context).apply { BicycleComponents.scrollStyle(this);addView(body) }
        width=minOf(BicycleDesign.dp(context,340),context.resources.displayMetrics.widthPixels-BicycleDesign.dp(context,32))
        contentView.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((context.resources.displayMetrics.heightPixels*.6).toInt(),View.MeasureSpec.AT_MOST))
        height=contentView.measuredHeight
    }
    fun show() { showAtLocation(anchor,android.view.Gravity.BOTTOM or android.view.Gravity.START,BicycleDesign.dp(anchor.context,8),BicycleDesign.dp(anchor.context,24)) }
}
