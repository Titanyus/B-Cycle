package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal enum class BikeType(val label: Int, val available: Boolean) {
    BIKE(R.string.bikes_type_bike, false), EBIKE(R.string.bikes_type_ebike, true), INDOOR(R.string.bikes_type_indoor, true)
}
internal enum class BikeCategory(val label: Int) {
    MOUNTAIN(R.string.bikes_category_mountain), ROAD(R.string.bikes_category_road),
    TREKKING(R.string.bikes_category_trekking), GRAVEL(R.string.bikes_category_gravel), CITY(R.string.bikes_category_city)
}
internal enum class BikeSensor(val label: Int, val supported: Boolean = true) {
    GPS(R.string.ui_source_gps), EBIKE(R.string.ui_source_bike), POWER(R.string.bikes_power_meter, false), TRAINER(R.string.bikes_trainer), HEART(R.string.ui_source_heart)
}
internal val BikeType.sensors get() = when (this) {
    BikeType.EBIKE -> listOf(BikeSensor.GPS, BikeSensor.EBIKE, BikeSensor.POWER, BikeSensor.HEART)
    BikeType.BIKE -> listOf(BikeSensor.GPS, BikeSensor.POWER, BikeSensor.HEART)
    BikeType.INDOOR -> listOf(BikeSensor.TRAINER, BikeSensor.HEART)
}
internal data class BikeProfile(val slot: Int, val name: String, val type: BikeType = BikeType.EBIKE,
    val category: BikeCategory = BikeCategory.MOUNTAIN, val wheelMm: Int = 2200, val autoConnect: Boolean = true,
    val gpsEnabled: Boolean = true, val bikeEnabled: Boolean = true, val heartEnabled: Boolean = true,
    val bikeAddress: String? = null, val bikeName: String? = null,
    val heartAddress: String? = null, val heartName: String? = null,
    val trainerAddress:String?=null,val trainerName:String?=null,val trainerEnabled:Boolean=true, val recording: RecordingConfig = RecordingConfig())

/** Up to six profiles with stable identities; array order is presentation only. */
internal class BikeProfileStore(private val context: Context, private val prefs: SharedPreferences) {
    companion object { const val MAX_COUNT = 6; private const val NEXT = "nextBikeSlot"; const val KEY = "bikeProfilesV1"; const val ACTIVE = "activeBikeSlot" }
    val activeSlot get() = parseStored().let { rows -> prefs.getInt(ACTIVE, 0).takeIf { id -> rows.any { it.slot == id } } ?: rows.first().slot }
    private fun default(slot: Int) = BikeProfile(slot, context.getString(R.string.bikes_default_name, slot + 1))
    init {
        if (!prefs.contains(KEY)) {
            val rows = mutableListOf(default(0))
            rows[0] = legacy(rows[0])
            writeLegacy(prefs.edit().putString(KEY, encode(rows)).putInt(ACTIVE, 0), rows[0]).apply()
        }
    }
    private fun legacy(p: BikeProfile) = p.copy(gpsEnabled = prefs.getBoolean("gpsEnabled", p.gpsEnabled),
        bikeEnabled = prefs.getBoolean("bikeEnabled", true), heartEnabled = prefs.getBoolean("heartEnabled", true),
        bikeAddress = prefs.getString("address", null), bikeName = prefs.getString("name", null),
        heartAddress = prefs.getString("heartAddress", null), heartName = prefs.getString("heartName", null),
        recording = RecordingConfig.decode(prefs.getStringSet("recordFields", null), prefs.getString("recordSpeed", null), prefs.getString("recordDistance", null)))
    private var cachedJson: String? = null
    private var cachedProfiles: List<BikeProfile>? = null
    private fun parseStored(): List<BikeProfile> {
        val json = prefs.getString(KEY, "[]") ?: "[]"
        val rows = if (json == cachedJson) cachedProfiles else null
        val profiles = rows ?: parse(json).also { cachedJson = json; cachedProfiles = it }
        return profiles
    }
    fun all(): List<BikeProfile> {
        val rows = parseStored(); val active = activeSlot
        return rows.map { if (it.slot == active) legacy(it) else it }
    }
    private fun parse(json: String): List<BikeProfile> {
        val array = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        val ids = mutableSetOf<Int>()
        return (0 until minOf(array.length(), MAX_COUNT)).mapNotNull { index ->
            val id = runCatching { array.getJSONObject(index).optInt("slot", index) }.getOrDefault(index)
            if (id < 0 || id == Int.MAX_VALUE || !ids.add(id)) return@mapNotNull null
            val fallback = default(id)
            val stored = runCatching {
                val o = array.getJSONObject(index)
                fallback.copy(name = o.getString("name").takeIf { it.isNotBlank() && it.length <= 60 && it.none(Char::isISOControl) } ?: fallback.name,
                    type = BikeType.valueOf(o.getString("type")), category = BikeCategory.valueOf(o.getString("category")),
                    wheelMm = o.getInt("wheelMm").takeIf { it in 500..4000 } ?: fallback.wheelMm,
                    autoConnect = o.getBoolean("autoConnect"), gpsEnabled = o.getBoolean("gpsEnabled"),
                    bikeEnabled = o.getBoolean("bikeEnabled"), heartEnabled = o.getBoolean("heartEnabled"),
                    bikeAddress = o.optString("bikeAddress").takeIf(String::isNotEmpty), bikeName = o.optString("bikeName").takeIf(String::isNotEmpty),
                    trainerAddress=o.optString("trainerAddress").takeIf(String::isNotEmpty), trainerName=o.optString("trainerName").takeIf(String::isNotEmpty),trainerEnabled=o.optBoolean("trainerEnabled",true),
                    heartAddress = o.optString("heartAddress").takeIf(String::isNotEmpty), heartName = o.optString("heartName").takeIf(String::isNotEmpty),
                    recording = RecordingConfig.decode(o.getJSONArray("fields").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }, o.getString("speed"), o.getString("distance")))
            }.getOrDefault(fallback)
            stored
        }.ifEmpty { listOf(default(0)) }
    }
    fun current() = all().first { it.slot == activeSlot }
    fun persistActive() = save(current())
    fun save(profile: BikeProfile) {
        require(profile.slot == activeSlot && profile.type.available && profile.name.isNotBlank() && profile.name.length <= 60 && profile.name.none(Char::isISOControl) && profile.wheelMm in 500..4000)
        val rows = all().toMutableList(); rows[rows.indexOfFirst { it.slot == profile.slot }] = profile
        writeLegacy(prefs.edit().putString(KEY, encode(rows)), profile).apply()
    }
    fun select(slot: Int) {
        val rows = all()
        val selected = rows.first { it.slot == slot }; require(selected.type.available)
        writeLegacy(prefs.edit().putString(KEY, encode(rows)).putInt(ACTIVE, slot), selected).apply()
    }
    fun add(): BikeProfile? {
        val rows = all(); if (rows.size >= MAX_COUNT) return null
        val id = maxOf(prefs.getInt(NEXT, 0), rows.maxOf { it.slot } + 1)
        if (id == Int.MAX_VALUE) return null
        val profile = default(id)
        prefs.edit().putString(KEY, encode(rows + profile)).putInt(NEXT, id + 1).apply()
        return profile
    }
    /** Switch runtime to a surviving profile before removing the previous active one. */
    fun remove(slot: Int): Boolean {
        val rows = all()
        if (rows.size <= 1 || slot == activeSlot || rows.none { it.slot == slot }) return false
        prefs.edit().putString(KEY, encode(rows.filterNot { it.slot == slot }))
            .putInt(NEXT, maxOf(prefs.getInt(NEXT, 0), rows.maxOf { it.slot } + 1)).apply()
        return true
    }
    fun move(slot: Int, position: Int): Boolean {
        val rows = all().toMutableList(); val from = rows.indexOfFirst { it.slot == slot }
        if (from < 0 || position !in rows.indices) return false
        rows.add(position, rows.removeAt(from))
        prefs.edit().putString(KEY, encode(rows)).apply()
        return true
    }
    private fun writeLegacy(edit: SharedPreferences.Editor, p: BikeProfile): SharedPreferences.Editor = edit
        .putString("address", p.bikeAddress).putString("name", p.bikeName)
        .putString("heartAddress", p.heartAddress).putString("heartName", p.heartName)
        .putBoolean("gpsEnabled", p.gpsEnabled).putBoolean("bikeEnabled", p.bikeEnabled).putBoolean("heartEnabled", p.heartEnabled)
        .putStringSet("recordFields", p.recording.fields).putString("recordSpeed", p.recording.speedSource).putString("recordDistance", p.recording.distanceSource)
    private fun encode(rows: List<BikeProfile>) = JSONArray().apply { rows.forEach { p -> put(JSONObject()
        .put("slot", p.slot).put("name", p.name).put("type", p.type.name).put("category", p.category.name).put("wheelMm", p.wheelMm).put("autoConnect", p.autoConnect)
        .put("gpsEnabled", p.gpsEnabled).put("bikeEnabled", p.bikeEnabled).put("heartEnabled", p.heartEnabled)
        .put("bikeAddress", p.bikeAddress.orEmpty()).put("bikeName", p.bikeName.orEmpty()).put("heartAddress", p.heartAddress.orEmpty()).put("heartName", p.heartName.orEmpty())
        .put("trainerAddress",p.trainerAddress.orEmpty()).put("trainerName",p.trainerName.orEmpty()).put("trainerEnabled",p.trainerEnabled)
        .put("fields", JSONArray(p.recording.fields.sorted())).put("speed", p.recording.speedSource).put("distance", p.recording.distanceSource)) } }.toString()
}
