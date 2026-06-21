package nota.android.crash.xp.app.config

import androidx.lifecycle.ViewModel
import nota.android.crash.xp.app.common.safeLog
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
    val errorMessage: String? = null,
)

class AppInterventionEditViewModel(
    private val packageName: String,
    private val repository: ManagedAppRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppInterventionEditUiState())
    val uiState: StateFlow<AppInterventionEditUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                val profile = withContext(ioDispatcher) {
                    repository.getProfile(packageName)
                }
                val catchAll = profile.rules.firstOrNull { it.type == InterventionRuleType.CATCH_ALL }
                _uiState.value = AppInterventionEditUiState(
                    profile = profile,
                    catchAllRule = catchAll,
                )
            } catch (e: Exception) {
                safeLog("AppInterventionEditViewModel", "loadProfile failed", e)
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
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
            withContext(mainDispatcher) {
                _uiState.value = _uiState.value.copy(saved = true)
            }
        }
    }
}
