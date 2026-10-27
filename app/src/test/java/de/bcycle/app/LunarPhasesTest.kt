package de.bcycle.app

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class LunarPhasesTest {
    @Test fun principalPhasesMatchApril2024AlmanacWithinFiveMinutes() {
        val events=LunarPhases.between(LocalDate.of(2024,4,1),LocalDate.of(2024,4,30),ZoneOffset.UTC)
        val expected=mapOf(LunarPhase.LAST_QUARTER to "2024-04-02T03:15:00Z",LunarPhase.NEW to "2024-04-08T18:21:00Z",
            LunarPhase.FIRST_QUARTER to "2024-04-15T19:13:00Z",LunarPhase.FULL to "2024-04-23T23:49:00Z")
        assertEquals(4,events.size)
        for(event in events) assertTrue("${event.phase}: ${event.instant}",kotlin.math.abs(Duration.between(Instant.parse(expected.getValue(event.phase)),event.instant).seconds)<300)
    }
    @Test fun localCalendarDaysRespectTimezoneAndThereAreNoDuplicateEventsAtYearBoundaries() {
        val berlin=ZoneId.of("Europe/Berlin")
        val local=LunarPhases.between(LocalDate.of(2024,4,24),LocalDate.of(2024,4,24),berlin)
        assertEquals(listOf(LunarPhase.FULL),local.map { it.phase })
        assertTrue(LunarPhases.between(LocalDate.of(2024,4,24),LocalDate.of(2024,4,24),ZoneOffset.UTC).isEmpty())
        val first=LocalDate.of(2025,12,1);val last=LocalDate.of(2026,2,1)
        val events=LunarPhases.between(first,last,berlin)
        assertEquals(events.size,events.distinctBy { it.instant }.size)
        assertTrue(events.zipWithNext().all { (a,b) -> Duration.between(a.instant,b.instant).toHours() in 140..205 })
        assertTrue(events.all { it.day(berlin) in first..last })
        assertTrue(LunarPhases.between(last,first,berlin).isEmpty())
    }
}
