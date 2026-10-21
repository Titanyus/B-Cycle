package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

/** Runs without Android/Robolectric or a dashboard instance. */
class RideMeasurementsTest {
    @Test fun recordingSchemaAndNumericSamplesAreAvailableWithoutUiInitialization() {
        assertEquals(46,RecordingFields.options.size)
        assertEquals(RecordingFields.options.size,RecordingFields.ids.size)
        val data=LiveData(2520,183,mapOf(LiveMetric.CADENCE to 171L,LiveMetric.BATTERY_TEMPERATURE to 49L,
            LiveMetric.ODOMETER to 14791937L,LiveMetric.BIKE_LIGHT to 0L,LiveMetric.ASSIST_MODE to 2L),
            assistNames=listOf("OFF","ECO","AUTO"),rangesKm=listOf(135,87))
        val expected=mapOf("SPEED" to 7.0,"RIDER_POWER" to 183.0,"CADENCE" to 85.5,"BATTERY_TEMPERATURE" to -2.5,
            "ODOMETER" to 14791937.0,"BIKE_LIGHT" to 0.0,"ASSIST_MODE" to 2.0,"BIKE_DISTANCE" to 70.0,
            "GPS_SPEED" to 6.0,"HEART_RATE" to 145.0,"CURRENT_RANGE" to 87.0)
        val previous=Locale.getDefault()
        try {
            listOf(Locale.GERMANY,Locale.US,Locale.forLanguageTag("ar")).forEach {
                Locale.setDefault(it)
                assertEquals(expected,recordingValues(data,GpsValues(speedKmh=21.6),70,PulseValues(bpm=145)))
            }
        } finally { Locale.setDefault(previous) }
    }
    @Test fun noNamesOrInvalidModeNeverInventRangeAndZeroIsAnActualMeasurement() {
        val data=LiveData(otherValues=mapOf(LiveMetric.ASSIST_MODE to 0L),assistNames=listOf("ECO","AUTO"),rangesKm=listOf(0,87))
        assertEquals(0L,RideMeasurements.currentRangeKm(data))
        assertEquals(0.0,recordingValues(data,GpsValues(),null,PulseValues())["CURRENT_RANGE"])
        val unknown=listOf(data.copy(assistNames=null),data.copy(rangesKm=null),data.copy(assistNames=listOf("UNKNOWN")),
            data.copy(otherValues=emptyMap()),data.copy(otherValues=mapOf(LiveMetric.ASSIST_MODE to 9L)),
            data.copy(otherValues=mapOf(LiveMetric.ASSIST_MODE to -1L)))
        unknown.forEach {
            assertNull(RideMeasurements.currentRangeKm(it))
            assertFalse(recordingValues(it,GpsValues(),null,PulseValues()).containsKey("CURRENT_RANGE"))
        }
        assertEquals(listOf("ECO","AUTO"),RideMeasurements.alignedRangeNames(data.copy(assistNames=listOf("off","ECO","AUTO"))))
    }
    @Test fun displayFormattingKeepsExistingUnitsRoundingAndFallbacks() {
        val data=LiveData(2520,183,mapOf(LiveMetric.CADENCE to 171L,LiveMetric.BATTERY_TEMPERATURE to 49L,LiveMetric.ASSIST_MODE to 2L),
            assistNames=listOf("OFF","ECO","AUTO"),rangesKm=listOf(135,87))
        assertEquals("25,2 km/h",data.display(LiveMetric.SPEED))
        assertEquals("183 W",data.display(LiveMetric.RIDER_POWER))
        assertEquals("85,5 U/min",data.display(LiveMetric.CADENCE))
        assertEquals("-2,5 °C",data.display(LiveMetric.BATTERY_TEMPERATURE))
        assertEquals("AUTO",data.display(LiveMetric.ASSIST_MODE))
        assertEquals("ECO: 135 km · AUTO: 87 km",data.displayRanges())
        assertEquals("Stufe 1: 135 km · Stufe 2: 87 km",data.copy(assistNames=null).displayRanges())
        assertEquals("—",LiveData().display(LiveMetric.SPEED))
        assertEquals("—",LiveData().displayRanges())
        assertEquals("Aus",LiveMetric.BIKE_LIGHT.format(0))
        assertEquals("An",LiveMetric.BIKE_LIGHT.format(1))
        assertEquals("—",LiveMetric.BIKE_LIGHT.format(2))
    }
}
