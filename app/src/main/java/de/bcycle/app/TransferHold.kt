package de.bcycle.app

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

/** Exact transfer gesture threshold; native long-click accessibility remains available. */
internal object TransferHold {
    const val DELAY_MS=600L
    fun bind(view:View,enabled:()->Boolean,click:()->Unit,hold:()->Unit) {
        var down=false;var fired=false;var x=0f;var y=0f
        val slop=ViewConfiguration.get(view.context).scaledTouchSlop
        val timer=Runnable { if(down && enabled()) { fired=true;view.performLongClick() } }
        fun cancel() { down=false;view.isPressed=false;view.removeCallbacks(timer) }
        view.setOnClickListener { if(enabled()) click() }
        view.setOnLongClickListener { if(enabled()) { hold();true } else false }
        view.setOnTouchListener { _,event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN->{
                    if(!enabled()) false else {
                        cancel();down=true;fired=false;x=event.x;y=event.y;view.isPressed=true
                        view.postDelayed(timer,DELAY_MS);true
                    }
                }
                MotionEvent.ACTION_MOVE->{ if(kotlin.math.abs(event.x-x)>slop || kotlin.math.abs(event.y-y)>slop) cancel();true }
                MotionEvent.ACTION_UP->{ val tap=down && !fired;cancel();if(tap) view.performClick();true }
                MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN->{ cancel();true }
                else->false
            }
        }
        view.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v:View) {}
            override fun onViewDetachedFromWindow(v:View) { cancel() }
        })
    }
}
