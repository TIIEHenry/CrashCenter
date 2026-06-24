package nota.android.crash.log

import nota.android.crash.common.data.CrashEvent
import nota.android.crash.root.RootAccessClient

object RootCrashLogMutation {

    suspend fun deleteById(rootClient: RootAccessClient, path: String, id: String): Boolean {
        val content = rootClient.readText(path) ?: return false
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val filtered = lines.filter { line ->
            val event = CrashEvent.fromJson(line)
            event == null || event.id != id
        }
        if (filtered.size == lines.size) return false
        return if (filtered.isEmpty()) {
            rootClient.delete(path)
        } else {
            rootClient.writeText(path, filtered.joinToString("\n", postfix = "\n"))
        }
    }

    suspend fun clearFile(rootClient: RootAccessClient, path: String): Boolean =
        rootClient.delete(path)

    suspend fun applyRetention(
        rootClient: RootAccessClient,
        path: String,
        maxEntries: Int,
        maxBytes: Long,
    ): Boolean {
        val content = rootClient.readText(path) ?: return false
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return true
        val trimmed = CrashLogJsonlStore.trimLines(lines, maxEntries, maxBytes)
        if (trimmed == lines) return true
        return if (trimmed.isEmpty()) {
            rootClient.delete(path)
        } else {
            rootClient.writeText(path, trimmed.joinToString("\n", postfix = "\n"))
        }
    }

    suspend fun writeEvents(rootClient: RootAccessClient, path: String, events: List<CrashEvent>): Boolean {
        if (events.isEmpty()) return rootClient.delete(path)
        val body = events.sortedBy { it.timestampMs }
            .joinToString("\n", postfix = "\n") { it.toJsonLine() }
        return rootClient.writeText(path, body)
    }
}
