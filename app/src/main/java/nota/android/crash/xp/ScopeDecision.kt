package nota.android.crash.xp

data class ScopeDecision(
    val shouldHook: Boolean,
    val showNotify: Boolean,
    val crashLogEnabled: Boolean = true,
)
