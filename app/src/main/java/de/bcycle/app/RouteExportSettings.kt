package de.bcycle.app

import android.content.Context
import javax.crypto.SecretKey

internal data class RouteExportConfig(val save: Boolean = true, val tree: String = "", val webdav: Boolean = false,
    val server: String = "", val user: String = "", val folder: String = "B-Cycle", val wifiOnly: Boolean = true)

/** Password never enters the route index, jobs, diagnostics or Android backups. */
internal class RouteExportSettings(context: Context, private val secretKey: (() -> SecretKey)? = null) {
    private val prefs = context.getSharedPreferences("route-export", Context.MODE_PRIVATE)
    fun read() = RouteExportConfig(prefs.getBoolean("save", true), prefs.getString("tree", "")!!,
        prefs.getBoolean("webdav", false), prefs.getString("server", "")!!, prefs.getString("user", "")!!,
        prefs.getString("folder", "B-Cycle")!!, prefs.getBoolean("wifi", true))
    fun hasPassword() = prefs.contains("password") && !prefs.getBoolean("needsPassword", false)
    private val secret = StoredSecret(prefs,"bcycle-webdav",secretKey)
    fun password(): String = secret.read()
    fun backupConfigured()=runCatching {
        WebDavClient.directory(read());password().isNotEmpty()
    }.getOrDefault(false)
    fun write(config: RouteExportConfig, newPassword: String? = null) {
        val edit = prefs.edit().putBoolean("save", config.save).putString("tree", config.tree)
            .putBoolean("webdav", config.webdav).putString("server", config.server).putString("user", config.user)
            .putString("folder", config.folder).putBoolean("wifi", config.wifiOnly)
        if (newPassword != null) {
            edit.remove("needsPassword")
            edit.putString("password", secret.encode(newPassword))
        }
        check(edit.commit()) { "Einstellungen konnten nicht gespeichert werden" }
    }
}
