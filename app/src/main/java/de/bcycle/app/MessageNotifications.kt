package de.bcycle.app

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.security.MessageDigest

internal enum class MessagePosition(val label: Int) {
    BOTTOM(R.string.notifications_position_bottom), CENTER(R.string.notifications_position_center), TOP(R.string.notifications_position_top)
}

internal data class IncomingMessage(val packageName: String, val sender: String, val text: String, val timestamp: Long = 0)

internal class MessageNotificationSettings(private val context: Context) {
    private val prefs=context.getSharedPreferences("message-notifications",Context.MODE_PRIVATE)
    var enabled: Boolean
        get()=prefs.getBoolean("enabled",false)
        set(value) { prefs.edit().putBoolean("enabled",value).apply();if(!value) MessageNotifications.clear() }
    var messengerOnly: Boolean
        get()=prefs.getBoolean("messengerOnly",true)
        set(value) { prefs.edit().putBoolean("messengerOnly",value).apply() }
    var packages: Set<String>
        get()=prefs.getStringSet("packages",emptySet())!!.toSet()
        set(value) { prefs.edit().putStringSet("packages",value.toSet()).apply();MessageNotifications.clear() }
    var position: MessagePosition
        get()=runCatching { MessagePosition.valueOf(prefs.getString("position","BOTTOM")!!) }.getOrDefault(MessagePosition.BOTTOM)
        set(value) { prefs.edit().putString("position",value.name).apply() }
    var durationSeconds: Int
        get()=prefs.getInt("durationSeconds",60).coerceIn(5,180)
        set(value) { require(value in 5..180);prefs.edit().putInt("durationSeconds",value).apply() }
    val granted get()=context.getSystemService(NotificationManager::class.java)
        .isNotificationListenerAccessGranted(ComponentName(context,MessageNotificationService::class.java))
    fun requestAccess() {
        val component=ComponentName(context,MessageNotificationService::class.java).flattenToString()
        try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch(_:android.content.ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    companion object {
        // Package identities, never localized app names. Filtering does not alter selected packages.
        fun isMessenger(name: String)=name in setOf("com.whatsapp","com.whatsapp.w4b","org.thoughtcrime.securesms",
            "org.telegram.messenger","org.telegram.messenger.web","com.facebook.orca","com.viber.voip",
            "ch.threema.app","ch.threema.app.work","im.vector.app","im.vector.alpha","io.element.android.x",
            "im.fluffychat.Fluffychat","de.spiritcroc.riotx","com.google.android.apps.messaging",
            "com.samsung.android.messaging","com.discord","com.skype.raider","org.kontalk","eu.siacs.conversations",
            "com.slack","com.microsoft.teams","com.beeper.android")
    }
}

/** Process-memory-only relay. No notification content, IDs or fingerprints are written to disk/logs. */
internal object MessageNotifications {
    val observers=linkedSetOf<(IncomingMessage?)->Unit>()
    val observedPackages=linkedSetOf<String>()
    private val seen=linkedMapOf<String,String>()
    fun clear() { seen.clear();observers.toList().forEach { it(null) } }
    fun receive(context: Context, key: String, packageName: String, notification: Notification) {
        if(packageName==context.packageName) return
        if(observedPackages.size<128) observedPackages+=packageName
        val settings=MessageNotificationSettings(context)
        if(!settings.enabled || !settings.granted || packageName !in settings.packages || observers.isEmpty()) return
        val decoded=runCatching { decode(packageName,notification) }.getOrNull() ?: return
        val digest=MessageDigest.getInstance("SHA-256").digest((decoded.sender+"\u0000"+decoded.text+"\u0000"+decoded.timestamp).toByteArray()).joinToString("") { "%02x".format(it) }
        if(seen[key]==digest) return
        seen.remove(key);seen[key]=digest
        if(seen.size>256) seen.remove(seen.keys.first())
        observers.toList().forEach { it(decoded) }
    }
    internal fun decode(packageName: String, n: Notification): IncomingMessage? {
        if(n.flags and (Notification.FLAG_GROUP_SUMMARY or Notification.FLAG_ONGOING_EVENT)!=0) return null
        @Suppress("DEPRECATION")
        val last=n.extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.let {
            Notification.MessagingStyle.Message.getMessagesFromBundleArray(it).lastOrNull()
        }
        // Null sender in MessagingStyle denotes the user's own reply.
        if(last!=null && last.senderPerson==null) return null
        val title=(last?.senderPerson?.name ?: n.extras.getCharSequence(Notification.EXTRA_TITLE))?.toString()?.trim().orEmpty()
        val text=(last?.text ?: n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: n.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n")
            ?: n.extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()
        if(title.isBlank() || text.isBlank()) return null
        return IncomingMessage(packageName,title.take(512),text.take(32_768),last?.timestamp ?: n.`when`)
    }
}

class MessageNotificationService: NotificationListenerService() {
    private val main=Handler(Looper.getMainLooper())
    private var generation=0
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val session=generation
        main.post { if(session==generation) MessageNotifications.receive(applicationContext,sbn.key,sbn.packageName,sbn.notification) }
    }
    override fun onListenerDisconnected() { generation++;main.removeCallbacksAndMessages(null);MessageNotifications.clear() }
    override fun onDestroy() { generation++;main.removeCallbacksAndMessages(null);MessageNotifications.clear();super.onDestroy() }
}
