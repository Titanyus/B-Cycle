package de.bcycle.app

import android.content.Context
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Blocking worker API for statistics. No polling/job, live account probe or upload side effect. */
internal class IntervalsFitnessRepository(private val context:Context,
    private val settings:IntervalsSettings=IntervalsSettings(context),
    private val store:IntervalsFitnessStore=IntervalsFitnessStore(File(context.cacheDir,"intervals-fitness")),
    private val network:(IntervalsConfig)->Boolean={ IntervalsHrvUploads.network(context,it) },
    private val api:(IntervalsConfig,String,()->Boolean)->IntervalsClient={ config,key,stop -> IntervalsClient(config,key,stop) },
    private val now:()->Long=System::currentTimeMillis) {
    fun cached(period:IntervalsFitnessPeriod,newest:LocalDate=LocalDate.now())=cached(period.oldest(newest),newest)
    fun cached(oldest:LocalDate,newest:LocalDate):IntervalsFitnessHistory? {
        val token=settings.connectionToken();val config=settings.read()
        if(!config.enabled || config.athlete.isBlank()) return null
        val cached=store.cached(config.athlete,oldest,newest)
        return cached.takeIf { settings.connectionToken()==token }
    }
    fun refresh(period:IntervalsFitnessPeriod,newest:LocalDate=LocalDate.now(),cancelled:()->Boolean={ false })=
        refresh(period.oldest(newest),newest,cancelled)
    fun refresh(oldest:LocalDate,newest:LocalDate,cancelled:()->Boolean={ false }):IntervalsFitnessHistory {
        require(newest>=oldest && ChronoUnit.DAYS.between(oldest,newest)<366)
        val config=settings.read();val token=settings.connectionToken()
        if(!config.enabled || config.athlete.isBlank() || !settings.hasKey()) throw IntervalsFailure(false,R.string.intervals_enable_first)
        fun checkActive() {
            if(cancelled()) throw IOException("Cancelled fitness read")
            if(settings.connectionToken()!=token || settings.read()!=config) throw IntervalsFailure(false,R.string.intervals_bulk_changed)
            if(!network(config)) throw IntervalsFailure(true,R.string.intervals_bulk_wifi)
        }
        checkActive()
        val client=api(config,settings.apiKey()) { cancelled() || settings.connectionToken()!=token || !network(config) }
        val result=ArrayList<IntervalsFitnessDay>();var start=oldest
        while(start<=newest) {
            checkActive()
            val end=minOf(start.plusDays(30),newest)
            val rows=client.fitness(start,end).associateBy { it.day }
            var day=start
            // Missing API records stay gaps on a continuous calendar axis, never made-up zeros.
            while(day<=end) { result.add(rows[day] ?: IntervalsFitnessDay(day,null,null));day=day.plusDays(1) }
            start=end.plusDays(1)
        }
        checkActive()
        val history=IntervalsFitnessHistory(oldest,newest,result,now())
        store.save(config.athlete,history)
        checkActive()
        return history
    }
}
