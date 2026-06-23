package nota.android.crash.xp.app.observe

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nota.android.crash.xp.app.common.BaseFlowViewModel

class LogcatViewModel(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseFlowViewModel<LogcatUiState>(LogcatUiState()) {

    /**
     * Parse raw logcat text on [ioDispatcher] and update the UI state.
     *
     * @param rawText full contents of the imported logcat file
     * @param crashOnly when true, only crash-related entries are kept
     */
    fun loadFromText(rawText: String?, crashOnly: Boolean = false) {
        loadWithState(viewModelScope) {
            val allEntries = withContext(ioDispatcher) {
                LogcatParser.parse(rawText)
            }
            val entries = if (crashOnly) {
                LogcatParser.filterCrashRelated(allEntries)
            } else {
                allEntries
            }
            emitState {
                copy(
                    isLoading = false,
                    entries = entries,
                    isFiltered = crashOnly,
                    totalRawCount = allEntries.size,
                )
            }
        }
    }

    /**
     * Re-parse the current entries with crash-only filter.
     */
    fun setCrashFilter(crashOnly: Boolean) {
        val current = _uiState.value
        if (current.entries.isEmpty()) return
        // Re-parse from scratch is not possible here; toggle isFiltered on existing entries
        // For a new file load, crashOnly is passed to loadFromText
        emitState { copy(isFiltered = crashOnly) }
    }

    /**
     * Toggle a specific log level visibility.
     */
    fun toggleLevel(level: LogcatLevel) {
        emitState {
            val newLevels = if (level in activeLevels) {
                activeLevels - level
            } else {
                activeLevels + level
            }
            copy(activeLevels = newLevels)
        }
    }

    /**
     * Reset level filters to the default set.
     */
    fun resetLevels() {
        emitState { copy(activeLevels = LogcatUiState.DEFAULT_LEVELS) }
    }
}
