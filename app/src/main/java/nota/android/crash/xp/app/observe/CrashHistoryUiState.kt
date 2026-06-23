package nota.android.crash.xp.app.observe

import nota.android.crash.xp.app.common.HasErrorMessage
import nota.android.crash.xp.app.common.LoadableState
import nota.android.crash.xp.app.data.CrashFilter
import nota.android.crash.xp.app.data.CrashSortMode

data class CrashHistoryUiState(
    override val isLoading: Boolean = false,
    val eventCount: Int = 0,
    val activeFilter: CrashFilter? = null,
    val activePackageFilter: String? = null,
    val sortMode: CrashSortMode = CrashSortMode.TIME_NEWEST,
    val historyCleared: Int = 0,
    override val errorMessage: String? = null,
) : LoadableState, HasErrorMessage {
    override fun copyWithLoading(isLoading: Boolean): CrashHistoryUiState = copy(isLoading = isLoading)
    override fun copyWithError(errorMessage: String?): CrashHistoryUiState =
        copy(isLoading = false, errorMessage = errorMessage)
    override fun withNoErrorMessage(): CrashHistoryUiState = copy(errorMessage = null)
}
