package de.bcycle.app

import android.content.Context
import com.garmin.fit.*
import java.io.File
import java.io.IOException
import kotlin.math.*

/** Two passes preserve all unrelated FIT messages/fields. No target is changed before validation. */
internal object FitElevationCorrection {
    const val preference="mapElevationCorrection"
    fun enabled(context:Context)=context.getSharedPreferences("bike",0).getBoolean(preference,false)
    private data class Point(val time:Double?,val lat:Double?,val lon:Double?,val speed:Double?,val running:Boolean,
        var height:Double?=null,var up:Double=0.0,var down:Double=0.0,var grade:Double?=null)
    fun encode(original:File,output:File,source:ElevationSource,cancelled:()->Boolean={false}) {
        if(cancelled() || Thread.currentThread().isInterrupted) throw IOException("Cancelled")
        require(original.canonicalFile!=output.canonicalFile)
        require(original.length() in 14..FitRead.MAX_BYTES)
        require(original.inputStream().buffered().use { Decode().checkFileIntegrity(it) })
        val points=ArrayList<Point>()
        var running=true
        fun number(m:Mesg,name:String)=m.getFieldDoubleValue(name)?.takeIf { it.isFinite() }
        fun checkStop() { if(cancelled() || Thread.currentThread().isInterrupted) throw IOException("Cancelled") }
        require(original.inputStream().buffered().use { Decode().read(it,MesgListener { m ->
            checkStop()
            if(m.num==21 && number(m,"event")==0.0) running=number(m,"event_type")==0.0
            if(m.num==20) {
                require(points.size<200000)
                val lat=number(m,"position_lat")?.times(180.0/2147483648.0)?.takeIf { it in -85.0..85.0 }
                val lon=number(m,"position_long")?.times(180.0/2147483648.0)?.takeIf { it in -180.0..180.0 }
                points+=Point(number(m,"timestamp"),lat,lon,number(m,"enhanced_speed")?:number(m,"speed"),running)
            }
        }) })
        require(points.count { it.lat!=null && it.lon!=null }>=2) { "No GPS positions" }
        var accepted:Point?=null
        for(p in points) {
            checkStop()
            if(p.lat==null || p.lon==null) continue
            val previous=accepted
            val dt=if(p.time!=null && previous?.time!=null) p.time-previous.time else null
            if(dt!=null && dt<0) continue
            if(previous?.lat!=null && previous.lon!=null && dt!=null && dt<=10 &&
                GpsRide.metres(GpsFix(0,previous.lat,previous.lon,0.0),GpsFix(0,p.lat,p.lon,0.0))>35*dt+50) continue
            p.height=(round(source.height(p.lat,p.lon)*5)/5).also { require(it.isFinite() && it in -500.0..10000.0) }
            accepted=p
        }
        val gain=RouteElevation();var previous:Point?=null
        val slope=ArrayDeque<Pair<Double,Double>>();var distance=0.0
        for(p in points) {
            checkStop()
            val prior=previous
            val gap=prior==null || p.time==null || prior.time==null || p.time-prior.time !in 0.0..10.0 || prior.height==null
            val moving=p.running && (p.speed==null || p.speed>=.7)
            gain.add(p.height,start=gap || !moving)
            p.up=gain.ascent?:0.0;p.down=gain.descent?:0.0
            if(gap || !moving || p.height==null) slope.clear()
            if(p.height!=null) {
                if(!gap && prior?.lat!=null && prior.lon!=null && p.lat!=null && p.lon!=null)
                    distance+=GpsRide.metres(GpsFix(0,prior.lat,prior.lon,0.0),GpsFix(0,p.lat,p.lon,0.0))
                slope.addLast(distance to p.height!!)
                while(slope.size>2 && slope.elementAt(1).first<=distance-40) slope.removeFirst()
                while(slope.size>100) slope.removeFirst()
                val start=slope.first()
                if(moving && distance-start.first>=30) p.grade=((p.height!!-start.second)/(distance-start.first)*100).takeIf { abs(it)<=50 }
            }
            previous=p
        }
        var index=0
        val encoder=FileEncoder(output,Fit.ProtocolVersion.V2_0)
        try {
            require(original.inputStream().buffered().use { Decode().read(it,MesgListener { m ->
                checkStop()
                if(m.num==20) {
                    val p=points[index++]
                    for(name in listOf("altitude","enhanced_altitude","grade")) {
                        if(p.height==null || (name=="grade" && p.grade==null)) m.getField(name)?.let { m.removeField(it) }
                    }
                    p.height?.let { m.setFieldValue("altitude",it);m.setFieldValue("enhanced_altitude",it) }
                    if(p.grade!=null && m.getField("grade")!=null) m.setFieldValue("grade",p.grade)
                    for(field in m.developerFields) when(field.name) {
                        "GPS_ALTITUDE" -> field.setValue(p.height)
                        "GPS_ALTITUDE_MSL" -> field.setValue(if(p.height!=null) 1.0 else null)
                        "GPS_ASCENT" -> field.setValue(p.up)
                        "GPS_DESCENT" -> field.setValue(p.down)
                        "GPS_GRADIENT" -> field.setValue(p.grade)
                    }
                } else if(m.num==18 || m.num==19) {
                    val start=number(m,"start_time");val end=number(m,"timestamp")
                    val range=points.filter { p -> p.time!=null && (start==null || p.time>=start) && (end==null || p.time<=end) }
                    val first=range.firstOrNull();val last=range.lastOrNull()
                    if(first!=null && last!=null) {
                        m.setFieldValue("total_ascent",(last.up-first.up).coerceAtLeast(0.0).roundToInt().coerceAtMost(65534))
                        m.setFieldValue("total_descent",(last.down-first.down).coerceAtLeast(0.0).roundToInt().coerceAtMost(65534))
                        val heights=range.mapNotNull { it.height }
                        if(heights.isNotEmpty()) for((name,value) in listOf("avg_altitude" to heights.average(),"min_altitude" to heights.min(),"max_altitude" to heights.max(),
                            "enhanced_avg_altitude" to heights.average(),"enhanced_min_altitude" to heights.min(),"enhanced_max_altitude" to heights.max())) {
                            if(m.getField(name)!=null) m.setFieldValue(name,value)
                        }
                    }
                }
                encoder.write(m)
                if(m.num==18) {
                    val memo=MemoGlobMesg().apply {
                        partIndex=0;mesgNum=18;parentIndex=number(m,"message_index")?.toInt()?:0
                        "Elevation corrected with Mapterhorn; source attribution and licences: https://mapterhorn.com/attribution".toByteArray(Charsets.UTF_8).forEachIndexed { i,b -> setMemo(i,b) }
                    }
                    encoder.write(memo)
                }
            }) })
        } finally { encoder.close() }
        checkStop()
        require(index==points.size && output.inputStream().buffered().use { Decode().checkFileIntegrity(it) })
        // Decode again before committing; CRC alone does not validate the message stream.
        require(output.inputStream().buffered().use { Decode().read(it,MesgListener { }) })
    }
    fun correct(context:Context,original:File,target:File,cancelled:()->Boolean={false},source:ElevationSource?=null) {
        val temporary=File.createTempFile("height-", ".fit",context.cacheDir)
        try {
            if(source!=null) encode(original,temporary,source,cancelled)
            else MapterhornElevations(context,cancelled).use { encode(original,temporary,it,cancelled) }
            if(cancelled()) throw IOException("Cancelled")
            target.parentFile!!.mkdirs()
            val staged=File.createTempFile("height-", ".staged",target.parentFile)
            try {
                staged.outputStream().use { out -> temporary.inputStream().use { it.copyTo(out) };out.fd.sync() }
                if(cancelled()) throw IOException("Cancelled")
                java.nio.file.Files.move(staged.toPath(),target.toPath(),java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            } finally { staged.delete() }

        } finally { temporary.delete() }
    }
}
