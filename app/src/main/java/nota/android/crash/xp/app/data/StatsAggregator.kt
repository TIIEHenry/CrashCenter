package nota.android.crash.xp.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsAggregator(private val repository: CrashLogRepository) {

    suspend fun computeStats(): CrashStats {
        val events = repository.getAll(CrashFilter(), limit = Int.MAX_VALUE, offset = 0)
        return CrashStats(
            totalCount = events.size,
            uniquePackageCount = events.map { it.packageName }.distinct().size,
            mostRecentTimestampMs = events.maxOfOrNull { it.timestampMs } ?: 0L,
            topPackages = events.groupBy { it.packageName }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.value }
                .take(5)
                .map { PackageCount(it.key, it.value) },
            topExceptionClasses = events.groupBy { it.exceptionClass }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.value }
                .take(5)
                .map { ExceptionCount(it.key, it.value) },
            dailyCounts = events.groupBy { dayOf(it.timestampMs) }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.key }
                .map { DailyCount(it.key, it.value) }
        )
    }

    private fun dayOf(timestampMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        return sdf.format(Date(timestampMs))
    }
}

data class CrashStats(
    val totalCount: Int,
    val uniquePackageCount: Int,
    val mostRecentTimestampMs: Long,
    val topPackages: List<PackageCount>,
    val topExceptionClasses: List<ExceptionCount>,
    val dailyCounts: List<DailyCount>,
)

data class PackageCount(val packageName: String, val count: Int)
data class ExceptionCount(val exceptionClass: String, val count: Int)
data class DailyCount(val date: String, val count: Int)
