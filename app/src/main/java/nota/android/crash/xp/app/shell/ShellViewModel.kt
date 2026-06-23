package nota.android.crash.xp.app.shell

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.CrashLogBackendRegistry
import nota.android.crash.root.RootAccessClient
import nota.android.crash.xp.app.common.BaseFlowViewModel

class ShellViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseFlowViewModel<ShellUiState>(restoredUiState(savedStateHandle)) {

    fun setSelectedTab(tab: ShellTab) {
        savedStateHandle[KEY_SELECTED_TAB] = tab.name
        emitState { copy(selectedTab = tab) }
    }

    fun refreshXposedStatus(isActive: Boolean) {
        emitState { copy(xposedActive = isActive) }
    }

    /**
     * Probe root availability and active backend count on an IO thread.
     * Called from [MainShellActivity] on every resume so status stays fresh.
     */
    fun refreshRootStatus(context: Context, rootAccessClient: RootAccessClient) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val root = rootAccessClient.probe()
                val backends = CrashLogBackendRegistry.enabledHookPhase2Backends(context)
                val activeCount = backends.count { backend ->
                    backend.probe(context) == BackendAvailability.READY
                }
                emitState {
                    copy(
                        rootAvailability = root,
                        activeBackendCount = activeCount,
                        totalBackendCount = backends.size,
                    )
                }
            } catch (e: Throwable) {
                Log.w("ShellViewModel", "refreshRootStatus failed", e)
            }
        }
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
