package de.bcycle.app

/** In-memory results only. Journal/FIT schemas remain unchanged. */
internal enum class RecordingFinishStage { ENCODE_FIT, RETAIN_LOCAL, PREPARE_LOCAL, QUEUE_UPLOADS, EXPORT_LOCAL, FINALIZE }
internal enum class RecordingDestination { LOCAL_FOLDER, WEBDAV, INTERVALS, DREEVE, STRAVA }
internal enum class RecordingWarning { INCOMPLETE_RECORDING, ELEVATION_CORRECTION_FAILED }

internal sealed interface RideCompletion {
    val rideId: String?
    // Remote destinations describe queueing only, never remote upload success.
    data class Completed(override val rideId: String, val destinations: Set<RecordingDestination> = emptySet(),
        val warnings: Set<RecordingWarning> = emptySet()) : RideCompletion
    data class LocallyRetained(override val rideId: String, val failedAt: RecordingFinishStage,
        val warnings: Set<RecordingWarning> = emptySet()) : RideCompletion
    data class NotSaved(val failedAt: RecordingFinishStage) : RideCompletion { override val rideId: String? = null }
}

internal sealed interface RecordingUpdate {
    enum class Progress : RecordingUpdate { SAVING_FIT, CORRECTING_ELEVATION }
    enum class Notice : RecordingUpdate { OVERLOADED, START_FAILED, WRITE_FAILED, NO_JOURNAL, NO_PENDING }
    data class Finished(val result: RideCompletion) : RecordingUpdate
    data class Recovered(val results: List<RideCompletion>) : RecordingUpdate {
        val completedCount get() = results.count { it is RideCompletion.Completed }
    }
    val busy get() = this is Progress
}
