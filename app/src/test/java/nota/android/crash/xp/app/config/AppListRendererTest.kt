package nota.android.crash.xp.app.config

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nota.android.crash.xp.app.common.ui.adapter.BaseListAdapter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppListRendererTest {

    private lateinit var context: Context
    private lateinit var recyclerView: RecyclerView
    private lateinit var filterChipRowRoot: LinearLayout
    private lateinit var countLabel: TextView
    private lateinit var adapter: TestAdapter
    private lateinit var renderer: AppListRenderer<AppItem>

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()

        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
        }

        countLabel = TextView(context).apply {
            id = View.generateViewId()
        }
        filterChipRowRoot = LinearLayout(context).apply {
            addView(countLabel)
        }

        adapter = TestAdapter()
        renderer = AppListRenderer(
            recyclerView = recyclerView,
            filterChipRowRoot = filterChipRowRoot,
            countLabelId = countLabel.id,
            adapter = adapter,
            dataSelector = { it.allApps },
        )
    }

    // ─── render ───

    @Test
    fun `render sets adapter on recyclerView`() {
        val state = ConfigUiState(
            allApps = listOf(
                fakeAppItem("com.a", "App A"),
                fakeAppItem("com.b", "App B"),
            )
        )

        renderer.render(state)

        assertEquals(adapter, recyclerView.adapter)
    }

    @Test
    fun `render submits list to adapter`() {
        val apps = listOf(
            fakeAppItem("com.a", "App A"),
            fakeAppItem("com.b", "App B"),
            fakeAppItem("com.c", "App C"),
        )
        val state = ConfigUiState(allApps = apps)

        renderer.render(state)

        assertEquals(3, adapter.currentList.size)
        assertEquals("App A", adapter.currentList[0].label)
        assertEquals("App B", adapter.currentList[1].label)
        assertEquals("App C", adapter.currentList[2].label)
    }

    @Test
    fun `render sets correct count text in label`() {
        val state = ConfigUiState(
            allApps = listOf(
                fakeAppItem("com.a", "App A"),
                fakeAppItem("com.b", "App B"),
                fakeAppItem("com.c", "App C"),
            )
        )

        renderer.render(state)

        val countText = countLabel.text.toString()
        assertEquals("3 apps", countText)
    }

    @Test
    fun `render returns correct item count`() {
        val state = ConfigUiState(
            allApps = listOf(
                fakeAppItem("com.a", "App A"),
                fakeAppItem("com.b", "App B"),
            )
        )

        val count = renderer.render(state)

        assertEquals(2, count)
    }

    @Test
    fun `render with empty list shows zero count`() {
        val state = ConfigUiState(allApps = emptyList())

        val count = renderer.render(state)

        assertEquals(0, count)
        assertEquals(adapter, recyclerView.adapter)
        val countText = countLabel.text.toString()
        assertEquals("0 apps", countText)
    }

    // ─── setVisibility ───

    @Test
    fun `setVisibility true makes root visible`() {
        renderer.setVisibility(true)
        assertEquals(View.VISIBLE, filterChipRowRoot.visibility)
    }

    @Test
    fun `setVisibility false makes root gone`() {
        renderer.setVisibility(false)
        assertEquals(View.GONE, filterChipRowRoot.visibility)
    }

    // ─── Helpers ───

    private class TestAdapter : BaseListAdapter<AppItem, TestAdapter.VH>(
        object : DiffUtil.ItemCallback<AppItem>() {
            override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean =
                oldItem == newItem
        }
    ) {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            return VH(View(parent.context))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {}

        class VH(itemView: View) : BaseViewHolder<AppItem>(itemView) {
            override fun bind(data: AppItem) {}
        }
    }

    private fun fakeAppItem(
        packageName: String,
        label: String,
    ): AppItem = AppItem(
        label = label,
        appInfo = android.content.pm.ApplicationInfo().apply {
            this.packageName = packageName
        },
        hookEnabled = false,
        packageName = packageName,
        isSystem = false,
        updateTime = 0L,
        installTime = 0L,
    )
}
