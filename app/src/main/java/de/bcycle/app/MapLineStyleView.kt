package de.bcycle.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.widget.*

/** Live recording and three saved-route styles; edits reuse the active profile's map preferences. */
internal class MapLineStyleView(context:Context,private val prefs:SharedPreferences,private val themes:ThemeManager,
    private val navigation:DialogNavigation,
    private val setSlotVisible:(Int,Boolean)->Unit={ slot,on ->
        val slots=MapRouteSlots.read(prefs)
        (if(prefs.getBoolean("mapGpxVisible",true)) slots else slots.all(false)).show(slot,on).write(prefs)
        prefs.edit().putBoolean("mapGpxVisible",true).apply()
    },private val kinds:List<MapLineKind> = MapLineKind.entries.take(4),private val heading:Int?=R.string.map_line_heading,private val changed:()->Unit):LinearLayout(context) {
    init {
        orientation=VERTICAL;tag="map_line_styles"
        heading?.let { addView(BicycleComponents.contentHeading(context,themes,context.getString(it))) }
        val groupedTracks=kinds==MapLineKind.entries.take(4)
        val trackBox=if(groupedTracks) LinearLayout(context).apply {
            orientation=VERTICAL;tag="map_track_style_group";clipToOutline=true
            val border=BicycleDesign.dp(context,BicycleDesign.controlBorder);setPadding(border,border,border,border)
            BicycleComponents.bindTheme(this,themes) { background=BicycleComponents.controlBackground(context,it,it.secondary) }
            this@MapLineStyleView.addView(this,LayoutParams(-1,-2))
        } else null
        kinds.forEach { kind ->
            val slot=MapLineKind.entries.indexOf(kind)-1
            val cyclingKey=when(kind) { MapLineKind.CYCLEWAYS->VtmBaseMap.cyclewaysKey;MapLineKind.CYCLE_ROUTES->MapVectorLayer.CYCLE_ROUTES.key;else->null }
            val visible=Switch(context).apply {
                tag=cyclingKey ?: if(kind==MapLineKind.RECORDED) "mapTrackVisible" else "map_line_visible_${kind.id}"
                isChecked=if(cyclingKey!=null) prefs.getBoolean(cyclingKey,kind==MapLineKind.CYCLEWAYS) else if(kind==MapLineKind.RECORDED) prefs.getBoolean("mapTrackVisible",true)
                    else prefs.getBoolean("mapGpxVisible",true) && MapRouteSlots.read(prefs).visible[slot]
                BicycleDialogStyle.control(this,themes)
                setOnCheckedChangeListener { _,on ->
                    if(cyclingKey!=null) prefs.edit().putBoolean(cyclingKey,on).apply()
                    else if(kind==MapLineKind.RECORDED) prefs.edit().putBoolean("mapTrackVisible",on).apply()
                    else setSlotVisible(slot,on)
                    changed()
                }
            }
            val frame=BicycleFrameDropdown(context,themes,context.getString(kind.title),open=false,trailing=visible,leadingIcon=BicycleIcon.Route).apply {
                tag="map_line_frame_${kind.id}"
                header.tag="map_line_toggle_${kind.id}"
            }
            val body=frame.content.apply { tag="map_line_body_${kind.id}" }
            fun current()=MapRouteStyles.read(prefs,kind)
            val preview=MapLinePreview(context,prefs,kind).apply { tag="map_line_preview_${kind.id}" }
            body.addView(preview,LayoutParams(-1,BicycleDesign.dp(context,150)))
            val colorRow=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL;tag=BicycleDesign.inlineControlRowTag }
            var updating=false
            val hex=BicycleComponents.input(context,themes).apply {
                tag="map_line_hex_${kind.id}";hint=context.getString(R.string.map_line_hex)
                contentDescription=context.getString(R.string.map_line_hex);setSingleLine()
                filters=arrayOf(InputFilter.LengthFilter(7));BicycleForm.outlinedInput(this,themes)
            }
            val swatch=ImageButton(context).apply {
                tag="map_line_swatch_${kind.id}";contentDescription=context.getString(R.string.map_line_choose_color)
            }
            fun colorDisplay() {
                frame.leadingIconColor=current().color
                updating=true;hex.setText(String.format(java.util.Locale.ROOT,"#%06X",current().color and 0xffffff));hex.error=null;updating=false
                swatch.background=BicycleComponents.controlBackground(context,themes.active.tokens,current().color)
            }
            BicycleComponents.bindTheme(swatch,themes) { colorDisplay() }
            fun chooseColor() { showColor(kind) { colorDisplay();preview.invalidate();changed() } }
            swatch.setOnClickListener { chooseColor() }
            colorRow.addView(swatch,LayoutParams(BicycleDesign.dp(context,BicycleDesign.touchSize),BicycleDesign.dp(context,BicycleDesign.touchSize)))
            colorRow.addView(hex,LayoutParams(0,-2,1f).apply { marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            body.addView(colorRow,LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.controlGap) })
            hex.onTextChanged {
                if(!updating) {
                    val text=hex.text.toString().removePrefix("#")
                    if(text.matches(Regex("[0-9a-fA-F]{6}"))) {
                        hex.error=null
                        MapRouteStyles.write(prefs,kind,current().copy(color=text.toLong(16).toInt() or Color.BLACK))
                        frame.leadingIconColor=current().color
                        swatch.background=BicycleComponents.controlBackground(context,themes.active.tokens,current().color)
                        preview.invalidate();changed()
                    } else hex.error=context.getString(R.string.map_line_hex_error)
                }
            }
            preview.isFocusable=true;preview.setOnClickListener { chooseColor() }
            addSlider(body,R.string.map_line_opacity,100,current().opacity,"map_line_opacity_${kind.id}",
                { context.getString(R.string.map_line_percent,it) }) { value ->
                MapRouteStyles.write(prefs,kind,current().copy(opacity=value));preview.invalidate();changed()
            }
            addSlider(body,R.string.map_line_width,15,(current().widthDp*2-1).toInt(),"map_line_width_${kind.id}",
                { context.getString(R.string.map_line_width_value,(it+1)/2f) }) { value ->
                MapRouteStyles.write(prefs,kind,current().copy(widthDp=(value+1)/2f));preview.invalidate();changed()
            }
            if(trackBox!=null) {
                frame.grouped=true
                if(trackBox.childCount>0) trackBox.addView(BicycleComponents.divider(context,themes),LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.controlBorder)).apply {
                    marginStart=BicycleDesign.dp(context,BicycleDesign.controlGap);marginEnd=marginStart
                })
                trackBox.addView(frame,LayoutParams(-1,-2).apply { topMargin=0;bottomMargin=0 })
            } else addView(frame,LayoutParams(-1,-2))
        }
    }
    private fun addSlider(parent:LinearLayout,title:Int,maximum:Int,initial:Int,name:String,
        format:(Int)->String,change:(Int)->Unit):SeekBar {
        return BicycleSettingSlider(context,themes,format).apply {
            max=maximum;progress=initial;keyProgressIncrement=1;tag=name;contentDescription=context.getString(title)
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,p:Int,user:Boolean) { bar.stateDescription=valueText;if(user) change(p) }
            })
            parent.addView(settingSliderRow(context,themes,context.getString(title),this),LayoutParams(-1,-2))
        }
    }

    private fun showColor(kind:MapLineKind,update:()->Unit) {
        val initial=MapRouteStyles.read(prefs,kind).color
        val slot=prefs.getInt(BikeProfileStore.ACTIVE,0)
        val palette=FixedColorPalette(context,themes,initial) { /* Persist the draft only when leaving. */ }
        val dialog=BicycleDialogBuilder(context,themes,navigation,fullscreen=false,helpEnabled=false)
            .setTitle(R.string.map_line_choose_color).setView(palette).show()
        navigation.onDismiss(dialog) {
            if(prefs.getInt(BikeProfileStore.ACTIVE,0)==slot && palette.selectedColor!=initial) {
                MapRouteStyles.write(prefs,kind,MapRouteStyles.read(prefs,kind).copy(color=palette.selectedColor))
                update()
            }
        }
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
