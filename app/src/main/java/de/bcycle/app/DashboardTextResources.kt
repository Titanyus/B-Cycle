package de.bcycle.app

import android.content.Context

/** Stable domain/source IDs remain untranslated; only their UI labels come from Android resources. */
internal object DashboardTextResources {
    private val titles = mapOf(
        "SPEED" to R.string.ui_field_speed,
        "RIDER_POWER" to R.string.ui_field_rider_power,
        "CADENCE" to R.string.ui_field_cadence,
        "ASSIST_MODE" to R.string.ui_field_assist_mode,
        "MOTOR_POWER" to R.string.ui_field_motor_power,
        "RIDER_TORQUE" to R.string.ui_field_rider_torque,
        "MOTOR_TORQUE" to R.string.ui_field_motor_torque,
        "BIKE_LIGHT" to R.string.ui_field_bike_light,
        "BATTERY" to R.string.ui_field_battery,
        "ODOMETER" to R.string.ui_field_odometer,
        "BATTERY_TEMPERATURE" to R.string.ui_field_battery_temperature,
        "REMAINING_ENERGY" to R.string.ui_field_remaining_energy,
        "DELIVERED_ENERGY" to R.string.ui_field_delivered_energy,
        "AVERAGE_POWER" to R.string.ui_field_average_power,
        "MAXIMUM_POWER" to R.string.ui_field_maximum_power,
        "AVERAGE_CADENCE" to R.string.ui_field_average_cadence,
        "MAXIMUM_CADENCE" to R.string.ui_field_maximum_cadence,
        "AVERAGE_SPEED" to R.string.ui_field_average_speed,
        "RIDER_ENERGY_SHARE" to R.string.ui_field_rider_energy_share,
        "GPS_SPEED" to R.string.ui_field_gps_speed,
        "GPS_DISTANCE" to R.string.ui_field_gps_distance,
        "GPS_MOVING_AVERAGE" to R.string.ui_field_gps_moving_average,
        "GPS_OVERALL_AVERAGE" to R.string.ui_field_gps_overall_average,
        "GPS_MAXIMUM_SPEED" to R.string.ui_field_gps_maximum_speed,
        "GPS_ELAPSED_TIME" to R.string.ui_field_gps_elapsed_time,
        "GPS_MOVING_TIME" to R.string.ui_field_gps_moving_time,
        "GPS_STOPPED_TIME" to R.string.ui_field_gps_stopped_time,
        "GPS_ALTITUDE" to R.string.ui_field_gps_altitude,
        "GPS_ASCENT" to R.string.ui_field_gps_ascent,
        "GPS_DESCENT" to R.string.ui_field_gps_descent,
        "GPS_GRADIENT" to R.string.ui_field_gps_gradient,
        "WEATHER_SUNRISE" to R.string.ui_field_weather_sunrise,
        "WEATHER_SUNSET" to R.string.ui_field_weather_sunset,
        "WEATHER_UNTIL_SUNSET" to R.string.ui_field_weather_until_sunset,
        "WEATHER_DAYLIGHT" to R.string.ui_field_weather_daylight,
        "BIKE_DISTANCE" to R.string.ui_field_bike_distance,
        "HEART_RATE" to R.string.ui_field_heart_rate,
        "HEART_AVERAGE" to R.string.ui_field_heart_average,
        "HEART_MAXIMUM" to R.string.ui_field_heart_maximum,
        "HEART_BATTERY" to R.string.ui_field_heart_battery,
        "CURRENT_RANGE" to R.string.ui_field_current_range,
        "RANGES" to R.string.ui_field_ranges
    )
    private val captions = mapOf(
        "SPEED" to R.string.ui_caption_speed,
        "GPS_SPEED" to R.string.ui_caption_gps_speed,
        "BIKE_DISTANCE" to R.string.ui_caption_bike_distance,
        "GPS_DISTANCE" to R.string.ui_caption_gps_distance,
        "GPS_MOVING_AVERAGE" to R.string.ui_caption_gps_moving_average,
        "GPS_OVERALL_AVERAGE" to R.string.ui_caption_gps_overall_average,
        "GPS_MAXIMUM_SPEED" to R.string.ui_caption_gps_maximum_speed,
        "GPS_ELAPSED_TIME" to R.string.ui_caption_gps_elapsed_time,
        "GPS_MOVING_TIME" to R.string.ui_caption_gps_moving_time,
        "GPS_STOPPED_TIME" to R.string.ui_caption_gps_stopped_time,
        "GPS_ALTITUDE" to R.string.ui_caption_gps_altitude,
        "GPS_ASCENT" to R.string.ui_caption_gps_ascent,
        "GPS_DESCENT" to R.string.ui_caption_gps_descent,
        "GPS_GRADIENT" to R.string.ui_caption_gps_gradient,
        "ODOMETER" to R.string.ui_caption_odometer,
        "RIDER_POWER" to R.string.ui_caption_rider_power,
        "RIDER_TORQUE" to R.string.ui_caption_rider_torque,
        "MOTOR_TORQUE" to R.string.ui_caption_motor_torque,
        "REMAINING_ENERGY" to R.string.ui_caption_remaining_energy,
        "DELIVERED_ENERGY" to R.string.ui_caption_delivered_energy,
        "RIDER_ENERGY_SHARE" to R.string.ui_caption_rider_energy_share,
        "AVERAGE_SPEED" to R.string.ui_caption_average_speed,
        "AVERAGE_POWER" to R.string.ui_caption_average_power,
        "AVERAGE_CADENCE" to R.string.ui_caption_average_cadence,
        "MAXIMUM_POWER" to R.string.ui_caption_maximum_power,
        "HEART_RATE" to R.string.ui_caption_heart_rate,
        "HEART_AVERAGE" to R.string.ui_caption_heart_average,
        "HEART_MAXIMUM" to R.string.ui_caption_heart_maximum,
        "CURRENT_RANGE" to R.string.ui_caption_current_range,
        "RANGES" to R.string.ui_caption_ranges
    )
    private val recordTitles = mapOf(
        "GPS_POSITION" to R.string.ui_record_field_gps_position,
        "NORMALIZED_POWER" to R.string.ui_record_field_normalized_power,
        "MECHANICAL_WORK" to R.string.ui_record_field_mechanical_work,
        "CALORIES" to R.string.ui_record_field_calories,
        "SPEED_POWER_RATIO" to R.string.ui_record_field_speed_power_ratio,
        "EFFICIENCY_FACTOR" to R.string.ui_record_field_efficiency_factor
    )
    private val sources = mapOf(
        "GPS" to R.string.ui_source_gps,
        "E-Bike" to R.string.ui_source_bike,
        "Pulsmesser" to R.string.ui_source_heart,
        "Wetter" to R.string.ui_source_weather,
        "Leistung" to R.string.ui_source_power,
        "Performance & Energie" to R.string.ui_section_performance_energy
    )
    private val sections = mapOf(
        "Fahrt" to R.string.ui_section_ride,
        "Höhe" to R.string.ui_section_elevation,
        "Leistung & Tritt" to R.string.ui_section_power_cadence,
        "Licht" to R.string.ui_section_light,
        "Akku" to R.string.ui_section_battery,
        "Reichweite" to R.string.ui_section_range,
        "Durchschnitt & Spitzenwerte" to R.string.ui_section_means,
        "Puls & Sensor" to R.string.ui_section_heart_sensor,
        "Sonne & Tageslicht" to R.string.ui_section_daylight,
        "Leistung" to R.string.ui_source_power
    )
    fun title(context: Context, id: String) = titles[id]?.let(context::getString)
    fun caption(context: Context, id: String) = captions[id]?.let(context::getString)
    fun recordTitle(context: Context, id: String) = recordTitles[id]?.let(context::getString)
    fun source(context: Context, key: String) = sources[key]?.let(context::getString) ?: key
    fun section(context: Context, key: String) = sections[key]?.let(context::getString) ?: key
}
