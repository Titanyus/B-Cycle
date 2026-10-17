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

internal fun testBack(dialog: android.app.AlertDialog) {
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    testDescendants(dialog.window!!.decorView).single { it.tag=="dialog_back" }.performClick()
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
}

internal fun selectTestDashboardLayout(picker:DashboardPresetPicker,template:DashboardTemplate) {
    picker.findViewWithTag<View>("dashboard_layout_choice").performClick()
    val popup=requireNotNull(picker.layoutPopup)
    popup.contentView.findViewWithTag<View>("preset_${template.name}").performClick()
    shadowOf(Looper.getMainLooper()).idle()
}
internal fun selectTestDashboardLayout(dialog:AlertDialog,template:DashboardTemplate) {
    val root=dialog.window!!.decorView
    selectTestDashboardLayout(testDescendants(root).filterIsInstance<DashboardPresetPicker>().single(),template)
}

internal fun selectTestBikeType(root:View,type:BikeType) {
    fun layout() {
        root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2100,View.MeasureSpec.EXACTLY))
        root.layout(0,0,1080,2100)
        shadowOf(Looper.getMainLooper()).idle()
    }
    layout()
    root.findViewWithTag<Spinner>("bike_type_choice").setSelection(type.ordinal)
    layout()
}
