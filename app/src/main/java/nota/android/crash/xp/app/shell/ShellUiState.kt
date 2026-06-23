package nota.android.crash.xp.app.shell

import nota.android.crash.root.RootAvailability

enum class ShellTab {
    CONFIG,
    OBSERVE,
}

data class ShellUiState(
    val selectedTab: ShellTab = ShellTab.CONFIG,
    val xposedActive: Boolean = false,
    val rootAvailability: RootAvailability? = null,
    val activeBackendCount: Int = 0,
    val totalBackendCount: Int = 0,
)
