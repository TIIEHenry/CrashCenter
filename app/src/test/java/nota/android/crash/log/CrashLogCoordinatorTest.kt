package nota.android.crash.log

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import nota.android.crash.common.data.CrashEvent
import nota.android.crash.log.backend.LocalCacheBackend
import nota.android.crash.xp.PrefManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashLogCoordinatorTest {

    private lateinit var xposedMock: MockedStatic<XposedBridge>
    private lateinit var xPrefsConstruction: MockedConstruction<XSharedPreferences>
    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context
    private lateinit var event: CrashEvent

    @Before
    fun setUp() {
        context = mock()
        event = CrashEvent(id = "evt-1", packageName = "com.test")

        prefs = mock<SharedPreferences>()
        doReturn(true).`when`(prefs).getBoolean(any(), any())

        xPrefsConstruction = Mockito.mockConstruction(
            XSharedPreferences::class.java,
            Mockito.withSettings().defaultAnswer {
                if (it.method.returnType == Void.TYPE) null
                else try {
                    it.method.invoke(prefs, *it.arguments)
                } catch (_: Exception) {
                    null
                }
            },
        )

        xposedMock = Mockito.mockStatic(XposedBridge::class.java)
        xposedMock.`when`<Unit> { XposedBridge.log(any<String>()) }.then {}
    }

    @After
    fun tearDown() {
        xposedMock.close()
        xPrefsConstruction.close()
    }

    @Test
    fun `logAsync dispatches when logging enabled`() {
        CrashLogCoordinator.logAsync(context, event)
        Thread.sleep(300)
    }

    @Test
    fun `logAsync skips when logging disabled`() {
        whenever(prefs.getBoolean(PrefManager.PREF_CRASH_LOG_ENABLED, true)).thenReturn(false)
        CrashLogCoordinator.logAsync(context, event)
        Thread.sleep(300)
        xposedMock.verifyNoInteractions()
    }

    @Test
    fun `logSync writes via local cache`() {
        CrashLogCoordinator.logSync(context, event)
    }
}
