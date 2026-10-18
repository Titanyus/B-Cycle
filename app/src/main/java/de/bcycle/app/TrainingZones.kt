package de.bcycle.app

import java.util.Locale

/** Inclusive integer Watt ranges; exact percentages are used for fractional live averages. */
data class PowerZone(val index: Int, val lowerW: Int, val upperW: Int?) {
    val isEmpty get() = upperW != null && lowerW > upperW
}
object Coggan {
    val boundaries = listOf(55, 75, 90, 105, 120, 150)
    fun zones(ftp: Int?): List<PowerZone>? {
        if (ftp == null || ftp !in RiderProfileValidation.ftpRange) return null
        val limits = boundaries.map { ftp * it / 100 }
        return List(7) { i -> PowerZone(i + 1, if (i == 0) 0 else limits[i - 1] + 1, limits.getOrNull(i)) }
    }
    fun zone(watts: Double?, ftp: Int?): Int? {
        if (watts == null || !watts.isFinite() || watts !in 0.0..65535.0 || ftp == null || ftp !in RiderProfileValidation.ftpRange) return null
        return boundaries.indexOfFirst { watts <= ftp!! * it / 100.0 }.let { if (it < 0) 7 else it + 1 }
    }
}

internal enum class TrainingMetric(val label: Int, val unit: String = "") {
    TRAINING_POWER_ZONE(R.string.training_power_zone), TRAINING_HEART_ZONE(R.string.training_heart_zone),
    TRAINING_FTP_PERCENT(R.string.training_ftp_percent, "%"), TRAINING_HRR_PERCENT(R.string.training_hrr_percent, "%");
    fun value(snapshot: TrainingSnapshot): DashboardValue {
        val raw = when (this) {
            TRAINING_POWER_ZONE -> snapshot.powerZone?.toDouble()
            TRAINING_HEART_ZONE -> snapshot.heartZone?.toDouble()
            TRAINING_FTP_PERCENT -> snapshot.powerPercent
            TRAINING_HRR_PERCENT -> snapshot.heartPercent
        }
        val text = raw?.let { if (unit.isEmpty()) "Z${it.toInt()}" else String.format(Locale.GERMANY, "%.0f", it) } ?: "—"
        return DashboardValue(text, unit, raw != null)
    }
}

internal enum class ZoneGlowSource(val label: Int) {
    OFF(R.string.zone_glow_off), POWER_CURRENT(R.string.zone_glow_power_current), POWER_AVERAGE(R.string.zone_glow_power_average),
    HEART_CURRENT(R.string.zone_glow_heart_current), HEART_AVERAGE(R.string.zone_glow_heart_average)
}
/** Restrict physiological sources to the matching displayed measurement family. */
internal object ZoneGlowCompatibility {
    fun allowed(field:DashboardField?):List<ZoneGlowSource> = when(field?.id) {
        "HEART_RATE","HEART_AVERAGE","HEART_MAXIMUM","TRAINING_HEART_ZONE","TRAINING_HRR_PERCENT" ->
            listOf(ZoneGlowSource.OFF,ZoneGlowSource.HEART_CURRENT,ZoneGlowSource.HEART_AVERAGE)
        "RIDER_POWER","MOTOR_POWER","AVERAGE_POWER","MAXIMUM_POWER","TRAINING_POWER_ZONE","TRAINING_FTP_PERCENT" ->
            listOf(ZoneGlowSource.OFF,ZoneGlowSource.POWER_CURRENT,ZoneGlowSource.POWER_AVERAGE)
        else -> ZoneGlowSource.entries
    }
}
internal data class ZoneGlowConfig(val sources: List<ZoneGlowSource> = List(DashboardLayout.FIELD_COUNT) { ZoneGlowSource.OFF }) {
    fun replace(slot: Int, source: ZoneGlowSource) = copy(sources = sources.mapIndexed { i, old -> if (i == slot) source else old })
    fun compatible(layout:DashboardLayout,visibility:DashboardDataVisibility)=copy(sources=sources.mapIndexed { i,source ->
        source.takeIf { it in ZoneGlowCompatibility.allowed(layout.fields[i].takeIf { visibility.visible[i] }) } ?: ZoneGlowSource.OFF
    })
    fun encode() = sources.joinToString("|") { it.name }
    companion object {
        fun key(page: Int) = "dashboardZoneGlow_$page"
        fun decode(text: String?): ZoneGlowConfig {
            val parts = text?.split('|').orEmpty()
            return ZoneGlowConfig(List(DashboardLayout.FIELD_COUNT) { i -> ZoneGlowSource.entries.find { it.name == parts.getOrNull(i) } ?: ZoneGlowSource.OFF })
        }
        fun read(prefs: android.content.SharedPreferences, page: Int) = decode(prefs.getString(key(page), null))
    }
}

/** Read-only timestamped inputs from the existing runtime; not a recorder or ride statistic. */
internal data class TrainingInputs(val power: Double? = null, val powerAt: Long? = null, val powerAverage: Double? = null,
    val heart: Double? = null, val heartAt: Long? = null, val heartAverage: Double? = null)
internal data class TrainingSnapshot(val profile: RiderProfile = RiderProfile(), val power: Double? = null,
    val heart: Double? = null, val averagePower: Double? = null, val averageHeart: Double? = null) {
    private val heartModel=profile.heartZones()
    private val reserveModel=Karvonen.calculate(profile.restingHeartRate,profile.maximumHeartRate)
    val powerZone get() = Coggan.zone(power, profile.ftpW)
    private fun powerPercentFor(value: Double?) = value?.takeIf { Coggan.zone(it,profile.ftpW)!=null }?.let { it * 100 / profile.ftpW!! }
    val powerPercent get() = powerPercentFor(power)
    private fun heartPercentFor(value: Double?): Double? = value?.takeIf { it.isFinite() && it in 1.0..250.0 }?.let { bpm ->
        reserveModel?.let { (bpm - it.restingHeartRate) * 100 / it.reserve }
    }
    private fun heartZoneFor(value: Double?): Int? {
        val model=heartModel ?: return null
        val bpm=value?.takeIf { it.isFinite() && it in 1.0..250.0 } ?: return null
        return model.zoneFor(bpm)?.index
    }
    val heartPercent get() = heartPercentFor(heart)
    val heartZone get() = heartZoneFor(heart)
    private fun modelPercentFor(value:Double?)=value?.takeIf { it.isFinite() && it in 1.0..250.0 }?.let { heartModel?.percent(it) }
    private fun heartColor(value: Double?) = modelPercentFor(value)?.takeIf { heartZoneFor(value)!=null }?.let { ZoneGlowColors.heart(it,heartModel!!.boundariesPercent) }
    fun glow(source: ZoneGlowSource, zoneTwo:Boolean=false): Int? {
        if(zoneTwo) return when(source) {
            ZoneGlowSource.OFF -> null
            ZoneGlowSource.POWER_CURRENT -> powerPercent?.let { ZoneTwoHelpColors.color(it,55.0,75.0) }
            ZoneGlowSource.POWER_AVERAGE -> powerPercentFor(averagePower)?.let { ZoneTwoHelpColors.color(it,55.0,75.0) }
            ZoneGlowSource.HEART_CURRENT -> modelPercentFor(heart)?.let { ZoneTwoHelpColors.color(it,heartModel!!.zones[1].fromPercent.toDouble(),heartModel.zones[1].toPercent.toDouble()) }
            ZoneGlowSource.HEART_AVERAGE -> modelPercentFor(averageHeart)?.let { ZoneTwoHelpColors.color(it,heartModel!!.zones[1].fromPercent.toDouble(),heartModel.zones[1].toPercent.toDouble()) }
        }
        return when (source) {
        ZoneGlowSource.OFF -> null
        ZoneGlowSource.POWER_CURRENT -> powerPercent?.let { ZoneGlowColors.power(it) }
        ZoneGlowSource.POWER_AVERAGE -> powerPercentFor(averagePower)?.let { ZoneGlowColors.power(it) }
        ZoneGlowSource.HEART_CURRENT -> heartColor(heart)
        ZoneGlowSource.HEART_AVERAGE -> heartColor(averageHeart)
        }
    }
}

/** Time-weighted display window. Missing intervals are never bridged. */
internal class TrainingDisplayWindow(private val windowMs:Long=3000) {
    private data class Sample(val at: Long, val value: Double)
    private val samples = java.util.ArrayDeque<Sample>()
    private var lastAt: Long? = null
    fun update(value: Double?, at: Long?, now: Long): Double? {
        if (value == null || !value.isFinite() || at == null || at > now || now - at > 5000 || (lastAt != null && at < lastAt!!)) {
            samples.clear(); lastAt = null; return null
        }
        if (at != lastAt) {
            if (lastAt != null && at - lastAt!! > 5000) samples.clear()
            samples.addLast(Sample(at, value)); lastAt = at
        }
        val start = now - windowMs
        while (samples.size > 1 && samples.elementAt(1).at <= start) samples.removeFirst()
        // Defensive cap for implausibly fast sources; ordinary BLE stays far below it.
        while (samples.size > 512) samples.removeFirst()
        var area = 0.0; var duration = 0L
        val iterator = samples.iterator(); var previous = if (iterator.hasNext()) iterator.next() else return null
        while (iterator.hasNext()) {
            val next = iterator.next(); val dt = (next.at - maxOf(previous.at, start)).coerceAtLeast(0)
            area += previous.value * dt; duration += dt; previous = next
        }
        val dt = (now - maxOf(previous.at, start)).coerceAtLeast(0)
        area += previous.value * dt; duration += dt
        return if (duration == 0L) previous.value else area / duration
    }
}
internal class TrainingPresentation {
    private val power = TrainingDisplayWindow(5000); private val heart = TrainingDisplayWindow()
    fun update(input: TrainingInputs, profile: RiderProfile, now: Long): TrainingSnapshot {
        val p = input.power?.takeIf { it in 0.0..65535.0 }
        val h = input.heart?.takeIf { it in 1.0..250.0 }
        return TrainingSnapshot(profile, power.update(p, input.powerAt, now), heart.update(h, input.heartAt, now),
            input.powerAverage?.takeIf { p != null && it.isFinite() && it in 0.0..65535.0 && input.powerAt?.let { now - it in 0..5000 } == true },
            input.heartAverage?.takeIf { h != null && it.isFinite() && it in 1.0..250.0 && input.heartAt?.let { now - it in 0..5000 } == true })
    }
}

internal object ZoneGlowColors {
    val powerColors = listOf(0xff8696a5.toInt(), 0xff42a5f5.toInt(), 0xff66bb6a.toInt(), 0xffffca28.toInt(), 0xffff8a26.toInt(), 0xffef5350.toInt(), 0xffab47bc.toInt())
    fun mix(a: Int, b: Int, fraction: Float): Int {
        val t = fraction.coerceIn(0f, 1f)
        fun channel(shift: Int) = (((a ushr shift and 255) * (1 - t)) + ((b ushr shift and 255) * t)).toInt()
        return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
    private fun color(percent: Double, boundaries: List<Int>, colors: List<Int>): Int {
        boundaries.forEachIndexed { i, boundary ->
            val width=minOf(3.0,boundaries.getOrNull(i-1)?.let { (boundary-it)/2.0 } ?: 3.0,boundaries.getOrNull(i+1)?.let { (it-boundary)/2.0 } ?: 3.0)
            if (percent < boundary - width) return colors[i]
            if (percent <= boundary + width) return mix(colors[i], colors[i + 1], ((percent - boundary + width) / (2*width)).toFloat())
        }
        return colors.last()
    }
    fun power(percent: Double) = color(percent, Coggan.boundaries, powerColors)
    fun heart(percent: Double,boundaries:List<Int> = listOf(60,70,80,90)) = color(percent,boundaries,TrainingZoneColors.values.toList())
}
