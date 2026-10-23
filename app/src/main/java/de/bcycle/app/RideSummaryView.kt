package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.widget.LinearLayout

/** Uses the same metric tiles and the saved analysis as the route details. No live-value estimates. */
internal class RideSummaryView(context:Context,private val themes:ThemeManager,private val completion:RideCompletion,openDetails:()->Unit={}):LinearLayout(context) {
    private val metrics=LinearLayout(context).apply { orientation=VERTICAL }
    private val status=BicycleComponents.description(context,themes,"").apply { tag="ride_summary_status" }
    private val problem=BicycleComponents.description(context,themes,context.getString(R.string.ride_summary_loading)).apply { tag="ride_summary_loading" }
    private var entry:ManagedRoute?=null
    private val receipts=listOf("route-uploads","intervals-uploads","activity-uploads-dreeve","activity-uploads-strava",
        "route-export","intervals-settings","activity-service-dreeve","activity-service-strava").map { context.getSharedPreferences(it,0) }
    private val listener=SharedPreferences.OnSharedPreferenceChangeListener { _,_ -> post { updateStatus() } }
    private val details=BicycleComponents.action(context,themes,context.getString(R.string.ride_summary_details),openDetails).apply { tag="ride_summary_details";isEnabled=false }
    init {
        orientation=VERTICAL;tag="ride_summary"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        addView(metrics,LayoutParams(-1,-2));addView(problem,LayoutParams(-1,-2))
        addView(status,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(details)
        showMetrics(emptyMap());updateStatus()
    }
    private fun showMetrics(values:Map<AnalysisMetric,Double>) {
        metrics.removeAllViews()
        listOf(listOf(AnalysisMetric.DISTANCE,AnalysisMetric.MOVING,AnalysisMetric.AVG_SPEED),
            listOf(AnalysisMetric.AVG_HEART,AnalysisMetric.NP)).forEach { fields ->
            metrics.addView(LinearLayout(context).apply {
                orientation=HORIZONTAL;isBaselineAligned=false
                fields.forEach { metric -> addView(AnalysisMetricTile(context,themes,listOf(metric),values),LayoutParams(0,-1,1f)) }
            },LayoutParams(-1,-2))
        }
    }
    fun show(analysis:RouteAnalysis) {
        entry=analysis.entry;details.isEnabled=true;showMetrics(analysis.metrics);problem.visibility=GONE;updateStatus()
    }
    fun loadingFailed() { problem.setText(R.string.ride_summary_load_failed) }
    private fun updateStatus() {
        val states=RideSummaryStatus.targets(completion).associateWith { target ->
            entry?.let { runCatching { target.status(context,it).state }.getOrDefault(UploadState.FAILED) } ?: UploadState.WAITING
        }
        status.text=RideSummaryStatus.text(context,completion,states)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow();receipts.forEach { it.registerOnSharedPreferenceChangeListener(listener) };updateStatus()
    }
    override fun onDetachedFromWindow() {
        receipts.forEach { it.unregisterOnSharedPreferenceChangeListener(listener) };super.onDetachedFromWindow()
    }
}

/** Queue acceptance is never reported as a successful remote upload. */
internal object RideSummaryStatus {
    fun targets(completion:RideCompletion):List<RouteTransferTarget> = (completion as? RideCompletion.Completed)?.destinations.orEmpty().mapNotNull { when(it) {
        RecordingDestination.LOCAL_FOLDER->null
        RecordingDestination.WEBDAV->RouteTransferTarget.WEBDAV
        RecordingDestination.INTERVALS->RouteTransferTarget.INTERVALS
        RecordingDestination.DREEVE->RouteTransferTarget.DREEVE
        RecordingDestination.STRAVA->RouteTransferTarget.STRAVA
    } }.sortedBy { it.ordinal }
    fun text(context:Context,completion:RideCompletion,states:Map<RouteTransferTarget,UploadState>):String = buildList {
        if(completion !is RideCompletion.Completed) { add(RecordingStatusText.text(context,RecordingUpdate.Finished(completion)));return@buildList }
        add(context.getString(R.string.ride_summary_saved))
        if(RecordingDestination.LOCAL_FOLDER in completion.destinations) add(context.getString(R.string.ride_summary_folder))
        if(RecordingWarning.INCOMPLETE_RECORDING in completion.warnings) add(context.getString(R.string.recording_incomplete).trim(' ','·'))
        if(RecordingWarning.ELEVATION_CORRECTION_FAILED in completion.warnings) add(context.getString(R.string.elevation_auto_failed))
        val targets=targets(completion)
        listOf(UploadState.UPLOADED to R.string.ride_summary_uploaded,UploadState.UPLOADING to R.string.ride_summary_uploading,
            UploadState.WAITING to R.string.ride_summary_waiting,UploadState.FAILED to R.string.ride_summary_upload_failed).forEach { (state,label) ->
            val names=targets.filter { (states[it] ?: UploadState.WAITING).let { actual -> if(actual==UploadState.NOT_UPLOADED) UploadState.WAITING else actual }==state }
                .map { if(it==RouteTransferTarget.WEBDAV) context.getString(R.string.ride_backup_webdav) else context.getString(it.title) }
            if(names.isNotEmpty()) add(context.getString(label,names.joinToString(", ")))
        }
    }.joinToString("\n")
}
