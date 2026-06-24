package nota.android.crash.xp.app.data

import android.content.Context
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.CanonicalJsonlWriter
import nota.android.crash.xp.app.config.AppFilterEngine
import android.content.SharedPreferences
import nota.android.crash.xp.PrefManager
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

interface CrashLogRepository {
    fun getAll(filter: CrashFilter = CrashFilter(), limit: Int, offset: Int): List<CrashEvent>
    fun getById(id: String): CrashEvent?
    fun getCount(filter: CrashFilter = CrashFilter()): Int
    fun getPackageCounts(filter: CrashFilter = CrashFilter()): List<Pair<String, Int>>
    fun deleteById(id: String): Boolean
    fun deleteByPackage(packageName: String): Int
    fun clear()
    fun applyRetention()
}

class FileCrashLogRepository(
    context: Context,
    private val prefs: SharedPreferences? = null,
) : CrashLogRepository {

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

    override fun getPackageCounts(filter: CrashFilter): List<Pair<String, Int>> {
        return lock.read {
            val counts = mutableMapOf<String, Int>()
            streamEvents(filter = filter) { event ->
                counts[event.packageName] = (counts[event.packageName] ?: 0) + 1
                true
            }
            counts.entries
                .sortedByDescending { it.value }
                .map { it.key to it.value }
        }
    }

    override fun deleteById(id: String): Boolean {
        return lock.write {
            val deleted = CanonicalJsonlWriter.deleteById(eventsFile, id)
            if (deleted) invalidateCache()
            deleted
        }
    }

    override fun deleteByPackage(packageName: String): Int {
        return lock.write {
            val removed = CanonicalJsonlWriter.deleteByPackage(eventsFile, packageName)
            if (removed > 0) invalidateCache()
            removed
        }
    }

    override fun clear() {
        lock.write {
            CanonicalJsonlWriter.clear(eventsFile)
            invalidateCache()
        }
    }

    /**
     * Streams events sorted by [CrashEvent.timestampMs] descending (newest first).
     * Duplicate IDs are deduplicated (first occurrence in file wins) to handle events
     * written by multiple backends.
     *
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

        // Collect all events from file (dedup by id), then sort descending by timestamp
        val allEvents = mutableListOf<CrashEvent>()
        val seenIds = mutableSetOf<String>()

        BufferedReader(FileReader(eventsFile)).use { reader ->
            reader.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach

                val event = CrashEvent.fromJson(trimmed) ?: return@forEach

                // Add to LRU cache if not already present
                if (!cache.containsKey(event.id)) {
                    cache.put(event.id, event)
                }

                // Deduplicate: skip events with IDs already seen
                if (!seenIds.add(event.id)) return@forEach

                allEvents.add(event)
            }
        }

        // Sort according to the sort mode in the filter (default: newest first)
        val sortMode = filter?.sortMode ?: CrashSortMode.TIME_NEWEST
        val sorted = when (sortMode) {
            CrashSortMode.TIME_NEWEST -> allEvents.sortedByDescending { it.timestampMs }
            CrashSortMode.TIME_OLDEST -> allEvents.sortedBy { it.timestampMs }
            CrashSortMode.PACKAGE_ASC -> allEvents.sortedBy { it.packageName }
            CrashSortMode.PACKAGE_DESC -> allEvents.sortedByDescending { it.packageName }
            CrashSortMode.EXCEPTION_ASC -> allEvents.sortedBy { it.exceptionClass }
            CrashSortMode.EXCEPTION_DESC -> allEvents.sortedByDescending { it.exceptionClass }
        }

        // Filter and invoke action on sorted events
        val matchingEvents = if (filter != null) mutableListOf<CrashEvent>() else null

        for (event in sorted) {
            val matches = filter == null || AppFilterEngine.matchesCrashEvent(event, filter)
            if (matches) {
                matchingEvents?.add(event)
                if (!action(event) && !collectAll) {
                    break
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
            if (prefs != null) {
                val maxEntries = prefs.getInt(
                    PrefManager.PREF_CRASH_LOG_MAX_ENTRIES,
                    CanonicalJsonlWriter.DEFAULT_MAX_ENTRIES,
                )
                val maxBytes = prefs.getLong(
                    PrefManager.PREF_CRASH_LOG_MAX_BYTES,
                    CanonicalJsonlWriter.DEFAULT_MAX_BYTES,
                )
                // Sync volatile fields so hook-side append() sees the updated limits
                CanonicalJsonlWriter.maxEntries = maxEntries
                CanonicalJsonlWriter.maxBytes = maxBytes
                CanonicalJsonlWriter.applyRetention(eventsFile, maxEntries, maxBytes)
            } else {
                CanonicalJsonlWriter.applyRetention(eventsFile)
            }
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
