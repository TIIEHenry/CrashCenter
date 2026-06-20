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

@OptIn(ExperimentalCoroutinesApi::class)
class AppInterventionEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAppRepository
    private val packageName = "com.example.test"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeAppRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AppInterventionEditViewModel {
        return AppInterventionEditViewModel(packageName, repository, ioDispatcher = testDispatcher)
    }

    @Test
    fun `initial state is empty`() = runTest(testDispatcher) {
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
        repository.setProfile(
            packageName,
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
        repository.setProfile(
            packageName,
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.catchAllRule?.enabled == true)

        viewModel.toggleRuleEnabled(false)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.catchAllRule?.enabled == false)
        assertTrue(viewModel.uiState.value.saved)
        val savedProfile = repository.getProfile(packageName)
        assertTrue(savedProfile.rules.first().enabled == false)
    }

    @Test
    fun `toggleRuleEnabled enables catch-all`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = false)
        repository.setProfile(
            packageName,
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.catchAllRule?.enabled == false)

        viewModel.toggleRuleEnabled(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.catchAllRule?.enabled == true)
        assertTrue(viewModel.uiState.value.saved)
        val savedProfile = repository.getProfile(packageName)
        assertTrue(savedProfile.rules.first().enabled == true)
    }

    @Test
    fun `updateShowNotify sets value`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true)
        repository.setProfile(
            packageName,
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.catchAllRule?.showNotify)

        viewModel.updateShowNotify(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.catchAllRule?.showNotify == true)
        assertTrue(viewModel.uiState.value.saved)
        val savedProfile = repository.getProfile(packageName)
        assertTrue(savedProfile.rules.first().showNotify == true)
    }

    @Test
    fun `updateShowNotify clears value`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true).copy(showNotify = true)
        repository.setProfile(
            packageName,
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.catchAllRule?.showNotify == true)

        viewModel.updateShowNotify(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.catchAllRule?.showNotify)
        val savedProfile = repository.getProfile(packageName)
        assertNull(savedProfile.rules.first().showNotify)
    }

    @Test
    fun `updateCrashLogEnabled sets value`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true)
        repository.setProfile(
            packageName,
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.catchAllRule?.crashLogEnabled)

        viewModel.updateCrashLogEnabled(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.catchAllRule?.crashLogEnabled == true)
        assertTrue(viewModel.uiState.value.saved)
        val savedProfile = repository.getProfile(packageName)
        assertTrue(savedProfile.rules.first().crashLogEnabled == true)
    }

    @Test
    fun `saveProfile persists changes`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true)
        repository.setProfile(
            packageName,
            AppInterventionProfile(rules = listOf(catchAll)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleRuleEnabled(false)
        viewModel.updateShowNotify(true)
        viewModel.updateCrashLogEnabled(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        val savedProfile = repository.getProfile(packageName)
        assertEquals(1, savedProfile.rules.size)
        val rule = savedProfile.rules.first()
        assertFalse(rule.enabled)
        assertTrue(rule.showNotify == true)
        assertTrue(rule.crashLogEnabled == true)
    }

    @Test
    fun `addCatchAllRule creates rule when none exists`() = runTest(testDispatcher) {
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
        val savedProfile = repository.getProfile(packageName)
        assertEquals(1, savedProfile.rules.size)
        assertEquals(InterventionRuleType.CATCH_ALL, savedProfile.rules.first().type)
    }

    @Test
    fun `deleteCatchAllRule removes rule`() = runTest(testDispatcher) {
        val catchAll = InterventionRule.defaultCatchAll(enabled = true)
        repository.setProfile(
            packageName,
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
        val savedProfile = repository.getProfile(packageName)
        assertTrue(savedProfile.rules.isEmpty())
    }

    @Test
    fun `removeManagedApp removes package from repository`() = runTest(testDispatcher) {
        repository.legacyMode = false
        repository.addManagedPackages(listOf(packageName))
        assertTrue(repository.readManagedPackageNames()?.contains(packageName) == true)

        val viewModel = createViewModel()
        viewModel.removeManagedApp()
        advanceUntilIdle()

        assertFalse(repository.readManagedPackageNames()?.contains(packageName) == true)
    }

    @Test
    fun `toggleRuleEnabled does nothing when no catch-all rule exists`() = runTest(testDispatcher) {
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
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateShowNotify(true)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.catchAllRule)
        assertEquals(AppInterventionProfile.EMPTY, viewModel.uiState.value.profile)
    }

    @Test
    fun `updateCrashLogEnabled does nothing when no catch-all rule exists`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateCrashLogEnabled(true)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.catchAllRule)
        assertEquals(AppInterventionProfile.EMPTY, viewModel.uiState.value.profile)
    }
}
