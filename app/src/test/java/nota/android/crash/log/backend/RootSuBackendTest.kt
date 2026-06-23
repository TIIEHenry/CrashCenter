package nota.android.crash.log.backend

import android.content.Context
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.ProcessSlot
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootAvailability
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RootSuBackendTest {

    private lateinit var context: Context
    private lateinit var mockAdapter: RootAccessClient
    private lateinit var originalAdapter: RootAccessClient

    private val sampleEvent = CrashEvent(
        id = "su-evt-1",
        packageName = "com.example.app",
        timestampMs = 1700000000000L,
        exceptionClass = "java.lang.NullPointerException",
        message = "null reference",
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        originalAdapter = RootSuBackend.adapter
        mockAdapter = mock()
        RootSuBackend.adapter = mockAdapter
    }

    @After
    fun tearDown() {
        RootSuBackend.adapter = originalAdapter
    }

    // ── identity ────────────────────────────────────────────────────────────

    @Test
    fun `backend metadata matches architecture`() {
        assertEquals(BackendId.ROOT_SU, RootSuBackend.id)
        assertEquals(0, RootSuBackend.tier)
        assertEquals(ProcessSlot.HOOK, RootSuBackend.runsOn)
    }

    // ── probe ───────────────────────────────────────────────────────────────

    @Test
    fun `probe returns READY when adapter reports AVAILABLE`() {
        mockAdapter.stub { on { probe() }.thenReturn(RootAvailability.AVAILABLE) }

        assertEquals(BackendAvailability.READY, RootSuBackend.probe(context))
    }

    @Test
    fun `probe returns MAYBE when adapter reports DENIED`() {
        mockAdapter.stub { on { probe() }.thenReturn(RootAvailability.DENIED) }

        assertEquals(BackendAvailability.MAYBE, RootSuBackend.probe(context))
    }

    @Test
    fun `probe returns UNAVAILABLE when adapter reports UNAVAILABLE`() {
        mockAdapter.stub { on { probe() }.thenReturn(RootAvailability.UNAVAILABLE) }

        assertEquals(BackendAvailability.UNAVAILABLE, RootSuBackend.probe(context))
    }

    @Test
    fun `probe returns UNAVAILABLE when adapter throws exception`() {
        mockAdapter.stub { on { probe() }.thenThrow(RuntimeException("su not found")) }

        assertEquals(BackendAvailability.UNAVAILABLE, RootSuBackend.probe(context))
    }

    // ── append ──────────────────────────────────────────────────────────────

    @Test
    fun `append returns Success when adapter returns true`() {
        mockAdapter.stub {
            onBlocking { appendBytes(any(), any(), any()) }.thenReturn(true)
        }

        val result = RootSuBackend.append(context, sampleEvent, deadlineMs = 5000L)

        assertTrue(result is AppendResult.Success)
    }

    @Test
    fun `append returns Failure when adapter returns false`() {
        mockAdapter.stub {
            onBlocking { appendBytes(any(), any(), any()) }.thenReturn(false)
        }

        val result = RootSuBackend.append(context, sampleEvent, deadlineMs = 5000L)

        assertTrue(result is AppendResult.Failure)
        assertEquals("su append failed", (result as AppendResult.Failure).reason)
    }

    @Test
    fun `append passes event file path to adapter`() {
        val capturedPath = mutableListOf<String>()
        mockAdapter.stub {
            onBlocking { appendBytes(any(), any(), any()) }.thenAnswer { invocation ->
                capturedPath.add(invocation.arguments[0] as String)
                true
            }
        }

        RootSuBackend.append(context, sampleEvent, deadlineMs = 5000L)

        assertEquals(1, capturedPath.size)
        assertTrue(
            "path should end with events.jsonl",
            capturedPath[0].endsWith("crash_logs/events.jsonl"),
        )
    }

    @Test
    fun `append uses hardcoded deadline instead of caller deadline`() {
        val capturedDeadline = mutableListOf<Long>()
        mockAdapter.stub {
            onBlocking { appendBytes(any(), any(), any()) }.thenAnswer { invocation ->
                capturedDeadline.add(invocation.arguments[2] as Long)
                true
            }
        }

        RootSuBackend.append(context, sampleEvent, deadlineMs = 99999L)

        assertEquals(1, capturedDeadline.size)
        assertEquals(1500L, capturedDeadline[0])
    }

    @Test
    fun `append stamps backendWritten on event`() {
        val capturedData = mutableListOf<ByteArray>()
        mockAdapter.stub {
            onBlocking { appendBytes(any(), any(), any()) }.thenAnswer { invocation ->
                capturedData.add(invocation.arguments[1] as ByteArray)
                true
            }
        }

        RootSuBackend.append(context, sampleEvent, deadlineMs = 5000L)

        val line = String(capturedData[0], Charsets.UTF_8).trim()
        val parsed = CrashEvent.fromJson(line)
        assertTrue(parsed?.backendWritten?.contains(BackendId.ROOT_SU.wireName) == true)
    }
}
