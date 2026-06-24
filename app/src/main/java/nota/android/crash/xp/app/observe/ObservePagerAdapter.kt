package nota.android.crash.xp.app.observe

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ObservePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun getItemId(position: Int): Long = position.toLong()

    override fun containsItem(itemId: Long): Boolean = itemId in 0..2

    override fun createFragment(position: Int): Fragment = when (position) {
        TAB_HISTORY -> CrashHistoryFragment.newInstance()
        TAB_STATS -> CrashStatsFragment.newInstance()
        TAB_LOGCAT -> LogcatFragment.newInstance()
        else -> throw IllegalArgumentException("Unknown position: $position")
    }

    companion object {
        const val TAB_HISTORY = 0
        const val TAB_STATS = 1
        const val TAB_LOGCAT = 2
    }
}
