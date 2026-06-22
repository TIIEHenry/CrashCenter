package nota.android.crash.log

import android.content.Context
import kotlinx.coroutines.runBlocking
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootAvailability
import nota.android.crash.xp.app.data.FileCrashLogRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CrashLogIngestCoordinatorTest {

    private lateinit var context: Context
    private lateinit var mockRoot: RootAccessClient
    private var originalClient: RootAccessClient? = null

    private val sampleEvent = CrashEvent(
        id = "relay-evt-1",
        packageName = "com.example.app",
        timestampMs = 1700000000000L,
        exceptionClass = "java.lang.NullPointerException",
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockRoot = mock()
        originalClient = CrashLogIngestCoordinator.testClient
        CrashLogIngestCoordinator.testClient = mockRoot
    }

    @After
    fun tearDown() {
        CrashLogIngestCoordinator.testClient = originalClient
    }

    // ── root unavailable ────────────────────────────────────────────────────

    @Test
    fun `ingest skips when root is unavailable`() {
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.UNAVAILABLE)
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)
            verify(mockRoot, never()).listDir(any())
        }
    }

    @Test
    fun `ingest skips when root is denied`() {
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.DENIED)
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)
            verify(mockRoot, never()).listDir(any())
        }
    }

    // ── successful merge ────────────────────────────────────────────────────

    @Test
    fun `ingest merges relay file into canonical JSONL and deletes relay`() {
        val relayPath = "/data/user/0/com.example.app/files/crashcenter_relay/${sampleEvent.id}.json"
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("${sampleEvent.id}.json"))
            onBlocking { readText(relayPath) }.thenReturn(sampleEvent.toJsonLine())
            onBlocking { delete(any()) }.thenReturn(true)
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)

            val eventsFile = FileCrashLogRepository.eventsFile(context)
            assertTrue("Canonical JSONL should exist", eventsFile.exists())
            val lines = eventsFile.readLines().filter { it.isNotBlank() }
            assertEquals(1, lines.size)
            val parsed = CrashEvent.fromJson(lines[0])
            assertEquals("relay-evt-1", parsed?.id)
            verify(mockRoot).delete(relayPath)
        }
    }

    @Test
    fun `ingest merges multiple relay files across packages`() {
        val event2 = sampleEvent.copy(id = "relay-evt-2", packageName = "com.other.app")
        val relayPath1 = "/data/user/0/com.example.app/files/crashcenter_relay/${sampleEvent.id}.json"
        val relayPath2 = "/data/user/0/com.other.app/files/crashcenter_relay/${event2.id}.json"
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app", "com.other.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("${sampleEvent.id}.json"))
            onBlocking { listDir("/data/user/0/com.other.app/files/crashcenter_relay") }
                .thenReturn(listOf("${event2.id}.json"))
            onBlocking { readText(relayPath1) }.thenReturn(sampleEvent.toJsonLine())
            onBlocking { readText(relayPath2) }.thenReturn(event2.toJsonLine())
            onBlocking { delete(any()) }.thenReturn(true)
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)

            val eventsFile = FileCrashLogRepository.eventsFile(context)
            val lines = eventsFile.readLines().filter { it.isNotBlank() }
            assertEquals(2, lines.size)
            verify(mockRoot).delete(relayPath1)
            verify(mockRoot).delete(relayPath2)
        }
    }

    // ── deletion guard ──────────────────────────────────────────────────────

    @Test
    fun `ingest does not delete relay file when readText returns null`() {
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("${sampleEvent.id}.json"))
            onBlocking { readText(any()) }.thenReturn(null)
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)
            verify(mockRoot, never()).delete(any())
        }
    }

    // ── malformed JSON ──────────────────────────────────────────────────────

    @Test
    fun `ingest skips malformed JSON relay file without deleting`() {
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("bad.json"))
            onBlocking { readText("/data/user/0/com.example.app/files/crashcenter_relay/bad.json") }
                .thenReturn("not valid json {{{{")
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)
            verify(mockRoot, never()).delete(any())
        }
    }

    @Test
    fun `ingest skips JSON with empty id`() {
        val noIdEvent = CrashEvent(id = "", packageName = "com.example.app")
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("no-id.json"))
            onBlocking { readText("/data/user/0/com.example.app/files/crashcenter_relay/no-id.json") }
                .thenReturn(noIdEvent.toJsonLine())
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)
            verify(mockRoot, never()).delete(any())
        }
    }

    // ── empty relay directory ───────────────────────────────────────────────

    @Test
    fun `ingest does nothing when relay directory is empty`() {
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(emptyList())
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)
            verify(mockRoot, never()).readText(any())
            verify(mockRoot, never()).delete(any())
        }
    }

    @Test
    fun `ingest does nothing when no users found`() {
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(emptyList())
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)
            verify(mockRoot, never()).readText(any())
        }
    }

    // ── non-numeric user id ─────────────────────────────────────────────────

    @Test
    fun `ingest skips non-numeric user directory names`() {
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(listOf("lost+found", ".Trash"))
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)
            verify(mockRoot, never()).readText(any())
        }
    }

    // ── non-json relay files are skipped ────────────────────────────────────

    @Test
    fun `ingest skips non-json files in relay directory`() {
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("readme.txt", "meta.json.bak"))
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)
            verify(mockRoot, never()).readText(any())
        }
    }

    // ── listDir failure is non-fatal ────────────────────────────────────────

    @Test
    fun `ingest handles listDir failure for user base path gracefully`() {
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenThrow(RuntimeException("permission denied"))
        }

        runBlocking { CrashLogIngestCoordinator.ingest(context, mockRoot) }

        // Should not crash
    }

    @Test
    fun `ingest continues processing packages when one package listDir fails`() {
        val relayPath = "/data/user/0/com.example.app/files/crashcenter_relay/${sampleEvent.id}.json"
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.fail.app", "com.example.app"))
            onBlocking { listDir("/data/user/0/com.fail.app/files/crashcenter_relay") }
                .thenThrow(RuntimeException("denied"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("${sampleEvent.id}.json"))
            onBlocking { readText(relayPath) }.thenReturn(sampleEvent.toJsonLine())
            onBlocking { delete(any()) }.thenReturn(true)
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)

            val eventsFile = FileCrashLogRepository.eventsFile(context)
            assertTrue(eventsFile.exists())
            val lines = eventsFile.readLines().filter { it.isNotBlank() }
            assertEquals(1, lines.size)
        }
    }

    // ── scanRelayFiles helper ───────────────────────────────────────────────

    @Test
    fun `scanRelayFiles returns correct refs`() {
        mockRoot.stub {
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0", "10"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.a"))
            onBlocking { listDir("/data/user/10") }.thenReturn(listOf("com.b"))
            onBlocking { listDir("/data/user/0/com.a/files/crashcenter_relay") }
                .thenReturn(listOf("e1.json", "e2.json"))
            onBlocking { listDir("/data/user/10/com.b/files/crashcenter_relay") }
                .thenReturn(listOf("e3.txt"))
        }

        val refs = runBlocking { CrashLogIngestCoordinator.scanRelayFiles(mockRoot) }

        assertEquals(2, refs.size)
        assertTrue(refs.contains(CrashLogIngestCoordinator.RelayFileRef(0, "com.a", "e1.json")))
        assertTrue(refs.contains(CrashLogIngestCoordinator.RelayFileRef(0, "com.a", "e2.json")))
        // e3.txt is not .json, so excluded
    }

    // ── idempotent: re-ingesting already-ingested relay is safe ──────────────

    @Test
    fun `ingest is idempotent when relay file no longer exists`() {
        val relayPath = "/data/user/0/com.example.app/files/crashcenter_relay/${sampleEvent.id}.json"
        mockRoot.stub {
            onBlocking { probe() }.thenReturn(RootAvailability.AVAILABLE)
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("${sampleEvent.id}.json"))
            // File was already deleted by previous ingest
            onBlocking { readText(relayPath) }.thenReturn(null)
        }

        runBlocking {
            CrashLogIngestCoordinator.ingest(context, mockRoot)
            verify(mockRoot, never()).delete(any())
            val eventsFile = FileCrashLogRepository.eventsFile(context)
            assertFalse("No events should be written", eventsFile.exists())
        }
    }
}
