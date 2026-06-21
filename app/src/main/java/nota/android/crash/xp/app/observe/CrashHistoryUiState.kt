package nota.android.crash.xp.app.observe

data class CrashHistoryUiState(
    val isLoading: Boolean = false,
    val eventCount: Int = 0,
    val errorMessage: String? = null,
)
