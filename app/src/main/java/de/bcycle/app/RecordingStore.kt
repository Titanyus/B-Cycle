package de.bcycle.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger

/** All journal/FIT/download I/O is serialized outside the main thread. */
class RecordingStore(private val context: Context, private val changed: (String, Boolean) -> Unit) {
    private val directory = File(context.filesDir, "pending-rides").apply { mkdirs() }
    private val handler = Handler(Looper.getMainLooper())
    private val worker = ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, ArrayBlockingQueue(32)) { task -> Thread(task, "B-Cycle-FIT") }
    private var journal: RideJournal? = null
    private val queuedSamples = AtomicInteger()
    @Volatile var failed = false; private set
    private fun report(text: String, busy: Boolean) { handler.post { changed(text, busy) } }
    private fun submit(action: () -> Unit): Boolean {
        return try { worker.execute(action); true } catch (_: RejectedExecutionException) {
            failed = true; report("Speicher überlastet · bitte STOPP drücken", false); false
        }
    }
    fun begin(start: Long, config: RecordingConfig) {
        failed = false
        val profileAppearance=runCatching { RideProfileColor.snapshot(context) }.getOrNull()
        submit {
            try {
                val name = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(start)) + "_" + UUID.randomUUID().toString().take(8)
                journal = RideJournal(File(directory, "$name.ride"), start, config)
                if(profileAppearance!=null) runCatching { File(directory,"$name.profile.json").writeText(profileAppearance) }
            } catch (_: Exception) { failed = true; report("Aufzeichnung konnte nicht angelegt werden · bitte STOPP", false) }
        }
    }
    fun append(sample: RideSample) {
        if (failed) return
        if (queuedSamples.incrementAndGet() > 24) {
            queuedSamples.decrementAndGet(); failed = true
            report("Speicher überlastet · bitte STOPP drücken", false); return
        }
        val accepted = submit {
            try {
                if (!failed) journal?.append(sample)
            } catch (_: Exception) { failed = true; report("Fehler beim Schreiben · Zwischendatei bleibt erhalten", false) }
            finally { queuedSamples.decrementAndGet() }
        }
        if (!accepted) queuedSamples.decrementAndGet()
    }
    fun finish() = submit {
        val current = journal; journal = null
        try { current?.close() } catch (_: Exception) { failed = true }
        if (current == null) { report("Keine Aufzeichnungsdatei vorhanden", false); return@submit }
        report("FIT wird gespeichert …", true)
        report(export(current.file), false)
    }
    fun retryPending() = submit {
        val files = directory.listFiles { file -> file.extension == "ride" && file != journal?.file }.orEmpty()
        if (files.isEmpty()) { report("Keine ungespeicherten Fahrten", false); return@submit }
        report("FIT wird gespeichert …", true)
        var saved = 0
        var last = ""
        for (file in files) { last = export(file); if (last.startsWith("Gespeichert:")) saved++ }
        report(if (files.size == 1) last else "$saved von ${files.size} Fahrten gespeichert · übrige Daten bleiben erhalten", false)
    }
    private fun export(source: File): String {
        val encoded = File(directory, source.nameWithoutExtension + ".fit")
        var retained = false
        try {
            FitWriter.encode(source, encoded)
            val original = FitLibrary.retainRecording(context.filesDir, encoded)
            retained = true
            RideAnalysisFiles.retain(context.filesDir,original.nameWithoutExtension,source)
            val appearance=File(directory,source.nameWithoutExtension+".profile.json")
            if(appearance.isFile) runCatching { RideProfileColor.confirm(context,original.nameWithoutExtension,appearance.readText()) }
            var correctionFailed=false
            var indoor=false
            RideJournal.read(source) { _,config,_,_ -> indoor=config.indoor }
            if(FitElevationCorrection.enabled(context) && !indoor) {
                report(context.getString(R.string.elevation_correcting),true)
                correctionFailed=runCatching {
                    FitElevationCorrection.correct(context,original,FitLibrary.correctedFile(context.filesDir,original.nameWithoutExtension))
                }.isFailure
            }
            val exportFile=FitLibrary.activeFile(context.filesDir,original.nameWithoutExtension)
            val config = RouteExportSettings(context).read()
            val upload = runCatching { RouteCloudUploads.enqueue(context, exportFile, "B-Cycle-" + encoded.name) }
            val local = runCatching { if (config.save) RouteLocalSaves.perform(context, original.nameWithoutExtension, exportFile, config.tree.ifEmpty { "Downloads/B-Cycle" }) {
                LocalRouteExport.write(context, exportFile, "B-Cycle-" + encoded.name, config.tree)
            } }
            upload.getOrThrow(); local.getOrThrow()
            source.delete(); encoded.delete();appearance.delete()
            return "Gespeichert: FIT in Streckensammlung" +
                (if (RouteExportSettings(context).read().save) " · auch im Zielordner" else "") +
                (if (RouteExportSettings(context).read().webdav) " · Nextcloud-Upload vorgemerkt" else "") + (if (failed) " · enthält Daten bis zum Speicherfehler" else "") +
                (if(IntervalsSettings(context).read().enabled) " · "+context.getString(R.string.intervals_queued) else "") +
                ActivityService.entries.filter { ActivityServiceSettings(context,it).read().enabled }.joinToString("") { " · "+context.getString(it.title) } +
                (if(correctionFailed) " · "+context.getString(R.string.elevation_auto_failed) else "")
        } catch (_: Exception) {
            return if (retained) "FIT lokal gespeichert · Ordnerexport oder Upload-Vormerkung fehlgeschlagen. Im Menü erneut versuchen."
            else "FIT nicht gespeichert · Daten bleiben erhalten. Im Menü erneut speichern."
        }
    }
}
