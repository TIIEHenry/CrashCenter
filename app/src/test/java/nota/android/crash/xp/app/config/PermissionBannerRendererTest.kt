package nota.android.crash.xp.app.config

import nota.android.crash.xp.app.PackageVisibilityHelper
import nota.android.crash.xp.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionBannerRendererTest {

    private fun status(visiblePackageCount: Int, needsUserAction: Boolean = true) =
        PackageVisibilityHelper.Status(
            hasFullVisibility = false,
            permissionGranted = false,
            needsUserAction = needsUserAction,
            visiblePackageCount = visiblePackageCount,
        )

    // -- compact plural (module inactive) --

    @Test
    fun `visiblePackageCount greater than 0 and module inactive uses compact plural`() {
        val decision = decideTitle(status(visiblePackageCount = 5), isModuleActive = false)

        assertTrue(decision.isPlural)
        assertEquals(R.plurals.permission_list_partial_compact, decision.pluralResId)
        assertEquals(5, decision.count)
        assertTrue(decision.compact)
    }

    // -- regular plural (module active) --

    @Test
    fun `visiblePackageCount greater than 0 and module active uses regular plural`() {
        val decision = decideTitle(status(visiblePackageCount = 3), isModuleActive = true)

        assertTrue(decision.isPlural)
        assertEquals(R.plurals.permission_list_partial, decision.pluralResId)
        assertEquals(3, decision.count)
        assertFalse(decision.compact)
    }

    // -- zero visible packages --

    @Test
    fun `visiblePackageCount zero and module inactive uses compact string resource`() {
        val decision = decideTitle(status(visiblePackageCount = 0), isModuleActive = false)

        assertFalse(decision.isPlural)
        assertEquals(R.string.permission_banner_title_compact, decision.stringResId)
        assertTrue(decision.compact)
    }

    @Test
    fun `visiblePackageCount zero and module active uses regular string resource`() {
        val decision = decideTitle(status(visiblePackageCount = 0), isModuleActive = true)

        assertFalse(decision.isPlural)
        assertEquals(R.string.permission_banner_title, decision.stringResId)
        assertFalse(decision.compact)
    }
}
