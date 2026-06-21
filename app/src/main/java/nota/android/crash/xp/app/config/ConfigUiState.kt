package nota.android.crash.xp.app.config

import nota.android.crash.xp.app.PackageVisibilityHelper
import nota.android.crash.xp.app.common.HasErrorMessage
import nota.android.crash.xp.app.common.LoadableState

enum class ManagedFilter {
    ALL, ENABLED, PENDING,
}

data class ConfigUiState(
    override val isLoading: Boolean = false,
    val allApps: List<AppItem> = emptyList(),
    val visibleApps: List<AppItem> = emptyList(),
    val managedApps: List<ManagedApp> = emptyList(),
    val visibleManagedApps: List<ManagedApp> = emptyList(),
    val query: String = "",
    val hookFilter: HookFilter = HookFilter.ALL,
    val managedFilter: ManagedFilter = ManagedFilter.ALL,
    val isLegacyMode: Boolean = true,
    val sortMode: SortMode = SortMode.UPDATE_TIME_DESC,
    val scopeMode: Boolean = false,
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
