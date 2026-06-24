package nota.android.crash.xp.app.observe

import java.util.Locale
import nota.android.crash.xp.app.common.HasErrorMessage
import nota.android.crash.xp.app.common.LoadableState

enum class SourceMode { NONE, ROOT, FILE }

enum class LogcatBuffer(val id: String, val label: String, val minApi: Int = 0) {
    MAIN("main", "Main"),
    SYSTEM("system", "System"),
    CRASH("crash", "Crash", 30),
    EVENTS("events", "Events"),
    RADIO("radio", "Radio"),
}

data class LogcatUiState(
    override val isLoading: Boolean = false,
    val entries: List<LogcatEntry> = emptyList(),
    val allEntries: List<LogcatEntry> = emptyList(),
    val activeLevels: Set<LogcatLevel> = DEFAULT_LEVELS,
    val isFiltered: Boolean = false,
    val totalRawCount: Int = 0,
    val sourceMode: SourceMode = SourceMode.NONE,
    val activeBuffer: LogcatBuffer = LogcatBuffer.MAIN,
    val rootLoadFailed: Boolean = false,
    val searchQuery: String = "",
    override val errorMessage: String? = null,
) : LoadableState, HasErrorMessage {

    val displayEntries: List<LogcatEntry>
        get() = entries
            .asSequence()
            .filter { it.level in activeLevels }
            .filter { matchesSearchQuery(it, searchQuery) }
            .toList()

    override fun copyWithLoading(isLoading: Boolean): LogcatUiState = copy(isLoading = isLoading)
    override fun copyWithError(errorMessage: String?): LogcatUiState =
        copy(isLoading = false, errorMessage = errorMessage)
    override fun withNoErrorMessage(): LogcatUiState = copy(errorMessage = null)

    companion object {
        fun matchesSearchQuery(entry: LogcatEntry, query: String): Boolean {
            val needle = query.trim().lowercase(Locale.ROOT)
            if (needle.isEmpty()) return true
            val haystack = "${entry.tag} ${entry.message} ${entry.rawLine}".lowercase(Locale.ROOT)
            return haystack.contains(needle)
        }

        val DEFAULT_LEVELS: Set<LogcatLevel> = setOf(
            LogcatLevel.ERROR,
            LogcatLevel.WARNING,
            LogcatLevel.INFO,
            LogcatLevel.DEBUG,
            LogcatLevel.VERBOSE,
            LogcatLevel.FATAL,
        )
    }
}
