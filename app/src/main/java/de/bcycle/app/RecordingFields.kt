package de.bcycle.app

/** Stable storage semantics. No dashboard, Android resources or formatted values. */
enum class FitFieldScope { RECORD, SUMMARY }
data class RecordingFieldDefinition(val id: String, val unit: String, val scope: FitFieldScope = FitFieldScope.RECORD)

object RecordingFields {
    // Preserve release order and legacy entries: journals and FIT developer indices depend on it.
    val options: List<RecordingFieldDefinition> = listOf(
        RecordingFieldDefinition("GPS_POSITION", "semicircles"),
        RecordingFieldDefinition("GPS_SPEED", "m/s"),
        RecordingFieldDefinition("GPS_DISTANCE", "m"),
        RecordingFieldDefinition("GPS_MOVING_AVERAGE", "m/s"),
        RecordingFieldDefinition("GPS_OVERALL_AVERAGE", "m/s"),
        RecordingFieldDefinition("GPS_MAXIMUM_SPEED", "m/s"),
        RecordingFieldDefinition("GPS_ELAPSED_TIME", "s"),
        RecordingFieldDefinition("GPS_MOVING_TIME", "s"),
        RecordingFieldDefinition("GPS_STOPPED_TIME", "s"),
        RecordingFieldDefinition("GPS_ALTITUDE", "m"),
        RecordingFieldDefinition("GPS_ASCENT", "m"),
        RecordingFieldDefinition("GPS_DESCENT", "m"),
        RecordingFieldDefinition("GPS_GRADIENT", "%"),
        RecordingFieldDefinition("SPEED", "m/s"),
        RecordingFieldDefinition("BIKE_DISTANCE", "m"),
        RecordingFieldDefinition("ODOMETER", "m"),
        RecordingFieldDefinition("ASSIST_MODE", "index"),
        RecordingFieldDefinition("RIDER_POWER", "W"),
        RecordingFieldDefinition("MOTOR_POWER", "W"),
        RecordingFieldDefinition("CADENCE", "U/min"),
        RecordingFieldDefinition("RIDER_TORQUE", "Nm"),
        RecordingFieldDefinition("MOTOR_TORQUE", "Nm"),
        RecordingFieldDefinition("BIKE_LIGHT", "bool"),
        RecordingFieldDefinition("BATTERY", "%"),
        RecordingFieldDefinition("BATTERY_TEMPERATURE", "°C"),
        RecordingFieldDefinition("REMAINING_ENERGY", "Wh"),
        RecordingFieldDefinition("DELIVERED_ENERGY", "Wh"),
        RecordingFieldDefinition("CURRENT_RANGE", "km"),
        RecordingFieldDefinition("AVERAGE_SPEED", "m/s"),
        RecordingFieldDefinition("AVERAGE_POWER", "W"),
        RecordingFieldDefinition("MAXIMUM_POWER", "W"),
        RecordingFieldDefinition("AVERAGE_CADENCE", "U/min"),
        RecordingFieldDefinition("MAXIMUM_CADENCE", "U/min"),
        RecordingFieldDefinition("RIDER_ENERGY_SHARE", "%"),
        RecordingFieldDefinition("HEART_RATE", "bpm"),
        RecordingFieldDefinition("HEART_AVERAGE", "bpm"),
        RecordingFieldDefinition("HEART_MAXIMUM", "bpm"),
        RecordingFieldDefinition("HEART_BATTERY", "%"),
        RecordingFieldDefinition("PHONE_TIME", ""),
        RecordingFieldDefinition("PHONE_BATTERY", "%"),
        RecordingFieldDefinition("NORMALIZED_POWER", "W", FitFieldScope.SUMMARY),
        RecordingFieldDefinition("MECHANICAL_WORK", "kJ", FitFieldScope.SUMMARY),
        RecordingFieldDefinition("CALORIES", "kcal", FitFieldScope.SUMMARY),
        RecordingFieldDefinition("SPEED_POWER_RATIO", "km/h/W", FitFieldScope.SUMMARY),
        RecordingFieldDefinition("EFFICIENCY_FACTOR", "W/bpm", FitFieldScope.SUMMARY),
        RecordingFieldDefinition("ESTIMATED_FTP", "W", FitFieldScope.SUMMARY),
    )
    val summaries = options.filter { it.scope == FitFieldScope.SUMMARY }
    val defaultIds = options.filter { it.scope == FitFieldScope.RECORD }.map { it.id }.toSet()
    val ids = options.map { it.id }.toSet()
}
