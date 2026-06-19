package nota.android.crash.xp.app.observe

data class CrashHistoryUiState(
    val isLoading: Boolean = false,
    val events: List<nota.android.crash.xp.app.data.CrashEvent> = emptyList(),
    val eventCount: Int = 0,
)
