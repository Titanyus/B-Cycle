package de.bcycle.app

import android.content.Context
import android.widget.LinearLayout
import android.view.View
import android.graphics.Rect
import android.view.inputmethod.InputMethodManager

/** Uses the same metric tiles and the saved analysis as the route details. No live-value estimates. */
internal class RideSummaryView(context:Context,private val themes:ThemeManager,private val completion:RideCompletion,openDetails:()->Unit={},save:()->Unit={}):LinearLayout(context) {
    private val metrics=LinearLayout(context).apply { orientation=VERTICAL }
    private val status=BicycleComponents.description(context,themes,"").apply { tag="ride_summary_status" }
    private val transfers=RideSummaryTransfers(context,themes)
    private val problem=BicycleComponents.description(context,themes,context.getString(R.string.ride_summary_loading)).apply { tag="ride_summary_loading" }
    private var entry:ManagedRoute?=null
    private var feedback:RideSummaryFeedbackRow?=null
    private val details=BicycleComponents.action(context,themes,context.getString(R.string.ride_summary_details),openDetails).apply { tag="ride_summary_details";isEnabled=false }
    init {
        orientation=VERTICAL;tag="ride_summary"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
        addView(metrics,LayoutParams(-1,-2));addView(problem,LayoutParams(-1,-2))
        addView(BicycleComponents.sectionDivider(context,themes).apply { tag="ride_summary_transfer_divider" })
        completion.rideId?.let { id ->
            addView(BicycleComponents.fieldLabel(context,themes,context.getString(R.string.ride_rpe_question)),LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            feedback=RideSummaryFeedbackRow(context,themes,id).also { addView(it,LayoutParams(-1,-2).apply { bottomMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) }) }
        }
        addView(LinearLayout(context).apply {
            tag="ride_summary_actions";orientation=HORIZONTAL;isBaselineAligned=false
            addView(BicycleComponents.action(context,themes,context.getString(R.string.route_action_save),save).apply { tag="ride_summary_save" },LayoutParams(0,-1,1f).apply { marginEnd=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            addView(details,LayoutParams(0,-1,1f))
        },LayoutParams(-1,-2))
        addView(status,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(transfers,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        isFocusableInTouchMode=true
        showMetrics(emptyMap());updateStatus()
    }
    private fun showMetrics(values:Map<AnalysisMetric,Double>) {
        metrics.removeAllViews()
        listOf(listOf(AnalysisMetric.DISTANCE,AnalysisMetric.MOVING,AnalysisMetric.AVG_SPEED),
            listOf(AnalysisMetric.AVG_HEART,AnalysisMetric.NP)).forEach { fields ->
            metrics.addView(LinearLayout(context).apply {
                orientation=HORIZONTAL;isBaselineAligned=false
                fields.forEach { metric -> addView(AnalysisMetricTile(context,themes,listOf(metric),values,showIcon=false),LayoutParams(0,-1,1f)) }
            },LayoutParams(-1,-2))
        }
        if(entry?.details?.bikeType==BikeType.EBIKE) {
            metrics.addView(BicycleComponents.sectionDivider(context,themes))
            metrics.addView(LinearLayout(context).apply {
                tag="ride_summary_battery";orientation=HORIZONTAL;isBaselineAligned=false
                listOf(AnalysisMetric.SHIFT_BATTERY_LEVEL,AnalysisMetric.BATTERY_LEVEL,AnalysisMetric.BATTERY_TEMPERATURE).forEach { metric ->
                    val saved=completion.shiftBatteryAtEnd?.let { values+(AnalysisMetric.SHIFT_BATTERY_LEVEL to it.toDouble()) } ?: values
                    addView(AnalysisMetricTile(context,themes,listOf(metric),saved,batteryWarning=metric!=AnalysisMetric.BATTERY_TEMPERATURE,showIcon=false),LayoutParams(0,-1,1f))
                }
            },LayoutParams(-1,-2))
        }
    }
    fun show(analysis:RouteAnalysis) {
        entry=analysis.entry;details.isEnabled=true;showMetrics(analysis.metrics);problem.visibility=GONE;updateStatus()
    }
    fun loadingFailed() { problem.setText(R.string.ride_summary_load_failed) }
    fun finishNoteOutside(x:Float,y:Float) {
        val editor=feedback?.note ?: return
        if(!editor.hasFocus()) return
        val bounds=Rect();editor.getGlobalVisibleRect(bounds)
        val root=IntArray(2);editor.rootView.getLocationOnScreen(root);bounds.offset(root[0],root[1])
        if(bounds.contains(x.toInt(),y.toInt())) return
        editor.clearFocus();requestFocus()
        context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(editor.windowToken,0)
    }
    private fun updateStatus() {
        val warning=RideSummaryStatus.warnings(context,completion)
        status.text=warning;status.visibility=if(warning.isEmpty()) GONE else VISIBLE
        val items=buildList {
            if((completion as? RideCompletion.Completed)?.destinations?.contains(RecordingDestination.LOCAL_FOLDER)==true)
                add(SummaryTransfer(RouteTransferTarget.LOCAL,context.getString(R.string.ride_summary_local_folder),UploadState.WAITING,planned=true))
            addAll(RideSummaryStatus.targets(completion).map { target ->
                SummaryTransfer(target,context.getString(if(target==RouteTransferTarget.WEBDAV) R.string.ride_backup_webdav else target.title),UploadState.WAITING,planned=true)
            })
        }
        transfers.show(items);transfers.visibility=if(items.isEmpty()) GONE else VISIBLE
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
    fun warnings(context:Context,completion:RideCompletion):String = buildList {
        if(completion !is RideCompletion.Completed) { add(RecordingStatusText.text(context,RecordingUpdate.Finished(completion)));return@buildList }
        if(RecordingWarning.INCOMPLETE_RECORDING in completion.warnings) add(context.getString(R.string.recording_incomplete).trim(' ','·'))
        if(RecordingWarning.ELEVATION_CORRECTION_FAILED in completion.warnings) add(context.getString(R.string.elevation_auto_failed))
    }.joinToString("\n")
}
