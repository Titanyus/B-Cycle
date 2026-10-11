package de.bcycle.app

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.*

/** Standard apparent horizon (solar centre -0.833°); no terrain/weather correction. */
data class SolarValues(val sunrise: Instant? = null, val sunset: Instant? = null,
    val remainingMs: Long? = null, val zone: ZoneId = ZoneId.systemDefault())

/** NOAA solar equations, independent of Android, network access and sensor sampling. */
internal object SolarCalculator {
    private fun rad(degrees: Double) = Math.toRadians(degrees)
    private fun solar(jd: Double): Pair<Double, Double> {
        val t = (jd - 2451545.0) / 36525.0
        val l = ((280.46646 + t * (36000.76983 + t * .0003032)) % 360 + 360) % 360
        val m = 357.52911 + t * (35999.05029 - .0001537 * t)
        val e = .016708634 - t * (.000042037 + .0000001267 * t)
        val c = sin(rad(m)) * (1.914602 - t * (.004817 + .000014 * t)) +
            sin(rad(2 * m)) * (.019993 - .000101 * t) + sin(rad(3 * m)) * .000289
        val omega = 125.04 - 1934.136 * t
        val apparent = l + c - .00569 - .00478 * sin(rad(omega))
        val ob = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (.00059 - t * .001813))) / 60) / 60 + .00256 * cos(rad(omega))
        val decl = asin(sin(rad(ob)) * sin(rad(apparent)))
        val y = tan(rad(ob / 2)).pow(2)
        val eq = 4 * Math.toDegrees(y * sin(2 * rad(l)) - 2 * e * sin(rad(m)) +
            4 * e * y * sin(rad(m)) * cos(2 * rad(l)) - .5 * y * y * sin(4 * rad(l)) - 1.25 * e * e * sin(2 * rad(m)))
        return eq to decl
    }
    fun calculate(date: LocalDate, zone: ZoneId, latitude: Double, longitude: Double): SolarValues {
        if (!latitude.isFinite() || latitude !in -90.0..90.0 || !longitude.isFinite() || longitude !in -180.0..180.0) return SolarValues(zone = zone)
        fun event(day: LocalDate, rising: Boolean): Instant? {
            val midnight = day.atStartOfDay(ZoneOffset.UTC).toInstant()
            val jd = midnight.toEpochMilli() / 86400000.0 + 2440587.5
            var minutes = 720 - 4 * longitude
            repeat(3) {
                val (eq, decl) = solar(jd + minutes / 1440)
                val hourCos = (cos(rad(90.833)) - sin(rad(latitude)) * sin(decl)) / (cos(rad(latitude)) * cos(decl))
                if (!hourCos.isFinite() || hourCos !in -1.0..1.0) return null
                val angle = Math.toDegrees(acos(hourCos))
                minutes = 720 - 4 * longitude - eq + (if (rising) -4 else 4) * angle
            }
            return midnight.plusMillis((minutes * 60000).roundToLong())
        }
        // Search adjacent UTC dates too: local day boundaries and date-line zones differ.
        fun today(rising: Boolean) = (-1L..1L).mapNotNull { event(date.plusDays(it), rising) }
            .firstOrNull { it.atZone(zone).toLocalDate() == date }
        return SolarValues(today(true), today(false), zone = zone)
    }
}

/** Recalculate at most hourly, or after 25 km, a local day/zone change. Stale GPS yields no data. */
internal class SolarWeather {
    private var cached: SolarValues? = null
    private var cachedDate: LocalDate? = null
    private var computedAt: Instant? = null
    private var lat = 0.0
    private var lon = 0.0
    internal var calculations = 0; private set
    fun snapshot(latitude: Double?, longitude: Double?, now: Instant, zone: ZoneId): SolarValues {
        if (latitude == null || longitude == null || !latitude.isFinite() || latitude !in -90.0..90.0 ||
            !longitude.isFinite() || longitude !in -180.0..180.0) return SolarValues(zone = zone)
        val date = now.atZone(zone).toLocalDate()
        val a = sin(Math.toRadians(latitude - lat) / 2).pow(2) + cos(Math.toRadians(lat)) *
            cos(Math.toRadians(latitude)) * sin(Math.toRadians(longitude - lon) / 2).pow(2)
        val moved = 6371000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        if (cached == null || cachedDate != date || cached!!.zone != zone || moved >= 25000 ||
            computedAt!!.let { now.isBefore(it) || now.epochSecond - it.epochSecond >= 3600 }) {
            cached = SolarCalculator.calculate(date, zone, latitude, longitude)
            cachedDate = date; computedAt = now; lat = latitude; lon = longitude; calculations++
        }
        return cached!!.copy(remainingMs = cached!!.sunset?.let { (it.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0) })
    }
}

enum class WeatherMetric(val title: String) {
    WEATHER_SUNRISE("Sonnenaufgang"), WEATHER_SUNSET("Sonnenuntergang"),
    WEATHER_UNTIL_SUNSET("Zeit bis Sonnenuntergang"), WEATHER_DAYLIGHT("Verbleibendes Tageslicht");
    fun value(solar: SolarValues): DashboardValue {
        val text = when (this) {
            WEATHER_SUNRISE -> solar.sunrise?.atZone(solar.zone)?.format(clock)
            WEATHER_SUNSET -> solar.sunset?.atZone(solar.zone)?.format(clock)
            else -> solar.remainingMs?.let { val minutes = it / 60000; "%02d:%02d".format(java.util.Locale.GERMANY, minutes / 60, minutes % 60) }
        }
        return DashboardValue(text ?: "—", "", text != null)
    }
    companion object { private val clock = DateTimeFormatter.ofPattern("HH:mm") }
}
