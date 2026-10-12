package de.bcycle.app

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Receipts for completed external saves, never mere presence of an app-internal original. */
internal object RouteLocalSaves {
    private val lock = Any()
    private fun prefs(context: Context) = context.getSharedPreferences("route-local-saves", Context.MODE_PRIVATE)
    private fun entries(context: Context) = JSONObject(prefs(context).getString("entries", "{}")!!)
    private fun store(context: Context, value: JSONObject) {
        check(prefs(context).edit().putString("entries", value.toString()).putLong("revision", System.nanoTime()).commit())
    }
    fun revision(context: Context) = prefs(context).getLong("revision", 0)
    fun completed(context: Context, id: String, stamp: RouteFileStamp?): Boolean = synchronized(lock) {
        val item = entries(context).optJSONObject(id) ?: return@synchronized false
        stamp != null && item.optString("hash").isNotEmpty() && item.optLong("length", -1) == stamp.length &&
            item.optLong("modified", -1) == stamp.modified && (stamp.hash == null || item.optString("hash") == stamp.hash)
    }
    fun forget(context: Context, id: String) = synchronized(lock) { val all = entries(context); all.remove(id); store(context, all) }
    fun perform(context: Context, id: String, original: File, destination: String,
                cancelled: () -> Boolean = { false }, write: () -> Unit) {
        forget(context, id)
        if (cancelled()) throw IOException(context.getString(R.string.route_save_cancelled))
        val stamp = RouteFileStamp(original.length(), original.lastModified(), WebDavClient.hash(original))
        write()
        if (cancelled()) throw IOException(context.getString(R.string.route_save_cancelled))
        check(original.length() == stamp.length && original.lastModified() == stamp.modified && WebDavClient.hash(original) == stamp.hash) { context.getString(R.string.route_source_changed) }
        synchronized(lock) {
            val all = entries(context)
            all.put(id, JSONObject().put("hash", stamp.hash).put("length", stamp.length).put("modified", stamp.modified).put("destination", destination))
            store(context, all)
        }
    }
}
