package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.widget.*

/** Two independent inline accordions; edits reuse the active profile's map preferences. */
internal class MapLineStyleView(context:Context,private val prefs:SharedPreferences,private val themes:ThemeManager,
    private val navigation:DialogNavigation,private val changed:()->Unit):LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="map_line_styles"
        MapLineKind.entries.forEach { kind ->
            val body=LinearLayout(context).apply { orientation=VERTICAL;visibility=GONE;tag="map_line_body_${kind.id}" }
            lateinit var toggle:Button
            toggle=BicycleComponents.action(context,themes,context.getString(kind.title)) {
                val open=body.visibility==GONE
                body.visibility=if(open) VISIBLE else GONE
                toggle.stateDescription=context.getString(if(open) R.string.zone_expanded else R.string.zone_collapsed)
                BicycleDialogStyle.actionIcon(toggle,themes,BicycleIcon.Route,BicycleIcon.Expand)
            }.apply { tag="map_line_toggle_${kind.id}";stateDescription=context.getString(R.string.zone_collapsed) }
            BicycleDialogStyle.accordion(toggle,themes)
            BicycleDialogStyle.actionIcon(toggle,themes,BicycleIcon.Route,BicycleIcon.Expand)
            fun current()=MapRouteStyles.read(prefs,kind)
            val preview=MapLinePreview(context,prefs,kind).apply { tag="map_line_preview_${kind.id}" }
            body.addView(preview,LayoutParams(-1,BicycleDesign.dp(context,150)))
            val color=BicycleComponents.action(context,themes,context.getString(R.string.map_line_color)) {
                showColor(kind) { preview.invalidate();changed() }
            }.apply { tag="map_line_color_${kind.id}" }
            body.addView(color,LayoutParams(-1,-2))
            preview.isFocusable=true;preview.setOnClickListener { color.performClick() }
            addSlider(body,R.string.map_line_width,15,(current().widthDp*2-1).toInt(),"map_line_width_${kind.id}",
                { context.getString(R.string.map_line_width_value,(it+1)/2f) }) { value ->
                MapRouteStyles.write(prefs,kind,current().copy(widthDp=(value+1)/2f));preview.invalidate();changed()
            }
            addView(toggle,LayoutParams(-1,-2));addView(body,LayoutParams(-1,-2))
        }
    }
    private fun addSlider(parent:LinearLayout,title:Int,maximum:Int,initial:Int,name:String,
        format:(Int)->String,change:(Int)->Unit):SeekBar {
        val row=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL;orientation=HORIZONTAL }
        val label=BicycleComponents.text(context,themes,context.getString(title))
        row.addView(label,LayoutParams(0,-2,1f))
        val value=BicycleComponents.text(context,themes,format(initial)).apply { gravity=Gravity.END }
        row.addView(value,LayoutParams(-2,-2));parent.addView(row,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,8) })
        return SeekBar(context).apply {
            max=maximum;progress=initial;keyProgressIncrement=1;tag=name;contentDescription=context.getString(title)
            minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
            BicycleComponents.bindTheme(this,themes) { tokens ->
                progressTintList=android.content.res.ColorStateList.valueOf(tokens.accent)
                thumbTintList=android.content.res.ColorStateList.valueOf(tokens.foreground)
            }
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,p:Int,user:Boolean) { value.text=format(p);if(user) change(p) }
            })
            parent.addView(this,LayoutParams(-1,-2))
        }
    }
    private fun showColor(kind:MapLineKind,update:()->Unit) {
        val root=BicycleComponents.dialogBody(context,themes)
        val preview=MapLinePreview(context,prefs,kind)
        root.addView(preview,LayoutParams(-1,BicycleDesign.dp(context,150)))
        val hsv=FloatArray(3);Color.colorToHSV(MapRouteStyles.read(prefs,kind).color,hsv)
        val hex=BicycleComponents.input(context,themes).apply {
            tag="map_line_hex";hint=context.getString(R.string.map_line_hex);setSingleLine()
            filters=arrayOf(InputFilter.LengthFilter(7));setText(String.format(java.util.Locale.ROOT,"#%06X",MapRouteStyles.read(prefs,kind).color and 0xffffff))
        }
        val sliders=mutableListOf<SeekBar>();var updating=false
        fun refreshHex() { updating=true;hex.setText(String.format(java.util.Locale.ROOT,"#%06X",MapRouteStyles.read(prefs,kind).color and 0xffffff));updating=false }
        fun saveColor() {
            MapRouteStyles.write(prefs,kind,MapRouteStyles.read(prefs,kind).copy(color=Color.HSVToColor(hsv)))
            refreshHex();preview.invalidate();update()
        }
        listOf(R.string.map_line_hue,R.string.map_line_saturation,R.string.map_line_brightness).forEachIndexed { i,title ->
            sliders+=addSlider(root,title,if(i==0) 360 else 100,if(i==0) hsv[i].toInt() else (hsv[i]*100).toInt(),"map_line_hsv_$i",{ it.toString() }) { p ->
                hsv[i]=if(i==0) p.toFloat() else p/100f;saveColor()
            }
        }
        addSlider(root,R.string.map_line_opacity,100,MapRouteStyles.read(prefs,kind).opacity,"map_line_opacity",
            { context.getString(R.string.map_line_percent,it) }) { p ->
            MapRouteStyles.write(prefs,kind,MapRouteStyles.read(prefs,kind).copy(opacity=p));preview.invalidate();update()
        }
        root.addView(hex,LayoutParams(-1,-2))
        hex.onTextChanged {
            if(!updating) {
                val text=hex.text.toString().removePrefix("#")
                if(text.matches(Regex("[0-9a-fA-F]{6}"))) {
                    hex.error=null
                    val color=text.toLong(16).toInt() or Color.BLACK
                    Color.colorToHSV(color,hsv)
                    sliders.forEachIndexed { i,bar -> bar.progress=if(i==0) hsv[i].toInt() else (hsv[i]*100).toInt() }
                    MapRouteStyles.write(prefs,kind,MapRouteStyles.read(prefs,kind).copy(color=color));preview.invalidate();update()
                } else hex.error=context.getString(R.string.map_line_hex_error)
            }
        }
        BicycleDialogBuilder(context,themes,navigation).setTitle(R.string.map_line_color)
            .setView(BicycleStickyScrollView(context).apply { addView(root) }).show()
    }
}

/** One bundled raster tile, never a live map or network request. Dark preview uses the map's palette. */
internal class MapLinePreview(context:Context,private val prefs:SharedPreferences,private val kind:MapLineKind):View(context) {
    companion object {
        private var cached:Pair<Bitmap,Bitmap>?=null
        @Synchronized private fun images(resources:android.content.res.Resources):Pair<Bitmap,Bitmap> {
            cached?.let { return it }
            val light=BitmapFactory.decodeResource(resources,R.drawable.map_style_preview)
            val dark=light.copy(Bitmap.Config.ARGB_8888,true).apply {
                val pixels=IntArray(width*height);getPixels(pixels,0,width,0,0,width,height)
                pixels.indices.forEach { pixels[it]=MapNightPalette.color(pixels[it]) }
                setPixels(pixels,0,width,0,0,width,height)
            }
            return (light to dark).also { cached=it }
        }
    }
    private val pictures=images(resources)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path=Path()
    init { contentDescription=context.getString(R.string.map_line_preview,context.getString(kind.title)) }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val chosen=if(prefs.getBoolean("mapNight",false)) pictures.second else pictures.first
        val cropHeight=(chosen.width*height.toFloat()/width.coerceAtLeast(1)).toInt().coerceIn(1,chosen.height)
        val top=(chosen.height-cropHeight)/2
        paint.style=Paint.Style.FILL;paint.color=Color.WHITE
        canvas.drawBitmap(chosen,Rect(0,top,chosen.width,top+cropHeight),Rect(0,0,width,height),paint)
        val style=MapRouteStyles.read(prefs,kind);val density=resources.displayMetrics.density
        path.reset();path.moveTo(width*.08f,height*.7f)
        path.cubicTo(width*.25f,height*.8f,width*.3f,height*.22f,width*.5f,height*.4f)
        path.cubicTo(width*.65f,height*.6f,width*.72f,height*.15f,width*.92f,height*.28f)
        paint.style=Paint.Style.STROKE;paint.strokeCap=Paint.Cap.ROUND;paint.strokeJoin=Paint.Join.ROUND
        val group=canvas.saveLayerAlpha(null,Color.alpha(style.argb))
        if(kind==MapLineKind.RECORDED) {
            paint.color=Color.WHITE;paint.strokeWidth=(style.widthDp+2)*density;canvas.drawPath(path,paint)
        }
        paint.color=style.color;paint.strokeWidth=style.widthDp*density;canvas.drawPath(path,paint)
        canvas.restoreToCount(group)
        paint.style=Paint.Style.FILL;paint.color=Color.argb(210,255,255,255);canvas.drawRect(0f,height-18*density,width.toFloat(),height.toFloat(),paint)
        paint.color=Color.BLACK;paint.textSize=10*density
        canvas.drawText(context.getString(R.string.map_line_credit),4*density,height-5*density,paint)
    }
}
