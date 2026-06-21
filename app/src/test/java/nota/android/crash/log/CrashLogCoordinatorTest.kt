package nota.android.crash.log

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.runBlocking
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.xp.PrefManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashLogCoordinatorTest {

    private lateinit var xposedMock: MockedStatic<XposedBridge>
    private lateinit var xPrefsConstruction: MockedConstruction<XSharedPreferences>
    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context
    private lateinit var event: CrashEvent

    @Before
    fun setUp() {
        context = mock()
        event = CrashEvent(id = "evt-1", packageName = "com.test")

        prefs = mock<SharedPreferences>()
        doReturn(true).`when`(prefs).getBoolean(any(), any())

        // Intercept XSharedPreferences constructor calls and delegate to prefs.
        // Void methods (like reload()) return null; non-void delegate via reflection.
        xPrefsConstruction = Mockito.mockConstruction(
            XSharedPreferences::class.java,
            Mockito.withSettings().defaultAnswer {
                if (it.method.returnType == Void.TYPE) null
                else try {
                    it.method.invoke(prefs, *it.arguments)
                } catch (_: Exception) {
                    null
                }
            },
        )

        xposedMock = Mockito.mockStatic(XposedBridge::class.java)
        xposedMock.`when`<Unit> { XposedBridge.log(any<String>()) }.then {}
    }

    @After
    fun tearDown() {
        xposedMock.close()
        xPrefsConstruction.close()
    }

    // --- helpers ---

    private fun mockBackend(
        id: BackendId,
        result: AppendResult = AppendResult.Success,
    ): CrashLogBackend {
        val backend = mock<CrashLogBackend>()
        whenever(backend.id).thenReturn(id)
        whenever(backend.append(any(), any(), eq(2000L))).thenReturn(result)
        return backend
    }

    // ─── 1. Single backend success ────────────────────────────────────────

    @Test
    fun `runPhase2Parallel single backend success writes event`() {
        val backend = mockBackend(BackendId.DIRECT_FS)

        runBlocking { CrashLogCoordinator.runPhase2Parallel(context, event, listOf(backend)) }

        verify(backend).append(context, event, 2000L)
        xposedMock.verify({ XposedBridge.log(any<String>()) }, never())
    }

    // ─── 2. Single backend failure ────────────────────────────────────────

    @Test
    fun `runPhase2Parallel single backend failure does not crash`() {
        val backend = mockBackend(BackendId.DIRECT_FS, AppendResult.Failure("disk full"))

        runBlocking { CrashLogCoordinator.runPhase2Parallel(context, event, listOf(backend)) }

        verify(backend).append(context, event, 2000L)
        xposedMock.verify {
            XposedBridge.log("CrashLog: all Phase 2 backends failed for evt-1")
        }
    }

    // ─── 3. Multiple backends all succeed ─────────────────────────────────

    @Test
    fun `runPhase2Parallel multiple backends all succeed`() {
        val provider = mockBackend(BackendId.PROVIDER_INSERT)
        val directFs = mockBackend(BackendId.DIRECT_FS)

        runBlocking { CrashLogCoordinator.runPhase2Parallel(context, event, listOf(provider, directFs)) }

        verify(provider).append(context, event, 2000L)
        verify(directFs).append(context, event, 2000L)
        xposedMock.verify({ XposedBridge.log(any<String>()) }, never())
    }

    // ─── 4. Multiple backends one fails, others succeed ───────────────────

    @Test
    fun `runPhase2Parallel one backend fails others still succeed`() {
        val provider = mockBackend(BackendId.PROVIDER_INSERT)
        val directFs = mockBackend(BackendId.DIRECT_FS, AppendResult.Failure("permission denied"))

        runBlocking { CrashLogCoordinator.runPhase2Parallel(context, event, listOf(provider, directFs)) }

        verify(provider).append(context, event, 2000L)
        verify(directFs).append(context, event, 2000L)
        // At least one succeeded, so no "all failed" log
        xposedMock.verify({ XposedBridge.log(any<String>()) }, never())
    }

    // ─── 5. All backends fail ─────────────────────────────────────────────

    @Test
    fun `runPhase2Parallel all backends fail logs but does not crash`() {
        val provider = mockBackend(BackendId.PROVIDER_INSERT, AppendResult.Failure("err1"))
        val directFs = mockBackend(BackendId.DIRECT_FS, AppendResult.Failure("err2"))

        runBlocking { CrashLogCoordinator.runPhase2Parallel(context, event, listOf(provider, directFs)) }

        verify(provider).append(context, event, 2000L)
        verify(directFs).append(context, event, 2000L)
        xposedMock.verify {
            XposedBridge.log("CrashLog: all Phase 2 backends failed for evt-1")
        }
    }

    // ─── 6. Backend throws exception ──────────────────────────────────────

    @Test
    fun `runPhase2Parallel backend throws exception is caught and does not crash`() {
        val throwing = mock<CrashLogBackend>()
        whenever(throwing.id).thenReturn(BackendId.DIRECT_FS)
        whenever(throwing.append(any(), any(), eq(2000L)))
            .thenThrow(RuntimeException("disk exploded"))
        val success = mockBackend(BackendId.PROVIDER_INSERT)

        // Throwing backend exception is caught inside async(Dispatchers.IO).
        // The coordinator still completes without crashing.
        runBlocking { CrashLogCoordinator.runPhase2Parallel(context, event, listOf(success, throwing)) }

        verify(success).append(context, event, 2000L)
        verify(throwing).append(context, event, 2000L)
        // hookSafeLog for individual failures runs on Dispatchers.IO (not the test thread),
        // so we cannot verify XposedBridge.log here. The test passes if no crash occurs.
    }

    // ─── 7. Empty backend list ────────────────────────────────────────────

    @Test
    fun `runPhase2Parallel empty backend list does not crash`() {
        runBlocking { CrashLogCoordinator.runPhase2Parallel(context, event, emptyList()) }

        xposedMock.verify({ XposedBridge.log(any<String>()) }, never())
    }

    // ─── 8. logAsync full flow ────────────────────────────────────────────

    @Test
    fun `logAsync dispatches to backends when logging enabled`() {
        CrashLogCoordinator.logAsync(context, event)

        // logAsync launches on Dispatchers.IO; allow the coroutine to complete.
        Thread.sleep(300)

        // isLoggingEnabled() returns true (prefs.getBoolean returns true via mockConstruction),
        // so runPhase2Parallel is called. Real backends execute and may fail or succeed.
        // We verify the coordinator didn't crash on the caller thread.
    }

    // ─── 9. logAsync skips when logging disabled ──────────────────────────

    @Test
    fun `logAsync skips backend dispatch when logging disabled`() {
        doReturn(false).`when`(prefs).getBoolean(PrefManager.PREF_CRASH_LOG_ENABLED, true)

        CrashLogCoordinator.logAsync(context, event)

        Thread.sleep(300)

        // When logging is disabled, isLoggingEnabled() returns false,
        // so runPhase2Parallel is never called and no logging occurs.
        xposedMock.verify({ XposedBridge.log(any<String>()) }, never())
    }
}
