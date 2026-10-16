package de.bcycle.app

import android.content.ClipData
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Three equal-width cards in a native horizontal scroller; IDs never depend on order. */
internal class BikeProfileStrip(context: Context, private val themes: ThemeManager,
    private val select: (Int) -> Unit, private val add: () -> Unit,
    private val move: (Int, Int) -> Boolean) : HorizontalScrollView(context) {
    private data class Drag(val owner: BikeProfileStrip, val slot: Int)
    private val row = LinearLayout(context).apply { orientation=LinearLayout.HORIZONTAL;isBaselineAligned=false }
    private var profiles = emptyList<BikeProfile>()
    private var painted = ""
    private var active = -1
    private var locked = false
    private var shape = emptyList<Int>()
    private var drag: Drag? = null
    private var edgeDirection = 0
    private val edgeScroll = object : Runnable {
        override fun run() {
            if (drag==null || edgeDirection==0 || locked) return
            scrollBy(dp(4)*edgeDirection,0)
            postDelayed(this,16)
        }
    }
    private fun dp(n:Int)=BicycleDesign.dp(context,n)
    init {
        tag="bike_profile_strip";isHorizontalScrollBarEnabled=false;isFillViewport=false
        addView(row,LayoutParams(-2,-2))
        row.setOnDragListener { _, event ->
            val payload=event.localState as? Drag
            if(payload?.owner!==this || locked) return@setOnDragListener false
            when(event.action) {
                DragEvent.ACTION_DRAG_STARTED -> { drag=payload;true }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    val x=event.x-scrollX
                    val direction=when { x<dp(32)->-1; x>width-dp(32)->1; else->0 }
                    if(direction!=edgeDirection) {
                        edgeDirection=direction;removeCallbacks(edgeScroll)
                        if(direction!=0) post(edgeScroll)
                    }
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> { edgeDirection=0;removeCallbacks(edgeScroll);true }
                DragEvent.ACTION_DROP -> {
                    edgeDirection=0;removeCallbacks(edgeScroll)
                    val target=profiles.indices.minByOrNull { index ->
                        val card=row.getChildAt(index)
                        kotlin.math.abs(event.x-(card.left+card.right)/2f)
                    }
                    target!=null && move(payload.slot,target)
                }
                DragEvent.ACTION_DRAG_ENDED -> { drag=null;edgeDirection=0;removeCallbacks(edgeScroll);true }
                else -> true
            }
        }
        BicycleComponents.bindTheme(this,themes) { paint() }
    }
    override fun onDetachedFromWindow() {
        removeCallbacks(edgeScroll);edgeDirection=0;drag=null
        super.onDetachedFromWindow()
    }
    fun show(rows:List<BikeProfile>, selected:Int, busy:Boolean) {
        profiles=rows;active=selected;locked=busy
        if(shape!=rows.map { it.slot }) {
            shape=rows.map { it.slot };painted="";row.removeAllViews()
            rows.forEach { p -> row.addView(card(p),LinearLayout.LayoutParams(0,dp(BicycleDesign.bikeTileHeight))) }
            if(rows.size<BikeProfileStore.MAX_COUNT) row.addView(card(null),LinearLayout.LayoutParams(0,dp(BicycleDesign.bikeTileHeight)))
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
            addView(BicycleComponents.text(context,themes,"").apply { gravity=Gravity.CENTER;maxLines=1;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(-1,-2))
            addView(ImageView(context).apply { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(-1,dp(BicycleDesign.bikePreviewHeight)))
            addView(BicycleComponents.text(context,themes,"").apply { gravity=Gravity.CENTER;setLines(2);ellipsize=android.text.TextUtils.TruncateAt.END;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(-1,-2))
            setOnClickListener { if(!locked) select(profile.slot) }
            setOnLongClickListener {
                if(locked || profiles.size<2) false else startDragAndDrop(ClipData.newPlainText("", ""),View.DragShadowBuilder(this),Drag(this@BikeProfileStrip,profile.slot),0)
            }
            accessibilityDelegate=object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host:View, info:AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host,info)
                    val index=profiles.indexOfFirst { it.slot==profile.slot }
                    if(!locked && index>0) info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.bike_move_left,context.getString(R.string.bikes_move_left)))
                    if(!locked && index in 0 until profiles.lastIndex) info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.bike_move_right,context.getString(R.string.bikes_move_right)))
                }
                override fun performAccessibilityAction(host:View, action:Int, args:android.os.Bundle?):Boolean {
                    val index=profiles.indexOfFirst { it.slot==profile.slot }
                    return when(action) {
                        R.id.bike_move_left -> !locked && move(profile.slot,index-1)
                        R.id.bike_move_right -> !locked && move(profile.slot,index+1)
                        else -> super.performAccessibilityAction(host,action,args)
                    }
                }
            }
        } else {
            addView(ImageView(context).apply { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(dp(BicycleDesign.touchSize),dp(BicycleDesign.touchSize)))
            addView(BicycleComponents.text(context,themes,context.getString(R.string.bikes_new)).apply { gravity=Gravity.CENTER;setLines(2);importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(-1,-2))
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
            card.contentDescription=if(p==null) context.getString(R.string.bikes_new) else p.name
            card.tooltipText=if(p!=null) context.getString(R.string.bikes_move_hint) else null
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
        val cardWidth=((available-gap*(BicycleDesign.bikeVisibleTiles-1))/BicycleDesign.bikeVisibleTiles).coerceAtLeast(1)
        for(index in 0 until row.childCount) {
            val card=row.getChildAt(index)
            (card.layoutParams as LinearLayout.LayoutParams).apply { width=cardWidth;marginStart=if(index==0) 0 else gap }
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
