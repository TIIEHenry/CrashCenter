package nota.android.crash.xp.app.observe

/**
 * Represents a single parsed logcat line.
 */
data class LogcatEntry(
    val timestamp: String,
    val pid: String,
    val tid: String,
    val level: LogcatLevel,
    val tag: String,
    val message: String,
) {
    /** Full raw line as it appeared in the logcat source. */
    val rawLine: String
        get() = "$timestamp $pid $tid ${level.label} $tag: $message"

    /** Short display string for the entry list row. */
    val displaySummary: String
        get() {
            val preview = message.lineSequence().firstOrNull().orEmpty()
            return if (preview.length > 120) preview.take(117) + "..." else preview
        }
}

enum class LogcatLevel(val label: String) {
    VERBOSE("V"),
    DEBUG("D"),
    INFO("I"),
    WARNING("W"),
    ERROR("E"),
    FATAL("F"),
    SILENT("S");

    companion object {
        fun fromLabel(label: String): LogcatLevel =
            entries.firstOrNull { it.label == label } ?: DEBUG
    }
}
