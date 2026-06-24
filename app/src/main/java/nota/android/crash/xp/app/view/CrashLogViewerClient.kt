package nota.android.crash.xp.app.view

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import tiiehenry.code.editor.client.BaseCodeEditorClient
import tiiehenry.code.editor.client.R

/**
 * Read-only stack trace viewer backed by CodeEditor.
 * Shared by [nota.android.crash.xp.app.observe.CrashDetailBottomSheet] and
 * [nota.android.crash.ActivityCrashInfo].
 */
class CrashLogViewerClient(
    hostContext: Context,
    packageContext: Context,
    inflater: LayoutInflater,
    resources: Resources,
) : BaseCodeEditorClient(hostContext, packageContext, inflater, resources) {

    override fun initView() {
        super.initView()
        isWordWrap = true
        // Keep editor interactive (select, zoom, scroll) — don't set isEditable=false
        quickFloatLayout.upView.visibility = View.GONE
        quickFloatLayout.downView.visibility = View.GONE
        layout.findViewById<View>(R.id.quick_input_layout)?.visibility = View.GONE
        quickControlLayout.hide()
    }

    fun showStackTrace(text: String?) {
        this.text = text ?: ""
    }

    var isWordWrap: Boolean
        get() = codeEditor.isWordWrap
        set(enabled) {
            codeEditor.setWordWrap(enabled)
        }

    fun toggleWordWrap(): Boolean {
        isWordWrap = !isWordWrap
        return isWordWrap
    }

    companion object {
        fun attach(
            hostContext: Context,
            container: ViewGroup,
        ): CrashLogViewerClient {
            val inflater = LayoutInflater.from(hostContext)
            val client = CrashLogViewerClient(
                hostContext,
                hostContext,
                inflater,
                hostContext.resources,
            )
            client.initView()
            container.addView(
                client.layout,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            return client
        }
    }
}
