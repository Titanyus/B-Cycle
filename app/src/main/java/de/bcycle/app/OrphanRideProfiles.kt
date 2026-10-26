package de.bcycle.app

import android.content.Context
import org.json.JSONObject

internal data class OrphanProfileGroup(val key:String,val name:String?,val type:BikeType?,val rides:List<ManagedRoute>)

/** Detection only. Display names are hints, never identities or permission to change a ride. */
internal object OrphanRideProfiles {
    fun find(context:Context,entries:List<ManagedRoute>):List<OrphanProfileGroup> {
        val catalog=RideProfileColor.catalog(context)
        val slots=BikeProfileStore(context,context.getSharedPreferences("bike",0)).all().map { it.slot }.toSet()
        val orphaned=entries.filter { it.origin==TrackOrigin.RECORDED && catalog.values[it.id]?.slot !in slots }
        fun key(ride:ManagedRoute)=catalog.raw[ride.id]?.let { runCatching { JSONObject(it).optString("bikeId") }.getOrNull() }
            ?.takeIf(BikeIdentities::valid) ?: "unassigned"
        return orphaned.groupBy(::key).map { (key,rides) ->
            val types=rides.mapNotNull { catalog.values[it.id]?.type ?: it.details.bikeType }.distinct()
            OrphanProfileGroup(key,if(key=="unassigned") null else rides.firstNotNullOfOrNull { catalog.values[it.id]?.name },types.singleOrNull(),rides)
        }
    }
    fun suggestion(group:OrphanProfileGroup,profiles:List<BikeProfile>):BikeProfile? =
        if(group.name==null || group.type==null) null else profiles.singleOrNull {
            it.type==group.type && it.name.trim().equals(group.name.trim(),ignoreCase=true)
        }
}
