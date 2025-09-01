package com.example.noteapp  // <- change to your package

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class CustomEditText  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var onSelectionChangedListener: ((start: Int, end: Int) -> Unit)? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChangedListener?.invoke(selStart, selEnd)
    }
    fun setOnSelectionChangedListener(listener: ((start: Int, end: Int) -> Unit)?) {
        onSelectionChangedListener = listener
    }

    override fun performClick(): Boolean {

        return super.performClick()
    }
    override fun onDetachedFromWindow() {
        onSelectionChangedListener = null
        super.onDetachedFromWindow()
    }
}
