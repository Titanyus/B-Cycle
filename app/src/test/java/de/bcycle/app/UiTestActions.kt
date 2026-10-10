package de.bcycle.app

import android.app.AlertDialog
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import org.robolectric.Shadows.shadowOf

internal fun testDescendants(view: View): List<View> = listOf(view) + if (view is ViewGroup)
    (0 until view.childCount).flatMap { testDescendants(view.getChildAt(it)) } else emptyList()
internal fun selectTestSource(dialog: AlertDialog, name: String) {
    val spinner = testDescendants(dialog.window!!.decorView).single { it.tag == "field_source" } as Spinner
    spinner.setSelection((0 until spinner.adapter.count).single { spinner.adapter.getItem(it) == name })
    shadowOf(Looper.getMainLooper()).idle()
}
internal fun swipeTestDashboard(root: View) {
    val dashboard = testDescendants(root).filterIsInstance<DashboardView>().single()
    val tile = testDescendants(dashboard).first { it.isClickable }
    val time = SystemClock.uptimeMillis()
    for ((offset, action, x) in listOf(Triple(0L, MotionEvent.ACTION_DOWN, tile.width - 10f),
        Triple(40L, MotionEvent.ACTION_MOVE, tile.width / 2f), Triple(80L, MotionEvent.ACTION_UP, 10f))) {
        val event = MotionEvent.obtain(time, time + offset, action, x, tile.height / 2f, 0)
        tile.dispatchTouchEvent(event); event.recycle()
    }
}
