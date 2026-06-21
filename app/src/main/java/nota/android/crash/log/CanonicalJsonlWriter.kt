package nota.android.crash.log

import nota.android.crash.common.data.CrashEvent
import java.io.File
import java.io.RandomAccessFile

/**
 * Append-only canonical [events.jsonl] with file lock and default retention
 * (500 entries or 8 MB, whichever limit is hit first).
 */
object CanonicalJsonlWriter {

    const val MAX_ENTRIES = 500
    const val MAX_BYTES = 8L * 1024L * 1024L

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
                applyRetentionLocked(raf)
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
                applyRetentionLocked(raf)
            } finally {
                lock.release()
            }
        }
    }

    /**
     * Trims the oldest lines when entry count or byte size limits are exceeded.
     * Caller must hold [RandomAccessFile.channel]'s lock for the duration.
     */
    private fun applyRetentionLocked(raf: RandomAccessFile) {
        val lines = readLinesUtf8(raf)
        if (lines.isEmpty()) return

        // Fast path: if well under both limits, skip all trimming work.
        if (lines.size <= MAX_ENTRIES && byteSize(lines) <= MAX_BYTES) return

        var trimmed = lines
        while (trimmed.size > MAX_ENTRIES) {
            trimmed = trimmed.drop(1)
        }
        while (byteSize(trimmed) > MAX_BYTES && trimmed.size > 1) {
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
