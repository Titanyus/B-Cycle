package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Compact palette selector: the same name and five real theme colors in header and popup. */
internal class ThemePickerView(context:Context,private val themes:ThemeManager):LinearLayout(context) {
    private fun dp(n:Int)=BicycleDesign.dp(context,n)
    internal var popup:ListPopupWindow?=null;private set
    private val chosen=row(themes.active,header=true)
    init {
        orientation=VERTICAL;tag="theme_picker"
        layoutParams=LayoutParams(-1,-2).apply { bottomMargin=dp(BicycleDesign.controlGap) }
        addView(chosen,LayoutParams(-1,-2))
        chosen.tag="theme_choice";chosen.isClickable=true;chosen.isFocusable=true
        chosen.setOnClickListener { open() }
        BicycleComponents.bindTheme(this,themes) {
            (chosen.getChildAt(0) as TextView).text=themes.active.name.uppercase(java.util.Locale.ROOT)
            (chosen.getChildAt(1) as ThemeSwatches).colors=palette(themes.active)
            chosen.contentDescription=themes.active.name
        }
    }
    private fun palette(theme:BicycleTheme)=theme.tokens.let { intArrayOf(it.primary,it.secondary,it.tertiary,it.foreground,it.accent) }
    private fun row(theme:BicycleTheme?,header:Boolean=false):LinearLayout = LinearLayout(context).apply {
        orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
        minimumHeight=dp(BicycleDesign.themeDropdownHeight)
        setPadding(dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.smallGap),dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.smallGap))
        tag=theme?.id ?: "theme_reserved";isEnabled=theme!=null
        contentDescription=theme?.name ?: context.getString(R.string.ui_reserved)
        addView(TextView(context).apply {
            BicycleDialogStyle.control(this,themes)
            text=theme?.name?.uppercase(java.util.Locale.ROOT) ?: context.getString(R.string.ui_reserved).uppercase(java.util.Locale.ROOT)
            typeface=resources.getFont(R.font.bebas_neue);textSize=BicycleDesign.settingsThemeNameSp
            maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
            BicycleComponents.bindTheme(this,themes) { setTextColor(it.foreground) }
        },LayoutParams(0,-2,1.2f).apply { marginEnd=dp(BicycleDesign.controlGap) })
        addView(ThemeSwatches(context).apply {
            colors=theme?.let(::palette) ?: intArrayOf(0xff333333.toInt(),0xff555555.toInt(),0xff777777.toInt(),0xffaaaaaa.toInt(),0xffdddddd.toInt())
        },LayoutParams(0,dp(BicycleDesign.iconSize),1f))
        addView(ImageView(context).apply {
            importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
            BicycleComponents.bindTheme(this,themes) {
                setImageDrawable(BicycleIcons.drawable(context,if(header) BicycleIcon.Expand else BicycleIcon.Check,it.foreground))
                visibility=if(header || theme?.id==themes.active.id) VISIBLE else INVISIBLE
            }
        },LayoutParams(dp(BicycleDesign.iconSize),dp(BicycleDesign.iconSize)).apply { marginStart=dp(BicycleDesign.controlGap) })
        BicycleComponents.bindTheme(this,themes) { tokens ->
            isSelected=header || (theme!=null && theme.id==themes.active.id)
            stateDescription=context.getString(if(isSelected) R.string.bikes_selected else R.string.theme_not_selected)
            alpha=if(theme==null) .4f else 1f
            background=GradientDrawable().apply {
                setColor(tokens.secondary);cornerRadius=dp(BicycleDesign.inputRadius).toFloat()
                setStroke(if(!header && isSelected) dp(BicycleDesign.themeSelectedBorder) else dp(BicycleDesign.controlBorder),
                    if(!header && isSelected) tokens.accent else tokens.foreground)
            }
        }
    }
    private fun open() {
        popup?.dismiss()
        val entries=themes.presets+listOf(null)
        popup=ListPopupWindow(context).apply {
            anchorView=chosen;isModal=true;width=chosen.width
            setAdapter(object:BaseAdapter() {
                override fun getCount()=entries.size
                override fun getItem(position:Int)=entries[position]
                override fun getItemId(position:Int)=position.toLong()
                override fun areAllItemsEnabled()=false
                override fun isEnabled(position:Int)=entries[position]!=null
                override fun getView(position:Int,convertView:View?,parent:ViewGroup)=row(entries[position])
            })
            setOnItemClickListener { _,_,position,_ ->
                entries[position]?.let { themes.activate(it.id) };dismiss()
            }
            setBackgroundDrawable(BicycleComponents.controlBackground(context,themes.active.tokens))
            show()
        }
    }
    override fun onDetachedFromWindow() { popup?.dismiss();popup=null;super.onDetachedFromWindow() }
}

/** Cached parallelograms: no gradients, each segment uses its exact palette color. */
private class ThemeSwatches(context:Context):View(context) {
    var colors=IntArray(5);set(value) { field=value;invalidate() }
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val paths=Array(5) { Path() }
    init { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
        val top=h*.12f;val bottom=h*.88f;val slant=h*.5f
        val step=(w-slant)/5f;val gap=BicycleDesign.dp(context,2).toFloat()
        paths.forEachIndexed { i,path ->
            val x=i*step;path.reset();path.moveTo(x+slant,top);path.lineTo(x+step+slant-gap,top)
            path.lineTo(x+step-gap,bottom);path.lineTo(x,bottom);path.close()
        }
    }
    override fun onDraw(canvas:Canvas) { paths.forEachIndexed { i,path -> paint.color=colors[i];canvas.drawPath(path,paint) } }
}
