package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class DashboardLayoutTest {
    @Test fun pickerGroupsBothSourcesWithoutDuplicatesAndEveryGpsFieldPersistsInEverySlot() {
        val groups = DashboardField.groups(LiveData(rangesKm = listOf(100, 80)))
        assertEquals(listOf("GPS", "E-Bike", "Pulsmesser", "Wetter"), groups.map { it.source }.distinct())
        val fields = groups.flatMap { it.fields }
        assertEquals(DashboardField.options(LiveData(rangesKm = listOf(100, 80))).toSet(), fields.toSet())
        assertEquals(fields.size, fields.distinct().size)
        assertEquals("GPS", fields.single { it.id == "GPS_SPEED" }.source)
        assertEquals("E-Bike", fields.single { it.id == "SPEED" }.source)
        assertTrue(fields.any { it.id == "GPS_DISTANCE" })
        assertTrue(fields.any { it.id == "BIKE_DISTANCE" })
        for (gps in GpsMetric.entries) for (slot in 0..6) {
            val layout = DashboardLayout.defaults.replace(slot, DashboardField(gps.name))
            assertEquals(gps.name, DashboardLayout.decode(layout.encode()).fields[slot].id)
        }
        assertEquals(DashboardValue("1,25", "km", true), DashboardField("GPS_DISTANCE").value(LiveData(), GpsValues(distanceM = 1250.0)))
        assertEquals(DashboardValue("0,19", "km", true), DashboardField("BIKE_DISTANCE").value(LiveData(), bikeDistanceM = 188))
        assertEquals(DashboardValue("1:01:01", "h:mm:ss", true), DashboardField("GPS_MOVING_TIME").value(LiveData(), GpsValues(movingMs = 3_661_000)))
    }
    @Test fun allSevenFieldsIncludingLargeCenterCanBeChangedAndRestored() {
        var layout = DashboardLayout.defaults
        for (index in 0..6) layout = layout.replace(index, DashboardField("RIDER_POWER"))
        val restored = DashboardLayout.decode(layout.encode())
        assertEquals(List(7) { DashboardField("RIDER_POWER") }, restored.fields)
        assertEquals("83", restored.fields[2].value(LiveData(riderPowerWatts = 83)).text)
        // Rider power has no reserved position and can be removed entirely.
        for (index in 0..6) layout = layout.replace(index, DashboardField("BATTERY"))
        assertTrue(layout.fields.none { it.id == "RIDER_POWER" })
    }

    @Test fun defaultsIncludeOwnPowerSeparatelyFromMotorPowerAndStableIdsSurviveInvalidSettings() {
        val defaults = DashboardLayout.defaults
        assertTrue(defaults.fields.any { it.id == "RIDER_POWER" })
        assertTrue(defaults.fields.any { it.id == "MOTOR_POWER" })
        val partial = DashboardLayout.decode("BATTERY|unknown|RIDER_POWER")
        assertEquals(DashboardField("BATTERY"), partial.fields[0])
        assertEquals(defaults.fields[1], partial.fields[1])
        assertEquals(DashboardField("RIDER_POWER"), partial.fields[2])
        assertEquals(defaults.fields.drop(3), partial.fields.drop(3))
        assertNull(DashboardField.fromId("RANGE_16"))
        assertNull(DashboardField.fromId("RANGE_-1"))
        assertEquals(defaults.encode(), DashboardLayout.decode(null).encode())
    }

    @Test fun allExistingMetricsCanBeSelectedAndFormatValuesAndUnitsSeparately() {
        val data = LiveData(2530, 83, mapOf(LiveMetric.MOTOR_POWER to 200L, LiveMetric.CADENCE to 110L))
        for (metric in LiveMetric.entries) assertTrue(DashboardField.baseOptions.any { it.id == metric.name })
        assertEquals(DashboardValue("25,3", "km/h", true), DashboardField("SPEED").value(data))
        assertEquals(DashboardValue("83", "W", true), DashboardField("RIDER_POWER").value(data))
        assertEquals(DashboardValue("200", "W", true), DashboardField("MOTOR_POWER").value(data))
        assertEquals(DashboardValue("55,0", "U/min", true), DashboardField("CADENCE").value(data))
        assertEquals(DashboardValue("—", "W", false), DashboardField("RIDER_POWER").value(LiveData()))
        assertEquals(DashboardValue("0", "W", true), DashboardField("RIDER_POWER").value(LiveData(riderPowerWatts = 0)))
    }

    @Test fun selectableRangesUseBikeModeNamesAndNeverGuessAnUnalignedCurrentRange() {
        val data = LiveData(otherValues = mapOf(LiveMetric.ASSIST_MODE to 2L), assistNames = listOf("OFF", "ECO", "AUTO"), rangesKm = listOf(135L, 87L))
        val options = DashboardField.options(data)
        val range = options.single { it.id == "RANGE_1" }
        assertEquals("Reichweite · AUTO", range.title(data))
        assertEquals(DashboardValue("87", "km", true), range.value(data))
        assertEquals(DashboardValue("87", "km", true), DashboardField("CURRENT_RANGE").value(data))
        assertEquals(DashboardValue("135 / 87", "km je Modus", true), DashboardField("RANGES").value(data))
        assertFalse(DashboardField("CURRENT_RANGE").value(data.copy(assistNames = null)).available)
        assertFalse(DashboardField("CURRENT_RANGE").value(data.copy(otherValues = mapOf(LiveMetric.ASSIST_MODE to 0L))).available)
        val restored = DashboardLayout.decode(DashboardLayout.defaults.replace(2, range).encode())
        assertEquals(range, restored.fields[2])
        assertFalse(restored.fields[2].value(LiveData()).available)
    }
}
