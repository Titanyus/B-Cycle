package de.bcycle.app

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*

internal fun EditText.onTextChanged(changed: () -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?,start: Int,count: Int,after: Int) = Unit
        override fun onTextChanged(s: CharSequence?,start: Int,before: Int,count: Int) = Unit
        override fun afterTextChanged(s: Editable?) { changed() }
    })
}
internal fun observeSettings(view: View, changed: () -> Unit) {
    when(view) {
        is EditText -> view.onTextChanged(changed)
        is CompoundButton -> view.setOnCheckedChangeListener { _,_->changed() }
        is Spinner -> {
            val previous=view.onItemSelectedListener
            view.onItemSelectedListener=object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) { previous?.onNothingSelected(parent) }
                override fun onItemSelected(parent: AdapterView<*>?,v: View?,position: Int,id: Long) { previous?.onItemSelected(parent,v,position,id); changed() }
            }
        }
    }
    if(view is ViewGroup) for(i in 0 until view.childCount) observeSettings(view.getChildAt(i),changed)
}
