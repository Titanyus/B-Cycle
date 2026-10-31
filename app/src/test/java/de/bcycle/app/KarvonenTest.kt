package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test

class KarvonenTest {
    @Test fun reserveAndKnownTargetsAndInclusiveDisplayRanges() {
        val result = Karvonen.calculate(55, 185)!!
        assertEquals(130, result.reserve)
        assertEquals(146.0, Karvonen.targetBpm(55, 185, .7)!!, 0.0)
        assertEquals(listOf(1 to 132, 133 to 145, 146 to 158, 159 to 171, 172 to 185), result.zones.map { it.lowerBpm to it.upperBpm })
        assertEquals(2, result.zoneFor(133)!!.index)
        assertEquals(5, result.zoneFor(185)!!.index)
        assertEquals(1,result.zoneFor(119)!!.index); assertNull(result.zoneFor(0)); assertNull(result.zoneFor(186))
        assertEquals("karvonen-hrr-v1", result.modelId)
    }
    @Test fun fractionalBoundariesRoundUpAndMeasuredBpmHaveExactlyOneZone() {
        assertEquals(listOf(1, 135, 147, 159, 171), Karvonen.calculate(61, 183)!!.zones.map { it.lowerBpm })
        for (rest in 20..140 step 7) for (max in 60..250 step 11) if (max > rest) {
            val result = Karvonen.calculate(rest, max)!!
            assertEquals(5, result.zones.size)
            for (bpm in 0..260) {
                val matching = result.zones.count { bpm in it }
                assertEquals(if (bpm in result.zones.first().lowerBpm..max) 1 else 0, matching)
                assertEquals(matching == 1, result.zoneFor(bpm) != null)
            }
            result.zones.zipWithNext().forEach { (a,b) -> assertEquals(a.upperExclusiveBpm,b.lowerBpm) }
        }
        val narrow = Karvonen.calculate(140, 141)!!
        assertTrue(narrow.zones.drop(1).take(3).all { it.isEmpty }); assertEquals(5,narrow.zoneFor(141)!!.index)
    }
    @Test fun missingInvalidAndReversedInputsNeverProduceZones() {
        listOf(null to 185, 55 to null, 0 to 185, -1 to 185, 55 to 0, 140 to 140, 140 to 120, 55 to 251).forEach {
            assertNull(Karvonen.calculate(it.first,it.second))
        }
        assertNull(Karvonen.targetBpm(55,185,Double.NaN)); assertNull(Karvonen.targetBpm(55,185,1.01))
    }
}
