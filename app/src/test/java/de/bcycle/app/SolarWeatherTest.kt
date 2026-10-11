package de.bcycle.app

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SolarWeatherTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private fun closeTo(actual: Instant?, date: String, time: String) {
        assertNotNull(actual)
        val expected = java.time.LocalDateTime.parse("${date}T$time").atZone(berlin).toInstant()
        assertTrue("$actual vs $expected", kotlin.math.abs(actual!!.epochSecond - expected.epochSecond) < 240)
    }
    @Test fun referenceSummerAndWinterIncludeDaylightSavingTime() {
        val summer = SolarCalculator.calculate(LocalDate.parse("2026-06-21"), berlin, 52.52, 13.405)
        closeTo(summer.sunrise, "2026-06-21", "04:43")
        closeTo(summer.sunset, "2026-06-21", "21:33")
        val winter = SolarCalculator.calculate(LocalDate.parse("2026-12-21"), berlin, 52.52, 13.405)
        closeTo(winter.sunrise, "2026-12-21", "08:15")
        closeTo(winter.sunset, "2026-12-21", "15:54")
    }
    @Test fun cacheCountdownMissingGpsAndRefreshBoundaries() {
        val engine = SolarWeather()
        val now = Instant.parse("2026-06-21T12:00:00Z")
        val first = engine.snapshot(52.52, 13.405, now, berlin)
        val later = engine.snapshot(52.52, 13.405, now.plusSeconds(600), berlin)
        assertEquals(1, engine.calculations)
        assertEquals(first.remainingMs!! - 600000, later.remainingMs)
        assertEquals(SolarValues(zone = berlin), engine.snapshot(null, null, now, berlin))
        assertEquals(1, engine.calculations)
        val after = engine.snapshot(52.52, 13.405, first.sunset!!.plusSeconds(1), berlin)
        assertEquals(0L, after.remainingMs)
        engine.snapshot(52.52, 13.405, now.plusSeconds(86400), berlin)
        assertEquals(LocalDate.parse("2026-06-22"), engine.snapshot(52.52, 13.405, now.plusSeconds(86400), berlin).sunset!!.atZone(berlin).toLocalDate())
        val count = engine.calculations
        engine.snapshot(53.52, 13.405, now.plusSeconds(86400), berlin)
        assertEquals(count + 1, engine.calculations)
    }
    @Test fun polarDaysAndInvalidPositionsDoNotInventSunEvents() {
        for (date in listOf("2026-06-21", "2026-12-21")) {
            val v = SolarCalculator.calculate(LocalDate.parse(date), berlin, 78.22, 15.65)
            assertNull(v.sunrise); assertNull(v.sunset)
        }
        assertNull(SolarCalculator.calculate(LocalDate.parse("2026-06-21"), berlin, Double.NaN, 0.0).sunrise)
    }
    @Test fun dateLineEventsBelongToRequestedLocalDay() {
        for ((zone, longitude) in listOf("Pacific/Kiritimati" to -157.4, "Pacific/Auckland" to 174.8)) {
            val z = ZoneId.of(zone); val date = LocalDate.parse("2026-03-21")
            val v = SolarCalculator.calculate(date, z, -10.0, longitude)
            assertEquals(date, v.sunrise!!.atZone(z).toLocalDate())
            assertEquals(date, v.sunset!!.atZone(z).toLocalDate())
            assertTrue(v.sunrise.isBefore(v.sunset))
        }
    }
    @Test fun fieldsPersistAndAreSelectableForDashboardAndMap() {
        val solar = SolarValues(Instant.parse("2026-06-21T02:43:00Z"), Instant.parse("2026-06-21T19:33:00Z"), 5040000, berlin)
        val gps = GpsValues(solar = solar)
        assertEquals("04:43", DashboardField("WEATHER_SUNRISE").value(LiveData(), gps).text)
        assertEquals("21:33", DashboardField("WEATHER_SUNSET").value(LiveData(), gps).text)
        for (metric in WeatherMetric.entries) {
            val f = DashboardField.fromId(metric.name)!!
            assertEquals("Wetter", f.source)
            assertFalse(f.value(LiveData()).available)
            assertEquals(f, DashboardLayout.decode(DashboardLayout.defaults.replace(0, f).encode()).fields[0])
        }
        assertEquals("01:24", DashboardField("WEATHER_DAYLIGHT").value(LiveData(), gps).text)
        assertEquals("00:00", WeatherMetric.WEATHER_UNTIL_SUNSET.value(solar.copy(remainingMs = 0)).text)
        assertEquals(4, DashboardField.groups(LiveData()).single { it.source == "Wetter" }.fields.size)
    }
}
