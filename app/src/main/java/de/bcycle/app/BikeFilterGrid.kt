package de.bcycle.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Same profile content and selection colors as the bike strip, with independent multi-selection. */
internal class BikeFilterGrid(context:Context,themes:ThemeManager,profiles:List<BikeProfile>,initial:Set<Int>,
    changed:(Int,Boolean)->Unit):GridLayout(context) {
    private val selected=initial.toMutableSet()
    fun showAvailable(slots:Set<Int>) {
        for(i in 0 until childCount) getChildAt(i).apply {
            val slot=tag.toString().removePrefix("route_bike_").toInt()
            isEnabled=slot in selected || slot in slots
            alpha=if(isEnabled) 1f else .5f
        }
    }
    init {
        columnCount=3;tag="route_bike_tiles"
        profiles.forEachIndexed { index,p ->
            val card=LinearLayout(context).apply {
                tag="route_bike_${p.slot}";orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
                val gap=BicycleDesign.dp(context,BicycleDesign.smallGap);setPadding(gap,gap,gap,gap)
                isClickable=true;isFocusable=true
                addView(BicycleComponents.text(context,themes,context.getString(p.type.label)).apply {
                    gravity=Gravity.CENTER;textSize=BicycleDesign.bodySp;maxLines=1
                },LinearLayout.LayoutParams(-1,-2))
                addView(ImageView(context).apply {
                    importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    BicycleComponents.bindTheme(this,themes) { setImageDrawable(BikePreview(p.category).apply { setTint(it.foreground) }) }
                },LinearLayout.LayoutParams(-1,BicycleDesign.dp(context,40)))
                addView(BicycleComponents.text(context,themes,p.name).apply {
                    gravity=Gravity.CENTER;textSize=BicycleDesign.bodySp;setLines(2);ellipsize=android.text.TextUtils.TruncateAt.END
                },LinearLayout.LayoutParams(-1,-2))
                contentDescription=context.getString(R.string.ui_source_field_description,context.getString(p.type.label),p.name)
                fun paint(tokens:ThemeTokens) {
                    val on=p.slot in selected;isSelected=on
                    stateDescription=context.getString(if(on) R.string.bikes_selected else R.string.ui_not_selected)
                    background=GradientDrawable().apply {
                        setColor(if(on) tokens.tertiary else tokens.secondary)
                        cornerRadius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat()
                        setStroke(BicycleDesign.dp(context,if(on) 2 else 1),BicycleComponents.selectionBorder(tokens,on))
                    }
                    val outline=BicycleInputFrame.decorate(this,tokens,on)
                    foreground=if(on) outline else null
                }
                BicycleComponents.bindTheme(this,themes,::paint)
                setOnClickListener {
                    if(!isEnabled) return@setOnClickListener
                    val on=p.slot !in selected;if(on) selected.add(p.slot) else selected.remove(p.slot)
                    paint(themes.active.tokens);changed(p.slot,on)
                }
            }
            addView(card,LayoutParams(spec(index/3),spec(index%3,1f)).apply {
                width=0;height=LayoutParams.WRAP_CONTENT
                if(index%3>0) leftMargin=BicycleDesign.dp(context,BicycleDesign.controlGap)
                if(index>=3) topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap)
            })
        }
    }
}
