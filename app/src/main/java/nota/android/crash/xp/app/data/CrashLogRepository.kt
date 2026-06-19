package nota.android.crash.xp.app.data

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

interface CrashLogRepository {
    fun getAll(filter: CrashFilter, limit: Int, offset: Int): List<CrashEvent>
    fun getById(id: String): CrashEvent?
    fun getCount(filter: CrashFilter): Int
}

class FileCrashLogRepository(context: Context) : CrashLogRepository {

    private val eventsFile = File(context.applicationContext.filesDir, "$LOG_DIR/$EVENTS_FILE")

    override fun getAll(filter: CrashFilter, limit: Int, offset: Int): List<CrashEvent> {
        val filtered = readAllEvents().filter { matchesFilter(it, filter) }
        return filtered.drop(offset).take(limit)
    }

    override fun getById(id: String): CrashEvent? =
        readAllEvents().firstOrNull { it.id == id }

    override fun getCount(filter: CrashFilter): Int =
        readAllEvents().count { matchesFilter(it, filter) }

    private fun readAllEvents(): List<CrashEvent> {
        if (!eventsFile.isFile) return emptyList()
        val events = mutableListOf<CrashEvent>()
        BufferedReader(FileReader(eventsFile)).use { reader ->
            reader.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach
                CrashEvent.fromJson(trimmed)?.let(events::add)
            }
        }
        return events.sortedByDescending { it.timestampMs }
    }

    private fun matchesFilter(event: CrashEvent, filter: CrashFilter): Boolean {
        filter.packageName?.let { pkg ->
            if (event.packageName != pkg) return false
        }
        filter.source?.let { source ->
            if (event.source != source) return false
        }
        filter.sinceMs?.let { since ->
            if (event.timestampMs < since) return false
        }
        filter.untilMs?.let { until ->
            if (event.timestampMs > until) return false
        }
        filter.query?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
            val q = query.lowercase()
            val haystack = listOfNotNull(
                event.appLabel,
                event.packageName,
                event.exceptionClass,
                event.message,
            ).joinToString(" ").lowercase()
            if (!haystack.contains(q)) return false
        }
        return true
    }

    companion object {
        const val LOG_DIR = "crash_logs"
        const val EVENTS_FILE = "events.jsonl"

        fun eventsFile(context: Context): File =
            File(context.applicationContext.filesDir, "$LOG_DIR/$EVENTS_FILE")
    }
}
