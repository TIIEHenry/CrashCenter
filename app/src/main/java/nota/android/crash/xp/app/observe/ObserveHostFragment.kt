package nota.android.crash.xp.app.observe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.databinding.FragmentObserveHostBinding
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.crashHistoryViewModelFactory
import nota.android.crash.xp.app.shell.MainShellActivity
import nota.android.crash.xp.app.shell.ShellTab

class ObserveHostFragment : Fragment() {

    private var _binding: FragmentObserveHostBinding? = null

    private var currentTab: Int = TAB_HISTORY

    private val historyViewModel: CrashHistoryViewModel by viewModels {
        ServiceLocator.crashHistoryViewModelFactory(requireContext())
    }

    private lateinit var saveZipLauncher: ActivityResultLauncher<String>
    private lateinit var menuActions: CrashHistoryMenuActions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveZipLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri != null && ::menuActions.isInitialized) {
                menuActions.writeZipToUri(uri)
            }
        }
        menuActions = CrashHistoryMenuActions(
            fragment = this,
            viewModel = historyViewModel,
            launchSaveZip = { name -> saveZipLauncher.launch(name) },
        )
    }

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

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_history))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_stats))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_logcat))

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

    fun selectSubTab(tab: Int) {
        _binding?.tabLayout?.getTabAt(tab)?.select()
    }

    fun openConfigTab() {
        (requireActivity() as? MainShellActivity)?.requestShellTab(ShellTab.CONFIG)
    }

    private fun switchToTab(position: Int) {
        val tag = tagForTab(position)
        if (position == currentTab && childFragmentManager.findFragmentByTag(tag) != null) {
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
                TAB_LOGCAT -> replace(
                    R.id.observeContent,
                    LogcatFragment.newInstance(),
                    LogcatFragment.TAG,
                )
            }
        }
        activity?.invalidateOptionsMenu()
    }

    private fun tagForTab(position: Int): String = when (position) {
        TAB_HISTORY -> CrashHistoryFragment.TAG
        TAB_STATS -> CrashStatsFragment.TAG
        TAB_LOGCAT -> LogcatFragment.TAG
        else -> CrashHistoryFragment.TAG
    }

    fun prepareOptionsMenu(menu: Menu) {
        val historyOnlyIds = intArrayOf(
            R.id.item_observe_filter,
            R.id.item_observe_package_filter,
            R.id.item_observe_sort,
        )
        val sharedIds = intArrayOf(
            R.id.item_observe_export,
            R.id.item_observe_retention,
            R.id.item_clear_history,
        )

        when (currentTab) {
            TAB_LOGCAT -> {
                for (id in historyOnlyIds + sharedIds) menu.findItem(id)?.isVisible = false
                menu.findItem(R.id.item_observe_import_logcat)?.isVisible = true
            }
            TAB_HISTORY -> {
                for (id in historyOnlyIds + sharedIds) menu.findItem(id)?.isVisible = true
                menu.findItem(R.id.item_observe_import_logcat)?.isVisible = false
                menuActions.prepareMenu(menu)
            }
            TAB_STATS -> {
                for (id in historyOnlyIds) menu.findItem(id)?.isVisible = false
                for (id in sharedIds) menu.findItem(id)?.isVisible = true
                menu.findItem(R.id.item_observe_import_logcat)?.isVisible = false
            }
        }
    }

    fun handleOptionsItem(item: MenuItem): Boolean {
        return when (currentTab) {
            TAB_LOGCAT -> logcatFragment()?.handleOptionsItem(item) == true
            TAB_HISTORY, TAB_STATS -> menuActions.handleItem(item)
            else -> false
        }
    }

    private fun logcatFragment(): LogcatFragment? {
        return childFragmentManager.findFragmentByTag(LogcatFragment.TAG) as? LogcatFragment
    }

    companion object {
        const val TAG = "observe"
        private const val TAB_HISTORY = 0
        private const val TAB_STATS = 1
        private const val TAB_LOGCAT = 2

        fun newInstance(): ObserveHostFragment = ObserveHostFragment()
    }
}
