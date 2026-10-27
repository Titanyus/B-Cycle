package de.bcycle.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.RotateDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch

/** Dropdown-Rahmen: one existing control outline encloses the header and expanded content.
 * The optional trailing switch/special-filled button owns its action independently of expansion.
 */
internal class BicycleFrameDropdown(context:Context,themes:ThemeManager,title:String,open:Boolean=true,
    trailing:View?=null,leadingIcon:BicycleIcon,private val changed:(Boolean)->Unit={}):LinearLayout(context) {
    /** Explicit typography/background role; generic dialog styling must not add a second frame. */
    internal class Header(context:Context):Button(context)
    val header:Button=Header(context)
    val content=LinearLayout(context).apply {
        orientation=VERTICAL
        val pad=BicycleDesign.dp(context,BicycleDesign.dropdownFramePadding)
        setPadding(pad,BicycleDesign.dp(context,BicycleDesign.dropdownFrameHeaderGap),pad,pad)
    }
    private val arrow=RotateDrawable().apply { fromDegrees=0f;toDegrees=180f }
    private val row=LinearLayout(context)
    private var trailingControl:View?=null
    private val endToggle:ImageButton=BicycleComponents.icon(context,themes,BicycleIcon.Expand,title) { setExpanded(!expanded) }
    val expandControl:View get()=endToggle
    var expanded=open;private set
    init {
        orientation=VERTICAL;clipChildren=false;clipToPadding=false
        row.apply {
            orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;isBaselineAligned=false
            setTag(R.id.inline_control_row,true)
        }
        header.apply {
            text=title;isAllCaps=false;typeface=BicycleDesign.bodyTypeface;textSize=BicycleDesign.choiceSp
            gravity=Gravity.START or Gravity.CENTER_VERTICAL
            minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
            val pad=BicycleDesign.dp(context,BicycleDesign.horizontalPadding)
            val vertical=BicycleDesign.dp(context,BicycleDesign.verticalPadding)
            setPadding(pad,vertical,pad,vertical)
            compoundDrawablePadding=vertical
            setOnClickListener { setExpanded(!expanded) }
        }
        row.addView(header,LayoutParams(0,-2,1f))
        row.addView(endToggle,LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize)))
        trailing?.let(::setTrailing)
        addView(row,LayoutParams(-1,-2));addView(content,LayoutParams(-1,-2))
        BicycleComponents.bindTheme(this,themes) { tokens ->
            background=BicycleComponents.controlBackground(context,tokens)
        }
        BicycleComponents.bindTheme(header,themes) { tokens ->
            header.setTextColor(tokens.foreground);header.backgroundTintList=null
            header.background=RippleDrawable(ColorStateList.valueOf((tokens.foreground and 0xffffff) or 0x26000000),null,
                GradientDrawable().apply { setColor(Color.WHITE);cornerRadius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat() })
            val size=BicycleDesign.dp(context,BicycleDesign.iconSize)
            arrow.drawable=BicycleIcons.drawable(context,BicycleIcon.Expand,tokens.foreground)
            arrow.level=if(expanded) 10000 else 0
            arrow.setBounds(0,0,size,size)
            val icon=BicycleTextIcon(header,BicycleIcons.drawable(context,leadingIcon,tokens.foreground)).apply { setBounds(0,0,size,size) }
            header.setCompoundDrawablesRelative(icon,null,null,null)
            endToggle.setImageDrawable(arrow)
        }
        render()
        BicycleComponents.controlSpacing(this)
    }
    /** Reparents the original control, preserving checked state and existing listeners. */
    fun setTrailing(action:View) {
        trailingControl?.let { row.removeView(it) }
        (action.parent as? android.view.ViewGroup)?.removeView(action)
        if(action is Switch) {
            action.contentDescription=header.text
            action.text="";action.setCompoundDrawablesRelative(null,null,null,null)
            action.setPadding(0,0,0,0);action.switchPadding=0
        }
        row.addView(action,LayoutParams(if(action is Switch) BicycleDesign.dp(context,BicycleDesign.switchWidth) else -2,-2).apply {
            marginEnd=BicycleDesign.dp(context,BicycleDesign.horizontalPadding)
            marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap)
        })
        trailingControl=action
    }
    fun setExpanded(open:Boolean) {
        if(expanded==open) return
        expanded=open;render();changed(open)
    }
    private fun render() {
        content.visibility=if(expanded) VISIBLE else GONE
        arrow.level=if(expanded) 10000 else 0
        header.stateDescription=context.getString(if(expanded) R.string.zone_expanded else R.string.zone_collapsed)
        header.invalidate()
        expandControl.stateDescription=header.stateDescription
        endToggle.invalidate()
    }
}
