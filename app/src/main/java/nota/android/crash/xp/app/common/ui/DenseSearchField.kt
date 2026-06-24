package nota.android.crash.xp.app.common.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import nota.android.crash.xp.app.databinding.ViewDenseSearchFieldBinding

object DenseSearchField {

    fun editText(root: View): EditText = ViewDenseSearchFieldBinding.bind(root).searchInput

    fun setHint(root: View, hint: CharSequence) {
        ViewDenseSearchFieldBinding.bind(root).searchLayout.hint = hint
    }

    fun setOnQueryChangeListener(root: View, listener: (String) -> Unit) {
        editText(root).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                listener(s?.toString().orEmpty())
            }
        })
    }
}
