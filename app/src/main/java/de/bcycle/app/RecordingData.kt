package de.bcycle.app

data class PulseValues(val bpm: Int? = null, val average: Double? = null, val maximum: Int? = null, val battery: Int? = null)

data class RecordingConfig(
    val fields: Set<String> = RecordField.options.map { it.id }.toSet(),
    val speedSource: String = "SPEED", val distanceSource: String = "BIKE_DISTANCE",
) {
    companion object {
        fun decode(fields: Set<String>?, speed: String?, distance: String?) = RecordingConfig(
            fields?.filter { id -> RecordField.options.any { it.id == id } }?.toSet() ?: RecordField.options.map { it.id }.toSet(),
            speed?.takeIf { it in listOf("SPEED", "GPS_SPEED") } ?: "SPEED",
            distance?.takeIf { it in listOf("BIKE_DISTANCE", "GPS_DISTANCE") } ?: "BIKE_DISTANCE",
        )
    }
}

data class RecordField(val id: String, val title: String, val source: String, val group: String, val unit: String) {
    companion object {
        val options: List<RecordField> = listOf(RecordField("GPS_POSITION", "GPS-Route / Position", "GPS", "Fahrt", "semicircles")) +
            DashboardField.groups(LiveData()).flatMap { group -> group.fields.filter { it.id != "RANGES" }.map { field ->
                val unit = when (field.id) {
                    "SPEED", "AVERAGE_SPEED", "GPS_SPEED", "GPS_MOVING_AVERAGE", "GPS_OVERALL_AVERAGE", "GPS_MAXIMUM_SPEED" -> "m/s"
                    "ODOMETER", "BIKE_DISTANCE", "GPS_DISTANCE" -> "m"
                    "GPS_ELAPSED_TIME", "GPS_MOVING_TIME", "GPS_STOPPED_TIME" -> "s"
                    "ASSIST_MODE" -> "index"
                    else -> field.value(LiveData()).unit
                }
                RecordField(field.id, field.title(LiveData()), group.source, group.title, unit)
            } }
    }
}

data class RideSample(val utcMs: Long, val elapsedMs: Long, val latitude: Double? = null, val longitude: Double? = null,
                      val values: Map<String, Double> = emptyMap())

/** Never parse formatted display strings or confuse motor power with rider power. */
fun recordingValues(bike: LiveData, gps: GpsValues, distanceM: Long?, pulse: PulseValues): Map<String, Double> = buildMap {
    for (metric in LiveMetric.entries) bike.value(metric)?.let { raw ->
        val signed = if (metric == LiveMetric.BATTERY_TEMPERATURE) (raw ushr 1) xor -(raw and 1) else raw
        val value = signed / metric.divisor
        put(metric.name, if (metric == LiveMetric.SPEED || metric == LiveMetric.AVERAGE_SPEED) value / 3.6 else if (metric == LiveMetric.ODOMETER) raw.toDouble() else value)
    }
    distanceM?.let { put("BIKE_DISTANCE", it.toDouble()) }
    fun add(id: String, value: Double?) { if (value != null && value.isFinite()) put(id, value) }
    add("GPS_SPEED", gps.speedKmh?.div(3.6)); add("GPS_DISTANCE", gps.distanceM)
    add("GPS_MOVING_AVERAGE", gps.movingAverageKmh?.div(3.6)); add("GPS_OVERALL_AVERAGE", gps.overallAverageKmh?.div(3.6))
    add("GPS_MAXIMUM_SPEED", gps.maximumKmh?.div(3.6))
    add("GPS_ELAPSED_TIME", gps.elapsedMs?.div(1000.0)); add("GPS_MOVING_TIME", gps.movingMs?.div(1000.0)); add("GPS_STOPPED_TIME", gps.stoppedMs?.div(1000.0))
    add("GPS_ALTITUDE", gps.altitudeM); add("GPS_ASCENT", gps.ascentM); add("GPS_DESCENT", gps.descentM); add("GPS_GRADIENT", gps.gradientPercent)
    add("HEART_RATE", pulse.bpm?.toDouble()); add("HEART_AVERAGE", pulse.average); add("HEART_MAXIMUM", pulse.maximum?.toDouble()); add("HEART_BATTERY", pulse.battery?.toDouble())
    // Preserve the actual altitude reference instead of silently treating ellipsoid height as MSL.
    if (gps.altitudeM != null) put("GPS_ALTITUDE_MSL", if (gps.seaLevel) 1.0 else 0.0)
    val range = DashboardField("CURRENT_RANGE").value(bike)
    if (range.available) add("CURRENT_RANGE", range.text.toDoubleOrNull())
}
