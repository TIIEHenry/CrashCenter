package nota.android.crash.xp.app.config

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.recyclerhelper.RecyclerAdapter
import nota.android.crash.xp.app.recyclerhelper.ViewHolder

class PickableAppAdapter(
    dataList: MutableList<PickableApp>,
) : RecyclerAdapter<PickableApp>(dataList) {

    private val selectedPackages = linkedSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, pos: Int): ViewHolder<PickableApp> {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pickable_app, parent, false)
        return ViewHolder(view)
    }

    override fun bindData(holder: ViewHolder<PickableApp>, data: PickableApp, pos: Int) {
        with(holder) {
            (getView(R.id.ivIcon) as ImageView).setImageDrawable(data.icon)
            (getView(R.id.tvName) as TextView).text = data.label
            (getView(R.id.tvPackageName) as TextView).text = data.packageName
            val checkbox = getView(R.id.checkbox) as CheckBox
            checkbox.isChecked = selectedPackages.contains(data.packageName)
        }
    }

    fun toggleSelection(app: PickableApp) {
        if (!selectedPackages.add(app.packageName)) {
            selectedPackages.remove(app.packageName)
        }
        notifyDataSetChanged()
    }

    fun selectedPackages(): Set<String> = selectedPackages.toSet()

    fun clearSelection() {
        selectedPackages.clear()
        notifyDataSetChanged()
    }
}
