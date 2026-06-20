package nota.android.crash.xp.app.common.ui.adapter

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class BaseListAdapter<T : Any, VH : BaseListAdapter.BaseViewHolder<T>>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, VH>(diffCallback) {

    private var onItemClickListener: ((View, T, Int) -> Unit)? = null
    private var onItemLongClickListener: ((View, T, Int) -> Boolean)? = null

    fun onItemClick(f: (rootView: View, data: T, pos: Int) -> Unit) {
        onItemClickListener = f
    }

    fun onItemLongClick(f: (rootView: View, data: T, pos: Int) -> Boolean) {
        onItemLongClickListener = f
    }

    protected fun notifyItemClick(view: View, position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            onItemClickListener?.invoke(view, getItem(position), position)
        }
    }

    protected fun notifyItemLongClick(view: View, position: Int): Boolean {
        return if (position != RecyclerView.NO_POSITION) {
            onItemLongClickListener?.invoke(view, getItem(position), position) ?: false
        } else {
            false
        }
    }

    abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(data: T)
    }
}

class SimpleDiffCallback<T : Any>(
    private val keySelector: (T) -> Any?
) : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return keySelector(oldItem) == keySelector(newItem)
    }

    @Suppress("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem == newItem
    }
}
