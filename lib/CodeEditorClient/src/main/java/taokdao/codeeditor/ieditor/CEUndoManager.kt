package taokdao.codeeditor.ieditor

import taokdao.api.ui.content.editor.base.edit.IUndoManager
import tiiehenry.code.view.CodeEditor

class CEUndoManager(val editor: CodeEditor) : IUndoManager {
    override fun redo(count: Int) {
        editor.redo(count)
    }

    override fun redo() {
        editor.redo()
    }

    override fun redoMore() {
        redo(5)
    }

    override fun undo(count: Int) {
        editor.undo(count)
    }

    override fun undo() {
        editor.undo()
    }

    override fun undoMore() {
        undo(5)
    }
}