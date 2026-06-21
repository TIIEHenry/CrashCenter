package nota.android.crash.xp.app.data

import android.content.Context
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.CanonicalJsonlWriter
import nota.android.crash.xp.app.config.AppFilterEngine
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
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

    // Per-filter cache of matching events for a single request cycle (e.g., PagingSource load).
    // Key: filter hash + file mtime + file length. Null means cache miss / no file.
    // Avoids double-parse when getCount() and getAll() are called with the same filter.
    private val cachedMatchingEvents = ConcurrentHashMap<Int, List<CrashEvent>>()
    private val cachedFullyCollected = ConcurrentHashMap<Int, Boolean>()

    override fun getAll(filter: CrashFilter, limit: Int, offset: Int): List<CrashEvent> {
        return lock.read {
            val result = mutableListOf<CrashEvent>()
            var skipped = 0
            var taken = 0

            streamEvents(filter = filter) { event ->
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
            // Check if a prior getAll() / getCount() already collected all matching events
            val key = cacheKeyFor(filter)
            if (cachedFullyCollected[key] == true) {
                cachedMatchingEvents[key]?.size?.let { return@read it }
            }

            var count = 0
            streamEvents(filter = filter) { _ ->
                count++
                true // always continue for count
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
                invalidateCache()
            } else {
                tempFile.delete()
            }

            deleted
        }
    }

    override fun clear() {
        lock.write {
            eventsFile.delete()
            invalidateCache()
        }
    }

    /**
     * Streams events from newest to oldest (file is sorted descending by timestamp).
     * The [action] returns true to continue, false to stop early.
     * Also populates the LRU cache as events are read.
     *
     * @param filter if non-null, collects all matching events into [cachedMatchingEvents]
     *               for reuse by a subsequent [getCount] or [getAll] call with the same filter.
     * @param collectAll when true, scans the entire file regardless of [action] early termination.
     */
    private inline fun streamEvents(
        filter: CrashFilter? = null,
        collectAll: Boolean = false,
        action: (CrashEvent) -> Boolean,
    ) {
        if (!eventsFile.isFile) return

        // Check if file changed since last read
        val currentModified = eventsFile.lastModified()
        val currentLength = eventsFile.length()
        if (currentModified != lastFileModified.get() || currentLength != lastFileLength.get()) {
            cache.clear()
            cachedMatchingEvents.clear()
            cachedFullyCollected.clear()
            lastFileModified.set(currentModified)
            lastFileLength.set(currentLength)
        }

        // Check if cached matching events are still valid for this filter+file combination
        if (filter != null) {
            val key = cacheKeyFor(filter)
            val cached = cachedMatchingEvents[key]
            if (cached != null && cachedFullyCollected[key] == true) {
                // Cache hit: replay matching events from cache
                for (event in cached) {
                    if (!action(event)) break
                }
                return
            }
        }

        val matchingEvents = if (filter != null) mutableListOf<CrashEvent>() else null

        BufferedReader(FileReader(eventsFile)).use { reader ->
            reader.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach

                val event = CrashEvent.fromJson(trimmed) ?: return@forEach

                // Add to cache if not already present
                if (!cache.containsKey(event.id)) {
                    cache.put(event.id, event)
                }

                val matches = filter == null || AppFilterEngine.matchesCrashEvent(event, filter)
                if (matches) {
                    matchingEvents?.add(event)
                    if (!action(event) && !collectAll) {
                        return@use // early termination
                    }
                }
            }
        }

        // Store the matching events for reuse by getCount()/getAll() with the same filter
        if (filter != null && matchingEvents != null) {
            val key = cacheKeyFor(filter)
            cachedMatchingEvents[key] = matchingEvents
            cachedFullyCollected[key] = true
        }
    }

    private fun cacheKeyFor(filter: CrashFilter): Int {
        var result = filter.hashCode()
        result = 31 * result + lastFileModified.get().hashCode()
        result = 31 * result + lastFileLength.get().hashCode()
        return result
    }

    override fun applyRetention() {
        lock.write {
            CanonicalJsonlWriter.applyRetention(eventsFile)
            invalidateCache()
        }
    }

    private fun invalidateCache() {
        cache.clear()
        cachedMatchingEvents.clear()
        cachedFullyCollected.clear()
        lastFileModified.set(0L)
        lastFileLength.set(0L)
    }

    companion object {
        const val LOG_DIR = "crash_logs"
        const val EVENTS_FILE = "events.jsonl"
        const val CACHE_CAPACITY = 200

        fun eventsFile(context: Context): File =
            File(context.applicationContext.filesDir, "$LOG_DIR/$EVENTS_FILE")
    }
}
