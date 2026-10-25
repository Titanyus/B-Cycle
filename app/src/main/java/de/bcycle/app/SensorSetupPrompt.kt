package de.bcycle.app

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/** A scoped dashboard card; it never changes the global dialog/header style. */
internal class SensorSetupPrompt(private val activity:Activity,private val host:FrameLayout,
    private val dashboard:View,themes:ThemeManager,private val later:()->Unit,search:()->Unit):AutoCloseable {
    private val accessibility=dashboard.importantForAccessibility
    private var closed=false
    val showing get()=!closed
    private fun dismiss() { later();close() }
    private val back=OnBackInvokedCallback { dismiss() }
    private val overlay=FrameLayout(activity).apply {
        tag="sensor_setup_prompt";isClickable=true;isFocusableInTouchMode=true
        elevation=BicycleDesign.dp(activity,BicycleDesign.sectionHeaderRaisedZ).toFloat()
        setBackgroundColor(android.graphics.Color.argb(BicycleDesign.hrvPromptDimAlpha,0,0,0))
        val gap=BicycleDesign.dp(activity,BicycleDesign.horizontalPadding)
        setPadding(gap,gap,gap,gap);clipChildren=false;clipToPadding=false
        contentDescription=activity.getString(R.string.sensor_setup_later)
        setOnClickListener { dismiss() }
        val card=object:ScrollView(activity) {
            override fun onMeasure(w:Int,h:Int) {
                val width=minOf(MeasureSpec.getSize(w),BicycleDesign.dp(activity,BicycleDesign.hrvPromptMaxWidth))
                super.onMeasure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),h)
            }
        }.apply {
            tag="sensor_setup_card";isClickable=true
            accessibilityPaneTitle=activity.getString(R.string.sensor_setup_message)
            BicycleComponents.scrollStyle(this)
            BicycleAlertSurface.bind(this,themes,ThemeRole.TERTIARY)
        }
        val content=LinearLayout(activity).apply {
            orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;isClickable=true
            val padding=BicycleDesign.dp(activity,BicycleDesign.hrvPromptPadding)
            setPadding(padding,padding,padding,padding)
            addView(BicycleComponents.text(activity,themes,activity.getString(R.string.sensor_setup_message),true).apply {
                gravity=Gravity.CENTER
            },LinearLayout.LayoutParams(-1,-2))
            addView(BicycleComponents.action(activity,themes,activity.getString(R.string.sensor_setup_search),false) {
                close();search()
            }.apply { tag="sensor_setup_search" },LinearLayout.LayoutParams(-1,-2).apply { topMargin=padding })
            addView(BicycleComponents.textAction(activity,themes,activity.getString(R.string.sensor_setup_later)) { dismiss() }.apply {
                tag="sensor_setup_later"
                BicycleComponents.bindTheme(this,themes) { setTextColor(it.foreground) }
            },LinearLayout.LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(activity,BicycleDesign.controlGap) })
        }
        card.addView(content);addView(card,FrameLayout.LayoutParams(-1,-2,Gravity.CENTER))
    }
    init {
        dashboard.importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        host.addView(overlay,FrameLayout.LayoutParams(-1,-1))
        activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY,back)
        overlay.requestFocus()
    }
    override fun close() {
        if(closed) return
        closed=true
        activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(back)
        host.removeView(overlay);dashboard.importantForAccessibility=accessibility
    }
}
