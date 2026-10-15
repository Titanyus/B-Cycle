package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal enum class BikeType(val label: Int, val available: Boolean) {
    BIKE(R.string.bikes_type_bike, false), EBIKE(R.string.bikes_type_ebike, true), INDOOR(R.string.bikes_type_indoor, false)
}
internal enum class BikeCategory(val label: Int) {
    MOUNTAIN(R.string.bikes_category_mountain), ROAD(R.string.bikes_category_road),
    TREKKING(R.string.bikes_category_trekking), GRAVEL(R.string.bikes_category_gravel), CITY(R.string.bikes_category_city)
}
internal enum class BikeSensor(val label: Int, val supported: Boolean = true) {
    GPS(R.string.ui_source_gps), EBIKE(R.string.ui_source_bike), POWER(R.string.bikes_power_meter, false), TRAINER(R.string.bikes_trainer, false), HEART(R.string.ui_source_heart)
}
internal val BikeType.sensors get() = when (this) {
    BikeType.EBIKE -> listOf(BikeSensor.GPS, BikeSensor.EBIKE, BikeSensor.POWER, BikeSensor.HEART)
    BikeType.BIKE -> listOf(BikeSensor.GPS, BikeSensor.POWER, BikeSensor.HEART)
    BikeType.INDOOR -> listOf(BikeSensor.TRAINER, BikeSensor.POWER, BikeSensor.HEART)
}
internal data class BikeProfile(val slot: Int, val name: String, val type: BikeType = BikeType.EBIKE,
    val category: BikeCategory = BikeCategory.MOUNTAIN, val wheelMm: Int = 2200, val autoConnect: Boolean = true,
    val gpsEnabled: Boolean = true, val bikeEnabled: Boolean = true, val heartEnabled: Boolean = true,
    val bikeAddress: String? = null, val bikeName: String? = null,
    val heartAddress: String? = null, val heartName: String? = null, val recording: RecordingConfig = RecordingConfig())

/** Four local profiles. Legacy active keys remain the runtime/backup compatibility boundary. */
internal class BikeProfileStore(private val context: Context, private val prefs: SharedPreferences) {
    companion object { const val COUNT = 4; const val KEY = "bikeProfilesV1"; const val ACTIVE = "activeBikeSlot" }
    val activeSlot get() = prefs.getInt(ACTIVE, 0).coerceIn(0, COUNT - 1)
    private fun default(slot: Int) = BikeProfile(slot, context.getString(R.string.bikes_default_name, slot + 1))
    init {
        if (!prefs.contains(KEY)) {
            val rows = List(COUNT) { default(it) }.toMutableList()
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
    fun all(): List<BikeProfile> {
        val json = prefs.getString(KEY, "[]") ?: "[]"
        val rows = if (json == cachedJson) cachedProfiles else null
        val profiles = rows ?: parse(json).also { cachedJson = json; cachedProfiles = it }
        return profiles.map { if (it.slot == activeSlot) legacy(it) else it }
    }
    private fun parse(json: String): List<BikeProfile> {
        val array = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        return List(COUNT) { slot ->
            val fallback = default(slot)
            val stored = runCatching {
                val o = array.getJSONObject(slot)
                fallback.copy(name = o.getString("name").takeIf { it.isNotBlank() && it.length <= 60 && it.none(Char::isISOControl) } ?: fallback.name,
                    type = BikeType.valueOf(o.getString("type")), category = BikeCategory.valueOf(o.getString("category")),
                    wheelMm = o.getInt("wheelMm").takeIf { it in 500..4000 } ?: fallback.wheelMm,
                    autoConnect = o.getBoolean("autoConnect"), gpsEnabled = o.getBoolean("gpsEnabled"),
                    bikeEnabled = o.getBoolean("bikeEnabled"), heartEnabled = o.getBoolean("heartEnabled"),
                    bikeAddress = o.optString("bikeAddress").takeIf(String::isNotEmpty), bikeName = o.optString("bikeName").takeIf(String::isNotEmpty),
                    heartAddress = o.optString("heartAddress").takeIf(String::isNotEmpty), heartName = o.optString("heartName").takeIf(String::isNotEmpty),
                    recording = RecordingConfig.decode(o.getJSONArray("fields").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }, o.getString("speed"), o.getString("distance")))
            }.getOrDefault(fallback)
            stored
        }
    }
    fun current() = all()[activeSlot]
    fun persistActive() = save(current())
    fun save(profile: BikeProfile) {
        require(profile.slot == activeSlot && profile.type.available && profile.name.isNotBlank() && profile.name.length <= 60 && profile.name.none(Char::isISOControl) && profile.wheelMm in 500..4000)
        val rows = all().toMutableList(); rows[profile.slot] = profile
        writeLegacy(prefs.edit().putString(KEY, encode(rows)), profile).apply()
    }
    fun select(slot: Int) {
        require(slot in 0 until COUNT)
        val rows = all()
        require(rows[slot].type.available)
        writeLegacy(prefs.edit().putString(KEY, encode(rows)).putInt(ACTIVE, slot), rows[slot]).apply()
    }
    private fun writeLegacy(edit: SharedPreferences.Editor, p: BikeProfile): SharedPreferences.Editor = edit
        .putString("address", p.bikeAddress).putString("name", p.bikeName)
        .putString("heartAddress", p.heartAddress).putString("heartName", p.heartName)
        .putBoolean("gpsEnabled", p.gpsEnabled).putBoolean("bikeEnabled", p.bikeEnabled).putBoolean("heartEnabled", p.heartEnabled)
        .putStringSet("recordFields", p.recording.fields).putString("recordSpeed", p.recording.speedSource).putString("recordDistance", p.recording.distanceSource)
    private fun encode(rows: List<BikeProfile>) = JSONArray().apply { rows.forEach { p -> put(JSONObject()
        .put("name", p.name).put("type", p.type.name).put("category", p.category.name).put("wheelMm", p.wheelMm).put("autoConnect", p.autoConnect)
        .put("gpsEnabled", p.gpsEnabled).put("bikeEnabled", p.bikeEnabled).put("heartEnabled", p.heartEnabled)
        .put("bikeAddress", p.bikeAddress.orEmpty()).put("bikeName", p.bikeName.orEmpty()).put("heartAddress", p.heartAddress.orEmpty()).put("heartName", p.heartName.orEmpty())
        .put("fields", JSONArray(p.recording.fields.sorted())).put("speed", p.recording.speedSource).put("distance", p.recording.distanceSource)) } }.toString()
}
