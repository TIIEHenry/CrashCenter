package nota.android.crash.xp.app.observe

import android.view.LayoutInflater
import android.view.ViewGroup
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.CrashEventRow
import nota.android.crash.xp.app.data.CrashEvent
import nota.android.crash.xp.app.recyclerhelper.RecyclerAdapter
import nota.android.crash.xp.app.recyclerhelper.ViewHolder

class CrashHistoryAdapter(
    dataList: MutableList<CrashEvent>,
) : RecyclerAdapter<CrashEvent>(dataList) {

    override fun onCreateViewHolder(parent: ViewGroup, pos: Int): ViewHolder<CrashEvent> {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_crash_event_row, parent, false)
        return ViewHolder(view)
    }

    override fun bindData(holder: ViewHolder<CrashEvent>, data: CrashEvent, pos: Int) {
        CrashEventRow.bind(holder.itemView, data, holder.itemView.context)
    }
}
