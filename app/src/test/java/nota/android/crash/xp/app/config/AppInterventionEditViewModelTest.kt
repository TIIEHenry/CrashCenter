package nota.android.crash.xp.app.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AppInterventionEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ManagedAppRepository
    private val packageName = "com.example.test"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AppInterventionEditViewModel {
        return AppInterventionEditViewModel(
            packageName,
            repository,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher,
        )
    }

    private fun captureSavedProfile(invocation: Int = 1): AppInterventionProfile {
        val captor = argumentCaptor<AppInterventionProfile>()
        verify(repository, org.mockito.kotlin.times(invocation)).saveProfile(any<String>(), captor.capture())
        return captor.allValues.last()
    }

    @Test
    fun `initial state is empty`() = runTest(testDispatcher) {
        whenever(repository.getProfile(packageName)).thenReturn(AppInterventionProfile.EMPTY)

        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(AppInterventionProfile.EMPTY, state.profile)
        assertNull(state.catchAllRule)
        assertFalse(state.saved)
    }

    @Test
    fun `init loads profile with catch-all rule`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true)
        whenever(repository.getProfile(packageName)).thenReturn(
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.catchAllRule)
        assertEquals(InterventionRuleType.CATCH_ALL, state.catchAllRule?.type)
        assertTrue(state.catchAllRule?.enabled == true)
        assertEquals(1, state.profile.rules.size)
    }

    @Test
    fun `toggleRuleEnabled disables catch-all`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true)
        whenever(repository.getProfile(packageName)).thenReturn(
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.catchAllRule?.enabled == true)

        viewModel.toggleRuleEnabled(false)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.catchAllRule?.enabled == false)
        assertTrue(viewModel.uiState.value.saved)
        val saved = captureSavedProfile()
        assertEquals(1, saved.rules.size)
        assertFalse(saved.rules[0].enabled)
    }

    @Test
    fun `toggleRuleEnabled enables catch-all`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = false)
        whenever(repository.getProfile(packageName)).thenReturn(
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.catchAllRule?.enabled == false)

        viewModel.toggleRuleEnabled(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.catchAllRule?.enabled == true)
        assertTrue(viewModel.uiState.value.saved)
        val saved = captureSavedProfile()
        assertEquals(1, saved.rules.size)
        assertTrue(saved.rules[0].enabled)
    }

    @Test
    fun `updateShowNotify sets value`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true)
        whenever(repository.getProfile(packageName)).thenReturn(
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.catchAllRule?.showNotify)

        viewModel.updateShowNotify(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.catchAllRule?.showNotify == true)
        assertTrue(viewModel.uiState.value.saved)
        val saved = captureSavedProfile()
        assertEquals(1, saved.rules.size)
        assertTrue(saved.rules[0].showNotify == true)
    }

    @Test
    fun `updateShowNotify clears value`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true).copy(showNotify = true)
        whenever(repository.getProfile(packageName)).thenReturn(
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.catchAllRule?.showNotify == true)

        viewModel.updateShowNotify(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.catchAllRule?.showNotify)
        val saved = captureSavedProfile()
        assertEquals(1, saved.rules.size)
        assertNull(saved.rules[0].showNotify)
    }

    @Test
    fun `updateCrashLogEnabled sets value`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true)
        whenever(repository.getProfile(packageName)).thenReturn(
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.catchAllRule?.crashLogEnabled)

        viewModel.updateCrashLogEnabled(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.catchAllRule?.crashLogEnabled == true)
        assertTrue(viewModel.uiState.value.saved)
        val saved = captureSavedProfile()
        assertEquals(1, saved.rules.size)
        assertTrue(saved.rules[0].crashLogEnabled == true)
    }

    @Test
    fun `saveProfile persists changes`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true)
        whenever(repository.getProfile(packageName)).thenReturn(
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleRuleEnabled(false)
        viewModel.updateShowNotify(true)
        viewModel.updateCrashLogEnabled(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        val saved = captureSavedProfile(invocation = 3)
        assertEquals(1, saved.rules.size)
        val rule = saved.rules[0]
        assertFalse(rule.enabled)
        assertTrue(rule.showNotify == true)
        assertTrue(rule.crashLogEnabled == true)
    }

    @Test
    fun `addCatchAllRule creates rule when none exists`() = runTest(testDispatcher) {
        whenever(repository.getProfile(packageName)).thenReturn(AppInterventionProfile.EMPTY)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.catchAllRule)

        viewModel.addCatchAllRule()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.catchAllRule)
        assertEquals(InterventionRuleType.CATCH_ALL, state.catchAllRule?.type)
        assertTrue(state.catchAllRule?.enabled == true)
        assertEquals(1, state.profile.rules.size)
        assertTrue(state.saved)
        val saved = captureSavedProfile()
        assertEquals(1, saved.rules.size)
        assertEquals(InterventionRuleType.CATCH_ALL, saved.rules[0].type)
        assertTrue(saved.rules[0].enabled)
    }

    @Test
    fun `deleteCatchAllRule removes rule`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true)
        whenever(repository.getProfile(packageName)).thenReturn(
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.catchAllRule)

        viewModel.deleteCatchAllRule()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.catchAllRule)
        assertEquals(AppInterventionProfile.EMPTY, state.profile)
        assertTrue(state.saved)
        verify(repository).saveProfile(packageName, AppInterventionProfile.EMPTY)
    }

    @Test
    fun `removeManagedApp removes package from repository`() = runTest(testDispatcher) {
        whenever(repository.getProfile(packageName)).thenReturn(AppInterventionProfile.EMPTY)

        val viewModel = createViewModel()
        viewModel.removeManagedApp()
        advanceUntilIdle()

        verify(repository).removeManagedPackage(packageName)
    }

    @Test
    fun `toggleRuleEnabled does nothing when no catch-all rule exists`() = runTest(testDispatcher) {
        whenever(repository.getProfile(packageName)).thenReturn(AppInterventionProfile.EMPTY)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.catchAllRule)

        viewModel.toggleRuleEnabled(true)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.catchAllRule)
        assertEquals(AppInterventionProfile.EMPTY, viewModel.uiState.value.profile)
    }

    @Test
    fun `updateShowNotify does nothing when no catch-all rule exists`() = runTest(testDispatcher) {
        whenever(repository.getProfile(packageName)).thenReturn(AppInterventionProfile.EMPTY)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateShowNotify(true)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.catchAllRule)
        assertEquals(AppInterventionProfile.EMPTY, viewModel.uiState.value.profile)
    }

    @Test
    fun `updateCrashLogEnabled does nothing when no catch-all rule exists`() = runTest(testDispatcher) {
        whenever(repository.getProfile(packageName)).thenReturn(AppInterventionProfile.EMPTY)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateCrashLogEnabled(true)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.catchAllRule)
        assertEquals(AppInterventionProfile.EMPTY, viewModel.uiState.value.profile)
    }
}
