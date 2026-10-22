package de.bcycle.app

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Selection precedes quality filtering: a later failed measurement must not resurrect an older value. */
internal object HrvDailySelection {
    fun day(row:HrvHistorySummary,fallbackZone:ZoneId=ZoneId.systemDefault()):LocalDate =
        row.importedDay?.let(LocalDate::parse) ?: Instant.ofEpochMilli(row.utcMs)
            .atZone(row.zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: fallbackZone).toLocalDate()

    fun latest(rows:List<HrvHistorySummary>,fallbackZone:ZoneId=ZoneId.systemDefault()):List<HrvHistorySummary> {
        // Missing-time imports cannot establish that they occurred after a known-time measurement.
        // A context-only CSV day likewise cannot replace a real measurement.
        val order=compareBy<HrvHistorySummary> { it.hasMeasurement }
            .thenBy { it.importedDay==null || it.importedTime!=null }
            .thenBy { it.utcMs }.thenBy { it.importedDay==null }.thenBy { it.id }
        return rows.groupBy { day(it,fallbackZone) }.values.map { group -> group.maxWith(order) }
            .sortedByDescending { day(it,fallbackZone) }
    }
}
