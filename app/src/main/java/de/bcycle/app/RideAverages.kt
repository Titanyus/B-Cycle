package de.bcycle.app

/** Positive cadence samples only; sensor records (including zero) remain untouched. */
internal class PedalingAverage {
    private var count = 0L
    var value: Double? = null; private set
    fun add(rpm: Double) {
        if (!rpm.isFinite() || rpm <= 0) return
        count++
        value = (value ?: rpm) + (rpm - (value ?: rpm)) / count
    }
    fun reset() { count = 0; value = null }
}

/** Display-only, time-weighted current values compared with the existing ride averages. */
internal class AverageTrends {
    companion object {
        private val aliases=mapOf("SPEED" to "AVERAGE_SPEED","GPS_SPEED" to "GPS_MOVING_AVERAGE",
            "RIDER_POWER" to "AVERAGE_POWER","CADENCE" to "AVERAGE_CADENCE","HEART_RATE" to "HEART_AVERAGE")
        private val averages=setOf("AVERAGE_SPEED","GPS_MOVING_AVERAGE","GPS_OVERALL_AVERAGE","AVERAGE_POWER","AVERAGE_CADENCE","HEART_AVERAGE")
        fun supports(field:DashboardField?)=field?.id in aliases || field?.id in averages
        internal fun level(id:String,deviation:Double,previous:Int=0):Int {
            if(!deviation.isFinite()) return 0
            val key=aliases[id] ?: id
            val limits=when(key) {
                "AVERAGE_POWER"->doubleArrayOf(2.0,20.0,40.0,1.0)
                "HEART_AVERAGE"->doubleArrayOf(1.0,3.0,7.0,.25)
                in averages->doubleArrayOf(1.0,5.0,15.0,.5)
                else->return 0
            }
            val magnitude=kotlin.math.abs(deviation)
            val sign=if(deviation>=0) 1 else -1
            val raw=when { magnitude<=limits[0]+1e-9->0;magnitude<=limits[1]+1e-9->1;magnitude<=limits[2]+1e-9->2;else->3 }
            if(previous==0 || previous*sign<0) return raw*sign
            var n=kotlin.math.abs(previous)
            while(n<raw && magnitude>limits[n]+limits[3]+1e-9) n++
            while(n>raw && magnitude<limits[n-1]-limits[3]-1e-9) n--
            return n*sign
        }
    }
    private class Channel {
        var window=TrainingDisplayWindow()
        var start:Long?=null;var last:Long?=null;var level=0
    }
    private val channels=mutableMapOf<String,Channel>()
    fun clear() { channels.clear() }
    fun indicator(field:DashboardField,data:LiveData,gps:GpsValues,pulse:PulseValues,now:Long):Int {
        val key=aliases[field.id] ?: field.id
        if(key !in averages) return 0
        val current=when(key) {
            "AVERAGE_POWER"->data.riderPowerWatts?.toDouble()
            "AVERAGE_CADENCE"->data.value(LiveMetric.CADENCE)?.div(LiveMetric.CADENCE.divisor)
            "AVERAGE_SPEED"->data.speedHundredths?.div(100.0)
            "HEART_AVERAGE"->pulse.bpm?.toDouble()?.takeIf { it>0 }
            else->gps.speedKmh
        }
        val reference=DashboardField(key).averageNumber(data,gps,pulse)
        if(current==null || !current.isFinite() || current<0 || reference==null || !reference.isFinite() || reference<=0) {
            channels.remove(key);return 0
        }
        val channel=channels.getOrPut(key) { Channel() }
        if(channel.last?.let { now<it || now-it>5000 }==true) {
            channel.window=TrainingDisplayWindow();channel.start=null;channel.level=0
        }
        channel.last=now
        if(channel.start==null) channel.start=now
        val mean=channel.window.update(current,now,now) ?: return 0
        if(now-channel.start!!<3000) return 0
        channel.level=level(key,(mean-reference)*100/reference,channel.level)
        return channel.level
    }
}

internal fun DashboardField.averageNumber(data: LiveData, gps: GpsValues, pulse: PulseValues): Double? = when (id) {
    "GPS_MOVING_AVERAGE" -> gps.movingAverageKmh
    "GPS_OVERALL_AVERAGE" -> gps.overallAverageKmh
    "HEART_AVERAGE" -> pulse.average
    "AVERAGE_SPEED" -> data.value(LiveMetric.AVERAGE_SPEED)?.div(LiveMetric.AVERAGE_SPEED.divisor)
    "AVERAGE_POWER" -> data.value(LiveMetric.AVERAGE_POWER)?.toDouble()
    "AVERAGE_CADENCE" -> data.value(LiveMetric.AVERAGE_CADENCE)?.div(LiveMetric.AVERAGE_CADENCE.divisor)
    else -> null
}

/** One stable span; only its cached chevrons redraw while the alpha values animate. */
internal class AverageChevronSpan(private val chevrons:TrendChevrons) : android.text.style.ReplacementSpan() {
    override fun getSize(paint:android.graphics.Paint,text:CharSequence,start:Int,end:Int,fm:android.graphics.Paint.FontMetricsInt?)=(paint.textSize*.45f).toInt()
    override fun draw(canvas:android.graphics.Canvas,text:CharSequence,start:Int,end:Int,x:Float,top:Int,y:Int,bottom:Int,paint:android.graphics.Paint) {
        chevrons.draw(canvas,x,(top+bottom)/2f,paint.textSize*.28f,paint.color)
    }
}

internal fun drawChevron(canvas: android.graphics.Canvas, paint: android.graphics.Paint,
                         x: Float, centerY: Float, size: Float, direction: Int, color: Int) {
    if (direction == 0) return
    paint.color = color; paint.strokeWidth = (size / 6).coerceAtLeast(1f)
    val tip = centerY - direction * size / 4
    val ends = centerY + direction * size / 4
    canvas.drawLine(x, ends, x + size / 2, tip, paint)
    canvas.drawLine(x + size / 2, tip, x + size, ends, paint)
}

/** Same 0.7 m/s movement threshold and 10 s continuity limit as the live GPS estimator. */
internal class MovingTime {
    private var previousTime: Long? = null
    private var previousSpeed: Double? = null
    private var observed = false
    private var total = 0L
    var observedMs=0L;private set
    val value: Long? get() = total.takeIf { observed }
    fun add(time: Long?, speed: Double?) {
        val valid = speed?.takeIf { it.isFinite() && it in 0.0..35.0 }
        val dt = time?.let { now -> previousTime?.let { now - it } }
        if (dt != null && dt in 1..10_000 && valid != null && previousSpeed != null) {
            observed = true;observedMs+=dt
            if (valid >= .7) total += dt
        }
        previousTime = time; previousSpeed = valid
    }
}
