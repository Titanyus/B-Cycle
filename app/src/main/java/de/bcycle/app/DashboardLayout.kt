package de.bcycle.app

import java.util.Locale

enum class GpsMetric(val title: String, val unit: String, val category: String) {
    GPS_SPEED("Geschwindigkeit", "km/h", "Fahrt"),
    GPS_DISTANCE("Gefahrene Distanz", "km", "Fahrt"),
    GPS_MOVING_AVERAGE("Ø Geschwindigkeit · Bewegung", "km/h", "Fahrt"),
    GPS_OVERALL_AVERAGE("Ø Geschwindigkeit · gesamt", "km/h", "Fahrt"),
    GPS_MAXIMUM_SPEED("Max. Geschwindigkeit", "km/h", "Fahrt"),
    GPS_ELAPSED_TIME("Fahrtdauer", "h:mm:ss", "Fahrt"),
    GPS_MOVING_TIME("Bewegungszeit", "h:mm:ss", "Fahrt"),
    GPS_STOPPED_TIME("Standzeit", "h:mm:ss", "Fahrt"),
    GPS_ALTITUDE("Höhe", "m", "Höhe"),
    GPS_ASCENT("Höhenzugewinn", "m", "Höhe"),
    GPS_DESCENT("Höhenverlust", "m", "Höhe"),
    GPS_GRADIENT("Steigung", "%", "Höhe");

    fun value(data: GpsValues): DashboardValue {
        val raw: Double? = when (this) {
            GPS_SPEED -> data.speedKmh
            GPS_DISTANCE -> data.distanceM?.div(1000)
            GPS_MOVING_AVERAGE -> data.movingAverageKmh
            GPS_OVERALL_AVERAGE -> data.overallAverageKmh
            GPS_MAXIMUM_SPEED -> data.maximumKmh
            GPS_ELAPSED_TIME -> data.elapsedMs?.toDouble()
            GPS_MOVING_TIME -> data.movingMs?.toDouble()
            GPS_STOPPED_TIME -> data.stoppedMs?.toDouble()
            GPS_ALTITUDE -> data.altitudeM
            GPS_ASCENT -> data.ascentM
            GPS_DESCENT -> data.descentM
            GPS_GRADIENT -> data.gradientPercent
        }
        val text = when {
            raw == null -> "—"
            unit == "h:mm:ss" -> {
                val seconds = raw.toLong() / 1000
                String.format(Locale.GERMANY, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
            }
            this == GPS_DISTANCE -> String.format(Locale.GERMANY, "%.2f", raw)
            unit == "m" -> String.format(Locale.GERMANY, "%.0f", raw)
            else -> String.format(Locale.GERMANY, "%.1f", raw)
        }
        return DashboardValue(text, unit, raw != null)
    }
}

data class FieldGroup(val source: String, val title: String, val fields: List<DashboardField>)

/** Stable IDs keep field choices independent of enum order and of current reception. */
data class DashboardField(val id: String) {
    private val metric get() = LiveMetric.entries.find { it.name == id }
    private val gpsMetric get() = GpsMetric.entries.find { it.name == id }
    private val rangeIndex get() = id.removePrefix("RANGE_").toIntOrNull()
    val source get() = if (gpsMetric != null) "GPS" else "E-Bike"
    val numeric get() = metric != LiveMetric.ASSIST_MODE && id != "RANGES"

    fun title(data: LiveData, gps: GpsValues = GpsValues()): String = metric?.title ?: gpsMetric?.let {
        if (it == GpsMetric.GPS_ALTITUDE && gps.altitudeM != null) "Höhe (${if (gps.seaLevel) "MSL" else "Ellipsoid"})" else it.title
    } ?: when (id) {
        "BIKE_DISTANCE" -> "Gefahrene Distanz"
        "CURRENT_RANGE" -> "Reichweite aktuell"
        "RANGES" -> "Reichweiten je Modus"
        else -> "Reichweite · ${rangeNames(data)?.getOrNull(rangeIndex ?: -1) ?: "Stufe ${(rangeIndex ?: 0) + 1}"}"
    }

    fun value(data: LiveData, gps: GpsValues = GpsValues(), bikeDistanceM: Long? = null): DashboardValue {
        gpsMetric?.let { return it.value(gps) }
        if (id == "BIKE_DISTANCE") return DashboardValue(bikeDistanceM?.let { String.format(Locale.GERMANY, "%.2f", it / 1000.0) } ?: "—", "km", bikeDistanceM != null)
        metric?.let {
            val raw = data.value(it)
            val formatted = data.display(it)
            return DashboardValue(if (it.unit.isEmpty()) formatted else formatted.removeSuffix(" ${it.unit}"), it.unit, raw != null)
        }
        val ranges = data.rangesKm
        val range = if (id == "CURRENT_RANGE") {
            val mode = data.value(LiveMetric.ASSIST_MODE)?.toInt()
            val names = data.assistNames
            // Do not infer a mode-to-range mapping without aligned bike-supplied names.
            val offset = if (names != null && ranges != null && names.size == ranges.size + 1 && names.first().equals("OFF", true)) 1 else 0
            if (mode != null && ranges != null && rangeNames(data) != null) ranges.getOrNull(mode - offset) else null
        } else rangeIndex?.let { ranges?.getOrNull(it) }
        return if (id == "RANGES") {
            DashboardValue(ranges?.takeIf { it.isNotEmpty() }?.joinToString(" / ") ?: "—", "km je Modus", !ranges.isNullOrEmpty())
        } else DashboardValue(range?.toString() ?: "—", "km", range != null)
    }

    private fun rangeNames(data: LiveData): List<String>? {
        val names = data.assistNames ?: return null
        val ranges = data.rangesKm ?: return null
        return when {
            names.size == ranges.size -> names
            names.size == ranges.size + 1 && names.first().equals("OFF", true) -> names.drop(1)
            else -> null
        }
    }

    companion object {
        val baseOptions = LiveMetric.entries.map { DashboardField(it.name) } +
            listOf("BIKE_DISTANCE", "CURRENT_RANGE", "RANGES").map { DashboardField(it) } +
            GpsMetric.entries.map { DashboardField(it.name) }
        fun options(data: LiveData) = baseOptions + List(data.rangesKm?.size ?: 0) { DashboardField("RANGE_$it") }
        fun groups(data: LiveData, selected: DashboardField? = null): List<FieldGroup> {
            fun fields(vararg ids: String) = ids.map { DashboardField(it) }
            val ranges = options(data).filter { it.id.startsWith("RANGE_") }.toMutableList().apply {
                if (selected != null && selected.id.startsWith("RANGE_") && selected !in this) add(selected)
            }
            return listOf(
                FieldGroup("GPS", "Fahrt", GpsMetric.entries.filter { it.category == "Fahrt" }.map { DashboardField(it.name) }),
                FieldGroup("GPS", "Höhe", GpsMetric.entries.filter { it.category == "Höhe" }.map { DashboardField(it.name) }),
                FieldGroup("E-Bike", "Fahrt", fields("SPEED", "BIKE_DISTANCE", "ODOMETER", "ASSIST_MODE")),
                FieldGroup("E-Bike", "Leistung & Tritt", fields("RIDER_POWER", "MOTOR_POWER", "CADENCE", "RIDER_TORQUE", "MOTOR_TORQUE")),
                FieldGroup("E-Bike", "Akku", fields("BATTERY", "BATTERY_TEMPERATURE", "REMAINING_ENERGY", "DELIVERED_ENERGY")),
                FieldGroup("E-Bike", "Reichweite", fields("CURRENT_RANGE") + ranges + fields("RANGES")),
                FieldGroup("E-Bike", "Bosch-Aktivität", fields("AVERAGE_SPEED", "AVERAGE_POWER", "MAXIMUM_POWER", "AVERAGE_CADENCE", "MAXIMUM_CADENCE", "RIDER_ENERGY_SHARE")),
            )
        }
        fun fromId(id: String): DashboardField? = baseOptions.find { it.id == id } ?: id.takeIf {
            it.startsWith("RANGE_") && it.removePrefix("RANGE_").toIntOrNull() in 0..15
        }?.let { DashboardField(it) }
    }
}

data class DashboardValue(val text: String, val unit: String, val available: Boolean)

class DashboardLayout private constructor(val fields: List<DashboardField>) {
    fun replace(index: Int, field: DashboardField): DashboardLayout {
        require(index in 0..6 && DashboardField.fromId(field.id) != null)
        return DashboardLayout(fields.mapIndexed { position, previous -> if (position == index) field else previous })
    }
    fun encode() = fields.joinToString("|") { it.id }

    companion object {
        val defaults get() = DashboardLayout(listOf("RIDER_POWER", "CADENCE", "SPEED", "BATTERY", "ASSIST_MODE", "ODOMETER", "MOTOR_POWER").map { DashboardField(it) })
        fun decode(encoded: String?): DashboardLayout {
            val saved = encoded?.split('|').orEmpty()
            return DashboardLayout(defaults.fields.mapIndexed { index, fallback ->
                saved.getOrNull(index)?.let { DashboardField.fromId(it) } ?: fallback
            })
        }
    }
}
