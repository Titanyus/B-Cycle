package de.bcycle.app

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*

internal enum class LunarPhase(val label:Int) {
    NEW(R.string.moon_new),FIRST_QUARTER(R.string.moon_first_quarter),FULL(R.string.moon_full),LAST_QUARTER(R.string.moon_last_quarter)
}
internal data class LunarEvent(val instant:Instant,val phase:LunarPhase) {
    fun day(zone:ZoneId)=instant.atZone(zone).toLocalDate()
}

/** Meeus, Astronomical Algorithms, chapter 49: true principal phases, computed offline.
 * JDE is dynamical time; the standard polynomial Delta-T approximation converts it to UTC.
 * Calendar annotations only, never inputs to HRV/readiness or physiological calculations. */
internal object LunarPhases {
    fun between(first:LocalDate,last:LocalDate,zone:ZoneId):List<LunarEvent> {
        if(last<first) return emptyList()
        val begin=first.atStartOfDay(zone).toInstant()
        val end=last.plusDays(1).atStartOfDay(zone).toInstant()
        val low=floor(((begin.epochSecond/86400.0+2440587.5)-2451550.09765)/29.530588853).toInt()-1
        val high=ceil(((end.epochSecond/86400.0+2440587.5)-2451550.09765)/29.530588853).toInt()+1
        return (low..high).flatMap { cycle -> LunarPhase.entries.mapIndexed { i,phase -> event(cycle+i/4.0,phase) } }
            .filter { it.instant>=begin && it.instant<end }.sortedBy { it.instant }
    }
    private fun event(k:Double,phase:LunarPhase):LunarEvent {
        val t=k/1236.85;val t2=t*t;val t3=t2*t;val t4=t3*t
        val e=1-.002516*t-.0000074*t2
        val m=2.5534+29.10535670*k-.0000014*t2-.00000011*t3
        val p=201.5643+385.81693528*k+.0107582*t2+.00001238*t3-.000000058*t4
        val f=160.7108+390.67050284*k-.0016118*t2-.00000227*t3+.000000011*t4
        val omega=124.7746-1.56375580*k+.0020672*t2+.00000215*t3
        fun s(degrees:Double)=sin(Math.toRadians(degrees%360))
        fun c(degrees:Double)=cos(Math.toRadians(degrees%360))
        var correction:Double
        if(phase==LunarPhase.NEW || phase==LunarPhase.FULL) {
            val full=phase==LunarPhase.FULL
            correction=-(if(full) .40614 else .40720)*s(p)+(if(full) .17302 else .17241)*e*s(m)+
                (if(full) .01614 else .01608)*s(2*p)+(if(full) .01043 else .01039)*s(2*f)+
                (if(full) .00734 else .00739)*e*s(p-m)-(if(full) .00515 else .00514)*e*s(p+m)+
                (if(full) .00209 else .00208)*e*e*s(2*m)-.00111*s(p-2*f)-.00057*s(p+2*f)+
                .00056*e*s(2*p+m)-.00042*s(3*p)+.00042*e*s(m+2*f)+.00038*e*s(m-2*f)-
                .00024*e*s(2*p-m)-.00017*s(omega)-.00007*s(p+2*m)+.00004*s(2*p-2*f)+
                .00004*s(3*m)+.00003*s(p+m-2*f)+.00003*s(2*p+2*f)-.00003*s(p+m+2*f)+
                .00003*s(p-m+2*f)-.00002*s(p-m-2*f)-.00002*s(3*p+m)+.00002*s(4*p)
        } else {
            correction=-.62801*s(p)+.17172*e*s(m)-.01183*e*s(p+m)+.00862*s(2*p)+.00804*s(2*f)+
                .00454*e*s(p-m)+.00204*e*e*s(2*m)-.00180*s(p-2*f)-.00070*s(p+2*f)-.00040*s(3*p)-
                .00034*e*s(2*p-m)+.00032*e*s(m+2*f)+.00032*e*s(m-2*f)-.00028*e*e*s(p+2*m)+
                .00027*e*s(2*p+m)-.00017*s(omega)-.00005*s(p-m-2*f)+.00004*s(2*p+2*f)-
                .00004*s(p+m+2*f)+.00004*s(p-2*m)+.00003*s(p+m-2*f)+.00003*s(3*m)+
                .00002*s(2*p-2*f)+.00002*s(p-m+2*f)-.00002*s(3*p+m)
            val w=.00306-.00038*e*c(m)+.00026*c(p)-.00002*c(p-m)+.00002*c(p+m)+.00002*c(2*f)
            correction+=if(phase==LunarPhase.FIRST_QUARTER) w else -w
        }
        val angles=doubleArrayOf(299.77+.107408*k-.009173*t2,251.88+.016321*k,251.83+26.651886*k,
            349.42+36.412478*k,84.66+18.206239*k,141.74+53.303771*k,207.14+2.453732*k,
            154.84+7.306860*k,34.52+27.261239*k,207.19+.121824*k,291.34+1.844379*k,
            161.72+24.198154*k,239.56+25.513099*k,331.55+3.592518*k)
        val weights=doubleArrayOf(.000325,.000165,.000164,.000126,.000110,.000062,.000060,.000056,.000047,.000042,.000040,.000037,.000035,.000023)
        correction+=angles.indices.sumOf { weights[it]*s(angles[it]) }
        val jde=2451550.09765+29.530588853*k+.0001337*t2-.000000150*t3+.00000000073*t4+correction
        val year=2000+k/12.3685
        val dt=if(year<2005) {
            val u=year-2000
            63.86+.3345*u-.060374*u*u+.0017275*u.pow(3)+.000651814*u.pow(4)+.00002373599*u.pow(5)
        } else if(year<2050) {
            val u=year-2000;62.92+.32217*u+.005589*u*u
        } else { -20+32*((year-1820)/100).pow(2)-.5628*(2150-year) }
        return LunarEvent(Instant.ofEpochMilli((((jde-2440587.5)*86400-dt)*1000).roundToLong()),phase)
    }
}
