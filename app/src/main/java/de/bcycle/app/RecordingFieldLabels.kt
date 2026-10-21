package de.bcycle.app

/** Presentation adapter for the existing FIT settings; labels/groups never define storage fields. */
data class RecordField(val id: String, val title: String, val source: String, val group: String, val unit: String, val scope: FitFieldScope = FitFieldScope.RECORD, val titleResource: Int? = null) {
    companion object {
        private data class Label(val title: String, val source: String, val group: String, val resource: Int? = null)
        private val labels = mapOf(
            "GPS_POSITION" to Label("GPS-Route / Position", "GPS", "Fahrt"),
            "GPS_SPEED" to Label("Geschwindigkeit", "GPS", "Fahrt"),
            "GPS_DISTANCE" to Label("Gefahrene Distanz", "GPS", "Fahrt"),
            "GPS_MOVING_AVERAGE" to Label("Ø Geschwindigkeit · Bewegung", "GPS", "Fahrt"),
            "GPS_OVERALL_AVERAGE" to Label("Ø Geschwindigkeit · gesamt", "GPS", "Fahrt"),
            "GPS_MAXIMUM_SPEED" to Label("Max. Geschwindigkeit", "GPS", "Fahrt"),
            "GPS_ELAPSED_TIME" to Label("Fahrtdauer", "GPS", "Fahrt"),
            "GPS_MOVING_TIME" to Label("Bewegungszeit", "GPS", "Fahrt"),
            "GPS_STOPPED_TIME" to Label("Standzeit", "GPS", "Fahrt"),
            "GPS_ALTITUDE" to Label("Höhe", "GPS", "Höhe"),
            "GPS_ASCENT" to Label("Höhenzugewinn", "GPS", "Höhe"),
            "GPS_DESCENT" to Label("Höhenverlust", "GPS", "Höhe"),
            "GPS_GRADIENT" to Label("Steigung", "GPS", "Höhe"),
            "SPEED" to Label("Geschwindigkeit", "E-Bike", "Fahrt"),
            "BIKE_DISTANCE" to Label("Gefahrene Distanz", "E-Bike", "Fahrt"),
            "ODOMETER" to Label("Gesamtkilometer", "E-Bike", "Fahrt"),
            "ASSIST_MODE" to Label("Unterstützung", "E-Bike", "Fahrt"),
            "RIDER_POWER" to Label("Fahrerleistung", "E-Bike", "Leistung & Tritt"),
            "MOTOR_POWER" to Label("Motorleistung", "E-Bike", "Leistung & Tritt"),
            "CADENCE" to Label("Kadenz", "E-Bike", "Leistung & Tritt"),
            "RIDER_TORQUE" to Label("Fahrerdrehmoment", "E-Bike", "Leistung & Tritt"),
            "MOTOR_TORQUE" to Label("Motordrehmoment", "E-Bike", "Leistung & Tritt"),
            "BIKE_LIGHT" to Label("Fahrradlicht", "E-Bike", "Licht"),
            "BATTERY" to Label("Akku", "E-Bike", "Akku"),
            "BATTERY_TEMPERATURE" to Label("Akkutemperatur", "E-Bike", "Akku"),
            "REMAINING_ENERGY" to Label("Verbleibende Energie", "E-Bike", "Akku"),
            "DELIVERED_ENERGY" to Label("Akkuenergie gesamt abgegeben", "E-Bike", "Akku"),
            "CURRENT_RANGE" to Label("Reichweite aktuell", "E-Bike", "Reichweite"),
            "AVERAGE_SPEED" to Label("Ø Geschwindigkeit", "E-Bike", "Durchschnitt & Spitzenwerte"),
            "AVERAGE_POWER" to Label("Ø Fahrerleistung", "E-Bike", "Durchschnitt & Spitzenwerte"),
            "MAXIMUM_POWER" to Label("Max. Fahrerleistung", "E-Bike", "Durchschnitt & Spitzenwerte"),
            "AVERAGE_CADENCE" to Label("Ø Kadenz · ohne Tretpausen", "E-Bike", "Durchschnitt & Spitzenwerte"),
            "MAXIMUM_CADENCE" to Label("Max. Kadenz", "E-Bike", "Durchschnitt & Spitzenwerte"),
            "RIDER_ENERGY_SHARE" to Label("Fahreranteil an Antriebsenergie", "E-Bike", "Durchschnitt & Spitzenwerte"),
            "HEART_RATE" to Label("Herzfrequenz", "Pulsmesser", "Puls & Sensor"),
            "HEART_AVERAGE" to Label("Ø Herzfrequenz", "Pulsmesser", "Puls & Sensor"),
            "HEART_MAXIMUM" to Label("Max. Herzfrequenz", "Pulsmesser", "Puls & Sensor"),
            "HEART_BATTERY" to Label("Sensorakku", "Pulsmesser", "Puls & Sensor"),
            "PHONE_TIME" to Label("Uhrzeit", "Smartphone", "Smartphone"),
            "PHONE_BATTERY" to Label("Smartphone-Akku", "Smartphone", "Smartphone"),
            "NORMALIZED_POWER" to Label("Normalized Power (W)", "Performance & Energie", "Fahrtkennzahlen"),
            "MECHANICAL_WORK" to Label("Mechanical Work (kJ)", "Performance & Energie", "Fahrtkennzahlen"),
            "CALORIES" to Label("Calories (kcal, geschätzt)", "Performance & Energie", "Fahrtkennzahlen"),
            "SPEED_POWER_RATIO" to Label("Speed-to-Power Ratio (km/h/W)", "Performance & Energie", "Fahrtkennzahlen"),
            "EFFICIENCY_FACTOR" to Label("Efficiency Factor (W/bpm)", "Performance & Energie", "Fahrtkennzahlen"),
            "ESTIMATED_FTP" to Label("", "Performance & Energie", "Fahrtkennzahlen", R.string.analysis_ftp_export),
        )
        val options = RecordingFields.options.map { field ->
            val label = labels.getValue(field.id)
            RecordField(field.id, label.title, label.source, label.group, field.unit, field.scope, label.resource)
        }
        val summaries = options.filter { it.scope == FitFieldScope.SUMMARY }
        val defaults get() = RecordingFields.defaultIds
    }
}
