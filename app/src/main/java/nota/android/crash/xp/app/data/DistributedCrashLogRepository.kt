package nota.android.crash.xp.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.CrashLogCacheScanner
import nota.android.crash.log.CrashLogJsonlStore
import nota.android.crash.log.CrashLogPaths
import nota.android.crash.log.RootCrashLogMutation
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootAvailability
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.config.AppFilterEngine
import nota.android.crash.xp.app.di.ServiceLocator
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext

/**
 * Root-required aggregate read over per-app `cache/crash_logs/events.jsonl` (ADR-024).
 */
class DistributedCrashLogRepository(
    context: Context,
    private val prefs: SharedPreferences? = null,
    private val rootClientProvider: (Context) -> RootAccessClient = { ServiceLocator.rootAccessClient(it) },
) : CrashLogRepository {

    private val appContext = context.applicationContext
    private val lock = ReentrantReadWriteLock()
    private val rootIoContext: CoroutineContext =
        Executors.newSingleThreadExecutor { r -> Thread(r, "crash-log-root-io") }.asCoroutineDispatcher()

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<String, CrashEvent>(CACHE_CAPACITY, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CrashEvent>?): Boolean =
                size > CACHE_CAPACITY
        },
    )

    private var lastSourceFingerprint: AtomicLong = AtomicLong(0L)
    private var aggregatedEvents: List<CrashEvent> = emptyList()
    private val cachedMatchingEvents = ConcurrentHashMap<Int, List<CrashEvent>>()
    private val cachedFullyCollected = ConcurrentHashMap<Int, Boolean>()

    override fun getAll(filter: CrashFilter, limit: Int, offset: Int): List<CrashEvent> {
        refreshAggregateIfNeeded()
        return lock.read {
            val result = mutableListOf<CrashEvent>()
            var skipped = 0
            var taken = 0
            streamEvents(filter = filter) { event ->
                if (skipped < offset) {
                    skipped++
                    return@streamEvents true
                }
                if (taken < limit) {
                    result.add(event)
                    taken++
                    return@streamEvents true
                }
                false
            }
            result
        }
    }

    override fun getById(id: String): CrashEvent? {
        refreshAggregateIfNeeded()
        return lock.read {
            cache[id]?.let { return@read it }
            aggregatedEvents.find { it.id == id }?.also { cache[id] = it }
        }
    }

    override fun getCount(filter: CrashFilter): Int {
        refreshAggregateIfNeeded()
        return lock.read {
            val key = cacheKeyFor(filter)
            if (cachedFullyCollected[key] == true) {
                cachedMatchingEvents[key]?.size?.let { return@read it }
            }
            var count = 0
            streamEvents(filter = filter) {
                count++
                true
            }
            count
        }
    }

    override fun getPackageCounts(filter: CrashFilter): List<Pair<String, Int>> {
        refreshAggregateIfNeeded()
        return lock.read {
            val counts = mutableMapOf<String, Int>()
            streamEvents(filter = filter) { event ->
                counts[event.packageName] = (counts[event.packageName] ?: 0) + 1
                true
            }
            counts.entries.sortedByDescending { it.value }.map { it.key to it.value }
        }
    }

    override fun deleteById(id: String): Boolean {
        refreshAggregateIfNeeded()
        val event = lock.read { aggregatedEvents.find { it.id == id } } ?: return false
        return lock.write {
            if (!isRootAvailable()) return@write false
            for (path in findPathsForPackage(event.packageName)) {
                val deleted = runRootIo {
                    RootCrashLogMutation.deleteById(rootClient(), path, id)
                }
                if (deleted) {
                    invalidateCache()
                    return@write true
                }
            }
            false
        }
    }

    override fun deleteByPackage(packageName: String): Int {
        return lock.write {
            if (!isRootAvailable()) return@write 0
            var removed = 0
            for (path in findPathsForPackage(packageName)) {
                val count = runRootIo {
                    val content = rootClient().readText(path) ?: return@runRootIo 0
                    val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
                    val lineCount = lines.count { CrashEvent.fromJson(it)?.packageName == packageName }
                    if (lineCount == 0) return@runRootIo 0
                    if (RootCrashLogMutation.clearFile(rootClient(), path)) lineCount else 0
                }
                removed += count
            }
            if (removed > 0) invalidateCache()
            removed
        }
    }

    override fun clear() {
        lock.write {
            if (!isRootAvailable()) return@write
            runRootIo {
                for (ref in CrashLogCacheScanner.scanEventFiles(rootClient())) {
                    RootCrashLogMutation.clearFile(rootClient(), ref.path)
                }
            }
            invalidateCache()
        }
    }

    override fun applyRetention() {
        lock.write {
            if (!isRootAvailable()) return@write
            val maxEntries = prefs?.getInt(
                PrefManager.PREF_CRASH_LOG_MAX_ENTRIES,
                CrashLogJsonlStore.DEFAULT_MAX_ENTRIES,
            ) ?: CrashLogJsonlStore.DEFAULT_MAX_ENTRIES
            val maxBytes = prefs?.getLong(
                PrefManager.PREF_CRASH_LOG_MAX_BYTES,
                CrashLogJsonlStore.DEFAULT_MAX_BYTES,
            ) ?: CrashLogJsonlStore.DEFAULT_MAX_BYTES
            CrashLogJsonlStore.maxEntries = maxEntries
            CrashLogJsonlStore.maxBytes = maxBytes
            runRootIo {
                for (ref in CrashLogCacheScanner.scanEventFiles(rootClient())) {
                    RootCrashLogMutation.applyRetention(rootClient(), ref.path, maxEntries, maxBytes)
                }
            }
            invalidateCache()
        }
    }

    private fun streamEvents(
        filter: CrashFilter? = null,
        collectAll: Boolean = false,
        action: (CrashEvent) -> Boolean,
    ) {
        val sortMode = filter?.sortMode ?: CrashSortMode.TIME_NEWEST
        val sorted = when (sortMode) {
            CrashSortMode.TIME_NEWEST -> aggregatedEvents.sortedByDescending { it.timestampMs }
            CrashSortMode.TIME_OLDEST -> aggregatedEvents.sortedBy { it.timestampMs }
            CrashSortMode.PACKAGE_ASC -> aggregatedEvents.sortedBy { it.packageName }
            CrashSortMode.PACKAGE_DESC -> aggregatedEvents.sortedByDescending { it.packageName }
            CrashSortMode.EXCEPTION_ASC -> aggregatedEvents.sortedBy { it.exceptionClass }
            CrashSortMode.EXCEPTION_DESC -> aggregatedEvents.sortedByDescending { it.exceptionClass }
        }

        if (filter != null) {
            val key = cacheKeyFor(filter)
            val cached = cachedMatchingEvents[key]
            if (cached != null && cachedFullyCollected[key] == true) {
                for (event in cached) {
                    if (!action(event)) break
                }
                return
            }
        }

        val matchingEvents = if (filter != null) mutableListOf<CrashEvent>() else null
        for (event in sorted) {
            val matches = filter == null || AppFilterEngine.matchesCrashEvent(event, filter)
            if (matches) {
                matchingEvents?.add(event)
                if (!action(event) && !collectAll) break
            }
        }

        if (filter != null && matchingEvents != null) {
            val key = cacheKeyFor(filter)
            cachedMatchingEvents[key] = matchingEvents
            cachedFullyCollected[key] = true
        }
    }

    private fun refreshAggregateIfNeeded() {
        val fingerprint = computeSourceFingerprint()
        if (fingerprint == lastSourceFingerprint.get()) {
            return
        }
        synchronized(this) {
            if (fingerprint == lastSourceFingerprint.get()) {
                return
            }
            aggregatedEvents = if (isRootAvailable()) {
                runRootIo { loadAggregatedEvents() }
            } else {
                emptyList()
            }
            lastSourceFingerprint.set(fingerprint)
            cache.clear()
            cachedMatchingEvents.clear()
            cachedFullyCollected.clear()
            aggregatedEvents.forEach { cache[it.id] = it }
        }
    }

    private suspend fun loadAggregatedEvents(): List<CrashEvent> {
        val byId = mutableMapOf<String, CrashEvent>()
        for (ref in CrashLogCacheScanner.scanEventFiles(rootClient())) {
            val content = rootClient().readText(ref.path) ?: continue
            for (line in content.lineSequence()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val event = CrashEvent.fromJson(trimmed) ?: continue
                val existing = byId[event.id]
                if (existing == null || event.timestampMs > existing.timestampMs) {
                    byId[event.id] = event
                }
            }
        }
        return byId.values.toList()
    }

    private fun computeSourceFingerprint(): Long {
        if (!isRootAvailable()) return 0L
        return runRootIo {
            val parts = CrashLogCacheScanner.scanEventFiles(rootClient())
                .sortedWith(compareBy({ it.userId }, { it.packageName }))
                .map { ref ->
                    val stat = rootClient().fileStat(ref.path)
                    "${ref.path}:${stat?.mtimeMs ?: 0L}:${stat?.length ?: 0L}"
                }
            parts.joinToString("|").hashCode().toLong()
        }
    }

    private fun findPathsForPackage(packageName: String): List<String> {
        if (!isRootAvailable()) return emptyList()
        return runRootIo {
            CrashLogCacheScanner.scanEventFiles(rootClient())
                .filter { it.packageName == packageName }
                .map { it.path }
        }
    }

    private fun <T> runRootIo(block: suspend () -> T): T = runBlocking(rootIoContext) { block() }

    private fun isRootAvailable(): Boolean =
        rootClient().probe() == RootAvailability.AVAILABLE

    private fun rootClient(): RootAccessClient = rootClientProvider(appContext)

    private fun invalidateCache() {
        cache.clear()
        cachedMatchingEvents.clear()
        cachedFullyCollected.clear()
        lastSourceFingerprint.set(0L)
        aggregatedEvents = emptyList()
    }

    private fun cacheKeyFor(filter: CrashFilter): Int {
        var result = filter.hashCode()
        result = 31 * result + lastSourceFingerprint.get().hashCode()
        return result
    }

    companion object {
        const val CACHE_CAPACITY = 200
    }
}
