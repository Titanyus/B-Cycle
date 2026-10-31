package de.bcycle.app

/** Stable persisted method IDs; five main zones share the existing semantic colors. */
enum class HeartRateMethod(val label:Int,val explanation:Int) {
    KARVONEN(R.string.heart_method_karvonen,R.string.heart_method_karvonen_explanation),
    MAXIMUM(R.string.heart_method_maximum,R.string.heart_method_maximum_explanation),
    THRESHOLD(R.string.heart_method_threshold,R.string.heart_method_threshold_explanation);
    val requiredFields get()=when(this) {
        KARVONEN->setOf(RiderField.RESTING_HEART_RATE,RiderField.MAXIMUM_HEART_RATE)
        MAXIMUM->setOf(RiderField.MAXIMUM_HEART_RATE)
        THRESHOLD->setOf(RiderField.THRESHOLD_HEART_RATE)
    }
}
data class HeartRateZoneModel(val method:HeartRateMethod,val offset:Int,val scale:Int,
    val zones:List<KarvonenZone>,val maximumHeartRate:Int,val openLastZone:Boolean=false) {
    val boundariesPercent get()=zones.drop(1).map { it.fromPercent }
    fun percent(bpm:Double)=(bpm-offset)*100/scale
    fun zoneFor(bpm:Int)=zones.firstOrNull { bpm in it }
    fun zoneFor(bpm:Double):KarvonenZone? {
        if(!bpm.isFinite() || bpm !in 1.0..maximumHeartRate.toDouble()) return null
        return zones[boundariesPercent.count { bpm>=offset+scale*it/100.0 }]
    }
}
object HeartRateZones {
    fun calculate(profile:RiderProfile):HeartRateZoneModel? {
        val method=profile.heartRateMethod
        if(method==HeartRateMethod.KARVONEN) {
            val old=Karvonen.calculate(profile.restingHeartRate,profile.maximumHeartRate) ?: return null
            return HeartRateZoneModel(method,old.restingHeartRate,old.reserve,old.zones,old.maximumHeartRate)
        }
        val reference=when(method) {
            HeartRateMethod.MAXIMUM->profile.maximumHeartRate?.takeIf { it in RiderProfileValidation.maximumRange }
            else->profile.thresholdHeartRate?.takeIf { it in RiderProfileValidation.thresholdRange }
        } ?: return null
        val boundaries=if(method==HeartRateMethod.MAXIMUM) listOf(0,60,70,80,90,100) else listOf(0,81,90,94,100,0)
        val open=method==HeartRateMethod.THRESHOLD
        val maximum=if(open) 250 else reference
        val zones=List(5) { i ->
            val lower=if(i==0) 1 else (reference*boundaries[i]+99)/100
            val upper=if(i==4) maximum+1 else (reference*boundaries[i+1]+99)/100
            KarvonenZone(i+1,boundaries[i],boundaries[i+1],lower,upper)
        }
        return HeartRateZoneModel(method,0,reference,zones,maximum,open)
    }
}
