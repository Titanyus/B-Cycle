package de.bcycle.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.RadioButton

/** Native mutually exclusive radio semantics, presented as a segment without a selection circle. */
internal class BicycleSegmentButton(context: Context, private val themes: ThemeManager,private val multiple:Boolean=false) : RadioButton(context) {
    private var ready=false
    init {
        buttonDrawable=null;buttonTintList=null
        gravity=Gravity.CENTER;typeface=BicycleDesign.bodyTypeface;textSize=BicycleDesign.choiceSp
        minWidth=0;minimumWidth=0;minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
        setPadding(BicycleDesign.dp(context,BicycleDesign.controlGap),0,BicycleDesign.dp(context,BicycleDesign.controlGap),0)
        maxLines=2
        BicycleComponents.bindTheme(this,themes,::paint)
        BicycleComponents.controlSpacing(this)
        ready=true
    }
    /** Keep icon and label together even when the segment is taller than its contents. */
    fun stackedIcon(icon:BicycleIcon) {
        includeFontPadding=false
        compoundDrawablePadding=BicycleDesign.dp(context,BicycleDesign.segmentIconTextGap)
        textSize=BicycleDesign.analysisLabelSp
        BicycleComponents.bindTheme(this,themes) {
            val drawable=BicycleTextIcon(this,BicycleIcons.drawable(context,icon,currentTextColor),above=true).apply {
                val size=BicycleDesign.dp(context,BicycleDesign.iconSize);setBounds(0,0,size,size)
            }
            setCompoundDrawables(null,drawable,null,null)
        }
    }
    override fun onAttachedToWindow() {
        (parent as? android.widget.LinearLayout)?.takeIf { it.orientation==android.widget.LinearLayout.HORIZONTAL }
            ?.let { BicycleComponents.segmentedControlRow(it,themes) }
        super.onAttachedToWindow()
    }
    override fun toggle() { if(multiple) isChecked=!isChecked else super.toggle() }
    override fun getAccessibilityClassName():CharSequence=if(multiple) android.widget.CheckBox::class.java.name else super.getAccessibilityClassName()
    override fun drawableStateChanged() { super.drawableStateChanged();if(ready) paint(themes.active.tokens) }
    private fun paint(tokens:ThemeTokens) {
        val fill=if(isChecked) tokens.accent else tokens.secondary
        val foreground=if(!isChecked || ThemeValidator.contrast(tokens.foreground,fill)>=ThemeValidator.minimumTextContrast) tokens.foreground
            else if(ThemeValidator.contrast(Color.BLACK,fill)>=ThemeValidator.contrast(Color.WHITE,fill)) Color.BLACK else Color.WHITE
        setTextColor(foreground)
        compoundDrawables.filterNotNull().forEach { it.setTint(foreground) }
        background=RippleDrawable(ColorStateList.valueOf((foreground and 0x00ffffff) or 0x26000000),if(isChecked) BicycleAccentGradient.fill(context,tokens,foreground) else GradientDrawable().apply {
            setColor(fill);cornerRadius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat()
        },null)
    }
}
