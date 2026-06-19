package nota.android.crash.xp.app.shell

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class ShellViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableLiveData(restoredUiState())
    val uiState: LiveData<ShellUiState> = _uiState

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
        val base = _uiState.value ?: ShellUiState()
        _uiState.value = base.block()
    }

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
    }
}
