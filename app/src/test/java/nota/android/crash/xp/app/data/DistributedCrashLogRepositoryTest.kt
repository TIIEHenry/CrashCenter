package nota.android.crash.xp.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import nota.android.crash.common.data.CrashEvent
import kotlinx.coroutines.runBlocking
import nota.android.crash.log.CrashLogCacheScanner
import nota.android.crash.log.CrashLogPaths
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootAvailability
import nota.android.crash.root.RootFileStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DistributedCrashLogRepositoryTest {

    private lateinit var context: Context
    private lateinit var mockRoot: FakeRootClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockRoot = FakeRootClient()
    }

    @Test
    fun `no root returns empty`() {
        mockRoot.availability = RootAvailability.DENIED
        val repo = DistributedCrashLogRepository(context, rootClientProvider = { mockRoot })
        assertEquals(0, repo.getCount())
        assertNull(repo.getById("any"))
    }

    @Test
    fun `crash event json parses`() {
        val original = CrashEvent(
            id = "1",
            timestampMs = 100L,
            packageName = "com.a",
            exceptionClass = "E",
        )
        val parsed = CrashEvent.fromJson(original.toJsonLine())
        assertEquals("1", parsed?.id)
    }

    @Test
    fun `scanner finds cache jsonl files`() = runBlocking {
        mockRoot.availability = RootAvailability.AVAILABLE
        mockRoot.dirEntries["/data/user"] = listOf("0")
        mockRoot.dirEntries["/data/user/0"] = listOf("com.a")
        mockRoot.dirEntries["/data/user/0/com.a/cache/crash_logs"] = listOf("events.jsonl")
        val refs = CrashLogCacheScanner.scanEventFiles(mockRoot)
        assertEquals(1, refs.size)
    }

    @Test
    fun `aggregates and dedupes by max timestamp`() {
        mockRoot.availability = RootAvailability.AVAILABLE
        val eventA1 = CrashEvent(id = "1", timestampMs = 100L, packageName = "com.a", exceptionClass = "E")
        val eventA2 = CrashEvent(id = "2", timestampMs = 200L, packageName = "com.a", exceptionClass = "F")
        val eventB1 = CrashEvent(id = "1", timestampMs = 300L, packageName = "com.b", exceptionClass = "G")
        val path = CrashLogPaths.eventsPath(0, "com.a")
        mockRoot.files[path] = listOf(eventA1, eventA2).joinToString("\n") { it.toJsonLine() } + "\n"
        val pathB = CrashLogPaths.eventsPath(0, "com.b")
        mockRoot.files[pathB] = eventB1.toJsonLine() + "\n"

        mockRoot.dirEntries["/data/user"] = listOf("0")
        mockRoot.dirEntries["/data/user/0"] = listOf("com.a", "com.b")
        mockRoot.dirEntries["/data/user/0/com.a/cache/crash_logs"] = listOf("events.jsonl")
        mockRoot.dirEntries["/data/user/0/com.b/cache/crash_logs"] = listOf("events.jsonl")

        val repo = DistributedCrashLogRepository(context, rootClientProvider = { mockRoot })
        val all = repo.getAll(limit = 10, offset = 0)
        assertEquals(2, all.size)
        assertEquals(300L, all.first { it.id == "1" }.timestampMs)
        assertEquals(200L, all.first { it.id == "2" }.timestampMs)
    }

    @Test
    fun `stable fingerprint avoids second full load`() {
        mockRoot.availability = RootAvailability.AVAILABLE
        val event = CrashEvent(id = "1", timestampMs = 100L, packageName = "com.a", exceptionClass = "E")
        val path = CrashLogPaths.eventsPath(0, "com.a")
        mockRoot.files[path] = event.toJsonLine() + "\n"
        mockRoot.dirEntries["/data/user"] = listOf("0")
        mockRoot.dirEntries["/data/user/0"] = listOf("com.a")
        mockRoot.dirEntries["/data/user/0/com.a/cache/crash_logs"] = listOf("events.jsonl")

        val repo = DistributedCrashLogRepository(context, rootClientProvider = { mockRoot })
        assertEquals(1, repo.getCount())
        val readsAfterFirst = mockRoot.readTextCount
        assertEquals(1, repo.getCount())
        assertEquals(readsAfterFirst, mockRoot.readTextCount)
    }

    private class FakeRootClient : RootAccessClient {
        var availability: RootAvailability = RootAvailability.UNAVAILABLE
        val files = mutableMapOf<String, String>()
        val dirEntries = mutableMapOf<String, List<String>>()
        var readTextCount = 0

        override fun probe(): RootAvailability = availability

        override suspend fun fileStat(path: String): RootFileStat? {
            val content = files[path] ?: return null
            return RootFileStat(mtimeMs = 1L, length = content.length.toLong())
        }

        override suspend fun readText(path: String): String? {
            readTextCount++
            return files[path]
        }

        override suspend fun listDir(path: String): List<String> = dirEntries[path].orEmpty()

        override suspend fun appendBytes(path: String, data: ByteArray, deadlineMs: Long): Boolean = false

        override suspend fun writeText(path: String, content: String): Boolean {
            files[path] = content
            return true
        }

        override suspend fun delete(path: String): Boolean {
            files.remove(path)
            return true
        }
    }
}
