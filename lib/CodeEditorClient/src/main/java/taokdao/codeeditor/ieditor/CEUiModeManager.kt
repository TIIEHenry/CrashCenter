package taokdao.codeeditor.ieditor

import taokdao.api.ui.content.editor.base.ui.IUiModeManager
import taokdao.api.ui.content.editor.base.ui.UiMode
import taokdao.codeeditor.CodeIEditor

class CEUiModeManager(val editor: CodeIEditor) : IUiModeManager {
    private lateinit var uiMode: UiMode
    override fun setUiMode(mode: UiMode) {
        this.uiMode = mode
        editor.setDark(mode == UiMode.UI_MODE_NIGHT_YES)
    }

    override fun getUiMode(): UiMode = uiMode
}