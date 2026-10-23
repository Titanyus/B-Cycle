package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

internal enum class RouteTransferTarget(val title:Int,val icon:BicycleIcon) {
    LOCAL(R.string.route_transfer_local,BicycleIcon.Save),
    WEBDAV(R.string.data_cloud,BicycleIcon.WebDav),
    INTERVALS(R.string.intervals_title,BicycleIcon.Intervals),
    DREEVE(R.string.dreeve_title,BicycleIcon.Dreeve),
    STRAVA(R.string.strava_title,BicycleIcon.Strava);
    fun enabled(c:Context)=when(this) {
        LOCAL->true // Manual export remains available independently of automatic local saving.
        WEBDAV->RouteExportSettings(c).read().webdav
        INTERVALS->IntervalsSettings(c).read().enabled
        DREEVE->ActivityServiceSettings(c,ActivityService.DREEVE).read().enabled
        STRAVA->ActivityServiceSettings(c,ActivityService.STRAVA).read().enabled
    }
    fun status(c:Context,entry:ManagedRoute)=when(this) {
        LOCAL->RouteUploadStatus(if(RouteLocalSaves.completed(c,entry.id,entry.fileStamp)) UploadState.UPLOADED else UploadState.NOT_UPLOADED)
        WEBDAV->RouteUploads.state(c,entry.id,entry.fileStamp)
        INTERVALS->IntervalsUploads.state(c,entry.id,entry.fileStamp)
        DREEVE->ActivityServiceUploads.dreeve.state(c,entry.id,entry.fileStamp)
        STRAVA->ActivityServiceUploads.strava.state(c,entry.id,entry.fileStamp)
    }
    fun retry(c:Context,id:String) { when(this) {
        LOCAL->error("Local export uses MapState.exportRoute")
        WEBDAV->RouteUploads.retry(c,id)
        INTERVALS->IntervalsUploads.retry(c,id)
        DREEVE->ActivityServiceUploads.dreeve.retry(c,id)
        STRAVA->ActivityServiceUploads.strava.retry(c,id)
    } }
}
internal data class RouteTransferRow(val target:RouteTransferTarget,val text:Int,val canStart:Boolean,val completed:Boolean=false)

/** Per-target receipts only. No aggregate success, file hashing or network work on the UI thread. */
internal object RouteTransferRows {
    fun read(context:Context,entry:ManagedRoute,busy:Boolean)=RouteTransferTarget.entries.map { target ->
        runCatching {
            when {
                target==RouteTransferTarget.DREEVE && (entry.origin==TrackOrigin.IMPORTED || entry.format!="FIT") && RideServiceLinks.links(context,entry.id).any { it.service==RideService.DREEVE && it.accountId==ActivityServiceSettings(context,ActivityService.DREEVE).read().identity(ActivityService.DREEVE) }->RouteTransferRow(target,R.string.dreeve_linked,false,true)
                !target.enabled(context)->RouteTransferRow(target,R.string.route_transfer_off,false)
                !entry.locallySaved->RouteTransferRow(target,R.string.route_transfer_missing,false)
                target!=RouteTransferTarget.LOCAL && (entry.format !in setOf("FIT","GPX") || entry.origin!=TrackOrigin.RECORDED ||
                    entry.format=="GPX" && (entry.details.durationMs==null || entry.details.durationMs<=0))->RouteTransferRow(target,R.string.route_transfer_ineligible,false)
                else->{
                    val state=target.status(context,entry).state
                    RouteTransferRow(target,when(state) {
                        UploadState.NOT_UPLOADED->if(target==RouteTransferTarget.LOCAL) R.string.route_transfer_local_ready else R.string.route_transfer_ready
                        UploadState.WAITING->R.string.route_transfer_waiting
                        UploadState.UPLOADING->if(target==RouteTransferTarget.DREEVE) R.string.dreeve_upload_processing else R.string.route_transfer_uploading
                        UploadState.UPLOADED->R.string.route_transfer_done
                        UploadState.FAILED->R.string.route_transfer_failed
                    },!busy && (state in setOf(UploadState.NOT_UPLOADED,UploadState.FAILED) || target==RouteTransferTarget.LOCAL),state==UploadState.UPLOADED)
                }
            }
        }.getOrElse { RouteTransferRow(target,R.string.route_transfer_config_error,false) }
    }.map { row ->
        val service=when(row.target) { RouteTransferTarget.DREEVE->RideService.DREEVE;RouteTransferTarget.STRAVA->RideService.STRAVA;RouteTransferTarget.INTERVALS->RideService.INTERVALS;else->null }
        if(!row.completed || service==null) row else when(RideNames.pending(context,entry.id,service)) {
            "pending"->row.copy(text=R.string.route_name_pending)
            "failed"->row.copy(text=R.string.route_name_failed)
            else->row
        }
    }
}

/** Anchored menu stays open while individual jobs update. Listeners exist only while visible. */
internal class RouteTransferMenu(private val context:Context,private val themes:ThemeManager,anchor:View,
    private val entry:()->ManagedRoute?,private val busy:()->Boolean,
    private val choose:(RouteTransferTarget)->Unit,configure:()->Unit,
    private val recheck:(RouteTransferTarget)->Unit={ choose(it) }) {
    private val handler=Handler(Looper.getMainLooper())
    private val preferences=listOf("route-export","intervals-settings","activity-service-dreeve","activity-service-strava",
        "route-uploads","intervals-uploads","activity-uploads-dreeve","activity-uploads-strava","route-local-saves","ride-service-links","ride-names")
        .map { context.getSharedPreferences(it,0) }
    private var rows=emptyList<RouteTransferRow>()
    private var attempt:RouteTransferTarget?=null
    private var sawBusy=false
    private var checking=false
    private val failedStarts=mutableSetOf<RouteTransferTarget>()
    private var themeSubscription:ThemeSubscription?=null
    private val changed=Runnable { refresh() }
    private val listener=SharedPreferences.OnSharedPreferenceChangeListener { _,_->handler.removeCallbacks(changed);handler.post(changed) }
    private val adapter=object:BaseAdapter() {
        override fun getCount()=rows.size
        override fun getItem(position:Int)=rows[position]
        override fun getItemId(position:Int)=rows[position].target.ordinal.toLong()
        override fun areAllItemsEnabled()=false
        override fun isEnabled(position:Int)=rows[position].canStart || canRecheck(rows[position])
        override fun getView(position:Int,convertView:View?,parent:ViewGroup):View {
            val row=rows[position];val tokens=themes.active.tokens
            return LinearLayout(context).apply {
                tag="route_transfer_${row.target.name}";gravity=Gravity.CENTER_VERTICAL
                minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
                setPadding(dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.verticalPadding),dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.verticalPadding))
                setBackgroundColor(tokens.tertiary)
                addView(ImageView(context).apply { setImageDrawable(BicycleIcons.drawable(context,row.target.icon,tokens.foreground));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(dp(BicycleDesign.iconSize),dp(BicycleDesign.iconSize)))
                addView(LinearLayout(context).apply {
                    orientation=LinearLayout.VERTICAL
                    addView(TextView(context).apply { text=context.getString(row.target.title);textSize=BicycleDesign.bodySp;setTextColor(tokens.foreground) })
                    addView(TextView(context).apply { tag="route_transfer_status_${row.target.name}";text=context.getString(row.text);textSize=BicycleDesign.sensorStatusSp;setTextColor(tokens.secondaryTextColor) })
                },LinearLayout.LayoutParams(0,-2,1f).apply { marginStart=dp(BicycleDesign.controlGap) })
                if(row.completed) addView(ImageView(context).apply {
                    setImageDrawable(BicycleIcons.drawable(context,BicycleIcon.Check,tokens.foreground));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },LinearLayout.LayoutParams(dp(BicycleDesign.iconSize),dp(BicycleDesign.iconSize)))
                contentDescription=context.getString(row.target.title)+", "+context.getString(row.text)
                if(row.completed) {
                    contentDescription=contentDescription.toString()+", "+context.getString(R.string.route_transfer_recheck_hint)
                    TransferHold.bind(this,{ popup.isShowing && !busy() && rows.any { it.target==row.target && canRecheck(it) } },
                        click={ if(row.canStart) start(row,false) },hold={ start(row,true) })
                }
                alpha=if(row.text==R.string.route_transfer_off) .6f else 1f
            }
        }
    }
    val configureButton=BicycleComponents.action(context,themes,context.getString(R.string.route_transfer_configure)) { dismiss();configure() }.apply {
        tag="route_transfer_configure";BicycleDialogStyle.actionIcon(this,themes,BicycleIcon.Advanced)
    }
    val popup:ListPopupWindow=ListPopupWindow(context).apply {
        anchorView=anchor;isModal=true
        width=minOf(dp(BicycleDesign.popupWidth),context.resources.displayMetrics.widthPixels-dp(BicycleDesign.horizontalPadding*2))
        promptPosition=ListPopupWindow.POSITION_PROMPT_BELOW
        setPromptView(configureButton)
        setAdapter(adapter)
        setOnItemClickListener { _,_,position,_->
            val row=rows.getOrNull(position) ?: return@setOnItemClickListener
            if(!row.canStart || busy()) return@setOnItemClickListener
            start(row,false)
        }
        setOnDismissListener {
            preferences.forEach { it.unregisterOnSharedPreferenceChangeListener(listener) }
            themeSubscription?.close();themeSubscription=null;handler.removeCallbacks(changed)
        }
    }
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    private fun canRecheck(row:RouteTransferRow)=row.completed && !busy() && row.target.enabled(context)
    private fun start(row:RouteTransferRow,check:Boolean) {
        if(busy() || if(check) !canRecheck(row) else !row.canStart) return
        failedStarts.remove(row.target);attempt=row.target;sawBusy=false;checking=check
        if(check) recheck(row.target) else choose(row.target)
        refresh()
    }
    fun show() {
        refresh()
        preferences.forEach { it.registerOnSharedPreferenceChangeListener(listener) }
        themeSubscription=themes.observe {
            popup.setBackgroundDrawable(BicycleComponents.dropdownBackground(context,it.tokens));adapter.notifyDataSetChanged()
        }
        popup.show()
    }
    fun dismiss() { popup.dismiss() }
    fun refresh() {
        val current=entry() ?: return dismiss()
        val running=busy()
        attempt?.let { target ->
            if(running) sawBusy=true
            else if(sawBusy) {
                val state=runCatching { target.status(context,current).state }.getOrDefault(UploadState.FAILED)
                if(state==UploadState.NOT_UPLOADED) failedStarts.add(target)
                attempt=null;sawBusy=false
            }
        }
        rows=RouteTransferRows.read(context,current,running).map { row ->
            when {
                row.target==attempt && running->row.copy(text=if(checking && row.target !in setOf(RouteTransferTarget.LOCAL,RouteTransferTarget.STRAVA)) R.string.route_transfer_checking else R.string.route_transfer_uploading,canStart=false)
                row.target in failedStarts && !row.completed->row.copy(text=R.string.route_transfer_failed)
                else->row
            }
        }
        adapter.notifyDataSetChanged()
    }
}
