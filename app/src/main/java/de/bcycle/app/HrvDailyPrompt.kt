package de.bcycle.app

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/** Daily entry is presentation only: the existing start callback retains sensor/HRV guards. */
internal class HrvDailyPrompt(private val activity:Activity,private val host:FrameLayout,
    private val dashboard:View,themes:ThemeManager,private val acknowledge:()->Unit,start:()->Unit):AutoCloseable {
    private val accessibility=dashboard.importantForAccessibility
    private var closed=false
    val showing get()=!closed
    private val back=OnBackInvokedCallback { close() }
    private fun later() { acknowledge();close() }
    private val overlay=FrameLayout(activity).apply {
        tag="hrv_daily_prompt";isClickable=true;isFocusableInTouchMode=true
        elevation=BicycleDesign.dp(activity,BicycleDesign.sectionHeaderRaisedZ).toFloat()
        setBackgroundColor(Color.argb(BicycleDesign.hrvPromptDimAlpha,0,0,0))
        setPadding(dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.horizontalPadding),dp(BicycleDesign.horizontalPadding))
        clipChildren=false;clipToPadding=false
        contentDescription=activity.getString(R.string.hrv_daily_dismiss)
        setOnClickListener { later() }
        addView(HrvDailyCard(activity,themes,::later) { acknowledge();close();start() },FrameLayout.LayoutParams(-1,-2,Gravity.CENTER))
    }
    private fun dp(value:Int)=BicycleDesign.dp(activity,value)
    init {
        dashboard.importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        host.addView(overlay,FrameLayout.LayoutParams(-1,-1))
        activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY,back)
        overlay.requestFocus()
    }
    override fun close() {
        if(closed) return
        closed=true;activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(back)
        host.removeView(overlay);dashboard.importantForAccessibility=accessibility
    }
}

internal class HrvDailyCard(context:Context,themes:ThemeManager,later:()->Unit,start:()->Unit):ScrollView(context) {
    private fun dp(value:Int)=BicycleDesign.dp(context,value)
    override fun onMeasure(w:Int,h:Int) {
        val width=minOf(MeasureSpec.getSize(w),dp(BicycleDesign.hrvPromptMaxWidth))
        super.onMeasure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),h)
    }
    init {
        tag="hrv_daily_card";isClickable=true;clipToOutline=true
        accessibilityPaneTitle=context.getString(R.string.hrv_daily_title)
        elevation=dp(BicycleDesign.hrvPromptShadow).toFloat()
        outlineAmbientShadowColor=Color.BLACK;outlineSpotShadowColor=Color.BLACK
        BicycleComponents.scrollStyle(this)
        BicycleComponents.bindTheme(this,themes) {
            elevation=if(BicycleAppearance.flat(context)) 0f else BicycleDesign.dp(context,BicycleDesign.hrvPromptShadow).toFloat()
            background=GradientDrawable().apply {
                cornerRadius=dp(BicycleDesign.dialogRadius).toFloat();setColor(it.secondary)
                setStroke(dp(BicycleDesign.hrvPromptBorder).coerceAtLeast(1),it.tertiary)
            }
        }
        val body=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL
            setPadding(dp(BicycleDesign.hrvPromptPadding),dp(BicycleDesign.hrvPromptPadding),dp(BicycleDesign.hrvPromptPadding),dp(BicycleDesign.hrvPromptPadding))
            isClickable=true // Taps within the card must never acknowledge the outside dismissal.
        }
        addView(body,LayoutParams(-1,-2))
        body.addView(HrvScreenHeart(context,themes),LinearLayout.LayoutParams(-1,-2))
        body.addView(BicycleComponents.text(context,themes,context.getString(R.string.hrv_daily_title)).apply {
            tag="hrv_daily_title";textSize=BicycleDesign.hrvPromptTitleSp;typeface=BicycleDesign.narrowTypeface(context)
            gravity=Gravity.CENTER;isAccessibilityHeading=true
        },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
        body.addView(BicycleComponents.text(context,themes,context.getString(R.string.hrv_daily_copy)).apply {
            textSize=BicycleDesign.hrvPromptCopySp;gravity=Gravity.CENTER
        },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
        val metrics=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL;tag="hrv_daily_metrics" }
        listOf(BicycleIcon.Heart to R.string.hrv_daily_resting,BicycleIcon.Speed to R.string.hrv_daily_readiness,
            BicycleIcon.SunriseSensor to R.string.hrv_daily_condition).forEach { (icon,label) ->
            metrics.addView(LinearLayout(context).apply {
                orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
                addView(ImageView(context).apply {
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                    BicycleComponents.bindTheme(this,themes) { setImageDrawable(BicycleIcons.drawable(context,icon,it.foreground)) }
                },LinearLayout.LayoutParams(dp(BicycleDesign.iconSize),dp(BicycleDesign.iconSize)))
                addView(BicycleComponents.text(context,themes,context.getString(label)).apply { gravity=Gravity.CENTER },
                    LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
            },LinearLayout.LayoutParams(0,-2,1f))
        }
        body.addView(metrics,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.hrvPromptPadding) })
        body.addView(BicycleComponents.action(context,themes,context.getString(R.string.hrv_daily_now),false,start).apply {
            tag="hrv_daily_start"
        },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.hrvPromptPadding) })
        body.addView(BicycleComponents.textAction(context,themes,context.getString(R.string.hrv_daily_later),later).apply {
            tag="hrv_daily_later"
            BicycleComponents.bindTheme(this,themes) { setTextColor(it.foreground) }
        },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(BicycleDesign.controlGap) })
    }
}

/** Draw directly onto the card's already painted surface: an isolated transparent layer would
 * retain the source's black rectangle. SCREEN makes black neutral and keeps the neon artwork. */
internal class HrvScreenHeart(context:Context,private val themes:ThemeManager?=null):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { blendMode=BlendMode.SCREEN }
    private val image=artwork ?: BitmapFactory.decodeResource(resources,R.drawable.hrv_prompt_heart).also { artwork=it }
    init { tag="hrv_daily_artwork";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO;themes?.let { BicycleComponents.bindTheme(this,it) { invalidate() } } }
    override fun onMeasure(w:Int,h:Int) {
        val width=MeasureSpec.getSize(w)
        setMeasuredDimension(width,resolveSize((width.toFloat()*image.height/image.width).toInt(),h))
    }
    override fun onDraw(canvas:Canvas) {
        canvas.drawBitmap(image,null,RectF(0f,0f,width.toFloat(),height.toFloat()),paint)
    }
    companion object { private var artwork:Bitmap?=null }
}
