package de.bcycle.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.View
import android.widget.*

/** Named pages use stable slots; the ten geometry presets remain independent choices. */
internal class DashboardPresetPicker(context:Context, initialPage:Int,
    private val themes:ThemeManager=ThemeManager(context.getSharedPreferences("bike",0))) : LinearLayout(context) {
    private val prefs=context.getSharedPreferences("bike",0)
    val pages=DashboardPages(context,prefs)
    var changed:()->Unit={}
    val selection get()=pages.all().map { template(it.id) }
    private val row=LinearLayout(context).apply { orientation=HORIZONTAL;isBaselineAligned=false }
    private val scroller=object:HorizontalScrollView(context) {
        override fun onMeasure(w:Int,h:Int) {
            val gap=dp(BicycleDesign.controlGap)
            val width=((MeasureSpec.getSize(w)-gap*2)/3).coerceAtLeast(1)
            for(i in 0 until row.childCount) {
                val child=row.getChildAt(i);val lp=child.layoutParams as LinearLayout.LayoutParams
                // Assign parameters again so resolved native margins follow rebuilt/selected cards.
                val margin=if(i==0) 0 else gap
                if(lp.width!=width || lp.marginStart!=margin || lp.leftMargin!=margin) {
                    lp.width=width;lp.marginStart=margin;lp.marginEnd=0
                    lp.leftMargin=margin;lp.rightMargin=0;child.layoutParams=lp
                }
            }
            super.onMeasure(w,h)
        }
    }.apply { tag="dashboard_page_strip";isHorizontalScrollBarEnabled=false;addView(row,FrameLayout.LayoutParams(-2,-2)) }
    private val layoutChoice=Spinner(context).apply { tag="dashboard_layout_choice" }
    private var binding=false
    private fun dp(n:Int)=BicycleDesign.dp(context,n)
    private fun template(id:Int)=DashboardTemplate.decode(prefs.getString("dashboardTemplate_$id",DashboardTemplate.entries[id].name))
    init {
        orientation=VERTICAL
        pages.select(initialPage)
        addView(scroller,LayoutParams(-1,-2))
        val names=DashboardTemplate.entries.map { it.displayTitle(context) }
        layoutChoice.adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,names)
        BicycleDialogStyle.spinner(layoutChoice,themes) { name,color ->
            val template=DashboardTemplate.entries[names.indexOf(name).coerceAtLeast(0)]
            val preview=DashboardPreview(template).apply { setBounds(0,0,dp(28),dp(40));setTint(color) }
            SpannableString("\uFFFC  $name").apply { setSpan(ImageSpan(preview,ImageSpan.ALIGN_CENTER),0,1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        }
        addView(layoutChoice,LayoutParams(-1,dp(56)))
        layoutChoice.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent:AdapterView<*>?)=Unit
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long) {
                if(binding) return
                val page=pages.selected();val selected=DashboardTemplate.entries[position]
                if(template(page)==selected) return
                prefs.edit().putString("dashboardTemplate_$page",selected.name).apply();refresh();changed()
            }
        }
        refresh()
    }
    fun refresh() {
        binding=true
        val x=scroller.scrollX
        row.removeAllViews()
        fun card(page:DashboardPage?) {
            val active=page?.id==pages.selected()
            val tile=LinearLayout(context).apply {
                orientation=VERTICAL;gravity=Gravity.CENTER;isClickable=true;isFocusable=true
                tag=page?.let { "preset_page_${it.id}" } ?: "dashboard_page_add"
                setPadding(dp(BicycleDesign.smallGap),dp(BicycleDesign.verticalPadding),dp(BicycleDesign.smallGap),dp(BicycleDesign.verticalPadding))
                contentDescription=page?.name ?: context.getString(R.string.dashboard_new)
                isSelected=active;stateDescription=context.getString(if(active) R.string.ui_selected else R.string.ui_not_selected)
                addView(ImageView(context).apply {
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                    BicycleComponents.bindTheme(this,themes) { tokens ->
                        setImageDrawable(page?.let { DashboardPreview(template(it.id)) } ?: BicycleIcons.drawable(context,BicycleIcon.AddSensor,tokens.foreground));setColorFilter(tokens.foreground)
                    }
                },LayoutParams(-1,dp(BicycleDesign.bikePreviewHeight)))
                addView(BicycleComponents.text(context,themes,page?.name ?: context.getString(R.string.dashboard_new)).apply {
                    gravity=Gravity.CENTER;setLines(2);ellipsize=android.text.TextUtils.TruncateAt.END;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                },LayoutParams(-1,-2))
                BicycleComponents.bindTheme(this,themes) { tokens ->
                    background=GradientDrawable().apply {
                        cornerRadius=dp(BicycleDesign.inputRadius).toFloat();setColor(if(active) tokens.tertiary else tokens.secondary)
                        setStroke(if(active) dp(2) else BicycleDesign.linePixels(context),BicycleComponents.selectionBorder(tokens,active))
                    }
                }
                setOnClickListener {
                    if(page==null) pages.add() else pages.select(page.id)
                    refresh();changed()
                    val index=pages.all().indexOfFirst { it.id==pages.selected() }
                    scroller.post { row.getChildAt(index)?.let { scroller.smoothScrollTo(it.left,0) } }
                }
            }
            row.addView(tile,LayoutParams(0,dp(BicycleDesign.bikeTileHeight)))
        }
        pages.all().forEach(::card);if(pages.all().size<DashboardPages.MAX) card(null)
        layoutChoice.setSelection(template(pages.selected()).ordinal)
        scroller.post { scroller.scrollTo(x,0) };binding=false
    }
}
