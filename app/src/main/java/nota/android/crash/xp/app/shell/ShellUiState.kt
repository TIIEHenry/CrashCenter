package nota.android.crash.xp.app.shell

enum class ShellTab {
    CONFIG,
    OBSERVE,
}

data class ShellUiState(
    val selectedTab: ShellTab = ShellTab.CONFIG,
    val xposedActive: Boolean = false,
)
