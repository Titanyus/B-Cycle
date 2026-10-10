package de.bcycle.app

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Durable upload queue, using Android's scheduler instead of another runtime library. */
internal object RouteUploads {
    private const val JOB = 8301
    private val lock = Any()
    private val runLock = Any()
    private var statusCacheKey = ""
    private var statusCache = JSONObject()
    private fun prefs(context: Context) = context.getSharedPreferences("route-uploads", Context.MODE_PRIVATE)
    private fun entries(context: Context) = JSONObject(prefs(context).getString("entries", "{}")!!)
    private fun save(context: Context, entries: JSONObject) {
        check(prefs(context).edit().putString("entries", entries.toString()).putLong("revision", System.nanoTime()).commit())
    }
    fun revision(context: Context) = prefs(context).getLong("revision", 0)
    fun status(context: Context, id: String): String = synchronized(lock) {
        val key = context.filesDir.path + ":" + revision(context)
        if (statusCacheKey != key) { statusCache = entries(context); statusCacheKey = key }
        val item = statusCache.optJSONObject(id) ?: return@synchronized ""
        when (item.optString("state")) {
            "saved" -> "Nextcloud gesichert"
            "failed" -> "Upload fehlgeschlagen · ${item.optString("error")}"
            "uploading" -> "Nextcloud-Upload läuft …"
            else -> if (RouteExportSettings(context).read().webdav) "Nextcloud-Upload ausstehend" +
                item.optString("error").takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty() else "Upload pausiert"
        }
    }
    private fun target(config: RouteExportConfig) = WebDavClient.directory(config).toString()
    fun enqueue(context: Context, file: File, name: String) {
        val config = RouteExportSettings(context).read()
        if (!config.webdav) return
        synchronized(lock) {
            val all = entries(context)
            val id = file.nameWithoutExtension
            val destination = target(config)
            val existing = all.optJSONObject(id)
            if (existing?.optString("target") == destination && existing.optString("state") == "saved") return
            all.put(id, JSONObject().put("name", existing?.optString("name")?.takeIf { it.isNotEmpty() } ?: name).put("target", destination).put("state", "pending"))
            save(context, all)
        }
        schedule(context)
    }
    fun retry(context: Context, id: String) {
        val config = RouteExportSettings(context).read()
        check(config.webdav) { "WebDAV bitte in den Streckeneinstellungen aktivieren" }
        val file = File(context.filesDir, "fit-library/$id.fit")
        require(id.matches(Regex("(recorded|imported)-[a-f0-9]{64}")) && file.isFile)
        synchronized(lock) {
            val all = entries(context)
            val name = all.optJSONObject(id)?.optString("name")?.takeIf { it.isNotEmpty() } ?: "B-Cycle-$id.fit"
            all.put(id, JSONObject().put("name", name).put("target", target(config)).put("state", "pending"))
            save(context, all)
        }
        schedule(context)
    }
    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        val config = RouteExportSettings(context).read()
        val pending = synchronized(lock) { val all = entries(context); all.keys().asSequence().any {
            all.getJSONObject(it).optString("state") in setOf("pending", "uploading") } }
        if (!config.webdav || !pending) { scheduler.cancel(JOB); return }
        // Replacing a running job interrupts it. Reuse it unless settings explicitly cancelled it.
        if (scheduler.getPendingJob(JOB) != null) return
        scheduler.schedule(JobInfo.Builder(JOB, ComponentName(context, RouteUploadService::class.java))
            .setRequiredNetworkType(if (config.wifiOnly) JobInfo.NETWORK_TYPE_UNMETERED else JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true).setBackoffCriteria(60000, JobInfo.BACKOFF_POLICY_EXPONENTIAL).build())
    }
    fun settingsChanged(context: Context) {
        context.getSystemService(JobScheduler::class.java).cancel(JOB)
        // Authentication/configuration failures become eligible after the user changes settings.
        synchronized(lock) { val all = entries(context); all.keys().forEach { id ->
            val item = all.getJSONObject(id)
            if (item.optString("state") == "failed" && item.optString("target") == runCatching { target(RouteExportSettings(context).read()) }.getOrNull())
                item.put("state", "pending")
        }; save(context, all) }
        schedule(context)
    }
    fun run(context: Context, cancelled: () -> Boolean, upload: (RouteExportConfig, File, String, () -> Boolean) -> Unit = { config, file, name, stopped ->
        WebDavClient(config, RouteExportSettings(context).password(), cancelled = stopped).upload(file, name)
    }): Boolean = synchronized(runLock) {
        val config = RouteExportSettings(context).read()
        if (!config.webdav || cancelled()) return false
        if (config.wifiOnly) {
            val network = context.getSystemService(ConnectivityManager::class.java)
            if (network.getNetworkCapabilities(network.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return true
        }
        val ids = synchronized(lock) { val all = entries(context); all.keys().asSequence().filter {
            all.getJSONObject(it).optString("state") in setOf("pending", "uploading") }.toList() }
        var retry = false
        for (id in ids) {
            if (cancelled()) return true
            val item = synchronized(lock) { entries(context).getJSONObject(id) }
            val file = File(context.filesDir, "fit-library/$id.fit")
            try {
                check(id.matches(Regex("(recorded|imported)-[a-f0-9]{64}")) && file.isFile) { "Lokale FIT-Datei fehlt" }
                check(item.getString("target") == target(config)) { "Ziel geändert · Upload erneut anstoßen" }
                update(context, id, "uploading")
                upload(config, file, item.getString("name")) {
                    cancelled() || RouteExportSettings(context).read().let { current ->
                        !current.webdav || runCatching { target(current) }.getOrNull() != item.getString("target")
                    }
                }
                update(context, id, "saved")
            } catch (e: Exception) {
                val temporary = cancelled() || (e is DavFailure && e.retryable) || (e is java.io.IOException && e !is DavFailure && e !is javax.net.ssl.SSLHandshakeException)
                retry = retry || temporary
                // Only fixed client messages are retained; server responses and credentials never are.
                val message = when {
                    e is DavFailure -> e.message ?: "Übertragung fehlgeschlagen"
                    e is java.security.GeneralSecurityException -> "App-Passwort neu eingeben"
                    e is javax.net.ssl.SSLHandshakeException -> "Serverzertifikat prüfen"
                    e is java.io.IOException -> "Netzwerk nicht erreichbar"
                    !file.isFile -> "Lokale FIT-Datei fehlt"
                    else -> "Einstellungen prüfen oder Upload erneut anstoßen"
                }
                update(context, id, if (temporary) "pending" else "failed", message)
            }
        }
        return retry || synchronized(lock) { val all = entries(context); all.keys().asSequence().any {
            all.getJSONObject(it).optString("state") in setOf("pending", "uploading") } }
    }
    private fun update(context: Context, id: String, state: String, error: String = "") = synchronized(lock) {
        val all = entries(context); all.optJSONObject(id)?.put("state", state)?.put("error", error); save(context, all)
    }
}

class RouteUploadService : JobService() {
    private var cancelled = AtomicBoolean(false)
    override fun onStartJob(params: JobParameters): Boolean {
        val token = AtomicBoolean(false); cancelled = token
        Thread({ val retry = try { RouteUploads.run(applicationContext, { token.get() }) } catch (_: Exception) { true }
            if (!token.get()) jobFinished(params, retry)
        }, "B-Cycle-WebDAV").start()
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { cancelled.set(true); return true }
}
