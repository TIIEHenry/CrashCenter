package nota.android.crash.xp.app.config

import android.content.pm.ApplicationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nota.android.crash.xp.app.PackageVisibilityHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseConfigViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TestConfigViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TestConfigViewModel(CoroutineScope(testDispatcher))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── applyFilters ───

    @Test
    fun `applyFilters filters items correctly by query`() = runTest {
        viewModel.setApps(
            listOf(
                fakeItem("com.alpha", "Alpha"),
                fakeItem("com.beta", "Beta"),
                fakeItem("com.gamma", "Gamma"),
            )
        )
        viewModel.applyFilters(preserveSort = false)

        viewModel.setQuery("Alp")
        val state = viewModel.uiState.value

        assertEquals(1, state.visibleApps.size)
        assertEquals("Alpha", state.visibleApps[0].label)
    }

    @Test
    fun `applyFilters returns empty list when no items match query`() = runTest {
        viewModel.setApps(
            listOf(
                fakeItem("com.alpha", "Alpha"),
                fakeItem("com.beta", "Beta"),
            )
        )
        viewModel.applyFilters(preserveSort = false)

        viewModel.setQuery("NonExistent")
        val state = viewModel.uiState.value

        assertTrue(state.visibleApps.isEmpty())
    }

    @Test
    fun `applyFilters sorts items correctly`() = runTest {
        viewModel.setApps(
            listOf(
                fakeItem("com.gamma", "Gamma", updateTime = 300),
                fakeItem("com.alpha", "Alpha", updateTime = 100),
                fakeItem("com.beta", "Beta", updateTime = 200),
            )
        )
        viewModel.applyFilters(preserveSort = false)

        // Default sort is UPDATE_TIME_DESC
        var state = viewModel.uiState.value
        assertEquals("Gamma", state.visibleApps[0].label)
        assertEquals("Beta", state.visibleApps[1].label)
        assertEquals("Alpha", state.visibleApps[2].label)

        viewModel.setSortMode(SortMode.NAME_ASC)
        state = viewModel.uiState.value
        assertEquals("Alpha", state.visibleApps[0].label)
        assertEquals("Beta", state.visibleApps[1].label)
        assertEquals("Gamma", state.visibleApps[2].label)
    }

    @Test
    fun `applyFilters updates state with filtered list and empty message`() = runTest {
        viewModel.setApps(
            listOf(
                fakeItem("com.alpha", "Alpha"),
                fakeItem("com.beta", "Beta"),
            )
        )
        viewModel.applyFilters(preserveSort = false)

        assertEquals(2, viewModel.uiState.value.visibleApps.size)
        assertNull(viewModel.uiState.value.emptyMessage)

        viewModel.setQuery("NonExistent")
        assertEquals(0, viewModel.uiState.value.visibleApps.size)
        assertEquals("empty", viewModel.uiState.value.emptyMessage)
    }

    @Test
    fun `applyFilters with custom filter function filters correctly`() = runTest {
        viewModel.setCustomFilter { items -> items.filter { it.hookEnabled } }
        viewModel.setApps(
            listOf(
                fakeItem("com.a", "A", hookEnabled = true),
                fakeItem("com.b", "B", hookEnabled = false),
                fakeItem("com.c", "C", hookEnabled = true),
            )
        )
        viewModel.applyFilters(preserveSort = false)

        assertEquals(2, viewModel.uiState.value.visibleApps.size)
        assertEquals("A", viewModel.uiState.value.visibleApps[0].label)
        assertEquals("C", viewModel.uiState.value.visibleApps[1].label)
    }

    // ─── loadWithState ───

    @Test
    fun `loadWithState sets loading true then false on success`() = runTest {
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.loadApps()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadWithState sets loading false and errorMessage on exception`() = runTest {
        viewModel.triggerFailingLoad()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Test failure", viewModel.uiState.value.errorMessage)
    }

    // ─── emitState ───

    @Test
    fun `emitState applies state transformation correctly`() = runTest {
        assertEquals("", viewModel.uiState.value.query)
        assertFalse(viewModel.uiState.value.scopeMode)

        viewModel.setQuery("test query")
        assertEquals("test query", viewModel.uiState.value.query)

        viewModel.setScopeMode(true)
        assertTrue(viewModel.uiState.value.scopeMode)
    }

    // ─── clearError ───

    @Test
    fun `clearError resets errorMessage to null`() = runTest {
        viewModel.triggerFailingLoad()
        advanceUntilIdle()
        assertEquals("Test failure", viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ─── Test subclass and helpers ───

    private class TestConfigViewModel(scope: CoroutineScope) : BaseConfigViewModel(
        scope = scope,
        isLegacyMode = true,
        scopeMode = false,
        handleSystem = false,
        showSystemUi = false,
        packageVisibility = PackageVisibilityHelper.Status(
            hasFullVisibility = true,
            permissionGranted = true,
            needsUserAction = false,
        ),
    ) {
        private var testItems: List<AppItem> = emptyList()
        private var customFilter: ((List<AppItem>) -> List<AppItem>)? = null

        fun setApps(items: List<AppItem>) {
            testItems = items
            emitState { copy(allApps = items) }
        }

        fun setCustomFilter(filter: (List<AppItem>) -> List<AppItem>) {
            customFilter = filter
        }

        override fun loadApps(forceReload: Boolean) {
            loadWithState {
                emitState { copy(isLoading = false, allApps = testItems) }
                applyFilters(preserveSort = false)
            }
        }

        fun triggerFailingLoad() {
            loadWithState { throw RuntimeException("Test failure") }
        }

        override fun applyFilters(preserveSort: Boolean) = applyFilters(
            preserveSort = preserveSort,
            filter = { state ->
                val base = AppFilterEngine.filterLegacyApps(
                    state.allApps, state.query, state.hookFilter, state.showSystemUi,
                )
                customFilter?.invoke(base) ?: base
            },
            emptyMessage = { filtered, _ ->
                if (filtered.isEmpty()) "empty" else null
            },
            setState = { filtered -> copy(visibleApps = filtered) },
            sourceExtractor = { it.allApps },
        )
    }

    private fun fakeItem(
        packageName: String,
        label: String,
        hookEnabled: Boolean = false,
        isSystem: Boolean = false,
        updateTime: Long = 0L,
        installTime: Long = 0L,
    ): AppItem = AppItem(
        label = label,
        appInfo = ApplicationInfo().apply {
            this.packageName = packageName
            flags = if (isSystem) ApplicationInfo.FLAG_SYSTEM else 0
        },
        hookEnabled = hookEnabled,
        packageName = packageName,
        isSystem = isSystem,
        updateTime = updateTime,
        installTime = installTime,
    )
}
