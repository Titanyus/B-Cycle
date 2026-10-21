package de.bcycle.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Shared full-size profile cards: manager strip or three-column dashboard grid. */
internal class BikeProfileStrip(context: Context, private val themes: ThemeManager,
    private val select: (Int) -> Unit, private val add: () -> Unit,
    private val move: (Int, Int) -> Boolean, private val compact:Boolean=false) : HorizontalScrollView(context) {
    private val row:ViewGroup = if(compact) GridLayout(context).apply { columnCount=BicycleDesign.bikeQuickVisibleTiles }
        else LinearLayout(context).apply { orientation=LinearLayout.HORIZONTAL;isBaselineAligned=false }
    private var profiles = emptyList<BikeProfile>()
    private var painted = ""
    private var active = -1
    private var locked = false
    private var shape = emptyList<Int>()
    private val reorder = if(compact) null else TileReorder(this,row as LinearLayout,themes,
        { profiles.map { it.slot } }, { locked }, move)
    private val scale get()=if(compact) BicycleDesign.bikeQuickScale else 1f
    private fun dp(n:Int)=(BicycleDesign.dp(context,n)*scale).toInt().coerceAtLeast(1)
    init {
        tag=if(compact) "bike_quick_strip" else "bike_profile_strip";isHorizontalScrollBarEnabled=false;isFillViewport=false
        addView(row,LayoutParams(-2,-2))
        BicycleComponents.bindTheme(this,themes) { paint() }
    }
    fun show(rows:List<BikeProfile>, selected:Int, busy:Boolean) {
        profiles=rows;active=selected;locked=busy
        if(shape!=rows.map { it.slot }) {
            shape=rows.map { it.slot };painted="";row.removeAllViews()
            rows.forEach { p -> row.addView(card(p),if(compact) GridLayout.LayoutParams().apply {
                width=0;height=dp(BicycleDesign.bikeTileHeight)
            } else LinearLayout.LayoutParams(0,dp(BicycleDesign.bikeTileHeight))) }
            if(!compact && rows.size<BikeProfileStore.MAX_COUNT) row.addView(card(null),LinearLayout.LayoutParams(0,dp(BicycleDesign.bikeTileHeight)))
            requestLayout()
        }
        paint()
    }
    private fun card(profile:BikeProfile?)=LinearLayout(context).apply {
        orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
        tag=profile?.let { "bike_profile_${it.slot}" } ?: "bike_profile_add"
        setPadding(dp(BicycleDesign.smallGap),dp(BicycleDesign.verticalPadding),dp(BicycleDesign.smallGap),dp(BicycleDesign.verticalPadding))
        isClickable=true;isFocusable=true
        if(profile!=null) {
            addView(BicycleComponents.text(context,themes,"").apply { gravity=Gravity.CENTER;textSize=BicycleDesign.bodySp*scale;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(-1,-2))
            addView(ImageView(context).apply { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(-1,dp(BicycleDesign.bikePreviewHeight)))
            addView(BicycleComponents.text(context,themes,"").apply { gravity=Gravity.CENTER;textSize=BicycleDesign.bodySp*scale;setLines(2);ellipsize=android.text.TextUtils.TruncateAt.END;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(-1,-2))
            setOnClickListener { if(!locked) select(profile.slot) }
            reorder?.bind(this,profile.slot)
        } else {
            addView(ImageView(context).apply { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(dp(BicycleDesign.touchSize),dp(BicycleDesign.touchSize)))
            addView(BicycleComponents.text(context,themes,context.getString(R.string.bikes_new)).apply { gravity=Gravity.CENTER;textSize=BicycleDesign.bodySp*scale;setLines(2);importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(-1,-2))
            setOnClickListener { if(!locked) add() }
        }
    }
    private fun paint() {
        val tokens=themes.active.tokens
        val signature="$active:$locked:${tokens.hashCode()}:"+profiles.joinToString { "${it.slot}:${it.name}:${it.type}:${it.category}" }
        if(signature==painted) return
        painted=signature
        for(index in 0 until row.childCount) {
            val card=row.getChildAt(index) as LinearLayout
            val p=profiles.getOrNull(index);val selected=p?.slot==active
            card.isEnabled=!locked;card.alpha=if(locked) .4f else 1f;card.isSelected=selected
            card.contentDescription=if(p==null) context.getString(R.string.bikes_new) else if(compact) context.getString(R.string.ui_source_field_description,context.getString(p.type.label),p.name) else p.name
            card.tooltipText=if(compact) p?.name else if(p!=null) context.getString(R.string.bikes_move_hint) else null
            card.stateDescription=context.getString(if(selected) R.string.bikes_selected else R.string.ui_not_selected)
            card.background=GradientDrawable().apply {
                setColor(if(selected) tokens.tertiary else tokens.secondary)
                cornerRadius=dp(BicycleDesign.inputRadius).toFloat()
                val color=BicycleComponents.selectionBorder(tokens,selected)
                if(p==null) setStroke(BicycleDesign.linePixels(context),color,dp(BicycleDesign.smallGap).toFloat(),dp(BicycleDesign.smallGap).toFloat())
                else setStroke(if(selected) dp(2) else BicycleDesign.linePixels(context),color)
            }
            if(p!=null) {
                (card.getChildAt(0) as TextView).text=context.getString(p.type.label)
                (card.getChildAt(1) as ImageView).setImageDrawable(BikePreview(p.category).apply { setTint(tokens.foreground) })
                (card.getChildAt(2) as TextView).text=p.name
            } else (card.getChildAt(0) as ImageView).setImageDrawable(BicycleIcons.drawable(context,BicycleIcon.AddSensor,tokens.foreground))
        }
    }
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        val available=MeasureSpec.getSize(widthMeasureSpec)-paddingLeft-paddingRight
        val gap=dp(BicycleDesign.controlGap)
        val visibleTiles=if(compact) BicycleDesign.bikeQuickVisibleTiles else BicycleDesign.bikeVisibleTiles
        val cardWidth=((available-gap*(visibleTiles-1))/visibleTiles).coerceAtLeast(1)
        for(index in 0 until row.childCount) {
            val card=row.getChildAt(index)
            val params=card.layoutParams as ViewGroup.MarginLayoutParams
            val start=if(if(compact) index%visibleTiles==0 else index==0) 0 else gap
            val top=if(compact && index>=visibleTiles) gap else 0
            if(params.width!=cardWidth || params.marginStart!=start || params.leftMargin!=start || params.topMargin!=top) {
                params.width=cardWidth;params.marginStart=start;params.marginEnd=0;params.leftMargin=start;params.rightMargin=0;params.topMargin=top
                if(params is GridLayout.LayoutParams) {
                    params.rowSpec=GridLayout.spec(index/visibleTiles)
                    params.columnSpec=GridLayout.spec(index%visibleTiles)
                }
                card.layoutParams=params
            }
        }
        super.onMeasure(widthMeasureSpec,heightMeasureSpec)
    }
    fun revealActive() {
        post {
            val index=profiles.indexOfFirst { it.slot==active }
            if(index>=0) { val card=row.getChildAt(index);if(card.left<scrollX) smoothScrollTo(card.left,0) else if(card.right>scrollX+width) smoothScrollTo(card.right-width,0) }
        }
    }
}
