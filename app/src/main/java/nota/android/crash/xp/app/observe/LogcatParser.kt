package nota.android.crash.xp.app.observe

/**
 * Parses raw logcat text into a list of [LogcatEntry] objects.
 *
 * Supports standard `threadtime` format:
 * ```
 * MM-DD HH:MM:SS.mmm  PID  TID LEVEL TAG: message
 * ```
 *
 * Continuation lines (those not matching the header pattern) are appended to
 * the previous entry's message.
 */
object LogcatParser {

    // MM-DD HH:MM:SS.mmm  PID  TID LEVEL TAG: message
    // Groups: (1=date) (2=time) (3=pid) (4=tid) (5=level) (6=tag) (7=message)
    private val LOGCAT_LINE = Regex(
        """^(\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEFS])\s+(.+?):\s(.*)$"""
    )

    /** Maximum raw text size to process (5 MB). */
    private const val MAX_TEXT_BYTES = 5 * 1024 * 1024

    /**
     * Parse [rawText] into entries. Returns an empty list when the text is null
     * or blank. Lines exceeding [MAX_TEXT_BYTES] are truncated with a warning.
     */
    fun parse(rawText: String?): List<LogcatEntry> {
        if (rawText.isNullOrBlank()) return emptyList()

        val text = if (rawText.length > MAX_TEXT_BYTES) {
            rawText.take(MAX_TEXT_BYTES)
        } else {
            rawText
        }

        val entries = mutableListOf<LogcatEntry>()
        var current: LogcatEntry? = null

        for (line in text.lineSequence()) {
            val match = LOGCAT_LINE.matchAt(line, 0)
            if (match != null) {
                current?.let { entries.add(it) }
                current = LogcatEntry(
                    timestamp = match.groupValues[1] + " " + match.groupValues[2],
                    pid = match.groupValues[3],
                    tid = match.groupValues[4],
                    level = LogcatLevel.fromLabel(match.groupValues[5]),
                    tag = match.groupValues[6],
                    message = match.groupValues[7],
                )
            } else if (current != null && line.isNotEmpty()) {
                // Continuation line — append to current entry
                current = current.copy(message = current.message + "\n" + line)
            }
        }
        current?.let { entries.add(it) }

        return entries
    }

    /**
     * Returns only crash-related entries from a pre-parsed list.
     *
     * Matches the heuristics defined in `docs/architecture/adb-logcat-analysis.md`:
     * - FATAL / AndroidRuntime entries
     * - XposedBridge entries (intercepted exceptions)
     * - Hook markers (`catch package`, `selfCheck`)
     * - Native crash hints (`Fatal signal`, `backtrace:`)
     * - Process death (`Process ... has died`)
     * - ANR hints (`ANR in`, `am_anr`, ActivityManager ANR / not responding) — [ADR-025]
     */
    fun filterCrashRelated(entries: List<LogcatEntry>): List<LogcatEntry> {
        return entries.filter { isCrashRelated(it) }
    }

    /**
     * Whether [entry] is an ANR diagnostic hint (`ANR_HINT` in adb-logcat-analysis.md).
     *
     * Generic `Process ... has died` alone is **not** an ANR hint (still [isCrashRelated]).
     */
    fun isAnrHint(entry: LogcatEntry): Boolean {
        val msg = entry.message
        val msgUpper = msg.uppercase()
        if ("ANR IN" in msgUpper) return true
        if ("AM_ANR" in msgUpper) return true
        if (entry.tag == "ActivityManager") {
            if (msg.contains("ANR", ignoreCase = true)) return true
            if (msg.contains("not responding", ignoreCase = true)) return true
            if (msg.contains("Input dispatching timed out", ignoreCase = true)) return true
        }
        return false
    }

    fun isCrashRelated(entry: LogcatEntry): Boolean {
        return isAnrHint(entry) || isOtherCrashRelated(entry)
    }

    private fun isOtherCrashRelated(entry: LogcatEntry): Boolean {
        val msg = entry.message
        val tag = entry.tag
        return entry.level == LogcatLevel.FATAL
            || tag == "AndroidRuntime"
            || tag == "XposedBridge"
            || tag == "XposedEntry"
            || tag == "CrashCenter"
            || msg.contains("FATAL EXCEPTION")
            || msg.contains("Fatal signal")
            || msg.contains("backtrace:")
            || msg.contains("catch package")
            || msg.contains("selfCheck")
            || Regex("""Process\s+\S+\s+has died""").containsMatchIn(msg)
    }
}
