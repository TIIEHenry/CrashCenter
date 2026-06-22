package nota.android.crash.xp.app.observe

import nota.android.crash.xp.app.common.HasErrorMessage
import nota.android.crash.xp.app.common.LoadableState
import nota.android.crash.xp.app.data.CrashFilter

data class CrashHistoryUiState(
    override val isLoading: Boolean = false,
    val eventCount: Int = 0,
    val activeFilter: CrashFilter? = null,
    override val errorMessage: String? = null,
) : LoadableState, HasErrorMessage {
    override fun copyWithLoading(isLoading: Boolean): CrashHistoryUiState = copy(isLoading = isLoading)
    override fun copyWithError(errorMessage: String?): CrashHistoryUiState =
        copy(isLoading = false, errorMessage = errorMessage)
    override fun withNoErrorMessage(): CrashHistoryUiState = copy(errorMessage = null)
}
