package taokdao.codeeditor.ieditor

import taokdao.api.ui.content.editor.base.select.ISelection
import taokdao.api.ui.content.editor.base.select.ISelector
import taokdao.api.ui.content.editor.base.select.Selection
import tiiehenry.code.view.CodeEditor

class CESelector(val editor: CodeEditor) : ISelector<String, Int> {
    override fun selectAll() {
        editor.selectAll()
    }

    override fun selectLess() {
        editor.selectLess()
    }

    override fun selectMore() {
        editor.selectMore()
    }

    override fun getSelection(): Selection? {
        if (editor.isInSelectionMode)
            return Selection(editor.selectionStart, editor.selectionEnd)
        return null
    }

    override fun setSelection(selection: ISelection<Int>?) {
        if (selection == null)
            editor.selectText(false)
        else
            editor.setSelection(selection.start, selection.end)
    }

    override fun getSelectionData(selection: ISelection<Int>): String {
        return editor.getTextForSelectionRange(selection.start, selection.end)
    }

    override fun getSelectedData(): String? {
        return editor.selectedText
    }

    override fun cutSelectedData(): String {
        return editor.cutSelection()
    }

    override fun deleteSelectedData(): String {
        return editor.deleteSelection()
    }

    override fun setSelectionData(selection: ISelection<Int>, data: String) {
        editor.replaceText(selection.start, selection.length(), data)
        setSelection(Selection(selection.start, selection.start + data.length))
    }

    override fun copySelectedData(): String {
        return editor.copySelection()
    }
}