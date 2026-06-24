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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.data.CrashSortMode
import nota.android.crash.xp.app.databinding.FragmentObserveHostBinding
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.crashHistoryViewModelFactory

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

        val pagerAdapter = ObservePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                TAB_HISTORY -> getString(R.string.tab_history)
                TAB_STATS -> getString(R.string.tab_stats)
                TAB_LOGCAT -> getString(R.string.tab_logcat)
                else -> ""
            }
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentTab = position
                binding.sortChipRow.visibility = if (position == TAB_HISTORY) View.VISIBLE else View.GONE
                activity?.invalidateMenu()
            }
        })

        if (savedInstanceState != null) {
            currentTab = binding.viewPager.currentItem
        }
        binding.sortChipRow.visibility = if (currentTab == TAB_HISTORY) View.VISIBLE else View.GONE

        setupSortChips(binding)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                historyViewModel.uiState.collect { syncSortChip(it.sortMode) }
            }
        }
    }

    private fun setupSortChips(binding: FragmentObserveHostBinding) {
        val chipToMode = mapOf(
            R.id.item_sort_time_newest to CrashSortMode.TIME_NEWEST,
            R.id.item_sort_time_oldest to CrashSortMode.TIME_OLDEST,
            R.id.item_sort_package_asc to CrashSortMode.PACKAGE_ASC,
            R.id.item_sort_package_desc to CrashSortMode.PACKAGE_DESC,
            R.id.item_sort_exception_asc to CrashSortMode.EXCEPTION_ASC,
            R.id.item_sort_exception_desc to CrashSortMode.EXCEPTION_DESC,
        )
        binding.sortChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val mode = chipToMode[checkedId] ?: return@setOnCheckedStateChangeListener
            historyViewModel.setSortMode(mode)
        }
        syncSortChip(historyViewModel.uiState.value.sortMode)
    }

    private fun syncSortChip(mode: CrashSortMode) {
        val binding = _binding ?: return
        val chipId = when (mode) {
            CrashSortMode.TIME_NEWEST -> R.id.item_sort_time_newest
            CrashSortMode.TIME_OLDEST -> R.id.item_sort_time_oldest
            CrashSortMode.PACKAGE_ASC -> R.id.item_sort_package_asc
            CrashSortMode.PACKAGE_DESC -> R.id.item_sort_package_desc
            CrashSortMode.EXCEPTION_ASC -> R.id.item_sort_exception_asc
            CrashSortMode.EXCEPTION_DESC -> R.id.item_sort_exception_desc
        }
        val chip = binding.root.findViewById<Chip>(chipId)
        if (chip != null && !chip.isChecked) {
            chip.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun selectSubTab(tab: Int) {
        _binding?.viewPager?.setCurrentItem(tab, true)
    }

    fun openLogcatTab() {
        selectSubTab(TAB_LOGCAT)
    }

    fun prepareOptionsMenu(menu: Menu) {
        val historyOnlyIds = intArrayOf(
            R.id.item_observe_filter,
            R.id.item_observe_package_filter,
        )
        val sharedIds = intArrayOf(
            R.id.item_observe_export,
            R.id.item_observe_retention,
            R.id.item_clear_history,
        )

        when (currentTab) {
            TAB_LOGCAT -> {
                for (id in historyOnlyIds + sharedIds) menu.findItem(id)?.isVisible = false
                menu.findItem(R.id.item_observe_refresh_logcat)?.isVisible = true
                menu.findItem(R.id.item_observe_import_logcat)?.isVisible = true
            }
            TAB_HISTORY -> {
                for (id in historyOnlyIds + sharedIds) menu.findItem(id)?.isVisible = true
                menu.findItem(R.id.item_observe_refresh_logcat)?.isVisible = false
                menu.findItem(R.id.item_observe_import_logcat)?.isVisible = false
                menuActions.prepareMenu(menu)
            }
            TAB_STATS -> {
                for (id in historyOnlyIds) menu.findItem(id)?.isVisible = false
                for (id in sharedIds) menu.findItem(id)?.isVisible = true
                menu.findItem(R.id.item_observe_refresh_logcat)?.isVisible = false
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
        return childFragmentManager.findFragmentByTag("f$TAB_LOGCAT") as? LogcatFragment
    }

    companion object {
        const val TAG = "observe"
        private const val TAB_HISTORY = 0
        private const val TAB_STATS = 1
        private const val TAB_LOGCAT = 2

        fun newInstance(): ObserveHostFragment = ObserveHostFragment()
    }
}
