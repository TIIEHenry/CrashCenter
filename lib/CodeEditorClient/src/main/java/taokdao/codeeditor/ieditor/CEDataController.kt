package taokdao.codeeditor.ieditor

import taokdao.api.ui.content.editor.base.edit.IDataController
import tiiehenry.code.view.CodeEditor

open class CEDataController(val editor: CodeEditor) : IDataController<String> {

    override fun getData(): String {
        return editor.string
    }

    override fun setData(s: String?) {
        if (s != null)
            editor.setText(s, true)
    }

    override fun isChanged(): Boolean {
        return editor.isEdited
    }

    /**
     * ioManager 中 write方法应该实现
     */
    override fun setChanged(changed: Boolean) {
        editor.isEdited = changed
    }
}