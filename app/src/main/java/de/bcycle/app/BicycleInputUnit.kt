package de.bcycle.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import kotlin.math.ceil

/** Pure decoration: units never enter Editable, selection, validation or persisted values. */
internal object BicycleInputUnit {
    fun unit(edit:EditText)=(edit.getTag(R.id.input_unit) as? Decoration)?.unit
    fun bind(edit:EditText,themes:ThemeManager,unit:String) {
        if(unit(edit)==unit) return
        check(edit.getTag(R.id.input_unit)==null) { "Input unit already bound" }
        edit.maxLines=1;edit.setHorizontallyScrolling(false)
        val decoration=Decoration(edit,unit)
        edit.setTag(R.id.input_unit,decoration)
        edit.overlay.add(decoration)
        edit.stateDescription=unit
        edit.addOnLayoutChangeListener { _,_,_,_,_,_,_,_,_->decoration.resize() }
        edit.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int)=Unit
            override fun afterTextChanged(s:Editable?) { decoration.invalidateSelf() }
        })
        BicycleComponents.bindTheme(edit,themes) { tokens ->
            decoration.color=(tokens.foreground and 0xffffff) or
                ((Color.alpha(tokens.foreground)*BicycleDesign.inputHintAlpha).toInt() shl 24)
            decoration.resize();decoration.invalidateSelf()
        }
    }
    private class Decoration(private val edit:EditText,val unit:String):Drawable() {
        private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        private val spacer=ColorDrawable(Color.TRANSPARENT)
        private val gap=BicycleDesign.dp(edit.context,BicycleDesign.inputUnitGap)
        var color=0
        private fun metrics() { paint.typeface=edit.typeface;paint.textSize=edit.textSize;paint.color=color }
        fun resize() {
            metrics()
            val reserve=ceil(paint.measureText(unit)).toInt()+gap
            if(spacer.bounds.width()!=reserve) {
                spacer.setBounds(0,0,reserve,1)
                val old=edit.compoundDrawablesRelative
                edit.setCompoundDrawablesRelative(old[0],old[1],spacer,old[3])
            }
            setBounds(0,0,edit.width,edit.height)
        }
        override fun draw(canvas:Canvas) {
            val layout=edit.layout ?: return
            // An existing placeholder such as "Distanz · km" already supplies its unit.
            if(edit.text.isEmpty() && !edit.hint.isNullOrEmpty()) return
            metrics()
            val end=edit.compoundPaddingLeft+layout.getPrimaryHorizontal(edit.length())-edit.scrollX
            val saved=canvas.save()
            canvas.clipRect(edit.paddingLeft,0,edit.width-edit.paddingRight,edit.height)
            // Native caret positions can exceed the last glyph edge by a fractional pixel.
            val start=minOf(end+gap,edit.width-edit.paddingRight-paint.measureText(unit))
            canvas.drawText(unit,start,edit.baseline.toFloat()-edit.scrollY,paint)
            canvas.restoreToCount(saved)
        }
        override fun setAlpha(alpha:Int)=Unit
        override fun setColorFilter(colorFilter:ColorFilter?)=Unit
        @Deprecated("Deprecated in Java") override fun getOpacity()=PixelFormat.TRANSLUCENT
    }
}
