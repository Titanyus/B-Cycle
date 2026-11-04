package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.widget.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger

/** Last statistics section; cached data first, worker download, no network on draw/drag frames. */
internal class IntervalsFitnessView(context:Context,private val themes:ThemeManager,
    private val repository:IntervalsFitnessRepository=IntervalsFitnessRepository(context.applicationContext),
    private val now:()->Long=System::currentTimeMillis):LinearLayout(context) {
    private val chart=IntervalsFitnessChart(context,themes)
    private val status=BicycleFeedback.status(context,themes).apply { tag="intervals_fitness_status" }
    private val stamp=BicycleComponents.description(context,themes,"")
    private val date=BicycleComponents.text(context,themes,"").apply { tag="intervals_fitness_date";gravity=Gravity.CENTER }
    private val metrics=ArrayList<TextView>()
    private val generation=AtomicInteger()
    private var worker:ExecutorService?=null
    private var force=false
    private val pending=Runnable { load(force) }
    private val number=java.text.NumberFormat.getNumberInstance().apply { maximumFractionDigits=1 }
    private val dayFormat=DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy")
    private val timeFormat=DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val refresh=BicycleComponents.action(context,themes,context.getString(R.string.intervals_fitness_refresh)) { request(true) }.apply { tag="intervals_fitness_refresh" }
    init {
        orientation=VERTICAL;tag="statistics_fitness"
        addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.intervals_fitness_title)))
        val periods=RadioGroup(context).apply { orientation=HORIZONTAL;tag="intervals_fitness_periods" }
        BicycleComponents.segmentedControlRow(periods,themes)
        IntervalsFitnessPeriod.entries.forEach { period -> periods.addView(BicycleSegmentButton(context,themes).apply {
            id=generateViewId();text=context.getString(period.label);tag="fitness_period_${period.name}";isChecked=period==IntervalsFitnessPeriod.WEEK
            setOnCheckedChangeListener { _,checked -> if(checked) chart.period(period) }
        },RadioGroup.LayoutParams(0,-2,1f)) }
        addView(periods,LayoutParams(-1,-2));addView(date,LayoutParams(-1,-2))
        val values=LinearLayout(context).apply { orientation=HORIZONTAL;isBaselineAligned=false;tag=BicycleDesign.inlineControlRowTag }
        listOf(R.string.intervals_fitness_fitness to FitnessChartColors.fitness,R.string.intervals_fitness_fatigue to FitnessChartColors.fatigue,R.string.intervals_fitness_form to FitnessChartColors.form).forEach { (title,color) ->
            val column=LinearLayout(context).apply { orientation=VERTICAL;gravity=Gravity.CENTER }
            column.addView(BicycleComponents.text(context,themes,context.getString(title)).apply { textSize=BicycleDesign.analysisLabelSp;gravity=Gravity.CENTER })
            val metric=BicycleComponents.text(context,themes,"--").apply {
                tag="fitness_value_${metrics.size}";gravity=Gravity.CENTER;typeface=BicycleDesign.narrowTypeface(context);textSize=BicycleDesign.settingsMetricSp
                BicycleComponents.bindTheme(this,themes) { setTextColor(color) }
            }
            metrics.add(metric);column.addView(metric);values.addView(column,LayoutParams(0,-2,1f))
        }
        addView(values,LayoutParams(-1,-2));addView(chart,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.statisticsChartHeight+BicycleDesign.analysisChartHeight)))
        chart.selected={ row ->
            date.text=dayFormat.format(row.day)
            listOf(row.fitness,row.fatigue,row.form).forEachIndexed { i,value -> metrics[i].text=value?.let(number::format) ?: "--" }
        }
        chart.rangeChanged={ request(false) }
        addView(BicycleComponents.description(context,themes,context.getString(R.string.intervals_fitness_hint)))
        val actions=LinearLayout(context).apply { orientation=HORIZONTAL;tag=BicycleDesign.inlineControlRowTag }
        actions.addView(BicycleComponents.action(context,themes,context.getString(R.string.intervals_fitness_today)) { chart.goToday() },LayoutParams(0,-2,1f))
        actions.addView(refresh,LayoutParams(0,-2,1f));addView(actions,LayoutParams(-1,-2))
        addView(status);addView(stamp)
        chart.goToday()
    }
    private fun request(fresh:Boolean) {
        force=fresh;generation.incrementAndGet();removeCallbacks(pending)
        if(isAttachedToWindow) postDelayed(pending,if(fresh) 0 else 250)
    }
    private fun load(fresh:Boolean) {
        val executor=worker ?: return
        val ticket=generation.incrementAndGet()
        val visibleStart=chart.oldest();val visibleEnd=chart.newest()
        val span=java.time.temporal.ChronoUnit.DAYS.between(visibleStart,visibleEnd)+1
        // One viewport on either side avoids another download on every short swipe.
        val from=visibleStart.minusDays(span);val to=minOf(LocalDate.now(),visibleEnd.plusDays(span))
        val settings=IntervalsSettings(context.applicationContext);val connection=settings.connectionToken()
        val config=settings.read()
        if(!config.enabled || !settings.hasKey() || config.athlete.isBlank()) {
            BicycleFeedback.update(status,context.getString(R.string.intervals_fitness_connect));refresh.isEnabled=false;return
        }
        refresh.isEnabled=false;BicycleFeedback.update(status,context.getString(R.string.intervals_fitness_loading))
        fun active()=ticket==generation.get() && connection==settings.connectionToken()
        executor.execute {
            val cached=repository.cached(from,to)
            if(cached!=null) post { if(active() && isAttachedToWindow) show(cached) }
            val result=runCatching {
                if(!fresh && cached!=null && now()-cached.fetchedAtMillis in 0 until 6*60*60*1000L) cached
                else repository.refresh(from,to) { !active() }
            }
            post {
                if(active() && isAttachedToWindow) {
                    refresh.isEnabled=true
                    result.onSuccess { history -> show(history);BicycleFeedback.update(status,if(history.hasValues) "" else context.getString(R.string.intervals_fitness_empty)) }
                        .onFailure { error -> BicycleFeedback.update(status,context.getString(ServiceProblems.text(error,R.string.intervals_fitness_read_failed))) }
                } else if(ticket==generation.get() && isAttachedToWindow) {
                    refresh.isEnabled=true;BicycleFeedback.update(status,context.getString(R.string.intervals_bulk_changed))
                }
            }
        }
    }
    private fun show(history:IntervalsFitnessHistory) {
        chart.show(history)
        stamp.text=context.getString(R.string.intervals_fitness_cached,timeFormat.format(Instant.ofEpochMilli(history.fetchedAtMillis).atZone(ZoneId.systemDefault())))
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow();worker=Executors.newSingleThreadExecutor();request(false) }
    override fun onDetachedFromWindow() {
        generation.incrementAndGet();removeCallbacks(pending);worker?.shutdownNow();worker=null;super.onDetachedFromWindow()
    }
}
