package nota.android.crash.xp.app.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigListControllerTest {

    private enum class TestFilter { ALL, ACTIVE, ARCHIVED }

    private val chipToFilter = mapOf(
        1 to TestFilter.ALL,
        2 to TestFilter.ACTIVE,
        3 to TestFilter.ARCHIVED,
    )

    private val defaultFilter = TestFilter.ALL

    // ---------- resolveFilter ----------

    @Test
    fun `resolveFilter returns mapped value for known chip id`() {
        val result = FilterConfig.resolveFilter(chipToFilter, defaultFilter, listOf(2))
        assertEquals(TestFilter.ACTIVE, result)
    }

    @Test
    fun `resolveFilter returns first mapped value when multiple ids present`() {
        val result = FilterConfig.resolveFilter(chipToFilter, defaultFilter, listOf(3, 1))
        assertEquals(TestFilter.ARCHIVED, result)
    }

    @Test
    fun `resolveFilter falls back to defaultFilter for empty list`() {
        val result = FilterConfig.resolveFilter(chipToFilter, defaultFilter, emptyList())
        assertEquals(TestFilter.ALL, result)
    }

    @Test
    fun `resolveFilter falls back to defaultFilter for unknown chip id`() {
        val result = FilterConfig.resolveFilter(chipToFilter, defaultFilter, listOf(999))
        assertEquals(TestFilter.ALL, result)
    }

    @Test
    fun `resolveFilter falls back to defaultFilter when list starts with unknown id`() {
        val result = FilterConfig.resolveFilter(chipToFilter, defaultFilter, listOf(999, 2))
        assertEquals(TestFilter.ALL, result)
    }

    @Test
    fun `resolveFilter returns first element of single-element list`() {
        val result = FilterConfig.resolveFilter(chipToFilter, defaultFilter, listOf(3))
        assertEquals(TestFilter.ARCHIVED, result)
    }
}
