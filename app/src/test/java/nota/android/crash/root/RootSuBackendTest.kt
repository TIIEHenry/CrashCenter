package nota.android.crash.root

import android.content.Context
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.AppendResult
import nota.android.crash.log.BackendAvailability
import nota.android.crash.log.BackendId
import nota.android.crash.log.backend.RootSuBackend
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import de.robv.android.xposed.XposedBridge

@RunWith(RobolectricTestRunner::class)
class RootSuBackendTest {

    private lateinit var context: Context
    private lateinit var mockAdapter: RootAccessClient
    private lateinit var originalAdapter: RootAccessClient
    private lateinit var xposedMock: MockedStatic<XposedBridge>

    private val event = CrashEvent(
        id = "test-1",
        packageName = "com.test.app",
        timestampMs = 1000L,
    )

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockAdapter = mock()
        originalAdapter = RootSuBackend.adapter
        RootSuBackend.adapter = mockAdapter
        xposedMock = Mockito.mockStatic(XposedBridge::class.java)
        xposedMock.`when`<Unit> { XposedBridge.log(any<String>()) }.then {}
    }

    @After
    fun tearDown() {
        RootSuBackend.adapter = originalAdapter
        xposedMock.close()
    }

    // --- id and metadata ---

    @Test
    fun `id is ROOT_SU`() {
        assertEquals(BackendId.ROOT_SU, RootSuBackend.id)
    }

    @Test
    fun `tier is 0`() {
        assertEquals(0, RootSuBackend.tier)
    }

    // --- probe ---

    @Test
    fun `probe returns READY when adapter reports AVAILABLE`() {
        whenever(mockAdapter.probe()).thenReturn(RootAvailability.AVAILABLE)

        assertEquals(BackendAvailability.READY, RootSuBackend.probe(context))
    }

    @Test
    fun `probe returns MAYBE when adapter reports DENIED`() {
        whenever(mockAdapter.probe()).thenReturn(RootAvailability.DENIED)

        assertEquals(BackendAvailability.MAYBE, RootSuBackend.probe(context))
    }

    @Test
    fun `probe returns UNAVAILABLE when adapter reports UNAVAILABLE`() {
        whenever(mockAdapter.probe()).thenReturn(RootAvailability.UNAVAILABLE)

        assertEquals(BackendAvailability.UNAVAILABLE, RootSuBackend.probe(context))
    }

    @Test
    fun `probe returns UNAVAILABLE when adapter throws`() {
        whenever(mockAdapter.probe()).thenThrow(RuntimeException("error"))

        assertEquals(BackendAvailability.UNAVAILABLE, RootSuBackend.probe(context))
    }

    // --- append ---

    @Test
    fun `append returns Success when adapter writes successfully`() {
        mockAdapter.stub {
            onBlocking { appendBytes(any(), any(), eq(1500L)) }.thenReturn(true)
        }

        val result = RootSuBackend.append(context, event, 2000L)

        assertTrue(result is AppendResult.Success)
    }

    @Test
    fun `append returns Failure when adapter write fails`() {
        mockAdapter.stub {
            onBlocking { appendBytes(any(), any(), eq(1500L)) }.thenReturn(false)
        }

        val result = RootSuBackend.append(context, event, 2000L)

        assertTrue(result is AppendResult.Failure)
    }

    @Test
    fun `append uses 1500ms deadline regardless of input`() {
        mockAdapter.stub {
            onBlocking { appendBytes(any(), any(), eq(1500L)) }.thenReturn(true)
        }

        val result = RootSuBackend.append(context, event, 5000L)

        assertTrue(result is AppendResult.Success)
    }
}
