package nota.android.crash.xp.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import nota.android.crash.log.CanonicalJsonlWriter
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

interface CrashLogRepository {
    fun getAll(filter: CrashFilter, limit: Int, offset: Int): List<CrashEvent>
    fun getById(id: String): CrashEvent?
    fun getCount(filter: CrashFilter): Int
    fun deleteById(id: String): Boolean
    fun clear()
    fun observeChanges(): Flow<Unit>
    fun applyRetention()
}

class FileCrashLogRepository(context: Context) : CrashLogRepository {

    private val eventsFile = File(context.applicationContext.filesDir, "$LOG_DIR/$EVENTS_FILE")
    private val lock = ReentrantReadWriteLock()

    // LRU cache: most recent events by insertion order (newest first after sort)
    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<String, CrashEvent>(CACHE_CAPACITY, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CrashEvent>?): Boolean {
                return size > CACHE_CAPACITY
            }
        }
    )

    // Track file modification time to invalidate cache when file changes
    private var lastFileModified: AtomicLong = AtomicLong(0L)
    private var lastFileLength: AtomicLong = AtomicLong(0L)

    override fun getAll(filter: CrashFilter, limit: Int, offset: Int): List<CrashEvent> {
        return lock.read {
            val result = mutableListOf<CrashEvent>()
            var skipped = 0
            var taken = 0

            streamEvents { event ->
                if (!matchesFilter(event, filter)) return@streamEvents true // continue
                if (skipped < offset) {
                    skipped++
                    return@streamEvents true // continue
                }
                if (taken < limit) {
                    result.add(event)
                    taken++
                    return@streamEvents true // continue
                }
                false // stop - we've reached the limit
            }
            result
        }
    }

    override fun getById(id: String): CrashEvent? {
        return lock.read {
            // Check cache first
            cache[id]?.let { return@read it }

            var found: CrashEvent? = null
            streamEvents { event ->
                if (event.id == id) {
                    found = event
                    cache[event.id] = event
                    false // stop reading
                } else {
                    true // continue
                }
            }
            found
        }
    }

    override fun getCount(filter: CrashFilter): Int {
        return lock.read {
            var count = 0
            streamEvents { event ->
                if (matchesFilter(event, filter)) {
                    count++
                }
                true // always continue for count (no early termination possible unless we had a max)
            }
            count
        }
    }

    override fun deleteById(id: String): Boolean {
        return lock.write {
            if (!eventsFile.isFile) return@write false

            val tempFile = File(eventsFile.parentFile, "${eventsFile.name}.tmp")
            var deleted = false

            BufferedReader(FileReader(eventsFile)).use { reader ->
                java.io.FileWriter(tempFile).use { writer ->
                    reader.lineSequence().forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) return@forEach

                        val event = CrashEvent.fromJson(trimmed)
                        if (event != null && event.id == id) {
                            deleted = true
                            return@forEach // skip this line
                        }
                        writer.write(line)
                        writer.write("\n")
                    }
                }
            }

            if (deleted) {
                if (tempFile.length() == 0L) {
                    eventsFile.delete()
                } else {
                    tempFile.renameTo(eventsFile)
                }
                cache.clear()
                lastFileModified.set(0L)
                lastFileLength.set(0L)
            } else {
                tempFile.delete()
            }

            deleted
        }
    }

    override fun clear() {
        lock.write {
            eventsFile.delete()
            cache.clear()
            lastFileModified.set(0L)
            lastFileLength.set(0L)
        }
    }

    /**
     * Streams events from newest to oldest (file is sorted descending by timestamp).
     * The [action] returns true to continue, false to stop early.
     * Also populates the LRU cache as events are read.
     */
    private inline fun streamEvents(action: (CrashEvent) -> Boolean) {
        if (!eventsFile.isFile) return

        // Check if file changed since last read
        val currentModified = eventsFile.lastModified()
        val currentLength = eventsFile.length()
        if (currentModified != lastFileModified.get() || currentLength != lastFileLength.get()) {
            cache.clear()
            lastFileModified.set(currentModified)
            lastFileLength.set(currentLength)
        }

        BufferedReader(FileReader(eventsFile)).use { reader ->
            reader.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach

                val event = CrashEvent.fromJson(trimmed) ?: return@forEach

                // Add to cache if not already present
                if (!cache.containsKey(event.id)) {
                    cache.put(event.id, event)
                }

                if (!action(event)) {
                    return@use // early termination
                }
            }
        }
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

    override fun observeChanges(): Flow<Unit> = emptyFlow()

    override fun applyRetention() {
        lock.write {
            CanonicalJsonlWriter.applyRetention(eventsFile)
            cache.clear()
            lastFileModified.set(0L)
            lastFileLength.set(0L)
        }
    }

    companion object {
        const val LOG_DIR = "crash_logs"
        const val EVENTS_FILE = "events.jsonl"
        const val CACHE_CAPACITY = 200

        fun eventsFile(context: Context): File =
            File(context.applicationContext.filesDir, "$LOG_DIR/$EVENTS_FILE")
    }
}
