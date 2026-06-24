package nota.android.crash.xp.app.config

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.CallbackSuppressor
import nota.android.crash.xp.app.common.ui.DenseSearchField
import nota.android.crash.xp.app.common.ui.FilterChipRow
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.common.ui.showErrorToast
import nota.android.crash.xp.app.databinding.FragmentConfigBinding
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.configViewModelFactory
import nota.android.crash.xp.app.observe.PerAppCrashActivity

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private val viewModel: ConfigViewModel by viewModels {
        ServiceLocator.configViewModelFactory(requireContext())
    }

    private lateinit var optionsMenuHelper: ConfigOptionsMenuHelper
    private lateinit var dialogHelper: ConfigDialogHelper
    private lateinit var adapter: AppToggleAdapter
    private var returningFromPermissionSettings = false
    private val suppressChipCallbacks = CallbackSuppressor()
    private var xposedDialogShown = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHelpers()
        setupAdapter()
        setupSettingsChips()
        setupSortChips()
        setupInterceptFilterChips()
        setupSearch()
        setupRecyclerView()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
        viewModel.loadApps(forceReload = savedInstanceState == null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (returningFromPermissionSettings) {
            returningFromPermissionSettings = false
            viewModel.loadApps(forceReload = true)
            return
        }
        viewModel.loadApps(forceReload = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun prepareOptionsMenu(menu: Menu) {
        val listCount = viewModel.uiState.value.visibleApps.size
        optionsMenuHelper.prepareOptionsMenu(menu, listCount)
    }

    fun handleOptionsItem(item: MenuItem): Boolean {
        return optionsMenuHelper.handleOptionsItem(item)
    }

    private fun setupHelpers() {
        optionsMenuHelper = ConfigOptionsMenuHelper(
            viewModel = viewModel,
            showAddManagedAppSheet = {}, // removed, no-op
            showHelpDialog = { dialogHelper.showHelpDialog() },
            showTestCrashDialog = { dialogHelper.showTestCrashDialog(::triggerTestCrash) },
        )
        dialogHelper = ConfigDialogHelper(
            context = requireContext(),
            prefs = ServiceLocator.prefs(requireContext()),
            onPermissionSettingsOpened = {
                returningFromPermissionSettings = true
            },
        )
    }

    private fun setupAdapter() {
        adapter = AppToggleAdapter(
            onToggle = { app -> viewModel.toggleIntercept(app.packageName) },
        ).apply {
            onItemClick { _, app, _ ->
                openPerAppCrash(app.packageName)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerv.apply {
            itemAnimator = DefaultItemAnimator().apply { addDuration = 100 }
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = this@ConfigFragment.adapter
        }
    }

    private fun setupSettingsChips() {
        val row = binding.settingChipRow.root
        FilterChipRow.chip(row, R.id.setting_chipGroup, R.id.chipHandleSystem)?.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressChipCallbacks.suppressed) viewModel.setHandleSystem(isChecked)
        }
        FilterChipRow.chip(row, R.id.setting_chipGroup, R.id.chipShowSystem)?.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressChipCallbacks.suppressed) viewModel.setShowSystemUi(isChecked)
        }
    }

    private fun setupSortChips() {
        val row = binding.sortChipRow.root
        FilterChipRow.setOnSingleSelectionChangeListener(row, R.id.sort_chipGroup) { _, checkedIds ->
            val mode = SORT_CHIP_TO_MODE[checkedIds.firstOrNull()] ?: return@setOnSingleSelectionChangeListener
            viewModel.setSortMode(mode)
        }
    }

    private fun setupInterceptFilterChips() {
        val row = binding.interceptFilterChipRow.root
        val chipToFilter = mapOf(
            R.id.hook_chipAll to InterceptFilter.ALL,
            R.id.hook_chipOn to InterceptFilter.ENABLED,
            R.id.hook_chipOff to InterceptFilter.DISABLED,
        )
        FilterChipRow.setOnSingleSelectionChangeListener(row, R.id.hook_chipGroup) { _, checkedIds ->
            val filter = chipToFilter[checkedIds.firstOrNull()] ?: return@setOnSingleSelectionChangeListener
            viewModel.setInterceptFilter(filter)
        }
    }

    private fun setupSearch() {
        DenseSearchField.setOnQueryChangeListener(binding.searchField.root, viewModel::setQuery)
    }

    private fun renderState(state: ConfigUiState) {
        val settingsRow = binding.settingChipRow.root
        suppressChipCallbacks.run {
            FilterChipRow.setChipChecked(settingsRow, R.id.setting_chipGroup, R.id.chipHandleSystem, state.handleSystem)
            FilterChipRow.setChipChecked(settingsRow, R.id.setting_chipGroup, R.id.chipShowSystem, state.showSystemUi)
        }
        val sortChipId = MODE_TO_SORT_CHIP[state.sortMode] ?: MODE_TO_SORT_CHIP[SortMode.UPDATE_TIME_DESC]!!
        suppressChipCallbacks.run {
            FilterChipRow.setChipChecked(binding.sortChipRow.root, R.id.sort_chipGroup, sortChipId, true)
        }
        val filterChipId = INTERCEPT_FILTER_TO_CHIP[state.interceptFilter]!!
        suppressChipCallbacks.run {
            FilterChipRow.setChipChecked(binding.interceptFilterChipRow.root, R.id.hook_chipGroup, filterChipId, true)
        }

        state.packageVisibility?.let { renderPermissionBanner(it) }

        adapter.submitList(state.visibleApps)

        binding.loadingPanel.root.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        if (state.isLoading) {
            LoadingState.bind(binding.loadingPanel.root)
        }

        val hasItems = state.visibleApps.isNotEmpty()
        val isEmpty = !state.isLoading && !hasItems
        binding.emptyState.root.visibility = if (isEmpty) View.VISIBLE else View.GONE

        requireContext().showErrorToast(state.errorMessage) { viewModel.clearError() }

        if (!state.isLoading && !xposedDialogShown) {
            xposedDialogShown = dialogHelper.showXposedInactiveDialogIfNeeded(xposedDialogShown)
        }

        activity?.invalidateMenu()
    }

    private fun openPerAppCrash(packageName: String) {
        startActivity(
            Intent(requireContext(), PerAppCrashActivity::class.java).apply {
                putExtra(PerAppCrashActivity.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    private fun renderPermissionBanner(status: nota.android.crash.xp.app.PackageVisibilityHelper.Status) {
        // Permission banner logic delegated to layout
    }

    private fun triggerTestCrash() {
        val event = nota.android.crash.common.data.CrashEvent(
            id = java.util.UUID.randomUUID().toString(),
            packageName = requireContext().packageName,
            exceptionClass = "java.lang.RuntimeException",
            message = "CrashCenter test crash — pipeline verification",
            stackTrace = "java.lang.RuntimeException: CrashCenter test crash — pipeline verification\n" +
                "\tat nota.android.crash.xp.app.config.ConfigFragment.triggerTestCrash(ConfigFragment.kt)",
            timestampMs = System.currentTimeMillis(),
            source = "test",
        )
        val file = nota.android.crash.xp.app.data.FileCrashLogRepository.eventsFile(requireContext())
        nota.android.crash.log.CanonicalJsonlWriter.append(file, event)
        android.widget.Toast.makeText(requireContext(), R.string.test_crash_recorded, android.widget.Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG = "config"

        fun newInstance(): ConfigFragment = ConfigFragment()

        private val SORT_CHIP_TO_MODE = mapOf(
            R.id.sort_chipRecent to SortMode.UPDATE_TIME_DESC,
            R.id.sort_chipName to SortMode.NAME_ASC,
        )
        private val MODE_TO_SORT_CHIP = SORT_CHIP_TO_MODE.entries.associate { it.value to it.key }

        private val INTERCEPT_FILTER_TO_CHIP = mapOf(
            InterceptFilter.ALL to R.id.hook_chipAll,
            InterceptFilter.ENABLED to R.id.hook_chipOn,
            InterceptFilter.DISABLED to R.id.hook_chipOff,
        )
    }
}
