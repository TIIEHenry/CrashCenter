package nota.android.crash.log

import nota.android.crash.common.data.CrashEvent
import java.io.File
import java.io.RandomAccessFile

/**
 * Per-file append-only JSONL store with file lock and default retention
 * (500 entries or 8 MB, whichever limit is hit first).
 */
object CrashLogJsonlStore {

    const val DEFAULT_MAX_ENTRIES = 500
    const val DEFAULT_MAX_BYTES = 8L * 1024L * 1024L

    const val MAX_ENTRIES = DEFAULT_MAX_ENTRIES
    const val MAX_BYTES = DEFAULT_MAX_BYTES

    @Volatile
    var maxEntries: Int = DEFAULT_MAX_ENTRIES

    @Volatile
    var maxBytes: Long = DEFAULT_MAX_BYTES

    fun append(
        eventsFile: File,
        event: CrashEvent,
        maxEntries: Int = this.maxEntries,
        maxBytes: Long = this.maxBytes,
    ) {
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
                applyRetentionLocked(raf, maxEntries, maxBytes)
            } finally {
                lock.release()
            }
        }
    }

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
                if (filtered.size == lines.size) return false

                if (filtered.isEmpty()) {
                    eventsFile.delete()
                } else {
                    rewriteLines(raf, filtered)
                }
            } finally {
                lock.release()
            }
        }
        return true
    }

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
                    rewriteLines(raf, filtered)
                }
                return removed
            } finally {
                lock.release()
            }
        }
    }

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
        applyRetention(eventsFile, maxEntries, maxBytes)
    }

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

    fun trimLines(lines: List<String>, maxEntries: Int, maxBytes: Long): List<String> {
        if (lines.isEmpty()) return lines
        var trimmed = lines
        while (trimmed.size > maxEntries) {
            trimmed = trimmed.drop(1)
        }
        while (byteSize(trimmed) > maxBytes && trimmed.size > 1) {
            trimmed = trimmed.drop(1)
        }
        return trimmed
    }

    private fun applyRetentionLocked(raf: RandomAccessFile, maxEntries: Int, maxBytes: Long) {
        val lines = readLinesUtf8(raf)
        if (lines.isEmpty()) return
        if (lines.size <= maxEntries && byteSize(lines) <= maxBytes) return

        val trimmed = trimLines(lines, maxEntries, maxBytes)
        if (trimmed == lines) return
        rewriteLines(raf, trimmed)
    }

    private fun rewriteLines(raf: RandomAccessFile, lines: List<String>) {
        raf.setLength(0)
        raf.seek(0)
        lines.forEach { line ->
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
