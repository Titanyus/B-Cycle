package de.bcycle.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Gravity
import android.view.WindowInsets
import android.view.Window
import android.view.WindowManager
import android.widget.*
import java.io.IOException

/** Native Views remain the UI; the runtime also survives while the recording service is active. */
class MainActivity : Activity() {
    private lateinit var runtime: RideRuntime
    private var dailyHrvPrompt:HrvDailyPrompt?=null
    private var sensorSetupPrompt:SensorSetupPrompt?=null
    private var sensorSetupProfileId:String?=null
    private var sensorSetupPosted=false
    private val sensorSetupLaunch by lazy { SensorSetupLaunch(this) }
    private var dashboardStatusReport:String?=null
    private lateinit var dashboardStatus:DashboardStatusStrip
    private lateinit var dashboardStatusMonitor:DashboardStatusMonitor
    private var statusForeground=false
    private val statusPreferences=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,key ->
        if(statusForeground && (key==DashboardFullscreen.key || key==DashboardStatusConfig.key)) updateDashboardFullscreen()
    }
    private val statusMessagesPreferences=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,_ ->
        if(statusForeground && ::dashboardStatus.isInitialized) { dashboardStatus.reloadMessenger();updateDashboardFullscreen() }
    }
    private lateinit var dashboardRoot:View
    private lateinit var cornerBikeLight:ImageView
    private lateinit var dashboardHost: FrameLayout
    private lateinit var dashboard: DashboardView
    private var hrvObservation:AutoCloseable?=null
    private var hrvWasBusy=false
    private lateinit var startButton: DashboardRecordButton
    private lateinit var noMapEditorFinish: Button
    private lateinit var drinkReminderPanel:DrinkReminderPanel
    private lateinit var shiftBatteryWarningPanel:ShiftBatteryWarningPanel
    private lateinit var messages: MessageNotificationPanel
    private var notificationSettings: MessageNotificationSettingsView?=null
    private val messageObserver: (IncomingMessage?)->Unit = { message ->
        if(message==null || (dialogs.empty && !dashboardEditing)) messages.receive(message)
    }
    private lateinit var mapPanel: MapPanel
    private lateinit var dashboardArea: LinearLayout
    private var routeManager: RouteManagerView? = null
    private var statisticsView:RideStatisticsView?=null
    private var orphanProfilesView:OrphanProfilesView?=null
    private var orphanProfilesScreen:AlertDialog?=null
    private var routeOrphanPending:RouteManagerView?=null
    private var routeImportButton: BicycleFooterAction? = null
    private var routePopup: ListPopupWindow? = null
    private var routeSignature = ""
    private val settingsWriter = java.util.concurrent.Executors.newSingleThreadExecutor { task -> Thread(task,"Bicycle-settings") }
    private var exportDraft: RouteExportConfig? = null
    private var exportPasswordDraft: String? = null
    private var pendingBackupExport: java.io.File? = null
    private var passwordOperation=0
    private var dataSettingsScreen: AlertDialog? = null
    private var localExportView: RouteExportView? = null
    private var pendingBikePhotoSlot:Int?=null
    private var activityServiceView:ActivityServiceView?=null
    private var pendingMapRoute: String? = null
    private var bikePicker:BikeQuickPicker?=null
    private var recordButtonSignature=""
    private var recordingIndicator:RecordingButtonIndicator?=null
    private lateinit var dashboardViewport:DashboardPageViewport
    private var dashboardPage = 0
    private val dashboardPages get() = DashboardPages(this,preferences)
    private var mapFullscreen = false
    private var controlsVisible = true
    private var displayedMapMessage = ""
    private val dashboardKey get() = if (dashboardPage == 0) "dashboard" else "dashboard_$dashboardPage"
    private val templateKey get() = "dashboardTemplate_$dashboardPage"
    private fun currentTemplate() = DashboardTemplate.decode(preferences.getString(templateKey, DashboardTemplate.entries[dashboardPage].name)).forBike(runtime.bikes.current().type)
    private var dashboardLayout = DashboardLayout.defaults
    private var dashboardDataVisibility = DashboardDataVisibility()
    private var dashboardTrendIndicators=TrendIndicatorConfig()
    private var dashboardGraphs = GraphOverlayConfig()
    private var dashboardTrainingHelp=DashboardTrainingHelp()
    private val trainingPresentation = TrainingPresentation()
    private var trainingSnapshot = TrainingSnapshot()
    private val mapBack = android.window.OnBackInvokedCallback { setMapFullscreen(false) }
    private var mapBackRegistered = false
    private fun updateMapBack() {
        if (mapFullscreen == mapBackRegistered) return
        if (mapFullscreen) onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,mapBack)
        else onBackInvokedDispatcher.unregisterOnBackInvokedCallback(mapBack)
        mapBackRegistered=mapFullscreen
    }
    internal var dashboardEditing = false; private set
    private val editorHandler=android.os.Handler(android.os.Looper.getMainLooper())
    private val editorTimeout=Runnable {
        if(dashboardEditing && runtime.visible) {
            finishDashboardEditor()
            if(!dashboardEditing) dialogs.closeAll() else restartEditorTimeout()
        }
    }
    private fun restartEditorTimeout() {
        editorHandler.removeCallbacks(editorTimeout)
        if(dashboardEditing && runtime.visible) editorHandler.postDelayed(editorTimeout,if(picker?.isShowing==true) 12000L else 5000L)
    }
    override fun onUserInteraction() { super.onUserInteraction();restartEditorTimeout() }
    internal fun setDashboardEditing(value: Boolean) {
        dashboardEditing = value
        dashboard.setEditing(value); mapPanel.setEditing(value)
        if(::noMapEditorFinish.isInitialized) noMapEditorFinish.visibility=if(value && !currentTemplate().hasMap) View.VISIBLE else View.GONE
        if (value && mapFullscreen) setMapFullscreen(false)
        restartEditorTimeout()
    }
    private fun finishDashboardEditor() {
        // commit waits for the existing field/graph apply writes; retain EDIT on a reported disk failure.
        if (!preferences.edit().commit()) {
            BicycleFeedback.show(this, R.string.dashboard_editor_save_failed, long = true); return
        }
        setDashboardEditing(false)
    }
    private fun enterDashboardEditor() {
        dialogs.closeAll(); setDashboardEditing(true)
    }
    private var picker: AlertDialog? = null
    private var menu: AlertDialog? = null
    private data class DetailSession(val id: String, val screen: AlertDialog, val content: RouteAnalysisView, val footer: LinearLayout, var analysis: RouteAnalysis? = null, var signature: String = "", var transfers:RouteTransferMenu?=null)
    private var routeDetail: DetailSession? = null
    private var sensorManager: AlertDialog? = null
    private var bikesManager: BikesView? = null
    private val expandedFitSources = mutableSetOf<String>()
    private var scan: BikeSensorScanner? = null
    private var pendingSensor: String? = null
    private var pendingStart = false
    private var pendingReport: String? = null
    private var displayedRecordingStatus: String? = null
    private var rideSummaryPosted=false
    private val observer: () -> Unit = { render(); refreshSensorRows(); refreshRouteRows();refreshOrphanProfiles();refreshStatistics() }
    private val preferences get() = runtime.preferences
    private val settingsContext by lazy { android.view.ContextThemeWrapper(this, R.style.SettingsDialogTheme) }
    private val settingsThemes get() = runtime.themes.settings
    private val dialogs = DialogNavigation()
    private fun dialog(fullscreen: Boolean = true):BicycleDialogBuilder {
        closeSensorSetupPrompt()
        return BicycleDialogBuilder(this,settingsThemes,dialogs,fullscreen)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(text: String, size: Float = BicycleDesign.bodySp) = BicycleComponents.text(settingsContext, settingsThemes, text).apply {
        isAccessibilityHeading = size >= BicycleDesign.inputSp
        BicycleDialogStyle.control(this, settingsThemes)
    }
    private fun column() = LinearLayout(settingsContext).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding))
    }
    private fun button(text: String, icon: BicycleIcon? = null, action: () -> Unit) = BicycleComponents.action(settingsContext, settingsThemes, text, action).apply {
        BicycleDialogStyle.control(this, settingsThemes)
        icon?.let { BicycleDialogStyle.actionIcon(this, settingsThemes, it) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = RideRuntime.get(this)
        pendingBikePhotoSlot=savedInstanceState?.getInt("pendingBikePhotoSlot",-1)?.takeIf { it >= 0 && runtime.bikes.all().any { bike -> bike.slot == it } }
        pendingReport = savedInstanceState?.getString("pendingReport")
        pendingBackupExport = savedInstanceState?.getString("settingsBackupPendingFile")?.takeIf { it.matches(Regex("settings-export-[a-f0-9-]{36}\\.zip")) }
            ?.let { java.io.File(cacheDir,it) }?.takeIf { it.isFile }
        dashboardPage = dashboardPages.selected()
        dashboardLayout = DashboardLayout.decode(preferences.getString(dashboardKey, null))
        dashboardGraphs=GraphOverlayConfig.read(preferences,dashboardPage); dashboardTrainingHelp=DashboardTrainingHelp.read(preferences,dashboardPage); dashboardDataVisibility=DashboardDataVisibility.read(preferences,dashboardPage); dashboardTrendIndicators=TrendIndicatorSettings.read(preferences)
        mapFullscreen = savedInstanceState?.getBoolean("mapFullscreen", false) ?: false
        @Suppress("DEPRECATION") // Explicit edge-to-edge remains necessary on Android 13–14.
        window.setDecorFitsSystemWindows(false)
        window.attributes=window.attributes.apply { layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.TRANSPARENT) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val custom=::dashboardStatus.isInitialized && dashboardStatus.visibility==View.VISIBLE
            val top=if(custom) maxOf(bars.top,insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top,dp(BicycleDesign.dashboardStatusMinHeight)) else bars.top
            view.setPadding(bars.left,top,bars.right,bars.bottom)
            if(::cornerBikeLight.isInitialized) {
                val p=cornerBikeLight.layoutParams as FrameLayout.LayoutParams
                p.topMargin=top
                p.marginEnd=bars.right+dp(BicycleDesign.horizontalPadding)
                cornerBikeLight.layoutParams=p
            }
            if(::dashboardStatus.isInitialized) {
                val params=dashboardStatus.layoutParams
                if(params.height!=top) { params.height=top;dashboardStatus.layoutParams=params }
                dashboardStatus.insets(bars.left,bars.right,insets.displayCutout?.boundingRects.orEmpty(),insets.privacyIndicatorBounds)
            }
            insets
        }
        dashboardRoot=root
        val host=FrameLayout(this).apply { tag="dashboard_host";setBackgroundColor(Color.BLACK);addView(root,FrameLayout.LayoutParams(-1,-1)) }
        dashboardHost=host
        dashboardStatus=DashboardStatusStrip(this,runtime.themes).apply { visibility=View.GONE }
        host.addView(dashboardStatus,FrameLayout.LayoutParams(-1,dp(BicycleDesign.dashboardStatusMinHeight),Gravity.TOP))
        cornerBikeLight=ImageView(this).apply {
            tag="dashboard_bike_light";visibility=View.GONE
            contentDescription=getString(R.string.ui_bike_light_on)
            setImageDrawable(BicycleIcons.drawable(this@MainActivity,BicycleIcon.Light,BicycleDesign.bikeLightStatusColor))
        }
        host.addView(cornerBikeLight,FrameLayout.LayoutParams(dp(BicycleDesign.bikeLightStatusWidth),dp(BicycleDesign.bikeLightStatusHeight),Gravity.TOP or Gravity.END).apply {
            topMargin=0;marginEnd=dp(BicycleDesign.horizontalPadding)
        })
        dashboardStatusMonitor=DashboardStatusMonitor(this) { dashboardStatus.show(it) }
        setContentView(host)
        dashboard = DashboardView(this) { chooseField(it) }
        dashboard.bindEditorTheme(runtime.themes)
        dashboard.activateEditor = { setDashboardEditing(true) }
        dashboard.graphWidthChanged={ runtime.liveGraphs.width=it }
        updateMapBack()
        dashboard.changePage = ::changeDashboard
        dashboard.setTemplate(currentTemplate())
        mapPanel = MapPanel(this, runtime, { setMapFullscreen(!mapFullscreen) }, ::enableGps, ::openMapLink)
        mapPanel.cameraVisible = { dialogs.empty && currentTemplate().hasMap }
        mapPanel.finishEditor = ::finishDashboardEditor
        mapPanel.selectMapField = { chooseField(it, mapField = true) }
        dashboardArea = object : LinearLayout(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val available = View.MeasureSpec.getSize(heightMeasureSpec) - if (controlsVisible) 0 else dp(58)
                (dashboard.layoutParams as? LayoutParams)?.let {
                    it.height = dashboard.heightForViewport(View.MeasureSpec.getSize(widthMeasureSpec), available.coerceAtLeast(0)); it.weight = 0f
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }.apply { orientation = LinearLayout.VERTICAL }
        dashboardArea.addView(dashboard, LinearLayout.LayoutParams(-1, 0, currentTemplate().dataFraction))
        dashboardArea.addView(mapPanel, LinearLayout.LayoutParams(-1, 0, 1f - currentTemplate().dataFraction))
        dashboard.visibility = if (mapFullscreen) View.GONE else View.VISIBLE
        if(!currentTemplate().hasMap) { mapFullscreen=false;dashboard.visibility=View.VISIBLE;mapPanel.visibility=View.GONE }
        mapPanel.showFullscreen(mapFullscreen)
        dashboardViewport=DashboardPageViewport(this).apply { setPage(dashboardArea) }
        root.addView(dashboardViewport,LinearLayout.LayoutParams(-1,0,1f))
        val toolbar = LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL;gravity=Gravity.BOTTOM;tag="ride_toolbar"
            styleBicycleFooter(this,runtime.themes,dashboard=true)
            BicycleComponents.bindTheme(this,runtime.themes) { tokens ->
                background=android.graphics.drawable.InsetDrawable(android.graphics.drawable.ColorDrawable(tokens.tertiary),0,dp(BicycleDesign.dashboardRecordCapHeight),0,0)
                setPadding(0,0,0,0)
            }
        }
        fun toolbarButton(title: String, role: ThemeRole = ThemeRole.PRIMARY, action: () -> Unit) = Button(this).apply {
            text = title; isAllCaps = false; setOnClickListener { action() }
            textSize = 20f; minWidth = 0; minHeight = 0
            setPadding(dp(4), 0, dp(4), 0)
            BicycleComponents.bindTheme(this, runtime.themes) { tokens ->
                setTextColor(tokens.foreground)
                backgroundTintList = null
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(tokens.color(role))
                }
            }
        }
        startButton = DashboardRecordButton(this,runtime.themes).apply { setOnClickListener {
            if (runtime.recording) startService(Intent(this@MainActivity, RecordingService::class.java).setAction(RecordingService.STOP)) else startRide()
        } }
        startButton.apply {
            tag="record_bike_button";setSingleLine();ellipsize=android.text.TextUtils.TruncateAt.END
            setOnLongClickListener {
                if(runtime.recording || runtime.saving) true else {
                    bikePicker?.dismiss()
                    val popup=BikeQuickPicker(this,runtime.themes,runtime.bikes.all(),runtime.bikes.activeSlot) { slot ->
                        if(!runtime.recording && !runtime.saving) { runtime.switchBike(slot);render() }
                    }
                    bikePicker=popup
                    popup.setOnDismissListener { if(bikePicker===popup) bikePicker=null }
                    popup.show(this);true
                }
            }
        }
        toolbar.addView(android.widget.ImageButton(this).apply {
            contentDescription = getString(R.string.ui_routes_open); tag = "dashboard_routes"
            setImageResource(BicycleIcon.Route.resource)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setPadding(dp(17), dp(17), dp(17), dp(17))
            setOnClickListener { routeSettings() }
            setOnLongClickListener { showHrvDailyPrompt();true }
            accessibilityDelegate=object:View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host:View,info:android.view.accessibility.AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host,info)
                    info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(
                        android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK,getString(R.string.hrv_daily_open)))
                }
            }
            BicycleComponents.bindTheme(this, runtime.themes) { tokens ->
                imageTintList = android.content.res.ColorStateList.valueOf(tokens.foreground)
                backgroundTintList = null
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(tokens.tertiary)
                }
            }
        }, LinearLayout.LayoutParams(0, dp(BicycleDesign.dashboardToolbarHeight), BicycleDesign.dashboardToolbarActionWeight))
        toolbar.addView(startButton, LinearLayout.LayoutParams(0, dp(BicycleDesign.dashboardToolbarHeight+BicycleDesign.dashboardRecordCapHeight), BicycleDesign.dashboardToolbarMainWeight))
        toolbar.addView(toolbarButton("☰", ThemeRole.TERTIARY) { showMenu() }.apply { contentDescription = getString(R.string.ui_menu_open) }, LinearLayout.LayoutParams(0, dp(BicycleDesign.dashboardToolbarHeight), BicycleDesign.dashboardToolbarActionWeight))
        root.addView(toolbar,LinearLayout.LayoutParams(-1,-2).apply { topMargin=-dp(BicycleDesign.dashboardRecordCapHeight) })
        // Keep the data area height stable; all reclaimed toolbar space goes to the map.
        mapPanel.controlsVisibilityChanged = { visible ->
            controlsVisible = visible || !currentTemplate().hasMap
            toolbar.visibility = if (controlsVisible) View.VISIBLE else View.GONE
            dashboard.showFooter(controlsVisible)
            dashboardArea.requestLayout()
        }
        for (i in 0 until toolbar.childCount) toolbar.getChildAt(i).setOnTouchListener { _, event ->
            if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) mapPanel.holdControls()
            if (event.actionMasked == android.view.MotionEvent.ACTION_UP || event.actionMasked == android.view.MotionEvent.ACTION_CANCEL) mapPanel.revealControls()
            false
        }
        noMapEditorFinish=BicycleComponents.action(this,runtime.themes,getString(R.string.dashboard_editor_finish),false) { finishDashboardEditor() }.apply {
            tag="dashboard_editor_finish_no_map";visibility=View.GONE
            setPadding(dp(BicycleDesign.horizontalPadding),paddingTop,dp(BicycleDesign.horizontalPadding),paddingBottom)
        }
        host.addView(noMapEditorFinish,FrameLayout.LayoutParams(-2,-2,android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL).apply { bottomMargin=dp(66) })
        drinkReminderPanel=DrinkReminderPanel(this,runtime.themes)
        host.addView(drinkReminderPanel,FrameLayout.LayoutParams(-1,-2,Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            marginStart=dp(BicycleDesign.horizontalPadding);marginEnd=dp(BicycleDesign.horizontalPadding)
        })
        shiftBatteryWarningPanel=ShiftBatteryWarningPanel(this,runtime.themes)
        host.addView(shiftBatteryWarningPanel,FrameLayout.LayoutParams(-1,-2,Gravity.CENTER).apply {
            marginStart=dp(BicycleDesign.horizontalPadding);marginEnd=dp(BicycleDesign.horizontalPadding)
        })
        drinkReminderPanel.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_->placeDrinkReminder() }
        mapPanel.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_->placeDrinkReminder() }
        dashboardArea.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_->placeDrinkReminder() }
        host.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_->placeDrinkReminder() }
        messages=MessageNotificationPanel(this,runtime.themes) {
            val mapLocation=IntArray(2);val hostLocation=IntArray(2)
            mapPanel.getLocationInWindow(mapLocation);host.getLocationInWindow(hostLocation)
            val position=MessageNotificationSettings(this).position
            val edge=if(position==MessagePosition.CENTER) 0 else dp(BicycleDesign.dashboardMessageEdgeInset)
            val available=(host.height-root.paddingTop-root.paddingBottom-2*edge).coerceAtLeast(0)
            if(position==MessagePosition.BOTTOM && currentTemplate().hasMap)
                (host.height-root.paddingBottom-edge-(mapLocation[1]-hostLocation[1])).coerceIn(0,available)
            else minOf(dp(320),available)
        }
        mapPanel.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_ -> if(messages.visibility==View.VISIBLE) messages.requestLayout() }
        host.addView(messages,FrameLayout.LayoutParams(-1,-2,android.view.Gravity.BOTTOM).apply {
            marginStart=dp(BicycleDesign.controlGap);marginEnd=dp(BicycleDesign.controlGap)
        })
        fun placeMessage() {
            val params=messages.layoutParams as FrameLayout.LayoutParams
            val position=MessageNotificationSettings(this).position
            val edge=if(position==MessagePosition.CENTER) 0 else dp(BicycleDesign.dashboardMessageEdgeInset)
            params.gravity=when(position) {
                MessagePosition.BOTTOM -> android.view.Gravity.BOTTOM
                MessagePosition.CENTER -> android.view.Gravity.CENTER
                MessagePosition.TOP -> android.view.Gravity.TOP
            }
            params.topMargin=root.paddingTop+edge;params.bottomMargin=root.paddingBottom+edge
            messages.layoutParams=params
        }
        messages.positionChanged=::placeMessage
        host.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_ ->
            val finishParams=noMapEditorFinish.layoutParams as FrameLayout.LayoutParams
            val bottom=root.paddingBottom+dp(66)
            if(finishParams.bottomMargin!=bottom) { finishParams.bottomMargin=bottom;noMapEditorFinish.layoutParams=finishParams }
            if(messages.visibility==View.VISIBLE) placeMessage()
        }
        render()
        activityServiceCallback(intent)
    }
    override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent);setIntent(intent);activityServiceCallback(intent)
    }
    private var shownProfileRevision=0
    private fun render() {
        if (!::dashboard.isInitialized) return
        if(::dashboardStatusMonitor.isInitialized) dashboardStatusMonitor.battery(runtime.batterySnapshot.level(BatterySource.PHONE))
        if(::dashboardStatus.isInitialized && (dashboardStatus.bikeLightOn!=(DashboardFullscreen.enabled(preferences) && runtime.currentBikeLight==true) || cornerBikeLight.visibility!=(if(!DashboardFullscreen.enabled(preferences) && dialogs.empty && runtime.currentBikeLight==true) View.VISIBLE else View.GONE))) updateDashboardFullscreen()
        if(::mapPanel.isInitialized && shownProfileRevision!=runtime.profileRevision) {
            shownProfileRevision=runtime.profileRevision
            dashboard.resetTrends()
            mapPanel.refreshProfileSettings()
            changeDashboard(0)
            updateDashboardFullscreen()
            return
        }
        val gps = runtime.gpsValues
        val bike=runtime.bikes.current()
        val status=getString(if(runtime.recording) R.string.ui_record_stop else if(runtime.saving) R.string.ui_record_saving else R.string.ui_record_start)
        val buttonLabel=if(runtime.saving) status else bike.name
        val buttonSignature="${bike.slot}:${bike.type}:$buttonLabel:${runtime.recording}:${runtime.themes.active.tokens.foreground}"
        if(buttonSignature!=recordButtonSignature) {
        recordButtonSignature=buttonSignature
        recordingIndicator?.stop();recordingIndicator=null
        startButton.recordIcon=if(runtime.recording)
            RecordingButtonIndicator(this@MainActivity,runtime.themes.active.tokens.foreground) { startButton.invalidate() }.also { recordingIndicator=it }
            else BicycleIcons.drawable(this@MainActivity,BicycleIcon.RecordReady,runtime.themes.active.tokens.foreground)
        startButton.text=android.text.SpannableString("\uFFFC $buttonLabel").apply {
            val drawable=BicycleIcons.drawable(this@MainActivity,BikeTypeIcons.forType(bike.type),runtime.themes.active.tokens.foreground)
            drawable.setBounds(0,0,dp(20),dp(20))
            setSpan(android.text.style.ImageSpan(drawable,android.text.style.ImageSpan.ALIGN_CENTER),0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        }
        recordingIndicator?.setAnimating(runtime.visible && dialogs.empty)
        startButton.contentDescription=getString(R.string.record_bike_description,status,bike.name)
        startButton.isEnabled = (!runtime.saving || runtime.recording) && !runtime.hrv.busy
        val message = runtime.recordingStatus
        if (message != displayedRecordingStatus) {
            displayedRecordingStatus = message
            val update=runtime.recordingUpdate
            if (update?.let(RecordingStatusText::showFeedback) == true && !(update is RecordingUpdate.Finished && update.result.rideId!=null))
                BicycleFeedback.show(this, message, long = true)
        }
        offerRideSummary()
        val trainingNow=android.os.SystemClock.elapsedRealtime()
        trainingSnapshot=trainingPresentation.update(runtime.trainingInputs(trainingNow),runtime.riderProfiles.read(),trainingNow)
        dashboard.showTraining(dashboardTrainingHelp.sources(dashboardLayout,dashboardDataVisibility),trainingSnapshot,dashboardTrainingHelp.mode==TrainingHelpMode.ZONE_TWO)
        dashboard.showDataVisibility(dashboardDataVisibility)
        dashboard.showTrendIndicators(dashboardTrendIndicators)
        runtime.liveGraphs.rolling=dashboardGraphs.windows.indices.any { dashboard.template.supportsGraph(it) && dashboardGraphs.slots[it].isNotEmpty() && dashboardGraphs.windows[it]!=GraphWindow.FULL }
        dashboard.showGraphs(dashboardGraphs,runtime.liveGraphs.frame)
        dashboard.show(dashboardLayout, runtime.dashboardBikeData, gps, runtime.ride.bike.distanceM, runtime.pulseValues,runtime.trendSnapshot)
        // Top mode light includes the system/cutout inset; content keeps its safe padding.
        if(dashboardHost.background!==dashboard.modeGlow) {
            dashboard.background=null;dashboardHost.background=dashboard.modeGlow
        }
        dashboard.showModeGlow(true, runtime.currentAssistMode, runtime.bikeData.assistNames, runtime.bikeBatteryWarningLevel)
        val drinkSettings=DrinkReminderSettings.read(preferences)
        val drinkVisible=runtime.visible && dialogs.empty && !dashboardEditing
        val warningEnabled=runtime.shiftEnabled
        if(!drinkVisible || !runtime.recording || !warningEnabled) shiftBatteryWarningPanel.clear()
        runtime.shiftBatteryWarning.take(runtime.recording,drinkVisible,warningEnabled)?.let { level ->
            drinkReminderPanel.clear();shiftBatteryWarningPanel.showBattery(level)
        }
        if(!drinkVisible || !runtime.recording || !drinkSettings.enabled) drinkReminderPanel.clear()
        if(runtime.drinkReminder.update(runtime.recording,runtime.elapsedMs,drinkSettings,drinkVisible && shiftBatteryWarningPanel.visibility!=View.VISIBLE)) {
            drinkReminderPanel.showReminder(drinkSettings.durationSeconds);placeDrinkReminder()
        }
        if (::mapPanel.isInitialized && currentTemplate().hasMap) mapPanel.refresh()
        val mapMessage = runtime.mapState.message
        if (mapMessage != displayedMapMessage) {
            displayedMapMessage = mapMessage
            if (mapMessage.isNotEmpty()) BicycleFeedback.show(this, mapMessage, long = true)
        }
        if (runtime.visible && (runtime.connection.isActive || runtime.trainer.active || runtime.powerMeter.active || runtime.gpsSensor.active || runtime.heart.active)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        offerSensorSetup()
    }
    private fun placeDrinkReminder() {
        if(!::drinkReminderPanel.isInitialized || drinkReminderPanel.visibility!=View.VISIBLE) return
        val root=dashboardHost.getChildAt(0)
        val params=drinkReminderPanel.layoutParams as FrameLayout.LayoutParams
        val position=DrinkReminderSettings.read(preferences).position
        val edge=if(position==MessagePosition.CENTER) 0 else dp(BicycleDesign.dashboardMessageEdgeInset)
        val gravity=when(position) {
            MessagePosition.BOTTOM->Gravity.BOTTOM
            MessagePosition.CENTER->Gravity.CENTER_VERTICAL
            MessagePosition.TOP->Gravity.TOP
        } or Gravity.CENTER_HORIZONTAL
        if(params.gravity!=gravity || params.topMargin!=root.paddingTop+edge || params.bottomMargin!=root.paddingBottom+edge) {
            params.gravity=gravity;params.topMargin=root.paddingTop+edge;params.bottomMargin=root.paddingBottom+edge
            drinkReminderPanel.layoutParams=params
        }
    }
    /** Source selector stays outside the scrollable list for dashboard and map fields. */
    private fun chooseField(index: Int, mapField: Boolean = false) {
        fun permitted(): Boolean = dashboardEditing
        if (!permitted()) return
        picker?.dismiss()
        val selected = if (mapField) MapFieldSelection.read(preferences, index) else dashboardLayout.fields[index]
        val graphs=if(!mapField && dashboard.template.supportsGraph(index))
            GraphOverlayPicker(settingsContext,settingsThemes,dashboardGraphs.slots[index],dashboardGraphs.windows[index],GraphAvailability.forProfile(runtime.bikes.current(),runtime.barometer.available)) else null
        fun saveGraphs() {
            if(graphs!=null) {
                dashboardGraphs=dashboardGraphs.replace(index,graphs.selected).withWindow(index,graphs.window)
                preferences.edit().putString(GraphOverlayConfig.key(dashboardPage),dashboardGraphs.encode())
                    .putString(GraphOverlayConfig.windowKey(dashboardPage),dashboardGraphs.encodeWindows())
                    .putString(DashboardDataVisibility.key(dashboardPage),dashboardDataVisibility.encode()).apply()
            }
        }
        graphs?.changed = { if(permitted()) { restartEditorTimeout();saveGraphs(); render() } }
        fun select(field: DashboardField?) {
            // Ignore stale selections after the dashboard editor has been exited.
            if (!permitted()) return
            restartEditorTimeout()
            if (mapField) {
                preferences.edit().apply {
                    if (field == null) remove(MapFieldSelection.key(index)) else putString(MapFieldSelection.key(index), field.id)
                }.apply()
                mapPanel.refreshSettings()
            } else {
                if(field!=null) dashboardLayout = dashboardLayout.replace(index, field)
                dashboardDataVisibility=dashboardDataVisibility.replace(index,field!=null)
                // Write the old display-mode key as compatibility metadata; visibility is authoritative.
                dashboardGraphs=dashboardGraphs.withDisplay(index,if(field==null) GraphDisplayMode.GRAPH_ONLY else GraphDisplayMode.WITH_DATA)
                preferences.edit().putString(dashboardKey,dashboardLayout.encode())
                    .putString(DashboardDataVisibility.key(dashboardPage),dashboardDataVisibility.encode())
                    .putString(GraphDisplaySettings.key(dashboardPage),dashboardGraphs.encodeModes()).apply()
            }
            saveGraphs(); render()
            if (field?.requiresGps == true && !runtime.isIndoor) enableGps()
        }
        val trainingPage=dashboardPage
        val content = FieldPickerView(settingsContext, runtime.dashboardBikeData, runtime.gpsValues,
            runtime.ride.bike.distanceM, runtime.pulseValues, selected, mapField, settingsThemes, graphs, null, trainingSnapshot, if(mapField) selected!=null else dashboardDataVisibility.visible[index],
            batteryChoose={ select(it) },trendConfig=dashboardTrendIndicators,
            trendChanged={ config ->
                if(permitted()) {
                    restartEditorTimeout()
                    dashboardTrendIndicators=config
                    TrendIndicatorSettings.save(preferences,config);mapPanel.refreshSettings();render()
                }
            },trainingHelp=trainingHelpControls { trainingPage },choose={ select(it) }).apply { interaction=::restartEditorTimeout }
        val builder = dialog().setTitle(if (mapField) getString(R.string.ui_map_field_title, MapFieldSelection.positionLabel(this, index)) else getString(R.string.ui_dashboard_field_title, index + 1))
            .setView(content)
        val screen=builder.create()
        picker=screen
        val window=screen.window!!
        val original=window.callback
        val interaction=object:Window.Callback by original {
            override fun dispatchTouchEvent(event:android.view.MotionEvent):Boolean {
                restartEditorTimeout();return original.dispatchTouchEvent(event)
            }
            override fun dispatchKeyEvent(event:android.view.KeyEvent):Boolean {
                restartEditorTimeout();return original.dispatchKeyEvent(event)
            }
        }
        window.callback=interaction
        dialogs.onDismiss(screen) {
            if(window.callback===interaction) window.callback=original
            if(picker===screen) picker=null
            restartEditorTimeout()
        }
        screen.show();restartEditorTimeout()
    }
    private fun showNotificationSettings() {
        val content=MessageNotificationSettingsView(settingsContext,settingsThemes)
        notificationSettings=content
        val screen=dialog().setTitle(getString(R.string.notifications_title))
            .setView(BicycleStickyScrollView(settingsContext).apply { addView(content) }).show()
        dialogs.onDismiss(screen) { if(notificationSettings===content) notificationSettings=null }
    }
    private fun showStatistics() {
        val content=RideStatisticsView(settingsContext,settingsThemes)
        statisticsView=content
        val screen=dialog().setTitle(R.string.statistics_title)
            .setView(BicycleStickyScrollView(settingsContext).apply { addView(content) }).show()
        dialogs.onDismiss(screen) { if(statisticsView===content) { statisticsView=null;runtime.mapState.cancelStatistics() } }
        refreshStatistics()
    }
    private fun refreshStatistics() {
        val content=statisticsView ?: return
        runtime.mapState.loadStatistics { if(statisticsView===content) content.show(it) }
    }
    private fun showMenu(advanced: Boolean = false) {
        // Capture before settings restore Android's own status bar and change the insets.
        if(::dashboardStatus.isInitialized && dashboardStatus.visibility==View.VISIBLE)
            dashboardStatusReport=dashboardStatus.geometryReport()
        setDashboardEditing(false)
        messages.clear()
        val content = SettingsMenuView(settingsContext, settingsThemes, advanced) { action ->
            when (action) {
                SettingsAction.PROFILES, SettingsAction.SENSORS -> showSensorManager()
                SettingsAction.NOTIFICATIONS -> showNotificationSettings()
                SettingsAction.HRV -> menu=dialog().setTitle(R.string.hrv_settings_title)
                    .setView(ScrollView(settingsContext).apply { addView(HrvSettingsView(settingsContext,settingsThemes,openMeasurement={ showHrv() },canEdit={ !runtime.hrv.busy },openAnalysis=::showHrvTrends)) }).show()
                SettingsAction.DATA -> dataSettings()
                SettingsAction.MAINTENANCE -> showMaintenance()
                SettingsAction.PROFILE -> { menu = showRiderProfile(settingsContext, settingsThemes, dialogs, runtime.riderProfiles) }
                SettingsAction.MAP -> mapSettings()
                SettingsAction.DASHBOARDS -> dashboardSettings()
                SettingsAction.ROUTES -> routeSettings()
                SettingsAction.STATISTICS -> showStatistics()
                SettingsAction.ADVANCED -> showMenu(advanced = true)
                SettingsAction.RECOVERY -> recoverySettings()
                SettingsAction.GPS_HELP -> menu = dialog().setTitle(getString(R.string.ui_gps_help_title)).setMessage(R.string.ui_gps_help_body).show()
                SettingsAction.DIAGNOSTIC -> diagnosticSettings()
                SettingsAction.RESET -> resetDashboardSettings()
                SettingsAction.SOURCES -> sourcesSettings()
                SettingsAction.FONT_LICENSE -> menu = dialog().setTitle(getString(R.string.ui_font_license_title)).setMessage(assets.open("League-Gothic-OFL.txt").bufferedReader().use { it.readText() }).show()
                SettingsAction.MAP_LICENSE -> menu = dialog().setTitle(getString(R.string.ui_library_license_title)).setMessage(
                    "Mapsforge · LGPL-3.0 mit Ausnahme\n\n" + assets.open("Mapsforge-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nGarmin FIT SDK 21.218.0\n\n" + assets.open("Garmin-FIT-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nOkHttp 4.12.0 · Apache-2.0\n\n" + assets.open("OkHttp-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nOkio 3.6.0 · Apache-2.0\n\n" + assets.open("Okio-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nZip4j 2.11.5 · Apache-2.0\n\n" + assets.open("Zip4j-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nLucide 0.468.0 · ISC / Feather MIT\n\n" + assets.open("Lucide-LICENSE.txt").bufferedReader().use { it.readText() } +
                    "\n\nHRV · NeuroKit2 · MIT\n\n" + assets.open("NeuroKit2-LICENSE.txt").bufferedReader().use { it.readText() })
                    .show()
            }
        }
        menu = dialog().setTitle(if(advanced) R.string.ui_advanced else R.string.ui_settings).create().apply {
            setView(ScrollView(settingsContext).apply { addView(content) }, 0, 0, 0, 0)
            show()
        }
    }
    private fun recoverySettings() {
        val root = column()
        root.addView(label(runtime.recordingStatus, BicycleDesign.bodySp).apply { setPadding(0, dp(12), 0, dp(12)) })
        root.addView(button(getString(R.string.ui_recovery_retry), BicycleIcon.ExportTrack) { menu?.dismiss(); runtime.retryPending() }.apply {
            tag = "recovery_execute"; isEnabled = !runtime.recording && !runtime.saving
        })
        menu = dialog().setTitle(getString(R.string.ui_recovery_title)).setView(ScrollView(settingsContext).apply { addView(root) }).show()
    }
    private fun diagnosticSettings() {
        val root = column()
        root.addView(button(getString(R.string.ui_diagnostic_export), BicycleIcon.ExportTrack) { menu?.dismiss(); exportDiagnostic() }.apply { tag = "diagnostic_execute" })
        menu = dialog().setTitle(getString(R.string.ui_diagnostic_title)).setView(ScrollView(settingsContext).apply { addView(root) }).show()
    }
    private fun resetDashboardSettings() {
        val root = column()
        root.addView(button(getString(R.string.ui_reset_occupancy), BicycleIcon.Reset) {
            dialog(fullscreen=false).setTitle(R.string.ui_reset_title).setMessage(R.string.ui_reset_confirm)
                .setPositiveButton(R.string.ui_reset_action) { _, _ ->
                    dashboardLayout = DashboardLayout.defaults
                    preferences.edit().putString(dashboardKey, dashboardLayout.encode()).apply()
                    render()
                    BicycleFeedback.show(this, R.string.ui_reset_done, long = false)
                }.show()
        }.apply { tag = "reset_execute" })
        menu = dialog().setTitle(getString(R.string.ui_reset_defaults_title)).setView(ScrollView(settingsContext).apply { addView(root) }).show()
    }
    private fun showSensorManager() {
        val content = BikesView(settingsContext, runtime, ::searchSensors, ::stopScan, ::enableGps, ::recordingControls, { slot ->
            pendingBikePhotoSlot=slot
            runCatching { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),14) }
                .onFailure { pendingBikePhotoSlot=null;BicycleFeedback.show(this,getString(R.string.bike_photo_failed)) }
        }, { slot, name, remove ->
            dialog(fullscreen=false).setTitle(R.string.profiles_delete)
                .setMessage(getString(R.string.bikes_delete_confirm,name))
                .setPositiveButton(R.string.profiles_delete) { _, _ ->
                    if(runtime.bikes.activeSlot==slot) remove()
                }.show()
        }, additionalSections={ listOf(
            R.string.ui_design_title to dashboardControls(embedded=true),
            R.string.training_help_title to trainingHelpControls { dashboardPages.selected() }.apply { tag="profile_training_help" },
            R.string.ui_settings_map to mapControls()
        ) }, editDashboard=::enterDashboardEditor,assignProfiles=::showOrphanProfiles)
        bikesManager = content
        val scroll=BicycleStickyScrollView(settingsContext).apply { addView(content) }
        val footer=ProfileContextFooter(settingsContext,settingsThemes,content.footer)
        fun updateFooter() { footer.update(scroll,content.designHeader) }
        content.footerChanged={ footer.bindDashboard(content.dashboardActions);scroll.post { updateFooter() } }
        footer.bindDashboard(content.dashboardActions)
        scroll.setOnScrollChangeListener { _:View, _:Int, _:Int, _:Int, _:Int -> updateFooter() }
        content.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_->updateFooter() }
        scroll.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_->updateFooter() }
        val root=LinearLayout(settingsContext).apply {
            orientation=LinearLayout.VERTICAL
            addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
            addView(footer,LinearLayout.LayoutParams(-1,dp(BicycleDesign.footerHeight)))
        }
        sensorManager = dialog().setTitle(R.string.profiles_title).setView(root).create().apply {
            dialogs.onDismiss(this) { stopScan();if(bikesManager===content) bikesManager=null };show()
        }
    }
    private fun refreshSensorRows() { bikesManager?.refresh() }
    private fun searchSensors() {
        if(runtime.recording || runtime.saving) return
        if(!runtime.hasBluetooth()) { requestSensorPermission("all");return }
        stopScan();runtime.connectAssignedSensors()
        val manager=bikesManager ?: return
        val slot=runtime.bikes.activeSlot
        val profile=runtime.bikes.current()
        manager.beginSearch()
        scan=BikeSensorScanner(this,setOfNotNull(if(profile.type==BikeType.INDOOR) profile.trainerAddress else profile.bikeAddress,profile.heartAddress,profile.powerAddress)+profile.shiftDevices.map { it.address },{ device,name,category ->
            if(runtime.bikes.activeSlot!=slot || bikesManager!==manager || category !in profile.type.sensors || !category.supported) return@BikeSensorScanner
            manager.addCandidate(name,device.address,category) {
                if(runtime.bikes.activeSlot==slot && !runtime.recording && !runtime.saving) {
                    stopScan()
                    when(category) {
                        BikeSensor.EBIKE->runtime.selectBike(device,name)
                        BikeSensor.HEART->runtime.selectHeart(device,name)
                        BikeSensor.TRAINER->runtime.selectTrainer(device,name)
                        BikeSensor.POWER->runtime.selectPowerMeter(device,name)
                        BikeSensor.SHIFT->runtime.selectShiftComponent(device,name)
                        else->Unit
                    }
                    manager.clearSearch();manager.refresh()
                }
            }
        },{ status -> if(runtime.bikes.activeSlot==slot && bikesManager===manager) manager.scanStatus(status) },profile.type.sensors.toSet()).also { it.start() }
    }
    private fun stopScan() { scan?.stop();scan=null }
    private fun requestSensorPermission(kind: String) {
        pendingSensor = kind
        requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 1)
    }
    private fun enableGps() {
        if (!runtime.hasGps()) requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 3)
        else {
            runtime.setGpsEnabled(true)
            if (runtime.recording) startForegroundService(Intent(this, RecordingService::class.java).setAction(RecordingService.START))
        }
    }
    private fun recordingControls(): LinearLayout {
        val slot = runtime.bikes.activeSlot
        val current = runtime.config; val selected = current.fields.toMutableSet(); var metadata=current.metadata; val root = column().apply { tag="bike_fit_settings";setPadding(0, 0, 0, 0) }
        BicycleForm.section(root,settingsThemes,R.string.ui_recording_title).titleView.tag="fit_primary_heading"
        var previewRevision=Long.MIN_VALUE
        var previewDetails:RouteDetails?=null
        root.addView(label(getString(R.string.bikes_fit_explanation)).apply { setPadding(0,dp(BicycleDesign.controlGap),0,0) })
        root.addView(RideNamingView(settingsContext,settingsThemes,latestRide={
            if(previewRevision!=runtime.mapState.routeRevision) {
                previewRevision=runtime.mapState.routeRevision
                previewDetails=runtime.mapState.entries.firstOrNull { it.origin==TrackOrigin.RECORDED }?.details
            }
            previewDetails
        }) { runtime.bikes.activeSlot==slot && !runtime.recording && !runtime.saving })
        var sources=current.sourcePreferences ?: RecordingSources.defaults(current,current.indoor)
        lateinit var sourceControls:RecordingSourcesView
        fun saveFields() {
            if(runtime.bikes.activeSlot!=slot || !sourceControls.isAttachedToWindow || runtime.recording || runtime.saving) return
            runtime.saveConfig(RecordingConfig(selected.toSet(),
                if(sources.preferred[RecordingQuantity.SPEED]==RecordingSource.GPS) "GPS_SPEED" else "SPEED",
                if(sources.preferred[RecordingQuantity.DISTANCE]==RecordingSource.GPS) "GPS_DISTANCE" else "BIKE_DISTANCE",
                metadata=metadata,sourcePreferences=sources))
        }
        sourceControls=RecordingSourcesView(settingsContext,settingsThemes,runtime.bikes.current(),runtime.barometer.available,sources,
            editable={runtime.bikes.activeSlot==slot && !runtime.recording && !runtime.saving}) { sources=it;saveFields() }
        root.addView(sourceControls)
        BicycleForm.section(root,settingsThemes,R.string.ui_track_recording)
        root.addView(BicycleComponents.description(settingsContext,settingsThemes,getString(R.string.track_recording_description)).apply { tag="track_recording_description" })
        root.addView(FitMetadataView(settingsContext,settingsThemes,runtime.bikes.current(),metadata,
            currentProfile=runtime.bikes::current,expanded="FIT_METADATA" in expandedFitSources,onExpand={ if(it) expandedFitSources.add("FIT_METADATA") else expandedFitSources.remove("FIT_METADATA") }) {
            metadata=it;saveFields()
        })
        for ((source, fields) in RecordField.options.filter { it.id!="GPS_POSITION" && (!current.indoor || it.id in IndoorFields.allowed) }.groupBy {
            if(current.indoor && (it.id in IndoorFields.trainerIds)) "Indoor-Bike" else it.source
        }) {
            val frame=BicycleFrameDropdown(settingsContext,settingsThemes,DashboardTextResources.source(this,source),
                open=source in expandedFitSources,leadingIcon=SensorIcons.forSource(source)) { open ->
                if(open) expandedFitSources.add(source) else expandedFitSources.remove(source)
            }
            frame.header.apply { tag="fit_source_$source";isAccessibilityHeading=true }
            val content=frame.content.apply { tag="fit_source_content_$source" }
            root.addView(frame)
            for (field in fields) content.addView(Switch(settingsContext).apply {
                setTag(R.id.compact_choice_role,true)
                text = field.titleResource?.let(::getString) ?: DashboardTextResources.recordTitle(this@MainActivity, field.id) ?: DashboardField(field.id).title(this@MainActivity, LiveData()); tag = "record_${field.id}"; isChecked = field.id in selected; minHeight = dp(BicycleDesign.touchSize)
                setOnCheckedChangeListener { _, checked -> if (checked) selected.add(field.id) else selected.remove(field.id);saveFields() }
            })
        }
        // Style all source accordions and their existing field controls.
        BicycleDialogStyle.content(root, settingsThemes)
        return root
    }

    private fun showMaintenance() {
        val repository=MaintenanceRepository.get(this)
        val content=DreeveMaintenanceView(settingsContext,settingsThemes) { repository.refresh(MaintenanceRepository.Reason.MANUAL) }
        val screen=dialog().setTitle(R.string.maintenance_title).setView(BicycleStickyScrollView(settingsContext).apply { addView(content) }).show()
        val subscription=repository.observe(content::render)
        dialogs.onDismiss(screen) { subscription.close() }
        repository.refresh()
    }
    private fun showHrv()=showHrv(false)
    private fun showHrv(autoStart:Boolean) {
        if(runtime.recording || runtime.saving) { BicycleFeedback.show(this,R.string.hrv_ride_busy);return }
        val body=HrvMeasurementView(this,runtime.themes,runtime.hrv,
            sensor={ runtime.heartName },canStart={ runtime.visible && !runtime.recording && !runtime.saving },history=::showHrvHistory,questionnaire=::showHrvQuestionnaire,autoStart=autoStart)
        val screen=BicycleDialogBuilder(this,runtime.themes,dialogs,headerRole=ThemeRole.SECONDARY,plainHeader=true,hideStatusBar=true).setTitle(R.string.hrv_title)
            .setView(ScrollView(this).apply { BicycleComponents.scrollStyle(this);addView(body) }).create()
        dialogs.onDismiss(screen) { runtime.hrv.abort("VIEW_CLOSED") }
        screen.show()
        screen.window!!.decorView.post {
            if(!screen.isShowing) return@post
            val header=screen.window!!.decorView.findViewWithTag<android.view.ViewGroup>("bicycle_navigation_header") ?: return@post
            val fade=HrvHeaderFade(header)
            val subscription=runtime.hrv.observe { fade.show(it.phase==de.bcycle.hrv.Phase.REST || it.phase==de.bcycle.hrv.Phase.MEASURE) }
            dialogs.onDismiss(screen) { subscription.close();fade.close() }
        }
    }
    private fun showHrvDailyAssessment(id:String) {
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;tag="hrv_daily_assessment_host"
            val pad=dp(BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
            addView(BicycleComponents.description(this@MainActivity,runtime.themes,getString(R.string.hrvt_loading)))
        }
        val screen=BicycleDialogBuilder(this,runtime.themes,dialogs,headerRole=ThemeRole.SECONDARY,plainHeader=true,hideStatusBar=true)
            .setTitle(R.string.hrva_title).setView(ScrollView(this).apply { BicycleComponents.scrollStyle(this);addView(root) }).show()
        runtime.hrv.dailyAssessment(id) { result ->
            if(isDestroyed || screen.window?.decorView?.isAttachedToWindow!=true) return@dailyAssessment
            root.removeAllViews()
            result.fold({ assessment ->
                root.setPadding(0,0,0,0)
                screen.window?.decorView?.findViewWithTag<TextView>("bicycle_dialog_title")?.text=getString(R.string.hrva_dated_title,assessment.day.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                root.addView(HrvDailyAssessmentView(this,runtime.themes,assessment))
            },{ root.addView(BicycleComponents.description(this,runtime.themes,getString(R.string.hrv_result_load_failed))) })
        }
    }
    private fun showHrvTrends() {
        fun datedTitle(day:java.time.LocalDate)=getString(R.string.hrvt_dated_title,day.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")))
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;tag="hrv_trends_host"
            val pad=dp(BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
            addView(BicycleComponents.description(this@MainActivity,runtime.themes,getString(R.string.hrvt_loading)))
        }
        val footer=bicycleFooter(this,runtime.themes).apply { visibility=View.GONE }
        val body=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            addView(ScrollView(this@MainActivity).apply { BicycleComponents.scrollStyle(this);addView(root) },LinearLayout.LayoutParams(-1,0,1f))
            addView(footer,LinearLayout.LayoutParams(-1,-2))
        }
        val screen=BicycleDialogBuilder(this,runtime.themes,dialogs,headerRole=ThemeRole.SECONDARY,plainHeader=true,hideStatusBar=true).setTitle(datedTitle(java.time.LocalDate.now()))
            .setView(body).show()
        var content:HrvTrendsView?=null
        fun load(preserveSelection:Boolean=false) { runtime.hrv.trendsWithContext { result ->
            if(isDestroyed || screen.window?.decorView?.isAttachedToWindow!=true) return@trendsWithContext
            result.fold({ presentation ->
                val series=presentation.series
                if(content!=null) content!!.updateSeries(series,presentation.context,preserveSelection)
                else {
                    root.removeAllViews();root.setPadding(0,0,0,0)
                    content=HrvTrendsView(this,runtime.themes,series,actionFooter=footer,dayContext=presentation.context,result=::showHrvHistoryResult,assess=::showHrvDailyAssessment,delete={ id ->
                        dialog(fullscreen=false).setTitle(R.string.hrv_delete_title).setMessage(R.string.hrvt_day_delete_confirm)
                            .setPositiveButton(R.string.route_action_delete) { _,_ ->
                                (0 until footer.childCount).forEach { footer.getChildAt(it).isEnabled=false }
                                runtime.hrv.deleteMeasurement(id) { outcome ->
                                    (0 until footer.childCount).forEach { footer.getChildAt(it).isEnabled=true }
                                    if(!isDestroyed && screen.window?.decorView?.isAttachedToWindow==true) outcome.fold(onSuccess={ load() },
                                        onFailure={ BicycleFeedback.show(this,R.string.hrv_delete_failed) })
                                }
                            }.setNegativeButton(R.string.route_cancel,null).show()
                    },open={ id ->
                        runtime.hrv.measurement(id) { result ->
                            if(!isDestroyed && screen.isShowing) result.fold(onSuccess={ row -> showHrvQuestionnaire(row,{ load(true) }) },
                                onFailure={ BicycleFeedback.show(this,R.string.hrv_result_load_failed) })
                        }
                    }).apply {
                        onDaySelected={ day -> screen.window?.decorView?.findViewWithTag<TextView>("bicycle_dialog_title")?.text=datedTitle(day) }
                        onPeriodSelected={ label -> screen.window?.decorView?.findViewWithTag<TextView>("bicycle_dialog_title")?.text=getString(R.string.hrvt_dated_title,label) }
                    }
                    root.addView(content,LinearLayout.LayoutParams(-1,-2))
                }
            },{
                if(content==null) { root.removeAllViews();root.addView(BicycleComponents.description(this,runtime.themes,getString(R.string.hrv_result_load_failed))) }
                else BicycleFeedback.show(this,R.string.hrv_result_load_failed)
            })
        } }
        load()
    }
    private fun offerRideSummary() {
        if(rideSummaryPosted || !runtime.visible || runtime.recording || runtime.saving || runtime.pendingRideSummary==null) return
        rideSummaryPosted=true
        dashboardHost.post {
            rideSummaryPosted=false
            if(isDestroyed || isFinishing || !runtime.visible || runtime.recording || runtime.saving) return@post
            val completion=runtime.takeRideSummary() ?: return@post
            val id=completion.rideId ?: return@post
            dialogs.closeAll();dailyHrvPrompt?.close();dailyHrvPrompt=null
            if(dashboardEditing) finishDashboardEditor()
            setMapFullscreen(false)
            lateinit var screen:AlertDialog
            val content=RideSummaryView(this,runtime.themes,completion) {
                runtime.mapState.entries.firstOrNull { it.id==id }?.let { entry -> screen.dismiss();routeAnalysis(entry) }
            }
            screen=BicycleDialogBuilder(this,runtime.themes,dialogs,fullscreen=false,headerRole=ThemeRole.SECONDARY,plainHeader=true,dashboardSurface=true)
                .setTitle(R.string.ride_summary_title).setView(ScrollView(this).apply { BicycleComponents.scrollStyle(this);addView(content) }).show()
            val backdrop=DashboardModal.blur(dashboardRoot)
            val cancel=runtime.mapState.loadAnalysis(id) { result ->
                if(!isDestroyed && screen.isShowing) result.fold(content::show) { content.loadingFailed() }
            }
            dialogs.onDismiss(screen) { cancel();backdrop.close() }
        }
    }
    private fun showHrvHistory() {
        runtime.hrv.history { rows ->
            if(isDestroyed) return@history
            if(rows.isEmpty()) { BicycleFeedback.show(this,R.string.hrv_history_empty);return@history }
            val labels=rows.map { row ->
                if(row.importedDay!=null) {
                    val date=java.time.LocalDate.parse(row.importedDay).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))+
                        (row.importedTime?.let { " $it" } ?: "")
                    getString(R.string.hrv4_history,date,row.rmssdMs?.let { getString(R.string.hrv_history_rmssd,it) } ?: getString(R.string.hrv4_context_only))
                } else {
                    val date=java.time.Instant.ofEpochMilli(row.utcMs).atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    val metric=if(row.acceptable) row.rmssdMs?.let { getString(R.string.hrv_history_rmssd,it) } else null
                    getString(R.string.hrv_history_item,date,metric ?: getString(R.string.hrv_invalid_short))
                }
            }
            lateinit var overview:android.app.AlertDialog
            val list=ListView(this).apply {
                tag="hrv_history_list"
                adapter=object:BaseAdapter() {
                    override fun getCount()=rows.size
                    override fun getItem(position:Int)=rows[position]
                    override fun getItemId(position:Int)=position.toLong()
                    override fun getView(position:Int,recycled:View?,parent:android.view.ViewGroup):View =
                        LinearLayout(this@MainActivity).apply {
                            gravity=android.view.Gravity.CENTER_VERTICAL
                            addView(BicycleComponents.text(this@MainActivity,runtime.themes,labels[position]).apply {
                                minimumHeight=dp(BicycleDesign.touchSize)
                                setPadding(dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.verticalPadding),0,dp(BicycleDesign.verticalPadding))
                                setOnClickListener { overview.dismiss();showHrvHistoryResult(rows[position].id) }
                            },LinearLayout.LayoutParams(0,-2,1f))
                            addView(BicycleComponents.icon(this@MainActivity,runtime.themes,BicycleIcon.DeleteTrack,
                                getString(R.string.route_action_delete)+": "+labels[position]) {
                                dialog(fullscreen=false).setTitle(R.string.hrv_delete_title).setMessage(R.string.hrv_delete_confirm)
                                    .setPositiveButton(R.string.route_action_delete) { _,_ ->
                                        runtime.hrv.deleteMeasurement(rows[position].id) { outcome ->
                                            if(!isDestroyed) outcome.fold(onSuccess={ overview.dismiss();showHrvHistory() },
                                                onFailure={ BicycleFeedback.show(this@MainActivity,R.string.hrv_delete_failed) })
                                        }
                                    }.setNegativeButton(android.R.string.cancel,null).show()
                            }.apply { tag="hrv_delete_${rows[position].id}" })
                        }
                }
                setOnItemClickListener { _,_,position,_ -> overview.dismiss();showHrvHistoryResult(rows[position].id) }
            }
            overview=BicycleDialogBuilder(this,runtime.themes,dialogs,headerRole=ThemeRole.SECONDARY,plainHeader=true,hideStatusBar=true).setTitle(R.string.hrv_history)
                .setView(list).show()
        }
    }
    private fun showHrvHistoryResult(id:String) {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL
            val pad=dp(BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad)
            addView(BicycleComponents.description(this@MainActivity,runtime.themes,getString(R.string.hrv_result_loading))) }
        val screen=BicycleDialogBuilder(this,runtime.themes,dialogs,headerRole=ThemeRole.SECONDARY,plainHeader=true,hideStatusBar=true).setTitle(R.string.hrv_result)
            .setView(ScrollView(this).apply { BicycleComponents.scrollStyle(this);addView(root) }).show()
        runtime.hrv.measurement(id) { result ->
            if(isDestroyed || !screen.isShowing) return@measurement
            root.removeAllViews()
            result.fold({ row ->
                root.addView(hrvResult(this,runtime.themes,row))
                root.addView(BicycleComponents.action(this,runtime.themes,getString(R.string.hrvq_title)) { showHrvQuestionnaire(row) }.apply { tag="hrv_questionnaire_button" })
            },{ root.addView(BicycleComponents.description(this,runtime.themes,getString(R.string.hrv_result_load_failed))) })
        }
    }
    private fun showHrvQuestionnaire(row:org.json.JSONObject)=showHrvQuestionnaire(row,null)
    private fun showHrvQuestionnaire(row:org.json.JSONObject,completed:(()->Unit)?) {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL
            val pad=dp(BicycleDesign.horizontalPadding);setPadding(pad,0,pad,pad) }
        val loading=BicycleComponents.description(this,runtime.themes,getString(R.string.hrvq_loading));root.addView(loading)
        val screen=BicycleDialogBuilder(this,runtime.themes,dialogs,headerRole=ThemeRole.SECONDARY,plainHeader=true,hideStatusBar=true).setTitle(R.string.hrvq_title)
            .setView(ScrollView(this).apply { BicycleComponents.scrollStyle(this);addView(root) }).show()
        if(completed!=null) dialogs.onDismiss(screen) { completed() }
        val id=row.getString("id")
        runtime.hrv.questionnaire(id) { result ->
            if(isDestroyed || !screen.isShowing) return@questionnaire
            if(result.isFailure) { loading.setText(R.string.hrvq_load_failed);return@questionnaire }
            val initial=result.getOrNull() ?: HrvQuestionnaire.forMeasurement(row)
            fun show(training:HrvTrainingSummary?) {
                if(isDestroyed || !screen.isShowing) return
                root.removeAllViews();root.setPadding(0,0,0,0)
                root.addView(HrvQuestionnaireView(this,runtime.themes,initial,training,complete={ if(screen.isShowing) { screen.dismiss();if(completed==null) showHrvTrends() } }) { value,done ->
                    runtime.hrv.saveQuestionnaire(id,value) { saved -> done(saved.isSuccess) }
                },LinearLayout.LayoutParams(-1,-2))
            }
            if(runtime.mapState.busy) show(null) else {
                val cancel=runtime.mapState.loadHrvTraining(java.time.LocalDate.parse(initial.day),java.time.ZoneId.of(row.getString("zone")),::show)
                dialogs.onDismiss(screen,cancel)
            }
        }
    }
    private fun startRide() {
        if (runtime.recording || runtime.saving || runtime.hrv.busy) return
        val permissions = mutableListOf<String>()
        if (!runtime.hasBluetooth()) permissions += listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        if (!preferences.getBoolean("recordPermissionsAsked", false)) {
            if (runtime.gpsEnabled && runtime.config.needsGps && !runtime.hasGps()) permissions += listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (permissions.isNotEmpty()) {
            pendingStart = true; preferences.edit().putBoolean("recordPermissionsAsked", true).apply(); requestPermissions(permissions.toTypedArray(), 5); return
        }
        try { startForegroundService(Intent(this, RecordingService::class.java).setAction(RecordingService.START)) }
        catch (_: SecurityException) { BicycleFeedback.show(this, getString(R.string.ui_record_permission_error), long = true) }
        catch (_: IllegalStateException) { BicycleFeedback.show(this, getString(R.string.ui_record_visible_error), long = true) }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                val kind = pendingSensor; pendingSensor = null
                if (runtime.hasBluetooth()) when (kind) { "bikeConnect" -> runtime.setBikeEnabled(true); "heartConnect" -> runtime.setHeartEnabled(true); "bike", "heart", "all" -> searchSensors() }
                else BicycleFeedback.show(this, getString(R.string.ui_bluetooth_missing), long = false)
            }
            3 -> if (runtime.hasGps()) enableGps() else BicycleFeedback.show(this, getString(R.string.ui_gps_permission), long = false)
            5 -> if (pendingStart) {
                pendingStart = false
                if (runtime.hasBluetooth()) {
                    if (runtime.gpsEnabled && runtime.config.needsGps && !runtime.hasGps()) BicycleFeedback.show(this, getString(R.string.ui_record_without_gps), long = true)
                    startRide()
                } else BicycleFeedback.show(this, getString(R.string.ui_record_bluetooth_required), long = true)
            }
        }
        runtime.reconnectSensors(); render()
    }
    private fun exportDiagnostic() {
        val gps = runtime.gpsValues
        pendingReport = runtime.diagnostics.report() + "\n" + dashboardStatusReport.orEmpty() + "\nRecording=${runtime.recording} GPS accepted=${runtime.ride.gps.accepted} rejected=${runtime.ride.gps.rejected}\n" +
            GpsMetric.entries.joinToString("\n") { "${it.name}=${it.value(gps).text} ${it.unit}" }
        try { startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, "B-Cycle-${AppVersion.name}-diagnose.txt"), 2) }
        catch (_: android.content.ActivityNotFoundException) { pendingReport = null; BicycleFeedback.show(this, R.string.export_unavailable, long = true) }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("settingsBackupPendingFile", pendingBackupExport?.name); outState.putString("pendingReport", pendingReport); outState.putBoolean("mapFullscreen", mapFullscreen); super.onSaveInstanceState(outState)
        pendingBikePhotoSlot?.let { outState.putInt("pendingBikePhotoSlot",it) } }
    @Deprecated("Native document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==19) {
            if(resultCode==RESULT_OK) data?.data?.let { uri ->
                dialog().setTitle(R.string.hrv4_import).setView(ScrollView(settingsContext).apply {
                    addView(HrvCsvImportView(settingsContext,settingsThemes,runtime.hrv,uri))
                }).show()
            }
            return
        }
        if(requestCode==14) {
            val slot=pendingBikePhotoSlot;pendingBikePhotoSlot=null
            if(resultCode==RESULT_OK && slot!=null) data?.data?.let { uri ->
                BikePhotos.worker.execute {
                    val result=runCatching {
                        if(runtime.bikes.all().any { it.slot==slot }) {
                            BikePhotos.import(applicationContext,slot,uri)
                            if(runtime.bikes.all().none { it.slot==slot }) BikePhotos.delete(applicationContext,slot)
                        }
                    }
                    runOnUiThread { if(!isDestroyed) {
                        if(result.isSuccess && runtime.bikes.activeSlot==slot) bikesManager?.refreshPhoto()
                        if(result.isFailure) BicycleFeedback.show(this,getString(R.string.bike_photo_failed))
                    } }
                }
            };return
        }
        if(requestCode == 12 || requestCode == 13) { backupResult(requestCode,resultCode,data);return }
        if(requestCode in setOf(16,17,22,23)) {
            if(resultCode==RESULT_OK) data?.data?.let { tree ->
                runCatching { contentResolver.takePersistableUriPermission(tree,data.flags and
                    (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) }
                chooseRideBackupDirectory(DocumentBackupDirectory(this,tree),
                    if(requestCode in setOf(16,22)) RideBackupAction.EXPORT else RideBackupAction.IMPORT,requestCode in setOf(22,23))
            }
            return
        }
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) data?.data?.let { uri ->
                try {
                    val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    if (flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION == 0) error("Keine Schreibberechtigung")
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    val settings=RouteExportSettings(this)
                    settings.write(settings.read().copy(tree=uri.toString()),null)
                    exportDraft = settings.read()
                    localExportView?.let { it.tree=uri.toString();it.refreshFolder() }
                } catch (_: Exception) { BicycleFeedback.show(this, getString(R.string.ui_folder_permission_error), long = true) }
            }
            if(localExportView!=null) { exportDraft=null;exportPasswordDraft=null }
            if (dialogs.empty) {
                showMenu();dataSettings()
                dataSettingsScreen?.window?.decorView?.findViewWithTag<View>("data_LOCAL")?.performClick()
            }
            return
        }
        if (requestCode == 15) {
            if (dialogs.empty) { showMenu(); routeSettings() }
            if (resultCode == RESULT_OK) data?.data?.let { showArchiveImport(it) }
            return
        }
        if (requestCode == 8 || requestCode == 9) {
            if (resultCode == RESULT_OK) data?.data?.let { runtime.mapState.import(it, recorded = requestCode == 9) }
            if (dialogs.empty) { showMenu(); routeSettings() }
            return
        }
        if (requestCode != 2) return
        if (dialogs.empty) { showMenu(); diagnosticSettings() }
        val report = pendingReport; pendingReport = null
        val uri = data?.data ?: return
        if (resultCode != RESULT_OK || report == null) return
        Thread({
            val ok = try { contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(report) } ?: throw IOException(); true }
            catch (_: IOException) { false } catch (_: SecurityException) { false }
            runOnUiThread { BicycleFeedback.show(applicationContext, if (ok) R.string.export_saved else R.string.export_failed, long = true) }
        }, "B-Cycle-diagnostic-export").start()
    }
    private fun closeSensorSetupPrompt() {
        sensorSetupPrompt?.close();sensorSetupPrompt=null;sensorSetupProfileId=null
    }
    private fun canOfferSensorSetup() = runtime.visible && !isFinishing && !isDestroyed &&
        !runtime.recording && !runtime.saving && !runtime.hrv.busy && !runtime.hrv.state.saveError &&
        dialogs.empty && !dashboardEditing && bikePicker?.isShowing!=true && dailyHrvPrompt?.showing!=true &&
        runtime.pendingRideSummary==null && !rideSummaryPosted
    private fun offerSensorSetup() {
        if(!canOfferSensorSetup()) { closeSensorSetupPrompt();return }
        val profile=runtime.bikes.current()
        if(!sensorSetupLaunch.shouldShow(profile)) { closeSensorSetupPrompt();return }
        if(sensorSetupPrompt?.showing==true && sensorSetupProfileId==sensorSetupLaunch.identity(profile)) return
        if(sensorSetupPosted) return
        sensorSetupPosted=true
        dashboardHost.post {
            sensorSetupPosted=false
            if(!canOfferSensorSetup()) return@post
            val selected=runtime.bikes.current()
            if(!sensorSetupLaunch.shouldShow(selected)) return@post
            val id=sensorSetupLaunch.identity(selected)
            if(sensorSetupPrompt?.showing==true && sensorSetupProfileId==id) return@post
            closeSensorSetupPrompt();sensorSetupProfileId=id
            sensorSetupPrompt=SensorSetupPrompt(this,dashboardHost,dashboardRoot,runtime.themes,
                later={ sensorSetupLaunch.later(selected) }) {
                closeSensorSetupPrompt()
                if(runtime.recording || runtime.saving || runtime.hrv.busy || !runtime.visible ||
                    sensorSetupLaunch.identity(runtime.bikes.current())!=id) return@SensorSetupPrompt
                showSensorManager()
                val manager=bikesManager
                dashboardHost.post search@{
                    if(manager==null || bikesManager!==manager || sensorManager?.isShowing!=true || !runtime.visible ||
                        runtime.recording || runtime.saving || runtime.hrv.busy || sensorSetupLaunch.identity(runtime.bikes.current())!=id) return@search
                    manager.revealSensorSetup();searchSensors()
                }
            }
        }
    }
    private fun showHrvDailyPrompt() {
        if(!runtime.visible || runtime.recording || runtime.saving || runtime.hrv.busy || runtime.hrv.state.saveError) return
        closeSensorSetupPrompt()
        dialogs.closeAll();bikePicker?.dismiss()
        if(dashboardEditing) finishDashboardEditor()
        setMapFullscreen(false)
        dailyHrvPrompt?.close()
        dailyHrvPrompt=HrvDailyPrompt(this,dashboardHost,dashboardRoot,runtime.themes,
            acknowledge={ HrvDailyLaunch(this).acknowledge() }) { showHrv(autoStart=true) }
    }
    override fun onStart() { super.onStart();
        statusForeground=true;preferences.registerOnSharedPreferenceChangeListener(statusPreferences)
        getSharedPreferences("message-notifications",MODE_PRIVATE).registerOnSharedPreferenceChangeListener(statusMessagesPreferences)
        hrvObservation=runtime.hrv.observe { if(hrvWasBusy!=runtime.hrv.busy) { hrvWasBusy=runtime.hrv.busy;render() } }; updateDashboardFullscreen(); MessageNotifications.observers.add(messageObserver);notificationSettings?.refreshAccess();
        val notifications=MessageNotificationSettings(this)
        if(notifications.enabled && notifications.granted) runCatching {
            android.service.notification.NotificationListenerService.requestRebind(android.content.ComponentName(this,MessageNotificationService::class.java))
        }; if(currentTemplate().hasMap) mapPanel.resume(); runtime.observers.add(observer); runtime.setVisible(true); render(); refreshRouteRows()
        val dailyLaunch=HrvDailyLaunch(this);val today=HrvDailyLaunch.day()
        if(dailyLaunch.shouldShow(today)) showHrvDailyPrompt()
        restartEditorTimeout()
    }
    override fun onStop() {
        if(::dashboardViewport.isInitialized) dashboardViewport.cancelTransition()
        statusForeground=false;preferences.unregisterOnSharedPreferenceChangeListener(statusPreferences)
        getSharedPreferences("message-notifications",MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(statusMessagesPreferences)
        dashboardStatusMonitor.stop()
        editorHandler.removeCallbacks(editorTimeout)
        // App switches preserve the dialog stack, scroll positions and in-progress input.
        hrvObservation?.close();hrvObservation=null
        closeSensorSetupPrompt()
        stopScan();bikePicker?.dismiss();recordingIndicator?.stop()
        MessageNotifications.observers.remove(messageObserver);messages.clear();drinkReminderPanel.clear();shiftBatteryWarningPanel.clear()
        mapPanel.pause(); runtime.observers.remove(observer); runtime.setVisible(false); window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); super.onStop()
    }
    override fun onDestroy() {
        dailyHrvPrompt?.close();dailyHrvPrompt=null
        closeSensorSetupPrompt()
        if(mapBackRegistered) onBackInvokedDispatcher.unregisterOnBackInvokedCallback(mapBack)
        recordingIndicator?.stop();bikePicker?.dismiss();dialogs.closeAll();editorHandler.removeCallbacks(editorTimeout); settingsWriter.shutdown(); mapPanel.destroy(); super.onDestroy()
    }

    private fun setMapFullscreen(full: Boolean) {
        mapFullscreen = full && currentTemplate().hasMap; updateMapBack(); dashboard.visibility = if (mapFullscreen) View.GONE else View.VISIBLE
        val wasVisible=mapPanel.visibility==View.VISIBLE
        mapPanel.visibility=if(currentTemplate().hasMap) View.VISIBLE else View.GONE
        if(runtime.visible && wasVisible!=currentTemplate().hasMap) { if(currentTemplate().hasMap) mapPanel.resume() else mapPanel.pause() }
        if(!currentTemplate().hasMap) mapPanel.revealControls()
        noMapEditorFinish.visibility=if(dashboardEditing && !currentTemplate().hasMap) View.VISIBLE else View.GONE
        mapPanel.showFullscreen(mapFullscreen); dashboardArea.requestLayout()
    }
    private fun changeDashboard(delta: Int) {
        val pages=dashboardPages.all()
        val position=pages.indexOfFirst { it.id==dashboardPages.selected() }.coerceAtLeast(0)
        val target=pages[Math.floorMod(position+delta,pages.size)].id
        if(delta!=0 && target!=dashboardPage && !dashboardEditing && runtime.visible && dialogs.empty) {
            dashboardViewport.slide(delta) { showDashboardPage(target) }
        } else {
            dashboardViewport.cancelTransition();showDashboardPage(target)
        }
    }
    private fun showDashboardPage(target:Int) {
        dashboardPage=target
        preferences.edit().putInt("dashboardPage", dashboardPage).apply()
        dashboardLayout = DashboardLayout.decode(preferences.getString(dashboardKey, null))
        dashboardGraphs=GraphOverlayConfig.read(preferences,dashboardPage); dashboardTrainingHelp=DashboardTrainingHelp.read(preferences,dashboardPage); dashboardDataVisibility=DashboardDataVisibility.read(preferences,dashboardPage); dashboardTrendIndicators=TrendIndicatorSettings.read(preferences)
        val template = currentTemplate(); dashboard.setTemplate(template)
        dashboardArea.requestLayout()
        (mapPanel.layoutParams as LinearLayout.LayoutParams).weight = 1f - template.dataFraction
        setMapFullscreen(false); render()
    }
    private fun updateDashboardFullscreen() {
        if(!::runtime.isInitialized) return
        val fullscreen=DashboardFullscreen.enabled(preferences) && dialogs.empty
        DashboardFullscreen.apply(window,fullscreen)
        if(::dashboardStatus.isInitialized) {
            val config=DashboardStatusConfig.read(preferences)
            dashboardStatus.config=config
            val lightOn=runtime.currentBikeLight==true
            dashboardStatus.bikeLightOn=fullscreen && lightOn
            cornerBikeLight.visibility=if(!fullscreen && dialogs.empty && lightOn) View.VISIBLE else View.GONE
            dashboardStatus.visibility=if(fullscreen && (config.populated || dashboardStatus.bikeLightOn)) View.VISIBLE else View.GONE
            if(statusForeground) dashboardStatusMonitor.configure(if(fullscreen) config else DashboardStatusConfig())
            dashboardRoot.requestApplyInsets()
        }
    }
    override fun onWindowFocusChanged(hasFocus:Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(!hasFocus && ::dashboardViewport.isInitialized) dashboardViewport.cancelTransition()
        if(hasFocus) updateDashboardFullscreen()
    }
    private fun dashboardSettings() {
        menu=dialog().setTitle(R.string.ui_design_title).setView(dashboardControls()).show()
    }
    private fun dashboardControls(embedded:Boolean=false):View {
        val root = column()
        root.addView(Switch(settingsContext).apply {
            tag="flat_design";setText(R.string.ui_flat_design);isChecked=settingsThemes.flatDesign
            BicycleDialogStyle.control(this,settingsThemes)
            setOnCheckedChangeListener { _,checked -> settingsThemes.setFlatDesign(checked) }
        })
        root.addView(ThemePickerView(settingsContext,settingsThemes))
        root.addView(DashboardStatusSettingsView(settingsContext,settingsThemes,preferences,changed={ updateDashboardFullscreen() }))
        root.addView(BicycleComponents.sectionDivider(settingsContext,settingsThemes).apply { tag="design_dashboard_divider" })
        root.addView(BicycleComponents.contentHeading(settingsContext,settingsThemes,getString(R.string.ui_dashboard_page_choose)))
        val presets = DashboardPresetPicker(settingsContext,dashboardPages.selected(),settingsThemes)
        val footer=bicycleFooter(settingsContext,settingsThemes)
        var trainingControls:TrainingHelpView?=null
        lateinit var removeDashboard:BicycleFooterAction
        fun refreshPages() {
            presets.refresh();changeDashboard(0)
            trainingControls?.show(DashboardTrainingHelp.read(preferences,dashboardPages.selected()))
            bikesManager?.findViewWithTag<TrainingHelpView>("profile_training_help")?.show(DashboardTrainingHelp.read(preferences,dashboardPages.selected()))
            removeDashboard.visibility=if(dashboardPages.all().size>1) View.VISIBLE else View.GONE
        }
        footer.addView(BicycleFooterAction(settingsContext,settingsThemes,BicycleIcon.RenameTrack,getString(R.string.dashboard_rename)) {
            val page=dashboardPages.all().first { it.id==dashboardPages.selected() }
            val input=BicycleComponents.input(settingsContext,settingsThemes).apply {
                tag="dashboard_name_edit";setSingleLine();setText(page.name);selectAll()
                filters=arrayOf(android.text.InputFilter.LengthFilter(60));contentDescription=getString(R.string.dashboard_name)
            }
            input.addTextChangedListener(object:android.text.TextWatcher {
                override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
                override fun afterTextChanged(s:android.text.Editable?)=Unit
                override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {
                    input.error=if(dashboardPages.rename(page.id,s.toString())) null else getString(R.string.dashboard_name_invalid)
                    presets.refresh()
                }
            })
            dialog(fullscreen=false).setTitle(R.string.route_action_rename).setView(input).show()
        }.apply { tag="dashboard_rename" },LinearLayout.LayoutParams(0,-2,1f))
        removeDashboard=BicycleFooterAction(settingsContext,settingsThemes,BicycleIcon.DeleteTrack,getString(R.string.dashboard_delete)) {
            val page=dashboardPages.all().first { it.id==dashboardPages.selected() }
            dialog(fullscreen=false).setTitle(R.string.dashboard_delete).setMessage(getString(R.string.dashboard_delete_confirm,page.name))
                .setPositiveButton(R.string.route_action_delete) { _,_->dashboardPages.delete(page.id);refreshPages() }
                .setNegativeButton(R.string.route_cancel,null).show()
        }.apply { tag="dashboard_delete";visibility=if(dashboardPages.all().size>1) View.VISIBLE else View.GONE }
        presets.changed=::refreshPages
        root.addView(presets)
        if(!embedded) trainingControls=appendTrainingHelp(root) { dashboardPages.selected() }
        footer.addView(BicycleFooterAction(settingsContext,settingsThemes,BicycleIcon.EditDashboard,getString(R.string.dashboard_editor_open)) { enterDashboardEditor() }.apply {
            tag="dashboard_editor_open";(getChildAt(1) as TextView).maxLines=2
        },LinearLayout.LayoutParams(0,-2,1f))
        footer.addView(removeDashboard,LinearLayout.LayoutParams(0,-2,1f))
        if(embedded) {
            // BikesView moves these existing actions into its context-sensitive fixed footer.
            footer.tag="dashboard_inline_actions"
            root.setPadding(0,0,0,0);root.addView(footer)
            BicycleDialogStyle.content(root,settingsThemes);return root
        }
        val body=LinearLayout(settingsContext).apply {
            orientation=LinearLayout.VERTICAL
            addView(BicycleStickyScrollView(settingsContext).apply { tag="design_ui_scroll";addView(root) },LinearLayout.LayoutParams(-1,0,1f))
            addView(footer,LinearLayout.LayoutParams(-1,-2))
        }
        return body
    }
    private fun drinkReminderControls():DrinkReminderSettingsView {
        val slot=runtime.bikes.activeSlot;val revision=runtime.profileRevision
        return DrinkReminderSettingsView(settingsContext,settingsThemes,preferences,
            canEdit={ slot==runtime.bikes.activeSlot && revision==runtime.profileRevision },changed={ render() })
    }
    private fun trainingHelpControls(page:()->Int)=
        TrainingHelpView(settingsContext,settingsThemes,DashboardTrainingHelp.read(preferences,page())) { config ->
            val id=page()
            preferences.edit().putString(DashboardTrainingHelp.key(id),config.encode()).apply()
            if(id==dashboardPage) dashboardTrainingHelp=config
            render()
        }.apply { addDrinkReminder(drinkReminderControls()) }
    private fun appendTrainingHelp(root:LinearLayout,page:()->Int):TrainingHelpView {
        val heading=BicycleForm.section(root,settingsThemes,R.string.training_help_title).apply { revealOnOpen=true }
        return trainingHelpControls(page).also { it.revealAnchor=heading;root.addView(it) }
    }
    private fun mapSettings() {
        menu=dialog().setTitle(R.string.ui_map_manager_title).setView(ScrollView(settingsContext).apply { addView(mapControls()) }).show()
    }
    private fun mapControls():LinearLayout {
        val slot=runtime.bikes.activeSlot
        val root = column()
        val source = Spinner(settingsContext).apply {
            contentDescription=getString(R.string.ui_map_source)
            adapter = ArrayAdapter(settingsContext, android.R.layout.simple_spinner_dropdown_item, MapSource.entries.map { it.label(this@MainActivity) })
            setSelection(MapSource.decode(preferences.getString("mapSource", null)).ordinal); root.addView(this)
        }
        BicycleDialogStyle.spinner(source,settingsThemes,BicycleDesign.profileDropdownSp)
        val keyRow=LinearLayout(settingsContext).apply { orientation=LinearLayout.VERTICAL;tag="map_provider_key_row" }
        keyRow.addView(BicycleComponents.fieldLabel(settingsContext,settingsThemes,getString(R.string.map_thunderforest_key)))
        val providerKey=BicycleComponents.input(settingsContext,settingsThemes).apply {
            tag="map_thunderforest_key";setSingleLine();contentDescription=getString(R.string.map_thunderforest_key)
            inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS;isSaveEnabled=false
            setText(MapProviderSettings.read(this@MainActivity))
        }
        BicycleForm.outlinedInput(providerKey,settingsThemes)
        keyRow.addView(providerKey);root.addView(keyRow)
        val providerStatus=BicycleFeedback.status(settingsContext,settingsThemes).apply { tag="map_provider_status" }
        root.addView(providerStatus)
        keyRow.visibility=if(MapSource.entries[source.selectedItemPosition]==MapSource.OPENCYCLEMAP) View.VISIBLE else View.GONE
        fun toggle(title: String, key: String, fallback: Boolean) = Switch(settingsContext).apply { text = title; tag = key; isChecked = preferences.getBoolean(key, fallback); root.addView(this) }
        root.addView(BicycleComponents.contentHeading(settingsContext,settingsThemes,getString(R.string.map_options_title)).apply { tag="map_options_title" })
        val track = toggle(getString(R.string.ui_map_track), "mapTrackVisible", true)
        val gpx = toggle(getString(R.string.ui_map_gpx), "mapGpxVisible", true)
        var lastGpxVisibility=gpx.isChecked
        val mapHeight = toggle(getString(R.string.ui_map_height), "mapHeightVisible", true)
        val windRose = toggle(getString(R.string.map_wind_rose), "mapWindRoseVisible", false)
        val elevation = toggle(getString(R.string.elevation_enable), FitElevationCorrection.preference, false).apply { tag="map_elevation_correction" }
        root.addView(BicycleComponents.sectionDivider(settingsContext,settingsThemes).apply { tag="map_line_style_divider" })
        root.addView(MapLineStyleView(settingsContext,preferences,settingsThemes,dialogs) {
            if(runtime.bikes.activeSlot==slot) mapPanel.refreshSettings()
        },LinearLayout.LayoutParams(-1,-2))
        observeSettings(root) {
            if(runtime.bikes.activeSlot!=slot || !root.isAttachedToWindow) return@observeSettings
            val chosen=MapSource.entries[source.selectedItemPosition]
            keyRow.visibility=if(chosen==MapSource.OPENCYCLEMAP) View.VISIBLE else View.GONE
            val entered=providerKey.text.toString().trim()
            val validKey=entered.isEmpty() || MapProviderSettings.valid(entered)
            providerKey.error=if(!validKey && chosen==MapSource.OPENCYCLEMAP) getString(R.string.map_thunderforest_invalid) else null
            if(validKey) MapProviderSettings.write(this,entered)
            val available=chosen!=MapSource.OPENCYCLEMAP || MapProviderSettings.read(this).isNotEmpty()
            BicycleFeedback.update(providerStatus,if(!available) getString(R.string.map_thunderforest_required) else "",hideWhenEmpty=true)
            val edit=preferences.edit()
            if(available) edit.putString("mapSource",chosen.name)
            edit.putBoolean("mapTrackVisible",track.isChecked).putBoolean("mapHeightVisible",mapHeight.isChecked).putBoolean("mapWindRoseVisible",windRose.isChecked)
                .putBoolean(FitElevationCorrection.preference,elevation.isChecked).apply()
            if(gpx.isChecked!=lastGpxVisibility) { lastGpxVisibility=gpx.isChecked;runtime.mapState.setRouteVisible(gpx.isChecked) }
            mapPanel.refreshSettings()
        }
        root.setPadding(0,0,0,0)
        BicycleDialogStyle.content(root,settingsThemes)
        return root
    }
    private fun importRoute(recorded: Boolean = false, archive: Boolean = false) {
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, if (archive) arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream") else if (recorded)
                    arrayOf("application/vnd.ant.fit", "application/octet-stream") else
                    arrayOf("application/gpx+xml", "application/xml", "text/xml", "application/vnd.ant.fit", "application/octet-stream")),
                if (archive) 15 else if (recorded) 9 else 8)
        } catch (_: android.content.ActivityNotFoundException) {
            BicycleFeedback.show(this, getString(R.string.ui_file_picker_missing), long = true)
        }
    }
    private fun routeAnalysis(entry: ManagedRoute) {
        val content = RouteAnalysisView(settingsContext, settingsThemes, entry.id, ::routeAnalysisMap)
        val footer = bicycleFooter(settingsContext, settingsThemes)
        val root = LinearLayout(settingsContext).apply {
            orientation = LinearLayout.VERTICAL
            addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(footer, LinearLayout.LayoutParams(-1, dp(BicycleDesign.footerHeight)))
        }
        val screen = dialog().setView(root).create()
        val detail = DetailSession(entry.id, screen, content, footer)
        val labels = listOf(R.string.route_action_map, R.string.route_action_rename, R.string.elevation_correct_action, R.string.route_transfer_upload, R.string.route_action_delete)
        val captions = listOf(R.string.footer_show, R.string.route_action_rename, R.string.elevation_correct_action, R.string.route_transfer_upload, R.string.route_action_delete)
        val icons = listOf(BicycleIcon.ShowOnMap, BicycleIcon.RenameTrack, BicycleIcon.CorrectElevation, BicycleIcon.Cloud, BicycleIcon.DeleteTrack)
        val tags = listOf("detail_map", "detail_rename", "detail_elevation", "detail_upload", "detail_delete")
        labels.indices.forEach { index ->
            footer.addView(BicycleFooterAction(settingsContext, settingsThemes, icons[index], getString(captions[index]), getString(labels[index])) {
                executeDetailAction(detail, index)
            }.apply { tag = tags[index] }, LinearLayout.LayoutParams(0, dp(BicycleDesign.footerHeight), 1f))
        }
        routeDetail = detail
        screen.setCustomTitle(analysisHeader(settingsContext, settingsThemes, entry, rename = { if(!runtime.mapState.busy) detailEntry(detail)?.let(::renameRoute) }, back = { screen.dismiss() }))
        dialogs.onDismiss(screen) {
            content.dispose();detail.transfers?.dismiss()
            if (routeDetail === detail) { routePopup?.dismiss(); routePopup = null; routeDetail = null }
        }
        screen.show()
        refreshDetails()
        fun load() {
            content.loading()
            content.cancellation(runtime.mapState.loadAnalysis(entry.id) { result ->
                result.fold({ analysis ->
                    detail.analysis = analysis
                    content.show(analysis)
                    refreshDetails()
                    screen.window?.decorView?.findViewWithTag<TextView>("analysis_date")?.let { date ->
                        date.text = java.time.format.DateTimeFormatter.ofPattern(getString(R.string.analysis_date_format))
                            .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(analysis.entry.details.dateMs))
                    }
                }) { content.failed { load() } }
            })
        }
        content.correctElevation = {
            if(!runtime.recording && !runtime.saving && !runtime.mapState.busy && entry.format=="FIT" && entry.origin==TrackOrigin.RECORDED && entry.hasRoute) {
                content.correctionStatus(true)
                content.cancellation(runtime.mapState.correctElevation(entry.id) { result ->
                    if(routeDetail===detail && screen.isShowing) result.fold({ load() },{ content.correctionStatus(false,getString(R.string.elevation_failed)) })
                })
            }
        }
        load()
    }
    private fun routeAnalysisMap(analysis: RouteAnalysis) {
        val route = analysis.route ?: return
        val map = RouteAnalysisMap(settingsContext, route, true)
        val screen = dialog().setTitle(analysis.entry.name).setView(map)
            .create()
        dialogs.onDismiss(screen, map::dispose)
        screen.show()
    }
    private fun canAssignProfiles()=!runtime.recording && !runtime.saving && !runtime.hrv.busy
    private fun refreshOrphanProfiles() {
        val content=orphanProfilesView ?: return
        if(orphanProfilesScreen?.isShowing==true) content.show(runtime.mapState.orphanProfiles(),runtime.bikes.all(),runtime.mapState.busy || !canAssignProfiles())
    }
    private fun showOrphanProfiles() {
        if(orphanProfilesScreen?.isShowing==true || runtime.mapState.busy) return
        val groups=runtime.mapState.orphanProfiles()
        if(groups.isEmpty()) { BicycleFeedback.show(this,R.string.profile_assignment_empty);return }
        lateinit var content:OrphanProfilesView
        content=OrphanProfilesView(settingsContext,settingsThemes) { group,target,type ->
            val accepted=runtime.mapState.assignOrphanProfile(group,target,type,::canAssignProfiles) { result ->
                if(orphanProfilesView!==content) return@assignOrphanProfile
                result.fold({ count ->
                    BicycleFeedback.show(this,getString(R.string.profile_assignment_done,count))
                    if(runtime.mapState.orphanProfiles().isEmpty()) orphanProfilesScreen?.dismiss() else refreshOrphanProfiles()
                },{ refreshOrphanProfiles();content.failed() })
            }
            if(!accepted) { refreshOrphanProfiles();content.failed() }
        }
        content.show(groups,runtime.bikes.all(),!canAssignProfiles())
        val screen=dialog().setTitle(R.string.profile_assignment_title)
            .setView(ScrollView(settingsContext).apply { BicycleComponents.scrollStyle(this);addView(content) }).create()
        orphanProfilesView=content;orphanProfilesScreen=screen
        dialogs.onDismiss(screen) { if(orphanProfilesView===content) { orphanProfilesView=null;orphanProfilesScreen=null } }
        screen.show()
    }
    private fun offerOrphanProfiles() {
        val content=routeOrphanPending ?: return
        if(!runtime.visible || runtime.mapState.busy || routeManager!==content || !content.isShown || orphanProfilesScreen?.isShowing==true) return
        routeOrphanPending=null
        if(runtime.mapState.orphanProfiles().isNotEmpty()) showOrphanProfiles()
    }
    private fun routeSettings() {
        setDashboardEditing(false)
        val state = runtime.mapState
        routeSignature = ""
        val content = RouteManagerView(settingsContext, { entry ->
            state.toggleMapRoute(entry.id)
        }, themes = settingsThemes, details = ::routeAnalysis)
        content.chooseMapSlot={ anchor,entry -> content.showRouteSlots(MapRouteSlotPopup(anchor,runtime,assignId=entry.id) {}) }
        routeManager = content
        refreshRouteRows()
        val footer = bicycleFooter(settingsContext, settingsThemes)
        val import = BicycleFooterAction(settingsContext, settingsThemes, BicycleIcon.Import, getString(R.string.footer_import_route)) {
            if (!state.busy) routeImportChoice()
        }.apply { tag = "route_import"; isEnabled = !state.busy }
        footer.addView(import,LinearLayout.LayoutParams(0,dp(BicycleDesign.footerHeight),1f))
        val filterAction=BicycleFooterAction(settingsContext,settingsThemes,BicycleIcon.Filter,getString(R.string.route_filter_button)) {
            content.showFilters(footer.findViewWithTag("route_filters"),dialogs)
        }.apply { tag="route_filters" }
        footer.addView(filterAction,LinearLayout.LayoutParams(0,dp(BicycleDesign.footerHeight),1f))
        footer.addView(BicycleFooterAction(settingsContext,settingsThemes,BicycleIcon.Sort,getString(R.string.route_sort_button)) {
            content.showSort(footer.findViewWithTag("route_sort"),dialogs)
        }.apply { tag="route_sort" },LinearLayout.LayoutParams(0,dp(BicycleDesign.footerHeight),1f))
        val delete=BicycleFooterAction(settingsContext,settingsThemes,BicycleIcon.DeleteTrack,getString(R.string.route_action_delete)) {
            val ids=content.checkedIds
            if(ids.isNotEmpty() && !state.busy) dialog(fullscreen=false).setTitle(R.string.route_delete_many_title)
                .setMessage(getString(R.string.route_delete_many_message,ids.size))
                .setPositiveButton(R.string.route_action_delete) { _,_-> state.deleteRoutes(ids);content.clearSelection() }.show()
        }.apply { tag="route_delete_selected";isEnabled=false }
        footer.addView(delete,LinearLayout.LayoutParams(0,dp(BicycleDesign.footerHeight),1f))
        content.selectionChanged={ delete.isEnabled=content.selecting && !state.busy;delete.setLabel(if(content.selecting) getString(R.string.route_delete_count,content.checkedIds.size) else getString(R.string.route_action_delete)) }
        content.queryChanged={ filterAction.setLabel(if(content.query.activeCount>0) getString(R.string.route_filter_count,content.query.activeCount) else getString(R.string.route_filter_button)) }
        val root = LinearLayout(settingsContext).apply {
            orientation = LinearLayout.VERTICAL
            addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(footer, LinearLayout.LayoutParams(-1, dp(BicycleDesign.footerHeight)))
        }
        content.bindFooter(footer)
        routeImportButton = import
        menu = dialog().setView(root).create().apply {
            setView(root, 0, 0, 0, 0)
            dialogs.onDismiss(this) { if (routeManager === content) { routePopup?.dismiss(); routePopup = null; routeManager = null; routeImportButton = null;routeOrphanPending=null } }; show()
        }
        routeOrphanPending=content
        menu?.window?.decorView?.post { offerOrphanProfiles() }
        menu?.window?.decorView?.findViewWithTag<View>("dialog_back")?.setOnClickListener {
            if(content.selecting) content.clearSelection() else menu?.dismiss()
        }
        menu?.setOnKeyListener { _,key,event ->
            if(key==android.view.KeyEvent.KEYCODE_BACK && content.selecting) {
                if(event.action==android.view.KeyEvent.ACTION_UP) content.clearSelection();true
            } else false
        }
        val managerScreen=menu!!
        val back=android.window.OnBackInvokedCallback { if(content.selecting) content.clearSelection() else managerScreen.dismiss() }
        managerScreen.window!!.onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,back)
        dialogs.onDismiss(managerScreen) { managerScreen.window?.onBackInvokedDispatcher?.unregisterOnBackInvokedCallback(back) }
        state.refreshLibrary()
    }
    private fun routeImportChoice() {
        val anchor = routeImportButton ?: return
        routeDropdown(anchor, listOf(getString(R.string.ui_import_planned), getString(R.string.ui_import_recorded), getString(R.string.archive_import_title),getString(R.string.dreeve_import_title)),
            listOf(BicycleIcon.TrackImported, BicycleIcon.TrackRecorded, BicycleIcon.Import,BicycleIcon.Dreeve), listOf("route_import_planned", "route_import_recorded", "route_import_archive","route_import_dreeve"),
            enabled={ it!=3 || ActivityServiceSettings(this,ActivityService.DREEVE).dreeveImportConfigured() }) {
            if(it==3) showDreeveImport() else importRoute(recorded = it == 1, archive = it == 2)
        }
    }
    private fun showArchiveImport(uri:android.net.Uri) {
        val closed=java.util.concurrent.atomic.AtomicBoolean(false)
        var archive:PreparedRideArchive?=null
        lateinit var screen:android.app.AlertDialog
        lateinit var content:DreeveImportView
        content=DreeveImportView(settingsContext,settingsThemes,loadPage={
            screen.dismiss();importRoute(archive=true)
        },importSelection={ selected ->
            archive?.let { prepared -> runtime.mapState.importPreparedArchive(prepared,selected.map { it.id }.toSet(),closed::get,
                progress={ if(!closed.get()) content.progress(it) },complete={ if(!closed.get()) content.complete(it) }) }
        },archiveMode=true)
        content.navigation=dialogs;content.preparingArchive()
        screen=dialog().setTitle(R.string.archive_import_title).setView(content).create()
        dialogs.onDismiss(screen) { closed.set(true);archive?.let(runtime.mapState::discardArchive) }
        screen.show()
        runtime.mapState.prepareArchive(uri,closed::get) { result ->
            result.fold({ prepared ->
                if(closed.get()) { runtime.mapState.discardArchive(prepared);return@fold }
                archive=prepared
                content.loaded(DreeveActivityPage(prepared.entries.map { it.selection() },1,1,prepared.entries.size),
                    prepared.entries.filter { it.known }.map { it.id }.toSet())
                val report=prepared.report
                if(report.interrupted) BicycleFeedback.show(this,getString(R.string.archive_prepared_incomplete),long=true)
                else if(report.failed>0 || report.unsupported>0) BicycleFeedback.show(this,
                    getString(R.string.archive_prepared_issues,report.failed,report.unsupported),long=true)
            },{ if(!closed.get()) content.failed(R.string.archive_import_open_failed) })
        }
    }
    private fun showDreeveImport() {
        val closed=java.util.concurrent.atomic.AtomicBoolean(false)
        val worker=java.util.concurrent.Executors.newSingleThreadExecutor { task -> Thread(task,"B-Cycle-dreeve-import") }
        val importCancelled=java.util.concurrent.atomic.AtomicBoolean(false)
        var importProgress:android.app.AlertDialog?=null
        var source:String?=null
        var listGeneration=0
        lateinit var content:DreeveImportView
        fun client():Pair<String,DreeveClient> {
            val settings=ActivityServiceSettings(this,ActivityService.DREEVE)
            val config=settings.read();val key=settings.credentials().optString("secret")
            if(config.address.isBlank() || key.isBlank()) throw ActivityServiceFailure(false,R.string.dreeve_import_config_missing)
            return config.identity(ActivityService.DREEVE) to DreeveClient(config,key,{ closed.get() || importCancelled.get() })
        }
        fun startImport(activities:List<DreeveActivity>,target:String,api:DreeveClient,mappings:Map<String,Int>,origin:TrackOrigin) {
            importCancelled.set(false)
            val progressView=DreeveImportProgressView(settingsContext,settingsThemes,activities.distinctBy { it.id }.size) { importCancelled.set(true) }
            val names=activities.associate { it.id to it.name }
            val progressDialog=BicycleDialogBuilder(settingsContext,settingsThemes,dialogs,fullscreen=false,helpEnabled=false)
                .setView(progressView).setCancelable(false).create()
            progressDialog.setCanceledOnTouchOutside(false)
            importProgress=progressDialog;progressDialog.show()
            runtime.mapState.importDreeve(target,activities,api,{ closed.get() || importCancelled.get() },
                progress={ item -> if(!closed.get()) { content.progress(item);progressView.completed(names[item.id] ?: item.id) } },
                complete={ result ->
                    progressDialog.dismiss();if(importProgress===progressDialog) importProgress=null
                    if(!closed.get()) content.complete(result)
                },mappings=mappings,origin=origin)
        }
        fun chooseBikes(activities:List<DreeveActivity>,target:String,api:DreeveClient) {
            val gear=activities.mapNotNull { it.summary.gearId }.distinct()
            val remembered=DreeveBikeMappings.read(this,target).toMutableMap()
            val profiles=BikeProfileStore(this,preferences).all()
            val body=DreeveBikeAssignmentView(settingsContext,settingsThemes,gear,content.gearNames,profiles,remembered)
            var started=false
            val screen=dialog().setTitle(R.string.dreeve_bikes_title).setView(ScrollView(settingsContext).apply { addView(body) })
                .setPositiveButton(R.string.dreeve_import_start) { _,_ ->
                    remembered.putAll(body.assignments())
                    DreeveBikeMappings.save(this,target,remembered);content.profileFor={ it.summary.gearId?.let(remembered::get)?.takeIf { slot -> slot>=0 } };started=true
                    startImport(activities,target,api,remembered,body.origin())
                }.create()
            dialogs.onDismiss(screen) { if(!started && !closed.get()) content.cancelImport() }
            screen.show()
        }
        content=DreeveImportView(settingsContext,settingsThemes,loadPage={ page ->
            val expected=source
            val generation=++listGeneration
            worker.execute {
                try {
                    val (target,api)=client()
                    if(page>1 && target!=expected) throw ActivityServiceFailure(false,R.string.dreeve_import_changed)
                    val gear=if(page==1) runCatching { api.gears() }.getOrDefault(emptyMap()) else emptyMap()
                    api.allActivities(page) { data ->
                        if(closed.get()) throw java.io.IOException()
                        runOnUiThread { if(!closed.get()) {
                            source=target
                            if(page==1 && data.page==1) content.gearNames=gear
                            val bikeMappings=DreeveBikeMappings.read(this,target)
                            content.profileFor={ activity -> activity.summary.gearId?.let { bikeMappings[it] }?.takeIf { it>=0 } }
                            content.loaded(data,runtime.mapState.existingDreeve(target,data.activities),continuing=data.page<data.totalPages)
                        } }
                    }
                    // The list is already usable. Missing/old metadata APIs must not block importing.
                    runCatching { api.originalFitActivities() }.getOrNull()?.let { available ->
                        runOnUiThread { if(!closed.get() && listGeneration==generation && source==target) content.originalFits(available) }
                    }
                } catch(e:Exception) {
                    runOnUiThread { if(!closed.get()) content.failed(ServiceProblems.text(e,R.string.dreeve_import_load_failed)) }
                }
            }
        },importSelection={ activities ->
            runCatching { client() }.fold({ (target,api) ->
                if(target!=source) content.failed(R.string.dreeve_import_changed)
                else chooseBikes(activities,target,api)
            },{ content.failed(ServiceProblems.text(it,R.string.dreeve_import_download_failed)) })
        })
        content.navigation=dialogs
        dialog().setTitle(R.string.dreeve_import_title).setView(content).create().apply {
            dialogs.onDismiss(this) { closed.set(true);importCancelled.set(true);importProgress?.dismiss();importProgress=null;worker.shutdownNow() }
            show()
        }
        content.refresh()
    }
    /** The same anchored native dropdown serves imports and each route's actions. */
    private fun routeDropdown(anchor: View, choices: List<String>, icons: List<BicycleIcon> = emptyList(),
                              tags: List<String> = emptyList(), enabled:(Int)->Boolean={true}, selected: (Int) -> Unit) {
        routePopup?.dismiss()
        val popup = ListPopupWindow(settingsContext).apply {
            anchorView = anchor; isModal = true
            width = minOf(dp(BicycleDesign.popupWidth), resources.displayMetrics.widthPixels - dp(BicycleDesign.horizontalPadding * 2))
            setBackgroundDrawable(BicycleComponents.dropdownBackground(settingsContext,settingsThemes.active.tokens))
            setAdapter(object : ArrayAdapter<String>(settingsContext, android.R.layout.simple_list_item_1, choices) {
                override fun areAllItemsEnabled()=choices.indices.all(enabled)
                override fun isEnabled(position:Int)=enabled(position)
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                    ((convertView as? TextView) ?: TextView(settingsContext).apply {
                        BicycleComponents.bindTheme(this, settingsThemes) { tokens ->
                            setTextColor(tokens.foreground)
                            compoundDrawablesRelative.filterNotNull().forEach { it.setTint(tokens.foreground) }
                        }
                    }).apply {
                        text = getItem(position); textSize = BicycleDesign.bodySp
                        gravity = android.view.Gravity.CENTER_VERTICAL; minHeight = dp(BicycleDesign.popupRowHeight)
                        setPadding(dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding), dp(BicycleDesign.horizontalPadding), dp(BicycleDesign.verticalPadding))
                        tag = tags.getOrNull(position)
                        isEnabled=enabled(position);alpha=if(isEnabled) 1f else .5f
                        setCompoundDrawablesRelative(icons.getOrNull(position)?.let { kind -> BicycleIcons.drawable(settingsContext, kind, settingsThemes.active.tokens.foreground)
                            .apply { setBounds(0, 0, dp(BicycleDesign.iconSize), dp(BicycleDesign.iconSize)) } }, null, null, null)
                        compoundDrawablePadding = dp(BicycleDesign.verticalPadding)
                    }
            })
            setOnItemClickListener { _, _, position, _ ->
                dismiss()
                if (!runtime.mapState.busy && enabled(position)) selected(position)
            }
            setOnDismissListener { if (routePopup === this) routePopup = null }
        }
        routePopup = popup
        popup.show()
        popup.listView?.let { list -> BicycleComponents.bindTheme(list, settingsThemes) { tokens ->
            popup.setBackgroundDrawable(BicycleComponents.dropdownBackground(settingsContext,tokens))
            list.selector = BicycleComponents.controlBackground(settingsContext,tokens)
        } }
    }
    private fun dataDialog(title: Int, body: View, action: Int? = null): AlertDialog {
        val builder = dialog().setTitle(title).setView(body)
        action?.takeIf { it!=R.string.route_action_save }?.let { builder.setPositiveButton(it, null) }
        val screen = builder.create()
        screen.setCustomTitle(BicycleComponents.header(settingsContext, settingsThemes).apply {
            tag = "bicycle_dialog_header"; setPadding(dp(BicycleDesign.smallGap),0,dp(BicycleDesign.verticalPadding),0)
            addView(BicycleComponents.icon(settingsContext,settingsThemes,BicycleIcon.Back,getString(R.string.data_back)) { screen.dismiss() })
            addView(BicycleComponents.heading(settingsContext,settingsThemes,getString(title)).apply {
                tag="bicycle_dialog_title";setAutoSizeTextTypeUniformWithConfiguration(18,BicycleDesign.headingSp.toInt(),1,android.util.TypedValue.COMPLEX_UNIT_SP)
            },LinearLayout.LayoutParams(0,dp(BicycleDesign.headerHeight),1f))
        })
        screen.show(); return screen
    }
    private fun dataSettings(focusRoutes:Boolean=false) {
        val content = DataSettingsView(settingsContext,settingsThemes) { action -> when(action) {
            DataAction.HRV_IMPORT -> try {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                    .putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("text/csv","text/comma-separated-values","application/csv","text/plain","application/octet-stream","application/vnd.ms-excel")),19)
            } catch(_:android.content.ActivityNotFoundException) { BicycleFeedback.show(this,R.string.backup_picker_error,long=true) }
            DataAction.HRV_EXPORT -> Unit // Installed as destination commands below.
            DataAction.HRV_RESTORE -> chooseBackupSource(R.string.hrv_restore_title) { requestRideBackup(it,RideBackupAction.IMPORT,true) }
            DataAction.HRV_RESTORE_GROUP -> Unit
            DataAction.RIDE_RESTORE -> chooseBackupSource(R.string.ride_restore_title) { requestRideBackup(it,RideBackupAction.IMPORT) }
            DataAction.BACKUP -> Unit
            DataAction.RESTORE -> chooseBackupSource(R.string.data_restore,::requestSettingsRestore)
            DataAction.IMPORT -> Unit // Inline accordion; actions reuse importRoute below.
            DataAction.RIDE_BACKUP -> Unit
            DataAction.LOCAL -> routeExportSettings(RouteExportPart.LOCAL)
            DataAction.CLOUD -> routeExportSettings(RouteExportPart.CLOUD)
            DataAction.INTERVALS -> intervalsSettings()
            DataAction.DREEVE -> activityServiceSettings(ActivityService.DREEVE)
            DataAction.STRAVA -> activityServiceSettings(ActivityService.STRAVA)
        } }
        val screen=dataDialog(R.string.data_title,BicycleStickyScrollView(settingsContext).apply { addView(content) })
        dataSettingsScreen=screen;menu=screen
        backupStatus=content.backupStatus
        fun targets(action:(RideBackupDestination)->Unit)=BackupDestinationView(settingsContext,settingsThemes,
            cloudAvailable={ RouteExportSettings(this).backupConfigured() },action=action)
        content.install(DataAction.BACKUP,targets { requestBackupExport(it) })
        content.install(DataAction.RIDE_BACKUP,targets { requestRideBackup(it,RideBackupAction.EXPORT) })
        content.install(DataAction.HRV_EXPORT,targets { requestRideBackup(it,RideBackupAction.EXPORT,true) })
        val local=routeExportSettings(RouteExportPart.LOCAL,screen)
        content.install(DataAction.LOCAL,local,local.findViewWithTag("export_save"))
        val cloud=routeExportSettings(RouteExportPart.CLOUD,screen)
        content.install(DataAction.CLOUD,cloud,cloud.findViewWithTag("export_webdav"))
        val intervals=intervalsSettings(screen)
        content.install(DataAction.INTERVALS,intervals,intervals.findViewWithTag("intervals_enabled"))
        for(service in listOf(ActivityService.DREEVE,ActivityService.STRAVA)) {
            val form=activityServiceSettings(service,screen)
            content.install(if(service==ActivityService.DREEVE) DataAction.DREEVE else DataAction.STRAVA,form,form.findViewWithTag("service_enabled"))
        }
        if(focusRoutes) content.addOnLayoutChangeListener(object:View.OnLayoutChangeListener {
            override fun onLayoutChange(v:View,l:Int,t:Int,r:Int,b:Int,ol:Int,ot:Int,or:Int,ob:Int) {
                val scroll=content.parent as? ScrollView ?: return
                val heading=content.findViewWithTag<View>("data_routes_heading") ?: return
                content.removeOnLayoutChangeListener(this)
                scroll.post { scroll.scrollTo(0,heading.top) }
            }
        })
        dialogs.onDismiss(screen) {
            if(dataSettingsScreen===screen) dataSettingsScreen=null
            if(backupStatus===content.backupStatus) backupStatus=null
        }
    }
    private var backupStatus:TextView?=null
    private fun backupMessage(text:String) { backupStatus?.let { BicycleFeedback.update(it,text) } }
    private fun chooseBackupSource(title:Int,action:(RideBackupDestination)->Unit) {
        lateinit var screen:AlertDialog
        val body=BackupDestinationView(settingsContext,settingsThemes,{ RouteExportSettings(this).backupConfigured() }) {
            screen.dismiss();action(it)
        }
        screen=dialog(fullscreen=false).setTitle(title).setView(body).show()
    }
    private fun requestSettingsRestore(destination:RideBackupDestination) {
        if(destination==RideBackupDestination.LOCAL) try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip"),13)
        } catch(_:android.content.ActivityNotFoundException) { BicycleFeedback.show(this,R.string.backup_picker_error,true) }
        else {
            val settings=RouteExportSettings(this)
            if(!settings.backupConfigured()) return
            backupMessage(getString(R.string.ride_backup_loading))
            BackupWork.start(this,"Bicycle-settings-list") {
                val result=runCatching {
                    val root=DavBackupDirectory(WebDavClient(settings.read(),settings.password()))
                    root to root.entries().filter { !it.directory && it.name.startsWith("Bicycle-Settings-") && it.name.endsWith(".zip") }.map { it.name }.sortedDescending().also { require(it.isNotEmpty()) }
                }
                runOnUiThread { if(!isDestroyed) result.fold({ (root,names) ->
                    backupMessage("")
                    dialog().setTitle(R.string.data_restore).setItems(names.toTypedArray()) { _,index ->
                        BackupWork.start(this,"Bicycle-settings-fetch") {
                            val bytes=runCatching { root.open(names[index]).use { it.readNBytes(SettingsArchive.MAX_BYTES+1).also { require(it.size<=SettingsArchive.MAX_BYTES) } } }
                            runOnUiThread { if(!isDestroyed) bytes.fold(::openSettingsPayload) { BicycleFeedback.show(this,ServiceProblems.text(it,R.string.backup_io_error),true) } }
                        }
                    }.show()
                },{ BicycleFeedback.show(this,ServiceProblems.text(it,R.string.backup_io_error),true) }) }
            }
        }
    }
    private fun requestRideBackup(destination:RideBackupDestination,action:RideBackupAction,hrv:Boolean=false) {
        if(runtime.recording || runtime.saving || runtime.mapState.busy || runtime.hrv.busy) { BicycleFeedback.show(this,R.string.ride_backup_busy);return }
        if(destination==RideBackupDestination.LOCAL) {
            try {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        if(action==RideBackupAction.EXPORT) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0),
                    if(hrv) { if(action==RideBackupAction.EXPORT) 22 else 23 } else { if(action==RideBackupAction.EXPORT) 16 else 17 })
            } catch(_:android.content.ActivityNotFoundException) { BicycleFeedback.show(this,R.string.backup_picker_error,true) }
        } else {
            val settings=RouteExportSettings(this)
            if(!settings.backupConfigured()) { BicycleFeedback.show(this,R.string.ride_backup_cloud_missing);return }
            chooseRideBackupDirectory(DavBackupDirectory(WebDavClient(settings.read(),settings.password())),action,hrv)
        }
    }
    private fun chooseRideBackupDirectory(root:BackupDirectory,action:RideBackupAction,hrv:Boolean=false) {
        if(action==RideBackupAction.EXPORT) { runRideBackup(root,false,hrv);return }
        backupMessage(getString(R.string.ride_backup_loading))
        BackupWork.start(this,"Bicycle-backup-folders") {
            val result=runCatching {
                val entries=root.entries()
                if(entries.any { !it.directory && it.name==if(hrv) HrvDirectoryBackup.INDEX else RideDirectoryBackup.INDEX }) null else
                    entries.filter { it.directory && it.name.startsWith(if(hrv) HrvDirectoryBackup.PREFIX else RideDirectoryBackup.PREFIX) }.map { it.name }.sortedDescending().also { require(it.isNotEmpty()) }
            }
            runOnUiThread { if(!isDestroyed) result.fold({ folders ->
                backupMessage("")
                fun chosen(directory:BackupDirectory) {
                    dialog(fullscreen=false).setTitle(if(hrv) R.string.hrv_restore_title else R.string.ride_restore_title)
                        .setMessage(if(hrv) R.string.backup_hrv_restore_confirm else R.string.ride_backup_confirm)
                        .setPositiveButton(R.string.backup_restore_execute) { _,_->runRideBackup(directory,true,hrv) }.show()
                }
                if(folders==null) chosen(root)
                else dialog().setTitle(R.string.ride_backup_choose).setItems(folders.toTypedArray()) { _,position ->
                    chosen(root.child(folders[position],false))
                }.show()
            },{ BicycleFeedback.show(this,ServiceProblems.text(it,R.string.ride_backup_invalid),true) }) }
        }
    }
    private fun runRideBackup(directory:BackupDirectory,restore:Boolean,hrv:Boolean=false):Boolean {
        if(runtime.recording || runtime.saving || runtime.mapState.busy || runtime.hrv.busy) { BicycleFeedback.show(this,R.string.ride_backup_busy);return false }
        backupMessage(getString(R.string.backup_processing))
        val started=runtime.mapState.transferBackup(directory,restore,hrv,
            progress={ backupMessage(getString(R.string.backup_progress,it)) },
            complete={ result ->
                val text=result.fold({ report ->
                    if(restore) getString(R.string.ride_backup_import_result,report.completed,report.duplicates,report.failed)
                    else getString(if(hrv) R.string.backup_hrv_result else R.string.ride_backup_result,report.completed,report.failed,report.folder)
                },{ getString(ServiceProblems.text(it,R.string.ride_backup_failed)) })
                if(restore && result.isSuccess && !hrv) runtime.mapState.restoreProfileRoute()
                if(!isDestroyed) { backupMessage(text);BicycleFeedback.show(this,text,long=true);render() }
            })
        if(!started) BicycleFeedback.show(this,R.string.ride_backup_busy)
        return started
    }
    private fun backupError(error: Throwable): Int = when((error as? SettingsBackupFailure)?.problem) {
        BackupProblem.FORMAT->R.string.backup_format_error;BackupProblem.VERSION->R.string.backup_version_error
        BackupProblem.TOO_LARGE->R.string.backup_size_error;BackupProblem.SELECTION->R.string.backup_selection_error
        BackupProblem.INVALID->R.string.backup_invalid_error;BackupProblem.ROLLBACK->R.string.backup_rollback_error
        BackupProblem.PASSWORD->R.string.backup_password_failed;BackupProblem.CREDENTIALS->R.string.backup_credentials_failed
        else->R.string.backup_io_error
    }
    private fun archivePassword(create:Boolean,perform:(CharArray,(Result<Unit>)->Unit)->Unit) {
        val generation=++passwordOperation
        lateinit var screen:AlertDialog
        val body=SettingsPasswordView(settingsContext,settingsThemes,create) { password,finish ->
            perform(password) { result ->
                finish(result)
                if(result.isSuccess) screen.dismiss()
            }
        }
        screen=dialog(fullscreen=false).setTitle(if(create) R.string.backup_password_title else R.string.backup_unlock_title).setView(body).show()
        dialogs.onDismiss(screen) { if(passwordOperation==generation) passwordOperation++ }
    }
    private fun openSettingsPayload(bytes:ByteArray) {
        if(!SettingsArchive.isZip(bytes)) { BicycleFeedback.show(this,R.string.backup_format_error,true);return }
        archivePassword(false) { password,finish ->
            val generation=passwordOperation
            BackupWork.start(this,"Bicycle-settings-unlock",failed={ password.fill('\u0000');finish(Result.failure(it)) }) {
                val result=try { runCatching { SettingsArchive(applicationContext).read(bytes,password) } } finally { password.fill('\u0000') }
                runOnUiThread {
                    if(!isDestroyed && passwordOperation==generation) result.fold({ document -> backupSettings(document);finish(Result.success(Unit)) },{ finish(Result.failure(it)) })
                }
            }
        }
    }
    private fun requestBackupExport(destination:RideBackupDestination) {
        archivePassword(true) { password,finish ->
            val generation=passwordOperation
            BackupWork.start(this,"Bicycle-settings-export",failed={ password.fill('\u0000');finish(Result.failure(it)) }) {
                val result:Result<java.io.File?> = try { runCatching {
                    val bytes=SettingsArchive(applicationContext).export(BackupSection.entries.toSet(),password)
                    val file=java.io.File(cacheDir,"settings-export-${java.util.UUID.randomUUID()}.zip").apply { writeBytes(bytes) }
                    if(destination==RideBackupDestination.WEBDAV) {
                        // Keep encryption and transfer in one protected operation, even if the screen locks between them.
                        try {
                            val settings=RouteExportSettings(applicationContext)
                            require(settings.backupConfigured())
                            DavBackupDirectory(WebDavClient(settings.read(),settings.password())).write("Bicycle-Settings-${java.time.LocalDate.now()}-${java.util.UUID.randomUUID().toString().take(8)}.zip",file)
                        } finally { file.delete() }
                        null
                    } else file
                } } finally { password.fill('\u0000') }
                runOnUiThread {
                    if(isDestroyed || passwordOperation!=generation) result.getOrNull()?.delete()
                    else result.fold({ file ->
                        if(file==null) {
                            finish(Result.success(Unit));BicycleFeedback.show(this,R.string.backup_written,true)
                        } else {
                            pendingBackupExport?.delete();pendingBackupExport=file
                            try {
                                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip")
                                    .putExtra(Intent.EXTRA_TITLE,getString(R.string.backup_filename,java.time.LocalDate.now().toString())),12)
                                finish(Result.success(Unit))
                            } catch(e:Exception) { file.delete();pendingBackupExport=null;finish(Result.failure(e)) }
                        }
                    },{ finish(Result.failure(it));BicycleFeedback.show(this,if(destination==RideBackupDestination.WEBDAV) ServiceProblems.text(it,backupError(it)) else backupError(it),true) })
                }
            }
        }
    }
    private fun backupSettings(document: SettingsBackupDocument) {
        val backup=SettingsBackup(this,settingsThemes)
        val choices=BackupSelectionView(settingsContext,settingsThemes,document.sections.keys)
        val screen=dataDialog(R.string.data_restore,
            ScrollView(settingsContext).apply { addView(choices) },R.string.backup_restore_execute)
        val button=screen.getButton(AlertDialog.BUTTON_POSITIVE)
        BicycleDialogStyle.actionIcon(button,settingsThemes,BicycleIcon.RestoreSettings)
        choices.changed={ button.isEnabled=choices.selected().isNotEmpty() }
        button.setOnClickListener {
            val selected=choices.selected()
            run {
                dialog(fullscreen=false).setTitle(R.string.backup_confirm_title)
                    .setMessage(getString(R.string.backup_confirm_message,selected.joinToString(", ") { getString(it.title) }))
                    .setPositiveButton(R.string.backup_restore_execute) { _,_->
                        if(runtime.recording || runtime.saving) { BicycleFeedback.show(this,R.string.ride_backup_busy);return@setPositiveButton }
                        button.isEnabled=false
                        BackupWork.start(this,"B-Cycle-settings-restore",failed={ button.isEnabled=choices.selected().isNotEmpty();BicycleFeedback.show(this,backupError(it),true) }) {
                            val result=runCatching {
                                val plan=backup.plan(document,selected);backup.apply(plan)
                                if(BackupSection.DESIGN in selected || BackupSection.PROFILE in selected) settingsThemes.reloadFromPreferences()
                                if(BackupSection.CLOUD in selected) {
                                    RouteUploads.settingsChanged(applicationContext)
                                    IntervalsUploads.settingsChanged(applicationContext)
                                    ActivityService.entries.forEach { ActivityServiceUploads.forService(it).settingsChanged(applicationContext) }
                                }
                                plan
                            }
                            runOnUiThread {
                                if(!isDestroyed) {
                                    button.isEnabled=choices.selected().isNotEmpty()
                                    result.fold({ plan ->
                                        if(plan.bike.containsKey(BikeProfileStore.KEY)) runtime.restoredBikeSettings()
                                        else if(BackupSection.MAPS in selected) runtime.mapState.restoreProfileRoute()
                                        if(BackupSection.DASHBOARD in selected || BackupSection.PROFILE in selected) {
                                            dashboardPage=dashboardPages.selected()
                                            dashboardLayout=DashboardLayout.decode(preferences.getString(dashboardKey,null))
                                            dashboardGraphs=GraphOverlayConfig.read(preferences,dashboardPage); dashboardTrainingHelp=DashboardTrainingHelp.read(preferences,dashboardPage); dashboardDataVisibility=DashboardDataVisibility.read(preferences,dashboardPage); dashboardTrendIndicators=TrendIndicatorSettings.read(preferences)
                                            dashboard.setTemplate(currentTemplate());dashboardArea.requestLayout()
                                            (mapPanel.layoutParams as LinearLayout.LayoutParams).weight=1f-currentTemplate().dataFraction
                                        }
                                        if(BackupSection.MAPS in selected || BackupSection.DASHBOARD in selected || BackupSection.PROFILE in selected) mapPanel.refreshSettings()
                                        render();screen.dismiss()
                                        BicycleFeedback.show(this, if(plan.cloudNeedsPassword) R.string.backup_cloud_secret else R.string.backup_restored, long = true)
                                    },{ BicycleFeedback.show(this, backupError(it), long = true) })
                                }
                            }
                        }
                    }.show()
            }
        }
    }
    private fun backupResult(requestCode:Int,resultCode:Int,data:Intent?) {
        if(dialogs.empty) { showMenu();dataSettings() }
        val file=if(requestCode==12) pendingBackupExport.also { pendingBackupExport=null } else null
        val uri=data?.data
        if(resultCode!=RESULT_OK || uri==null) { file?.delete();return }
        val origin=dataSettingsScreen
        BackupWork.start(this,"B-Cycle-settings-document",failed={ file?.delete();BicycleFeedback.show(this,backupError(it),true) }) {
            val result=runCatching {
                if(requestCode==12) {
                    require(file!=null && file.isFile)
                    contentResolver.openOutputStream(uri,"wt")?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: throw IOException()
                    null
                } else contentResolver.openInputStream(uri)?.use { it.readNBytes(SettingsArchive.MAX_BYTES+1).also { bytes ->
                    if(bytes.size>SettingsArchive.MAX_BYTES) throw SettingsBackupFailure(BackupProblem.TOO_LARGE)
                } } ?: throw IOException()
            }
            file?.delete()
            runOnUiThread {
                if(!isDestroyed && dataSettingsScreen===origin && origin?.isShowing==true) result.fold({ bytes ->
                    if(bytes==null) BicycleFeedback.show(this,R.string.backup_written,true) else openSettingsPayload(bytes)
                },{ BicycleFeedback.show(this,backupError(it),true) })
            }
        }
    }
    private fun sourcesSettings() {
        val body=BicycleComponents.dialogBody(settingsContext,settingsThemes)
        for((title,description) in listOf(R.string.sources_maps to R.string.sources_maps_text,
            R.string.sources_elevation to R.string.sources_elevation_text,R.string.sources_weather to R.string.sources_weather_text,
            R.string.sources_uploads to R.string.sources_uploads_text)) {
            body.addView(BicycleComponents.contentHeading(settingsContext,settingsThemes,getString(title)))
            body.addView(BicycleComponents.text(settingsContext,settingsThemes,getString(description)).apply {
                android.text.util.Linkify.addLinks(this,android.text.util.Linkify.WEB_URLS)
                movementMethod=android.text.method.LinkMovementMethod.getInstance()
                BicycleComponents.bindTheme(this,settingsThemes) { setLinkTextColor(it.foreground) }
            },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap);bottomMargin=dp(BicycleDesign.controlGap) })
        }
        dataDialog(R.string.sources_title,ScrollView(settingsContext).apply { addView(body) })
    }
    private fun activityServiceCallback(intent:Intent) {
        val uri=intent.data ?: return
        if(uri.scheme!="bcycle" || uri.host!="localhost" || uri.path!="/strava") return
        intent.data=null
        val settings=ActivityServiceSettings(this,ActivityService.STRAVA)
        val code=runCatching { settings.consumeCallback(uri) }
        if(code.isFailure) { BicycleFeedback.show(this,getString(R.string.service_auth_failed));return }
        settingsWriter.execute {
            val result=runCatching { StravaClient(settings).authorize(code.getOrThrow());ActivityServiceUploads.strava.settingsChanged(applicationContext) }
            runOnUiThread {
                if(isDestroyed) return@runOnUiThread
                val text=getString(if(result.isSuccess) R.string.service_connected else ServiceProblems.text(result.exceptionOrNull(),R.string.service_auth_failed))
                activityServiceView?.takeIf { it.service==ActivityService.STRAVA }?.let {
                    it.showAccount(settings.read().account);BicycleFeedback.update(it.result,text)
                } ?: BicycleFeedback.show(this,text)
            }
        }
    }
    private fun activityServiceSettings(service:ActivityService,owner:AlertDialog?=null):View {
        val settings=ActivityServiceSettings(this,service)
        val worker=java.util.concurrent.Executors.newSingleThreadExecutor()
        val revision=java.util.concurrent.atomic.AtomicLong()
        val closed=java.util.concurrent.atomic.AtomicBoolean()
        var syncing=false
        lateinit var content:ActivityServiceView
        content=ActivityServiceView(settingsContext,settingsThemes,service,settings.read(),settings.hasSecret(),syncNames={
            if(!syncing) {
                if(runtime.recording || runtime.saving || runtime.mapState.busy) BicycleFeedback.show(this,R.string.ride_backup_busy)
                else {
                    val localNames=runtime.mapState.entries.associate { it.id to it.name }
                    val token=revision.get();syncing=true;content.nameSync.isEnabled=false
                    BicycleFeedback.update(content.nameSyncStatus,getString(R.string.dreeve_names_starting))
                    worker.execute {
                        fun cancelled()=closed.get() || revision.get()!=token
                        val result=runCatching {
                            check(settings.dreeveImportConfigured())
                            val config=settings.read();val account=config.identity(service)
                            val client=DreeveClient(config,settings.credentials().getString("secret"),::cancelled)
                            fun exists(id:String)=listOf("fit-library/$id.fit","gpx-library/$id.gpx","dreeve-summaries/$id.json").any { java.io.File(filesDir,it).isFile }
                            RideNames.pullDreeve(applicationContext,account,localNames,::exists,::cancelled,{ client.activityName(it.activityId) }) { done,total ->
                                runOnUiThread { if(!closed.get() && !isDestroyed) BicycleFeedback.update(content.nameSyncStatus,getString(R.string.dreeve_names_progress,done,total)) }
                            }
                        }
                        runOnUiThread {
                            syncing=false
                            if(!closed.get() && !isDestroyed) {
                                content.nameSync.isEnabled=settings.dreeveImportConfigured()
                                val text=result.fold({ report -> getString(R.string.dreeve_names_result,report.updated,report.unchanged,report.failed)+(report.problem?.let { "\n"+getString(it) } ?: "")+
                                    if(report.interrupted) "\n"+getString(R.string.dreeve_names_interrupted) else "" },{ getString(ServiceProblems.text(it,R.string.service_check_settings)) })
                                BicycleFeedback.update(content.nameSyncStatus,text);refreshDetails();render()
                            }
                        }
                    }
                }
            }
        }) {
            val config=content.config();val secret=content.newSecret();content.busy(true)
            worker.execute {
                var authorization:android.net.Uri?=null
                val result=runCatching {
                    settings.write(config,secret)
                    if(service==ActivityService.DREEVE) DreeveClient(settings.read(),settings.credentials().getString("secret")).test()
                    else authorization=settings.authorizationUrl()
                }
                runOnUiThread {
                    if(isDestroyed) return@runOnUiThread
                    content.busy(false)
                    if(result.isSuccess && authorization!=null) {
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW,authorization)) }.onFailure {
                            BicycleFeedback.update(content.result,getString(R.string.service_auth_failed))
                        }
                    } else BicycleFeedback.update(content.result,getString(if(result.isSuccess) R.string.route_export_test_ok
                        else ServiceProblems.text(result.exceptionOrNull(),R.string.service_check_settings)))
                }
            }
        }
        if(service==ActivityService.DREEVE) content.nameSync.isEnabled=settings.dreeveImportConfigured()
        ActivityServiceUploads.forService(service).problem(this)?.let { BicycleFeedback.update(content.result,getString(it)) }
        activityServiceView=content
        val screen=owner ?: dataDialog(service.title,ScrollView(settingsContext).apply { addView(content) })
        observeSettings(content) {
            val config=content.config();val secret=content.newSecret();val token=revision.incrementAndGet()
            worker.execute {
                if(token==revision.get()) {
                    val result=runCatching { settings.write(config,secret);ActivityServiceUploads.forService(service).settingsChanged(applicationContext) }
                    runOnUiThread { if(token==revision.get() && !isDestroyed) {
                        val input=if(service==ActivityService.DREEVE) content.address else content.clientId
                        input.error=if(result.isFailure) getString(R.string.service_check_settings) else null
                        content.showAccount(settings.read().account)
                        if(service==ActivityService.DREEVE) content.nameSync.isEnabled=!syncing && result.isSuccess && settings.dreeveImportConfigured()
                    } }
                }
            }
        }
        dialogs.onDismiss(screen) { closed.set(true);if(activityServiceView===content) activityServiceView=null;worker.shutdown() }
        return content
    }
    private fun intervalsSettings(owner:AlertDialog?=null):View {
        val settings=IntervalsSettings(this)
        val worker=java.util.concurrent.Executors.newSingleThreadExecutor()
        lateinit var content:IntervalsView
        content=IntervalsView(settingsContext,settingsThemes,settings.read(),settings.hasKey()) {
            val config=content.config();val typedKey=content.newKey()
            content.testing(true)
            worker.execute {
                val result=runCatching { IntervalsClient(config,typedKey ?: settings.apiKey()).test() }
                runOnUiThread {
                    content.testing(false)
                    BicycleFeedback.update(content.result,getString(if(result.isSuccess) R.string.route_export_test_ok
                        else ServiceProblems.text(result.exceptionOrNull(),R.string.intervals_connection_failed)))
                }
            }
        }
        val screen=owner ?: dataDialog(R.string.intervals_title,ScrollView(settingsContext).apply { addView(content) })
        val closed=java.util.concurrent.atomic.AtomicBoolean(false)
        var bulkStop:java.util.concurrent.atomic.AtomicBoolean?=null
        var hrvStop:java.util.concurrent.atomic.AtomicBoolean?=null
        content.uploadAll={
            val running=bulkStop
            if(running!=null) { running.set(true);BicycleFeedback.update(content.bulkResult,getString(R.string.intervals_bulk_stopping)) }
            else if(runtime.recording || runtime.saving || runtime.mapState.busy || runtime.hrv.busy) BicycleFeedback.show(this,R.string.ride_backup_busy)
            else {
                val stopped=java.util.concurrent.atomic.AtomicBoolean(false);bulkStop=stopped
                content.bulkRunning(true);BicycleFeedback.update(content.bulkResult,getString(R.string.intervals_bulk_starting))
                // Reuse the settings worker so pending athlete/key edits are committed first.
                worker.execute { runOnUiThread {
                    if(closed.get()) return@runOnUiThread
                    val started=runtime.mapState.uploadAllIntervals({ stopped.get() || closed.get() || runtime.recording || runtime.saving || runtime.hrv.busy },
                        progress={ done,total,item -> if(!closed.get()) BicycleFeedback.update(content.bulkResult,getString(R.string.intervals_bulk_progress,done,total,item.name)) },
                        complete={ result ->
                            bulkStop=null
                            if(!closed.get()) {
                                content.bulkRunning(false)
                                BicycleFeedback.update(content.bulkResult,result.fold({ report ->
                                    getString(R.string.intervals_bulk_done,report.count(IntervalsBulkState.UPLOADED),report.count(IntervalsBulkState.EXISTS),report.count(IntervalsBulkState.SKIPPED),report.count(IntervalsBulkState.UNCLEAR),report.count(IntervalsBulkState.FAILED))+
                                        (if(report.interrupted) "\n"+getString(R.string.intervals_bulk_interrupted) else "")+
                                        report.problem?.let { "\n"+getString(it) }.orEmpty()+
                                        report.items.filter { it.state in setOf(IntervalsBulkState.FAILED,IntervalsBulkState.UNCLEAR,IntervalsBulkState.SKIPPED) }.take(20).joinToString("") { item ->
                                            "\n"+item.name+" · "+getString(item.problem ?: if(item.state==IntervalsBulkState.UNCLEAR) R.string.intervals_bulk_unclear else R.string.intervals_bulk_skipped)
                                        }
                                },{ getString(ServiceProblems.text(it,R.string.intervals_upload_failed)) }))
                            }
                        })
                    if(!started) { bulkStop=null;content.bulkRunning(false);BicycleFeedback.update(content.bulkResult,getString(R.string.ride_backup_busy)) }
                } }
            }
        }
        content.uploadAllHrv={
            val running=hrvStop
            if(running!=null) { running.set(true);BicycleFeedback.update(content.hrvResult,getString(R.string.intervals_bulk_stopping)) }
            else if(bulkStop!=null || runtime.recording || runtime.saving || runtime.mapState.busy || runtime.hrv.busy) BicycleFeedback.show(this,R.string.ride_backup_busy)
            else {
                val stop=java.util.concurrent.atomic.AtomicBoolean(false);hrvStop=stop
                val cancelled={ stop.get() || closed.get() || runtime.recording || runtime.saving || runtime.mapState.busy || runtime.hrv.busy }
                content.hrvRunning(true);BicycleFeedback.update(content.hrvResult,getString(R.string.intervals_hrv_starting))
                // Read on the existing HRV storage worker; no RR arrays or network IO on the UI thread.
                worker.execute { runOnUiThread {
                    if(!closed.get()) runtime.hrv.intervalsWellness { snapshot ->
                        if(!closed.get()) worker.execute {
                            val result=snapshot.mapCatching { rows -> IntervalsHrvUpload(applicationContext).run(rows,cancelled) { done,total,item ->
                                runOnUiThread { if(!closed.get()) BicycleFeedback.update(content.hrvResult,getString(R.string.intervals_bulk_progress,done,total,item.name)) }
                            } }
                            runOnUiThread {
                                hrvStop=null
                                if(!closed.get()) {
                                    content.hrvRunning(false)
                                    BicycleFeedback.update(content.hrvResult,result.fold({ report ->
                                        (report.detail ?: report.problem?.let(::getString))?.let { "$it\n" }.orEmpty()+
                                            getString(R.string.intervals_hrv_done,report.count(IntervalsBulkState.UPLOADED),report.count(IntervalsBulkState.EXISTS),report.count(IntervalsBulkState.FAILED))+
                                            (if(report.interrupted && report.count(IntervalsBulkState.FAILED)==0) "\n"+getString(R.string.intervals_hrv_interrupted) else "")
                                    },{ getString(ServiceProblems.text(it,R.string.intervals_hrv_failed)) }))
                                }
                            }
                        }
                    }
                } }
            }
        }
        val revision=java.util.concurrent.atomic.AtomicLong()
        observeSettings(content) {
            val config=content.config();val key=content.newKey();val token=revision.incrementAndGet()
            worker.execute {
                if(token==revision.get()) {
                    val result=runCatching { settings.write(config,key);IntervalsUploads.settingsChanged(applicationContext) }
                    runOnUiThread {
                        if(token==revision.get()) {
                            val error=if(result.isFailure) getString(R.string.intervals_check_settings) else null
                            content.athlete.error=error
                        }
                    }
                }
            }
        }
        dialogs.onDismiss(screen) { closed.set(true);bulkStop?.set(true);hrvStop?.set(true);worker.shutdown() }
        return content
    }
    private fun routeExportSettings(part: RouteExportPart,owner:AlertDialog?=null):View {
        val settings = RouteExportSettings(this)
        lateinit var content: RouteExportView
        content = RouteExportView(settingsContext, exportDraft ?: settings.read(), settings.hasPassword(), {
            exportDraft = content.read(); exportPasswordDraft = content.password.text.toString().takeIf { it.isNotEmpty() }
            try {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), 10)
            } catch (_: android.content.ActivityNotFoundException) {
                BicycleFeedback.show(this, getString(R.string.route_export_no_picker), long = true)
            }
        }, {
            val config = content.read()
            val newPassword = content.password.text.toString().takeIf { it.isNotEmpty() }
            content.testing(true)
            Thread({
                val message = try {
                    WebDavClient(config, newPassword ?: settings.password()).test(); getString(R.string.route_export_test_ok)
                } catch (e: Exception) {
                    ServiceProblems.of(e)?.let { getString(it.text) } ?: when (e) {
                        is DavFailure -> e.message ?: getString(R.string.ui_connection_failed)
                        is IllegalArgumentException -> e.message ?: getString(R.string.ui_check_settings)
                        else -> getString(R.string.route_export_test_failed)
                    }
                }
                runOnUiThread { content.testing(false); BicycleFeedback.update(content.result, message) }
            }, "B-Cycle-WebDAV-Test").start()
        }, settingsThemes, storedPassword = {
            runCatching { settings.password() }.getOrElse {
                BicycleFeedback.update(content.result, getString(R.string.route_export_password_failed))
                ""
            }
        }, part = part)
        BicycleDialogStyle.content(content, settingsThemes)
        exportPasswordDraft?.let { content.password.setText(it) }
        exportDraft = null; exportPasswordDraft = null
        val screen = owner ?: dataDialog(if(part == RouteExportPart.LOCAL) R.string.data_local else R.string.data_cloud,
            ScrollView(settingsContext).apply { addView(content) }, R.string.route_action_save)
        if(part==RouteExportPart.LOCAL) {
            localExportView=content
            dialogs.onDismiss(screen) { if(localExportView===content) localExportView=null }
        }
        var saveRevision=0L
        observeSettings(content) {
            if(content.restoringPassword) return@observeSettings
            val revision=++saveRevision
            try {
                val draft = content.read(); val current = settings.read()
                val config = if(part == RouteExportPart.LOCAL) current.copy(save = draft.save, tree = draft.tree)
                    else current.copy(webdav = draft.webdav, server = draft.server, user = draft.user, folder = draft.folder, wifiOnly = draft.wifiOnly)
                val password = if(part == RouteExportPart.LOCAL) null else content.password.text.toString().takeIf { it.isNotEmpty() }
                if (part == RouteExportPart.CLOUD && config.webdav) {
                    WebDavClient.directory(config)
                    require(password != null || settings.hasPassword()) { getString(R.string.route_export_need_password) }
                }
                val appContext = applicationContext
                settingsWriter.execute {
                    val success = runCatching {
                        val latest=settings.read()
                        val merged=if(part==RouteExportPart.LOCAL) latest.copy(save=draft.save,tree=draft.tree)
                            else latest.copy(webdav=draft.webdav,server=draft.server,user=draft.user,folder=draft.folder,wifiOnly=draft.wifiOnly)
                        settings.write(merged,password);RouteUploads.settingsChanged(appContext)
                    }.isSuccess
                    runOnUiThread {
                        if (!isDestroyed && revision==saveRevision) {
                            if (success) { routeSignature = ""; refreshRouteRows(); BicycleFeedback.update(content.result, "") }
                            else BicycleFeedback.update(content.result, getString(R.string.route_export_save_failed))
                        }
                    }
                }
            } catch (e: Exception) {
                BicycleFeedback.update(content.result, if (e is IllegalArgumentException) e.message else getString(R.string.route_export_save_failed))
            }
        }
        return content
    }
    private fun refreshRouteRows() {
        refreshDetails()
        offerOrphanProfiles()
        val state = runtime.mapState
        if (!state.busy && pendingMapRoute != null) {
            if (pendingMapRoute == state.selectedRoute && state.route != null) {
                dialogs.closeAll(); mapPanel.fitRoute()
            }
            pendingMapRoute = null
        }
        val manager = routeManager ?: return
        manager.showImportStatus(state.archiveStatus)
        routeImportButton?.isEnabled = !state.busy
        val displayed = state.selectedRoute.takeIf { state.route != null && preferences.getBoolean("mapGpxVisible", true) }
        val signature = "${state.routeRevision}:${state.busy}:${RouteCloudUploads.revision(this)}:$displayed"
        if (routeSignature == signature) return
        routeSignature = signature
        manager.submit(state.entries, displayed, state.busy,
            state.mapSlots.ids.mapIndexedNotNull { index,id -> id?.let { it to index+1 } }.toMap(),state.visibleRouteIds())
    }
    private fun renameRoute(entry: ManagedRoute) {
        val input = EditText(settingsContext).apply { setText(entry.name); setSingleLine(); selectAll()
            filters = arrayOf(android.text.InputFilter.LengthFilter(120)); tag = "route_rename" }
        val rename = dialog().setTitle(getString(R.string.route_rename_title)).setView(input)
            .create()
        rename.show()
        val handler=android.os.Handler(android.os.Looper.getMainLooper())
        var pending: String?=null
        val save=object : Runnable {
            override fun run() {
                val name=pending ?: return
                if(runtime.mapState.busy) { handler.postDelayed(this,50);return }
                pending=null;runtime.mapState.renameRoute(entry.id,name)
            }
        }
        input.onTextChanged {
            val name=input.text.toString().trim()
            if(name.isEmpty()) input.error=getString(R.string.route_name_required)
            else { input.error=null;pending=name;handler.removeCallbacks(save);handler.postDelayed(save,200) }
        }
    }
    private fun deleteRoute(entry: ManagedRoute) {
        dialog(fullscreen = false).setTitle(getString(R.string.route_delete_title))
            .setMessage(getString(R.string.route_delete_message, entry.name))
            .setPositiveButton(getString(R.string.route_action_delete)) { _, _ -> runtime.mapState.deleteRoute(entry.id) }
            .show()
    }
    private fun detailEntry(detail: DetailSession): ManagedRoute? {
        val current = runtime.mapState.entry(detail.id) ?: return null
        val verified = detail.analysis?.entry?.fileStamp
        val stamp = if (verified != null && verified.length == current.fileStamp?.length && verified.modified == current.fileStamp.modified) verified else current.fileStamp
        return current.copy(fileStamp = stamp)
    }
    private fun refreshDetails() {
        val detail = routeDetail ?: return
        val entry = detailEntry(detail)
        if (!runtime.mapState.busy) {
            if (entry == null) { detail.screen.dismiss(); return }
            detail.screen.window?.decorView?.findViewWithTag<TextView>("analysis_title")?.apply {
                if (text.toString() != entry.name) text = entry.name
                contentDescription = entry.name
            }
            detail.analysis = detail.analysis?.let { it.copy(entry = it.entry.copy(name = entry.name, fileStamp = entry.fileStamp)) }
        }
        detail.transfers?.let { if(it.popup.isShowing) it.refresh() }
        val signature = "${runtime.mapState.routeRevision}:${runtime.mapState.busy}:${runtime.recording}:${runtime.saving}:${RouteCloudUploads.revision(this)}:${RouteLocalSaves.revision(this)}:${settingsThemes.active.tokens.hashCode()}:${detail.analysis?.route != null}:${entry?.fileStamp}"
        if (signature == detail.signature) return
        detail.signature = signature
        val kinds = listOf(BicycleIcon.ShowOnMap,BicycleIcon.RenameTrack,BicycleIcon.CorrectElevation,BicycleIcon.Cloud,BicycleIcon.DeleteTrack)
        kinds.indices.forEach { index ->
            val button = detail.footer.getChildAt(index) as BicycleFooterAction
            button.setIcon(kinds[index], settingsThemes.active.tokens.foreground)
            button.isEnabled = !runtime.mapState.busy && entry != null && when (index) {
                0 -> detail.analysis?.route != null
                2 -> !runtime.recording && !runtime.saving && entry.format == "FIT" && entry.origin == TrackOrigin.RECORDED && entry.hasRoute
                else -> true
            }
        }
    }
    private fun executeDetailAction(detail: DetailSession, index: Int) {
        if (runtime.mapState.busy) return
        val entry = detailEntry(detail) ?: return
        when (index) {
            0 -> detail.analysis?.let(::routeAnalysisMap)
            1 -> renameRoute(entry)
            2 -> detail.content.correctElevation?.invoke()
            3 -> {
                detail.transfers?.dismiss()
                detail.transfers=RouteTransferMenu(settingsContext,settingsThemes,detail.footer.getChildAt(3),
                    { detailEntry(detail) },{ runtime.mapState.busy },{ target ->
                        if(target==RouteTransferTarget.LOCAL) runtime.mapState.exportRoute(detail.id)
                        else runtime.mapState.uploadRoute(detail.id,target)
                    },{ dataSettings(focusRoutes=true) },recheck={ target -> runtime.mapState.recheckTransfer(detail.id,target) }).also { it.show() }
            }
            4 -> deleteRoute(entry)
        }
    }
    private fun openMapLink(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
        catch (_: android.content.ActivityNotFoundException) { BicycleFeedback.show(this, getString(R.string.ui_browser_missing), long = false) }
    }
}
