package de.bcycle.app

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar

/** Modal presentation only; cancellation belongs to the existing import worker/token. */
internal class DreeveImportProgressView(context:Context,themes:ThemeManager,private val total:Int,cancel:()->Unit):LinearLayout(context) {
    private val count=BicycleFeedback.status(context,themes).apply { tag="dreeve_import_progress_count" }
    private val progress=ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal).apply {
        tag="dreeve_import_progress_bar";max=total.coerceAtLeast(1)
        BicycleComponents.bindTheme(this,themes) { progressTintList=ColorStateList.valueOf(it.primary) }
    }
    private val current=BicycleComponents.description(context,themes,"").apply { tag="dreeve_import_progress_name";maxLines=2 }
    private val abort=BicycleComponents.textAction(context,themes,context.getString(R.string.route_cancel)) {
        isCancelling=true;abortRequested();cancel()
    }.apply { tag="dreeve_import_progress_cancel" }
    private var done=0
    private var isCancelling=false
    init {
        orientation=VERTICAL;tag="dreeve_import_progress"
        val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding);setPadding(pad,pad,pad,pad)
        addView(BicycleComponents.contentHeading(context,themes,context.getString(R.string.dreeve_import_running)))
        addView(count);addView(progress,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.controlGap)))
        addView(current,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        addView(abort,LayoutParams(-2,-2).apply { gravity=Gravity.CENTER_HORIZONTAL;topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
        updateCount()
    }
    private fun updateCount() { BicycleFeedback.update(count,context.getString(R.string.dreeve_import_progress_count,done,total)) }
    private fun abortRequested() {
        abort.isEnabled=false;current.setText(R.string.dreeve_import_cancelling)
    }
    fun completed(name:String) {
        done=(done+1).coerceAtMost(total);progress.progress=done;updateCount()
        if(!isCancelling) current.text=name
    }
}
