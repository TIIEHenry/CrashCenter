package nota.android.crash.log

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.root.RootAccessClient
import nota.android.crash.root.RootAvailability
import nota.android.crash.root.RootFileStat
import nota.android.crash.xp.PrefManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashLogMigrationCoordinatorTest {

    private lateinit var context: Context
    private lateinit var root: RecordingRootClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        root = RecordingRootClient()
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        CrashLogPaths.legacyCanonicalFile(context).delete()
    }

    @Test
    fun `migrates legacy canonical to per-app cache`() = runBlocking {
        val event = CrashEvent(
            id = "m1",
            timestampMs = 100L,
            packageName = "com.example",
            exceptionClass = "E",
        )
        val canonical = CrashLogPaths.legacyCanonicalFile(context)
        canonical.parentFile?.mkdirs()
        canonical.writeText(event.toJsonLine() + "\n")

        root.availability = RootAvailability.AVAILABLE
        root.dirEntries["/data/user"] = listOf("0")
        root.dirEntries["/data/user/0"] = emptyList()

        CrashLogMigrationCoordinator.migrate(context, root)

        val targetPath = CrashLogPaths.eventsPath(0, "com.example")
        assertTrue(root.files.containsKey(targetPath))
        assertTrue(root.files[targetPath]!!.contains("m1"))
        assertFalse(canonical.exists())
        assertTrue(
            context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(PrefManager.PREF_DISTRIBUTED_CACHE_MIGRATED, false),
        )
    }

    @Test
    fun `does not mark migrated when root write fails`() = runBlocking {
        val event = CrashEvent(
            id = "m2",
            timestampMs = 200L,
            packageName = "com.fail",
            exceptionClass = "E",
        )
        val canonical = CrashLogPaths.legacyCanonicalFile(context)
        canonical.parentFile?.mkdirs()
        canonical.writeText(event.toJsonLine() + "\n")

        root.availability = RootAvailability.AVAILABLE
        root.writeShouldSucceed = false
        root.dirEntries["/data/user"] = listOf("0")
        root.dirEntries["/data/user/0"] = emptyList()

        CrashLogMigrationCoordinator.migrate(context, root)

        assertTrue(canonical.exists())
        assertFalse(
            context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(PrefManager.PREF_DISTRIBUTED_CACHE_MIGRATED, false),
        )
    }

    @Test
    fun `relay events migrate to matching userId path`() = runBlocking {
        val event = CrashEvent(
            id = "r1",
            timestampMs = 300L,
            packageName = "com.work",
            exceptionClass = "E",
        )
        val userId = 10
        val relayPath = CrashLogPaths.legacyRelayDir(userId, "com.work")
        root.files["$relayPath/e1.json"] = event.toJsonLine()
        root.dirEntries["/data/user"] = listOf("10")
        root.dirEntries["/data/user/10"] = listOf("com.work")
        root.dirEntries[relayPath] = listOf("e1.json")
        root.availability = RootAvailability.AVAILABLE

        CrashLogMigrationCoordinator.migrate(context, root)

        val targetPath = CrashLogPaths.eventsPath(userId, "com.work")
        assertTrue(root.files.containsKey(targetPath))
        assertEquals(
            true,
            context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(PrefManager.PREF_DISTRIBUTED_CACHE_MIGRATED, false),
        )
    }

    private class RecordingRootClient : RootAccessClient {
        var availability: RootAvailability = RootAvailability.UNAVAILABLE
        var writeShouldSucceed = true
        val files = mutableMapOf<String, String>()
        val dirEntries = mutableMapOf<String, List<String>>()

        override fun probe(): RootAvailability = availability

        override suspend fun fileStat(path: String): RootFileStat? {
            val content = files[path] ?: return null
            return RootFileStat(mtimeMs = 1L, length = content.length.toLong())
        }

        override suspend fun readText(path: String): String? = files[path]

        override suspend fun listDir(path: String): List<String> = dirEntries[path].orEmpty()

        override suspend fun appendBytes(path: String, data: ByteArray, deadlineMs: Long): Boolean = false

        override suspend fun writeText(path: String, content: String): Boolean {
            if (!writeShouldSucceed) return false
            files[path] = content
            return true
        }

        override suspend fun delete(path: String): Boolean {
            files.remove(path)
            return true
        }
    }
}
