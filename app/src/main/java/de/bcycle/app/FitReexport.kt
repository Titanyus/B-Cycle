package de.bcycle.app

import android.content.Context
import android.util.AtomicFile
import com.garmin.fit.*
import java.io.File

/** Explicit library-worker action. Originals and complete analysis files are never overwritten. */
internal object FitReexport {
    fun rebuild(c:Context,entry:ManagedRoute):Boolean {
        if(entry.format!="FIT" || !entry.id.startsWith("recorded-") || entry.origin!=TrackOrigin.RECORDED) return false
        val full=RideAnalysisFiles.file(c.filesDir,entry.id)
        if(!full.isFile) return false
        val slot=RideProfileColor.slot(c,entry.id) ?: error(c.getString(R.string.fit_reexport_profile_missing))
        val profile=BikeProfileStore(c,c.getSharedPreferences("bike",0)).all().singleOrNull { it.slot==slot }
            ?: error(c.getString(R.string.fit_reexport_profile_missing))
        val config=(if(profile.type==BikeType.INDOOR) IndoorFields.config(profile.recording) else profile.recording).withGpsPosition(profile.gpsEnabled)
        return install(file(c.filesDir,entry.id)) { temporary ->
            encode(full,temporary,config,FitLibrary.correctedFile(c.filesDir,entry.id).takeIf { it.isFile })
        }
    }
    fun file(root:File,id:String):File {
        require(id.matches(Regex("recorded-[a-f0-9]{64}|imported-[a-f0-9]{64}")))
        return File(root,"fit-library/reexported/$id.fit")
    }
    /** A later elevation correction updates an existing export without changing its field mask. */
    fun refreshElevation(root:File,id:String) {
        val current=file(root,id);val corrected=FitLibrary.correctedFile(root,id)
        if(current.isFile && corrected.isFile) install(current) { encode(current,it,null,corrected) }
    }
    private fun install(target:File,write:(File)->Unit):Boolean {
        target.parentFile!!.mkdirs()
        val temporary=File.createTempFile("reexport-",".fit",target.parentFile)
        try {
            write(temporary)
            require(temporary.inputStream().buffered().use { Decode().checkFileIntegrity(it) })
            FitRead.read(temporary)
            if(target.isFile && WebDavClient.hash(target)==WebDavClient.hash(temporary)) return false
            val atomic=AtomicFile(target);val output=atomic.startWrite()
            try { temporary.inputStream().use { it.copyTo(output) };atomic.finishWrite(output) }
            catch(e:Exception) { atomic.failWrite(output);throw e }
            return true
        } finally { temporary.delete() }
    }
    private val heightFields=setOf("altitude","enhanced_altitude","grade")
    private val heightSummary=setOf("total_ascent","total_descent","avg_altitude","min_altitude","max_altitude","enhanced_avg_altitude","enhanced_min_altitude","enhanced_max_altitude")
    private val heightDeveloper=setOf("GPS_ALTITUDE","GPS_ALTITUDE_MSL","GPS_ASCENT","GPS_DESCENT","GPS_GRADIENT")
    private data class Height(val time:Double?,val values:Map<String,Double?>)
    private fun value(message:Mesg,name:String)=message.getFieldDoubleValue(name)?.takeIf { it.isFinite() }
    /** A null mask keeps every existing field (used solely to overlay a newer correction). */
    internal fun encode(source:File,destination:File,config:RecordingConfig?,corrected:File?=null) {
        require(source.inputStream().buffered().use { Decode().checkFileIntegrity(it) })
        val heights=mutableListOf<Height>();val summary=mutableMapOf<Pair<Int,Double?>,Map<String,Double?>>()
        if(corrected!=null) {
            require(corrected.inputStream().buffered().use { Decode().read(it,MesgListener { m ->
                if(m.num==20) {
                    require(heights.size<200000)
                    val values=heightFields.associateWith { key -> value(m,key) }.toMutableMap()
                    m.developerFields.filter { it.name in heightDeveloper }.forEach { values[it.name]=(it.getValue(0) as? Number)?.toDouble()?.takeIf(Double::isFinite) }
                    heights.add(Height(value(m,"timestamp"),values))
                } else if(m.num in setOf(18,19)) summary[m.num to value(m,"start_time")]=heightSummary.associateWith { name -> value(m,name) }
            }) })
        }
        var index=0
        val encoder=FileEncoder(destination,Fit.ProtocolVersion.V2_0)
        try {
            require(source.inputStream().buffered().use { Decode().read(it,MesgListener { original ->
                val m=if(config==null) Mesg(original) else MaskedMesg(original,config)
                if(config!=null && (m.num==23 && FitMetadataField.RECORDER !in config.metadata || m.num==6 && m.numFields==0)) return@MesgListener
                if(m.num==20 && corrected!=null) {
                    val row=heights.getOrNull(index++) ?: error("Elevation record count changed")
                    require(row.time==value(m,"timestamp")) { "Elevation timestamp changed" }
                    row.values.filterKeys { it in heightFields }.forEach { (name,v) ->
                        m.getField(name)?.let { field -> if(v==null) m.removeField(field) else m.setFieldValue(name,v) }
                    }
                    m.developerFields.filter { it.name in heightDeveloper && row.values.containsKey(it.name) }.forEach { it.setValue(row.values[it.name]) }
                } else if(corrected!=null && m.num in setOf(18,19)) {
                    summary[m.num to value(m,"start_time")]?.forEach { (name,v) ->
                        m.getField(name)?.let { field -> if(v==null) m.removeField(field) else m.setFieldValue(name,v) }
                    }
                }
                encoder.write(m)
            }) })
            if(corrected!=null) require(index==heights.size) { "Elevation record count changed" }
        } finally { encoder.close() }
    }
    private class MaskedMesg(source:Mesg,config:RecordingConfig):Mesg(source.name,source.num) {
        init {
            source.fields.filter { field -> keepStandard(source.num,field,config) }.forEach { addField(Field(it)) }
            source.developerFields.filter { field -> keepDeveloper(field.name,config) }.forEach { addDeveloperField(DeveloperField(it)) }
        }
    }
    private val qualityDeveloper=setOf("POWER_DATA_COVERAGE","POWER_ZERO_STATE_TIME","POWER_MISSING_TIME","POWER_MAX_GAP","PERFORMANCE_MODEL_VERSION")
    private fun keepDeveloper(name:String,config:RecordingConfig):Boolean = when {
        name in config.fields->true
        name=="GPS_ALTITUDE_MSL"->"GPS_ALTITUDE" in config.fields
        name in PrimaryRecording.columns->PrimaryRecording.control(name,config) in config.fields
        name=="ENERGY_GROSS_EFFICIENCY"->"CALORIES" in config.fields
        name in qualityDeveloper->RecordingFields.summaries.any { it.id in config.fields }
        else->false
    }
    private fun keepStandard(message:Int,field:Field,c:RecordingConfig):Boolean {
        if(message==6) {
            val meta=when(field.num) { 0->FitMetadataField.BIKE_NAME;1->FitMetadataField.SPORT;2->FitMetadataField.SUB_SPORT;14->FitMetadataField.PROFILE_ID;254->FitMetadataField.PROFILE_INDEX;else->null }
            return meta==null || meta in c.metadata
        }
        val control=if(message==20) when(field.num) {
            0,1->"GPS_POSITION";2,78->"GPS_ALTITUDE";3->"HEART_RATE";4,53->"CADENCE";5->c.distanceSource
            6,73->c.speedSource;7->"RIDER_POWER";9->"GPS_GRADIENT";81->"BATTERY";82->"MOTOR_POWER";else->null
        } else if(message in setOf(18,19)) when(field.name) {
            "avg_power","max_power"->"RIDER_POWER";"avg_heart_rate","max_heart_rate"->"HEART_RATE"
            "avg_cadence","max_cadence","avg_fractional_cadence","max_fractional_cadence"->"CADENCE"
            "total_distance"->c.distanceSource;"avg_speed","max_speed","enhanced_avg_speed","enhanced_max_speed"->c.speedSource
            "normalized_power"->"NORMALIZED_POWER";"total_work"->"MECHANICAL_WORK";"total_calories"->"CALORIES"
            "total_ascent"->"GPS_ASCENT";"total_descent"->"GPS_DESCENT"
            "avg_altitude","min_altitude","max_altitude","enhanced_avg_altitude","enhanced_min_altitude","enhanced_max_altitude"->"GPS_ALTITUDE"
            else->null
        } else null
        return control==null || control in c.fields
    }
}
