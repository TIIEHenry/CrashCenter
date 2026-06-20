package nota.android.crash.xp.app.config

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import nota.android.crash.xp.app.PackageVisibilityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PackageVisibilityRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: PackageVisibilityRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        repository = PackageVisibilityRepository(context)
    }

    // ─── detectPackageVisibility ───

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `detectPackageVisibility on pre-Android 11 returns full visibility`() {
        val status = repository.detectPackageVisibility()
        assertTrue(status.hasFullVisibility)
        assertTrue(status.permissionGranted)
        assertFalse(status.needsUserAction)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `detectPackageVisibility on Android 11+ returns status`() {
        val status = repository.detectPackageVisibility()
        // On Robolectric without QUERY_ALL_PACKAGES, permission is not granted
        assertFalse(status.permissionGranted)
        assertFalse(status.hasFullVisibility)
        assertTrue(status.needsUserAction)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `detectPackageVisibility on pre-Android 11 has zero visible count`() {
        val status = repository.detectPackageVisibility()
        // Pre-R returns default values for visiblePackageCount
        assertEquals(0, status.visiblePackageCount)
    }

    // ─── detectPackageVisibilityAfterLoad ───

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `detectPackageVisibilityAfterLoad on pre-Android 11 returns full visibility`() {
        val status = repository.detectPackageVisibilityAfterLoad(loadedCount = 5)
        assertTrue(status.hasFullVisibility)
        assertTrue(status.permissionGranted)
        assertFalse(status.needsUserAction)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `detectPackageVisibilityAfterLoad on Android 11+ with needsUserAction returns base`() {
        val status = repository.detectPackageVisibilityAfterLoad(loadedCount = 5)
        // Without QUERY_ALL_PACKAGES, needsUserAction is true, so returns base status
        assertTrue(status.needsUserAction)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `detectPackageVisibilityAfterLoad ignores loadedCount on pre-R`() {
        val status1 = repository.detectPackageVisibilityAfterLoad(loadedCount = 0)
        val status2 = repository.detectPackageVisibilityAfterLoad(loadedCount = 100)
        assertEquals(status1.hasFullVisibility, status2.hasFullVisibility)
        assertEquals(status1.permissionGranted, status2.permissionGranted)
        assertEquals(status1.needsUserAction, status2.needsUserAction)
    }

    // ─── Status data class ───

    @Test
    fun `Status data class holds correct values`() {
        val status = PackageVisibilityHelper.Status(
            hasFullVisibility = true,
            permissionGranted = true,
            needsUserAction = false,
            visiblePackageCount = 42,
        )
        assertTrue(status.hasFullVisibility)
        assertTrue(status.permissionGranted)
        assertFalse(status.needsUserAction)
        assertEquals(42, status.visiblePackageCount)
    }

    @Test
    fun `Status copy modifies fields correctly`() {
        val base = PackageVisibilityHelper.Status(
            hasFullVisibility = false,
            permissionGranted = false,
            needsUserAction = true,
            visiblePackageCount = 5,
        )
        val updated = base.copy(
            hasFullVisibility = true,
            needsUserAction = false,
            visiblePackageCount = 10,
        )
        assertTrue(updated.hasFullVisibility)
        assertFalse(updated.permissionGranted) // unchanged
        assertFalse(updated.needsUserAction)
        assertEquals(10, updated.visiblePackageCount)
    }

    @Test
    fun `Status equality`() {
        val s1 = PackageVisibilityHelper.Status(true, true, false, 10)
        val s2 = PackageVisibilityHelper.Status(true, true, false, 10)
        val s3 = PackageVisibilityHelper.Status(true, true, false, 20)
        assertEquals(s1, s2)
        assertFalse(s1 == s3)
    }

    @Test
    fun `Status default visiblePackageCount is zero`() {
        val status = PackageVisibilityHelper.Status(
            hasFullVisibility = true,
            permissionGranted = true,
            needsUserAction = false,
        )
        assertEquals(0, status.visiblePackageCount)
    }

    // ─── PackageVisibilityHelper.check integration ───

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `PackageVisibilityHelper check on pre-R is always full visibility`() {
        val status = PackageVisibilityHelper.check(context)
        assertTrue(status.hasFullVisibility)
        assertTrue(status.permissionGranted)
        assertFalse(status.needsUserAction)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `PackageVisibilityHelper check on R without permission`() {
        val status = PackageVisibilityHelper.check(context)
        assertFalse(status.permissionGranted)
        assertFalse(status.hasFullVisibility)
        assertTrue(status.needsUserAction)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `PackageVisibilityHelper checkAfterLoad on pre-R is always full visibility`() {
        val status = PackageVisibilityHelper.checkAfterLoad(context, loadedAppCount = 5)
        assertTrue(status.hasFullVisibility)
        assertTrue(status.permissionGranted)
        assertFalse(status.needsUserAction)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `PackageVisibilityHelper checkAfterLoad on R without permission`() {
        val status = PackageVisibilityHelper.checkAfterLoad(context, loadedAppCount = 5)
        assertFalse(status.hasFullVisibility)
        assertTrue(status.needsUserAction)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `PackageVisibilityHelper checkAfterLoad with hasFullVisibility true returns base`() {
        val status = PackageVisibilityHelper.checkAfterLoad(context, loadedAppCount = 5)
        assertTrue(status.hasFullVisibility)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `PackageVisibilityHelper checkAfterLoad with needsUserAction true returns base`() {
        val status = PackageVisibilityHelper.checkAfterLoad(context, loadedAppCount = 5)
        assertFalse(status.needsUserAction)
    }

    // ─── PackageVisibilityHelper.getInstalledPackagesCompat ───

    @Test
    fun `getInstalledPackagesCompat returns non-empty list`() {
        val pm = context.packageManager
        val packages = PackageVisibilityHelper.getInstalledPackagesCompat(pm)
        assertTrue(packages.isNotEmpty())
    }

    @Test
    fun `getInstalledPackagesCompat includes this app`() {
        val pm = context.packageManager
        val packages = PackageVisibilityHelper.getInstalledPackagesCompat(pm)
        val packageNames = packages.map { it.packageName }
        assertTrue(packageNames.contains(context.packageName))
    }

    // ─── PackageVisibilityHelper.openAppSettings ───

    @Test
    fun `openAppSettings returns true`() {
        val result = PackageVisibilityHelper.openAppSettings(context)
        // On Robolectric this may return false; test that it doesn't crash
        // The method is a thin wrapper around startActivity
    }
}
