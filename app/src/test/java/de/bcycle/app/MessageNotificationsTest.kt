package de.bcycle.app

import android.app.Notification
import android.app.NotificationManager
import android.app.Person
import android.content.ComponentName
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class MessageNotificationsTest {
    private val app get()=RuntimeEnvironment.getApplication()
    private fun notification(text:String="Text",time:Long=1)=Notification.Builder(app,"chat")
        .setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle("Alice").setContentText(text).setWhen(time)
    private fun grant()=shadowOf(app.getSystemService(NotificationManager::class.java))
        .setNotificationListenerAccessGranted(ComponentName(app,MessageNotificationService::class.java),true)
    @Test fun fullTextMessagingSendersAndOwnRepliesAreDecodedWithoutGroupSummaries() {
        val long="Vollständiger Text ".repeat(100)
        val big=notification().setStyle(Notification.BigTextStyle().bigText(long)).build()
        // Model the full text supplied by an app; Notification.Builder may itself truncate input.
        big.extras.putCharSequence(Notification.EXTRA_BIG_TEXT,long)
        assertEquals(long.trim(),MessageNotifications.decode("com.whatsapp",big)!!.text)
        val me=Person.Builder().setName("Ich").build();val alice=Person.Builder().setName("Alice Sender").build()
        val chat=notification().setStyle(Notification.MessagingStyle(me).addMessage("alt",1,alice).addMessage("neu",2,alice)).build()
        assertEquals(IncomingMessage("com.whatsapp","Alice Sender","neu",2),MessageNotifications.decode("com.whatsapp",chat))
        val own=notification().setStyle(Notification.MessagingStyle(me).addMessage("Antwort",3,null as Person?)).build()
        assertNull(MessageNotifications.decode("com.whatsapp",own))
        val summary=notification().setGroup("group").setGroupSummary(true).build()
        assertNull(MessageNotifications.decode("com.whatsapp",summary))
        assertNull(MessageNotifications.decode("com.whatsapp",notification().setOngoing(true).build()))
        assertNull(MessageNotifications.decode("com.whatsapp",notification("").build()))
    }
    @Test fun accessEnableAndAppSelectionAreRequiredAndIdenticalUpdatesAreDeduplicated() {
        val settings=MessageNotificationSettings(app)
        val received=mutableListOf<IncomingMessage?>();val observer:(IncomingMessage?)->Unit={ received+=it }
        MessageNotifications.observers+=observer
        try {
            MessageNotifications.receive(app,"key","com.whatsapp",notification().build());assertTrue(received.isEmpty())
            settings.enabled=true;settings.packages=setOf("com.whatsapp");received.clear()
            MessageNotifications.receive(app,"key","com.whatsapp",notification().build());assertTrue(received.isEmpty())
            grant()
            MessageNotifications.receive(app,"key","com.whatsapp",notification().build())
            MessageNotifications.receive(app,"key","com.whatsapp",notification().build())
            assertEquals(1,received.size)
            MessageNotifications.receive(app,"key","com.whatsapp",notification(time=2).build()) // Same words, new incoming message.
            assertEquals(2,received.size)
            MessageNotifications.receive(app,"other","unselected.app",notification().build());assertEquals(2,received.size)
            settings.enabled=false;assertNull(received.last())
            MessageNotifications.receive(app,"key","com.whatsapp",notification("secret").build())
            assertFalse(received.filterNotNull().any { it.text=="secret" })
            assertEquals(setOf("enabled","packages","packageOrder"),app.getSharedPreferences("message-notifications",0).all.keys)
        } finally { MessageNotifications.observers-=observer;MessageNotifications.clear() }
    }
    @Test fun preferencesPersistAndFilteringDoesNotRemoveSelections() {
        val settings=MessageNotificationSettings(app)
        assertFalse(settings.enabled);assertEquals(60,settings.durationSeconds);assertEquals(MessagePosition.BOTTOM,settings.position)
        settings.packages=setOf("com.whatsapp","custom.app");settings.messengerOnly=true
        settings.position=MessagePosition.TOP;settings.durationSeconds=37
        val again=MessageNotificationSettings(app)
        assertEquals(37,again.durationSeconds);assertEquals(MessagePosition.TOP,again.position)
        assertEquals(setOf("com.whatsapp","custom.app"),again.packages)
        assertTrue(MessageNotificationSettings.isMessenger("im.vector.app"));assertFalse(MessageNotificationSettings.isMessenger("custom.app"))
        assertThrows(IllegalArgumentException::class.java) { settings.durationSeconds=0 }
    }
    @Test fun cardCapsHeightScrollsFullTextQueuesAndClearsAllReferencesOnDismissTimeoutAndBackground() {
        val context=android.view.ContextThemeWrapper(app,R.style.SettingsDialogTheme)
        val themes=ThemeManager(app.getSharedPreferences("bike",0))
        val host=FrameLayout(context)
        val panel=MessageNotificationPanel(context,themes) { 400 }
        host.addView(panel,FrameLayout.LayoutParams(-1,-2,android.view.Gravity.BOTTOM))
        fun layout() { host.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1600,View.MeasureSpec.EXACTLY));host.layout(0,0,900,1600) }
        MessageNotificationSettings(app).durationSeconds=5
        panel.receive(IncomingMessage(app.packageName,"Alice","kurz"));layout()
        assertTrue(panel.height in 1..399);assertEquals(1600,panel.bottom)
        val long="Lange Nachricht\n".repeat(100)
        panel.receive(IncomingMessage(app.packageName,"Bob",long))
        panel.findViewWithTag<View>("message_notification_dismiss").performClick();layout()
        assertEquals(400,panel.height);assertEquals("Bob",panel.current!!.sender)
        assertEquals(long,panel.findViewWithTag<TextView>("message_notification_text").text.toString())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        assertEquals(View.GONE,panel.visibility);assertNull(panel.current)
        assertEquals("",panel.findViewWithTag<TextView>("message_notification_text").text.toString())
        repeat(20) { panel.receive(IncomingMessage(app.packageName,"Sender $it","Text $it")) }
        panel.clear();assertNull(panel.current);assertEquals(View.GONE,panel.visibility)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(10));assertNull(panel.current)
    }
}
