package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View

/** Same glyphs in the dashboard, slot cards and preview. No click targets in the status strip. */
internal class DashboardStatusDrawing(private val context:Context) {
    val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=BicycleDesign.bodyTypeface }
    private val rect=RectF()
    private val bell=BicycleIcons.drawable(context,BicycleIcon.Notifications,android.graphics.Color.WHITE)
    var messenger:Drawable?=null
    private val density=context.resources.displayMetrics.density
    private fun batteryText(state:DashboardStatusSnapshot)=state.battery?.takeIf { it in 0..100 }?.let { context.getString(R.string.dashboard_status_percent,it) }
        ?: context.getString(R.string.hrvt_unavailable)
    fun width(item:DashboardStatusItem,state:DashboardStatusSnapshot):Float=when(item) {
        DashboardStatusItem.TIME->paint.measureText(state.time)
        DashboardStatusItem.VPN->paint.measureText("VPN")
        DashboardStatusItem.NOTIFICATIONS->16*density+if(state.notifications>0) 3*density+paint.measureText(state.notifications.coerceAtMost(99).toString()+if(state.notifications>99) "+" else "") else 0f
        DashboardStatusItem.BATTERY->25*density+paint.measureText(batteryText(state))
        DashboardStatusItem.CELLULAR->17*density
        DashboardStatusItem.EMPTY->0f
    }
    fun draw(canvas:Canvas,item:DashboardStatusItem,state:DashboardStatusSnapshot,x:Float,center:Float,color:Int) {
        paint.color=color;paint.style=Paint.Style.FILL;paint.strokeWidth=density
        val baseline=center-(paint.ascent()+paint.descent())/2
        when(item) {
            DashboardStatusItem.TIME->canvas.drawText(state.time,x,baseline,paint)
            DashboardStatusItem.VPN->canvas.drawText("VPN",x,baseline,paint)
            DashboardStatusItem.NOTIFICATIONS->{
                val icon=messenger ?: bell.apply { setTint(color) }
                icon.setBounds(x.toInt(),(center-8*density).toInt(),(x+16*density).toInt(),(center+8*density).toInt());icon.draw(canvas)
                if(state.notifications>0) canvas.drawText(state.notifications.coerceAtMost(99).toString()+if(state.notifications>99) "+" else "",x+19*density,baseline,paint)
            }
            DashboardStatusItem.BATTERY->{
                rect.set(x,center-5*density,x+18*density,center+5*density)
                paint.style=Paint.Style.STROKE;canvas.drawRoundRect(rect,2*density,2*density,paint);paint.style=Paint.Style.FILL
                canvas.drawRect(x+19*density,center-2*density,x+21*density,center+2*density,paint)
                val level=state.battery
                if(level==null) canvas.drawRect(x+6*density,center-.5f*density,x+12*density,center+.5f*density,paint)
                else canvas.drawRect(x+2*density,center-3*density,x+(2+14*level.coerceIn(0,100)/100f)*density,center+3*density,paint)
                canvas.drawText(batteryText(state),x+25*density,baseline,paint)
            }
            DashboardStatusItem.CELLULAR->{
                for(i in 0..3) {
                    paint.alpha=if(state.cellular!=null && i<state.cellular) 255 else 65
                    canvas.drawRect(x+i*4*density,center+6*density-(i+1)*3*density,x+(i*4+2)*density,center+6*density,paint)
                }
                paint.alpha=255
                if(state.cellular==null) canvas.drawText("?",x+6*density,baseline,paint)
            }
            DashboardStatusItem.EMPTY->Unit
        }
    }
}

internal class DashboardStatusStrip(context:Context,themes:ThemeManager,private val preview:Boolean=false):View(context) {
    private val drawing=DashboardStatusDrawing(context)
    private var inkColor=themes.active.tokens.foreground
    var config=DashboardStatusConfig();set(value) { field=value;describe();invalidate() }
    private var state=DashboardStatusSnapshot()
    private var messengerPackage:String?=null
    private var cutouts:List<Rect> = emptyList()
    private var safeLeft=0;private var safeRight=0
    private val density=resources.displayMetrics.density
    private val rect=RectF()
    init {
        tag=if(preview) "dashboard_status_preview" else "dashboard_status_strip"
        importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES
        BicycleComponents.bindTheme(this,themes) { inkColor=it.foreground;invalidate() }
        reloadMessenger()
    }
    fun reloadMessenger() {
        val selected=MessageNotificationSettings(context).orderedPackages.firstOrNull()
        if(selected!=messengerPackage) {
            messengerPackage=selected;drawing.messenger=selected?.let { runCatching { context.packageManager.getApplicationIcon(it).mutate() }.getOrNull() };invalidate()
        }
    }
    fun insets(left:Int,right:Int,blocked:List<Rect>) { safeLeft=left;safeRight=right;cutouts=blocked.map(::Rect);invalidate() }
    fun show(value:DashboardStatusSnapshot) {
        if(value==state) return
        state=value;describe();invalidate()
    }
    private fun describe() {
        val value=state
        contentDescription=config.slots.filter { visible(it,value) }.joinToString(", ") {
            context.getString(it.label)+when(it) {
                DashboardStatusItem.TIME->" "+value.time
                DashboardStatusItem.NOTIFICATIONS->" "+value.notifications
                DashboardStatusItem.BATTERY->value.battery?.let { number -> " $number%" }.orEmpty()
                DashboardStatusItem.CELLULAR->value.cellular?.let { level -> " $level/4" }.orEmpty()
                else->""
            }
        }
    }
    private fun visible(item:DashboardStatusItem,s:DashboardStatusSnapshot)=when(item) {
        DashboardStatusItem.EMPTY->false
        DashboardStatusItem.VPN->s.vpn || preview
        DashboardStatusItem.NOTIFICATIONS->s.notifications>0 || preview
        else->true
    }
    /** Largest safe interval in each half also handles off-centre camera holes. */
    internal fun regions():List<Pair<Float,Float>> {
        val gap=8*density;val mid=width/2f
        val blocked=if(preview) listOf(Rect((mid-22*density).toInt(),0,(mid+22*density).toInt(),height)) else cutouts.filter { it.top<height && it.bottom>0 }
        return listOf(safeLeft+gap to mid-gap,mid+gap to width-safeRight-gap).mapIndexed { side,region ->
            var free=listOf(region)
            blocked.forEach { r -> free=free.flatMap { (start,end) ->
                if(r.right+gap<=start || r.left-gap>=end) listOf(start to end)
                else listOf(start to minOf(end,r.left-gap),maxOf(start,r.right+gap) to end).filter { it.second>it.first }
            } }
            (if(side==0) free else free.reversed()).maxByOrNull { it.second-it.first } ?: (0f to 0f)
        }
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        drawing.paint.textSize=BicycleDesign.dashboardStatusSp*resources.displayMetrics.scaledDensity
        val center=height/2f
        if(preview) {
            drawing.paint.color=android.graphics.Color.BLACK
            rect.set(width/2f-22*density,0f,width/2f+22*density,height*.55f)
            canvas.drawRoundRect(rect,8*density,8*density,drawing.paint)
        }
        regions().forEachIndexed { side,(left,right) ->
            val items=config.slots.subList(side*3,side*3+3).filter { visible(it,state) }
            if(items.isEmpty() || right<=left) return@forEachIndexed
            val gap=6*density;val widths=items.map { drawing.width(it,state) };val total=widths.sum()+gap*(items.size-1)
            if(total<=0) return@forEachIndexed
            val scale=minOf(1f,(right-left)/total)
            if(scale<.55f) return@forEachIndexed
            val save=canvas.save();canvas.clipRect(left,0f,right,height.toFloat())
            val origin=if(side==0) left else right-total*scale
            canvas.translate(origin,center);canvas.scale(scale,scale)
            var x=0f
            items.forEachIndexed { i,item -> drawing.draw(canvas,item,state,x,0f,inkColor);x+=widths[i]+gap }
            canvas.restoreToCount(save)
        }
    }
}
