package de.bcycle.hrv

import kotlin.math.*

/** Welch PSD of the corrected NN tachogram. Time comes from NN, never packet delivery. */
data class FrequencyMetrics(val lfMs2:Double,val hfMs2:Double,val lfHf:Double?,
    val durationSeconds:Double,val segmentSamples:Int,val segments:Int) {
    companion object { const val VERSION="welch-cubic-4hz-hann-64s-v1" }
}
object FrequencyDomain {
    const val SAMPLE_HZ=4.0
    fun calculate(nn:List<Double>):FrequencyMetrics? {
        require(nn.all { it.isFinite() && it>0 })
        if(nn.size<4) return null
        val t=DoubleArray(nn.size);for(i in 1 until nn.size) t[i]=t[i-1]+nn[i]/1000
        val count=floor(t.last()*SAMPLE_HZ).toInt()+1
        if(count<200) return null // at least 50 s of observed corrected NN time
        // Natural cubic spline, without extrapolation outside observed NN endpoints.
        val second=DoubleArray(nn.size);val u=DoubleArray(nn.size)
        for(i in 1 until nn.lastIndex) {
            val sig=(t[i]-t[i-1])/(t[i+1]-t[i-1]);val p=sig*second[i-1]+2
            second[i]=(sig-1)/p
            u[i]=(6*((nn[i+1]-nn[i])/(t[i+1]-t[i])-(nn[i]-nn[i-1])/(t[i]-t[i-1]))/(t[i+1]-t[i-1])-sig*u[i-1])/p
        }
        for(i in nn.lastIndex-1 downTo 0) second[i]=second[i]*second[i+1]+u[i]
        var j=0
        val values=DoubleArray(count) { k ->
            val at=k/SAMPLE_HZ
            while(j<nn.size-2 && t[j+1]<at) j++
            val h=t[j+1]-t[j];val a=(t[j+1]-at)/h;val b=(at-t[j])/h
            a*nn[j]+b*nn[j+1]+((a*a*a-a)*second[j]+(b*b*b-b)*second[j+1])*h*h/6
        }
        val length=minOf(256,count);val step=length/2
        var nfft=1;while(nfft<length) nfft*=2
        val psd=DoubleArray(nfft/2+1);val window=DoubleArray(length) { .5-.5*cos(2*PI*it/(length-1)) }
        val scale=SAMPLE_HZ*window.sumOf { it*it };var segments=0
        for(start in 0..count-length step step) {
            val mean=(0 until length).sumOf { values[start+it] }/length
            val center=(length-1)/2.0
            val slope=(0 until length).sumOf { (it-center)*(values[start+it]-mean) }/
                (0 until length).sumOf { (it-center).pow(2) }
            val input=DoubleArray(length) { (values[start+it]-mean-slope*(it-center))*window[it] }
            // Small fixed 256-point transform; direct DFT avoids a new numerical dependency.
            for(k in psd.indices) {
                var real=0.0;var imag=0.0
                for(i in input.indices) { val angle=2*PI*k*i/nfft;real+=input[i]*cos(angle);imag-=input[i]*sin(angle) }
                psd[k]+=(real*real+imag*imag)/scale*if(k==0 || k==nfft/2) 1 else 2
            }
            segments++
        }
        for(k in psd.indices) psd[k]/=segments
        val df=SAMPLE_HZ/nfft
        fun at(hz:Double):Double { val pos=hz/df;val lo=floor(pos).toInt();val part=pos-lo;return psd[lo]*(1-part)+psd[lo+1]*part }
        fun band(lo:Double,hi:Double):Double {
            var sum=0.0;var previous=lo;var power=at(lo)
            for(k in 1 until psd.size) {
                val hz=k*df;if(hz<=lo) continue
                val end=minOf(hz,hi);val next=at(end)
                sum+=(end-previous)*(power+next)/2;previous=end;power=next
                if(hz>=hi) break
            }
            return sum.coerceAtLeast(0.0)
        }
        val lf=band(.04,.15);val hf=band(.15,.4)
        return FrequencyMetrics(lf,hf,if(hf>1e-9) lf/hf else null,t.last(),length,segments)
    }
}
