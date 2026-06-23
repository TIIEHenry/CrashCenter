package nota.android.crash.capture

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import de.robv.android.xposed.XposedBridge
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.feedback.CrashFeedbackFacade
import nota.android.crash.log.CrashLogCoordinator
import nota.android.crash.xp.ScopeDecision
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class CrashCapturePipelineTest {

    private lateinit var app: Application
    private lateinit var mockCoordinator: CrashLogCoordinator
    private lateinit var mockFeedback: CrashFeedbackFacade
    private lateinit var xposedBridgeMock: MockedStatic<XposedBridge>
    private lateinit var appInfo: ApplicationInfo
    private lateinit var decision: ScopeDecision

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        mockCoordinator = mock(CrashLogCoordinator::class.java)
        mockFeedback = mock(CrashFeedbackFacade::class.java)
        xposedBridgeMock = Mockito.mockStatic(XposedBridge::class.java)

        appInfo = ApplicationInfo().apply { packageName = app.packageName }

        decision = ScopeDecision(shouldInstall = true, shouldIntercept = true, showNotify = true)

        CrashCapturePipeline.testCoordinator = mockCoordinator
        CrashCapturePipeline.testFeedback = mockFeedback
    }

    @After
    fun tearDown() {
        CrashCapturePipeline.testCoordinator = null
        CrashCapturePipeline.testFeedback = null
        xposedBridgeMock.close()
    }

    // --- Happy path ---

    @Test
    fun `happy path calls coordinator and feedback`() {
        val throwable = RuntimeException("boom")

        CrashCapturePipeline.onException(
            application = app,
            packageName = "com.example.app",
            appInfo = appInfo,
            throwable = throwable,
            source = "uncaught",
            decision = decision,
        )

        verify(mockCoordinator).logAsync(eq<Application>(app), any<CrashEvent>())
        verify(mockFeedback).show(
            eq<Application>(app),
            eq<String>("com.example.app"),
            eq<ApplicationInfo>(appInfo),
            eq<Throwable>(throwable),
            any<String>(),
            eq<Boolean>(true),
        )
    }

    // --- Coordinator failure ---

    @Test
    fun `coordinator throws still calls feedback facade`() {
        val throwable = RuntimeException("crash")
        val coordException = RuntimeException("coord failed")
        Mockito.doThrow(coordException).`when`(mockCoordinator)
            .logAsync(eq<Application>(app), any<CrashEvent>())

        CrashCapturePipeline.onException(
            application = app,
            packageName = "com.example.app",
            appInfo = appInfo,
            throwable = throwable,
            source = "uncaught",
            decision = decision,
        )

        verify(mockFeedback).show(
            eq<Application>(app),
            eq<String>("com.example.app"),
            eq<ApplicationInfo>(appInfo),
            eq<Throwable>(throwable),
            any<String>(),
            eq<Boolean>(true),
        )
    }

    // --- Feedback failure ---

    @Test
    fun `feedback facade throws does not propagate`() {
        val throwable = RuntimeException("crash")
        val feedbackException = RuntimeException("notify failed")
        Mockito.doThrow(feedbackException).`when`(mockFeedback).show(
            eq<Application>(app),
            any<String>(),
            any<ApplicationInfo>(),
            any<Throwable>(),
            any<String>(),
            any<Boolean>(),
        )

        try {
            CrashCapturePipeline.onException(
                application = app,
                packageName = "com.example.app",
                appInfo = appInfo,
                throwable = throwable,
                source = "uncaught",
                decision = decision,
            )
        } catch (e: Throwable) {
            fail("Exception should not propagate: ${e.message}")
        }

        verify(mockCoordinator).logAsync(eq<Application>(app), any<CrashEvent>())
    }

    // --- Unique event IDs ---

    @Test
    fun `event ID is unique per call`() {
        val events = mutableListOf<CrashEvent>()
        Mockito.doAnswer { invocation ->
            events.add(invocation.getArgument<CrashEvent>(1))
            null
        }.`when`(mockCoordinator).logAsync(eq<Application>(app), any<CrashEvent>())

        repeat(2) {
            CrashCapturePipeline.onException(
                application = app,
                packageName = "com.example.app",
                appInfo = appInfo,
                throwable = RuntimeException("err $it"),
                source = "test",
                decision = decision,
            )
        }

        assertEquals(2, events.size)
        assertNotEquals(events[0].id, events[1].id)
        assertTrue(events[0].id.isNotEmpty())
        assertTrue(events[1].id.isNotEmpty())
    }

    // --- Per-app crashLogEnabled gate ---

    @Test
    fun `crashLogEnabled false skips coordinator but still calls feedback`() {
        val disabledDecision = ScopeDecision(
            shouldInstall = true,
            shouldIntercept = true,
            showNotify = true,
            crashLogEnabled = false,
        )
        val throwable = RuntimeException("boom")

        CrashCapturePipeline.onException(
            application = app,
            packageName = "com.example.app",
            appInfo = appInfo,
            throwable = throwable,
            source = "uncaught",
            decision = disabledDecision,
        )

        verify(mockCoordinator, never()).logAsync(any<Application>(), any<CrashEvent>())
        verify(mockCoordinator, never()).logSync(any<Application>(), any<CrashEvent>())
        verify(mockFeedback).show(
            eq<Application>(app),
            eq<String>("com.example.app"),
            eq<ApplicationInfo>(appInfo),
            eq<Throwable>(throwable),
            any<String>(),
            eq<Boolean>(true),
        )
    }

    // --- Field population ---

    @Test
    fun `event fields are populated correctly`() {
        val events = mutableListOf<CrashEvent>()
        Mockito.doAnswer { invocation ->
            events.add(invocation.getArgument<CrashEvent>(1))
            null
        }.`when`(mockCoordinator).logAsync(eq<Application>(app), any<CrashEvent>())

        val throwable = IllegalStateException("illegal state")

        CrashCapturePipeline.onException(
            application = app,
            packageName = "com.example.app",
            appInfo = appInfo,
            throwable = throwable,
            source = "uncaught",
            decision = decision,
        )

        val event = events.single()
        assertEquals("com.example.app", event.packageName)
        assertEquals("java.lang.IllegalStateException", event.exceptionClass)
        assertEquals("illegal state", event.message)
        assertEquals("uncaught", event.source)
        assertNotNull(event.id)
        assertTrue(event.id.isNotEmpty())
        assertTrue(event.timestampMs > 0)
        assertTrue(event.processName!!.isNotEmpty())
        assertTrue(event.stackTrace.isNotEmpty())
    }

    @Test
    fun `observe mode calls logSync not logAsync`() {
        val observeDecision = ScopeDecision(
            shouldInstall = true,
            shouldIntercept = false,
            showNotify = false,
            crashLogEnabled = true,
        )
        val throwable = RuntimeException("observe")

        CrashCapturePipeline.onException(
            application = app,
            packageName = "com.example.app",
            appInfo = appInfo,
            throwable = throwable,
            source = "uncaught",
            decision = observeDecision,
        )

        verify(mockCoordinator).logSync(eq<Application>(app), any<CrashEvent>())
        verify(mockCoordinator, never()).logAsync(any<Application>(), any<CrashEvent>())
    }
}
