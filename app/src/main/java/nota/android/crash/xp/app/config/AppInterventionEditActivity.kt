package nota.android.crash.xp.app.config

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View

import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.SystemBars
import nota.android.crash.xp.app.common.ui.CallbackSuppressor
import nota.android.crash.xp.app.common.ui.showErrorToast
import nota.android.crash.xp.app.common.ui.ToolbarHeaderInsets
import nota.android.crash.xp.app.di.ServiceLocator
import nota.android.crash.xp.app.di.appInterventionEditViewModelFactory
import nota.android.crash.xp.app.databinding.ActivityAppInterventionEditBinding

class AppInterventionEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppInterventionEditBinding
    private lateinit var packageName: String
    private val suppressNotifyCallbacks = CallbackSuppressor()

    private val viewModel: AppInterventionEditViewModel by viewModels {
        ServiceLocator.appInterventionEditViewModelFactory(this, packageName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            ?: run {
                finish()
                return
            }

        binding = ActivityAppInterventionEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBars.setup(this)
        ToolbarHeaderInsets.apply(binding.toolbarHeader)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadAppHeader()
        setupRuleControls()
        setupActions()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderProfile(state)
                    if (state.saved) {
                        Toast.makeText(this@AppInterventionEditActivity, R.string.rule_saved, Toast.LENGTH_SHORT).show()
                    }
                    showErrorToast(state.errorMessage) { viewModel.clearError() }
                }
            }
        }
    }

    private fun loadAppHeader() {
        try {
            val packageManager = packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            binding.ivIcon.setImageDrawable(appInfo.loadIcon(packageManager))
            binding.tvLabel.text = appInfo.loadLabel(packageManager)
            binding.toolbar.title = appInfo.loadLabel(packageManager)
        } catch (_: PackageManager.NameNotFoundException) {
            binding.tvLabel.text = packageName
            binding.toolbar.title = packageName
        }
        binding.tvPackageName.text = packageName
    }

    private fun renderProfile(state: AppInterventionEditUiState) {
        val profile = state.profile
        val catchAllRule = state.catchAllRule

        val enabledCount = profile.enabledRuleCount
        binding.tvStatusSummary.text = if (profile.hasEnabledRule) {
            resources.getQuantityString(R.plurals.intervention_status_enabled, enabledCount, enabledCount)
        } else {
            getString(R.string.intervention_status_pending)
        }

        val hasCatchAll = catchAllRule != null
        binding.ruleCard.visibility = if (hasCatchAll) View.VISIBLE else View.GONE
        binding.rulesEmpty.visibility = if (hasCatchAll) View.GONE else View.VISIBLE

        catchAllRule?.let { rule ->
            binding.swRuleEnabled.setOnCheckedChangeListener(null)
            binding.swRuleEnabled.isChecked = rule.enabled
            binding.swRuleEnabled.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleRuleEnabled(isChecked)
            }
            bindNotifyTriState(rule.showNotify)
        }
    }

    private fun bindNotifyTriState(showNotify: Boolean?) {
        val chipId = when (showNotify) {
            true -> R.id.chipNotifyOn
            false -> R.id.chipNotifyOff
            null -> R.id.chipNotifyInherit
        }
        suppressNotifyCallbacks.run {
            binding.notifyChipGroup.check(chipId)
        }
    }

    private fun setupRuleControls() {
        binding.notifyChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressNotifyCallbacks.suppressed) return@setOnCheckedStateChangeListener
            val notifyValue = when (checkedIds.firstOrNull()) {
                R.id.chipNotifyOn -> true
                R.id.chipNotifyOff -> false
                else -> null
            }
            viewModel.updateShowNotify(notifyValue)
        }

        binding.btnAddRule.setOnClickListener {
            viewModel.addCatchAllRule()
        }

        binding.btnDeleteRule.setOnClickListener {
            viewModel.deleteCatchAllRule()
        }
    }

    private fun setupActions() {
        binding.btnRemoveApp.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.remove_managed_app_confirm_title)
                .setMessage(R.string.remove_managed_app_confirm_message)
                .setPositiveButton(R.string.remove_managed_app) { _, _ ->
                    viewModel.removeManagedApp()
                    Toast.makeText(this, R.string.remove_managed_app_success, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "packageName"
    }
}
