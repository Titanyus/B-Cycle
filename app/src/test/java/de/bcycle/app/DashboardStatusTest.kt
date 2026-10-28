package de.bcycle.app

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
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
            val choice=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
            choice.window!!.decorView.findViewWithTag<View>("dashboard_status_choice_TIME").performClick()
            assertEquals(DashboardStatusItem.TIME,DashboardStatusConfig.read(prefs).slots[0])
            root.findViewWithTag<View>("dashboard_status_slot_3").performClick()
            org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().window!!.decorView.findViewWithTag<View>("dashboard_status_choice_VPN").performClick()
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
    @Test fun accordionSlotsUseNamePartCardsAndNativeReorderAccessibilityPersistsTheNewOrder() {
        val host=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val a=host.get();val prefs=a.getSharedPreferences("bike",0);val themes=ThemeManager(prefs)
            DashboardStatusConfig.write(prefs,full)
            var chosenIndex=-1
            val view=DashboardStatusSettingsView(a,themes,prefs,choose={ index,_,choose -> chosenIndex=index;choose(DashboardStatusItem.BATTERY) },changed={})
            a.setContentView(view);layout(view,360,620)
            view.findViewWithTag<View>("dashboard_status_expand").performClick();layout(view,360,620)
            assertEquals(View.VISIBLE,view.findViewWithTag<View>("dashboard_status_details").visibility)
            val first=view.findViewWithTag<BicycleSegmentButton>("dashboard_status_slot_0")
            assertEquals(BicycleDesign.dp(a,BicycleDesign.rideNamePartHeight),first.height)
            assertEquals("10:28",first.text.toString());assertTrue(first.isChecked)
            assertTrue(first.performAccessibilityAction(R.id.bike_move_right,null))
            assertEquals(DashboardStatusItem.NOTIFICATIONS,DashboardStatusConfig.read(prefs).slots[0])
            view.findViewWithTag<View>("dashboard_status_slot_0").performClick();assertEquals(0,chosenIndex)
            assertEquals(DashboardStatusItem.BATTERY,DashboardStatusConfig.read(prefs).slots[0])
            assertEquals(DashboardStatusItem.EMPTY,DashboardStatusConfig.read(prefs).slots[4])
            DashboardStatusConfig.write(prefs,full)
            for(flat in listOf(false,true)) {
                themes.setFlatDesign(flat)
                val preview=DashboardStatusSettingsView(a,themes,prefs,choose={_,_,_->},changed={})
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
