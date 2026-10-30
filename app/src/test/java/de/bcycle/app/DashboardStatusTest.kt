package de.bcycle.app

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.widget.Switch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardStatusTest {
    private val c get()=RuntimeEnvironment.getApplication()
    private val full=DashboardStatusConfig(listOf(DashboardStatusItem.TIME,DashboardStatusItem.NOTIFICATIONS,DashboardStatusItem.CELLULAR,
        DashboardStatusItem.VPN,DashboardStatusItem.BATTERY,DashboardStatusItem.EMPTY))
    private fun layout(v:View,w:Int=360,h:Int=500) { v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h) }
    @Test fun realDesignPickerSavesSlotsAndOnlyShowsTheStripWithDashboardFullscreen() {
        val host=Robolectric.buildActivity(MainActivity::class.java).setup().visible()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0)
            val strip=a.window.decorView.findViewWithTag<DashboardStatusStrip>("dashboard_status_strip")
            assertEquals(View.GONE,strip.visibility)
            MainActivity::class.java.getDeclaredMethod("dashboardSettings").apply { isAccessible=true }.invoke(a)
            val screen=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            val root=screen.window!!.decorView
            root.findViewWithTag<View>("dashboard_status_expand").performClick()
            root.findViewWithTag<View>("dashboard_status_slot_0").performClick()
            val settings=root.findViewWithTag<DashboardStatusSettingsView>("dashboard_fullscreen_settings")
            settings.popup!!.contentView.findViewWithTag<View>("dashboard_status_choice_TIME").performClick()
            assertEquals(DashboardStatusItem.TIME,DashboardStatusConfig.read(prefs).slots[0])
            root.findViewWithTag<View>("dashboard_status_slot_3").performClick()
            settings.popup!!.contentView.findViewWithTag<View>("dashboard_status_choice_VPN").performClick()
            assertEquals(DashboardStatusItem.VPN,DashboardStatusConfig.read(prefs).slots[3])
            screen.dismiss();a.onWindowFocusChanged(true)
            assertEquals(View.VISIBLE,strip.visibility)
            prefs.edit().putBoolean(DashboardFullscreen.key,false).commit();a.onWindowFocusChanged(true)
            assertEquals(View.GONE,strip.visibility)
            prefs.edit().putBoolean(DashboardFullscreen.key,true).commit();a.onWindowFocusChanged(true)
            assertEquals(View.VISIBLE,strip.visibility)
            host.pause().stop()
            val monitor=MainActivity::class.java.getDeclaredField("dashboardStatusMonitor").apply { isAccessible=true }.get(a)
            assertNull(DashboardStatusMonitor::class.java.getDeclaredField("receiver").apply { isAccessible=true }.get(monitor))
        } finally { host.destroy() }
    }
    @Test fun sixNeutralSlotsShareOneLineAndDropdownHasTwoAlignedRowsBelowTheSameAnchor() {
        val host=Robolectric.buildActivity(Activity::class.java).setup().visible()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            DashboardStatusConfig.write(prefs,full)
            for(flat in listOf(false,true)) for(width in listOf(320,360,480)) {
                themes.setFlatDesign(flat)
                val content=DashboardStatusSettingsView(a,themes,prefs,changed={})
                a.setContentView(content);content.findViewWithTag<View>("dashboard_status_expand").performClick();layout(content,width,500)
                val cards=(0..5).map { content.findViewWithTag<BicycleSegmentButton>("dashboard_status_slot_$it") }
                val positions=cards.map { card -> IntArray(2).also(card::getLocationOnScreen) }
                assertEquals(1,positions.map { it[1] }.distinct().size)
                assertTrue(cards.all { it.width>0 && !it.showSelectionHighlight })
                assertTrue(positions.zipWithNext().all { it.second[0]>it.first[0] })
                assertTrue(positions.last()[0]+cards.last().width<=width)
                val dot=content.findViewWithTag<View>("dashboard_status_separator")
                val dotPosition=IntArray(2).also(dot::getLocationOnScreen)
                assertEquals(width/2f,dotPosition[0]+dot.width/2f,1f)
                assertTrue(cards.first().isChecked) // Styling must not erase assignment or native state.
                for(index in listOf(0,3,5)) {
                    val card=cards[index]
                    assertTrue(card.textSize>0);assertTrue(card.layout.width<=card.width)
                    assertTrue(card.layout.getLineWidth(0)>0)
                }
                val sample=Bitmap.createBitmap(cards.first().width,cards.first().height,Bitmap.Config.ARGB_8888)
                cards.first().draw(Canvas(sample));assertEquals(themes.active.tokens.secondary,sample.getPixel(sample.width/2,8));sample.recycle()
                val body=content.findViewWithTag<android.view.ViewGroup>("dashboard_status_details")
                assertFalse((0 until body.childCount).map(body::getChildAt).filterIsInstance<android.widget.TextView>().isNotEmpty())
                val phone=content.findViewWithTag<View>("dashboard_status_phone_preview")
                val phonePosition=IntArray(2).also(phone::getLocationOnScreen)
                assertTrue(phonePosition[1]>=positions[0][1]+cards.first().height)
                cards.first().performClick()
                val popup=content.popup!!;assertTrue(popup.isShowing)
                val grid=popup.contentView as android.view.ViewGroup
                layout(grid,popup.width,180)
                assertEquals(2,grid.childCount)
                for(rowIndex in 0..1) {
                    val row=grid.getChildAt(rowIndex) as android.view.ViewGroup
                    assertEquals(3,row.childCount)
                    assertEquals(1,(0..2).map { row.getChildAt(it).top }.distinct().size)
                    assertTrue((0..2).all { !(row.getChildAt(it) as BicycleSegmentButton).showSelectionHighlight })
                    assertTrue((0..2).map { row.getChildAt(it).height }.distinct().size==1)
                }
                assertEquals(grid.getChildAt(0).bottom+BicycleDesign.dp(a,BicycleDesign.controlGap),grid.getChildAt(1).top)
                if(!flat && width==360) {
                    val image=Bitmap.createBitmap(grid.width,grid.height,Bitmap.Config.ARGB_8888)
                    grid.draw(Canvas(image))
                    java.io.File("build/reports/ui/dashboard-status-options.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
                }
                val old=DashboardStatusConfig.read(prefs);popup.dismiss();assertEquals(old,DashboardStatusConfig.read(prefs))
            }
        } finally { host.pause().stop().destroy() }
    }
    @Test fun batteryGlyphIncludesMeasuredPercentAndNeverInventsZeroWhenUnknown() {
        val drawing=DashboardStatusDrawing(c);drawing.paint.textSize=12f
        val bitmap=Bitmap.createBitmap(180,40,Bitmap.Config.ARGB_8888)
        val texts=mutableListOf<String>()
        val canvas=object:Canvas(bitmap) { override fun drawText(text:String,x:Float,y:Float,paint:android.graphics.Paint) { texts+=text;super.drawText(text,x,y,paint) } }
        for(level in listOf(0,72,100,null)) {
            texts.clear();val state=DashboardStatusSnapshot(battery=level)
            drawing.draw(canvas,DashboardStatusItem.BATTERY,state,0f,20f,Color.WHITE)
            assertEquals(listOf(level?.let { c.getString(R.string.dashboard_status_percent,it) } ?: c.getString(R.string.hrvt_unavailable)),texts)
            assertEquals(25f+drawing.paint.measureText(texts.single()),drawing.width(DashboardStatusItem.BATTERY,state),.01f)
        }
        bitmap.recycle()
    }
    @Test fun slotsAreEmptyInitiallyUniqueMovableAndStrictlyValidatedInBackup() {
        val prefs=c.getSharedPreferences("bike",0);val config=DashboardStatusConfig.read(prefs)
        assertFalse(config.populated)
        val moved=full.assign(5,DashboardStatusItem.TIME)
        assertEquals(DashboardStatusItem.EMPTY,moved.slots[0]);assertEquals(DashboardStatusItem.TIME,moved.slots[5])
        assertEquals(listOf(DashboardStatusItem.NOTIFICATIONS,DashboardStatusItem.CELLULAR,DashboardStatusItem.TIME),full.move(0,2).slots.take(3))
        assertThrows(IllegalArgumentException::class.java) { full.move(0,5) }
        assertThrows(IllegalArgumentException::class.java) { DashboardStatusConfig.parse("TIME,TIME,EMPTY,EMPTY,EMPTY,EMPTY") }
        assertThrows(IllegalArgumentException::class.java) { DashboardStatusConfig.parse("TIME") }
        DashboardStatusConfig.write(prefs,full)
        val backup=SettingsBackup(c,ThemeManager(prefs));val sections=setOf(BackupSection.DESIGN)
        val bytes=backup.export(sections);prefs.edit().remove(DashboardStatusConfig.key).commit()
        backup.apply(backup.plan(backup.decode(bytes),sections));assertEquals(full,DashboardStatusConfig.read(prefs))
        assertTrue(BikeUiConfigurationScope.designKey(DashboardStatusConfig.key))
        val store=BikeUiConfigurationStore(prefs);store.ensure(listOf(0,1));store.save(0)
        DashboardStatusConfig.write(prefs,DashboardStatusConfig());store.save(1);store.apply(0)
        assertEquals(full,DashboardStatusConfig.read(prefs));store.apply(1);assertFalse(DashboardStatusConfig.read(prefs).populated)
    }
    @Test fun activeNotificationKeysCountOnceRespectSelectionAndRemovalAndRememberFirstChosenApp() {
        MessageStatusNotifications.clear()
        val settings=MessageNotificationSettings(c)
        settings.enabled=true;settings.packages=linkedSetOf("org.thoughtcrime.securesms","com.whatsapp")
        assertEquals("org.thoughtcrime.securesms",settings.orderedPackages.first())
        settings.packages=settings.packages+"org.telegram.messenger"
        assertEquals("org.thoughtcrime.securesms",settings.orderedPackages.first())
        val originalOrder=settings.orderedPackages
        val backup=SettingsBackup(c,ThemeManager(c.getSharedPreferences("bike",0)));val sections=setOf(BackupSection.DESIGN)
        val bytes=backup.export(sections);c.getSharedPreferences("message-notifications",0).edit().clear().commit()
        backup.apply(backup.plan(backup.decode(bytes),sections));assertEquals(originalOrder,settings.orderedPackages)
        shadowOf(c.getSystemService(NotificationManager::class.java)).setNotificationListenerAccessGranted(ComponentName(c,MessageNotificationService::class.java),true)
        fun notification()=Notification.Builder(c,"test").setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle("Sender").setContentText("Text").build()
        MessageStatusNotifications.posted(c,"one","com.whatsapp",notification())
        MessageStatusNotifications.posted(c,"one","com.whatsapp",notification())
        MessageStatusNotifications.posted(c,"two","unknown.app",notification())
        assertEquals(1,MessageStatusNotifications.count(c))
        MessageStatusNotifications.posted(c,"three","org.telegram.messenger",notification());assertEquals(2,MessageStatusNotifications.count(c))
        MessageStatusNotifications.removed("one");assertEquals(1,MessageStatusNotifications.count(c))
        settings.packages=setOf("com.whatsapp");assertEquals(0,MessageStatusNotifications.count(c));assertEquals(listOf("com.whatsapp"),settings.orderedPackages)
        settings.enabled=false;assertEquals(0,MessageStatusNotifications.count(c));MessageStatusNotifications.clear()
    }
    @Test fun vpnUsesAndroidTransportAndLateCallbacksAfterStopDoNotRestoreIt() {
        val snapshots=mutableListOf<DashboardStatusSnapshot>()
        val monitor=DashboardStatusMonitor(c) { snapshots+=it }
        monitor.configure(DashboardStatusConfig().assign(3,DashboardStatusItem.VPN))
        val field=DashboardStatusMonitor::class.java.getDeclaredField("networks").apply { isAccessible=true }
        val callback=field.get(monitor) as ConnectivityManager.NetworkCallback
        val network=org.robolectric.shadows.ShadowNetwork.newInstance(42)
        callback.onAvailable(network);assertTrue(snapshots.last().vpn)
        callback.onLost(network);assertFalse(snapshots.last().vpn)
        val caps=NetworkCapabilities()
        org.robolectric.util.ReflectionHelpers.callInstanceMethod<NetworkCapabilities>(caps,"addTransportType",org.robolectric.util.ReflectionHelpers.ClassParameter.from(Int::class.javaPrimitiveType,NetworkCapabilities.TRANSPORT_VPN))
        callback.onCapabilitiesChanged(network,caps);assertTrue(snapshots.last().vpn)
        monitor.stop();val count=snapshots.size
        callback.onAvailable(network);assertEquals(count,snapshots.size);assertNull(field.get(monitor))
        monitor.configure(DashboardStatusConfig().assign(0,DashboardStatusItem.TIME))
        assertFalse(snapshots.last().vpn);assertTrue(snapshots.last().time.isNotBlank());assertNull(field.get(monitor));monitor.stop()
    }
    @Test fun cellularUsesPlatformSignalLevelAndAirplaneModeDoesNotRetainOldBars() {
        val snapshots=mutableListOf<DashboardStatusSnapshot>()
        shadowOf(c.packageManager).setSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY_RADIO_ACCESS,true)
        val manager=c.getSystemService(android.telephony.TelephonyManager::class.java)
        val strength=org.robolectric.util.ReflectionHelpers.callConstructor(android.telephony.SignalStrength::class.java)
        shadowOf(manager).setSignalStrength(strength)
        val monitor=DashboardStatusMonitor(c) { snapshots+=it }
        try {
            val config=DashboardStatusConfig().assign(2,DashboardStatusItem.CELLULAR)
            monitor.configure(config)
            val callback=shadowOf(manager).lastTelephonyCallback as android.telephony.TelephonyCallback.SignalStrengthsListener
            callback.onSignalStrengthsChanged(strength);assertEquals(strength.level,snapshots.last().cellular)
            android.provider.Settings.Global.putInt(c.contentResolver,android.provider.Settings.Global.AIRPLANE_MODE_ON,1)
            c.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED))
            shadowOf(android.os.Looper.getMainLooper()).idle();assertNull(snapshots.last().cellular)
            callback.onSignalStrengthsChanged(strength);assertNull(snapshots.last().cellular)
        } finally { monitor.stop();android.provider.Settings.Global.putInt(c.contentResolver,android.provider.Settings.Global.AIRPLANE_MODE_ON,0) }
    }
    @Test fun safeRegionsAvoidCentralNotchesAndOffCentreHolesAndRenderBothDesigns() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val themes=ThemeManager(a.getSharedPreferences("status-render",0))
            val strip=DashboardStatusStrip(a,themes);a.setContentView(strip);layout(strip,360,32)
            strip.config=full;strip.show(DashboardStatusSnapshot("10:28",true,72,3,3))
            strip.insets(0,0,emptyList())
            assertEquals(8f,strip.regions()[0].first,0f);assertEquals(335f,strip.regions()[1].second,0f)
            val indicator=Rect(333,0,360,24)
            strip.insets(0,0,emptyList(),indicator)
            assertEquals(331f,strip.regions()[1].second,0f)
            indicator.setEmpty() // Insets own their rectangle: later caller changes must not shift the strip.
            assertEquals(331f,strip.regions()[1].second,0f)
            strip.insets(0,0,emptyList(),Rect(333,40,360,60))
            assertEquals(335f,strip.regions()[1].second,0f)
            strip.insets(0,0,emptyList(),Rect())
            assertEquals(335f,strip.regions()[1].second,0f)
            strip.insets(0,0,listOf(Rect(145,0,215,28)))
            assertTrue(strip.regions()[0].second<=137f);assertTrue(strip.regions()[1].first>=223f)
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                val image=Bitmap.createBitmap(360,32,Bitmap.Config.ARGB_8888);val canvas=Canvas(image);canvas.drawColor(android.graphics.Color.BLACK);strip.draw(canvas)
                for(x in 145..214) for(y in 0..27) assertEquals(android.graphics.Color.BLACK,image.getPixel(x,y))
                java.io.File("build/reports/ui/dashboard-status-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle()
            }
            strip.insets(0,0,listOf(Rect(8,0,34,24)))
            assertTrue(strip.regions()[0].first>=42f)
            strip.config=DashboardStatusConfig();strip.show(DashboardStatusSnapshot());assertEquals("",strip.contentDescription)
        } finally { host.pause().stop().destroy() }
    }
    @Test fun privacyReportsOnlyAddFourDpOnRightAndNeverShiftTheLeftGroup() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val strip=DashboardStatusStrip(a,ThemeManager(a.getSharedPreferences("status",0)))
            a.setContentView(strip);layout(strip,360,32)
            val right=Rect(333,0,360,24)
            strip.insets(0,0,emptyList(),right)
            assertEquals(8f,strip.regions()[0].first,0f);assertEquals(331f,strip.regions()[1].second,0f)
            assertTrue(strip.geometryReport().contains("privacyIndicatorBoundsPx=Rect(333, 0 - 360, 24)"))
            right.setEmpty();assertTrue(strip.geometryReport().contains("Rect(333, 0 - 360, 24)"))
            // Even a left-side OEM report is only a presence signal for the fixed right-side increment.
            strip.insets(0,0,emptyList(),Rect(0,0,32,24))
            assertEquals(8f,strip.regions()[0].first,0f);assertEquals(331f,strip.regions()[1].second,0f)
            assertTrue(strip.geometryReport().contains("privacyExtraRightDp=4"))
            assertTrue(strip.geometryReport().contains("cameraBoundsPx=[] privacyIndicatorBoundsPx=Rect(0, 0 - 32, 24)"))
            strip.bikeLightOn=true
            val light=strip.lightBounds()
            strip.insets(0,0,emptyList(),Rect(0,0,180,32))
            assertEquals(light,strip.lightBounds()) // A falsely placed privacy rectangle cannot suppress the light either.
            strip.insets(0,0,listOf(Rect(0,0,32,24)))
            assertEquals(40f,strip.regions()[0].first,0f) // Real camera cutouts still reserve space.
            strip.insets(12,0,emptyList())
            assertEquals(20f,strip.regions()[0].first,0f)
            val report=strip.geometryReport()
            assertTrue(report.contains("safeInsetsPx left=12 right=0"));assertTrue(report.contains("configuredDp left=8 right=25"))
            assertTrue(report.contains("privacyIndicatorBoundsPx=null"));assertFalse(report.contains("10:28"))
        } finally { host.pause().stop().destroy() }
    }
    @Test fun accordionSlotsUseNamePartCardsAndNativeReorderAccessibilityPersistsTheNewOrder() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            DashboardStatusConfig.write(prefs,full)
            val view=DashboardStatusSettingsView(a,themes,prefs,changed={})
            a.setContentView(view);layout(view,360,620)
            view.findViewWithTag<View>("dashboard_status_expand").performClick();layout(view,360,620)
            assertEquals(View.VISIBLE,view.findViewWithTag<View>("dashboard_status_details").visibility)
            val first=view.findViewWithTag<BicycleSegmentButton>("dashboard_status_slot_0")
            assertEquals(BicycleDesign.dp(a,BicycleDesign.rideNamePartHeight),first.height)
            assertEquals("10:28",first.text.toString());assertTrue(first.isChecked)
            assertTrue(first.performAccessibilityAction(R.id.bike_move_right,null))
            assertEquals(DashboardStatusItem.NOTIFICATIONS,DashboardStatusConfig.read(prefs).slots[0])
            view.findViewWithTag<View>("dashboard_status_slot_0").performClick()
            view.popup!!.contentView.findViewWithTag<View>("dashboard_status_choice_BATTERY").performClick()
            assertEquals(DashboardStatusItem.BATTERY,DashboardStatusConfig.read(prefs).slots[0])
            assertEquals(DashboardStatusItem.EMPTY,DashboardStatusConfig.read(prefs).slots[4])
            DashboardStatusConfig.write(prefs,full)
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                val preview=DashboardStatusSettingsView(a,themes,prefs,changed={})
                a.setContentView(preview);preview.findViewWithTag<View>("dashboard_status_expand").performClick();layout(preview,360,620)
                val row=preview.findViewWithTag<android.view.ViewGroup>("dashboard_status_side_0")
                assertEquals((0 until row.childCount).map { row.getChildAt(it).top }.distinct().size,1)
                val bitmap=Bitmap.createBitmap(360,620,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);canvas.drawColor(themes.active.tokens.secondary);preview.draw(canvas)
                java.io.File("build/reports/ui/dashboard-status-settings-${if(flat) "flat" else "glow"}.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            }
            view.findViewWithTag<Switch>("dashboard_fullscreen").isChecked=false;assertFalse(DashboardFullscreen.enabled(prefs))
        } finally { host.pause().stop().destroy() }
    }
}
