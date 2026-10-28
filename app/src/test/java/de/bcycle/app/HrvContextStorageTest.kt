package de.bcycle.app

import android.app.Activity
import android.os.Looper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class HrvContextStorageTest {
    @Test fun controllerLoadsDailyAnswersEvenWithoutMeasurementAndKeepsHrvValuesUnchanged() {
        val host=Robolectric.buildActivity(Activity::class.java).setup();val a=host.get();val controller=HrvController(a)
        fun drain() {
            (HrvController::class.java.getDeclaredField("worker").apply { isAccessible=true }.get(controller) as ExecutorService).submit {}.get(10,TimeUnit.SECONDS)
            shadowOf(Looper.getMainLooper()).idle()
        }
        try {
            drain();val store=HrvStore(File(a.filesDir,"hrv-measurements"));val day=LocalDate.of(2026,10,27)
            val entry=Hrv4TrainingCsv.preview("date,time,rMSSD\n2026-10-27,07:15,40\n".byteInputStream(),ZoneId.of("Europe/Berlin")).entries.single()
            store.importHrv4Training(entry);store.saveQuestionnaire(entry.result.getString("id"),HrvQuestionnaire(day.toString(),ratings=mapOf("sleep" to 0)))
            val tomorrow=HrvQuestionnaire(day.plusDays(1).toString(),ratings=mapOf("sleep" to 10),training=3)
            store.restore(JSONObject().put("format","bicycle-hrv-day-backup").put("schema",1).put("questionnaire",tomorrow.json()).toString().toByteArray())
            var data:HrvTrendPresentation?=null;controller.trendsWithContext { data=it.getOrThrow() };drain()
            assertEquals(40.0,data!!.series.getValue(HrvTrendMetric.RMSSD).single().value!!,0.0)
            assertEquals(setOf(day),data!!.series.getValue(HrvTrendMetric.RMSSD).map { it.day }.toSet())
            assertEquals(TrainingZoneColors.red,HrvContextColors.color(HrvContextMode.TRAINING,data!!.context.getValue(day)[HrvContextMode.TRAINING]))
            assertEquals(TrainingZoneColors.purple,HrvContextColors.color(HrvContextMode.SLEEP,data!!.context.getValue(day)[HrvContextMode.SLEEP]))
            assertEquals(TrainingZoneColors.green,HrvContextColors.color(HrvContextMode.SLEEP,data!!.context.getValue(day.plusDays(1))[HrvContextMode.SLEEP]))
        } finally { controller.close();host.pause().stop().destroy() }
    }
}
