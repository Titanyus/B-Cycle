package de.bcycle.app

import java.util.Locale

/** Display formatting only; measurement and recording code never parses these strings. */
fun LiveMetric.format(raw: Long): String {
    if (this == LiveMetric.BIKE_LIGHT) return when (raw) { 0L -> "Aus"; 1L -> "An"; else -> "—" }
    val value = if (this == LiveMetric.BATTERY_TEMPERATURE) (raw ushr 1) xor -(raw and 1) else raw
    return if (divisor == 1.0) "$value $unit".trim() else
        String.format(Locale.GERMANY, "%.1f %s", value / divisor, unit)
}

fun LiveData.display(metric: LiveMetric): String {
    val raw = value(metric) ?: return "—"
    if (metric == LiveMetric.ASSIST_MODE) return assistNames?.getOrNull(raw.toInt()) ?: "Stufe $raw"
    return metric.format(raw)
}

fun LiveData.displayRanges(): String {
    val ranges = rangesKm ?: return "—"
    if (ranges.isEmpty()) return "—"
    val names = assistNames
    val offset = if (names?.size == ranges.size + 1 && names.first().equals("OFF", true)) 1 else 0
    return ranges.mapIndexed { index, km ->
        val name = if (names?.size == ranges.size + offset) names[index + offset] else "Stufe ${index + 1}"
        "$name: $km km"
    }.joinToString(" · ")
}

