package nota.android.crash.xp.app.config

import android.content.Context
import nota.android.crash.xp.PrefManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PackageInfoLoaderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    // ─── passesSystemFilter ───

    @Test
    fun `passesSystemFilter returns true for non-system app`() {
        assertTrue(PackageInfoLoader.passesSystemFilter(isSystemApp = false, handleSystem = false))
        assertTrue(PackageInfoLoader.passesSystemFilter(isSystemApp = false, handleSystem = true))
    }

    @Test
    fun `passesSystemFilter returns true for system app when handleSystem true`() {
        assertTrue(PackageInfoLoader.passesSystemFilter(isSystemApp = true, handleSystem = true))
    }

    @Test
    fun `passesSystemFilter returns false for system app when handleSystem false`() {
        assertFalse(PackageInfoLoader.passesSystemFilter(isSystemApp = true, handleSystem = false))
    }

    @Test
    fun `passesSystemFilter all combinations`() {
        val cases = listOf(
            Triple(false, false, true),
            Triple(false, true, true),
            Triple(true, false, false),
            Triple(true, true, true),
        )
        for ((isSystem, handleSystem, expected) in cases) {
            assertEquals(
                "Failed for isSystem=$isSystem, handleSystem=$handleSystem",
                expected,
                PackageInfoLoader.passesSystemFilter(isSystem, handleSystem),
            )
        }
    }

    // ─── isSystemPackage ───

    @Test
    fun `isSystemPackage returns false for unknown package`() {
        assertFalse(PackageInfoLoader.isSystemPackage(context, "com.nonexistent.package"))
    }

    @Test
    fun `isSystemPackage returns false for self package`() {
        assertFalse(PackageInfoLoader.isSystemPackage(context, context.packageName))
    }

    // ─── enumerateInstalledPackageNames ───

    @Test
    fun `enumerateInstalledPackageNames returns non-empty set`() {
        val names = PackageInfoLoader.enumerateInstalledPackageNames(context)
        assertTrue(names.isNotEmpty())
        assertFalse(names.contains(PrefManager.ITSELF))
    }

    @Test
    fun `enumerateInstalledPackageNames excludes self`() {
        val names = PackageInfoLoader.enumerateInstalledPackageNames(context)
        assertFalse(names.contains(PrefManager.ITSELF))
    }
}
