package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class BikeTypeIconsTest {
    @Test fun bikeAndSensorIdentityMatchAcrossSources() {
        assertEquals(BicycleIcon.Bikes,BikeTypeIcons.forType(null))
        assertEquals(BicycleIcon.Bikes,BikeTypeIcons.forType(BikeType.BIKE))
        assertEquals(BikeTypeIcons.forType(BikeType.EBIKE),SensorIcons.forSensor(BikeSensor.EBIKE))
        assertEquals(SensorIcons.forSensor(BikeSensor.EBIKE),SensorIcons.forSource("E-Bike"))
        assertEquals(BikeTypeIcons.forType(BikeType.INDOOR),SensorIcons.forSource("Indoor-Bike"))
        assertEquals(SensorIcons.forSensor(BikeSensor.TRAINER),BatteryFields.icon(BatterySource.TRAINER))
        assertNotEquals(BikeTypeIcons.forType(BikeType.INDOOR),BikeTypeIcons.forType(BikeType.EBIKE))
    }
    @Test fun fitChoicesExcludeDashboardBatteryCategoryButKeepSensorBatteries() {
        assertTrue(RecordField.options.none { it.source=="Akku" || it.id.startsWith("BATTERIES") })
        assertEquals("E-Bike",RecordField.options.single { it.id=="BATTERY" }.source)
        assertEquals("Pulsmesser",RecordField.options.single { it.id=="HEART_BATTERY" }.source)
        assertEquals(RecordField.options.size,RecordField.options.map { it.id }.distinct().size)
    }
}
