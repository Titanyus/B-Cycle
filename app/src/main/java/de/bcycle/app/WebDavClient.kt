package de.bcycle.app

import java.io.File
import java.io.IOException
import java.net.URI
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink

internal class DavFailure(val retryable: Boolean, message: String,val problem:Int?=null) : IOException(message)

/** Ordinary TLS validation, no redirects or credential-bearing URLs. */
internal class WebDavClient(private val config: RouteExportConfig, private val password: String,
    private val client: OkHttpClient = defaultClient,
    private val cancelled: () -> Boolean = { false }) {
    companion object {
        private val defaultClient = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.MINUTES).build()
        fun directory(config: RouteExportConfig): URI {
            val base = URI(config.server.trim().trimEnd('/'))
            require(base.scheme.equals("https", true) && !base.host.isNullOrBlank() && base.rawUserInfo == null &&
                base.rawQuery == null && base.rawFragment == null) { "Bitte eine HTTPS-Nextcloud-Adresse ohne Zugangsdaten eingeben" }
            require(config.user.isNotBlank() && config.user !in setOf(".", "..") && !config.user.any { it == '/' || it == '\\' || it == ':' || it.isISOControl() }) { "Benutzername ungültig" }
            val folders = config.folder.trim('/').split('/').filter { it.isNotEmpty() }
            require(folders.none { it == "." || it == ".." || it.contains('\\') || it.any(Char::isISOControl) }) { "Zielverzeichnis ungültig" }
            val path = base.path.trimEnd('/')
            // Nextcloud's copied WebDAV URL already contains the internal user ID.
            // The login may instead be an email address; never append a second DAV endpoint.
            val completeDavRoot = Regex(".*/remote\\.php/dav/files/[^/]+")
            val root = when {
                completeDavRoot.matches(path) || path.endsWith("/remote.php/webdav") -> path
                else -> {
                    require(!path.contains("/remote.php/")) { "Bitte die Nextcloud-Startadresse oder die WebDAV-Adresse des Hauptverzeichnisses eingeben" }
                    path + "/remote.php/dav/files/" + config.user
                }
            }
            return URI("https", null, base.host, base.port, (listOf(root) + folders).joinToString("/") + "/", null, null)
        }
        fun hash(file: File): String = file.inputStream().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(16384)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
    private val target = directory(config)
    private var backupDirectoryReady=false
    private val auth = "Basic " + Base64.getEncoder().encodeToString((config.user + ":" + password).toByteArray(Charsets.UTF_8))
    private fun request(uri: URI, method: String, file: File? = null, headers: Map<String, String> = emptyMap(),
                        consume: ((okhttp3.Response) -> Unit)? = null): Int {
        if (cancelled()) throw DavFailure(true, "Upload unterbrochen")
        val body = file?.let { source -> object : RequestBody() {
            override fun contentType() = when(source.extension.lowercase()) {
                "json"->"application/json";"gpx"->"application/gpx+xml";"webp"->"image/webp";else->"application/vnd.ant.fit"
            }.toMediaType()
            override fun contentLength() = source.length()
            override fun writeTo(sink: BufferedSink) { source.inputStream().use { input ->
                val buffer = ByteArray(16384)
                while (true) { if (cancelled()) throw DavFailure(true, "Upload unterbrochen")
                    val n = input.read(buffer); if (n < 0) break; sink.write(buffer, 0, n) }
            } }
        } }
        val request = Request.Builder().url(uri.toURL()).method(method, body)
            .header("Authorization", auth).header("User-Agent", "B-Cycle")
        headers.forEach { (key, value) -> request.header(key, value) }
        client.newCall(request.build()).execute().use { response ->
            val code = response.code
            if (code in 200..299) consume?.invoke(response)
            if (code == 401 || code == 403) throw DavFailure(false, "Zugang oder Schreibberechtigung prüfen",R.string.service_auth_failed)
            if (code in 300..399) throw DavFailure(false, "Serveradresse prüfen · Weiterleitung wird nicht verfolgt")
            if (code == 429 || code >= 500) throw DavFailure(true, "Nextcloud vorübergehend nicht verfügbar (HTTP $code)",ServiceProblems.http(code)?.text)
            return code
        }
    }
    fun test() {
        require(password.isNotEmpty()) { "App-Passwort fehlt" }
        val root = directory(config.copy(folder = ""))
        probeDirectory(root)
        ensureDirectories()
        if (target != root) probeDirectory(target)
    }
    private fun probeDirectory(uri: URI) {
        var collection = false
        val code = request(uri, "PROPFIND", headers = mapOf("Depth" to "0"), consume = { response ->
            val bytes = java.io.ByteArrayOutputStream()
            (response.body ?: throw DavFailure(false, "WebDAV-Antwort fehlt")).byteStream().use { input ->
                val buffer = ByteArray(4096)
                while (true) { val n = input.read(buffer); if (n < 0) break
                    if (bytes.size() + n > 128 * 1024) throw DavFailure(false, "WebDAV-Antwort zu groß")
                    bytes.write(buffer, 0, n) }
            }
            val xml = bytes.toString("UTF-8")
            // DAV uses UTF-8. Parse from the checked string so an alternative byte encoding
            // cannot hide a DTD. No external entities, server text or error bodies are logged.
            if (xml.contains("<!DOCTYPE", ignoreCase = true)) throw DavFailure(false, "WebDAV-Antwort enthält eine unzulässige Dokumentdefinition")
            try {
                val builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true; isExpandEntityReferences = false
                }.newDocumentBuilder()
                builder.setErrorHandler(object : org.xml.sax.helpers.DefaultHandler() {
                    override fun error(e: org.xml.sax.SAXParseException) { throw e }
                    override fun fatalError(e: org.xml.sax.SAXParseException) { throw e }
                })
                val document = builder.parse(org.xml.sax.InputSource(java.io.StringReader(xml)))
                val stats = document.getElementsByTagNameNS("DAV:", "propstat")
                for (i in 0 until stats.length) {
                    val stat = stats.item(i) as org.w3c.dom.Element
                    val status = stat.getElementsByTagNameNS("DAV:", "status").item(0)?.textContent.orEmpty()
                    if (Regex("HTTP/[^ ]+ 200(?: .*|$)").matches(status.trim()) &&
                        stat.getElementsByTagNameNS("DAV:", "collection").length > 0) collection = true
                }
            } catch (_: Exception) { throw DavFailure(false, "Server liefert keine gültige WebDAV-Verzeichnisantwort") }
        })
        checkResponse(code, setOf(207), "Nextcloud-Verzeichnis nicht erreichbar")
        if (!collection) throw DavFailure(false, "Ziel ist kein erreichbares WebDAV-Verzeichnis")
    }
    private fun ensureDirectories() {
        if(backupDirectoryReady) return
        val root = directory(config.copy(folder = ""))
        var path = root.path.trimEnd('/')
        config.folder.trim('/').split('/').filter { it.isNotEmpty() }.forEach { segment ->
            path += "/$segment"
            val uri = URI("https", null, target.host, target.port, "$path/", null, null)
            checkResponse(request(uri, "MKCOL"), setOf(201, 405), "Zielverzeichnis konnte nicht angelegt werden")
        }
    }
    fun upload(file: File, name: String) {
        require(password.isNotEmpty()) { "App-Passwort fehlt" }
        require(name.matches(Regex("[A-Za-z0-9_.-]+\\.(fit|json|gpx|webp)")))
        ensureDirectories()
        val uri = URI("https", null, target.host, target.port, target.path + name, null, null)
        val code = request(uri, "PUT", file, mapOf("If-None-Match" to "*"))
        checkResponse(code, setOf(200, 201, 204, 412), "FIT-Upload fehlgeschlagen")
        // A previous upload may have succeeded before a network/process failure. Verify bytes,
        // rather than overwrite a remote file or create a duplicate on retry.
        var actual: String? = null
        checkResponse(request(uri, "GET", consume = { connection ->
            val digest = MessageDigest.getInstance("SHA-256"); var size = 0L
            (connection.body ?: throw IOException("Antwort fehlt")).byteStream().use { input -> val buffer = ByteArray(16384)
                while (true) { if (cancelled()) throw DavFailure(true, "Upload unterbrochen")
                    val n = input.read(buffer); if (n < 0) break; size += n
                    if (size > file.length()) throw DavFailure(false, "Zieldatei unterscheidet sich · nichts überschrieben")
                    digest.update(buffer, 0, n) }
            }
            actual = digest.digest().joinToString("") { "%02x".format(it) }
        }), setOf(200), "Upload konnte nicht geprüft werden")
        if (actual != hash(file)) throw DavFailure(false, "Zieldatei unterscheidet sich · nichts überschrieben")
    }
    private fun checkResponse(code: Int, allowed: Set<Int>, text: String) {
        if (code !in allowed) throw DavFailure(false, "$text (HTTP $code)",ServiceProblems.http(code)?.text)
    }
    fun backupChild(name:String,create:Boolean):WebDavClient {
        backupName(name)
        val child=WebDavClient(config.copy(folder=config.folder.trimEnd('/')+"/"+name),password,client,cancelled)
        if(create) {
            ensureDirectories()
            checkResponse(request(directory(config.copy(folder=config.folder.trimEnd('/')+"/"+name)),"MKCOL"),
                setOf(201),"Sicherungsverzeichnis konnte nicht neu angelegt werden")
            child.backupDirectoryReady=true
        }
        return child
    }
    fun backupIndexPresent():Boolean {
        val uri=URI("https",null,target.host,target.port,target.path+RideDirectoryBackup.INDEX,null,null)
        val code=request(uri,"HEAD")
        if(code==404) return false
        checkResponse(code,setOf(200),"Sicherungsverzeichnis nicht erreichbar")
        return true
    }
    fun openBackupFile(name:String):java.io.InputStream {
        backupName(name)
        if(cancelled()) throw DavFailure(true,"Übertragung unterbrochen")
        val uri=URI("https",null,target.host,target.port,target.path+name,null,null)
        val response=client.newCall(Request.Builder().url(uri.toURL()).get().header("Authorization",auth).build()).execute()
        try {
            checkResponse(response.code,setOf(200),"Sicherungsdatei nicht lesbar")
            val stream=response.body?.byteStream() ?: error("Antwort fehlt")
            return object:java.io.FilterInputStream(stream) {
                override fun close() { try { super.close() } finally { response.close() } }
            }
        } catch(e:Exception) { response.close();throw e }
    }
    fun listBackupDirectory():List<BackupDirectoryEntry> {
        val result=mutableListOf<BackupDirectoryEntry>()
        val code=request(target,"PROPFIND",headers=mapOf("Depth" to "1"),consume={ response ->
            val bytes=response.body?.byteStream()?.use { it.readNBytes(1024*1024+1) } ?: error("Antwort fehlt")
            require(bytes.size<=1024*1024)
            val xml=String(bytes,Charsets.UTF_8)
            require(!xml.contains("<!DOCTYPE",true) && !xml.contains("<!ENTITY",true))
            val factory=javax.xml.parsers.DocumentBuilderFactory.newInstance().apply { isNamespaceAware=true;isExpandEntityReferences=false }
            val builder=factory.newDocumentBuilder()
            builder.setEntityResolver { _,_->throw java.io.IOException("Externe Entität") }
            val doc=builder.parse(org.xml.sax.InputSource(java.io.StringReader(xml)))
            val rows=doc.getElementsByTagNameNS("DAV:","response");require(rows.length<=20000)
            for(i in 0 until rows.length) {
                val row=rows.item(i) as org.w3c.dom.Element
                val href=row.getElementsByTagNameNS("DAV:","href").item(0)?.textContent ?: continue
                val uri=target.resolve(href).normalize()
                require(uri.scheme==target.scheme && uri.host==target.host && uri.port==target.port &&
                    uri.rawUserInfo==null && uri.rawQuery==null && uri.rawFragment==null && uri.path.startsWith(target.path))
                val name=uri.path.removePrefix(target.path).trimEnd('/')
                if(name.isEmpty()) continue
                if(runCatching { backupName(name) }.isFailure) continue // Ignore unrelated filenames; only immediate safe children are usable.
                val stats=row.getElementsByTagNameNS("DAV:","propstat")
                for(j in 0 until stats.length) {
                    val stat=stats.item(j) as org.w3c.dom.Element
                    if(!Regex("HTTP/[^ ]+ 200(?: .*|$)").matches(stat.getElementsByTagNameNS("DAV:","status").item(0)?.textContent.orEmpty().trim())) continue
                    result+=BackupDirectoryEntry(name,stat.getElementsByTagNameNS("DAV:","collection").length>0);break
                }
            }
        })
        checkResponse(code,setOf(207),"Sicherungsverzeichnis nicht lesbar")
        require(result.map { it.name }.distinct().size==result.size)
        return result
    }
}
