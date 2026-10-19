package de.bcycle.app

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import kotlin.math.*

/** Fixed spectrum in staggered hexagonal rows. Native buttons retain focus and accessibility. */
internal class FixedColorPalette(context:Context,themes:ThemeManager,initial:Int,choose:(Int)->Unit):LinearLayout(context) {
    companion object {
        // Axial hex coordinates: 61 spectrum colors plus nine evenly spaced neutral shades.
        val spectrum:List<List<Int>> = (-4..4).map { row ->
            (maxOf(-4,-row-4)..minOf(4,-row+4)).map { col ->
                val x=col+row/2.0;val y=row*sqrt(3.0)/2
                val radius=sqrt(x*x+y*y)/4
                val hue=((atan2(y,x)*180/PI+360)%360).toFloat()
                Color.HSVToColor(floatArrayOf(hue,radius.toFloat().coerceIn(0f,1f),(.98-maxOf(0.0,y/4)*.35).toFloat()))
            }
        }
        val neutrals=(0..8).map { val c=(255*(1-it/8.0)).roundToInt();Color.rgb(c,c,c) }
    }
    init {
        orientation=VERTICAL;gravity=Gravity.CENTER;tag="map_color_palette"
        val gap=BicycleDesign.dp(context,BicycleDesign.controlGap)
        setPadding(gap,gap,gap,gap)
        var number=0
        (spectrum+listOf(neutrals)).forEachIndexed { rowIndex,colors ->
            addView(LinearLayout(context).apply {
                gravity=Gravity.CENTER;orientation=HORIZONTAL
                colors.forEach { color ->
                    val ordinal=++number
                    addView(ImageButton(context).apply {
                        tag="map_palette_${String.format(java.util.Locale.ROOT,"%06X",color and 0xffffff)}"
                        contentDescription=context.getString(R.string.map_palette_color,ordinal)
                        isSelected=color==initial
                        if(isSelected) stateDescription=context.getString(R.string.bikes_selected)
                        BicycleComponents.bindTheme(this,themes) { tokens ->
                            background=HexColorDrawable(color,isSelected,tokens.foreground,resources.displayMetrics.density)
                        }
                        setOnClickListener { choose(color) }
                    },LayoutParams(BicycleDesign.dp(context,30),BicycleDesign.dp(context,34)))
                }
            },LayoutParams(-1,-2).apply { if(rowIndex==spectrum.size) topMargin=gap })
        }
    }
}

private class HexColorDrawable(private val color:Int,private val selected:Boolean,private val foreground:Int,private val density:Float):Drawable() {
    private val path=Path();private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onBoundsChange(bounds:Rect) {
        path.reset()
        for(i in 0..5) {
            val angle=(60*i-90)*PI/180
            val x=bounds.exactCenterX()+(bounds.width()/2f-1)*cos(angle).toFloat()
            val y=bounds.exactCenterY()+(bounds.height()/2f-1)*sin(angle).toFloat()
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
