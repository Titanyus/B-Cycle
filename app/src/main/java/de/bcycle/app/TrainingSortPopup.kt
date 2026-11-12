package de.bcycle.app

import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow

/** Shared native icon tiles and footer positioning as the route sort menu. */
internal class TrainingSortPopup(private val context:Context,themes:ThemeManager,sort:TrainingSort,descending:Boolean,
    select:(TrainingSort)->Unit,direction:(Boolean)->Unit):PopupWindow() {
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    init {
        val body=BicycleComponents.dialogBody(context,themes)
        val row=LinearLayout(context).apply { orientation=LinearLayout.HORIZONTAL;isBaselineAligned=false;tag="training_sort_choices";setTag(R.id.inline_control_row,true) }
        val choices=linkedMapOf<TrainingSort,BicycleSegmentButton>()
        fun tile(button:BicycleSegmentButton,index:Int) {
            button.textSize=BicycleDesign.analysisLabelSp;button.setPadding(dp(2),0,dp(2),0);button.maxLines=1
            button.setAutoSizeTextTypeUniformWithConfiguration(BicycleDesign.analysisUnitMinSp,BicycleDesign.analysisLabelSp.toInt(),1,android.util.TypedValue.COMPLEX_UNIT_SP)
            row.addView(button,LinearLayout.LayoutParams(0,dp(BicycleDesign.rideNamePartHeight),1f).apply { marginStart=if(index==0) 0 else dp(BicycleDesign.controlGap) })
            BicycleComponents.controlSpacing(button)
        }
        (listOf(TrainingSort.STRENGTH)+TrainingSort.entries.filter { it!=TrainingSort.STRENGTH }).forEachIndexed { index,value ->
            val button=BicycleSegmentButton(context,themes,standalone=true).apply {
                tag="training_sort_${value.name}";setText(value.title);stackedIcon(value.icon)
                isChecked=sort==value;contentDescription=context.getString(value.title)
                setOnClickListener { choices.forEach { (key,view)->view.isChecked=key==value };select(value) }
            }
            choices[value]=button;tile(button,index)
        }
        var down=descending
        val toggle=BicycleSegmentButton(context,themes,multiple=true,standalone=true).apply {
            tag="training_sort_direction";showSelectionHighlight=false;stackedIcon(BicycleIcon.Sort)
        }
        fun label() {
            toggle.isChecked=down
            toggle.setText(if(down) R.string.route_sort_down else R.string.route_sort_up)
            toggle.contentDescription=toggle.text
        }
        label();toggle.setOnClickListener { down=!down;label();direction(down) };tile(toggle,TrainingSort.entries.size)
        body.addView(row,LinearLayout.LayoutParams(-1,-2));BicycleComponents.bindTheme(body,themes) { body.setBackgroundColor(it.secondary) }
        contentView=body;contentView.contentDescription=context.getString(R.string.route_sort_button)
        isFocusable=true;isOutsideTouchable=true
        setBackgroundDrawable(BicycleComponents.controlBackground(context,themes.active.tokens,BicycleComponents.controlSurface(context,themes.active.tokens)))
    }
    fun show(anchor:View) {
        val frame=Rect().also(anchor::getWindowVisibleDisplayFrame)
        if(frame.isEmpty) frame.set(0,0,context.resources.displayMetrics.widthPixels,context.resources.displayMetrics.heightPixels)
        width=frame.width().coerceAtLeast(1)
        val limit=(frame.height()*.75f).toInt().coerceAtLeast(dp(48))
        contentView.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(limit,View.MeasureSpec.AT_MOST))
        height=contentView.measuredHeight.coerceIn(dp(48),limit)
        val position=IntArray(2);anchor.getLocationOnScreen(position)
        val top=(position[1]-height).coerceIn(frame.top,(frame.bottom-height).coerceAtLeast(frame.top))
        setIsClippedToScreen(true);setIsLaidOutInScreen(true)
        showAtLocation(anchor,Gravity.TOP or Gravity.LEFT,frame.left,top)
    }
}
