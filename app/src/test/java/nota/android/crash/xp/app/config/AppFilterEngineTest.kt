package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.app.data.CrashFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterEngineTest {

    // ─── filterLegacyApps ───

    @Test
    fun `filterLegacyApps with empty list returns empty`() {
        val result = AppFilterEngine.filterLegacyApps(
            apps = emptyList(),
            query = "anything",
            hookFilter = HookFilter.ALL,
            showSystemUi = true,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterLegacyApps with empty query and ALL hookFilter shows matching system apps`() {
        val apps = listOf(
            fakeAppItem("com.a", "App A", isSystem = false, hookEnabled = true),
            fakeAppItem("com.b", "App B", isSystem = true, hookEnabled = false),
        )
        val result = AppFilterEngine.filterLegacyApps(
            apps = apps,
            query = "",
            hookFilter = HookFilter.ALL,
            showSystemUi = true,
        )
        assertEquals(1, result.size)
        assertEquals("App B", result[0].name)
    }

    @Test
    fun `filterLegacyApps filters by system app when showSystemUi false`() {
        val apps = listOf(
            fakeAppItem("com.a", "App A", isSystem = false),
            fakeAppItem("com.sys", "System", isSystem = true),
        )
        val result = AppFilterEngine.filterLegacyApps(
            apps = apps,
            query = "",
            hookFilter = HookFilter.ALL,
            showSystemUi = false,
        )
        assertEquals(1, result.size)
        assertEquals("App A", result[0].name)
    }

    @Test
    fun `filterLegacyApps filters by system app when showSystemUi true`() {
        val apps = listOf(
            fakeAppItem("com.a", "App A", isSystem = false),
            fakeAppItem("com.sys", "System", isSystem = true),
        )
        val result = AppFilterEngine.filterLegacyApps(
            apps = apps,
            query = "",
            hookFilter = HookFilter.ALL,
            showSystemUi = true,
        )
        assertEquals(1, result.size)
        assertEquals("System", result[0].name)
    }

    @Test
    fun `filterLegacyApps filters by hookFilter ON`() {
        val apps = listOf(
            fakeAppItem("com.a", "App A", hookEnabled = true),
            fakeAppItem("com.b", "App B", hookEnabled = false),
        )
        val result = AppFilterEngine.filterLegacyApps(
            apps = apps,
            query = "",
            hookFilter = HookFilter.ON,
            showSystemUi = false,
        )
        assertEquals(1, result.size)
        assertEquals("App A", result[0].name)
    }

    @Test
    fun `filterLegacyApps filters by hookFilter OFF`() {
        val apps = listOf(
            fakeAppItem("com.a", "App A", hookEnabled = true),
            fakeAppItem("com.b", "App B", hookEnabled = false),
        )
        val result = AppFilterEngine.filterLegacyApps(
            apps = apps,
            query = "",
            hookFilter = HookFilter.OFF,
            showSystemUi = false,
        )
        assertEquals(1, result.size)
        assertEquals("App B", result[0].name)
    }

    @Test
    fun `filterLegacyApps filters by query name case insensitive`() {
        val apps = listOf(
            fakeAppItem("com.alpha", "Alpha Browser"),
            fakeAppItem("com.beta", "Beta Mail"),
        )
        val result = AppFilterEngine.filterLegacyApps(
            apps = apps,
            query = "alpha",
            hookFilter = HookFilter.ALL,
            showSystemUi = false,
        )
        assertEquals(1, result.size)
        assertEquals("Alpha Browser", result[0].name)
    }

    @Test
    fun `filterLegacyApps filters by query package name`() {
        val apps = listOf(
            fakeAppItem("com.alpha.app", "Browser"),
            fakeAppItem("com.beta.app", "Mail"),
        )
        val result = AppFilterEngine.filterLegacyApps(
            apps = apps,
            query = "beta",
            hookFilter = HookFilter.ALL,
            showSystemUi = false,
        )
        assertEquals(1, result.size)
        assertEquals("Mail", result[0].name)
    }

    @Test
    fun `filterLegacyApps no match returns empty`() {
        val apps = listOf(
            fakeAppItem("com.a", "App A"),
        )
        val result = AppFilterEngine.filterLegacyApps(
            apps = apps,
            query = "zzz",
            hookFilter = HookFilter.ALL,
            showSystemUi = false,
        )
        assertTrue(result.isEmpty())
    }

    // ─── filterManagedApps ───

    @Test
    fun `filterManagedApps with empty list returns empty`() {
        val result = AppFilterEngine.filterManagedApps(
            apps = emptyList(),
            query = "anything",
            managedFilter = ManagedFilter.ALL,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterManagedApps with empty query and ALL filter shows all`() {
        val apps = listOf(
            fakeManagedApp("com.a", "App A", InterventionStatus.ENABLED),
            fakeManagedApp("com.b", "App B", InterventionStatus.PENDING),
        )
        val result = AppFilterEngine.filterManagedApps(
            apps = apps,
            query = "",
            managedFilter = ManagedFilter.ALL,
        )
        assertEquals(2, result.size)
    }

    @Test
    fun `filterManagedApps filters by ENABLED status`() {
        val apps = listOf(
            fakeManagedApp("com.a", "App A", InterventionStatus.ENABLED),
            fakeManagedApp("com.b", "App B", InterventionStatus.PENDING),
        )
        val result = AppFilterEngine.filterManagedApps(
            apps = apps,
            query = "",
            managedFilter = ManagedFilter.ENABLED,
        )
        assertEquals(1, result.size)
        assertEquals("App A", result[0].label)
    }

    @Test
    fun `filterManagedApps filters by PENDING status`() {
        val apps = listOf(
            fakeManagedApp("com.a", "App A", InterventionStatus.ENABLED),
            fakeManagedApp("com.b", "App B", InterventionStatus.PENDING),
        )
        val result = AppFilterEngine.filterManagedApps(
            apps = apps,
            query = "",
            managedFilter = ManagedFilter.PENDING,
        )
        assertEquals(1, result.size)
        assertEquals("App B", result[0].label)
    }

    @Test
    fun `filterManagedApps filters by query label case insensitive`() {
        val apps = listOf(
            fakeManagedApp("com.alpha", "Alpha Browser", InterventionStatus.ENABLED),
            fakeManagedApp("com.beta", "Beta Mail", InterventionStatus.ENABLED),
        )
        val result = AppFilterEngine.filterManagedApps(
            apps = apps,
            query = "ALPHA",
            managedFilter = ManagedFilter.ALL,
        )
        assertEquals(1, result.size)
        assertEquals("Alpha Browser", result[0].label)
    }

    @Test
    fun `filterManagedApps filters by query package name`() {
        val apps = listOf(
            fakeManagedApp("com.alpha.app", "Browser", InterventionStatus.ENABLED),
            fakeManagedApp("com.beta.app", "Mail", InterventionStatus.ENABLED),
        )
        val result = AppFilterEngine.filterManagedApps(
            apps = apps,
            query = "beta",
            managedFilter = ManagedFilter.ALL,
        )
        assertEquals(1, result.size)
        assertEquals("Mail", result[0].label)
    }

    @Test
    fun `filterManagedApps no match returns empty`() {
        val apps = listOf(
            fakeManagedApp("com.a", "App A", InterventionStatus.ENABLED),
        )
        val result = AppFilterEngine.filterManagedApps(
            apps = apps,
            query = "zzz",
            managedFilter = ManagedFilter.ALL,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterManagedApps status filter applied before query filter`() {
        val apps = listOf(
            fakeManagedApp("com.match", "Match", InterventionStatus.PENDING),
            fakeManagedApp("com.nomatch", "Match Too", InterventionStatus.ENABLED),
        )
        val result = AppFilterEngine.filterManagedApps(
            apps = apps,
            query = "Match",
            managedFilter = ManagedFilter.PENDING,
        )
        assertEquals(1, result.size)
        assertEquals("com.match", result[0].packageName)
    }

    // ─── filterByQuery ───

    @Test
    fun `filterByQuery with empty list returns empty`() {
        val result = AppFilterEngine.filterByQuery(
            items = emptyList<String>(),
            query = "anything",
            labelExtractor = { it },
            packageNameExtractor = { it },
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterByQuery with empty query returns all items`() {
        val items = listOf("Alpha", "Beta", "Gamma")
        val result = AppFilterEngine.filterByQuery(
            items = items,
            query = "",
            labelExtractor = { it },
            packageNameExtractor = { it },
        )
        assertEquals(3, result.size)
    }

    @Test
    fun `filterByQuery matches label case insensitive`() {
        val items = listOf("Alpha", "Beta", "Gamma")
        val result = AppFilterEngine.filterByQuery(
            items = items,
            query = "alpha",
            labelExtractor = { it },
            packageNameExtractor = { "unused" },
        )
        assertEquals(listOf("Alpha"), result)
    }

    @Test
    fun `filterByQuery matches packageName when label does not match`() {
        val items = listOf("Alpha", "Beta")
        val result = AppFilterEngine.filterByQuery(
            items = items,
            query = "beta",
            labelExtractor = { "fixed" },
            packageNameExtractor = { it },
        )
        assertEquals(listOf("Beta"), result)
    }

    @Test
    fun `filterByQuery no match returns empty`() {
        val items = listOf("Alpha", "Beta")
        val result = AppFilterEngine.filterByQuery(
            items = items,
            query = "zzz",
            labelExtractor = { it },
            packageNameExtractor = { it },
        )
        assertTrue(result.isEmpty())
    }

    // ─── sort ───

    @Test
    fun `sort NAME_ASC sorts ascending`() {
        val list = mutableListOf("Charlie", "Alpha", "Beta")
        AppFilterEngine.sort(
            list = list,
            mode = SortMode.NAME_ASC,
            nameExtractor = { it },
            installTimeExtractor = { 0L },
            updateTimeExtractor = { 0L },
        )
        assertEquals(listOf("Alpha", "Beta", "Charlie"), list)
    }

    @Test
    fun `sort NAME_DESC sorts descending`() {
        val list = mutableListOf("Charlie", "Alpha", "Beta")
        AppFilterEngine.sort(
            list = list,
            mode = SortMode.NAME_DESC,
            nameExtractor = { it },
            installTimeExtractor = { 0L },
            updateTimeExtractor = { 0L },
        )
        assertEquals(listOf("Charlie", "Beta", "Alpha"), list)
    }

    @Test
    fun `sort INSTALL_TIME_ASC sorts ascending`() {
        val list = mutableListOf(3L, 1L, 2L)
        AppFilterEngine.sort(
            list = list,
            mode = SortMode.INSTALL_TIME_ASC,
            nameExtractor = { "" },
            installTimeExtractor = { it },
            updateTimeExtractor = { 0L },
        )
        assertEquals(listOf(1L, 2L, 3L), list)
    }

    @Test
    fun `sort INSTALL_TIME_DESC sorts descending`() {
        val list = mutableListOf(3L, 1L, 2L)
        AppFilterEngine.sort(
            list = list,
            mode = SortMode.INSTALL_TIME_DESC,
            nameExtractor = { "" },
            installTimeExtractor = { it },
            updateTimeExtractor = { 0L },
        )
        assertEquals(listOf(3L, 2L, 1L), list)
    }

    @Test
    fun `sort UPDATE_TIME_ASC sorts ascending`() {
        val list = mutableListOf(3L, 1L, 2L)
        AppFilterEngine.sort(
            list = list,
            mode = SortMode.UPDATE_TIME_ASC,
            nameExtractor = { "" },
            installTimeExtractor = { 0L },
            updateTimeExtractor = { it },
        )
        assertEquals(listOf(1L, 2L, 3L), list)
    }

    @Test
    fun `sort UPDATE_TIME_DESC sorts descending`() {
        val list = mutableListOf(3L, 1L, 2L)
        AppFilterEngine.sort(
            list = list,
            mode = SortMode.UPDATE_TIME_DESC,
            nameExtractor = { "" },
            installTimeExtractor = { 0L },
            updateTimeExtractor = { it },
        )
        assertEquals(listOf(3L, 2L, 1L), list)
    }

    @Test
    fun `sort with empty list does not throw`() {
        val list = mutableListOf<String>()
        AppFilterEngine.sort(
            list = list,
            mode = SortMode.NAME_ASC,
            nameExtractor = { it },
            installTimeExtractor = { 0L },
            updateTimeExtractor = { 0L },
        )
        assertTrue(list.isEmpty())
    }

    @Test
    fun `sort with single element is stable`() {
        val list = mutableListOf("Only")
        AppFilterEngine.sort(
            list = list,
            mode = SortMode.NAME_DESC,
            nameExtractor = { it },
            installTimeExtractor = { 0L },
            updateTimeExtractor = { 0L },
        )
        assertEquals(listOf("Only"), list)
    }

    // ─── matchesCrashEvent ───

    private val sampleEvent = CrashEvent(
        id = "evt-1",
        timestampMs = 1000L,
        packageName = "com.example.app",
        appLabel = "Example App",
        exceptionClass = "java.lang.NullPointerException",
        message = "Attempt to invoke virtual method on null reference",
        source = "uncaught",
    )

    @Test
    fun `matchesCrashEvent empty filter matches any event`() {
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, CrashFilter()))
    }

    @Test
    fun `matchesCrashEvent packageName match`() {
        val filter = CrashFilter(packageName = "com.example.app")
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent packageName mismatch`() {
        val filter = CrashFilter(packageName = "com.other.app")
        assertFalse(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent source match`() {
        val filter = CrashFilter(source = "uncaught")
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent source mismatch`() {
        val filter = CrashFilter(source = "anr")
        assertFalse(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent sinceMs excludes older events`() {
        val filter = CrashFilter(sinceMs = 1001L)
        assertFalse(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent sinceMs includes exact boundary`() {
        val filter = CrashFilter(sinceMs = 1000L)
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent untilMs excludes newer events`() {
        val filter = CrashFilter(untilMs = 999L)
        assertFalse(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent untilMs includes exact boundary`() {
        val filter = CrashFilter(untilMs = 1000L)
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent query matches appLabel case insensitive`() {
        val filter = CrashFilter(query = "EXAMPLE")
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent query matches packageName`() {
        val filter = CrashFilter(query = "com.example")
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent query matches exceptionClass`() {
        val filter = CrashFilter(query = "NullPointer")
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent query matches message`() {
        val filter = CrashFilter(query = "null reference")
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent query with no match returns false`() {
        val filter = CrashFilter(query = "zzzzz")
        assertFalse(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent query blank is ignored`() {
        val filter = CrashFilter(query = "   ")
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent full filter all match`() {
        val filter = CrashFilter(
            packageName = "com.example.app",
            source = "uncaught",
            sinceMs = 500L,
            untilMs = 1500L,
            query = "null",
        )
        assertTrue(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent full filter one field mismatch`() {
        val filter = CrashFilter(
            packageName = "com.example.app",
            source = "anr",
            sinceMs = 500L,
            untilMs = 1500L,
            query = "null",
        )
        assertFalse(AppFilterEngine.matchesCrashEvent(sampleEvent, filter))
    }

    @Test
    fun `matchesCrashEvent null appLabel and message still matches packageName`() {
        val event = CrashEvent(
            id = "evt-2",
            timestampMs = 2000L,
            packageName = "com.minimal.app",
            appLabel = null,
            exceptionClass = "java.lang.Exception",
            message = null,
        )
        val filter = CrashFilter(query = "minimal")
        assertTrue(AppFilterEngine.matchesCrashEvent(event, filter))
    }

    // ─── Helpers ───

    private fun fakeAppItem(
        packageName: String,
        name: String,
        isSystem: Boolean = false,
        hookEnabled: Boolean = false,
        updateTime: Long = 0L,
        installTime: Long = 0L,
    ): AppItem = AppItem(
        name = name,
        appInfo = ApplicationInfo().apply {
            this.packageName = packageName
        },
        hookEnabled = hookEnabled,
        packageName = packageName,
        isSystemApp = isSystem,
        updateTime = updateTime,
        installTime = installTime,
    )

    private fun fakeManagedApp(
        packageName: String,
        label: String,
        status: InterventionStatus,
    ): ManagedApp = ManagedApp(
        packageName = packageName,
        label = label,
        appInfo = ApplicationInfo().apply {
            this.packageName = packageName
        },
        isSystem = false,
        interventionStatus = status,
        switchChecked = false,
        enabledRuleCount = 0,
        summary = null,
    )
}
