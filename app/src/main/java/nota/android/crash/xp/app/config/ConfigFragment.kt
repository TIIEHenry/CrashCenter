package nota.android.crash.xp.app.config

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.ModuleActivation
import nota.android.crash.xp.app.PackageVisibilityHelper
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.DenseSearchField
import nota.android.crash.xp.app.common.ui.EmptyState
import nota.android.crash.xp.app.common.ui.FilterChipRow
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.common.ui.PermissionBanner
import nota.android.crash.xp.app.databinding.FragmentConfigBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConfigViewModel by viewModels {
        val context = requireContext()
        ConfigViewModel.Factory(
            AppRepository(context),
        )
    }

    private lateinit var legacyAdapter: AppToggleAdapter
    private lateinit var managedAdapter: ManagedAppAdapter
    private var returningFromPermissionSettings = false
    private var suppressChipCallbacks = false
    private var xposedDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            AddManagedAppBottomSheet.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val packages = bundle.getStringArrayList(AddManagedAppBottomSheet.ARG_PACKAGES).orEmpty()
            viewModel.addManagedPackages(packages)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupSettingsChips()
        setupSearch()
        setupManagedFilterChips()
        setupLegacyFilterChips()
        setupPermissionBanner()
        viewModel.uiState.observe(viewLifecycleOwner, ::renderState)
        viewModel.loadApps(forceReload = savedInstanceState == null)
    }

    override fun onResume() {
        super.onResume()
        if (returningFromPermissionSettings) {
            returningFromPermissionSettings = false
            viewModel.loadApps(forceReload = true)
            return
        }
        if (viewModel.uiState.value?.isLegacyMode == false) {
            viewModel.loadApps(forceReload = true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun prepareOptionsMenu(menu: Menu) {
        val legacy = viewModel.uiState.value?.isLegacyMode ?: true
        menu.findItem(R.id.item_select_all)?.isVisible = legacy
        menu.findItem(R.id.item_cancel_all)?.isVisible = legacy
        menu.findItem(R.id.item_add_managed_app)?.isVisible = !legacy
    }

    fun handleOptionsItem(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_add_managed_app -> {
                showAddManagedAppSheet()
                true
            }
            R.id.item_cancel_all -> {
                viewModel.selectAll(enabled = false)
                true
            }
            R.id.item_select_all -> {
                viewModel.selectAll(enabled = true)
                true
            }
            R.id.item_help -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.using_warning_title))
                    .setMessage(getString(R.string.using_warning))
                    .show()
                true
            }
            R.id.item_test -> {
                Toast.makeText(requireContext(), R.string.test_hint, Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    delay(2000)
                    throw RuntimeException("just for test")
                }
                true
            }
            R.id.item_sort_by_name -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.NAME_ASC)
                true
            }
            R.id.item_sort_by_name_reverse -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.NAME_DESC)
                true
            }
            R.id.item_sort_by_install_time -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.INSTALL_TIME_ASC)
                true
            }
            R.id.item_sort_by_install_time_reverse -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.INSTALL_TIME_DESC)
                true
            }
            R.id.item_sort_by_update_time -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.UPDATE_TIME_ASC)
                true
            }
            R.id.item_sort_by_update_time_reverse -> {
                item.isChecked = true
                viewModel.setSortMode(SortMode.UPDATE_TIME_DESC)
                true
            }
            else -> false
        }
    }

    private fun setupList() {
        legacyAdapter = AppToggleAdapter()
        legacyAdapter.onItemClick { _, data, _ ->
            viewModel.toggleApp(data.packageName)
        }

        managedAdapter = ManagedAppAdapter()
        managedAdapter.onSwitchChanged = { app, enabled ->
            viewModel.setManagedSwitch(app.packageName, enabled)
        }
        managedAdapter.onItemClick { _, data, _ ->
            openInterventionEdit(data.packageName)
        }

        binding.recyclerv.apply {
            itemAnimator = DefaultItemAnimator().apply { addDuration = 100 }
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        }
    }

    private fun setupPermissionBanner() {
        PermissionBanner.setOnActionClickListener(binding.permissionBanner.root, View.OnClickListener {
            showPermissionRationaleDialog()
        })
    }

    private fun setupSettingsChips() {
        val row = binding.settingChipRow.root
        FilterChipRow.chip(row, R.id.chipScopeMode)?.setOnLongClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.item_scope_mode)
                .setMessage(R.string.scope_mode_description)
                .show()
            true
        }
        FilterChipRow.chip(row, R.id.chipScopeMode)?.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressChipCallbacks) viewModel.setScopeMode(isChecked)
        }
        FilterChipRow.chip(row, R.id.chipHandleSystem)?.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressChipCallbacks) viewModel.setHandleSystem(isChecked)
        }
        FilterChipRow.chip(row, R.id.chipShowSystem)?.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressChipCallbacks) viewModel.setShowSystemUi(isChecked)
        }
    }

    private fun setupSearch() {
        DenseSearchField.setOnQueryChangeListener(binding.searchField.root, viewModel::setQuery)
    }

    private fun setupManagedFilterChips() {
        FilterChipRow.setOnSingleSelectionChangeListener(binding.managedFilterChipRow.root) { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipEnabled -> ManagedFilter.ENABLED
                R.id.chipPending -> ManagedFilter.PENDING
                else -> ManagedFilter.ALL
            }
            viewModel.setManagedFilter(filter)
        }
    }

    private fun setupLegacyFilterChips() {
        FilterChipRow.setOnSingleSelectionChangeListener(binding.hookFilterChipRow.root) { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipOn -> HookFilter.ON
                R.id.chipOff -> HookFilter.OFF
                else -> HookFilter.ALL
            }
            viewModel.setHookFilter(filter)
        }
    }

    private fun renderState(state: ConfigUiState) {
        suppressChipCallbacks = true
        val settingsRow = binding.settingChipRow.root
        FilterChipRow.setChipChecked(settingsRow, R.id.chipScopeMode, state.scopeMode)
        FilterChipRow.setChipChecked(settingsRow, R.id.chipHandleSystem, state.handleSystem)
        FilterChipRow.setChipChecked(settingsRow, R.id.chipShowSystem, state.showSystemUi)
        suppressChipCallbacks = false

        state.packageVisibility?.let { updatePermissionBanner(it) }

        binding.managedFilterChipRow.root.visibility =
            if (state.isLegacyMode) View.GONE else View.VISIBLE
        binding.hookFilterChipRow.root.visibility =
            if (state.isLegacyMode) View.VISIBLE else View.GONE

        val listCount: Int
        if (state.isLegacyMode) {
            binding.recyclerv.adapter = legacyAdapter
            legacyAdapter.setData(state.visibleApps)
            listCount = state.visibleApps.size
            FilterChipRow.setCountLabel(
                binding.hookFilterChipRow.root,
                getString(R.string.app_count_format, listCount),
            )
        } else {
            binding.recyclerv.adapter = managedAdapter
            managedAdapter.setData(state.visibleManagedApps)
            listCount = state.visibleManagedApps.size
            FilterChipRow.setCountLabel(
                binding.managedFilterChipRow.root,
                getString(R.string.app_count_format, listCount),
            )
        }

        binding.loadingPanel.root.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        if (state.isLoading) {
            LoadingState.bind(binding.loadingPanel.root)
        }

        val hasVisibleItems = !state.isLoading && listCount > 0
        val empty = !state.isLoading && listCount == 0
        binding.emptyState.root.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerv.visibility = if (hasVisibleItems) View.VISIBLE else View.GONE

        if (empty) {
            val message = when (state.emptyMessage) {
                ConfigViewModel.EMPTY_MANAGED_LIST -> getString(R.string.managed_empty_message)
                else -> getString(
                    if (state.isLegacyMode) {
                        R.string.filter_empty
                    } else {
                        R.string.managed_filter_empty
                    },
                )
            }
            val showAddAction = !state.isLegacyMode &&
                state.emptyMessage == ConfigViewModel.EMPTY_MANAGED_LIST
            EmptyState.bind(
                binding.emptyState.root,
                message,
                if (showAddAction) getString(R.string.add_managed_app) else null,
                if (showAddAction) ({ showAddManagedAppSheet() }) else null,
                if (showAddAction) R.drawable.ic_tab_config else null,
            )
        }

        if (!state.isLoading && !xposedDialogShown) {
            xposedDialogShown = true
            showXposedInactiveDialogIfNeeded()
        }

        activity?.invalidateOptionsMenu()
    }

    private fun showAddManagedAppSheet() {
        if (parentFragmentManager.findFragmentByTag(AddManagedAppBottomSheet.TAG) != null) return
        AddManagedAppBottomSheet.newInstance()
            .show(parentFragmentManager, AddManagedAppBottomSheet.TAG)
    }

    private fun openInterventionEdit(packageName: String) {
        startActivity(
            Intent(requireContext(), AppInterventionEditActivity::class.java).apply {
                putExtra(AppInterventionEditActivity.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    private fun showXposedInactiveDialogIfNeeded() {
        val context = requireContext()
        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PrefManager.PREF_XPOSED_DIALOG_DISMISSED, false)) return
        if (ModuleActivation.isModuleActive()) return

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.xposed_not_active)
            .setMessage(R.string.xposed_hint)
            .setNeutralButton(R.string.btn_dont_show_again) { _, _ ->
                prefs.edit().putBoolean(PrefManager.PREF_XPOSED_DIALOG_DISMISSED, true).apply()
            }
            .show()
    }

    private fun updatePermissionBanner(status: PackageVisibilityHelper.Status) {
        val compact = !ModuleActivation.isModuleActive()
        val title = if (status.visiblePackageCount > 0) {
            getString(
                if (compact) R.string.permission_list_partial_hint_compact else R.string.permission_list_partial_hint,
                status.visiblePackageCount,
            )
        } else {
            getString(
                if (compact) R.string.permission_banner_title_compact else R.string.permission_banner_title,
            )
        }
        PermissionBanner.bind(binding.permissionBanner.root, status.needsUserAction, title, compact)
    }

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_rationale_title)
            .setMessage(R.string.permission_rationale_message)
            .setPositiveButton(R.string.permission_open_settings) { _, _ ->
                returningFromPermissionSettings = true
                if (!PackageVisibilityHelper.openAppSettings(requireContext())) {
                    returningFromPermissionSettings = false
                    Toast.makeText(requireContext(), R.string.permission_settings_failed, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val TAG = "config"

        fun newInstance(): ConfigFragment = ConfigFragment()
    }
}
