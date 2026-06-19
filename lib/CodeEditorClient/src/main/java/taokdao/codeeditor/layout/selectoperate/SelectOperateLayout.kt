package taokdao.codeeditor.layout.selectoperate

import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import taokdao.codeeditor.CodeIEditor
import taokdao.codeeditor.base.getDrawableCompact
import tiiehenry.code.editor.client.R


class SelectOperateLayout(val editor: CodeIEditor, val layout: RecyclerView) {
    private val selectionOperateAdapter = SelectOperateAdapter()
    private val context = layout.context

    private val noteBlock = SelectOperate(
        context.getDrawableCompact(R.drawable.codeeditor_selectoperate_comment),
        {
            editor.noteBlock()
        },
        {
            editor.noteDocBlock()
            true
        })
    private val selectMore = SelectOperate(
        context.getDrawableCompact(R.drawable.codeeditor_selectoperate_select_group),
        {
            editor.selectMore()
        },
        {
            editor.selectAll(false)
            true
        })
    private val cut = SelectOperate(
        context.getDrawableCompact(R.drawable.codeeditor_selectoperate_cut),
        View.OnClickListener {
            editor.cut()
        })
    private val copy = SelectOperate(
        context.getDrawableCompact(R.drawable.codeeditor_selectoperate_copy),
        View.OnClickListener {
            editor.copy()
        })
    private val paste = SelectOperate(
        context.getDrawableCompact(R.drawable.codeeditor_selectoperate_paste),
        View.OnClickListener {
            editor.paste()
        })

    private val findReplace = SelectOperate(
        context.getDrawableCompact(R.drawable.codeeditor_selectoperate_findreplace),
        {
            editor.searcher.showFinder()
            editor.selector.selectedData?.let { it1 -> editor.searcher.setFindText(it1) }
        },
        {
            editor.searcher.showReplacer()
            editor.selector.selectedData?.let { it1 -> editor.searcher.setFindText(it1) }
            true
        })

    init {
        layout.apply {
            adapter = selectionOperateAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        selectionOperateAdapter.submitList(
            listOf(
                noteBlock, selectMore, cut, copy, paste, findReplace
            )
        )

    }

    fun show() {
        if (!editor.isEditable)
            return
        layout.visibility = View.VISIBLE
    }

    fun hide() {
        layout.visibility = View.GONE
    }

}