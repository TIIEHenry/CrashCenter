package nota.android.crash.xp.app.observe

import nota.android.crash.xp.app.common.HasErrorMessage
import nota.android.crash.xp.app.common.LoadableState
import nota.android.crash.xp.app.data.CrashStats

data class CrashStatsUiState(
    override val isLoading: Boolean = true,
    val stats: CrashStats? = null,
    override val errorMessage: String? = null,
) : LoadableState, HasErrorMessage {
    override fun copyWithLoading(isLoading: Boolean): CrashStatsUiState = copy(isLoading = isLoading)
    override fun copyWithError(errorMessage: String?): CrashStatsUiState =
        copy(isLoading = false, errorMessage = errorMessage)
    override fun withNoErrorMessage(): CrashStatsUiState = copy(errorMessage = null)
}
