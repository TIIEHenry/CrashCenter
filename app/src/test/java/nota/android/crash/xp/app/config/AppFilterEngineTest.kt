package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.CrashFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AppFilterEngineTest {

    @Test
    fun `sort NAME_ASC sorts ascending`() {
        val list = listOf(
            fakeAppItem("com.c", "Charlie"),
            fakeAppItem("com.a", "Alpha"),
            fakeAppItem("com.b", "Beta"),
        )
        val sorted = AppFilterEngine.sort(list, SortMode.NAME_ASC)
        assertEquals(listOf("Alpha", "Beta", "Charlie"), sorted.map { it.label })
    }

    @Test
    fun `sort UPDATE_TIME_DESC sorts descending`() {
        val list = listOf(
            fakeAppItem("com.c", "C", updateTime = 3),
            fakeAppItem("com.a", "A", updateTime = 1),
            fakeAppItem("com.b", "B", updateTime = 2),
        )
        val sorted = AppFilterEngine.sort(list, SortMode.UPDATE_TIME_DESC)
        assertEquals(listOf(3L, 2L, 1L), sorted.map { it.updateTime })
    }

    @Test
    fun `matchesCrashEvent empty filter matches any event`() {
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, CrashFilter()))
    }

    @Test
    fun `matchesCrashEvent packageName mismatch`() {
        val filter = CrashFilter(packageName = "com.other.app")
        assertFalse(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent query matches appLabel case insensitive`() {
        val filter = CrashFilter(query = "EXAMPLE")
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent I lowercased correctly under Turkish locale`() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr", "TR"))
            val event = CrashEvent(
                id = "evt-tr",
                timestampMs = 1000L,
                packageName = "com.example.app",
                appLabel = "IllegalAccess",
                exceptionClass = "java.lang.Exception",
                message = "illegal",
            )
            val filter = CrashFilter(query = "illegal")
            assertTrue(AppFilterEngine.matchesCrashEvent(event, filter))
        } finally {
            Locale.setDefault(saved)
        }
    }

    private val sampleEvent = CrashEvent(
        id = "evt-1",
        timestampMs = 1000L,
        packageName = "com.example.app",
        appLabel = "Example App",
        exceptionClass = "java.lang.NullPointerException",
        message = "Attempt to invoke virtual method on null reference",
        source = "uncaught",
    )

    private fun fakeAppItem(
        packageName: String,
        label: String,
        isSystem: Boolean = false,
        interceptEnabled: Boolean = false,
        updateTime: Long = 0L,
        installTime: Long = 0L,
    ): AppItem = AppItem(
        packageName = packageName,
        label = label,
        appInfo = ApplicationInfo().apply { this.packageName = packageName },
        isSystem = isSystem,
        interceptEnabled = interceptEnabled,
        updateTime = updateTime,
        installTime = installTime,
    )
}
