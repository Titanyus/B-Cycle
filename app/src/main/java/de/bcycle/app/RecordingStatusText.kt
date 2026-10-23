package de.bcycle.app

import android.content.Context

/** Legacy wording is rendered here; storage decisions never inspect these strings. */
internal object RecordingStatusText {
    fun showFeedback(update: RecordingUpdate): Boolean = when (update) {
        is RecordingUpdate.Progress -> false
        is RecordingUpdate.Notice -> update in setOf(RecordingUpdate.Notice.OVERLOADED, RecordingUpdate.Notice.START_FAILED, RecordingUpdate.Notice.WRITE_FAILED)
        is RecordingUpdate.Finished -> true
        is RecordingUpdate.Recovered -> update.results.isNotEmpty()
    }
    fun text(context: Context, update: RecordingUpdate): String = when (update) {
        RecordingUpdate.Progress.SAVING_FIT -> context.getString(R.string.recording_saving_fit)
        RecordingUpdate.Progress.CORRECTING_ELEVATION -> context.getString(R.string.elevation_correcting)
        RecordingUpdate.Notice.OVERLOADED -> context.getString(R.string.recording_storage_overloaded)
        RecordingUpdate.Notice.START_FAILED -> context.getString(R.string.recording_start_failed)
        RecordingUpdate.Notice.WRITE_FAILED -> context.getString(R.string.recording_write_failed)
        RecordingUpdate.Notice.NO_JOURNAL -> context.getString(R.string.recording_no_journal)
        RecordingUpdate.Notice.NO_PENDING -> context.getString(R.string.recording_no_pending)
        is RecordingUpdate.Finished -> completion(context, update.result)
        is RecordingUpdate.Recovered -> when (update.results.size) {
            0 -> context.getString(R.string.recording_no_pending)
            1 -> completion(context, update.results.single())
            else -> context.getString(R.string.recording_recovered, update.completedCount.toString(), update.results.size.toString())
        }
    }
    private fun completion(context: Context, result: RideCompletion): String = when (result) {
        RideCompletion.TooShort -> context.getString(R.string.recording_too_short)
        is RideCompletion.NotSaved -> context.getString(R.string.recording_not_saved)
        is RideCompletion.LocallyRetained -> context.getString(R.string.recording_locally_retained)
        is RideCompletion.Completed -> buildString {
            append(context.getString(R.string.recording_saved))
            if (RecordingDestination.LOCAL_FOLDER in result.destinations) append(context.getString(R.string.recording_saved_folder))
            if (RecordingDestination.WEBDAV in result.destinations) append(context.getString(R.string.recording_queued_webdav))
            if (RecordingWarning.INCOMPLETE_RECORDING in result.warnings) append(context.getString(R.string.recording_incomplete))
            if (RecordingDestination.INTERVALS in result.destinations) append(" · ").append(context.getString(R.string.intervals_queued))
            if (RecordingDestination.DREEVE in result.destinations) append(" · ").append(context.getString(R.string.dreeve_title))
            if (RecordingDestination.STRAVA in result.destinations) append(" · ").append(context.getString(R.string.strava_title))
            if (RecordingWarning.ELEVATION_CORRECTION_FAILED in result.warnings) append(" · ").append(context.getString(R.string.elevation_auto_failed))
        }
    }
}
