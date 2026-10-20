package de.bcycle.app

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

class BackupDirectoryTransportTest {
    private fun server(test:(MockWebServer,OkHttpClient)->Unit) {
        val certificate=HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val tls=HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val trust=HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        MockWebServer().use { server ->
            server.useHttps(tls.sslSocketFactory(),false);server.start()
            test(server,OkHttpClient.Builder().sslSocketFactory(trust.sslSocketFactory(),trust.trustManager)
                .followRedirects(false).followSslRedirects(false).build())
        }
    }
    @Test fun webdavDirectoryWritesFitAndJsonWithoutOverwriteListsSnapshotsAndReadsExactBytes()=server { server,http ->
        val files=ConcurrentHashMap<String,ByteArray>();val directories=mutableSetOf<String>()
        val root="/remote.php/dav/files/rider/Backup/"
        server.dispatcher=object:Dispatcher() {
            override fun dispatch(request:RecordedRequest):MockResponse {
                val path=request.requestUrl!!.encodedPath
                if(request.getHeader("Authorization")!="Basic "+java.util.Base64.getEncoder().encodeToString("rider:password".toByteArray())) return MockResponse().setResponseCode(401)
                return when(request.method) {
                    "MKCOL" -> MockResponse().setResponseCode(if(synchronized(directories) { directories.add(path) }) 201 else 405)
                    "PUT" -> {
                        assertEquals("*",request.getHeader("If-None-Match"))
                        val existing=files.putIfAbsent(path,request.body.readByteArray())
                        MockResponse().setResponseCode(if(existing==null) 201 else 412)
                    }
                    "HEAD" -> MockResponse().setResponseCode(if(files.containsKey(path)) 200 else 404)
                    "GET" -> files[path]?.let { MockResponse().setBody(okio.Buffer().write(it)) } ?: MockResponse().setResponseCode(404)
                    "PROPFIND" -> {
                        assertEquals("1",request.getHeader("Depth"))
                        val children=synchronized(directories) { directories.filter { it.startsWith(path) && it!=path && '/' !in it.removePrefix(path).trimEnd('/') }.map { it to true } }+
                            files.keys.filter { it.startsWith(path) && '/' !in it.removePrefix(path) }.map { it to false }
                        val xml="<d:multistatus xmlns:d='DAV:'>"+children.joinToString("") { (name,dir) ->
                            "<d:response><d:href>$name</d:href><d:propstat><d:prop><d:resourcetype>"+(if(dir) "<d:collection/>" else "")+
                                "</d:resourcetype></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>"
                        }+"</d:multistatus>"
                        MockResponse().setResponseCode(207).setBody(xml)
                    }
                    else -> MockResponse().setResponseCode(400)
                }
            }
        }
        val directory=DavBackupDirectory(WebDavClient(RouteExportConfig(server=server.url("/").toString(),user="rider",folder="Backup"),"password",http))
        val snapshot=directory.child("Bicycle-Backup-test",true)
        val file=Files.createTempFile("backup-data",".fit").toFile().apply { writeBytes(byteArrayOf(1,2,3,4)) }
        val json=Files.createTempFile("backup-data",".json").toFile().apply { writeText("{\"id\":\"portable\"}") }
        try {
            assertFalse(directory.isSnapshot())
            snapshot.write("ride.fit",file);snapshot.write("ride.json",json)
            assertTrue(directory.entries().contains(BackupDirectoryEntry("Bicycle-Backup-test",true)))
            assertEquals(setOf("ride.fit","ride.json"),snapshot.entries().map { it.name }.toSet())
            assertArrayEquals(file.readBytes(),snapshot.open("ride.fit").use { it.readBytes() })
            assertArrayEquals(json.readBytes(),snapshot.open("ride.json").use { it.readBytes() })
            snapshot.write(RideDirectoryBackup.INDEX,json);assertTrue(snapshot.isSnapshot())
            snapshot.write("ride.fit",file) // Retry only verifies the exact previously written bytes.
            file.writeText("other")
            assertThrows(DavFailure::class.java) { snapshot.write("ride.fit",file) }
            assertArrayEquals(byteArrayOf(1,2,3,4),files[root+"Bicycle-Backup-test/ride.fit"])
            assertThrows(DavFailure::class.java) { directory.child("Bicycle-Backup-test",true) }
        } finally { file.delete();json.delete() }
    }
    @Test fun foreignAndTraversalWebdavHrefsAreRejectedWithoutSendingCredentialsToAnotherDestination()=server { server,http ->
        val client=WebDavClient(RouteExportConfig(server=server.url("/").toString(),user="rider",folder="Backup"),"password",http)
        for(href in listOf("https://evil.invalid/steal.fit","/remote.php/dav/files/rider/Backup/../secret.fit")) {
            server.enqueue(MockResponse().setResponseCode(207).setBody("<d:multistatus xmlns:d='DAV:'><d:response><d:href>$href</d:href></d:response></d:multistatus>"))
            assertThrows(IllegalArgumentException::class.java) { client.listBackupDirectory() }
        }
        assertEquals(2,server.requestCount)
    }
}
