package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.view.View
import android.widget.TextView

/** Same renderer in light dashboard fields and dark map fields; no sensor reads or timers. */
internal class BatteryGridRenderer(private val context:Context, private val foreground:Int) {
    private val density=context.resources.displayMetrics.density
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val icons=mutableMapOf<BatterySource,android.graphics.drawable.Drawable>()
    private val clip=Path()
    private val font=BicycleDesign.narrowTypeface(context)
    fun draw(canvas:Canvas,bounds:RectF,sources:List<BatterySource>,snapshot:BatterySnapshot) {
        if(bounds.width()<=0 || bounds.height()<=0 || sources.isEmpty()) return
        val columns=if(sources.size==1) 1 else 2
        val rows=(sources.size+columns-1)/columns
        val cellWidth=bounds.width()/columns
        val scale=minOf(cellWidth/(48*density),bounds.height()/(rows*24*density),1.9f)*.9f
        if(scale<=0) return
        val u=density*scale
        val rowHeight=24*u
        val top=bounds.centerY()-rows*rowHeight/2
        sources.forEachIndexed { index,source ->
            val left=bounds.left+(index%columns)*cellWidth+(cellWidth-46*u)/2
            val center=top+(index/columns+.5f)*rowHeight
            val level=snapshot.level(source)
            val color=when(BatteryFields.warning(level)) { 2->if(foreground==Color.BLACK) 0xffa51c1c.toInt() else 0xffff3030.toInt();1->if(foreground==Color.BLACK) 0xff8c5700.toInt() else 0xffffb000.toInt();else->foreground }
            icons.getOrPut(source) { BicycleIcons.drawable(context,BatteryFields.icon(source),foreground) }.apply { setBounds(left.toInt(),(center-7*u).toInt(),(left+14*u).toInt(),(center+7*u).toInt());draw(canvas) }
            val bar=RectF(left+17*u,center-8*u,left+23*u,center+8*u)
            paint.style=Paint.Style.FILL;paint.color=(foreground and 0x00ffffff) or 0x33000000
            canvas.drawRoundRect(bar,2*u,2*u,paint)
            if(level!=null && level>0) {
                paint.color=color
                canvas.save();canvas.clipRoundRectCompat(bar,2*u)
                canvas.drawRect(bar.left,bar.bottom-bar.height()*level/100f,bar.right,bar.bottom,paint);canvas.restore()
            }
            paint.style=Paint.Style.STROKE;paint.strokeWidth=BicycleDesign.dividerDp*density
            paint.color=(foreground and 0x00ffffff) or 0x88000000.toInt()
            canvas.drawRoundRect(bar,2*u,2*u,paint)
            paint.style=Paint.Style.FILL;paint.typeface=font;paint.textSize=12*u
            paint.color=color;paint.textAlign=Paint.Align.LEFT
            canvas.drawText(level?.let { context.getString(R.string.battery_percent_compact,it) } ?: context.getString(R.string.battery_missing),left+26*u,center-(paint.ascent()+paint.descent())/2,paint)
        }
    }
    private fun Canvas.clipRoundRectCompat(rect:RectF,radius:Float) {
        clip.reset();clip.addRoundRect(rect,radius,radius,Path.Direction.CW);clipPath(clip)
    }
}

internal class BatteryGridView(context:Context):View(context) {
    private val renderer by lazy { BatteryGridRenderer(context,Color.WHITE) }
    private var sources=emptyList<BatterySource>()
    private var snapshot=BatterySnapshot()
    init { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO }
    fun show(sources:List<BatterySource>,snapshot:BatterySnapshot) {
        if(this.sources==sources && this.snapshot==snapshot) return
        this.sources=sources;this.snapshot=snapshot;invalidate()
    }
    override fun onDraw(canvas:Canvas) { super.onDraw(canvas);renderer.draw(canvas,RectF(0f,0f,width.toFloat(),height.toFloat()),sources,snapshot) }
}

/** Preserves map field TextViews, their editor hit targets and record/light status marks. */
internal class BatteryMapTextView(context:Context):TextView(context) {
    private val renderer by lazy { BatteryGridRenderer(context,Color.BLACK) }
    private var sources=emptyList<BatterySource>()
    private var snapshot=BatterySnapshot()
    private var status=false
    fun batteries(sources:List<BatterySource>,snapshot:BatterySnapshot,status:Boolean) {
        if(this.sources==sources && this.snapshot==snapshot && this.status==status) return
        this.sources=sources;this.snapshot=snapshot;this.status=status;invalidate()
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        if(sources.isNotEmpty()) renderer.draw(canvas,RectF(paddingLeft.toFloat(),if(status) 24*resources.displayMetrics.density else 0f,
            (width-paddingRight).toFloat(),height.toFloat()),sources,snapshot)
    }
}
