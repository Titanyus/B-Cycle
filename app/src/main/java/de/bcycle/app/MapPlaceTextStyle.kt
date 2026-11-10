package de.bcycle.app

internal enum class MapPlaceClass(val key:String,val title:Int,val defaultWeight:Int,val defaultSize:Int) {
    CITY("city",R.string.map_place_city,900,90),
    TOWN("town",R.string.map_place_town,900,90),
    VILLAGE("village",R.string.map_place_village,400,100),
    HAMLET("hamlet",R.string.map_place_hamlet,700,100),
    COUNTRY("country",R.string.map_text_country,700,100),
    CONTINENT("continent",R.string.map_text_continent,700,100),
    OTHER("other",R.string.map_text_other,400,100),
    ROAD("transportation_name",R.string.map_layer_road_names,400,100),
    WATER("water_name",R.string.map_style_water_text,400,100),
    HOUSE("housenumber",R.string.map_house_numbers,400,100);
    val defaults get()=MapPlaceTextStyle(defaultWeight,defaultSize)
}
internal data class MapPlaceTextStyle(val weight:Int,val sizePercent:Int)

internal object MapPlaceHalo { const val widthScale=2;const val opacity=1 }
