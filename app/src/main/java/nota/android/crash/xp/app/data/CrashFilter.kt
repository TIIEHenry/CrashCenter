package nota.android.crash.xp.app.data

data class CrashFilter(
    val query: String? = null,
    val packageName: String? = null,
    val exceptionClass: String? = null,
    val sinceMs: Long? = null,
    val untilMs: Long? = null,
    val source: String? = null,
    val sortMode: CrashSortMode = CrashSortMode.TIME_NEWEST,
)
