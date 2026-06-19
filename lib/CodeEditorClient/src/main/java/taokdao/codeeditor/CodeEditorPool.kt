package taokdao.codeeditor

import taokdao.api.ui.content.editor.base.edit.IDataController
import taokdao.api.ui.content.editor.base.edit.IUndoManager
import taokdao.api.ui.content.editor.base.select.ICursorController
import taokdao.api.ui.content.editor.base.select.ISelector
import taokdao.api.ui.content.editor.base.ui.IIMEController
import taokdao.api.ui.content.editor.base.ui.IUiModeManager
import taokdao.codeeditor.ieditor.*

class CodeEditorPool(val editor: CodeIEditor) {
    var undoManager: IUndoManager = CEUndoManager(editor)
    var selector: ISelector<String, Int> = CESelector(editor)
    var imeController: IIMEController = CEIMEController(editor)
    var dataController: IDataController<String> = CEDataController(editor)
    var cursorController: ICursorController<Int> = CECursorController(editor)
    var uiModeManager: IUiModeManager = CEUiModeManager(editor)
}