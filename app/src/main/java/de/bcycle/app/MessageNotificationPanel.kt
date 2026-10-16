package de.bcycle.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*

/** In-app dashboard card, never a system overlay. At most eight pending messages, memory only. */
internal class MessageNotificationPanel(context: Context, private val themes: ThemeManager,
    private val maximumHeight: ()->Int): LinearLayout(context) {
    private val timer=Handler(Looper.getMainLooper())
    private val pending=java.util.ArrayDeque<IncomingMessage>()
    private val icon=ImageView(context)
    private val sender=BicycleComponents.text(context,themes,"").apply {
        typeface=BicycleDesign.strongTypeface; textSize=BicycleDesign.bodySp
    }
    private val body=BicycleComponents.text(context,themes,"").apply {
        textSize=BicycleDesign.bodySp;setTextIsSelectable(true)
    }
    private val scroll=ScrollView(context).apply { addView(body);BicycleComponents.scrollStyle(this) }
    private val timeout=Runnable { showNext() }
    var positionChanged: ()->Unit = {}
    internal var current: IncomingMessage?=null;private set
    init {
        tag="message_notification_panel";orientation=VERTICAL;visibility=GONE
        isClickable=true;accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE
        val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
        setPadding(gap,gap,gap,gap)
        elevation=BicycleDesign.dp(context,8).toFloat()
        BicycleComponents.bindTheme(this,themes) { t ->
            background=GradientDrawable().apply {
                setColor(t.secondary);cornerRadius=BicycleDesign.dp(context,BicycleDesign.inputRadius).toFloat()
                setStroke(BicycleDesign.dp(context,3),t.accent)
            }
        }
        addView(LinearLayout(context).apply {
            gravity=Gravity.CENTER_VERTICAL
            addView(icon,LayoutParams(BicycleDesign.dp(context,32),BicycleDesign.dp(context,32)).apply { marginEnd=gap })
            icon.importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
            sender.tag="message_notification_sender"
            addView(sender,LayoutParams(0,-2,1f))
            addView(BicycleComponents.icon(context,themes,BicycleIcon.Cancel,context.getString(R.string.notifications_dismiss)) { showNext() }
                .apply { tag="message_notification_dismiss" },LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize)))
        },LayoutParams(-1,-2))
        body.tag="message_notification_text"
        addView(scroll,LayoutParams(-1,-2).apply { topMargin=gap })
        scroll.setOnTouchListener { _,event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> timer.removeCallbacks(timeout)
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> schedule()
            }
            false
        }
    }
    fun receive(message: IncomingMessage?) {
        if(message==null) { clear();return }
        positionChanged()
        if(pending.size>=8) pending.removeFirst()
        pending.addLast(message)
        if(current==null) showNext()
    }
    private fun schedule() {
        timer.removeCallbacks(timeout)
        if(current!=null) timer.postDelayed(timeout,MessageNotificationSettings(context).durationSeconds*1000L)
    }
    private fun showNext() {
        timer.removeCallbacks(timeout)
        current=pending.pollFirst()
        val message=current
        if(message==null) { clear();return }
        sender.text=message.sender;body.text=message.text;scroll.scrollTo(0,0)
        val appName=runCatching {
            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(message.packageName,0)).toString()
        }.getOrDefault(message.packageName)
        sender.contentDescription=context.getString(R.string.notifications_sender_accessibility,appName,message.sender)
        icon.setImageDrawable(runCatching { context.packageManager.getApplicationIcon(message.packageName) }.getOrNull()
            ?: BicycleIcons.drawable(context,BicycleIcon.Notifications,themes.active.tokens))
        visibility=VISIBLE;requestLayout();schedule()
    }
    fun clear() {
        timer.removeCallbacksAndMessages(null);pending.clear();current=null
        sender.text="";sender.contentDescription=null;body.text="";icon.setImageDrawable(null);visibility=GONE
    }
    override fun onMeasure(widthMeasureSpec: Int,heightMeasureSpec: Int) {
        val available=maximumHeight().coerceAtLeast(0)
        super.onMeasure(widthMeasureSpec,MeasureSpec.makeMeasureSpec(available,MeasureSpec.AT_MOST))
    }
    override fun onDetachedFromWindow() { clear();super.onDetachedFromWindow() }
}
