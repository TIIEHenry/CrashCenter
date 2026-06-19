package taokdao.codeeditor.layout

import android.view.View
import android.widget.ImageView
import taokdao.codeeditor.CodeIEditor
import tiiehenry.code.editor.client.R

class QuickControlLayout(private val editor: CodeIEditor, val layout: View) {
    val leftView = layout.findViewById<ImageView>(R.id.quick_control_left)
    val rightView = layout.findViewById<ImageView>(R.id.quick_control_right)
    fun init(): QuickControlLayout {
        leftView.setOnClickListener {
            editor.undoManager.undo()
        }
        leftView.setOnLongClickListener {
            editor.format()
            true
        }
        rightView.setOnClickListener {
            editor.undoManager.redo()
        }

        rightView.setOnLongClickListener {
            true
        }

        return this
    }

    fun show() {
        leftView.visibility = View.VISIBLE
        rightView.visibility = View.VISIBLE
    }

    fun hide() {
        leftView.visibility = View.GONE
        rightView.visibility = View.GONE
    }
}