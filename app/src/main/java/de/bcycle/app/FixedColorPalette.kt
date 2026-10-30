package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import kotlin.math.*

/** Regular axial hex grid with a white centre; native swatches keep focus/accessibility. */
internal class FixedColorPalette(context:Context,themes:ThemeManager,initial:Int,choose:(Int)->Unit):LinearLayout(context) {
    companion object {
        private const val rings=5 // Six cells per outer edge, 91 cells including the centre.
        private val coordinates=(-rings..rings).flatMap { row ->
            (maxOf(-rings,-row-rings)..minOf(rings,-row+rings)).map { col -> col to row }
        }
        val spectrum:List<List<Int>> = (-rings..rings).map { row ->
            coordinates.filter { it.second==row }.map { (col,r) ->
                val hue=((atan2(r*sqrt(3.0)/2,col+r/2.0)*180/PI+360)%360).toFloat()
                val saturation=maxOf(abs(col),abs(r),abs(col+r)).toFloat()/rings
                Color.HSVToColor(floatArrayOf(hue,saturation,1f))
            }
        }
        fun withBrightness(color:Int,step:Int):Int {
            val hsv=FloatArray(3);Color.colorToHSV(color,hsv);hsv[2]=step.coerceIn(0,9)/9f
            return Color.HSVToColor(hsv)
        }
    }
    var selectedColor:Int=initial;private set
    init {
        orientation=VERTICAL;gravity=Gravity.CENTER;tag="map_color_palette"
        val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
        setPadding(gap,gap,gap,gap)
        val hsv=FloatArray(3);Color.colorToHSV(initial,hsv)
        var brightness=(hsv[2]*9).roundToInt()
        val baseColors=spectrum.flatten()
        val originalBase=Color.HSVToColor(floatArrayOf(hsv[0],hsv[1],1f))
        var selectedBase=originalBase
        var selectedIndex=baseColors.indices.minBy { index ->
            val c=baseColors[index]
            (Color.red(c)-Color.red(originalBase)).toDouble().pow(2)+(Color.green(c)-Color.green(originalBase)).toDouble().pow(2)+(Color.blue(c)-Color.blue(originalBase)).toDouble().pow(2)
        }
        val grid=object:ViewGroup(context) {
            private var cellWidth=0f;private var cellHeight=0f
            override fun onMeasure(w:Int,h:Int) {
                val available=if(MeasureSpec.getMode(w)==MeasureSpec.UNSPECIFIED) BicycleDesign.dp(context,330) else MeasureSpec.getSize(w)
                cellWidth=minOf(BicycleDesign.dp(context,30).toFloat(),available/11f)
                cellHeight=cellWidth*2/sqrt(3f)
                for(i in 0 until childCount) getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(cellWidth.roundToInt(),MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(cellHeight.roundToInt(),MeasureSpec.EXACTLY))
                setMeasuredDimension(resolveSize(ceil(cellWidth*11).toInt(),w),resolveSize(ceil(cellHeight*8.5f).toInt(),h))
            }
            override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int) {
                coordinates.forEachIndexed { index,(col,row) ->
                    val child=getChildAt(index)
                    val x=(width/2f+(col+row/2f)*cellWidth-child.measuredWidth/2f).roundToInt()
                    val y=((row+rings)*cellHeight*.75f).roundToInt()
                    child.layout(x,y,x+child.measuredWidth,y+child.measuredHeight)
                }
            }
        }.apply { tag="map_palette_grid" }
        fun paint(button:ImageButton,index:Int,tokens:ThemeTokens) {
            val color=withBrightness(baseColors[index],brightness)
            button.tag="map_palette_${String.format(java.util.Locale.ROOT,"%06X",color and 0xffffff)}"
            button.contentDescription=context.getString(R.string.map_palette_color,index+1)+" · "+String.format(java.util.Locale.ROOT,"#%06X",color and 0xffffff)
            button.isSelected=index==selectedIndex
            button.stateDescription=if(button.isSelected) context.getString(R.string.bikes_selected) else null
            button.background=HexColorDrawable(color,button.isSelected,tokens.foreground,resources.displayMetrics.density)
        }
        baseColors.forEachIndexed { index,base ->
            grid.addView(ImageButton(context).apply {
                BicycleComponents.bindTheme(this,themes) { paint(this,index,it) }
                setOnClickListener {
                    selectedIndex=index;selectedBase=base;selectedColor=withBrightness(selectedBase,brightness)
                    for(i in baseColors.indices) paint(grid.getChildAt(i) as ImageButton,i,themes.active.tokens)
                    choose(selectedColor)
                }
            })
        }
        addView(grid,LayoutParams(-1,-2))
        addView(BicycleComponents.text(context,themes,context.getString(R.string.map_palette_brightness)).apply {
            setPadding(0,gap,0,0)
        },LayoutParams(-1,-2))
        val slider=BicycleRatingSlider(context,themes,0,9).apply {
            tag="map_palette_brightness";contentDescription=context.getString(R.string.map_palette_brightness);progress=brightness
            minimumHeight=BicycleDesign.dp(context,BicycleDesign.touchSize)
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar:SeekBar)=Unit
                override fun onStopTrackingTouch(bar:SeekBar)=Unit
                override fun onProgressChanged(bar:SeekBar,value:Int,fromUser:Boolean) {
                    brightness=value
                    selectedColor=withBrightness(selectedBase,brightness)
                    if(fromUser) choose(selectedColor)
                    stateDescription="${(value*100/9f).roundToInt()} %"
                    for(i in baseColors.indices) paint(grid.getChildAt(i) as ImageButton,i,themes.active.tokens)
                }
            })
        }
        addView(slider,LayoutParams(-1,BicycleDesign.dp(context,BicycleDesign.touchSize)))
        addView(LinearLayout(context).apply {
            for((label,gravity) in listOf(R.string.map_palette_black to Gravity.START,R.string.map_palette_full to Gravity.END))
                addView(BicycleComponents.sliderScaleLabel(context,themes,context.getString(label)).apply { this.gravity=gravity },LayoutParams(0,-2,1f))
        },LayoutParams(-1,-2).apply { topMargin=BicycleDesign.dp(context,BicycleDesign.sliderScaleLabelGap) })
    }
}

internal class HexColorDrawable(private val color:Int,private val selected:Boolean,private val foreground:Int,private val density:Float):Drawable() {
    private val path=Path();private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onBoundsChange(bounds:Rect) {
        path.reset()
        val radius=(minOf(bounds.width()/sqrt(3f),bounds.height()/2f)-density).coerceAtLeast(0f)
        for(i in 0..5) {
            val angle=(60*i-90)*PI/180
            val x=bounds.exactCenterX()+radius*cos(angle).toFloat()
            val y=bounds.exactCenterY()+radius*sin(angle).toFloat()
            if(i==0) path.moveTo(x,y) else path.lineTo(x,y)
        }
        path.close()
    }
    override fun draw(canvas:Canvas) {
        paint.style=Paint.Style.FILL;paint.color=color;canvas.drawPath(path,paint)
        if(selected) {
            paint.style=Paint.Style.STROKE;paint.strokeWidth=2*density
            paint.color=if(ThemeValidator.contrast(color,foreground)>=3) foreground else if(ThemeValidator.contrast(color,Color.BLACK)>ThemeValidator.contrast(color,Color.WHITE)) Color.BLACK else Color.WHITE
            canvas.drawPath(path,paint)
        }
    }
    override fun setAlpha(alpha:Int)=Unit
    override fun setColorFilter(filter:ColorFilter?)=Unit
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
