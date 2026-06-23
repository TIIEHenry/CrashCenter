package nota.android.crash.log.backend

import android.content.Context
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.ProcessSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class TargetRelayBackendTest {

    private lateinit var context: Context

    private val sampleEvent = CrashEvent(
        id = "relay-evt-1",
        packageName = "com.example.app",
        timestampMs = 1700000000000L,
        exceptionClass = "java.lang.NullPointerException",
        message = "null reference",
        stackTrace = "at com.example.Main.run(Main.java:10)",
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    // ── identity ────────────────────────────────────────────────────────────

    @Test
    fun `backend metadata matches architecture`() {
        assertEquals(BackendId.TARGET_RELAY, TargetRelayBackend.id)
        assertEquals(3, TargetRelayBackend.tier)
        assertEquals(ProcessSlot.HOOK, TargetRelayBackend.runsOn)
    }

    // ── probe ───────────────────────────────────────────────────────────────

    @Test
    fun `probe always returns READY`() {
        assertEquals(BackendAvailability.READY, TargetRelayBackend.probe(context))
    }

    // ── append ──────────────────────────────────────────────────────────────

    @Test
    fun `append writes relay file with correct JSON content`() {
        val result = TargetRelayBackend.append(context, sampleEvent, deadlineMs = 5000L)

        assertTrue(result is AppendResult.Success)

        val relayDir = File(context.filesDir, TargetRelayBackend.RELAY_DIR)
        assertTrue("relay dir should exist", relayDir.isDirectory)

        val relayFile = File(relayDir, "${sampleEvent.id}.json")
        assertTrue("relay file should exist", relayFile.exists())

        val content = relayFile.readText(Charsets.UTF_8)
        val parsed = CrashEvent.fromJson(content)
        assertEquals(sampleEvent.id, parsed?.id)
        assertEquals(sampleEvent.packageName, parsed?.packageName)
        assertEquals(sampleEvent.exceptionClass, parsed?.exceptionClass)
    }

    @Test
    fun `append stamps backendWritten on event`() {
        TargetRelayBackend.append(context, sampleEvent, deadlineMs = 5000L)

        val relayFile = File(
            File(context.filesDir, TargetRelayBackend.RELAY_DIR),
            "${sampleEvent.id}.json",
        )
        val parsed = CrashEvent.fromJson(relayFile.readText(Charsets.UTF_8))
        assertTrue(parsed?.backendWritten?.contains(BackendId.TARGET_RELAY.wireName) == true)
    }

    @Test
    fun `append creates relay directory if absent`() {
        val relayDir = File(context.filesDir, TargetRelayBackend.RELAY_DIR)
        // Ensure clean state
        relayDir.deleteRecursively()

        val result = TargetRelayBackend.append(context, sampleEvent, deadlineMs = 5000L)

        assertTrue(result is AppendResult.Success)
        assertTrue("relay dir should be created", relayDir.isDirectory)
    }

    @Test
    fun `append uses eventId as filename`() {
        val event = sampleEvent.copy(id = "unique-id-42")
        TargetRelayBackend.append(context, event, deadlineMs = 5000L)

        val relayFile = File(
            File(context.filesDir, TargetRelayBackend.RELAY_DIR),
            "unique-id-42.json",
        )
        assertTrue("file should be named by event id", relayFile.exists())
    }
}
