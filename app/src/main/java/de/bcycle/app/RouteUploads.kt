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

internal enum class UploadState { NOT_UPLOADED, WAITING, UPLOADING, UPLOADED, FAILED }
internal data class RouteUploadStatus(val state: UploadState, val message: String = "")

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
    fun status(context: Context, id: String): String = state(context, id).message
    fun state(context: Context, id: String, stamp: RouteFileStamp? = null): RouteUploadStatus = synchronized(lock) {
        val key = context.filesDir.path + ":" + revision(context)
        if (statusCacheKey != key) { statusCache = entries(context); statusCacheKey = key }
        val item = statusCache.optJSONObject(id) ?: return@synchronized RouteUploadStatus(UploadState.NOT_UPLOADED)
        val currentTarget = runCatching { target(RouteExportSettings(context).read()) }.getOrNull()
        if (item.optString("target") != currentTarget) return@synchronized RouteUploadStatus(UploadState.NOT_UPLOADED, "Uploadziel geändert")
        if (!RouteTransferPayload.metadataMatches(context,id,item) || (stamp != null && !RouteTransferPayload.matches(item,stamp)))
            return@synchronized RouteUploadStatus(UploadState.NOT_UPLOADED, "Datei geändert · Upload erneut prüfen")
        when (item.optString("state")) {
            "saved" -> if (item.optString("hash").isNotEmpty()) RouteUploadStatus(UploadState.UPLOADED, "Nextcloud gesichert")
                else RouteUploadStatus(UploadState.NOT_UPLOADED, "Früheren Upload erneut prüfen")
            "failed" -> RouteUploadStatus(UploadState.FAILED, "Upload fehlgeschlagen · ${item.optString("error")}")
            "uploading" -> RouteUploadStatus(UploadState.UPLOADING, "Nextcloud-Upload läuft …")
            else -> RouteUploadStatus(UploadState.WAITING, if (RouteExportSettings(context).read().webdav)
                "Nextcloud-Upload ausstehend" + item.optString("error").takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty() else "Upload pausiert")
        }
    }
    private fun fingerprint(file: File) = WebDavClient.hash(file)
    private fun target(config: RouteExportConfig) = WebDavClient.directory(config).toString()
    fun enqueue(context: Context, file: File, name: String) {
        val config = RouteExportSettings(context).read()
        if (!config.webdav) return
        queue(context, file, name, config)
    }
    fun retry(context: Context, id: String, force:Boolean=false) {
        val config = RouteExportSettings(context).read()
        check(config.webdav) { "WebDAV bitte in den Streckeneinstellungen aktivieren" }
        require(eligible(context,id))
        val file = RouteTransferPayload.file(context,id)
        require(file.isFile)
        // MapState verifies RECORDED metadata before exposing this action; restored rides
        // may retain an imported-prefixed immutable original after metadata recovery.
        queue(context, file, "B-Cycle-$id.fit", config,force)
    }
    internal fun eligible(context: Context, id: String): Boolean {
        if(id.matches(Regex("[a-f0-9-]{36}"))) return RouteTransferPayload.eligibleGpx(context,id)
        val index = File(context.filesDir, "fit-library/index.json")
        if (!index.isFile) return id.startsWith("recorded-")
        val known = runCatching { JSONObject(index.readText()).getJSONArray("entries") }.getOrNull() ?: return false
        for (i in 0 until known.length()) {
            val item = known.getJSONObject(i)
            if (item.optString("id") == id) return if (item.has("origin")) item.optString("origin") == "RECORDED"
                else item.opt("recorded") == true
        }
        // A recording worker retains the original before MapState registers its index entry.
        return id.startsWith("recorded-")
    }
    private fun queue(context: Context, file: File, name: String, config: RouteExportConfig,force:Boolean=false) {
        require(eligible(context, file.nameWithoutExtension)) { "Cloud-Upload nur für selbst gefahrene FIT-Fahrten" }
        val hash = fingerprint(file) // Only recording/library workers call this method.
        val metadata=RouteTransferPayload.annotate(context,file,JSONObject())
        synchronized(lock) {
            val all = entries(context); val id = file.nameWithoutExtension
            val destination = target(config); val existing = all.optJSONObject(id)
            if (!force && existing?.optString("target") == destination && existing.optString("hash") == hash &&
                existing.optLong("length") == file.length() && existing.optLong("modified") == file.lastModified() &&
                existing.optString("state") in setOf("pending", "uploading", "saved")) return@synchronized
            all.put(id, metadata.put("name", existing?.optString("name")?.takeIf { it.isNotEmpty() } ?: name)
                .put("target", destination).put("state", "pending").put("hash", hash)
                .put("length", file.length()).put("modified", file.lastModified()))
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
            var file = if(id.matches(Regex("[a-f0-9-]{36}"))) File(context.filesDir,"gpx-library/$id.gpx") else FitLibrary.activeFile(context.filesDir,id)
            try {
                check(eligible(context, id)) { "Cloud-Upload nur für selbst gefahrene FIT-Fahrten" }
                check(RouteTransferPayload.metadataMatches(context,id,item))
                file=RouteTransferPayload.file(context,id)
                check(file.isFile) { "Lokale FIT-Datei fehlt" }
                check(item.getString("target") == target(config)) { "Ziel geändert · Upload erneut anstoßen" }
                val hash = fingerprint(file)
                if (item.optString("hash").isNotEmpty()) check(item.getString("hash") == hash) { "Datei geändert" }
                item.put("hash", hash).put("length", file.length()).put("modified", file.lastModified())
                val sameGeneration = synchronized(lock) {
                    val all = entries(context); val current = all.getJSONObject(id)
                    if (current.optString("target") != item.getString("target") ||
                        (current.optString("hash").isNotEmpty() && current.optString("hash") != hash)) false
                    else { item.put("state", "uploading"); all.put(id, item); save(context, all); true }
                }
                if (!sameGeneration) { retry = true; continue }
                upload(config, file, item.getString("name")) {
                    cancelled() || RouteExportSettings(context).read().let { current ->
                        !current.webdav || runCatching { target(current) }.getOrNull() != item.getString("target")
                    }
                }
                if (cancelled() || RouteExportSettings(context).read().let { !it.webdav || runCatching { target(it) }.getOrNull() != item.getString("target") }) throw java.io.IOException("Upload cancelled")
                check(fingerprint(file) == hash && file.length() == item.getLong("length") && file.lastModified() == item.getLong("modified")) { "Datei während Upload geändert" }
                update(context, id, "saved", expectedHash = hash, expectedTarget = item.getString("target"))
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
                update(context, id, if (temporary) "pending" else "failed", message, item.optString("hash").takeIf { it.isNotEmpty() }, item.optString("target"))
            }
        }
        return retry || synchronized(lock) { val all = entries(context); all.keys().asSequence().any {
            all.getJSONObject(it).optString("state") in setOf("pending", "uploading") } }
    }
    private fun update(context: Context, id: String, state: String, error: String = "", expectedHash: String? = null, expectedTarget: String? = null) = synchronized(lock) {
        val all = entries(context); val current = all.optJSONObject(id) ?: return@synchronized
        if (expectedHash != null && current.optString("hash") != expectedHash) return@synchronized
        if (expectedTarget != null && current.optString("target") != expectedTarget) return@synchronized
        current.put("state", state).put("error", error); save(context, all)
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
