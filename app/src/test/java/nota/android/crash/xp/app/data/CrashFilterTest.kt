package nota.android.crash.xp.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CrashFilterTest {

    // ─── Default construction ───

    @Test
    fun `default construction has all fields null or default`() {
        val filter = CrashFilter()

        assertNull(filter.query)
        assertNull(filter.packageName)
        assertNull(filter.exceptionClass)
        assertNull(filter.sinceMs)
        assertNull(filter.untilMs)
        assertNull(filter.source)
        assertEquals(CrashSortMode.TIME_NEWEST, filter.sortMode)
    }

    // ─── copy with exceptionClass ───

    @Test
    fun `copy with exceptionClass sets only exceptionClass`() {
        val filter = CrashFilter().copy(exceptionClass = "NullPointerException")

        assertNull(filter.query)
        assertNull(filter.packageName)
        assertEquals("NullPointerException", filter.exceptionClass)
        assertNull(filter.sinceMs)
        assertNull(filter.untilMs)
        assertNull(filter.source)
        assertEquals(CrashSortMode.TIME_NEWEST, filter.sortMode)
    }

    @Test
    fun `copy with exceptionClass replaces previous value`() {
        val filter = CrashFilter(exceptionClass = "NullPointerException")
            .copy(exceptionClass = "IllegalStateException")

        assertEquals("IllegalStateException", filter.exceptionClass)
    }

    // ─── copy with packageName ───

    @Test
    fun `copy with packageName sets only packageName`() {
        val filter = CrashFilter().copy(packageName = "com.example.app")

        assertNull(filter.query)
        assertEquals("com.example.app", filter.packageName)
        assertNull(filter.exceptionClass)
        assertNull(filter.sinceMs)
        assertNull(filter.untilMs)
        assertNull(filter.source)
        assertEquals(CrashSortMode.TIME_NEWEST, filter.sortMode)
    }

    @Test
    fun `copy with packageName replaces previous value`() {
        val filter = CrashFilter(packageName = "com.old.app")
            .copy(packageName = "com.new.app")

        assertEquals("com.new.app", filter.packageName)
    }

    // ─── copy with sortMode ───

    @Test
    fun `copy with sortMode changes only sortMode`() {
        val filter = CrashFilter().copy(sortMode = CrashSortMode.EXCEPTION_ASC)

        assertNull(filter.query)
        assertNull(filter.packageName)
        assertNull(filter.exceptionClass)
        assertNull(filter.sinceMs)
        assertNull(filter.untilMs)
        assertNull(filter.source)
        assertEquals(CrashSortMode.EXCEPTION_ASC, filter.sortMode)
    }

    @Test
    fun `copy with sortMode TIME_OLDEST`() {
        val filter = CrashFilter().copy(sortMode = CrashSortMode.TIME_OLDEST)
        assertEquals(CrashSortMode.TIME_OLDEST, filter.sortMode)
    }

    @Test
    fun `copy with sortMode PACKAGE_ASC`() {
        val filter = CrashFilter().copy(sortMode = CrashSortMode.PACKAGE_ASC)
        assertEquals(CrashSortMode.PACKAGE_ASC, filter.sortMode)
    }

    @Test
    fun `copy with sortMode PACKAGE_DESC`() {
        val filter = CrashFilter().copy(sortMode = CrashSortMode.PACKAGE_DESC)
        assertEquals(CrashSortMode.PACKAGE_DESC, filter.sortMode)
    }

    @Test
    fun `copy with sortMode EXCEPTION_DESC`() {
        val filter = CrashFilter().copy(sortMode = CrashSortMode.EXCEPTION_DESC)
        assertEquals(CrashSortMode.EXCEPTION_DESC, filter.sortMode)
    }

    // ─── Data class equality ───

    @Test
    fun `two default filters are equal`() {
        assertEquals(CrashFilter(), CrashFilter())
    }

    @Test
    fun `filters with same values are equal`() {
        val a = CrashFilter(
            packageName = "com.example",
            exceptionClass = "NPE",
            sortMode = CrashSortMode.PACKAGE_ASC,
        )
        val b = CrashFilter(
            packageName = "com.example",
            exceptionClass = "NPE",
            sortMode = CrashSortMode.PACKAGE_ASC,
        )
        assertEquals(a, b)
    }

    @Test
    fun `filter hashCode is consistent with equality`() {
        val a = CrashFilter(packageName = "com.example", exceptionClass = "NPE")
        val b = CrashFilter(packageName = "com.example", exceptionClass = "NPE")
        assertEquals(a.hashCode(), b.hashCode())
    }
}
