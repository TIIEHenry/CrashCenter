package taokdao.codeeditor.ieditor

import taokdao.api.ui.content.editor.base.edit.ISearcher
import taokdao.api.ui.content.editor.base.select.ISelection
import taokdao.api.ui.content.editor.base.select.Selection
import taokdao.codeeditor.CodeIEditor
import taokdao.codeeditor.base.toast
import taokdao.codeeditor.layout.FindReplaceLayout
import taokdao.codeeditor.layout.quickinput.QuickInputLayout
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class CESearcher(val editor: CodeIEditor, private val findReplaceLayout: FindReplaceLayout, private val quickInputLayout: QuickInputLayout) : ISearcher<String, Int> {
    override fun showFinder() {
        findReplaceLayout.show()
//        quickInputLayout.hide()
    }

    override fun hideFinder() {
        findReplaceLayout.hideAll()
        quickInputLayout.show()
    }

    override fun showReplacer() {
//        quickInputLayout.hide()
        findReplaceLayout.show(true)
    }

    override fun hideReplacer() {
        findReplaceLayout.hideReplaceLayout()
    }

    fun setFindText(s: String) {
        findReplaceLayout.setFindText(s)
    }

    override fun findPrior(data: String, index: Int, start: Int, end: Int): Selection? {
        try {
            return findPrior(findReplaceLayout.isMatchCase, findReplaceLayout.isMatchRegex, data, index, start, end)
        } catch (e: PatternSyntaxException) {
            e.printStackTrace()
        }
        return null
    }

    override fun findNext(data: String, index: Int, start: Int, end: Int): Selection? {
        try {
            return findNext(findReplaceLayout.isMatchCase, findReplaceLayout.isMatchRegex, data, index, start, end)
        } catch (e: PatternSyntaxException) {
            e.printStackTrace()

        }
        return null
    }

    override fun findAll(data: String, index: Int, start: Int, end: Int): MutableList<Selection> {
        var startIndex = index
        val list = mutableListOf<Selection>()
        var selection: Selection? = null
        do {
            try {
                selection = findNext(findReplaceLayout.isMatchCase, findReplaceLayout.isMatchRegex, data, startIndex, start, end)
            } catch (e: PatternSyntaxException) {
                e.printStackTrace()
            }
            if (selection != null) {
                list.add(selection)
                startIndex = selection.end + 1
            }
        } while (selection != null)
        return list
    }

    fun replace(data: String, selection: ISelection<Int>) {
        var repStr = data
        if (findReplaceLayout.isMatchRegex) {
            val rp = Pattern.compile("\\\\n")
            val m = rp.matcher(data)
            repStr = m.replaceAll("\n")
            val rpw = Pattern.compile("\\\\r")
            val mw = rpw.matcher(repStr)
            repStr = mw.replaceAll("\r")
            val rpwr = Pattern.compile("\\\\r\\\\n")
            val mwr = rpwr.matcher(repStr)
            repStr = mwr.replaceAll("\r\n")
        }
        editor.selector.setSelectionData(selection, repStr)
    }

    override fun replaceAll(data: String, target: String, index: Int, start: Int, end: Int): MutableList<Selection> {
        val list = findAll(data, index, start, end)
        var repStr = target
        if (findReplaceLayout.isMatchRegex) {
            val rp = Pattern.compile("\\\\n")
            val m = rp.matcher(target)
            repStr = m.replaceAll("\n")
            val rpw = Pattern.compile("\\\\r")
            val mw = rpw.matcher(repStr)
            repStr = mw.replaceAll("\r")
            val rpwr = Pattern.compile("\\\\r\\\\n")
            val mwr = rpwr.matcher(repStr)
            repStr = mwr.replaceAll("\r\n")
        }
        list.reversed().forEach{  it ->
            editor.selector.setSelectionData(it, repStr)
        }
/*        list.forEachReversedByIndex {
            editor.selector.setSelectionData(it, repStr)
        }*/
        return list
    }

    private var inSearchEdge = false

    fun findPrior(isMatchCase: Boolean, isMatchRegex: Boolean, str: String, index: Int, start: Int, end: Int): Selection? {
        val text = editor.dataController.data.subSequence(start, end)
        val selEnd = index - start
        val nstr = if (isMatchRegex) str else str.replace("\\", "\\\\")
        var p = Pattern.compile(nstr)
        if (!isMatchCase) {
            p = Pattern.compile(nstr, Pattern.CASE_INSENSITIVE)
        }
        val m = p.matcher(text)
        var findEnd = selEnd - 1
        val isFinded = m.find(0)
        if (!inSearchEdge && -1 <= findEnd && isFinded) {
            inSearchEdge = false
            var indexStart = m.start()
            var indexEnd = m.end()
            var lastStart: Int
            var lastEnd: Int
            w@ while (m.find()) {
                lastStart = m.start()
                lastEnd = m.end()
                if (lastEnd >= findEnd + 1)
                    break@w
                indexStart = lastStart
                indexEnd = lastEnd
            }
            if (indexEnd == selEnd + 1) {
                findReplaceLayout.findLayout.context.toast("未找到结果，即将从头开始搜索")
                inSearchEdge = true
                return null
            }
            return Selection(indexStart + start, indexEnd + start)
        } else {
            if (inSearchEdge && isFinded) {
                inSearchEdge = false
                findEnd = editor.dataController.data.length
                var indexStart = m.start()
                var indexEnd = m.end()
                var lastStart: Int
                var lastEnd: Int
                w@ while (m.find()) {
                    lastStart = m.start()
                    lastEnd = m.end()
                    if (lastEnd >= findEnd)
                        break@w
                    indexStart = lastStart
                    indexEnd = lastEnd
                }
                return Selection(indexStart + start, indexEnd + start)
            }
        }

        return null
    }

    fun findNext(isMatchCase: Boolean, isMatchRegex: Boolean, str: String, index: Int, start: Int, end: Int): Selection? {
        val text = editor.dataController.data.subSequence(start, end)
        val selStart = index - start
        val nstr = if (isMatchRegex) str else str.replace("\\", "\\\\")

        var p = Pattern.compile(nstr)
        if (!isMatchCase) {
            p = Pattern.compile(nstr, Pattern.CASE_INSENSITIVE)
        }
        val m = p.matcher(text)
        val findStart = selStart + 1
        if (findStart <= text.length && m.find(findStart)) {
            inSearchEdge = false
            val indexStart = m.start()
            val indexEnd = m.end()
            return Selection(indexStart + start, indexEnd + start)
        } else {
            if (inSearchEdge && m.find(0)) {
                val indexStart = m.start()
                val indexEnd = m.end()
                return Selection(indexStart + start, indexEnd + start)
            } else {
                findReplaceLayout.findLayout.context.toast("未找到结果，即将从头开始搜索")
                inSearchEdge = true
            }
        }
        findReplaceLayout.findLayout.context.toast("未找到结果")
        return null
    }

}