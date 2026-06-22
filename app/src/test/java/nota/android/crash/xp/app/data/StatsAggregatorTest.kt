package nota.android.crash.xp.app.data

import kotlinx.coroutines.test.runTest
import nota.android.crash.common.data.CrashEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatsAggregatorTest {

    private lateinit var repository: FakeCrashLogRepository
    private lateinit var aggregator: StatsAggregator

    @Before
    fun setUp() {
        repository = FakeCrashLogRepository()
        aggregator = StatsAggregator(repository)
    }

    // ─── Empty repository ───

    @Test
    fun `empty repository returns zero stats`() = runTest {
        val stats = aggregator.computeStats()

        assertEquals(0, stats.totalCount)
        assertEquals(0, stats.uniquePackageCount)
        assertEquals(0L, stats.mostRecentTimestampMs)
        assertTrue(stats.topPackages.isEmpty())
        assertTrue(stats.topExceptionClasses.isEmpty())
        assertTrue(stats.dailyCounts.isEmpty())
    }

    // ─── Single event ───

    @Test
    fun `single event returns correct counts`() = runTest {
        repository.events = listOf(
            CrashEvent(
                id = "e1",
                timestampMs = dayTimestamp(2026, 1, 15),
                packageName = "com.example.a",
                exceptionClass = "NullPointerException",
            )
        )

        val stats = aggregator.computeStats()

        assertEquals(1, stats.totalCount)
        assertEquals(1, stats.uniquePackageCount)
        assertEquals(dayTimestamp(2026, 1, 15), stats.mostRecentTimestampMs)
        assertEquals(1, stats.topPackages.size)
        assertEquals("com.example.a", stats.topPackages[0].packageName)
        assertEquals(1, stats.topPackages[0].count)
        assertEquals(1, stats.topExceptionClasses.size)
        assertEquals("NullPointerException", stats.topExceptionClasses[0].exceptionClass)
        assertEquals(1, stats.dailyCounts.size)
        assertEquals("2026-01-15", stats.dailyCounts[0].date)
        assertEquals(1, stats.dailyCounts[0].count)
    }

    // ─── Multiple events from same package ───

    @Test
    fun `multiple events from same package counted correctly`() = runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.example.a", exceptionClass = "NullPointerException"),
        )

        val stats = aggregator.computeStats()

        assertEquals(3, stats.totalCount)
        assertEquals(1, stats.uniquePackageCount)
        assertEquals(1, stats.topPackages.size)
        assertEquals("com.example.a", stats.topPackages[0].packageName)
        assertEquals(3, stats.topPackages[0].count)
    }

    // ─── Top packages sorted and limited ───

    @Test
    fun `top packages sorted by count descending and limited to 5`() = runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e4", timestampMs = 4000L, packageName = "com.b", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e5", timestampMs = 5000L, packageName = "com.b", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e6", timestampMs = 6000L, packageName = "com.c", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e7", timestampMs = 7000L, packageName = "com.d", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e8", timestampMs = 8000L, packageName = "com.e", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e9", timestampMs = 9000L, packageName = "com.f", exceptionClass = "NullPointerException"),
        )

        val stats = aggregator.computeStats()

        assertEquals(5, stats.topPackages.size)
        // Sorted descending: com.a=3, com.b=2, com.c=1, com.d=1, com.e=1
        assertEquals("com.a", stats.topPackages[0].packageName)
        assertEquals(3, stats.topPackages[0].count)
        assertEquals("com.b", stats.topPackages[1].packageName)
        assertEquals(2, stats.topPackages[1].count)
    }

    // ─── Top exception classes sorted and limited ───

    @Test
    fun `top exception classes sorted by count descending and limited to 5`() = runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e4", timestampMs = 4000L, packageName = "com.b", exceptionClass = "IllegalStateException"),
            CrashEvent(id = "e5", timestampMs = 5000L, packageName = "com.b", exceptionClass = "IllegalStateException"),
            CrashEvent(id = "e6", timestampMs = 6000L, packageName = "com.c", exceptionClass = "RuntimeException"),
            CrashEvent(id = "e7", timestampMs = 7000L, packageName = "com.d", exceptionClass = "ClassCastException"),
            CrashEvent(id = "e8", timestampMs = 8000L, packageName = "com.e", exceptionClass = "IndexOutOfBoundsException"),
            CrashEvent(id = "e9", timestampMs = 9000L, packageName = "com.f", exceptionClass = "ArithmeticException"),
        )

        val stats = aggregator.computeStats()

        assertEquals(5, stats.topExceptionClasses.size)
        // Sorted descending: NPE=3, ISE=2, RCE=1, CCE=1, IOOBE=1
        assertEquals("NullPointerException", stats.topExceptionClasses[0].exceptionClass)
        assertEquals(3, stats.topExceptionClasses[0].count)
        assertEquals("IllegalStateException", stats.topExceptionClasses[1].exceptionClass)
        assertEquals(2, stats.topExceptionClasses[1].count)
    }

    // ─── Daily counts grouped correctly ───

    @Test
    fun `daily counts grouped correctly`() = runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = dayTimestamp(2026, 6, 20), packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = dayTimestamp(2026, 6, 20) + 1000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e3", timestampMs = dayTimestamp(2026, 6, 21), packageName = "com.b", exceptionClass = "NullPointerException"),
        )

        val stats = aggregator.computeStats()

        assertEquals(2, stats.dailyCounts.size)
        // Sorted descending by date
        assertEquals("2026-06-21", stats.dailyCounts[0].date)
        assertEquals(1, stats.dailyCounts[0].count)
        assertEquals("2026-06-20", stats.dailyCounts[1].date)
        assertEquals(2, stats.dailyCounts[1].count)
    }

    // ─── Most recent timestamp ───

    @Test
    fun `most recent timestamp is correct`() = runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 5000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.a", exceptionClass = "NullPointerException"),
        )

        val stats = aggregator.computeStats()

        assertEquals(5000L, stats.mostRecentTimestampMs)
    }

    // ─── Unique package count deduplicates ───

    @Test
    fun `unique package count deduplicates correctly`() = runTest {
        repository.events = listOf(
            CrashEvent(id = "e1", timestampMs = 1000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e2", timestampMs = 2000L, packageName = "com.a", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e3", timestampMs = 3000L, packageName = "com.b", exceptionClass = "NullPointerException"),
            CrashEvent(id = "e4", timestampMs = 4000L, packageName = "com.a", exceptionClass = "NullPointerException"),
        )

        val stats = aggregator.computeStats()

        assertEquals(4, stats.totalCount)
        assertEquals(2, stats.uniquePackageCount)
    }

    // ─── Helpers ───

    /**
     * Returns a timestamp in milliseconds for midnight UTC of the given date.
     */
    private fun dayTimestamp(year: Int, month: Int, day: Int): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        return sdf.parse("$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}")!!.time
    }
}
