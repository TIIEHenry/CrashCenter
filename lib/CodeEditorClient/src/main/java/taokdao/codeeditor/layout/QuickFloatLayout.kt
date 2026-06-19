package taokdao.codeeditor.layout

import android.view.View
import android.widget.ImageButton
import taokdao.codeeditor.CodeIEditor
import tiiehenry.code.editor.client.R

class QuickFloatLayout (private val editor: CodeIEditor, val layout: View) {

    val upView = layout.findViewById<ImageButton>(R.id.quick_edit_float_save)
    val downView = layout.findViewById<ImageButton>(R.id.quick_edit_float_exit)

}