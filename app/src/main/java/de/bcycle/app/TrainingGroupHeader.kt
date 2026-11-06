package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.widget.LinearLayout

/** Same title/value/label layout and top rule as the route manager's group summaries. */
internal class TrainingGroupHeader(context:Context,themes:ThemeManager):LinearLayout(context) {
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    private val title=BicycleComponents.text(context,themes,"").apply {
        BicycleComponents.headerTypography(this)
        gravity=Gravity.CENTER;isAccessibilityHeading=true;includeFontPadding=false;maxLines=1
        setAutoSizeTextTypeUniformWithConfiguration(16,BicycleDesign.headingSp.toInt(),1,android.util.TypedValue.COMPLEX_UNIT_SP)
        tag="training_group_title"
    }
    private val count=RouteListReading(context,themes,R.string.training_group_count).apply {
        number.setHorizontallyScrolling(false);unit.setHorizontallyScrolling(false)
    }
    init {
        orientation=VERTICAL
        BicycleComponents.bindTheme(this,themes) { tokens ->
            background=object:ColorDrawable(tokens.tertiary) {
                private val rule=Paint().apply { color=BicycleComponents.sectionRuleColor(tokens) }
                override fun draw(canvas:Canvas) {
                    super.draw(canvas)
                    canvas.drawRect(bounds.left.toFloat(),bounds.top.toFloat(),bounds.right.toFloat(),bounds.top+dp(2).toFloat(),rule)
                }
            }
        }
        setPadding(dp(BicycleDesign.verticalPadding),dp(BicycleDesign.rowGap),dp(BicycleDesign.verticalPadding),dp(BicycleDesign.sectionDividerGap))
        setOnTouchListener { _,_->true }
        addView(title,LayoutParams(-1,-2))
        addView(count,LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
    }
    fun bind(label:String,amount:Int) {
        title.text=label;contentDescription=label
        count.bind(amount.toString(),label,"training_group_count")
    }
}
