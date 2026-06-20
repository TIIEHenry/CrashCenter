package nota.android.crash.xp.app.shell

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ShellViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoredUiState())
    val uiState: StateFlow<ShellUiState> = _uiState

    fun setSelectedTab(tab: ShellTab) {
        savedStateHandle[KEY_SELECTED_TAB] = tab.name
        emit { copy(selectedTab = tab) }
    }

    fun refreshXposedStatus(isActive: Boolean) {
        emit { copy(xposedActive = isActive) }
    }

    private fun restoredUiState(): ShellUiState {
        val tabName = savedStateHandle.get<String>(KEY_SELECTED_TAB)
        val tab = tabName?.let { runCatching { ShellTab.valueOf(it) }.getOrNull() } ?: ShellTab.CONFIG
        return ShellUiState(selectedTab = tab)
    }

    private inline fun emit(block: ShellUiState.() -> ShellUiState) {
        _uiState.value = _uiState.value.block()
    }

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
    }
}
