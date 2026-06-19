package taokdao.codeeditor

import android.content.Context
import android.util.AttributeSet
import taokdao.api.ui.content.editor.IEditor
import taokdao.api.ui.content.editor.base.edit.IDataController
import taokdao.api.ui.content.editor.base.edit.ISearcher
import taokdao.api.ui.content.editor.base.edit.IUndoManager
import taokdao.api.ui.content.editor.base.select.ICursorController
import taokdao.api.ui.content.editor.base.select.ISelector
import taokdao.api.ui.content.editor.base.ui.IIMEController
import taokdao.codeeditor.ieditor.CESearcher
import tiiehenry.code.view.CodeEditor

class CodeIEditor : CodeEditor, IEditor<String, Int> {
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context) : super(context)

    lateinit var ceSearcher: CESearcher

    private val pool = CodeEditorPool(this)

    override fun getUndoManager() = pool.undoManager
    override fun setUndoManager(undoManager: IUndoManager?) {
        if (undoManager != null) {
            pool.undoManager = undoManager
        }
    }

    override fun getSelector() = pool.selector
    override fun setSelector(selector: ISelector<String, Int>?) {
        if (selector != null) {
            pool.selector = selector
        }
    }

    override fun getIMEController() = pool.imeController
    override fun setIMEController(imeController: IIMEController?) {
        if (imeController != null) {
            pool.imeController = imeController
        }
    }


    override fun getDataController() = pool.dataController
    override fun setDataController(dataController: IDataController<String>) {
        pool.dataController = dataController
    }

    override fun getCursorController() = pool.cursorController
    override fun setCursorController(cursorController: ICursorController<Int>?) {
        if (cursorController != null) {
            pool.cursorController = cursorController
        }
    }

    override fun getSearcher() = ceSearcher
    override fun setSearcher(searcher: ISearcher<String, Int>?) {
    }

    override fun getUiModeManager() = pool.uiModeManager

}
