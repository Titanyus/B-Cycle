package de.bcycle.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
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
    private fun report(update: RecordingUpdate) {
        val text = RecordingStatusText.text(context, update)
        handler.post { changed(text, update.busy) }
    }
    private fun submit(action: () -> Unit): Boolean {
        return try { worker.execute(action); true } catch (_: RejectedExecutionException) {
            failed = true; report(RecordingUpdate.Notice.OVERLOADED); false
        }
    }
    fun begin(start: Long, config: RecordingConfig) {
        failed = false
        val naming=RideNamingSettings(context).read()
        val namingZone=ZoneId.systemDefault()
        val namingLocale=java.util.Locale.getDefault().toLanguageTag()
        val profileAppearance=runCatching { RideProfileColor.snapshot(context) }.getOrNull()
        submit {
            try {
                val name = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(start)) + "_" + UUID.randomUUID().toString().take(8)
                File(directory,"$name.naming.json").writeText(JSONObject().put("template",naming.json()).put("zone",namingZone.id).put("locale",namingLocale).toString())
                journal = RideJournal(File(directory, "$name.ride"), start, config)
                if(profileAppearance!=null) runCatching { File(directory,"$name.profile.json").writeText(profileAppearance) }
            } catch (_: Exception) { failed = true; report(RecordingUpdate.Notice.START_FAILED) }
        }
    }
    fun append(sample: RideSample) {
        if (failed) return
        if (queuedSamples.incrementAndGet() > 24) {
            queuedSamples.decrementAndGet(); failed = true
            report(RecordingUpdate.Notice.OVERLOADED); return
        }
        val accepted = submit {
            try {
                if (!failed) journal?.append(sample)
            } catch (_: Exception) { failed = true; report(RecordingUpdate.Notice.WRITE_FAILED) }
            finally { queuedSamples.decrementAndGet() }
        }
        if (!accepted) queuedSamples.decrementAndGet()
    }
    fun finish() = submit {
        val current = journal; journal = null
        try { current?.close() } catch (_: Exception) { failed = true }
        if (current == null) { report(RecordingUpdate.Notice.NO_JOURNAL); return@submit }
        report(RecordingUpdate.Progress.SAVING_FIT)
        report(RecordingUpdate.Finished(export(current.file)))
    }
    fun retryPending() = submit {
        val files = directory.listFiles { file -> file.extension == "ride" && file != journal?.file }.orEmpty()
        if (files.isEmpty()) { report(RecordingUpdate.Notice.NO_PENDING); return@submit }
        report(RecordingUpdate.Progress.SAVING_FIT)
        report(RecordingUpdate.Recovered(files.map(::export)))
    }
    private fun export(source: File): RideCompletion {
        val encoded = File(directory, source.nameWithoutExtension + ".fit")
        var retainedId: String? = null
        var stage = RecordingFinishStage.ENCODE_FIT
        val warnings = mutableSetOf<RecordingWarning>()
        try {
            FitWriter.encode(source, encoded)
            stage = RecordingFinishStage.RETAIN_LOCAL
            val original = FitLibrary.retainRecording(context.filesDir, encoded)
            retainedId = original.nameWithoutExtension
            stage = RecordingFinishStage.PREPARE_LOCAL
            RideAnalysisFiles.retain(context.filesDir,original.nameWithoutExtension,source)
            val appearance=File(directory,source.nameWithoutExtension+".profile.json")
            if(appearance.isFile) runCatching { RideProfileColor.confirm(context,original.nameWithoutExtension,appearance.readText()) }
            var correctionFailed=false
            var indoor=false
            RideJournal.read(source) { _,config,_,_ -> indoor=config.indoor }
            if(FitElevationCorrection.enabled(context) && !indoor) {
                report(RecordingUpdate.Progress.CORRECTING_ELEVATION)
                correctionFailed=runCatching {
                    FitElevationCorrection.correct(context,original,FitLibrary.correctedFile(context.filesDir,original.nameWithoutExtension))
                }.isFailure
            }
            if (correctionFailed) warnings += RecordingWarning.ELEVATION_CORRECTION_FAILED
            val namingFile=File(directory,source.nameWithoutExtension+".naming.json")
            if(namingFile.isFile && RideNames.name(context,original.nameWithoutExtension)==null) {
                val snapshot=JSONObject(namingFile.readText())
                val template=RideNaming.decode(snapshot.getJSONObject("template"))
                val details=FitRead.read(RideAnalysisFiles.file(context.filesDir,original.nameWithoutExtension)).details
                RideNames.manual(context,original.nameWithoutExtension,template.format(details,ZoneId.of(snapshot.getString("zone")),java.util.Locale.forLanguageTag(snapshot.getString("locale"))))
            }
            val exportFile=FitLibrary.activeFile(context.filesDir,original.nameWithoutExtension)
            val config = RouteExportSettings(context).read()
            val upload = runCatching { RouteCloudUploads.enqueue(context, exportFile, "B-Cycle-" + encoded.name) }
            val local = runCatching { if (config.save) RouteLocalSaves.perform(context, original.nameWithoutExtension, exportFile, config.tree.ifEmpty { "Downloads/B-Cycle" }) {
                LocalRouteExport.write(context, exportFile, "B-Cycle-" + encoded.name, config.tree)
            } }
            stage = RecordingFinishStage.QUEUE_UPLOADS
            upload.getOrThrow()
            stage = RecordingFinishStage.EXPORT_LOCAL
            local.getOrThrow()
            stage = RecordingFinishStage.FINALIZE
            source.delete(); encoded.delete();appearance.delete();namingFile.delete()
            val destinations = buildSet {
                if (RouteExportSettings(context).read().save) add(RecordingDestination.LOCAL_FOLDER)
                if (RouteExportSettings(context).read().webdav) add(RecordingDestination.WEBDAV)
                if (IntervalsSettings(context).read().enabled) add(RecordingDestination.INTERVALS)
                ActivityService.entries.filter { ActivityServiceSettings(context,it).read().enabled }.forEach {
                    add(when (it) { ActivityService.DREEVE -> RecordingDestination.DREEVE; ActivityService.STRAVA -> RecordingDestination.STRAVA })
                }
            }
            if (failed) warnings += RecordingWarning.INCOMPLETE_RECORDING
            return RideCompletion.Completed(original.nameWithoutExtension, destinations, warnings.toSet())
        } catch (_: Exception) {
            if (failed) warnings += RecordingWarning.INCOMPLETE_RECORDING
            return retainedId?.let { RideCompletion.LocallyRetained(it, stage, warnings.toSet()) }
                ?: RideCompletion.NotSaved(stage)
        }
    }
}
