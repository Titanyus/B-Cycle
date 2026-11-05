package de.bcycle.app

/** Missing/ambiguous data stays solid; only explicit unsealed surfaces receive road dashes. */
internal object MapRoadSurface {
    const val policy="unpaved-dashed"
    const val dashLength=12
    const val gapLength=8
    private val unsealed=setOf("unpaved","unsealed","gravel","fine_gravel","compacted","ground","dirt","earth","grass","sand","mud","woodchips","pebblestone","rock","stones")
    fun unpaved(surface:String?)=surface?.trim()?.lowercase(java.util.Locale.ROOT) in unsealed
    fun roadId(id:String)=id.startsWith("highway-") || id.startsWith("bridge-") && !id.contains("railway") ||
        id.startsWith("tunnel-") && !id.contains("railway") || id.startsWith("bcycle-track") || id=="bcycle-cycleway" || id=="road_pier"
    fun target(target:MapStyleTarget)=target in setOf(MapStyleTarget.MOTORWAY,MapStyleTarget.PRIMARY,MapStyleTarget.SECONDARY,MapStyleTarget.MINOR,MapStyleTarget.PATH,MapStyleTarget.TRACK,MapStyleTarget.CYCLE)
}
