package tiiehenry.code.editor.client

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import taokdao.codeeditor.CodeIEditor
import taokdao.codeeditor.ieditor.CESearcher
import taokdao.codeeditor.layout.FindReplaceLayout
import taokdao.codeeditor.layout.QuickControlLayout
import taokdao.codeeditor.layout.QuickFloatLayout
import taokdao.codeeditor.layout.autocomplete.AutoCompleteAdapter
import taokdao.codeeditor.layout.quickinput.QuickInputLayout
import taokdao.codeeditor.layout.selectoperate.SelectOperateLayout
import tiiehenry.code.format.FormatListener
import tiiehenry.code.view.ColorScheme

open class BaseCodeEditorClient(
    val hostContext: Context,
    val packageContext: Context,
    val inflater: LayoutInflater,
    val resources: Resources,
) {

    lateinit var selectOperateLayout: SelectOperateLayout
    lateinit var findReplaceLayout: FindReplaceLayout
    lateinit var quickControlLayout: QuickControlLayout
    lateinit var quickInputLayout: QuickInputLayout
    lateinit var quickFloatLayout: QuickFloatLayout

    val layout: View = inflater.inflate(R.layout.contents_codeeditor, null)

    val codeEditor = layout.findViewById<CodeIEditor>(R.id.code_ieditor_editor)


    open fun initView() {
        initCodeEditor(codeEditor)
        initCodeEditorSetting(codeEditor)

        selectOperateLayout =
            SelectOperateLayout(codeEditor, layout.findViewById(R.id.select_operate_layout))
        codeEditor.setOnSelectionChangedListener { active, _, _ ->
            if (active) {
                selectOperateLayout.show()
            } else {
                selectOperateLayout.hide()
            }
        }

        quickFloatLayout = QuickFloatLayout(codeEditor, layout)
        quickControlLayout = QuickControlLayout(codeEditor, layout).init()
        quickInputLayout =
            QuickInputLayout(codeEditor, layout.findViewById(R.id.quick_input_layout)).init()
        findReplaceLayout =
            FindReplaceLayout(
                codeEditor,
                layout.findViewById(R.id.find_layout),
                layout.findViewById(R.id.replace_layout)
            ).init()
        codeEditor.ceSearcher = CESearcher(codeEditor, findReplaceLayout, quickInputLayout)

    }

    open fun asDialog(): Dialog {
        val dialog = Dialog(hostContext, android.R.style.Theme_Material_NoActionBar)

        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener { dialogInterface, i, keyEvent ->
            keyEvent.keyCode == KeyEvent.KEYCODE_BACK
        }
        dialog.setContentView(layout)
        return dialog
    }

    open fun showInDialog() {
        asDialog().show()
    }


    open fun initCodeEditor(editor: CodeIEditor) {
        editor.formatListener = object : FormatListener {
            val progressDialog = ProgressDialog(hostContext)
            override fun onPrepare() {
                progressDialog.setTitle("Formatting")
                progressDialog.setMessage("Please wait...")
                //            progressDialog.setCancelable(false);
                progressDialog.show()
            }

            override fun onDone() {
                progressDialog.dismiss()
            }

            override fun onError(errMsg: String) {
                progressDialog.setTitle("Format Error")
                progressDialog.setMessage(errMsg)
                //            progressDialog.setCancelable(true);
                progressDialog.setButton(
                    DialogInterface.BUTTON_POSITIVE, "OK"
                ) { dialog, which -> onDone() }
                //            onDone();
            }
        }
//            isWordWrap = language.isWordWrap

        editor.apply {
            setLexTask(editor.language.newLexTask())
            setNonPrintingCharVisibility(false)
        }

        editor.autoCompletePanel.apply {
            setAdapter(AutoCompleteAdapter(packageContext))
            setBackground(ColorDrawable(resources.getColor(R.color.main_content_background_color)))
            panel.animationStyle = R.style.CodeEditor_AutoComplete_PopupAnimation
            panel.setListSelector(resources.getDrawable(R.drawable.cr_ripple_row_overlay))
        }

        editor.autoCompletePanel.apply {
            window.listView?.tag = VIEW_TAG
            window.anchorView?.tag = VIEW_TAG
            window.listView?.parent?.let {
                if (it is View) {
                    it.tag = VIEW_TAG
                }
            }
            window.anchorView?.parent?.let {
                if (it is View) {
                    it.tag = VIEW_TAG
                }
            }
        }
    }

    //检查当前系统是否已开启暗黑模式

    private fun getDarkModeStatus(): Boolean {
        val mode = hostContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun initCodeEditorSetting(editor: CodeIEditor) {
        val night = getDarkModeStatus()
        if (night) {
            ColorScheme.Colorable.CARET_BACKGROUND.color =
                resources.getColor(R.color.main_appbar_background_color)
        } else {
            ColorScheme.Colorable.CARET_BACKGROUND.color =
                resources.getColor(R.color.blueGrey_400)
        }
        editor.setDark(night)
    }

    var text: String
        get() = codeEditor.text.toString()
        set(v) = codeEditor.setText(v)

    companion object {
        @kotlin.jvm.JvmField
        var VIEW_TAG: String = "code_editor"
    }

}