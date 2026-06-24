package nota.android.crash.xp.app.config

import nota.android.crash.xp.app.PackageVisibilityHelper
import nota.android.crash.xp.app.common.HasErrorMessage
import nota.android.crash.xp.app.common.LoadableState

enum class InterceptFilter { ALL, ENABLED, DISABLED }

data class ConfigUiState(
    override val isLoading: Boolean = false,
    val apps: List<AppItem> = emptyList(),
    val visibleApps: List<AppItem> = emptyList(),
    val query: String = "",
    val interceptFilter: InterceptFilter = InterceptFilter.ALL,
    val sortMode: SortMode = SortMode.UPDATE_TIME_DESC,
    val handleSystem: Boolean = false,
    val showSystemUi: Boolean = false,
    val packageVisibility: PackageVisibilityHelper.Status? = null,
    val emptyMessage: String? = null,
    override val errorMessage: String? = null,
) : LoadableState, HasErrorMessage {
    override fun copyWithLoading(isLoading: Boolean): ConfigUiState = copy(isLoading = isLoading)
    override fun copyWithError(errorMessage: String?): ConfigUiState =
        copy(isLoading = false, errorMessage = errorMessage)
    override fun withNoErrorMessage(): ConfigUiState = copy(errorMessage = null)
}
