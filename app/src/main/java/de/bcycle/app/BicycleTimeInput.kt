package de.bcycle.app

import android.graphics.Color
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.widget.EditText

/** Shared fixed separator mask. Only digits are answers; labels, empty slots and units are decoration. */
internal class BicycleTimeInput(private val edit:EditText,themes:ThemeManager,title:String,initial:String,
    private val changed:(String?)->Unit) {
    private val prefix=if(title.isBlank()) "" else "$title · "
    private var digits=arrayOfNulls<Char>(4)
    private var updating=false
    private var start=0;private var removed=0;private var inserted=""
    private var oldLength=0
    private var ghost=0
    /** null = incomplete, empty = cleared, otherwise an HH:mm value. */
    val value:String? get()=when {
        digits.all { it==null }->""
        digits.any { it==null }->null
        else->digits.joinToString("").let { it.dropLast(2)+":"+it.takeLast(2) }
    }
    init {
        edit.filters=emptyArray();edit.setSingleLine()
        edit.inputType=android.text.InputType.TYPE_CLASS_NUMBER
        load(initial)
        BicycleComponents.bindTheme(edit,themes) { tokens ->
            ghost=(tokens.foreground and 0xffffff) or ((Color.alpha(tokens.foreground)*BicycleDesign.inputHintAlpha).toInt() shl 24)
            render(edit.selectionStart)
        }
        edit.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,at:Int,count:Int,after:Int) {
                if(!updating) { start=at;removed=count;oldLength=s?.length ?: 0 }
            }
            override fun onTextChanged(s:CharSequence?,at:Int,before:Int,count:Int) {
                if(!updating) inserted=s?.subSequence(at,at+count)?.toString().orEmpty()
            }
            override fun afterTextChanged(s:Editable?) {
                if(updating) return
                var cursor=prefix.length
                if(start==0 && removed==oldLength) {
                    load(inserted);cursor=prefix.length+digits.size+1
                } else {
                    fun position(index:Int)=prefix.length+index+if(index>=digits.size-2) 1 else 0
                    val slots=digits.indices.filter { position(it) in start until start+removed }
                    slots.forEach { digits[it]=null }
                    var index=digits.indices.firstOrNull { position(it)>=start } ?: digits.size
                    if(inserted.isEmpty() && removed>0 && slots.isEmpty() && start==prefix.length+digits.size-2) {
                        index=(index-1).coerceAtLeast(0);digits[index]=null
                    }
                    inserted.filter { it in '0'..'9' }.forEach { digit -> if(index<digits.size) digits[index++]=digit }
                    cursor=if(index<digits.size) position(index) else prefix.length+digits.size+1
                }
                render(cursor);changed(value)
            }
        })
    }
    private fun load(raw:String) {
        val text=raw.removePrefix(prefix).removeSuffix(" h").trim()
        val parts=text.split(':')
        val normalized=if(parts.size==2 && parts[0].length==1) "0$text" else text
        val numbers=normalized.filter { it in '0'..'9' }
        val count=if(parts.size==2) maxOf(4,numbers.length) else 4
        digits=arrayOfNulls(count)
        numbers.take(count).forEachIndexed { i,c -> digits[i]=c }
    }
    private fun render(selection:Int) {
        updating=true
        val hours=digits.size-2
        val text=SpannableString(prefix+digits.map { it ?: '0' }.joinToString("").let { it.take(hours)+":"+it.drop(hours) }+" h")
        fun fade(a:Int,b:Int) { if(b>a) text.setSpan(ForegroundColorSpan(ghost),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        fade(0,prefix.length)
        digits.forEachIndexed { i,c -> if(c==null) { val p=prefix.length+i+if(i>=hours) 1 else 0;fade(p,p+1) } }
        fade(prefix.length+hours,prefix.length+hours+1)
        fade(text.length-2,text.length)
        edit.setText(text);edit.setSelection(selection.coerceIn(prefix.length,text.length-2))
        updating=false
    }
}

/** A persistent, non-answer unit directly after the editable number. */
internal object BicycleInputUnit {
    fun bind(edit:EditText,themes:ThemeManager,unit:String,changed:(String)->Boolean) {
        var updating=false;var ghost=0
        fun render() {
            if(updating) return
            updating=true
            val old=edit.text.toString();val selection=edit.selectionStart
            val raw=old.replace(unit,"").trim()
            val text=SpannableString(if(raw.isEmpty()) "" else "$raw $unit")
            if(raw.isNotEmpty()) text.setSpan(ForegroundColorSpan(ghost),raw.length,text.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            edit.setText(text);edit.setSelection(selection.coerceIn(0,raw.length))
            updating=false
        }
        edit.filters=emptyArray()
        BicycleComponents.bindTheme(edit,themes) { tokens ->
            ghost=(tokens.foreground and 0xffffff) or ((Color.alpha(tokens.foreground)*BicycleDesign.inputHintAlpha).toInt() shl 24)
            render()
        }
        edit.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int)=Unit
            override fun afterTextChanged(s:Editable?) {
                if(updating) return
                val raw=s.toString().replace(unit,"").trim()
                render();edit.error=if(changed(raw)) null else edit.context.getString(R.string.hrvq_invalid_number)
            }
        })
    }
}
