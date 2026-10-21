package de.bcycle.app

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Shared convention for overflowing text. Durations are independent of string length. */
internal class TextScrollerMotion(private val distance:Float,density:Float) {
    companion object {
        const val FADE_IN_MS=3000.0
        const val HOLD_START_MS=4000.0
        const val ACCELERATE_MS=4000.0
        const val BRAKE_MS=4000.0
        const val HOLD_END_MS=4000.0
        const val FADE_OUT_MS=3000.0
        const val EMPTY_MS=2000.0
        const val SPEED_DP_PER_SECOND=18.0 // 60% of the previous nominal 30 dp/s.
    }
    data class Frame(val offset:Float,val alpha:Float)
    // Small overflows keep both 4-s ramps and use a lower peak speed, without overshoot.
    private val speed=min(SPEED_DP_PER_SECOND*density/1000,distance/4000.0)
    private val coastMs=if(speed>0) (distance/speed-4000).coerceAtLeast(0.0) else 0.0
    val travelMs=ACCELERATE_MS+coastMs+BRAKE_MS
    val durationMs=FADE_IN_MS+HOLD_START_MS+travelMs+HOLD_END_MS+FADE_OUT_MS+EMPTY_MS
    private fun smooth(value:Double)=.5-.5*cos(PI*value.coerceIn(0.0,1.0))
    fun frame(elapsedMs:Double):Frame {
        val elapsed=elapsedMs.coerceAtLeast(0.0)%durationMs
        val moving=elapsed-FADE_IN_MS-HOLD_START_MS
        val rampDistance=speed*ACCELERATE_MS/2
        val offset=when {
            moving<=0->0.0
            moving<ACCELERATE_MS->speed/2*(moving-ACCELERATE_MS/PI*sin(PI*moving/ACCELERATE_MS))
            moving<ACCELERATE_MS+coastMs->rampDistance+speed*(moving-ACCELERATE_MS)
            moving<travelMs->{
                val braking=moving-ACCELERATE_MS-coastMs
                rampDistance+speed*coastMs+speed/2*(braking+BRAKE_MS/PI*sin(PI*braking/BRAKE_MS))
            }
            else->distance.toDouble()
        }
        val fadeOutAt=FADE_IN_MS+HOLD_START_MS+travelMs+HOLD_END_MS
        val alpha=when {
            elapsed<FADE_IN_MS->smooth(elapsed/FADE_IN_MS)
            elapsed<fadeOutAt->1.0
            elapsed<fadeOutAt+FADE_OUT_MS->1-smooth((elapsed-fadeOutAt)/FADE_OUT_MS)
            else->0.0
        }
        return Frame(offset.toFloat().coerceIn(0f,distance.coerceAtLeast(0f)),alpha.toFloat())
    }
}
