package nota.android.crash.log.backend

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.robv.android.xposed.XSharedPreferences
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.CrashLogPaths
import nota.android.crash.log.ProcessSlot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalCacheBackendTest {

    private lateinit var context: Context
    private lateinit var xPrefsConstruction: MockedConstruction<XSharedPreferences>
    private val sampleEvent = CrashEvent(
        id = "local-1",
        timestampMs = 1000L,
        packageName = "com.test",
        exceptionClass = "java.lang.RuntimeException",
        stackTrace = "at Foo.bar",
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        CrashLogPaths.eventsFile(context).delete()

        xPrefsConstruction = Mockito.mockConstruction(XSharedPreferences::class.java) { mock, _ ->
            whenever(mock.reload()).then { }
            whenever(mock.getInt(any(), any())).thenReturn(500)
            whenever(mock.getLong(any(), any())).thenReturn(8L * 1024L * 1024L)
        }
    }

    @After
    fun tearDown() {
        xPrefsConstruction.close()
    }

    @Test
    fun metadata() {
        assertEquals(BackendId.LOCAL_CACHE, LocalCacheBackend.id)
        assertEquals(0, LocalCacheBackend.tier)
        assertEquals(ProcessSlot.HOOK, LocalCacheBackend.runsOn)
        assertEquals(BackendAvailability.READY, LocalCacheBackend.probe(context))
    }

    @Test
    fun append_writes_cache_jsonl() {
        val result = LocalCacheBackend.append(context, sampleEvent, 5000L)
        assertTrue(result is AppendResult.Success)
        val file = CrashLogPaths.eventsFile(context)
        assertTrue(file.exists())
        val parsed = CrashEvent.fromJson(file.readText().trim())
        assertEquals("local-1", parsed?.id)
        assertTrue(parsed?.backendWritten?.contains(BackendId.LOCAL_CACHE.wireName) == true)
    }
}
