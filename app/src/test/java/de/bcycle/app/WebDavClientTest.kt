package de.bcycle.app

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WebDavClientTest {
    private val directoryXml = "<d:multistatus xmlns:d='DAV:'><d:response><d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>"
    private fun server(test: (MockWebServer, OkHttpClient, RouteExportConfig) -> Unit) {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        MockWebServer().use { server ->
            server.useHttps(serverTls.sslSocketFactory(), false); server.start()
            val client = OkHttpClient.Builder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager)
                .followRedirects(false).followSslRedirects(false).build()
            test(server, client, RouteExportConfig(webdav = true, server = server.url("/cloud").toString(), user = "rider", folder = "B Cycle/2026"))
        }
    }
    private fun file(): File = Files.createTempFile("bcycle-dav", ".fit").toFile().apply { writeBytes(byteArrayOf(14, 32, 46, 70, 73, 84)) }
    @Test fun httpsUploadCreatesNestedFoldersStreamsUnchangedFitAndVerifiesContents() = server { server, client, config ->
        val file = file()
        try {
            server.enqueue(MockResponse().setResponseCode(201)); server.enqueue(MockResponse().setResponseCode(405))
            server.enqueue(MockResponse().setResponseCode(201)); server.enqueue(MockResponse().setBody(okio.Buffer().write(file.readBytes())))
            WebDavClient(config, "secret", client).upload(file, "B-Cycle-test.fit")
            assertEquals("MKCOL", server.takeRequest().method)
            assertEquals("MKCOL", server.takeRequest().method)
            val put = server.takeRequest()
            assertEquals("PUT", put.method); assertEquals("*", put.getHeader("If-None-Match"))
            assertEquals("/cloud/remote.php/dav/files/rider/B%20Cycle/2026/B-Cycle-test.fit", put.path)
            assertArrayEquals(file.readBytes(), put.body.readByteArray())
            assertEquals("GET", server.takeRequest().method)
        } finally { file.delete() }
    }
    @Test fun retryAcceptsSameRemoteFileButRejectsDifferentContentsWithoutOverwriting() = server { server, client, config ->
        val file = file(); val dav = WebDavClient(config.copy(folder = ""), "secret", client)
        try {
            server.enqueue(MockResponse().setResponseCode(412)); server.enqueue(MockResponse().setBody(okio.Buffer().write(file.readBytes())))
            dav.upload(file, "B-Cycle-test.fit")
            server.takeRequest(); server.takeRequest()
            server.enqueue(MockResponse().setResponseCode(412)); server.enqueue(MockResponse().setBody("other!"))
            val error = assertThrows(DavFailure::class.java) { dav.upload(file, "B-Cycle-test.fit") }
            assertFalse(error.retryable); assertFalse(error.message!!.contains("secret"))
            assertEquals(2, server.requestCount - 2) // Conditional PUT + verification, never an overwrite PUT.
        } finally { file.delete() }
    }
    @Test fun authenticationFailureIsPermanentNetworkServerFailureRetryableAndRedirectNotFollowed() = server { server, client, config ->
        val dav = WebDavClient(config, "secret", client)
        for ((code, temporary) in listOf(401 to false, 403 to false, 503 to true, 429 to true, 302 to false)) {
            server.enqueue(MockResponse().setResponseCode(code).setHeader("Location", server.url("/elsewhere")))
            val error = assertThrows(DavFailure::class.java) { dav.test() }
            assertEquals(temporary, error.retryable); assertFalse(error.message!!.contains("secret"))
            assertEquals("PROPFIND", server.takeRequest().method)
        }
        assertEquals(5, server.requestCount)
    }
    @Test fun connectionTestChecksDavRootAndCreatesConfiguredFolderWithoutUploadingFit() = server { server, client, config ->
        server.enqueue(MockResponse().setResponseCode(207).setBody(directoryXml)); server.enqueue(MockResponse().setResponseCode(201)); server.enqueue(MockResponse().setResponseCode(405))
        server.enqueue(MockResponse().setResponseCode(207).setBody(directoryXml))
        WebDavClient(config, "secret", client).test()
        val first = server.takeRequest(); assertEquals("PROPFIND", first.method); assertEquals("0", first.getHeader("Depth"))
        assertEquals("/cloud/remote.php/dav/files/rider/", first.path)
        assertEquals("MKCOL", server.takeRequest().method); assertEquals("MKCOL", server.takeRequest().method)
        assertEquals("PROPFIND", server.takeRequest().method)
    }
    @Test fun connectionTestRejectsMissingDavCollectionAndExternalEntityResponses() = server { server, client, config ->
        val dav = WebDavClient(config.copy(folder = ""), "secret", client)
        for (body in listOf(directoryXml.replace("200 OK", "404 Not Found"), "<html/>",
            "<!DOCTYPE x [<!ENTITY secret SYSTEM 'file:///etc/passwd'>]><x>&secret;</x>")) {
            server.enqueue(MockResponse().setResponseCode(207).setBody(body))
            assertFalse(assertThrows(DavFailure::class.java) { dav.test() }.retryable)
            assertEquals("PROPFIND", server.takeRequest().method)
        }
    }
    @Test fun emptyTargetProbesUserRootWithExactlyOneTrailingSlash() = server { server, client, config ->
        server.enqueue(MockResponse().setResponseCode(207).setBody(directoryXml))
        WebDavClient(config.copy(folder = ""), "secret", client).test()
        val request = server.takeRequest()
        assertEquals("PROPFIND", request.method)
        assertEquals("/cloud/remote.php/dav/files/rider/", request.path)
        assertEquals(1, server.requestCount)
        assertEquals("https://cloud.example.org/remote.php/dav/files/rider/",
            WebDavClient.directory(config.copy(server = "https://cloud.example.org/", folder = "")).toASCIIString())
    }
    @Test fun copiedDavUserRootWorksWithEmailLoginAndEncodedNamesWithoutDuplicatingEndpoint() = server { server, client, config ->
        val direct = config.copy(server = server.url("/cloud/remote.php/dav/files/internal%20id/").toString(),
            user = "rider@example.org", folder = "")
        server.enqueue(MockResponse().setResponseCode(207).setBody(directoryXml))
        WebDavClient(direct, "secret", client).test()
        assertEquals("/cloud/remote.php/dav/files/internal%20id/", server.takeRequest().path)
        assertEquals("/cloud/remote.php/dav/files/internal id/Sport/Fahrten/",
            WebDavClient.directory(direct.copy(folder = "Sport/Fahrten")).path)
        assertEquals("/cloud/remote.php/webdav/", WebDavClient.directory(direct.copy(server = server.url("/cloud/remote.php/webdav/").toString())).path)
        assertThrows(IllegalArgumentException::class.java) {
            WebDavClient.directory(direct.copy(server = server.url("/cloud/remote.php/dav/files/internal-id/already-a-folder/").toString()))
        }
    }
    @Test fun addressesRejectInsecureSchemesEmbeddedSecretsAndPathTraversalAndEncodeNames() {
        val base = RouteExportConfig(server = "https://example.org/nextcloud", user = "Rad Fahrer", folder = "Fahrten/2026")
        assertTrue(WebDavClient.directory(base).toASCIIString().contains("Rad%20Fahrer/Fahrten/2026/"))
        for (url in listOf("http://example.org", "https://user:secret@example.org", "https://example.org/?token=secret", "https://example.org/#x"))
            assertThrows(IllegalArgumentException::class.java) { WebDavClient.directory(base.copy(server = url)) }
        assertThrows(IllegalArgumentException::class.java) { WebDavClient.directory(base.copy(folder = "../x")) }
    }
}
