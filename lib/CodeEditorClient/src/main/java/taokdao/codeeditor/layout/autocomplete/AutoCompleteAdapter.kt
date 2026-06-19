package taokdao.codeeditor.layout.autocomplete

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import tiiehenry.code.editor.client.R
import tiiehenry.code.praser.Variable.Type.*
import tiiehenry.code.view.AutoCompletePanel
import tiiehenry.code.view.ColorScheme
import tiiehenry.code.view.autocomplete.AutoCompleteData

class AutoCompleteAdapter(val context: Context) : AutoCompletePanel.AutoCompleteListAdapter<AutoCompleteAdapter.ViewHolder>(context) {

    override fun getLayoutId() = R.layout.contents_codeeditor_autocomplete_list_item

    override fun getItemHeight(): Int {
        return context.resources.getDimensionPixelSize(R.dimen.codeeditor_autocomplete_list_item_H)
    }

    private fun parseText2Span(data: AutoCompleteData): SpannableString {
        var text = data.text
        return when {
            text.contains("(") -> {
                //函数
                SpannableString(text).apply {
                    setSpan(ForegroundColorSpan(ColorScheme.Colorable.AUTOCOMPLETE_FUNC.color), 0, text.indexOf('('), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            text.contains("[keyword]") -> {
                //log("key:"+text);
                //关键字
                val idx = text.indexOf("[keyword]")
                text = text.substring(0, idx)
                SpannableString(text).apply {
                    setSpan(ForegroundColorSpan(ColorScheme.Colorable.AUTOCOMPLETE_KEYWORD.color), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            data.type == Keyword -> {
                SpannableString(text).apply {
                    setSpan(ForegroundColorSpan(ColorScheme.Colorable.AUTOCOMPLETE_KEYWORD.color), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            data.type == Func_User || data.type == Func_Internal -> {
                SpannableString(text).apply {
                    setSpan(ForegroundColorSpan(ColorScheme.Colorable.AUTOCOMPLETE_FUNC.color), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            else -> {
                //其他
                SpannableString(text).apply {
                    setSpan(ForegroundColorSpan(ColorScheme.Colorable.AUTOCOMPLETE_TEXT.color), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

    }


    override fun newViewHolder(convertView: View): ViewHolder {
        return ViewHolder(
                convertView.findViewById(R.id.iv_autocomplete_type),
                convertView.findViewById(R.id.tv_autocomplete_text)
        )
    }

    override fun convert(holder: ViewHolder, item: AutoCompleteData, position: Int) {
        val iconId = when (item.type) {
            Keyword -> R.drawable.ic_alpha_k_grey600_36dp
            Var -> R.drawable.ic_variable_grey600_36dp
            Func_User -> R.drawable.ic_function_variant_grey600_36dp
            Func_Internal -> R.drawable.ic_function_grey600_36dp
            Package -> R.drawable.ic_package_variant_closed_grey600_36dp
            Param -> R.drawable.ic_package_variant_closed_white_36dp
            else -> 0
        }
        holder.imgv.setImageResource(iconId)
        val spannableString = parseText2Span(item)
        holder.textv.text = spannableString
    }


    data class ViewHolder(val imgv: ImageView, val textv: TextView)

}
