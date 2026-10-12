package de.bcycle.app

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

fun interface ThemeSubscription : AutoCloseable { override fun close() }

/** Application-owned, page-independent palette store. No Activity or View is retained here. */
class ThemeManager(private val preferences: SharedPreferences) {
    companion object {
        const val activeKey = "uiTheme.activeId"
        const val customsKey = "uiTheme.customThemes"
        const val maximumCustomThemes = 32
    }
    private val lock = Any()
    private val main = Handler(Looper.getMainLooper())
    private var customs = loadCustoms()
    private var current: BicycleTheme = find(savedActiveId()) ?: BicycleThemes.red
    private val observers = linkedSetOf<Subscription>()
    val presets get() = BicycleThemes.presets
    val customThemes: List<CustomTheme> get() = synchronized(lock) { customs.toList() }
    val active: BicycleTheme get() = synchronized(lock) { current }
    init {
        check(presets.all { ThemeValidator.validate(it).valid } && presets.map { it.id }.distinct().size == presets.size)
        if (savedActiveId() != current.id) preferences.edit().putString(activeKey, current.id).apply()
    }
    /** Apply externally restored validated configuration without retaining stale theme objects. */
    fun reloadFromPreferences() {
        synchronized(lock) { customs = loadCustoms(); current = find(savedActiveId()) ?: BicycleThemes.red }
        publish()
    }
    private fun savedActiveId(): String? = runCatching { preferences.getString(activeKey, null) }.getOrNull()
    private fun find(id: String?): BicycleTheme? = presets.find { it.id == id } ?: customs.find { it.id == id }
    /** Invalid or removed IDs restore the standard palette and persist that fallback. */
    fun activate(id: String): Boolean {
        val valid: Boolean
        val changed: Boolean
        synchronized(lock) {
            val candidate = find(id)?.takeIf { ThemeValidator.validate(it).valid }
            valid = candidate != null
            val next = candidate ?: BicycleThemes.red
            changed = current != next; current = next
            preferences.edit().putString(activeKey, next.id).apply()
        }
        if (changed) publish()
        return valid
    }
    /** Technical create/edit API only; invalid drafts never enter storage or the active UI. */
    fun saveCustom(theme: CustomTheme): ThemeValidation {
        val validation = ThemeValidator.validate(theme)
        if (!validation.valid) return validation
        var changed = false
        synchronized(lock) {
            val existing = customs.indexOfFirst { it.id == theme.id }
            if (existing < 0 && customs.size >= maximumCustomThemes)
                return ThemeValidation(listOf("Maximal $maximumCustomThemes eigene Themes möglich"))
            customs = if (existing < 0) customs + theme else customs.map { if (it.id == theme.id) theme else it }
            if (current.id == theme.id) { changed = current != theme; current = theme }
            storeCustoms()
        }
        if (changed) publish()
        return validation
    }
    fun copyTheme(sourceId: String, name: String): CustomTheme? {
        val source = synchronized(lock) { find(sourceId) } ?: return null
        val copy = CustomTheme("custom:${UUID.randomUUID()}", name, source.tokens)
        return copy.takeIf { saveCustom(it).valid }
    }
    /** Presets cannot be deleted; deleting the active custom palette restores BICYCLE Red. */
    fun deleteCustom(id: String): Boolean {
        var changed = false
        synchronized(lock) {
            if (customs.none { it.id == id }) return false
            customs = customs.filterNot { it.id == id }
            if (current.id == id) { current = BicycleThemes.red; changed = true }
            storeCustoms()
        }
        if (changed) publish()
        return true
    }
    /** Initial delivery and all subsequent notifications run on Android's main thread. */
    fun observe(observer: (BicycleTheme) -> Unit): ThemeSubscription {
        val subscription = Subscription(observer)
        synchronized(lock) { observers += subscription }
        subscription.deliver()
        return subscription
    }
    private inner class Subscription(private val callback: (BicycleTheme) -> Unit) : ThemeSubscription {
        @Volatile private var closed = false
        fun deliver() {
            val delivery = Runnable { if (!closed) callback(active) }
            if (Looper.myLooper() == Looper.getMainLooper()) delivery.run() else main.post(delivery)
        }
        override fun close() { closed = true; synchronized(lock) { observers -= this } }
    }
    private fun publish() = synchronized(lock) { observers.toList() }.forEach { it.deliver() }
    private fun storeCustoms() {
        val rows = JSONArray()
        fun hex(color: Int) = "#%06X".format(java.util.Locale.ROOT, color and 0xffffff)
        for (theme in customs) rows.put(JSONObject().put("id", theme.id).put("name", theme.name).put("kind", "custom")
            .put("primary", hex(theme.tokens.primary)).put("secondary", hex(theme.tokens.secondary))
            .put("tertiary", hex(theme.tokens.tertiary)).put("foreground", hex(theme.tokens.foreground))
            .put("accent", hex(theme.tokens.accent)))
        preferences.edit().putString(customsKey, JSONObject().put("version", 2).put("themes", rows).toString())
            .putString(activeKey, current.id).apply()
    }
    private fun loadCustoms(): List<CustomTheme> = runCatching {
        val raw = preferences.getString(customsKey, null) ?: return emptyList()
        require(raw.length <= 32_768)
        val root = JSONObject(raw); require(root.opt("version") is Int && root.getInt("version") in 1..2)
        val rows = root.getJSONArray("themes"); require(rows.length() <= maximumCustomThemes)
        fun color(row: JSONObject, key: String): Int {
            require(row.opt(key) is String)
            val value = row.getString(key); require(value.matches(Regex("#[0-9a-fA-F]{6}")))
            return (0xff000000L or value.substring(1).toLong(16)).toInt()
        }
        (0 until rows.length()).mapNotNull { index -> runCatching {
            val row = rows.getJSONObject(index)
            require(row.opt("kind") == "custom" && row.opt("id") is String && row.opt("name") is String)
            CustomTheme(row.getString("id"), row.getString("name"),
                ThemeTokens(color(row, "primary"), color(row, "secondary"), color(row, "tertiary"), color(row, "foreground"),
                    if (row.has("accent")) color(row, "accent") else color(row, "foreground")))
                .takeIf { ThemeValidator.validate(it).valid }
        }.getOrNull() }.groupBy { it.id }.values.filter { it.size == 1 }.map { it.single() }
    }.getOrDefault(emptyList())
}
