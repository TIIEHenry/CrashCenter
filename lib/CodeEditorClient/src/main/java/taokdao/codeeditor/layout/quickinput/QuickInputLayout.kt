package taokdao.codeeditor.layout.quickinput

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import taokdao.codeeditor.CodeIEditor
import tiiehenry.code.editor.client.R

class QuickInputLayout(val editor: CodeIEditor, val layout: View) {
    val indentView = layout.findViewById<ImageView>(R.id.quick_input_ibtn_indent)
    val controlView = layout.findViewById<ImageView>(R.id.quick_input_ibtn_tabtool)

    private val quickInputAdapter = QuickInputAdapter({ _, item, _ ->
        if (!editor.isEditable)
            return@QuickInputAdapter
        editor.insert(item)
    }, { _, s, _ ->
        if (!editor.isEditable)
            return@QuickInputAdapter
        val b = ("[]-+=/&|").contains(s)
        val b2 = ("'\"").contains(s)
        val b3 = ("~!<>").contains(s)
//            val b5 = ("\$").contains(s)
        when {
            b -> editor.paste(s + s)
            b2 -> {
                editor.paste(s + s)
                editor.moveCaretLeft(true)
            }

            b3 -> editor.paste("$s=")
            b -> {
                editor.paste("$s{}")
                editor.moveCaretLeft(true)
            }

            s == "(" -> {
                editor.paste("()")
                editor.moveCaretLeft(true)
            }

            s == ")" -> editor.paste("()")
            else -> editor.paste(s)
        }
    }).apply {
        submitList(editor.language.symbolList)

    }

    fun init(): QuickInputLayout {
        layout.findViewById<RecyclerView>(R.id.quick_input_rv).apply {
            adapter = quickInputAdapter
            layoutManager = LinearLayoutManager(layout.context, RecyclerView.HORIZONTAL, false)
        }
        indentView.setOnClickListener {
            editor.indentLines()
        }
        return this
    }


    fun show() {
        layout.visibility = View.VISIBLE
    }

    fun hide() {
        layout.visibility = View.GONE
    }
}
