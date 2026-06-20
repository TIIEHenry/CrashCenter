package nota.android.crash.xp.app.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInterventionEditUiState(
    val profile: AppInterventionProfile = AppInterventionProfile.EMPTY,
    val catchAllRule: InterventionRule? = null,
    val saved: Boolean = false,
)

class AppInterventionEditViewModel(
    private val packageName: String,
    private val repository: AppRepositoryInterface,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppInterventionEditUiState())
    val uiState: StateFlow<AppInterventionEditUiState> = _uiState

    init {
        viewModelScope.launch {
            val profile = withContext(ioDispatcher) {
                repository.getProfile(packageName)
            }
            val catchAll = profile.rules.firstOrNull { it.type == InterventionRuleType.CATCH_ALL }
            _uiState.value = AppInterventionEditUiState(
                profile = profile,
                catchAllRule = catchAll,
            )
        }
    }

    fun toggleRuleEnabled(enabled: Boolean) {
        val current = _uiState.value.catchAllRule ?: return
        updateCatchAllRule(current.copy(enabled = enabled))
    }

    fun updateShowNotify(showNotify: Boolean?) {
        val current = _uiState.value.catchAllRule ?: return
        updateCatchAllRule(current.copy(showNotify = showNotify))
    }

    fun updateCrashLogEnabled(crashLogEnabled: Boolean?) {
        val current = _uiState.value.catchAllRule ?: return
        updateCatchAllRule(current.copy(crashLogEnabled = crashLogEnabled))
    }

    fun addCatchAllRule() {
        val newRule = InterventionRule.defaultCatchAll(enabled = true)
        _uiState.value = _uiState.value.copy(
            catchAllRule = newRule,
            profile = AppInterventionProfile(rules = listOf(newRule)),
        )
        saveProfile()
    }

    fun deleteCatchAllRule() {
        _uiState.value = _uiState.value.copy(
            catchAllRule = null,
            profile = AppInterventionProfile.EMPTY,
        )
        saveProfile()
    }

    fun removeManagedApp() {
        viewModelScope.launch(ioDispatcher) {
            repository.removeManagedPackage(packageName)
        }
    }

    private fun updateCatchAllRule(updatedRule: InterventionRule) {
        val otherRules = _uiState.value.profile.rules.filter { it.type != InterventionRuleType.CATCH_ALL }
        val newProfile = _uiState.value.profile.copy(rules = otherRules + updatedRule)
        _uiState.value = _uiState.value.copy(
            catchAllRule = updatedRule,
            profile = newProfile,
        )
        saveProfile()
    }

    private fun saveProfile() {
        viewModelScope.launch(ioDispatcher) {
            repository.saveProfile(packageName, _uiState.value.profile)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(saved = true)
            }
        }
    }
}
