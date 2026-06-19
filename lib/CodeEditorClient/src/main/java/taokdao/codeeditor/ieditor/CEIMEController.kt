package taokdao.codeeditor.ieditor

import taokdao.api.ui.content.editor.base.ui.IIMEController
import tiiehenry.code.view.CodeEditor

class CEIMEController(val editor: CodeEditor) : IIMEController {
    override fun showSoftInput(isShow: Boolean) {
        editor.showIME(isShow)
    }

}