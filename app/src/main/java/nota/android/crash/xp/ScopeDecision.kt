package nota.android.crash.xp

data class ScopeDecision(
    /** Install Application.onCreate capture (ADR-023). False only for ignored / system filter. */
    val shouldInstall: Boolean,
    /** Looper resurrection + swallow exceptions (ADR-001). Switch / CATCH_ALL.enabled. */
    val shouldIntercept: Boolean,
    val showNotify: Boolean,
    val crashLogEnabled: Boolean = true,
)
