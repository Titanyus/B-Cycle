package de.bcycle.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HrvContextColorsTest {
    private val day=LocalDate.of(2026,10,28)
    @Test fun usesSharedZoneColorsAndNeverConfusesMissingAnswersWithTheMiddle() {
        assertEquals(TrainingZoneColors.grey,HrvContextColors.color(HrvContextMode.SLEEP,null))
        val colors=listOf(0.0,2.0,4.0,6.0,8.0,10.0).map { HrvContextColors.color(HrvContextMode.SLEEP,it) }
        assertEquals(TrainingZoneColors.powerValues.drop(2)+TrainingZoneColors.purple,colors)
        assertEquals(listOf(TrainingZoneColors.grey,TrainingZoneColors.green,TrainingZoneColors.orange,TrainingZoneColors.red),
            (0..3).map { HrvContextColors.color(HrvContextMode.TRAINING,it.toDouble()) })
    }
    @Test fun interpretsGoodAndBadDirectionsSeparatelyAndAssignsTrainingToItsActualDay() {
        val q=HrvQuestionnaire(day.toString(),ratings=mapOf("sleep" to 10,"energy" to 0,"soreness" to 8,"motivation" to 10,"fatigue" to 2),training=3)
        val values=HrvContextColors.daily(mapOf(day to q))
        assertEquals(0.0,values.getValue(day).getValue(HrvContextMode.SLEEP),0.0)
        assertEquals(10.0,values.getValue(day).getValue(HrvContextMode.ENERGY),0.0)
        assertEquals(8.0,values.getValue(day).getValue(HrvContextMode.SORENESS),0.0)
        assertEquals(4.0,values.getValue(day).getValue(HrvContextMode.WELLBEING),0.0)
        assertFalse(values.getValue(day).containsKey(HrvContextMode.TRAINING))
        assertEquals(3.0,values.getValue(day.minusDays(1)).getValue(HrvContextMode.TRAINING),0.0)
    }
    @Test fun aggregatesAvailableAnswersAcrossRealCalendarPeriodsIncludingDaysWithoutMeasurements() {
        val daily=HrvContextColors.daily(mapOf(day to HrvQuestionnaire(day.toString(),ratings=mapOf("sleep" to 10)),
            day.plusDays(1) to HrvQuestionnaire(day.plusDays(1).toString(),ratings=mapOf("sleep" to 0,"energy" to 10)),
            day.plusDays(2) to HrvQuestionnaire(day.plusDays(2).toString())))
        for(period in listOf(HrvTrendPeriod.WEEK,HrvTrendPeriod.MONTH)) {
            val values=HrvContextColors.aggregate(daily,period).getValue(period.start(day))
            assertEquals(5.0,values.getValue(HrvContextMode.SLEEP),0.0)
            assertEquals(0.0,values.getValue(HrvContextMode.ENERGY),0.0)
            assertFalse(values.containsKey(HrvContextMode.SORENESS))
        }
    }
}
