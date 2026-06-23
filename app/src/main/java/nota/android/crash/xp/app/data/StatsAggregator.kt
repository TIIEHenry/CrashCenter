package nota.android.crash.xp.app.data

import nota.android.crash.analysis.CrashSignature
import nota.android.crash.analysis.RuleEngine
import nota.android.crash.common.data.CrashEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsAggregator(
    private val repository: CrashLogRepository,
    private val ruleEngine: RuleEngine? = null,
) {

    suspend fun computeStats(): CrashStats {
        val events = repository.getAll(CrashFilter(), limit = Int.MAX_VALUE, offset = 0)
        return CrashStats(
            totalCount = events.size,
            uniquePackageCount = events.map { it.packageName }.distinct().size,
            mostRecentTimestampMs = events.maxOfOrNull { it.timestampMs } ?: 0L,
            topPackages = topPackages(events),
            topExceptionClasses = topExceptionClasses(events),
            topCategories = topCategories(events),
            topClusters = topClusters(events),
            dailyCounts = events.groupBy { dayOf(it.timestampMs) }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.key }
                .map { DailyCount(it.key, it.value) },
        )
    }

    suspend fun computePerAppStats(packageName: String): PerAppStats {
        val events = repository.getAll(
            CrashFilter(packageName = packageName),
            limit = Int.MAX_VALUE,
            offset = 0,
        )
        val topException = topExceptionClasses(events, limit = 1).firstOrNull()
        return PerAppStats(
            totalCount = events.size,
            mostRecentTimestampMs = events.maxOfOrNull { it.timestampMs } ?: 0L,
            topExceptionClass = topException?.exceptionClass,
            topExceptionCount = topException?.count ?: 0,
        )
    }

    fun topPackages(events: List<CrashEvent>, limit: Int = TOP_N): List<PackageCount> =
        events.groupBy { it.packageName }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .take(limit)
            .map { PackageCount(it.key, it.value) }

    fun topExceptionClasses(events: List<CrashEvent>, limit: Int = TOP_N): List<ExceptionCount> =
        events.groupBy { it.exceptionClass }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .take(limit)
            .map { ExceptionCount(it.key, it.value) }

    fun topCategories(events: List<CrashEvent>, limit: Int = TOP_N): List<CategoryCount> =
        events.groupBy { resolveCategory(it) }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .take(limit)
            .map { CategoryCount(it.key, it.value) }

    fun topClusters(events: List<CrashEvent>, limit: Int = TOP_N): List<ClusterCount> =
        events.groupBy { CrashSignature.clusterId(it.exceptionClass, it.stackTrace) }
            .map { (clusterId, clusterEvents) ->
                val sample = clusterEvents.first()
                ClusterCount(
                    clusterId = clusterId,
                    label = clusterLabel(sample),
                    count = clusterEvents.size,
                )
            }
            .sortedByDescending { it.count }
            .take(limit)

    private fun resolveCategory(event: CrashEvent): String {
        val matched = ruleEngine?.match(event.exceptionClass, event.stackTrace)
        return matched?.category ?: event.shortExceptionClass
    }

    private fun clusterLabel(event: CrashEvent): String {
        val frame = CrashSignature.firstFrameLabel(event.stackTrace)
        return if (frame != null) {
            "${event.shortExceptionClass} · $frame"
        } else {
            event.shortExceptionClass
        }
    }

    private fun dayOf(timestampMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        return sdf.format(Date(timestampMs))
    }

    companion object {
        const val TOP_N = 5
    }
}

data class CrashStats(
    val totalCount: Int,
    val uniquePackageCount: Int,
    val mostRecentTimestampMs: Long,
    val topPackages: List<PackageCount>,
    val topExceptionClasses: List<ExceptionCount>,
    val topCategories: List<CategoryCount>,
    val topClusters: List<ClusterCount>,
    val dailyCounts: List<DailyCount>,
)

data class PackageCount(val packageName: String, val count: Int)
data class ExceptionCount(val exceptionClass: String, val count: Int)
data class CategoryCount(val category: String, val count: Int)
data class ClusterCount(val clusterId: String, val label: String, val count: Int)
data class DailyCount(val date: String, val count: Int)
data class PerAppStats(
    val totalCount: Int,
    val mostRecentTimestampMs: Long,
    val topExceptionClass: String?,
    val topExceptionCount: Int,
)
