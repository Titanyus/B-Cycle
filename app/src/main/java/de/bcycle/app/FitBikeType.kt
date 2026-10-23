package de.bcycle.app

import com.garmin.fit.*
import java.io.File

/** Profile metadata overlay; original files and all measurement fields stay intact. */
internal object FitBikeType {
    fun subSport(type:BikeType?)=when(type) {
        BikeType.EBIKE->SubSport.E_BIKE_FITNESS
        BikeType.INDOOR->SubSport.INDOOR_CYCLING
        else->SubSport.GENERIC
    }
    fun write(source:File,destination:File,type:BikeType) {
        require(source.inputStream().buffered().use { Decode().checkFileIntegrity(it) })
        val encoder=FileEncoder(destination,Fit.ProtocolVersion.V2_0)
        try {
            require(source.inputStream().buffered().use { Decode().read(it,MesgListener { message ->
                val typed=when(message.num) {
                    6->BikeProfileMesg(message).apply {
                        if(getField(BikeProfileMesg.SubSportFieldNum)!=null) setSubSport(subSport(type))
                    }
                    12->SportMesg(message).apply { setSport(Sport.CYCLING);setSubSport(subSport(type)) }
                    18->SessionMesg(message).apply { setSport(Sport.CYCLING);setSubSport(subSport(type)) }
                    19->LapMesg(message).apply { setSport(Sport.CYCLING);setSubSport(subSport(type)) }
                    else->message
                }
                encoder.write(typed)
            }) })
        } finally { encoder.close() }
        require(FitRead.read(destination).details.bikeType==type)
    }
}
