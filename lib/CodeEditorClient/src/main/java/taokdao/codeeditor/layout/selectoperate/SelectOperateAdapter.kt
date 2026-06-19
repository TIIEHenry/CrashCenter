package taokdao.codeeditor.layout.selectoperate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tiiehenry.code.editor.client.databinding.ContentsCodeeditorSelectoperateItemBinding

class SelectOperateAdapter : ListAdapter<SelectOperate, SelectOperateAdapter.ViewHolder>(object :
    DiffUtil.ItemCallback<SelectOperate>() {
    override fun areItemsTheSame(oldItem: SelectOperate, newItem: SelectOperate) =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: SelectOperate, newItem: SelectOperate) = false
}) {
    class ViewHolder(val binding: ContentsCodeeditorSelectoperateItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ContentsCodeeditorSelectoperateItemBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.ibtnButton.apply {
            setImageDrawable(item.icon)
            setOnClickListener(item.click)
            setOnLongClickListener(item.longClick)
        }
    }
}