package de.bcycle.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal enum class RideService { INTERVALS, STRAVA, DREEVE }

/** A confirmed remote activity, independent of the current file's upload/queue state. */
internal data class RideServiceLink(
    val rideId: String,
    val service: RideService,
    val accountId: String,
    val activityId: String,
    val fileHash: String
)

/** Immutable-original FIT IDs remain the local identity across renames and corrected versions.
 * Keep every confirmed remote ID: uploading a new version must not erase the old association.
 * No credentials, tokens, or user-visible ride names belong in this store.
 */
internal object RideServiceLinks {
    private val lock = Any()
    private fun prefs(c: Context) = c.getSharedPreferences("ride-service-links", Context.MODE_PRIVATE)
    private fun entries(c: Context) = JSONObject(prefs(c).getString("entries", "{}")!!)
    private fun save(c: Context, all: JSONObject) {
        check(prefs(c).edit().putString("entries", all.toString()).commit())
    }

    fun links(c: Context, rideId: String): List<RideServiceLink> = synchronized(lock) {
        migrateIntervals(c, rideId)
        decode(entries(c), rideId)
    }

    fun confirm(c: Context, link: RideServiceLink) = synchronized(lock) {
        require(link.rideId.matches(Regex("(recorded|imported)-[a-f0-9]{64}")))
        require(link.fileHash.matches(Regex("[a-f0-9]{64}")))
        require(listOf(link.accountId, link.activityId).all { it.isNotBlank() && it.length <= 512 && it.none(Char::isISOControl) })
        val all = entries(c)
        // A remote activity may never silently become associated with another local ride.
        all.keys().forEach { id ->
            require(decode(all, id).none {
                it.service == link.service && it.accountId == link.accountId &&
                    it.activityId == link.activityId && it.rideId != link.rideId
            })
        }
        if (link in decode(all, link.rideId)) return@synchronized
        val items = all.optJSONArray(link.rideId) ?: JSONArray()
        items.put(JSONObject().put("service", link.service.name).put("account", link.accountId)
            .put("activity", link.activityId).put("hash", link.fileHash))
        all.put(link.rideId, items)
        save(c, all)
    }

    fun forget(c: Context, rideId: String) = synchronized(lock) {
        val all = entries(c)
        all.remove(rideId)
        save(c, all)
    }

    private fun decode(all: JSONObject, id: String): List<RideServiceLink> {
        val items = all.optJSONArray(id) ?: return emptyList()
        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            RideServiceLink(id, RideService.valueOf(item.getString("service")), item.getString("account"),
                item.getString("activity"), item.getString("hash"))
        }
    }

    // Preserve existing confirmed intervals.icu IDs before replacing an older upload receipt.
    private fun migrateIntervals(c: Context, id: String) {
        val old = JSONObject(c.getSharedPreferences("intervals-uploads", 0).getString("entries", "{}")!!)
            .optJSONObject(id) ?: return
        if (old.optString("state") == "saved" && old.optString("remoteId").isNotBlank())
            confirm(c, RideServiceLink(id, RideService.INTERVALS, old.getString("athlete"),
                old.getString("remoteId"), old.getString("hash")))
    }
}
