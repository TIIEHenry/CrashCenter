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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.DenseSearchField
import nota.android.crash.xp.app.common.ui.FilterChipRow
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.databinding.FragmentConfigBinding
import nota.android.crash.xp.app.di.ServiceLocator

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private val viewModel: ConfigViewModel by viewModels {
        ConfigViewModel.Factory(
            ServiceLocator.appRepository(requireContext()),
        )
    }

    private lateinit var legacyAdapter: AppToggleAdapter
    private lateinit var managedAdapter: ManagedAppAdapter
    private lateinit var legacyRenderer: LegacyRenderer
    private lateinit var managedRenderer: ManagedRenderer
    private lateinit var permissionBannerRenderer: PermissionBannerRenderer
    private lateinit var emptyStateRenderer: EmptyStateRenderer
    private lateinit var optionsMenuHelper: ConfigOptionsMenuHelper
    private lateinit var dialogHelper: ConfigDialogHelper
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
        setupHelpers()
        setupList()
        setupSettingsChips()
        setupSearch()
        setupManagedFilterChips()
        setupLegacyFilterChips()
        legacyRenderer = LegacyRenderer(binding, legacyAdapter)
        managedRenderer = ManagedRenderer(binding, managedAdapter)
        permissionBannerRenderer = PermissionBannerRenderer(binding, ::openPermissionRationaleDialog)
        emptyStateRenderer = EmptyStateRenderer(binding, ::showAddManagedAppSheet)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { renderState(it) }
            }
        }
        viewModel.loadApps(forceReload = savedInstanceState == null)
    }

    override fun onResume() {
        super.onResume()
        if (returningFromPermissionSettings) {
            returningFromPermissionSettings = false
            viewModel.loadApps(forceReload = true)
            return
        }
        if (viewModel.uiState.value.isLegacyMode == false) {
            viewModel.loadApps(forceReload = true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun prepareOptionsMenu(menu: Menu) {
        optionsMenuHelper.prepareOptionsMenu(menu, viewModel.uiState.value.isLegacyMode)
    }

    fun handleOptionsItem(item: MenuItem): Boolean {
        return optionsMenuHelper.handleOptionsItem(item)
    }

    internal fun showTestToastAndCrash() {
        Toast.makeText(requireContext(), R.string.test_hint, Toast.LENGTH_LONG).show()
        lifecycleScope.launch {
            delay(2000)
            throw RuntimeException("just for test")
        }
    }

    private fun setupHelpers() {
        optionsMenuHelper = ConfigOptionsMenuHelper(
            showTestToastAndCrash = ::showTestToastAndCrash,
            viewModel = viewModel,
            showAddManagedAppSheet = ::showAddManagedAppSheet,
            showHelpDialog = { dialogHelper.showHelpDialog() },
        )
        dialogHelper = ConfigDialogHelper(
            context = requireContext(),
            onPermissionSettingsOpened = {
                returningFromPermissionSettings = true
            },
        )
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

    private fun setupSettingsChips() {
        val row = binding.settingChipRow.root
        FilterChipRow.chip(row, R.id.setting_chipGroup, R.id.chipScopeMode)?.setOnLongClickListener {
            dialogHelper.showHelpDialog()
            true
        }
        FilterChipRow.chip(row, R.id.setting_chipGroup, R.id.chipScopeMode)?.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressChipCallbacks) viewModel.setScopeMode(isChecked)
        }
        FilterChipRow.chip(row, R.id.setting_chipGroup, R.id.chipHandleSystem)?.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressChipCallbacks) viewModel.setHandleSystem(isChecked)
        }
        FilterChipRow.chip(row, R.id.setting_chipGroup, R.id.chipShowSystem)?.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressChipCallbacks) viewModel.setShowSystemUi(isChecked)
        }
    }

    private fun setupSearch() {
        DenseSearchField.setOnQueryChangeListener(binding.searchField.root, viewModel::setQuery)
    }

    private fun setupManagedFilterChips() {
        FilterChipRow.setOnSingleSelectionChangeListener(binding.managedFilterChipRow.root, R.id.managed_chipGroup) { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.managed_chipEnabled -> ManagedFilter.ENABLED
                R.id.managed_chipPending -> ManagedFilter.PENDING
                else -> ManagedFilter.ALL
            }
            viewModel.setManagedFilter(filter)
        }
    }

    private fun setupLegacyFilterChips() {
        FilterChipRow.setOnSingleSelectionChangeListener(binding.hookFilterChipRow.root, R.id.hook_chipGroup) { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.hook_chipOn -> HookFilter.ON
                R.id.hook_chipOff -> HookFilter.OFF
                else -> HookFilter.ALL
            }
            viewModel.setHookFilter(filter)
        }
    }

    private fun renderState(state: ConfigUiState) {
        suppressChipCallbacks = true
        val settingsRow = binding.settingChipRow.root
        FilterChipRow.setChipChecked(settingsRow, R.id.setting_chipGroup, R.id.chipScopeMode, state.scopeMode)
        FilterChipRow.setChipChecked(settingsRow, R.id.setting_chipGroup, R.id.chipHandleSystem, state.handleSystem)
        FilterChipRow.setChipChecked(settingsRow, R.id.setting_chipGroup, R.id.chipShowSystem, state.showSystemUi)
        suppressChipCallbacks = false

        state.packageVisibility?.let { permissionBannerRenderer.render(it) }

        legacyRenderer.setVisibility(state.isLegacyMode)
        managedRenderer.setVisibility(!state.isLegacyMode)

        val listCount = if (state.isLegacyMode) {
            legacyRenderer.render(state)
        } else {
            managedRenderer.render(state)
        }

        binding.loadingPanel.root.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        if (state.isLoading) {
            LoadingState.bind(binding.loadingPanel.root)
        }

        emptyStateRenderer.render(state, listCount)

        if (!state.isLoading && !xposedDialogShown) {
            xposedDialogShown = dialogHelper.showXposedInactiveDialogIfNeeded(xposedDialogShown)
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

    private fun openPermissionRationaleDialog() {
        dialogHelper.showPermissionRationaleDialog()
    }

    companion object {
        const val TAG = "config"

        fun newInstance(): ConfigFragment = ConfigFragment()
    }
}
