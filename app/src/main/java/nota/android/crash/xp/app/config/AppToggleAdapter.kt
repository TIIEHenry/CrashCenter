package nota.android.crash.xp.app.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.recyclerhelper.RecyclerAdapter
import nota.android.crash.xp.app.recyclerhelper.ViewHolder

class AppToggleAdapter(
    dataList: MutableList<AppItem>,
) : RecyclerAdapter<AppItem>(dataList) {

    override fun onCreateViewHolder(parent: ViewGroup, pos: Int): ViewHolder<AppItem> {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_main_appitem, parent, false)
        return ViewHolder(view)
    }

    override fun bindData(holder: ViewHolder<AppItem>, data: AppItem, pos: Int) {
        with(holder) {
            val context = holder.itemView.context
            itemView.contentDescription = context.getString(
                R.string.legacy_app_row_a11y,
                data.name,
                data.packageName,
            )
            (getView(R.id.ivIcon) as ImageView).setImageDrawable(data.icon)
            (getView(R.id.tvName) as TextView).text = data.name
            val switchView = getView(R.id.sw) as SwitchMaterial
            switchView.contentDescription = context.getString(
                if (data.hookEnabled) {
                    R.string.switch_disable_intervention
                } else {
                    R.string.switch_enable_intervention
                },
            )
            switchView.isChecked = data.hookEnabled
            (getView(R.id.tvPackageName) as TextView).text = data.packageName
            getView(R.id.tvSystemBadge)?.visibility =
                if (data.isSystemApp) View.VISIBLE else View.GONE
        }
    }
}
