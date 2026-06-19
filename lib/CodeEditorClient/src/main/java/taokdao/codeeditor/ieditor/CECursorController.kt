package taokdao.codeeditor.ieditor

import taokdao.api.ui.content.editor.base.select.ICursorController
import tiiehenry.code.view.CodeEditor

class CECursorController(val editor: CodeEditor) : ICursorController<Int> {
    override fun getMinIndex(): Int {
        return 0
    }

    override fun getMaxIndex(): Int {
        return editor.length
    }

    override fun getCurrentIndex(): Int {
        return editor.caretPosition
    }

    override fun setCurrentIndex(index: Int) {
        editor.setSelection(index)
    }
}