package nota.android.crash.log

import nota.android.crash.common.data.CrashEvent
import java.io.File
import java.io.RandomAccessFile

/**
 * Append-only canonical [events.jsonl] with file lock and default retention
 * (500 entries or 8 MB, whichever limit is hit first).
 */
object CanonicalJsonlWriter {

    const val DEFAULT_MAX_ENTRIES = 500
    const val DEFAULT_MAX_BYTES = 8L * 1024L * 1024L

    // Backwards-compatible aliases for tests and direct callers
    const val MAX_ENTRIES = DEFAULT_MAX_ENTRIES
    const val MAX_BYTES = DEFAULT_MAX_BYTES

    @Volatile
    var maxEntries: Int = DEFAULT_MAX_ENTRIES

    @Volatile
    var maxBytes: Long = DEFAULT_MAX_BYTES

    fun append(eventsFile: File, event: CrashEvent) {
        val parent = eventsFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return
        }
        val line = event.toJsonLine()
        RandomAccessFile(eventsFile, "rw").use { raf ->
            val lock = raf.channel.lock()
            try {
                raf.seek(raf.length())
                raf.write(line.toByteArray(Charsets.UTF_8))
                raf.write('\n'.code)

                // Retention runs inside the same lock to prevent races where
                // concurrent writers see partially-trimmed files and lose entries.
                applyRetentionLocked(raf, maxEntries, maxBytes)
            } finally {
                lock.release()
            }
        }
    }

    /**
     * Deletes all lines whose parsed [CrashEvent.id] matches [id].
     * Returns `true` if at least one line was removed, `false` if the id was not found
     * or the file does not exist.
     */
    fun deleteById(eventsFile: File, id: String): Boolean {
        if (!eventsFile.isFile) return false
        RandomAccessFile(eventsFile, "rw").use { raf ->
            val lock = raf.channel.lock()
            try {
                val lines = readLinesUtf8(raf)
                val filtered = lines.filter { line ->
                    val event = CrashEvent.fromJson(line)
                    event == null || event.id != id
                }
                if (filtered.size == lines.size) return false // id not found

                if (filtered.isEmpty()) {
                    eventsFile.delete()
                } else {
                    raf.setLength(0)
                    raf.seek(0)
                    filtered.forEach { line ->
                        raf.write(line.toByteArray(Charsets.UTF_8))
                        raf.write('\n'.code)
                    }
                }
            } finally {
                lock.release()
            }
        }
        return true
    }

    /**
     * Deletes all lines whose [CrashEvent.packageName] matches [packageName].
     * Returns the number of lines removed, or 0 if none matched / file does not exist.
     */
    fun deleteByPackage(eventsFile: File, packageName: String): Int {
        if (!eventsFile.isFile) return 0
        RandomAccessFile(eventsFile, "rw").use { raf ->
            val lock = raf.channel.lock()
            try {
                val lines = readLinesUtf8(raf)
                val filtered = lines.filter { line ->
                    val event = CrashEvent.fromJson(line)
                    event == null || event.packageName != packageName
                }
                val removed = lines.size - filtered.size
                if (removed == 0) return 0

                if (filtered.isEmpty()) {
                    eventsFile.delete()
                } else {
                    raf.setLength(0)
                    raf.seek(0)
                    filtered.forEach { line ->
                        raf.write(line.toByteArray(Charsets.UTF_8))
                        raf.write('\n'.code)
                    }
                }
                return removed
            } finally {
                lock.release()
            }
        }
    }

    /**
     * Removes [eventsFile] under a file lock so concurrent writers finish first.
     * No-op if the file does not exist.
     */
    fun clear(eventsFile: File) {
        if (!eventsFile.isFile) return
        RandomAccessFile(eventsFile, "rw").use { raf ->
            val lock = raf.channel.lock()
            try {
                eventsFile.delete()
            } finally {
                lock.release()
            }
        }
    }

    fun applyRetention(eventsFile: File) {
        if (!eventsFile.isFile) return
        RandomAccessFile(eventsFile, "rw").use { raf ->
            val lock = raf.channel.lock()
            try {
                applyRetentionLocked(raf, maxEntries, maxBytes)
            } finally {
                lock.release()
            }
        }
    }

    /**
     * Overload for callers that want to supply explicit limits (e.g. from SharedPreferences).
     */
    fun applyRetention(eventsFile: File, maxEntries: Int, maxBytes: Long) {
        if (!eventsFile.isFile) return
        RandomAccessFile(eventsFile, "rw").use { raf ->
            val lock = raf.channel.lock()
            try {
                applyRetentionLocked(raf, maxEntries, maxBytes)
            } finally {
                lock.release()
            }
        }
    }

    /**
     * Trims the oldest lines when entry count or byte size limits are exceeded.
     * Caller must hold [RandomAccessFile.channel]'s lock for the duration.
     */
    private fun applyRetentionLocked(raf: RandomAccessFile, maxEntries: Int, maxBytes: Long) {
        val lines = readLinesUtf8(raf)
        if (lines.isEmpty()) return

        // Fast path: if well under both limits, skip all trimming work.
        if (lines.size <= maxEntries && byteSize(lines) <= maxBytes) return

        var trimmed = lines
        while (trimmed.size > maxEntries) {
            trimmed = trimmed.drop(1)
        }
        while (byteSize(trimmed) > maxBytes && trimmed.size > 1) {
            trimmed = trimmed.drop(1)
        }
        if (trimmed == lines) return

        raf.setLength(0)
        raf.seek(0)
        trimmed.forEach { line ->
            raf.write(line.toByteArray(Charsets.UTF_8))
            raf.write('\n'.code)
        }
    }

    private fun byteSize(lines: List<String>): Long =
        lines.sumOf { it.toByteArray(Charsets.UTF_8).size + 1L }

    private fun readLinesUtf8(raf: RandomAccessFile): List<String> {
        val len = raf.length().toInt()
        if (len == 0) return emptyList()
        val bytes = ByteArray(len)
        raf.seek(0)
        raf.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
