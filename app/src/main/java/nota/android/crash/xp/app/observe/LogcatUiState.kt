package nota.android.crash.xp.app.observe

import nota.android.crash.xp.app.common.HasErrorMessage
import nota.android.crash.xp.app.common.LoadableState

data class LogcatUiState(
    override val isLoading: Boolean = false,
    val entries: List<LogcatEntry> = emptyList(),
    val allEntries: List<LogcatEntry> = emptyList(),
    val activeLevels: Set<LogcatLevel> = DEFAULT_LEVELS,
    val isFiltered: Boolean = false,
    val totalRawCount: Int = 0,
    override val errorMessage: String? = null,
) : LoadableState, HasErrorMessage {

    val displayEntries: List<LogcatEntry>
        get() = entries.filter { it.level in activeLevels }

    override fun copyWithLoading(isLoading: Boolean): LogcatUiState = copy(isLoading = isLoading)
    override fun copyWithError(errorMessage: String?): LogcatUiState =
        copy(isLoading = false, errorMessage = errorMessage)
    override fun withNoErrorMessage(): LogcatUiState = copy(errorMessage = null)

    companion object {
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
