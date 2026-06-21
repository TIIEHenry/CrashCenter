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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.common.ui.CallbackSuppressor
import nota.android.crash.xp.app.common.ui.showErrorToast
import nota.android.crash.xp.app.common.ui.DenseSearchField
import nota.android.crash.xp.app.common.ui.FilterChipRow
import nota.android.crash.xp.app.common.ui.LoadingState
import nota.android.crash.xp.app.databinding.FragmentConfigBinding
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.configViewModelFactory

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding accessed after onDestroyView" }

    private val viewModel: ConfigViewModel by viewModels {
        ServiceLocator.configViewModelFactory(requireContext())
    }

    private lateinit var legacyController: ConfigListController<AppItem>
    private lateinit var managedController: ConfigListController<ManagedApp>
    private lateinit var permissionBannerRenderer: PermissionBannerRenderer
    private lateinit var emptyStateRenderer: EmptyStateRenderer
    private lateinit var optionsMenuHelper: ConfigOptionsMenuHelper
    private lateinit var dialogHelper: ConfigDialogHelper
    private var returningFromPermissionSettings = false
    private val suppressChipCallbacks = CallbackSuppressor()
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
        setupControllers()
        setupSettingsChips()
        setupSearch()
        setupRecyclerView()
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

    private fun setupHelpers() {
        optionsMenuHelper = ConfigOptionsMenuHelper(
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

    private fun setupControllers() {
        val legacyAdapter = AppToggleAdapter().apply {
            onItemClick { _, data, _ -> viewModel.toggleApp(data.packageName) }
        }
        legacyController = ConfigListController(
            binding = binding,
            adapter = legacyAdapter,
            countLabelId = R.id.hook_countLabel,
            dataSelector = { it.visibleApps },
            filterConfig = FilterConfig(
                chipRowRoot = binding.hookFilterChipRow.root,
                chipGroupId = R.id.hook_chipGroup,
                chipToFilter = mapOf(
                    R.id.hook_chipOn to HookFilter.ON,
                    R.id.hook_chipOff to HookFilter.OFF,
                ),
                defaultFilter = HookFilter.ALL,
                onFilterChanged = viewModel::setHookFilter,
            ),
        )

        val managedAdapter = ManagedAppAdapter().apply {
            onSwitchChanged = { app, enabled -> viewModel.setManagedSwitch(app.packageName, enabled) }
            onItemClick { _, data, _ -> openInterventionEdit(data.packageName) }
        }
        managedController = ConfigListController(
            binding = binding,
            adapter = managedAdapter,
            countLabelId = R.id.managed_countLabel,
            dataSelector = { it.visibleManagedApps },
            filterConfig = FilterConfig(
                chipRowRoot = binding.managedFilterChipRow.root,
                chipGroupId = R.id.managed_chipGroup,
                chipToFilter = mapOf(
                    R.id.managed_chipEnabled to ManagedFilter.ENABLED,
                    R.id.managed_chipPending to ManagedFilter.PENDING,
                ),
                defaultFilter = ManagedFilter.ALL,
                onFilterChanged = viewModel::setManagedFilter,
            ),
        )
    }

    private fun setupRecyclerView() {
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
            if (!suppressChipCallbacks.suppressed) viewModel.setScopeMode(isChecked)
        }
        FilterChipRow.chip(row, R.id.setting_chipGroup, R.id.chipHandleSystem)?.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressChipCallbacks.suppressed) viewModel.setHandleSystem(isChecked)
        }
        FilterChipRow.chip(row, R.id.setting_chipGroup, R.id.chipShowSystem)?.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressChipCallbacks.suppressed) viewModel.setShowSystemUi(isChecked)
        }
    }

    private fun setupSearch() {
        DenseSearchField.setOnQueryChangeListener(binding.searchField.root, viewModel::setQuery)
    }

    private fun renderState(state: ConfigUiState) {
        val settingsRow = binding.settingChipRow.root
        suppressChipCallbacks.run {
            FilterChipRow.setChipChecked(settingsRow, R.id.setting_chipGroup, R.id.chipScopeMode, state.scopeMode)
            FilterChipRow.setChipChecked(settingsRow, R.id.setting_chipGroup, R.id.chipHandleSystem, state.handleSystem)
            FilterChipRow.setChipChecked(settingsRow, R.id.setting_chipGroup, R.id.chipShowSystem, state.showSystemUi)
        }

        state.packageVisibility?.let { permissionBannerRenderer.render(it) }

        legacyController.setVisibility(state.isLegacyMode)
        managedController.setVisibility(!state.isLegacyMode)

        val listCount = if (state.isLegacyMode) {
            legacyController.render(state)
        } else {
            managedController.render(state)
        }

        binding.loadingPanel.root.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        if (state.isLoading) {
            LoadingState.bind(binding.loadingPanel.root)
        }

        emptyStateRenderer.render(state, listCount)

        requireContext().showErrorToast(state.errorMessage)

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
