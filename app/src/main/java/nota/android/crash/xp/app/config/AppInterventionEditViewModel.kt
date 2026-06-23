package nota.android.crash.xp.app.config

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nota.android.crash.xp.app.common.BaseFlowViewModel
import nota.android.crash.xp.app.common.HasErrorMessage
import nota.android.crash.xp.app.common.safeLog

data class AppInterventionEditUiState(
    val profile: AppInterventionProfile = AppInterventionProfile.EMPTY,
    val catchAllRule: InterventionRule? = null,
    val saved: Boolean = false,
    override val errorMessage: String? = null,
) : HasErrorMessage {
    override fun withNoErrorMessage(): AppInterventionEditUiState = copy(errorMessage = null)
}

class AppInterventionEditViewModel(
    private val packageName: String,
    private val repository: ManagedAppRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : BaseFlowViewModel<AppInterventionEditUiState>(AppInterventionEditUiState()) {

    init {
        launchWithErrorHandling(
            scope = viewModelScope,
            onError = { e -> emitState { copy(errorMessage = e.message) } },
        ) {
            val profile = withContext(ioDispatcher) {
                repository.getProfile(packageName)
            }
            val catchAll = profile.rules.firstOrNull { it.type == InterventionRuleType.CATCH_ALL }
            emitState {
                AppInterventionEditUiState(
                    profile = profile,
                    catchAllRule = catchAll,
                )
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
        emitState {
            copy(
                catchAllRule = newRule,
                profile = AppInterventionProfile(rules = listOf(newRule)),
            )
        }
        saveProfile()
    }

    fun deleteCatchAllRule() {
        emitState {
            copy(
                catchAllRule = null,
                profile = AppInterventionProfile.EMPTY,
            )
        }
        saveProfile()
    }

    fun removeManagedApp() {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.removeManagedPackage(packageName)
            } catch (e: Exception) {
                safeLog("AppInterventionEditViewModel", "removeManagedApp failed", e)
            }
        }
    }

    private fun updateCatchAllRule(updatedRule: InterventionRule) {
        val otherRules = _uiState.value.profile.rules.filter { it.type != InterventionRuleType.CATCH_ALL }
        val newProfile = _uiState.value.profile.copy(rules = otherRules + updatedRule)
        emitState {
            copy(
                catchAllRule = updatedRule,
                profile = newProfile,
            )
        }
        saveProfile()
    }

    private fun saveProfile() {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.saveProfile(packageName, _uiState.value.profile)
                withContext(mainDispatcher) {
                    emitState { copy(saved = true) }
                }
            } catch (e: Exception) {
                safeLog("AppInterventionEditViewModel", "saveProfile failed", e)
            }
        }
    }
}
