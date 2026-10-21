package de.bcycle.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.widget.LinearLayout

/** Cached, downward-only visual depth; no elevation-dependent blur or extra layout space. */
internal class BicycleHeaderShadow(context: Context) {
    private val height=BicycleDesign.dp(context,BicycleDesign.headerShadowHeight).toFloat()
    private val paint=Paint().apply {
        shader=LinearGradient(0f,0f,0f,height,Color.argb(BicycleDesign.headerShadowAlpha,0,0,0),Color.TRANSPARENT,Shader.TileMode.CLAMP)
    }
    fun draw(canvas: Canvas, left: Float, top: Float, right: Float) {
        val saved=canvas.save();canvas.translate(0f,top)
        canvas.drawRect(left,0f,right,height,paint);canvas.restoreToCount(saved)
    }
}

internal class BicycleFullscreenHost(context: Context) : LinearLayout(context) {
    var showHeaderShadow=true
    var contentSpacingTarget:android.view.View?=null
    private val shadow=BicycleHeaderShadow(context)
    private var header: android.view.View?=null
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int) {
        contentSpacingTarget?.let(BicycleContentSpacing::page)
        super.onMeasure(widthMeasureSpec,heightMeasureSpec)
    }
    override fun onLayout(changed: Boolean,l: Int,t: Int,r: Int,b: Int) {
        super.onLayout(changed,l,t,r,b)
        header=findViewWithTag("bicycle_navigation_header")
    }
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        header?.takeIf { showHeaderShadow }?.let {
            shadow.draw(canvas,it.left.toFloat(),it.bottom.toFloat(),it.right.toFloat())
        }
    }
}
