package de.bcycle.app

import android.app.Notification
import android.content.Context

/** Only active notification keys/packages in RAM. No message content or unread-message claims. */
internal object MessageStatusNotifications {
    private val active=linkedMapOf<String,String>()
    val observers=linkedSetOf<()->Unit>()
    fun count(context:Context):Int {
        val settings=MessageNotificationSettings(context)
        return if(!settings.enabled || !settings.granted) 0 else active.values.count { it in settings.packages }
    }
    fun posted(context:Context,key:String,packageName:String,notification:Notification) {
        val eligible=packageName!=context.packageName && runCatching { MessageNotifications.decode(packageName,notification)!=null }.getOrDefault(false)
        val old=active.remove(key)
        if(eligible) { active[key]=packageName;if(active.size>2048) active.remove(active.keys.first()) }
        if(old!=active[key]) changed()
    }
    fun removed(key:String) { if(active.remove(key)!=null) changed() }
    fun clear() { if(active.isNotEmpty()) { active.clear();changed() } }
    private fun changed() { observers.toList().forEach { it() } }
}
