package nota.android.crash.xp.app.config

import android.view.LayoutInflater
import android.view.ViewGroup
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.ManagedAppRow
import nota.android.crash.xp.app.recyclerhelper.RecyclerAdapter
import nota.android.crash.xp.app.recyclerhelper.ViewHolder

class ManagedAppAdapter(
    dataList: MutableList<ManagedApp>,
) : RecyclerAdapter<ManagedApp>(dataList) {

    var onSwitchChanged: ((ManagedApp, Boolean) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, pos: Int): ViewHolder<ManagedApp> {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_managed_app_row, parent, false)
        return ViewHolder(view)
    }

    override fun bindData(holder: ViewHolder<ManagedApp>, data: ManagedApp, pos: Int) {
        ManagedAppRow.bind(holder.rootView, data, holder.rootView.context, onSwitchChanged)
    }
}
