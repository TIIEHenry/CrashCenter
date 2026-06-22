package nota.android.crash.xp.app.observe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.tabs.TabLayout
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.FragmentObserveHostBinding

class ObserveHostFragment : Fragment() {

    private var _binding: FragmentObserveHostBinding? = null

    private var currentTab: Int = TAB_HISTORY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentObserveHostBinding.inflate(inflater, container, false)
        return checkNotNull(_binding) { "Binding accessed after onDestroyView" }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = checkNotNull(_binding)

        // Add tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_history))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_stats))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                switchToTab(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        if (savedInstanceState == null) {
            currentTab = TAB_HISTORY
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(TAB_HISTORY))
            childFragmentManager.commit {
                replace(R.id.observeContent, CrashHistoryFragment.newInstance(), CrashHistoryFragment.TAG)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun switchToTab(position: Int) {
        if (position == currentTab && childFragmentManager.findFragmentByTag(
                if (position == TAB_HISTORY) CrashHistoryFragment.TAG else CrashStatsFragment.TAG
            ) != null
        ) {
            return
        }
        currentTab = position
        childFragmentManager.commit {
            when (position) {
                TAB_HISTORY -> replace(
                    R.id.observeContent,
                    CrashHistoryFragment.newInstance(),
                    CrashHistoryFragment.TAG,
                )
                TAB_STATS -> replace(
                    R.id.observeContent,
                    CrashStatsFragment.newInstance(),
                    CrashStatsFragment.TAG,
                )
            }
        }
    }

    // ─── Options Menu forwarding ───

    fun prepareOptionsMenu(menu: Menu) {
        crashHistoryFragment()?.prepareOptionsMenu(menu)
    }

    fun handleOptionsItem(item: MenuItem): Boolean {
        return crashHistoryFragment()?.handleOptionsItem(item) == true
    }

    private fun crashHistoryFragment(): CrashHistoryFragment? {
        return childFragmentManager.findFragmentByTag(CrashHistoryFragment.TAG) as? CrashHistoryFragment
    }

    companion object {
        const val TAG = "observe"
        private const val TAB_HISTORY = 0
        private const val TAB_STATS = 1

        fun newInstance(): ObserveHostFragment = ObserveHostFragment()
    }
}
