package nota.android.crash.root

/** Metadata for root-accessible files (fingerprint / cache invalidation). */
data class RootFileStat(
    val mtimeMs: Long,
    val length: Long,
)
