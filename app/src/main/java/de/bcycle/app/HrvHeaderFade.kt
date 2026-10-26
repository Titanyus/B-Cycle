package de.bcycle.app

import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.ValueAnimator

/** Only the measurement header's contents fade; its surface and navigation handlers stay intact. */
internal class HrvHeaderFade(private val header:ViewGroup):AutoCloseable {
    private var measuring=false
    private var animation:ValueAnimator?=null
    private fun opacity(value:Float) { for(index in 0 until header.childCount) header.getChildAt(index).alpha=value }
    fun show(active:Boolean) {
        if(active==measuring) return
        measuring=active
        animation?.cancel();animation=null
        if(active) animation=ValueAnimator.ofFloat(if(header.childCount>0) header.getChildAt(0).alpha else 1f,0f).apply {
            duration=2000;interpolator=AccelerateDecelerateInterpolator()
            addUpdateListener { opacity(it.animatedValue as Float) };start()
        } else opacity(1f)
    }
    override fun close() {
        animation?.cancel();animation=null;opacity(1f)
    }
}
