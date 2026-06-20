package nota.android.crash.xp.app.config

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import nota.android.crash.xp.app.R
import nota.android.crash.xp.app.SystemBars
import nota.android.crash.xp.app.common.ui.ToolbarHeaderInsets
import nota.android.crash.xp.app.databinding.ActivityAppInterventionEditBinding

class AppInterventionEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppInterventionEditBinding
    private lateinit var repository: AppRepository
    private lateinit var packageName: String

    private var profile: AppInterventionProfile = AppInterventionProfile.EMPTY
    private var catchAllRule: InterventionRule? = null
    private var suppressNotifyCallbacks = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            ?: run {
                finish()
                return
            }

        repository = AppRepository(this)
        binding = ActivityAppInterventionEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBars.setup(this)
        ToolbarHeaderInsets.apply(binding.toolbarHeader)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadAppHeader()
        loadProfile()
        setupRuleControls()
        setupActions()
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

    private fun loadProfile() {
        profile = repository.getProfile(packageName)
        catchAllRule = profile.rules.firstOrNull { it.type == InterventionRuleType.CATCH_ALL }
        renderProfile()
    }

    private fun renderProfile() {
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
                updateCatchAllRule(rule.copy(enabled = isChecked))
            }
            bindNotifyTriState(rule.showNotify)
        }
    }

    private fun bindNotifyTriState(showNotify: Boolean?) {
        suppressNotifyCallbacks = true
        val chipId = when (showNotify) {
            true -> R.id.chipNotifyOn
            false -> R.id.chipNotifyOff
            null -> R.id.chipNotifyInherit
        }
        binding.notifyChipGroup.check(chipId)
        suppressNotifyCallbacks = false
    }

    private fun setupRuleControls() {
        binding.notifyChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressNotifyCallbacks) return@setOnCheckedStateChangeListener
            val rule = catchAllRule ?: return@setOnCheckedStateChangeListener
            val notifyValue = when (checkedIds.firstOrNull()) {
                R.id.chipNotifyOn -> true
                R.id.chipNotifyOff -> false
                else -> null
            }
            updateCatchAllRule(rule.copy(showNotify = notifyValue))
        }

        binding.btnAddRule.setOnClickListener {
            val newRule = InterventionRule.defaultCatchAll(enabled = true)
            catchAllRule = newRule
            profile = profile.copy(rules = listOf(newRule))
            persistProfile()
            renderProfile()
        }

        binding.btnDeleteRule.setOnClickListener {
            catchAllRule = null
            profile = AppInterventionProfile.EMPTY
            persistProfile()
            renderProfile()
        }
    }

    private fun setupActions() {
        binding.btnRemoveApp.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.remove_managed_app_confirm_title)
                .setMessage(R.string.remove_managed_app_confirm_message)
                .setPositiveButton(R.string.remove_managed_app) { _, _ ->
                    repository.removeManagedPackage(packageName)
                    Toast.makeText(this, R.string.remove_managed_app_success, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun updateCatchAllRule(updatedRule: InterventionRule) {
        catchAllRule = updatedRule
        val otherRules = profile.rules.filter { it.type != InterventionRuleType.CATCH_ALL }
        profile = profile.copy(rules = otherRules + updatedRule)
        persistProfile()
        renderProfile()
    }

    private fun persistProfile() {
        repository.saveProfile(packageName, profile)
        Toast.makeText(this, R.string.rule_saved, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "packageName"
    }
}
