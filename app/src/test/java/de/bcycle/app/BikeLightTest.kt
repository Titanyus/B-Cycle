package de.bcycle.app

import com.garmin.fit.FitDecoder
import com.garmin.fit.plugins.ActivityFileValidationPlugin
import com.garmin.fit.plugins.ActivityFileValidationResult
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BikeLightTest {
    @get:Rule val folder = TemporaryFolder()
    private fun packet(vararg bytes: Int) = bytes.map { it.toByte() }.toByteArray()
    @Test fun readOnlySubscriptionDecodesBooleanPushesAndKeepsUnknownSeparateFromOff() {
        val p = McspProtocol()
        assertTrue(p.subscriptions().any { it.description.contains("181C", true) })
        assertTrue(p.reads().any { it.description.contains("181C", true) })
        val on = p.receive(packet(0x30, 4, 0x98, 0x1c, 8, 1)).update
        val off = p.receive(packet(0x30, 2, 0x98, 0x1c)).update
        assertEquals(1L, on.value(LiveMetric.BIKE_LIGHT))
        assertEquals(0L, on.merge(off).value(LiveMetric.BIKE_LIGHT))
        val ack = p.receive(packet(0x30, 5, 0x18, 0x1c, 0xc0, 0x80, 0x70)).update
        assertNull(ack.value(LiveMetric.BIKE_LIGHT))
        val mixed = p.receive(packet(0x30, 4, 0x98, 0x1c, 8, 2, 0x30, 4, 0x98, 0x5b, 8, 123))
        assertNull(mixed.update.value(LiveMetric.BIKE_LIGHT))
        assertEquals(123, mixed.update.riderPowerWatts)
        assertTrue(mixed.events.any { it.contains("UNKNOWN_LIGHT") })
        val f = DashboardField("BIKE_LIGHT")
        assertFalse(f.value(LiveData()).available)
        assertEquals("An", f.value(on).text); assertEquals("Aus", f.value(off).text)
        assertFalse(f.numeric)
        assertTrue(DashboardField.groups(LiveData()).any { f in it.fields })
        assertEquals("bool", RecordField.options.single { it.id == "BIKE_LIGHT" }.unit)
        assertFalse(RecordField.options.any { it.source == "Wetter" })
    }
    @Test fun fitRecordsOnOffAndMissingAsDeveloperFieldWithGarminValidation() {
        val start = 1791626400000L
        val config = RecordingConfig(setOf("BIKE_LIGHT", "SPEED", "BIKE_DISTANCE"))
        val journal = folder.newFile("light.ride")
        RideJournal(journal, start, config).use { j ->
            listOf(1L, 0L, null).forEachIndexed { i, raw ->
                val bike = raw?.let { LiveData.sample(LiveMetric.BIKE_LIGHT, it) } ?: LiveData()
                j.append(RideSample(start + i * 1000, i * 1000L, values = recordingValues(bike, GpsValues(), i.toLong(), PulseValues()) + ("SPEED" to 0.0)))
            }
        }
        val fit = folder.newFile("light.fit"); FitWriter.encode(journal, fit)
        val validator = ActivityFileValidationPlugin()
        val messages = fit.inputStream().use { FitDecoder().decode(it, validator) }
        assertFalse(validator.results.any { it.status == ActivityFileValidationResult.Status.FAILED })
        val values = messages.recordMesgs.map { it.developerFields.singleOrNull { f -> f.name == "BIKE_LIGHT" }?.getValue(0) }
        assertEquals(1.0, (values[0] as Number).toDouble(), 0.0)
        assertEquals(0.0, (values[1] as Number).toDouble(), 0.0)
        assertNull(values[2])
        assertEquals("bool", messages.recordMesgs.first().developerFields.single { it.name == "BIKE_LIGHT" }.units)
    }
}
