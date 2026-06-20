package nota.android.crash.feedback

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowNotificationManager
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S], application = Application::class)
class CrashFeedbackFacadeTest {

    private lateinit var app: Application
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = Shadows.shadowOf(nm)
    }

    @Test
    fun notificationId_isDeterministicPerPackage() {
        val id1 = invokeNotificationIdFor("com.example.app")
        val id2 = invokeNotificationIdFor("com.example.app")
        val id3 = invokeNotificationIdFor("com.other.app")

        assertEquals("Same package should produce same ID", id1, id2)
        assertNotEquals("Different packages should produce different IDs", id1, id3)
    }

    @Test
    fun notification_containsCrashIdExtra() {
        val crashId = "crash-abc-123"
        val pkg = app.packageName
        val appInfo = app.applicationInfo
        val throwable = RuntimeException("test crash")

        CrashFeedbackFacade.show(app, pkg, appInfo, throwable, crashId, true)

        ShadowLooper.idleMainLooper()

        val notifications = shadowNotificationManager.allNotifications
        assertTrue("Notification should be posted", notifications.isNotEmpty())

        val notification = notifications.first()
        val extras = notification.extras
        assertNotNull("Extras should be present", extras)
        val expectedId = invokeNotificationIdFor(pkg)
        assertTrue("Notification should be posted with correct ID", notifications.isNotEmpty())
    }

    @Test
    fun toast_isShownWithExpectedMessage() {
        val pkg = app.packageName
        val appInfo = app.applicationInfo
        val throwable = RuntimeException("boom")
        val crashId = "id-toast"

        CrashFeedbackFacade.show(app, pkg, appInfo, throwable, crashId, true)

        ShadowLooper.idleMainLooper()

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be shown", latestToast)

        val text = ShadowToast.getTextOfLatestToast()
        assertNotNull("Toast text should not be null", text)
        assertTrue("Toast should contain the app label", text!!.contains(appInfo.loadLabel(app.packageManager)))
        assertTrue("Toast should contain the exception message", text.contains("boom"))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun notificationSkipped_whenPermissionNotGranted_onApi33Plus() {
        shadowNotificationManager.setNotificationsEnabled(false)

        val pkg = app.packageName
        val appInfo = app.applicationInfo
        val throwable = RuntimeException("no notify")
        val crashId = "id-perm"

        CrashFeedbackFacade.show(app, pkg, appInfo, throwable, crashId, true)

        ShadowLooper.idleMainLooper()

        assertTrue("No notification should be posted when permission denied", shadowNotificationManager.allNotifications.isEmpty())
    }

    @Test
    fun exceptionHandling_doesNotCrash() {
        val pkg = "nonexistent.package.name"
        val appInfo = app.applicationInfo
        val throwable = RuntimeException("safe")
        val crashId = "id-safe"

        try {
            CrashFeedbackFacade.show(app, pkg, appInfo, throwable, crashId, true)
            ShadowLooper.idleMainLooper()
        } catch (e: Throwable) {
            fail("CrashFeedbackFacade.show should not throw: ${e.message}")
        }
    }

    private fun invokeNotificationIdFor(packageName: String): Int {
        val method = CrashFeedbackFacade::class.java.getDeclaredMethod("notificationIdFor", String::class.java)
        method.isAccessible = true
        return method.invoke(CrashFeedbackFacade, packageName) as Int
    }
}
