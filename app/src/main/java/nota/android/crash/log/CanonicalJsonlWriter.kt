package nota.android.crash.log

import nota.android.crash.common.data.CrashEvent
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
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
            } finally {
                lock.release()
            }
        }
        applyRetention(eventsFile)
    }

    fun applyRetention(eventsFile: File) {
        if (!eventsFile.isFile) return
        val lines = readNonEmptyLines(eventsFile)
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

        RandomAccessFile(eventsFile, "rw").use { raf ->
            val lock = raf.channel.lock()
            try {
                raf.setLength(0)
                trimmed.forEach { line ->
                    raf.write(line.toByteArray(Charsets.UTF_8))
                    raf.write('\n'.code)
                }
            } finally {
                lock.release()
            }
        }
    }

    private fun byteSize(lines: List<String>): Long =
        lines.sumOf { it.toByteArray(Charsets.UTF_8).size + 1L }

    private fun readNonEmptyLines(file: File): List<String> {
        val lines = mutableListOf<String>()
        BufferedReader(FileReader(file)).use { reader ->
            reader.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    lines.add(trimmed)
                }
            }
        }
        return lines
    }
}
