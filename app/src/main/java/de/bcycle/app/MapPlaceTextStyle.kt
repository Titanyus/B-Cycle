package de.bcycle.app

internal enum class MapPlaceClass(val key:String,val title:Int,val defaultWeight:Int,val defaultSize:Int) {
    CITY("city",R.string.map_place_city,900,90),
    TOWN("town",R.string.map_place_town,900,90),
    VILLAGE("village",R.string.map_place_village,400,100),
    HAMLET("hamlet",R.string.map_place_hamlet,700,100);
    val defaults get()=MapPlaceTextStyle(defaultWeight,defaultSize)
}
internal data class MapPlaceTextStyle(val weight:Int,val sizePercent:Int)

internal object MapPlaceHalo { const val widthScale=2;const val opacity=1 }
