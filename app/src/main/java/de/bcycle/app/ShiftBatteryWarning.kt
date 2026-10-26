package de.bcycle.app

/** One start-of-ride warning, independent of the periodic drinking reminder. */
internal class ShiftBatteryWarning {
    private var pending:Int?=null
    fun start(level:Int?,enabled:Boolean) { pending=level?.takeIf { enabled && it in 0..19 } }
    fun take(recording:Boolean,visible:Boolean,enabled:Boolean):Int? {
        if(!recording || !enabled) { clear();return null }
        if(!visible) return null
        return pending.also { pending=null }
    }
    fun clear() { pending=null }
}
