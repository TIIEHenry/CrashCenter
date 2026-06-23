package nota.android.crash.log.backend

import android.content.Context
import kotlinx.coroutines.runBlocking
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.BackendId
import nota.android.crash.log.ProcessSlot
import nota.android.crash.root.RootAccessClient
import nota.android.crash.xp.app.data.FileCrashLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class RelayMergeBackendTest {

    private lateinit var context: Context
    private lateinit var mockRoot: RootAccessClient

    private val sampleEvent = CrashEvent(
        id = "relay-evt-1",
        packageName = "com.example.app",
        timestampMs = 1700000000000L,
        exceptionClass = "java.lang.NullPointerException",
        backendWritten = listOf(BackendId.TARGET_RELAY.wireName),
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockRoot = mock()
    }

    // ── identity ────────────────────────────────────────────────────────────

    @Test
    fun `backend metadata matches architecture`() {
        assertEquals(BackendId.RELAY_MERGE, RelayMergeBackend.id)
        assertEquals(2, RelayMergeBackend.tier)
        assertEquals(ProcessSlot.MODULE, RelayMergeBackend.runsOn)
        assertEquals("relay_merge", BackendId.RELAY_MERGE.wireName)
    }

    // ── mergeDecision (pure dedupe) ─────────────────────────────────────────

    @Test
    fun `mergeDecision appends when id not in canonical`() {
        val decision = RelayMergeBackend.mergeDecision(sampleEvent, emptySet())
        assertTrue(decision is RelayMergeBackend.MergeDecision.Append)
        val stamped = (decision as RelayMergeBackend.MergeDecision.Append).stamped
        assertEquals(BackendId.TARGET_RELAY.wireName, stamped.ingestedFrom)
        assertTrue(stamped.backendWritten.contains(BackendId.RELAY_MERGE.wireName))
        assertTrue(stamped.backendWritten.contains(BackendId.TARGET_RELAY.wireName))
    }

    @Test
    fun `mergeDecision skips when id already in canonical`() {
        val decision = RelayMergeBackend.mergeDecision(sampleEvent, setOf("relay-evt-1"))
        assertEquals(RelayMergeBackend.MergeDecision.SkipDuplicate, decision)
    }

    @Test
    fun `mergeDecision unions backendWritten without duplicates`() {
        val event = sampleEvent.copy(
            backendWritten = listOf(BackendId.TARGET_RELAY.wireName, BackendId.RELAY_MERGE.wireName),
        )
        val decision = RelayMergeBackend.mergeDecision(event, emptySet()) as RelayMergeBackend.MergeDecision.Append
        assertEquals(2, decision.stamped.backendWritten.size)
    }

    // ── readCanonicalIds ────────────────────────────────────────────────────

    @Test
    fun `readCanonicalIds returns empty for missing file`() {
        val file = File(context.cacheDir, "missing-events.jsonl")
        assertTrue(RelayMergeBackend.readCanonicalIds(file).isEmpty())
    }

    @Test
    fun `readCanonicalIds collects ids from JSONL`() {
        val file = File(context.cacheDir, "canonical.jsonl")
        val e1 = sampleEvent.copy(id = "a")
        val e2 = sampleEvent.copy(id = "b")
        file.writeText("${e1.toJsonLine()}\n${e2.toJsonLine()}\n")
        assertEquals(setOf("a", "b"), RelayMergeBackend.readCanonicalIds(file))
    }

    @Test
    fun `readCanonicalIds ignores malformed lines`() {
        val file = File(context.cacheDir, "partial.jsonl")
        file.writeText("not json\n${sampleEvent.toJsonLine()}\n")
        assertEquals(setOf("relay-evt-1"), RelayMergeBackend.readCanonicalIds(file))
    }

    // ── harvest / mergeRelayFile ────────────────────────────────────────────

    @Test
    fun `harvest merges relay file into canonical JSONL and deletes relay`() {
        val relayPath = RelayMergeBackend.relayFilePath(0, "com.example.app", "${sampleEvent.id}.json")
        val deleted = mutableListOf<String>()
        mockRoot.stub {
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("${sampleEvent.id}.json"))
            onBlocking { readText(relayPath) }.thenReturn(sampleEvent.toJsonLine())
            onBlocking { delete(any()) }.thenAnswer { invocation ->
                deleted.add(invocation.arguments[0] as String)
                true
            }
        }

        runBlocking { RelayMergeBackend.harvest(context, mockRoot) }

        val eventsFile = FileCrashLogRepository.eventsFile(context)
        assertTrue(eventsFile.exists())
        val lines = eventsFile.readLines().filter { it.isNotBlank() }
        assertEquals(1, lines.size)
        val parsed = CrashEvent.fromJson(lines[0])
        assertEquals("relay-evt-1", parsed?.id)
        assertEquals(BackendId.TARGET_RELAY.wireName, parsed?.ingestedFrom)
        assertEquals(listOf(relayPath), deleted)
    }

    @Test
    fun `harvest skips append but deletes relay when id already in canonical`() {
        val eventsFile = FileCrashLogRepository.eventsFile(context)
        val existing = sampleEvent.copy(ingestedFrom = null, backendWritten = listOf("provider_insert"))
        eventsFile.parentFile?.mkdirs()
        eventsFile.writeText("${existing.toJsonLine()}\n")

        val relayPath = RelayMergeBackend.relayFilePath(0, "com.example.app", "${sampleEvent.id}.json")
        val deleted = mutableListOf<String>()
        mockRoot.stub {
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("${sampleEvent.id}.json"))
            onBlocking { readText(relayPath) }.thenReturn(sampleEvent.toJsonLine())
            onBlocking { delete(any()) }.thenAnswer { invocation ->
                deleted.add(invocation.arguments[0] as String)
                true
            }
        }

        runBlocking { RelayMergeBackend.harvest(context, mockRoot) }

        val lines = eventsFile.readLines().filter { it.isNotBlank() }
        assertEquals(1, lines.size)
        assertFalse(lines[0].contains("relay_merge"))
        assertEquals(listOf(relayPath), deleted)
    }

    @Test
    fun `harvest merges multiple relay files across packages`() {
        val event2 = sampleEvent.copy(id = "relay-evt-2", packageName = "com.other.app")
        val relayPath1 = RelayMergeBackend.relayFilePath(0, "com.example.app", "${sampleEvent.id}.json")
        val relayPath2 = RelayMergeBackend.relayFilePath(0, "com.other.app", "${event2.id}.json")
        mockRoot.stub {
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

        runBlocking { RelayMergeBackend.harvest(context, mockRoot) }

        val eventsFile = FileCrashLogRepository.eventsFile(context)
        val lines = eventsFile.readLines().filter { it.isNotBlank() }
        assertEquals(2, lines.size)
    }

    @Test
    fun `harvest does not delete relay when readText returns null`() {
        val deleted = mutableListOf<String>()
        mockRoot.stub {
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("${sampleEvent.id}.json"))
            onBlocking { readText(any()) }.thenReturn(null)
            onBlocking { delete(any()) }.thenAnswer { invocation ->
                deleted.add(invocation.arguments[0] as String)
                true
            }
        }

        runBlocking { RelayMergeBackend.harvest(context, mockRoot) }
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun `harvest skips malformed JSON without deleting`() {
        val badPath = RelayMergeBackend.relayFilePath(0, "com.example.app", "bad.json")
        val deleted = mutableListOf<String>()
        mockRoot.stub {
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.example.app"))
            onBlocking { listDir("/data/user/0/com.example.app/files/crashcenter_relay") }
                .thenReturn(listOf("bad.json"))
            onBlocking { readText(badPath) }.thenReturn("not valid json {{{{")
            onBlocking { delete(any()) }.thenAnswer { invocation ->
                deleted.add(invocation.arguments[0] as String)
                true
            }
        }

        runBlocking { RelayMergeBackend.harvest(context, mockRoot) }
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun `harvest dedupes two relay files with same id in one pass`() {
        val relayPath1 = RelayMergeBackend.relayFilePath(0, "com.a", "${sampleEvent.id}.json")
        val relayPath2 = RelayMergeBackend.relayFilePath(0, "com.b", "${sampleEvent.id}.json")
        val deleted = mutableListOf<String>()
        mockRoot.stub {
            onBlocking { listDir("/data/user") }.thenReturn(listOf("0"))
            onBlocking { listDir("/data/user/0") }.thenReturn(listOf("com.a", "com.b"))
            onBlocking { listDir("/data/user/0/com.a/files/crashcenter_relay") }
                .thenReturn(listOf("${sampleEvent.id}.json"))
            onBlocking { listDir("/data/user/0/com.b/files/crashcenter_relay") }
                .thenReturn(listOf("${sampleEvent.id}.json"))
            onBlocking { readText(relayPath1) }.thenReturn(sampleEvent.toJsonLine())
            onBlocking { readText(relayPath2) }.thenReturn(sampleEvent.toJsonLine())
            onBlocking { delete(any()) }.thenAnswer { invocation ->
                deleted.add(invocation.arguments[0] as String)
                true
            }
        }

        runBlocking { RelayMergeBackend.harvest(context, mockRoot) }

        val eventsFile = FileCrashLogRepository.eventsFile(context)
        val lines = eventsFile.readLines().filter { it.isNotBlank() }
        assertEquals(1, lines.size)
        assertEquals(setOf(relayPath1, relayPath2), deleted.toSet())
    }

    // ── scanRelayFiles ────────────────────────────────────────────────────────

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

        val refs = runBlocking { RelayMergeBackend.scanRelayFiles(mockRoot) }

        assertEquals(2, refs.size)
        assertTrue(refs.contains(RelayMergeBackend.RelayFileRef(0, "com.a", "e1.json")))
        assertTrue(refs.contains(RelayMergeBackend.RelayFileRef(0, "com.a", "e2.json")))
    }
}
