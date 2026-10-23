package de.bcycle.app

import org.json.JSONObject

/** Sensor capabilities, independent of dashboard fields and the current connection status. */
enum class RecordingQuantity(val label:Int) {
    SPEED(R.string.bikes_fit_primary_speed),DISTANCE(R.string.bikes_fit_primary_distance),
    POWER(R.string.source_power),CADENCE(R.string.source_cadence),HEART(R.string.source_heart),HEIGHT(R.string.source_height)
}
enum class RecordingSource(val label:Int) {
    BIKE(R.string.ui_source_bike),GPS(R.string.ui_source_gps),TRAINER(R.string.bikes_trainer),
    HEART(R.string.ui_source_heart),BAROMETER(R.string.source_phone_barometer)
}
data class RecordingSources(val preferred:Map<RecordingQuantity,RecordingSource>) {
    fun json()=JSONObject().apply { preferred.forEach { (quantity,source)->put(quantity.name,source.name) } }.toString()
    companion object {
        const val KEY="recordSourcesV1"
        fun allowed(q:RecordingQuantity)=when(q) {
            RecordingQuantity.SPEED,RecordingQuantity.DISTANCE->setOf(RecordingSource.BIKE,RecordingSource.GPS,RecordingSource.TRAINER)
            RecordingQuantity.POWER,RecordingQuantity.CADENCE->setOf(RecordingSource.BIKE,RecordingSource.TRAINER)
            RecordingQuantity.HEART->setOf(RecordingSource.HEART,RecordingSource.TRAINER)
            RecordingQuantity.HEIGHT->setOf(RecordingSource.GPS,RecordingSource.BAROMETER)
        }
        fun decode(text:String):RecordingSources {
            require(text.length<=4096);val json=JSONObject(text)
            return RecordingSources(json.keys().asSequence().associate { key ->
                val q=RecordingQuantity.valueOf(key);val source=RecordingSource.valueOf(json.getString(key))
                require(source in allowed(q));q to source
            })
        }
        fun defaults(config:RecordingConfig,indoor:Boolean)=RecordingSources(mapOf(
            RecordingQuantity.SPEED to if(config.speedSource=="GPS_SPEED" && !indoor) RecordingSource.GPS else if(indoor) RecordingSource.TRAINER else RecordingSource.BIKE,
            RecordingQuantity.DISTANCE to if(config.distanceSource=="GPS_DISTANCE" && !indoor) RecordingSource.GPS else if(indoor) RecordingSource.TRAINER else RecordingSource.BIKE,
            RecordingQuantity.POWER to if(indoor) RecordingSource.TRAINER else RecordingSource.BIKE,
            RecordingQuantity.CADENCE to if(indoor) RecordingSource.TRAINER else RecordingSource.BIKE,
            RecordingQuantity.HEART to RecordingSource.HEART,RecordingQuantity.HEIGHT to RecordingSource.GPS))
        internal fun available(profile:BikeProfile,barometer:Boolean):Map<RecordingQuantity,List<RecordingSource>> = RecordingQuantity.entries.associateWith { q ->
            buildList {
                if(profile.type==BikeType.INDOOR) {
                    if(profile.trainerEnabled && profile.trainerAddress!=null && q in setOf(RecordingQuantity.SPEED,RecordingQuantity.DISTANCE,RecordingQuantity.POWER,RecordingQuantity.CADENCE,RecordingQuantity.HEART)) add(RecordingSource.TRAINER)
                } else {
                    if(profile.bikeEnabled && profile.bikeAddress!=null && q in setOf(RecordingQuantity.SPEED,RecordingQuantity.DISTANCE,RecordingQuantity.POWER,RecordingQuantity.CADENCE)) add(RecordingSource.BIKE)
                    if(profile.gpsEnabled && q in setOf(RecordingQuantity.SPEED,RecordingQuantity.DISTANCE,RecordingQuantity.HEIGHT)) add(RecordingSource.GPS)
                    if(barometer && q==RecordingQuantity.HEIGHT) add(RecordingSource.BAROMETER)
                }
                if(profile.heartEnabled && profile.heartAddress!=null && q==RecordingQuantity.HEART) add(RecordingSource.HEART)
            }
        }
    }
}
internal data class SourceReading(val value:Double,val atMs:Long,val seaLevel:Boolean=false)
internal data class ChosenReading(val source:RecordingSource,val reading:SourceReading)
internal fun chooseReading(q:RecordingQuantity,preferences:RecordingSources,readings:Map<RecordingSource,SourceReading>,now:Long):ChosenReading? {
    val valid=readings.filter { (source,value)->source in RecordingSources.allowed(q) && value.value.isFinite() && now-value.atMs in 0..5000 }
    val source=preferences.preferred[q]?.takeIf { it in valid } ?: valid.keys.firstOrNull() ?: return null
    return ChosenReading(source,valid.getValue(source))
}
/** Adding hidden columns preserves BCY7 framing and existing public/developer field IDs. */
internal object PrimaryRecording {
    const val FLAG="PRIMARY_SOURCES_V1"
    val columns=listOf("PRIMARY_SPEED","PRIMARY_DISTANCE","PRIMARY_POWER","PRIMARY_CADENCE","PRIMARY_HEART","PRIMARY_HEIGHT","PRIMARY_HEIGHT_MSL","PRIMARY_ASCENT","PRIMARY_DESCENT","PRIMARY_GRADIENT")
    fun column(id:String,config:RecordingConfig):String?=when(id) {
        config.speedSource->"PRIMARY_SPEED";config.distanceSource->"PRIMARY_DISTANCE"
        "RIDER_POWER"->"PRIMARY_POWER";"CADENCE"->"PRIMARY_CADENCE";"HEART_RATE"->"PRIMARY_HEART"
        "GPS_ALTITUDE"->"PRIMARY_HEIGHT";"GPS_ALTITUDE_MSL"->"PRIMARY_HEIGHT_MSL"
        "GPS_ASCENT"->"PRIMARY_ASCENT";"GPS_DESCENT"->"PRIMARY_DESCENT";"GPS_GRADIENT"->"PRIMARY_GRADIENT";else->null
    }
    fun value(values:Map<String,Double>,id:String,config:RecordingConfig):Double?=
        if(FLAG in config.fields) values[column(id,config) ?: id] else values[id]
    fun control(column:String,config:RecordingConfig):String?=when(column) {
        "PRIMARY_SPEED"->config.speedSource;"PRIMARY_DISTANCE"->config.distanceSource
        "PRIMARY_POWER"->"RIDER_POWER";"PRIMARY_CADENCE"->"CADENCE";"PRIMARY_HEART"->"HEART_RATE"
        "PRIMARY_HEIGHT","PRIMARY_HEIGHT_MSL"->"GPS_ALTITUDE";"PRIMARY_ASCENT"->"GPS_ASCENT";"PRIMARY_DESCENT"->"GPS_DESCENT";"PRIMARY_GRADIENT"->"GPS_GRADIENT";else->null
    }
    fun unit(id:String)=when(id) { "PRIMARY_SPEED"->"m/s";"PRIMARY_POWER"->"W";"PRIMARY_CADENCE"->"rpm";"PRIMARY_HEART"->"bpm";"PRIMARY_HEIGHT_MSL"->"bool";"PRIMARY_GRADIENT"->"%";else->"m" }
}
/** Sum only observed deltas of the active source. Rejoining counters never add their old total. */
internal class SourceDistance {
    private var source:RecordingSource?=null
    private var previous:Double?=null
    var meters=0.0;private set
    fun add(chosen:ChosenReading?):Double {
        if(chosen==null) { source=null;previous=null;return meters }
        val value=chosen.reading.value
        if(source==chosen.source && previous!=null && value>=previous!!) meters+=value-previous!!
        source=chosen.source;previous=value;return meters
    }
}
