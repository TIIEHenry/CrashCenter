package nota.android.crash.log.backend

import android.content.Context
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.ProcessSlot
import nota.android.crash.xp.app.data.FileCrashLogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DirectFsBackendTest {

    private lateinit var context: Context

    private val sampleEvent = CrashEvent(
        id = "dfs-evt-1",
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
        assertEquals(BackendId.DIRECT_FS, DirectFsBackend.id)
        assertEquals(2, DirectFsBackend.tier)
        assertEquals(ProcessSlot.HOOK, DirectFsBackend.runsOn)
    }

    // ── probe ───────────────────────────────────────────────────────────────

    @Test
    fun `probe returns READY when context can create log dir`() {
        val availability = DirectFsBackend.probe(context)
        assertEquals(BackendAvailability.READY, availability)
    }

    @Test
    fun `probe creates crash_logs directory if absent`() {
        val logDir = File(context.filesDir, FileCrashLogRepository.LOG_DIR)
        logDir.deleteRecursively()

        val availability = DirectFsBackend.probe(context)

        assertEquals(BackendAvailability.READY, availability)
        assertTrue("log dir should be created by probe", logDir.isDirectory)
    }

    // ── append ──────────────────────────────────────────────────────────────

    @Test
    fun `append writes event to events_jsonl`() {
        val result = DirectFsBackend.append(context, sampleEvent, deadlineMs = 5000L)

        assertTrue(result is AppendResult.Success)

        val eventsFile = File(
            context.filesDir,
            "${FileCrashLogRepository.LOG_DIR}/${FileCrashLogRepository.EVENTS_FILE}",
        )
        assertTrue("events file should exist", eventsFile.exists())

        val content = eventsFile.readText(Charsets.UTF_8)
        val parsed = CrashEvent.fromJson(content.trim())
        assertEquals(sampleEvent.id, parsed?.id)
        assertEquals(sampleEvent.packageName, parsed?.packageName)
        assertEquals(sampleEvent.exceptionClass, parsed?.exceptionClass)
    }

    @Test
    fun `append stamps backendWritten on event`() {
        DirectFsBackend.append(context, sampleEvent, deadlineMs = 5000L)

        val eventsFile = File(
            context.filesDir,
            "${FileCrashLogRepository.LOG_DIR}/${FileCrashLogRepository.EVENTS_FILE}",
        )
        val parsed = CrashEvent.fromJson(eventsFile.readText(Charsets.UTF_8).trim())
        assertTrue(
            "backendWritten should contain direct_fs",
            parsed?.backendWritten?.contains(BackendId.DIRECT_FS.wireName) == true,
        )
    }

    @Test
    fun `append creates crash_logs directory if absent`() {
        val logDir = File(context.filesDir, FileCrashLogRepository.LOG_DIR)
        logDir.deleteRecursively()

        val result = DirectFsBackend.append(context, sampleEvent, deadlineMs = 5000L)

        assertTrue(result is AppendResult.Success)
        assertTrue("log dir should be created by append", logDir.isDirectory)
    }

    @Test
    fun `append writes multiple events as separate lines`() {
        val second = sampleEvent.copy(id = "dfs-evt-2")

        DirectFsBackend.append(context, sampleEvent, deadlineMs = 5000L)
        DirectFsBackend.append(context, second, deadlineMs = 5000L)

        val eventsFile = File(
            context.filesDir,
            "${FileCrashLogRepository.LOG_DIR}/${FileCrashLogRepository.EVENTS_FILE}",
        )
        val lines = eventsFile.readText(Charsets.UTF_8)
            .trim()
            .lines()
            .filter { it.isNotBlank() }
        assertEquals(2, lines.size)
    }
}
