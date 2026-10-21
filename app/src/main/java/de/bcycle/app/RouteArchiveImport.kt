package de.bcycle.app

import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

internal data class ArchiveImportResult(val imported: Int = 0, val duplicates: Int = 0,
    val failed: Int = 0, val unsupported: Int = 0, val interrupted: Boolean = false)

/** One bounded temporary file at a time; ZIP paths are never used as filesystem paths. Worker only. */
internal class RouteArchiveImport(private val root: File, private val cache: File,
    private val fits: FitLibrary, private val gpx: GpxLibrary,
    private val limits: Limits = Limits()) {
    internal data class Limits(val entries: Int = 100_000, val totalBytes: Long = 10L * 1024 * 1024 * 1024,
        val fileBytes: Long = FitRead.MAX_BYTES)
    private class LimitReached : IOException()

    fun read(input: InputStream, progress: (Int) -> Unit = {}): ArchiveImportResult = scan(input,progress)

    fun prepare(input:InputStream,cancelled:()->Boolean={false},progress:(Int)->Unit={}):PreparedRideArchive {
        val directory=java.nio.file.Files.createTempDirectory(cache.toPath(),"ride-selection-").toFile()
        val entries=mutableListOf<PreparedArchiveRide>()
        try {
            val report=scan(input,progress,cancelled) { source,name,format,digest,known ->
                val id="$format-$digest"
                if(entries.none { it.id==id }) {
                    val target=File(directory,"$id.$format")
                    val entry=PreparedArchiveRide.read(id,name,format.uppercase(Locale.ROOT),source,target,known)
                    if(!known) source.copyTo(target)
                    entries+=entry
                }
            }
            return PreparedRideArchive(directory,entries.toList(),report)
        } catch(e:Exception) { directory.deleteRecursively();throw e }
    }

    private fun scan(input:InputStream,progress:(Int)->Unit,cancelled:()->Boolean={false},
        preview:((File,String,String,String,Boolean)->Unit)?=null):ArchiveImportResult {
        var result = ArchiveImportResult()
        val fitHashes = fits.entries.mapTo(hashSetOf()) { it.id.substringAfter('-') }
        val gpxHashes = gpx.entries.mapNotNullTo(hashSetOf()) { entry ->
            gpx.file(entry.id).takeIf { it.isFile }?.let(::hash)
        }
        var total = 0L
        var expanded = 0L
        val buffered = input.buffered()
        buffered.mark(4)
        val signature = ByteArray(4)
        var count = 0
        while (count < 4) { val n = buffered.read(signature, count, 4 - count); if (n < 0) break; count += n }
        buffered.reset()
        if (count != 4 || signature[0] != 0x50.toByte() || signature[1] != 0x4b.toByte() ||
            !((signature[2] == 3.toByte() && signature[3] == 4.toByte()) ||
              (signature[2] == 5.toByte() && signature[3] == 6.toByte()))) {
            buffered.close()
            return result.copy(interrupted = true)
        }
        val zip = ZipInputStream(buffered)
        // Count even ignored ZIP entries. A huge unrelated file must not bypass archive limits.
        val bounded = object : FilterInputStream(zip) {
            override fun read(): Int = `in`.read().also { if (it >= 0) count(1) }
            override fun read(b: ByteArray, off: Int, len: Int): Int = `in`.read(b, off, len).also { if (it > 0) count(it) }
            fun count(n: Int) { total += n; if (total > limits.totalBytes || cancelled()) throw LimitReached() }
            override fun close() {} // Individual parsers must not close the shared ZIP stream.
        }
        try {
            zip.use {
                var entries = 0
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    if(cancelled()) throw LimitReached()
                    val entry = zip.nextEntry ?: break
                    if (++entries > limits.entries) throw LimitReached()
                    if (entry.isDirectory) { while (bounded.read(buffer) >= 0) {} ; continue }
                    val name = entry.name.substringAfterLast('/').substringAfterLast('\\')
                    val plainName = if (name.endsWith(".gz", true)) name.dropLast(3) else name
                    val extension = plainName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    if (extension !in setOf("fit", "gpx")) {
                        result = result.copy(unsupported = result.unsupported + 1)
                        while (bounded.read(buffer) >= 0) {}
                        continue
                    }
                    val temporary = File.createTempFile("route-archive-", ".tmp", cache)
                    try {
                        val decoded = if (name.endsWith(".gz", true)) GZIPInputStream(bounded) else bounded
                        var bytes = 0L
                        val maximum = minOf(limits.fileBytes, if (extension == "gpx") GpxImport.MAX_BYTES else FitRead.MAX_BYTES)
                        decoded.use { source ->
                            temporary.outputStream().use { output ->
                                while (true) {
                                    val n = source.read(buffer); if (n < 0) break
                                    bytes += n; expanded += n
                                    if (expanded > limits.totalBytes || cancelled()) throw LimitReached()
                                    if (bytes > maximum) throw IOException("Entry too large")
                                    output.write(buffer, 0, n)
                                }
                            }
                        }
                        val digest = hash(temporary)
                        val hashes = if (extension == "fit") fitHashes else gpxHashes
                        if(preview!=null) {
                            preview(temporary,plainName,extension,digest,digest in hashes)
                            if(digest in hashes) result=result.copy(duplicates=result.duplicates+1)
                            else { hashes+=digest;result=result.copy(imported=result.imported+1) }
                        }
                        else if (digest in hashes) result = result.copy(duplicates = result.duplicates + 1)
                        else {
                            temporary.inputStream().use { original ->
                                if (extension == "fit") checkNotNull(fits.add(original, plainName, recorded = false, preserveDate = true))
                                else gpx.add(original, plainName, select = false, preserveDate = true)
                            }
                            hashes += digest
                            result = result.copy(imported = result.imported + 1)
                        }
                    } catch (e: LimitReached) { throw e }
                    catch (_: Exception) { result = result.copy(failed = result.failed + 1) }
                    finally { temporary.delete() }
                    // Drain through the counter before nextEntry (also after a failed GZIP/parse).
                    while (bounded.read(buffer) >= 0) {}
                    progress(result.imported + result.duplicates + result.failed)
                }
            }
        } catch (_: Exception) { result = result.copy(interrupted = true) }
        return result
    }

    private fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
