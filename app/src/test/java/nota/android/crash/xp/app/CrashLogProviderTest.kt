package nota.android.crash.xp.app

import android.content.ContentValues
import android.net.Uri
import android.os.Binder
import android.os.Process
import nota.android.crash.log.CrashLogContract
import nota.android.crash.xp.app.data.CrashEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBinder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CrashLogProviderTest {

    private lateinit var provider: CrashLogProvider
    private val myPackage = "nota.android.crash.xp.app"
    private val foreignPackage = "com.example.foreign"
    private val foreignUid = 10123

    @Before
    fun setUp() {
        provider = CrashLogProvider()
        provider.onCreate()

        // Inject Application context via reflection (Robolectric doesn't expose shadow setContext for ContentProvider)
        val ctx = RuntimeEnvironment.getApplication()
        val mContext = android.content.ContentProvider::class.java.getDeclaredField("mContext")
            .apply { isAccessible = true }
        mContext.set(provider, ctx)

        // Register foreign package under foreign UID
        val pm = Shadows.shadowOf(ctx.packageManager)
        pm.setPackagesForUid(foreignUid, foreignPackage)
    }

    @After
    fun tearDown() {
        // Clear rate-limit state between tests via reflection
        val rateWindowStart = CrashLogProvider::class.java
            .getDeclaredField("rateWindowStart")
            .apply { isAccessible = true }
            .get(null) as java.util.concurrent.ConcurrentHashMap<*, *>
        val rateCounts = CrashLogProvider::class.java
            .getDeclaredField("rateCounts")
            .apply { isAccessible = true }
            .get(null) as java.util.concurrent.ConcurrentHashMap<*, *>
        rateWindowStart.clear()
        rateCounts.clear()
    }

    // ---------- validateCaller ----------

    @Test
    fun validateCaller_sameUid_returnsTrue() {
        ShadowBinder.setCallingUid(Process.myUid())
        assertTrue(provider.validateCaller(myPackage))
    }

    @Test
    fun validateCaller_foreignUid_matchingPackage_returnsTrue() {
        ShadowBinder.setCallingUid(foreignUid)
        assertTrue(provider.validateCaller(foreignPackage))
    }

    @Test
    fun validateCaller_foreignUid_mismatchedPackage_returnsFalse() {
        ShadowBinder.setCallingUid(foreignUid)
        assertFalse(provider.validateCaller("com.example.other"))
    }

    @Test
    fun validateCaller_noPackagesForUid_returnsFalse() {
        ShadowBinder.setCallingUid(99999)
        assertFalse(provider.validateCaller(foreignPackage))
    }

    // ---------- checkRateLimit ----------

    @Test
    fun checkRateLimit_firstCall_returnsTrue() {
        ShadowBinder.setCallingUid(Process.myUid())
        assertTrue(provider.checkRateLimit())
    }

    @Test
    fun checkRateLimit_withinLimit_returnsTrue() {
        ShadowBinder.setCallingUid(Process.myUid())
        repeat(30) {
            assertTrue("failed at iteration $it", provider.checkRateLimit())
        }
    }

    @Test
    fun checkRateLimit_exceedsLimit_returnsFalse() {
        ShadowBinder.setCallingUid(Process.myUid())
        repeat(30) { provider.checkRateLimit() }
        assertFalse(provider.checkRateLimit())
    }

    @Test
    fun checkRateLimit_separateUidsIndependent() {
        ShadowBinder.setCallingUid(1000)
        repeat(30) { provider.checkRateLimit() }
        assertFalse(provider.checkRateLimit())

        ShadowBinder.setCallingUid(1001)
        assertTrue(provider.checkRateLimit())
    }

    // ---------- insert payload validation ----------

    @Test
    fun insert_malformedJson_returnsNull() {
        ShadowBinder.setCallingUid(Process.myUid())
        val values = ContentValues().apply {
            put(CrashLogContract.COLUMN_PACKAGE_NAME, myPackage)
            put(CrashLogContract.COLUMN_PAYLOAD, "not json at all")
        }
        assertNull(provider.insert(CrashLogContract.EVENTS_URI, values))
    }

    @Test
    fun insert_oversizedPayload_returnsNull() {
        ShadowBinder.setCallingUid(Process.myUid())
        val huge = "x".repeat(64 * 1024 + 1)
        val values = ContentValues().apply {
            put(CrashLogContract.COLUMN_PACKAGE_NAME, myPackage)
            put(CrashLogContract.COLUMN_PAYLOAD, huge)
        }
        assertNull(provider.insert(CrashLogContract.EVENTS_URI, values))
    }

    @Test
    fun insert_packageMismatch_returnsNull() {
        ShadowBinder.setCallingUid(Process.myUid())
        val event = validEvent(myPackage)
        val values = ContentValues().apply {
            put(CrashLogContract.COLUMN_PACKAGE_NAME, foreignPackage) // mismatched
            put(CrashLogContract.COLUMN_PAYLOAD, event.toJsonLine())
        }
        assertNull(provider.insert(CrashLogContract.EVENTS_URI, values))
    }

    // ---------- insert / query basic behaviour ----------

    @Test
    fun insert_validPayload_returnsUriWithEventId() {
        ShadowBinder.setCallingUid(Process.myUid())
        val event = validEvent(myPackage)
        val values = ContentValues().apply {
            put(CrashLogContract.COLUMN_PACKAGE_NAME, myPackage)
            put(CrashLogContract.COLUMN_PAYLOAD, event.toJsonLine())
        }
        val uri = provider.insert(CrashLogContract.EVENTS_URI, values)
        assertNotNull(uri)
        assertTrue(uri.toString().endsWith(event.id))
    }

    @Test
    fun insert_wrongUri_returnsNull() {
        ShadowBinder.setCallingUid(Process.myUid())
        val wrongUri = Uri.parse("content://${CrashLogContract.AUTHORITY}/wrong")
        val values = ContentValues().apply {
            put(CrashLogContract.COLUMN_PACKAGE_NAME, myPackage)
            put(CrashLogContract.COLUMN_PAYLOAD, validEvent(myPackage).toJsonLine())
        }
        assertNull(provider.insert(wrongUri, values))
    }

    @Test
    fun insert_missingPayload_returnsNull() {
        ShadowBinder.setCallingUid(Process.myUid())
        val values = ContentValues().apply {
            put(CrashLogContract.COLUMN_PACKAGE_NAME, myPackage)
        }
        assertNull(provider.insert(CrashLogContract.EVENTS_URI, values))
    }

    @Test
    fun insert_missingPackageName_returnsNull() {
        ShadowBinder.setCallingUid(Process.myUid())
        val values = ContentValues().apply {
            put(CrashLogContract.COLUMN_PAYLOAD, validEvent(myPackage).toJsonLine())
        }
        assertNull(provider.insert(CrashLogContract.EVENTS_URI, values))
    }

    @Test
    fun insert_untrustedCaller_returnsNull() {
        ShadowBinder.setCallingUid(foreignUid)
        val values = ContentValues().apply {
            put(CrashLogContract.COLUMN_PACKAGE_NAME, myPackage)
            put(CrashLogContract.COLUMN_PAYLOAD, validEvent(myPackage).toJsonLine())
        }
        assertNull(provider.insert(CrashLogContract.EVENTS_URI, values))
    }

    @Test
    fun query_returnsNull() {
        assertNull(provider.query(CrashLogContract.EVENTS_URI, null, null, null, null))
    }

    // ---------- helpers ----------

    private fun validEvent(pkg: String) = CrashEvent(
        id = "test-id-" + System.currentTimeMillis(),
        timestampMs = System.currentTimeMillis(),
        packageName = pkg,
        exceptionClass = "java.lang.RuntimeException",
        stackTrace = "at com.example.Foo.bar(Foo.java:42)",
    )
}
