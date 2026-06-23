package nota.android.crash.xp.app.observe

import nota.android.crash.xp.app.common.HasErrorMessage
import nota.android.crash.xp.app.common.LoadableState
import nota.android.crash.xp.app.data.PerAppStats

data class PerAppCrashUiState(
    override val isLoading: Boolean = true,
    val summary: PerAppStats? = null,
    override val errorMessage: String? = null,
) : LoadableState, HasErrorMessage {
    override fun copyWithLoading(isLoading: Boolean): PerAppCrashUiState = copy(isLoading = isLoading)
    override fun copyWithError(errorMessage: String?): PerAppCrashUiState =
        copy(isLoading = false, errorMessage = errorMessage)
    override fun withNoErrorMessage(): PerAppCrashUiState = copy(errorMessage = null)
}
