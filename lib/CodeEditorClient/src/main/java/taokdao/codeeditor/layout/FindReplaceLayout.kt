package taokdao.codeeditor.layout

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import tiiehenry.celestialruler.ui.text.SimpleTextWatcher
import taokdao.codeeditor.CodeIEditor
import tiiehenry.code.editor.client.R
import kotlin.math.max

class FindReplaceLayout(
    val editor: CodeIEditor,
    val findLayout: View,
    private val replaceLayout: View
) {
    private val context: Context = findLayout.context

    private fun findPrior() {
        val index = editor.selector.selection?.start ?: editor.cursorController.currentIndex
        val selection =
            editor.searcher.findPrior(getFindText(), index, 0, editor.dataController.data.length)
        if (selection != null) {
            editor.selector.selection = selection
        }
    }

    private fun findNextFromStart(len: Int) {
        val index = max(
            (editor.selector.selection?.end
                ?: editor.cursorController.currentIndex) - len, 0
        )
        val selection =
            editor.searcher.findNext(getFindText(), index, 0, editor.dataController.data.length)
        if (selection != null) {
            editor.selector.selection = selection
        }
    }

    private fun findNext() {
        val index = editor.selector.selection?.end ?: editor.cursorController.currentIndex
        val selection =
            editor.searcher.findNext(getFindText(), index, 0, editor.dataController.data.length)
        if (selection != null) {
            editor.selector.selection = selection
        }
    }


    private fun replace() {
        val selection = editor.selector.selection
        if (selection != null) {
            editor.searcher.replace(getReplaceText(), selection)
            findNext()
        } else {

            Toast.makeText(
                context,
                R.string.contents_codeeditor_findreplace_notfound,
                Toast.LENGTH_SHORT
            ).show()
            findNext()
        }
    }

    val findEditInput = findLayout.findViewById<EditText>(R.id.find_edit_input)
    val findIbtnPrior = findLayout.findViewById<ImageButton>(R.id.find_ibtn_prior)
    val findIbtnNext = findLayout.findViewById<ImageButton>(R.id.find_ibtn_next)
    val findIbtnCase = findLayout.findViewById<ImageButton>(R.id.find_ibtn_case)
    val findIbtnRegex = findLayout.findViewById<ImageButton>(R.id.find_ibtn_regex)
    val findIbtnClose = findLayout.findViewById<ImageButton>(R.id.find_ibtn_close)
    val replaceEditInput = replaceLayout.findViewById<EditText>(R.id.replace_edit_input)
    val replaceReplaceOne = replaceLayout.findViewById<Button>(R.id.replace_replace_one)
    val replaceReplaceAll = replaceLayout.findViewById<Button>(R.id.replace_replace_all)

    fun setFindText(s: String) {
        findEditInput.setText(s)
    }

    private fun getFindText(): String {
        return findEditInput.text.toString()
    }

    fun setReplaceText(s: String) {
        replaceEditInput.setText(s)
    }

    private fun getReplaceText(): String {
        return replaceEditInput.text.toString()
    }

    private fun replaceAll() {
        editor.searcher.replaceAll(
            getFindText(),
            getReplaceText(),
            0,
            0,
            editor.dataController.data.length
        )
    }

    private fun switchMatchCase(v: View) {
        isMatchCase = !isMatchCase
        v.isSelected = isMatchCase
        Toast.makeText(
            findLayout.context,
            context.getString(R.string.contents_codeeditor_findreplace_matchcase) + isMatchCase,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun switchMatchRegex(v: View) {
        isMatchRegex = !isMatchRegex
        v.isSelected = isMatchRegex
        Toast.makeText(
            context,
            context.getString(R.string.contents_codeeditor_findreplace_matchregex) + isMatchRegex,
            Toast.LENGTH_SHORT
        ).show()
    }

    var isMatchCase = false
    var isMatchRegex = false

    fun init(): FindReplaceLayout {
        findEditInput.addTextChangedListener(SimpleTextWatcher.newAfterWatcher {
            if (it.isNotEmpty())
                findNextFromStart(it.length)
        })
        val onClickSearchLayout = View.OnClickListener { v ->
            when (v) {
                findIbtnPrior -> findPrior()
                findIbtnNext -> findNext()
                replaceReplaceOne -> replace()
                replaceReplaceAll -> replaceAll()
                findIbtnCase -> switchMatchCase(v)
                findIbtnRegex -> switchMatchRegex(v)
                findIbtnClose -> editor.searcher.hideFinder()
            }
        }
//        val onLongClickSearchLayout = View.OnLongClickListener { v ->
//            true
//        }
        arrayListOf<View>(
            findIbtnPrior, findIbtnNext, replaceReplaceOne, replaceReplaceAll,
            findIbtnCase, findIbtnRegex, findIbtnClose
        ).forEach {
            it.setOnClickListener(onClickSearchLayout)
//            it.setOnLongClickListener(onLongClickSearchLayout)
        }
        return this
    }

    fun show(showReplaceLayout: Boolean = false) {
        if (!editor.isEditable)
            return
        replaceLayout.visibility = if (showReplaceLayout) View.VISIBLE else View.GONE
        findLayout.visibility = View.VISIBLE
    }


    fun hideAll() {
        findLayout.visibility = View.GONE
        hideReplaceLayout()
    }

    fun hideReplaceLayout() {
        replaceLayout.visibility = View.GONE
    }
}