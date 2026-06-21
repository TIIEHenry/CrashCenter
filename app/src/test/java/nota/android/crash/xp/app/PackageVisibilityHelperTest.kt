package nota.android.crash.xp.app

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class PackageVisibilityHelperTest {

    private lateinit var context: Context
    private lateinit var pm: PackageManager
    private lateinit var shadowPm: ShadowPackageManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        pm = context.packageManager
        shadowPm = Shadows.shadowOf(pm)
        Shadows.shadowOf(context as Application).grantPermissions(android.Manifest.permission.QUERY_ALL_PACKAGES)
    }

    // ─── check() with sufficient packages ───

    @Test
    fun `check with sufficient packages returns no warning`() {
        registerAppPackages(20)
        registerProbePackage()

        val status = PackageVisibilityHelper.check(context)

        assertTrue(status.permissionGranted)
        assertTrue(status.hasFullVisibility)
        assertFalse(status.needsUserAction)
    }

    // ─── check() with too few packages ───

    @Test
    fun `check with too few packages returns warning`() {
        registerAppPackages(3)
        registerProbePackage()

        val status = PackageVisibilityHelper.check(context)

        assertTrue(status.permissionGranted)
        assertFalse(status.hasFullVisibility)
        assertTrue(status.needsUserAction)
    }

    // ─── check() with invisible probe ───

    @Test
    fun `check with invisible probe returns warning`() {
        registerAppPackages(20)
        // Do NOT register probe package

        val status = PackageVisibilityHelper.check(context)

        assertTrue(status.permissionGranted)
        assertFalse(status.hasFullVisibility)
        assertTrue(status.needsUserAction)
    }

    // ─── checkAfterLoad() with complete list ───

    @Test
    fun `checkAfterLoad with complete list returns ok`() {
        registerAppPackages(10)
        registerProbePackage()

        val status = PackageVisibilityHelper.checkAfterLoad(context, loadedAppCount = 20)

        assertTrue(status.permissionGranted)
        assertTrue(status.hasFullVisibility)
        assertFalse(status.needsUserAction)
    }

    // ─── checkAfterLoad() with likely incomplete list ───

    @Test
    fun `checkAfterLoad with incomplete list returns warning`() {
        registerAppPackages(3)
        registerProbePackage()

        val status = PackageVisibilityHelper.checkAfterLoad(context, loadedAppCount = 1)

        assertFalse(status.hasFullVisibility)
        assertTrue(status.needsUserAction)
    }

    // ─── check() on pre-Android 11 ───

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `check on pre-Android 11 returns full visibility without warning`() {
        val status = PackageVisibilityHelper.check(context)

        assertTrue(status.hasFullVisibility)
        assertTrue(status.permissionGranted)
        assertFalse(status.needsUserAction)
        assertEquals(0, status.visiblePackageCount)
    }

    // ─── checkAfterLoad() on pre-Android 11 ───

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `checkAfterLoad on pre-Android 11 returns full visibility`() {
        val status = PackageVisibilityHelper.checkAfterLoad(context, loadedAppCount = 0)

        assertTrue(status.hasFullVisibility)
        assertTrue(status.permissionGranted)
        assertFalse(status.needsUserAction)
    }

    // ─── Threshold boundary: no launcher activities ───
    // MIN_PACKAGES_WITHOUT_LAUNCHER = 8
    // Robolectric auto-includes the test app in getInstalledPackages count.

    @Test
    fun `check threshold below 8 packages without launcher triggers warning`() {
        registerAppPackages(5) // total = 5 + probe + default app = 7 < 8
        registerProbePackage()

        val status = PackageVisibilityHelper.check(context)

        assertFalse(status.hasFullVisibility)
        assertTrue(status.needsUserAction)
    }

    @Test
    fun `check threshold at 8 packages without launcher passes`() {
        registerAppPackages(6) // total = 6 + probe + default app = 8 >= 8
        registerProbePackage()

        val status = PackageVisibilityHelper.check(context)

        assertTrue(status.hasFullVisibility)
        assertFalse(status.needsUserAction)
    }

    // ─── Threshold boundary: with launcher activities ───
    // MIN_PACKAGES_WITH_LAUNCHER = 10
    // threshold = max(10, launcherCount / 2)
    // Note: addResolveInfoForIntent registers intents that are matched by
    // queryIntentActivities, so launcher activities ARE counted.

    @Test
    fun `check threshold with launcher activities passes at boundary`() {
        registerAppPackages(10)
        registerProbePackage()
        registerLauncherActivities(5)

        val status = PackageVisibilityHelper.check(context)

        // With 5 launcher activities: threshold = max(10, 5/2) = 10
        // Total packages = 10 + probe + default = 12 >= 10 → passes
        assertTrue(status.hasFullVisibility)
        assertFalse(status.needsUserAction)
    }

    @Test
    fun `check threshold with launcher activities fails below boundary`() {
        registerAppPackages(6) // total = 6 + probe + default = 8 < 10
        registerProbePackage()
        registerLauncherActivities(5)

        val status = PackageVisibilityHelper.check(context)

        // With 5 launcher activities: threshold = max(10, 5/2) = 10
        // Total packages = 8 < 10 → fails
        assertFalse(status.hasFullVisibility)
        assertTrue(status.needsUserAction)
    }

    // ─── visiblePackageCount field ───

    @Test
    fun `check populates visiblePackageCount correctly`() {
        registerAppPackages(15)
        registerProbePackage()

        val status = PackageVisibilityHelper.check(context)

        // visiblePackageCount = registered packages + probe + default app
        assertTrue(status.visiblePackageCount >= 15)
    }

    // ─── Helpers ───

    private fun registerAppPackages(count: Int) {
        for (i in 1..count) {
            val info = PackageInfo().apply {
                packageName = "com.test.app$i"
            }
            shadowPm.addPackage(info)
        }
    }

    private fun registerProbePackage() {
        val info = PackageInfo().apply {
            packageName = "com.android.settings"
        }
        shadowPm.addPackage(info)
    }

    private fun registerLauncherActivities(count: Int) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val resolveInfos = (1..count).map { i ->
            android.content.pm.ResolveInfo().apply {
                activityInfo = android.content.pm.ActivityInfo().apply {
                    packageName = "com.test.launcher$i"
                    name = "LauncherActivity$i"
                }
            }
        }
        shadowPm.addResolveInfoForIntent(intent, resolveInfos)
    }
}
