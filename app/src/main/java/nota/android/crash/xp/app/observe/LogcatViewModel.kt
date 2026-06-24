package nota.android.crash.xp.app.observe

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nota.android.crash.root.RootLogcatReader
import nota.android.crash.xp.app.common.BaseFlowViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class LogcatViewModel(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseFlowViewModel<LogcatUiState>(LogcatUiState()) {

    private var loadJob: Job? = null
    private var loadGeneration = 0
    private var cachedFileText: String? = null

    val pagingData: Flow<PagingData<LogcatEntry>> = _uiState
        .map { state ->
            if (state.isLoading) emptyList() else state.displayEntries
        }
        .distinctUntilChanged()
        .flatMapLatest { entries ->
            Pager(
                config = PagingConfig(
                    pageSize = 50,
                    enablePlaceholders = false,
                    initialLoadSize = 50,
                ),
                pagingSourceFactory = { LogcatEntryPagingSource(entries) },
            ).flow
        }
        .cachedIn(viewModelScope)

    /** Cancel any in-flight root/file load (e.g. before SAF import). */
    fun cancelActiveLoad() {
        loadJob?.cancel()
        loadJob = null
    }

    /**
     * Parse raw logcat text on [ioDispatcher] and update the UI state.
     *
     * @param rawText full contents of the imported logcat file
     * @param crashOnly when true, only crash-related entries are kept
     */
    fun loadFromText(rawText: String?, crashOnly: Boolean? = null) {
        startLoad { generation ->
            val allEntries = withContext(ioDispatcher) {
                LogcatParser.parse(rawText)
            }
            if (isStale(generation)) return@startLoad
            val applyCrashFilter = crashOnly ?: _uiState.value.isFiltered
            val entries = withContext(ioDispatcher) {
                displayEntries(allEntries, applyCrashFilter)
            }
            if (isStale(generation)) return@startLoad
            if (!rawText.isNullOrBlank()) {
                cachedFileText = rawText
            }
            emitState {
                copy(
                    isLoading = false,
                    entries = entries,
                    allEntries = allEntries,
                    isFiltered = applyCrashFilter,
                    totalRawCount = allEntries.size,
                    sourceMode = SourceMode.FILE,
                    rootLoadFailed = false,
                )
            }
        }
    }

    /**
     * Apply or remove the crash-only filter using the stored [LogcatUiState.allEntries].
     */
    fun setCrashFilter(crashOnly: Boolean) {
        val current = _uiState.value
        if (current.isFiltered == crashOnly) return
        if (current.allEntries.isEmpty()) {
            emitState { copy(isFiltered = crashOnly) }
            return
        }
        viewModelScope.launch {
            val entries = withContext(ioDispatcher) {
                displayEntries(current.allEntries, crashOnly)
            }
            emitState {
                copy(
                    entries = entries,
                    isFiltered = crashOnly,
                )
            }
        }
    }

    /**
     * Toggle a specific log level visibility.
     */
    fun toggleLevel(level: LogcatLevel) {
        setLevelVisible(level, level !in _uiState.value.activeLevels)
    }

    /** Show or hide [level]; checked Filter chips map to visible levels. */
    fun setLevelVisible(level: LogcatLevel, visible: Boolean) {
        emitState {
            val newLevels = if (visible) activeLevels + level else activeLevels - level
            if (newLevels == activeLevels) this else copy(activeLevels = newLevels)
        }
    }

    /**
     * Reset level filters to the default set.
     */
    fun resetLevels() {
        emitState { copy(activeLevels = LogcatUiState.DEFAULT_LEVELS) }
    }

    fun setSearchQuery(query: String) {
        if (_uiState.value.searchQuery == query) return
        emitState { copy(searchQuery = query) }
    }

    /**
     * Read a logcat buffer via root and update the UI state.
     *
     * @param context Android context for the reader
     * @param buffer  which logcat buffer to read
     */
    fun loadFromRoot(context: Context, buffer: LogcatBuffer = LogcatBuffer.MAIN, crashOnly: Boolean? = null) {
        startLoad { generation ->
            val allEntries = withContext(ioDispatcher) {
                val rawText = RootLogcatReader.readBuffer(buffer) ?: return@withContext null
                LogcatParser.parse(rawText)
            }
            if (isStale(generation)) return@startLoad
            if (allEntries != null) {
                val applyCrashFilter = crashOnly ?: _uiState.value.isFiltered
                val entries = withContext(ioDispatcher) {
                    displayEntries(allEntries, applyCrashFilter)
                }
                if (isStale(generation)) return@startLoad
                emitState {
                    copy(
                        isLoading = false,
                        entries = entries,
                        allEntries = allEntries,
                        sourceMode = SourceMode.ROOT,
                        activeBuffer = buffer,
                        totalRawCount = allEntries.size,
                        rootLoadFailed = false,
                        isFiltered = applyCrashFilter,
                    )
                }
            } else {
                emitState {
                    copy(
                        isLoading = false,
                        sourceMode = SourceMode.ROOT,
                        activeBuffer = buffer,
                        rootLoadFailed = true,
                    )
                }
            }
        }
    }

    fun clearRootLoadFailed() {
        emitState { copy(rootLoadFailed = false) }
    }

    /**
     * Switch to a different logcat buffer and reload from root.
     */
    fun switchBuffer(context: Context, buffer: LogcatBuffer) {
        if (_uiState.value.sourceMode != SourceMode.ROOT) return
        if (_uiState.value.activeBuffer == buffer && _uiState.value.allEntries.isNotEmpty()) return
        loadFromRoot(context, buffer, crashOnly = _uiState.value.isFiltered)
    }

    /** Reload using the current source (root buffer or last imported file). */
    fun reloadCurrentSource(context: Context, crashOnly: Boolean? = null) {
        when (_uiState.value.sourceMode) {
            SourceMode.ROOT -> loadFromRoot(
                context,
                _uiState.value.activeBuffer,
                crashOnly = crashOnly ?: _uiState.value.isFiltered,
            )
            SourceMode.FILE -> {
                val text = cachedFileText
                if (text != null) {
                    loadFromText(text, crashOnly = crashOnly ?: _uiState.value.isFiltered)
                }
            }
            SourceMode.NONE -> Unit
        }
    }

    private fun startLoad(block: suspend (generation: Int) -> Unit) {
        loadJob?.cancel()
        val generation = ++loadGeneration
        emitState { copyWithLoading(true) }
        loadJob = viewModelScope.launch {
            try {
                block(generation)
            } catch (_: CancellationException) {
                if (generation == loadGeneration) {
                    emitState { copy(isLoading = false) }
                }
            } catch (e: Exception) {
                if (generation == loadGeneration) {
                    emitState { copyWithError(e.message) }
                }
            }
        }
    }

    private fun isStale(generation: Int): Boolean = generation != loadGeneration

    private fun displayEntries(allEntries: List<LogcatEntry>, crashOnly: Boolean): List<LogcatEntry> =
        if (crashOnly) LogcatParser.filterCrashRelated(allEntries) else allEntries
}
