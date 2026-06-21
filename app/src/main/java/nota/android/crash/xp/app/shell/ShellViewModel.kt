package nota.android.crash.xp.app.shell

import androidx.lifecycle.SavedStateHandle
import nota.android.crash.xp.app.common.BaseFlowViewModel

class ShellViewModel(
    private val savedStateHandle: SavedStateHandle,
) : BaseFlowViewModel<ShellUiState>(restoredUiState(savedStateHandle)) {

    fun setSelectedTab(tab: ShellTab) {
        savedStateHandle[KEY_SELECTED_TAB] = tab.name
        emitState { copy(selectedTab = tab) }
    }

    fun refreshXposedStatus(isActive: Boolean) {
        emitState { copy(xposedActive = isActive) }
    }

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"

        private fun restoredUiState(savedStateHandle: SavedStateHandle): ShellUiState {
            val tabName = savedStateHandle.get<String>(KEY_SELECTED_TAB)
            val tab = tabName?.let { runCatching { ShellTab.valueOf(it) }.getOrNull() } ?: ShellTab.CONFIG
            return ShellUiState(selectedTab = tab)
        }
    }
}
