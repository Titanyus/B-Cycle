package de.bcycle.app

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

/** Profile-only main level: connected panel, inset content and scoped sticky categories. */
internal class ProfileAccordion(context:Context,themes:ThemeManager,title:String,key:String,
    icon:BicycleIcon,body:View,initiallyOpen:Boolean,private val changed:(Boolean)->Unit) : LinearLayout(context) {
    private val frame=FrameLayout(context)
    private val end=View(context).apply { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
    private val chevron=ImageView(context).apply { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
    val header=LinearLayout(context)
    var isOpen=initiallyOpen;private set
    private fun dp(n:Int)=BicycleDesign.dp(context,n)
    init {
        orientation=VERTICAL;tag="profile_group_$key"
        header.apply {
            tag="profile_accordion_$key";orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
            minimumHeight=dp(BicycleDesign.profileAccordionHeaderHeight)
            isClickable=true;isFocusable=true;isAccessibilityHeading=true;contentDescription=title
            accessibilityDelegate=object:View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host:View,info:AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host,info);info.className="android.widget.Button"
                }
            }
            val lead=View(context).apply { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
            addView(lead,LayoutParams(dp(BicycleDesign.profileAccordionLeadingWidth),-1))
            addView(ImageView(context).apply {
                importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,icon,it.foreground)) }
            },LayoutParams(dp(BicycleDesign.settingsIconSize),dp(BicycleDesign.settingsIconSize)).apply {
                marginStart=dp(BicycleDesign.horizontalPadding);marginEnd=dp(BicycleDesign.controlGap)
            })
            addView(BicycleComponents.text(context,themes,title,true).apply {
                importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                gravity=Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(0,dp(BicycleDesign.verticalPadding),0,dp(BicycleDesign.verticalPadding))
            },LayoutParams(0,-2,1f))
            addView(chevron,LayoutParams(dp(BicycleDesign.iconSize),dp(BicycleDesign.iconSize)).apply {
                marginStart=dp(BicycleDesign.controlGap);marginEnd=dp(BicycleDesign.horizontalPadding)
            })
            clipToOutline=true
            BicycleComponents.bindTheme(this,themes) { tokens ->
                background=ProfileAccordionHeader(tokens.tertiary,tokens.foreground,
                    dp(BicycleDesign.profileAccordionRadius).toFloat(),dp(BicycleDesign.profileAccordionLeadingWidth).toFloat())
                chevron.setImageDrawable(BicycleIcons.drawable(context,BicycleIcon.Expand,tokens.foreground))
            }
            setOnClickListener { show(!isOpen);changed(isOpen) }
        }
        frame.apply {
            tag="profile_content_$key"
            // Draw the body behind the overlapping rounded header, never over it.
            translationZ=-1f;clipToPadding=false
            setPadding(dp(BicycleDesign.sectionHeaderPadding),dp(BicycleDesign.profileAccordionRadius+BicycleDesign.controlGap),dp(BicycleDesign.sectionHeaderPadding),dp(BicycleDesign.sectionHeaderPadding))
            addView(body,FrameLayout.LayoutParams(-1,-2))
            (body as? ViewGroup)?.apply { clipChildren=false;clipToPadding=false }
            BicycleComponents.bindTheme(this,themes) { tokens ->
                setBackgroundColor(tokens.secondary)
            }
        }
        addView(header,LayoutParams(-1,-2))
        addView(frame,LayoutParams(-1,-2).apply { marginStart=dp(BicycleDesign.profileAccordionBodyInset);topMargin=-dp(BicycleDesign.profileAccordionRadius) })
        addView(end,LayoutParams(-1,0))
        fun scope(view:View) {
            if(view is BicycleSectionHeader) view.sectionEnd=end
            if(view is ViewGroup) for(i in 0 until view.childCount) scope(view.getChildAt(i))
        }
        scope(body)
        show(initiallyOpen)
    }
    /** The parent draws one continuous axis over the body, gap and adjoining header curves. */
    val connectionX get()=dp(BicycleDesign.profileAccordionBodyInset)+BicycleDesign.linePixels(context)/2f
    fun connectionY(bottom:Boolean):Float {
        val leading=dp(BicycleDesign.profileAccordionLeadingWidth)/2f
        val radius=dp(BicycleDesign.profileAccordionRadius).toFloat()
        val t=1f-kotlin.math.sqrt(((connectionX-leading)/(radius-leading)).coerceIn(0f,1f))
        val y=leading+(radius-leading)*t*t
        return header.top+if(bottom) header.height-y else y
    }
    private fun show(open:Boolean) {
        isOpen=open;frame.visibility=if(open) VISIBLE else GONE
        // Keep the inner visibility contract used by existing profile/FIT controls.
        frame.getChildAt(0).visibility=if(open) VISIBLE else GONE
        header.stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
        chevron.rotation=if(open) 180f else 0f
    }
}

/** Rounded fill and a curved leading accent; deliberately no surrounding outline. */
private class ProfileAccordionHeader(private val fill:Int,private val foreground:Int,
    private val radius:Float,private val leading:Float):android.graphics.drawable.Drawable() {
    private val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val rect=android.graphics.RectF()
    private val accent=android.graphics.Path()
    private var opacity=255
    override fun onBoundsChange(bounds:android.graphics.Rect) {
        rect.set(bounds)
        val x=bounds.left+leading/2;val top=bounds.top+leading/2;val bottom=bounds.bottom-leading/2
        accent.reset();accent.moveTo(bounds.left+radius,top)
        accent.quadTo(x,top,x,bounds.top+radius)
        accent.lineTo(x,bounds.bottom-radius)
        accent.quadTo(x,bottom,bounds.left+radius,bottom)
    }
    override fun draw(canvas:android.graphics.Canvas) {
        fun color(value:Int) { paint.color=value;paint.alpha=android.graphics.Color.alpha(value)*opacity/255 }
        paint.style=android.graphics.Paint.Style.FILL;color(fill)
        canvas.drawRoundRect(rect,radius,radius,paint)
        paint.style=android.graphics.Paint.Style.STROKE
        paint.strokeWidth=leading;paint.strokeCap=android.graphics.Paint.Cap.ROUND;color(foreground)
        canvas.drawPath(accent,paint)
    }
    override fun getOutline(outline:android.graphics.Outline) { outline.setRoundRect(bounds,radius) }
    override fun setAlpha(alpha:Int) { opacity=alpha;invalidateSelf() }
    override fun setColorFilter(filter:android.graphics.ColorFilter?) { paint.colorFilter=filter;invalidateSelf() }
    @Deprecated("Deprecated in Java") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}
