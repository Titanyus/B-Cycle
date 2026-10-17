package de.bcycle.app

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.Switch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="xxhdpi")
class MessageNotificationUiTest {
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    private fun install(context:Context,name:String,label:String,appFlags:Int=0) {
        shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            packageName=name;applicationInfo=ApplicationInfo().apply { packageName=name;nonLocalizedLabel=label;flags=appFlags }
        })
    }
    @Test fun settingsPersistAppsDurationAndThreePositionsAndLaunchAndroidGrantScreen() {
        val app=RuntimeEnvironment.getApplication();install(app,"com.whatsapp","WhatsApp");install(app,"custom.app","Other")
        val view=MessageNotificationSettingsView(android.view.ContextThemeWrapper(app,R.style.SettingsDialogTheme),ThemeManager(app.getSharedPreferences("bike",0)))
        val settings=MessageNotificationSettings(app)
        assertNotNull(view.findViewWithTag<View>("notification_app_com.whatsapp"));assertNull(view.findViewWithTag<View>("notification_app_custom.app"))
        view.findViewWithTag<CheckBox>("notification_app_com.whatsapp").isChecked=true
        view.findViewWithTag<Switch>("notification_messenger_filter").isChecked=false
        view.findViewWithTag<CheckBox>("notification_app_custom.app").isChecked=true
        view.findViewWithTag<Switch>("notification_messenger_filter").isChecked=true
        assertEquals(setOf("com.whatsapp","custom.app"),settings.packages)
        val slider=view.findViewWithTag<SeekBar>("notification_duration")
        slider.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(61,settings.durationSeconds)
        for(position in MessagePosition.entries) {
            view.findViewWithTag<View>("notification_position_${position.name}").performClick()
            assertEquals(position,settings.position)
            assertEquals(1,MessagePosition.entries.count { view.findViewWithTag<View>("notification_position_${it.name}").isSelected })
        }
        view.findViewWithTag<Switch>("notifications_enabled").isChecked=true
        val intent=shadowOf(app).nextStartedActivity
        assertTrue(intent.action in setOf(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS,android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
    @Test fun fullAppListHidesSystemServicesButKeepsPreinstalledAppsAndMessengerSelections() {
        val app=RuntimeEnvironment.getApplication();val pm=app.packageManager
        for((name,label) in listOf("android.system.service" to "Systemdienst","system.updated.service" to "Aktualisierter Systemdienst","system.regular.app" to "Vorinstallierte App","com.google.android.apps.messaging" to "Messages","custom.app" to "Meine App")) {
            install(app,name,label,when(name) {
                "custom.app"->0
                "system.updated.service"->ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                else->ApplicationInfo.FLAG_SYSTEM
            })
        }
        val launcher=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage("system.regular.app")
        shadowOf(pm).addResolveInfoForIntent(launcher,ResolveInfo().apply {
            activityInfo=ActivityInfo().apply { packageName="system.regular.app";name="MainActivity";applicationInfo=pm.getApplicationInfo(packageName,0) }
        })
        MessageNotifications.observedPackages.add("android.system.service")
        val settings=MessageNotificationSettings(app).apply {
            messengerOnly=false;packages=setOf("android.system.service","custom.app")
        }
        val view=MessageNotificationSettingsView(android.view.ContextThemeWrapper(app,R.style.SettingsDialogTheme),ThemeManager(app.getSharedPreferences("bike",0)))
        assertNull(view.findViewWithTag<View>("notification_app_android.system.service"))
        assertNull(view.findViewWithTag<View>("notification_app_system.updated.service"))
        assertNotNull(view.findViewWithTag<View>("notification_app_system.regular.app"))
        assertNotNull(view.findViewWithTag<View>("notification_app_com.google.android.apps.messaging"))
        assertTrue(view.findViewWithTag<CheckBox>("notification_app_custom.app").isChecked)
        view.findViewWithTag<Switch>("notification_messenger_filter").isChecked=true
        assertNull(view.findViewWithTag<View>("notification_app_custom.app"))
        assertNotNull(view.findViewWithTag<View>("notification_app_com.google.android.apps.messaging"))
        view.findViewWithTag<Switch>("notification_messenger_filter").isChecked=false
        assertTrue(view.findViewWithTag<CheckBox>("notification_app_custom.app").isChecked)
        assertEquals(setOf("android.system.service","custom.app"),settings.packages)
        MessageNotifications.observedPackages.remove("android.system.service")
    }
    @Test fun notificationSectionsPinToViewportAndPushOffAtNextSection() {
        val app=RuntimeEnvironment.getApplication()
        val content=MessageNotificationSettingsView(android.view.ContextThemeWrapper(app,R.style.SettingsDialogTheme),ThemeManager(app.getSharedPreferences("bike",0)))
        val scroll=BicycleStickyScrollView(content.context).apply { addView(content) }
        scroll.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY))
        scroll.layout(0,0,1080,600)
        val headers=views(content).filterIsInstance<BicycleSectionHeader>()
        assertEquals(3,headers.size)
        scroll.scrollTo(0,headers.first().top+20)
        assertTrue(headers.first().translationY>0f)
        assertEquals(scroll.scrollY.toFloat(),headers.first().top+headers.first().translationY,1f)
        scroll.scrollTo(0,headers[1].top)
        assertEquals(0f,headers.first().translationY,0f)
    }
    @Test fun realDashboardCardHonorsAllThreeAnchorsAndForegroundAndDialogBoundaries() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();val settings=MessageNotificationSettings(a)
            settings.enabled=true;settings.packages=setOf("com.whatsapp")
            shadowOf(a.getSystemService(NotificationManager::class.java)).setNotificationListenerAccessGranted(ComponentName(a,MessageNotificationService::class.java),true)
            val root=a.window.decorView
            fun layout() { root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2240,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,2240) }
            layout()
            val panel=views(root).filterIsInstance<MessageNotificationPanel>().single()
            val map=views(root).filterIsInstance<MapPanel>().single()
            val parent=panel.parent as View
            val text="Vollständiger Nachrichtentext\n".repeat(120)
            for((i,position) in MessagePosition.entries.withIndex()) {
                panel.clear();settings.position=position
                val n=Notification.Builder(a,"chat").setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle("Alice").setContentText("short").setWhen(i.toLong()).build()
                n.extras.putCharSequence(Notification.EXTRA_BIG_TEXT,text)
                MessageNotifications.receive(a,"key-$i","com.whatsapp",n);layout();layout()
                assertEquals(View.VISIBLE,panel.visibility)
                when(position) {
                    MessagePosition.BOTTOM -> {
                        val point=IntArray(2);map.getLocationInWindow(point)
                        val panelPoint=IntArray(2);panel.getLocationInWindow(panelPoint)
                        assertTrue("Bottom message cannot cover dashboard values",panelPoint[1]>=point[1])
                    }
                    MessagePosition.CENTER -> assertEquals(parent.height/2.0,panel.top+panel.height/2.0,2.0)
                    MessagePosition.TOP -> assertEquals(0,panel.top)
                }
                if(position!=MessagePosition.BOTTOM) assertTrue(panel.height<=BicycleDesign.dp(a,320))
            }
            views(root).single { it.contentDescription==a.getString(R.string.ui_menu_open) }.performClick()
            assertNull(panel.current)
            val menu=ShadowAlertDialog.getLatestAlertDialog()
            views(menu.window!!.decorView).single { it.tag=="settings_NOTIFICATIONS" }.performClick()
            val settingsDialog=ShadowAlertDialog.getLatestAlertDialog()
            assertEquals(1,views(settingsDialog.window!!.decorView).filterIsInstance<BicycleStickyScrollView>().size)
            settings.durationSeconds=37
            controller.pause().stop().start().resume().visible()
            assertTrue(settingsDialog.isShowing)
            assertEquals(37,MessageNotificationSettings(a).durationSeconds)
            MessageNotifications.receive(a,"hidden","com.whatsapp",Notification.Builder(a,"chat").setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle("Bob").setContentText("private").build())
            assertNull(panel.current)
        } finally { controller.pause().stop().destroy() }
    }
}
