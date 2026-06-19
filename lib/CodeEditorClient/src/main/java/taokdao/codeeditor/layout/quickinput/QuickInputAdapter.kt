package taokdao.codeeditor.layout.quickinput

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tiiehenry.code.editor.client.databinding.ContentsCodeeditorQuickinputItemBinding


class QuickInputAdapter(
    val onClick: (QuickInputAdapter, String, Int) -> Unit,
    val onLongClick: (QuickInputAdapter, String, Int) -> Unit
) :
    ListAdapter<String, QuickInputAdapter.ViewHolder>(object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }) {

    class ViewHolder(val binding: ContentsCodeeditorQuickinputItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding =
            ContentsCodeeditorQuickinputItemBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = getItem(position)
        holder.binding.tvQuickinputText.setOnClickListener { onClick(this, item, position) }
        holder.binding.tvQuickinputText.setOnLongClickListener {
            onLongClick(
                this,
                item,
                position
            ); true
        }
        holder.binding.tvQuickinputText.text = item
    }
}