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
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RootFsBackendTest {

    private lateinit var context: Context
    private lateinit var mockClient: RootAccessClient

    private val sampleEvent = CrashEvent(
        id = "fs-evt-1",
        packageName = "com.example.app",
        timestampMs = 1700000000000L,
        exceptionClass = "java.lang.RuntimeException",
        message = "boom",
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockClient = mock()
        RootFsBackend.testClient = mockClient
    }

    @After
    fun tearDown() {
        RootFsBackend.testClient = null
    }

    // ── identity ────────────────────────────────────────────────────────────

    @Test
    fun `backend metadata matches architecture`() {
        assertEquals(BackendId.ROOT_FS, RootFsBackend.id)
        assertEquals(0, RootFsBackend.tier)
        assertEquals(ProcessSlot.MODULE, RootFsBackend.runsOn)
    }

    // ── probe ───────────────────────────────────────────────────────────────

    @Test
    fun `probe returns READY when client reports AVAILABLE`() {
        mockClient.stub { on { probe() }.thenReturn(RootAvailability.AVAILABLE) }

        assertEquals(BackendAvailability.READY, RootFsBackend.probe(context))
    }

    @Test
    fun `probe returns MAYBE when client reports DENIED`() {
        mockClient.stub { on { probe() }.thenReturn(RootAvailability.DENIED) }

        assertEquals(BackendAvailability.MAYBE, RootFsBackend.probe(context))
    }

    @Test
    fun `probe returns UNAVAILABLE when client reports UNAVAILABLE`() {
        mockClient.stub { on { probe() }.thenReturn(RootAvailability.UNAVAILABLE) }

        assertEquals(BackendAvailability.UNAVAILABLE, RootFsBackend.probe(context))
    }

    // ── append ──────────────────────────────────────────────────────────────

    @Test
    fun `append returns Success when client returns true`() {
        mockClient.stub {
            onBlocking { appendBytes(any(), any(), any()) }.thenReturn(true)
        }

        val result = RootFsBackend.append(context, sampleEvent, deadlineMs = 5000L)

        assertTrue(result is AppendResult.Success)
    }

    @Test
    fun `append returns Failure when client returns false`() {
        mockClient.stub {
            onBlocking { appendBytes(any(), any(), any()) }.thenReturn(false)
        }

        val result = RootFsBackend.append(context, sampleEvent, deadlineMs = 5000L)

        assertTrue(result is AppendResult.Failure)
        assertEquals("root append failed", (result as AppendResult.Failure).reason)
    }

    @Test
    fun `append passes events file path to client`() {
        val capturedPath = mutableListOf<String>()
        mockClient.stub {
            onBlocking { appendBytes(any(), any(), any()) }.thenAnswer { invocation ->
                capturedPath.add(invocation.arguments[0] as String)
                true
            }
        }

        RootFsBackend.append(context, sampleEvent, deadlineMs = 5000L)

        assertEquals(1, capturedPath.size)
        assertTrue(
            "path should end with events.jsonl",
            capturedPath[0].endsWith("crash_logs/events.jsonl"),
        )
    }

    @Test
    fun `append passes caller deadline through to client`() {
        val capturedDeadline = mutableListOf<Long>()
        mockClient.stub {
            onBlocking { appendBytes(any(), any(), any()) }.thenAnswer { invocation ->
                capturedDeadline.add(invocation.arguments[2] as Long)
                true
            }
        }

        RootFsBackend.append(context, sampleEvent, deadlineMs = 3000L)

        assertEquals(1, capturedDeadline.size)
        assertEquals(3000L, capturedDeadline[0])
    }

    @Test
    fun `append stamps backendWritten on event`() {
        val capturedData = mutableListOf<ByteArray>()
        mockClient.stub {
            onBlocking { appendBytes(any(), any(), any()) }.thenAnswer { invocation ->
                capturedData.add(invocation.arguments[1] as ByteArray)
                true
            }
        }

        RootFsBackend.append(context, sampleEvent, deadlineMs = 5000L)

        val line = String(capturedData[0], Charsets.UTF_8).trim()
        val parsed = CrashEvent.fromJson(line)
        assertTrue(parsed?.backendWritten?.contains(BackendId.ROOT_FS.wireName) == true)
    }
}
