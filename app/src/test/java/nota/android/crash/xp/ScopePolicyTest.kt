package nota.android.crash.xp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopePolicyTest {

    private companion object {
        const val TEST_PKG = "com.example.app"
        const val SYSTEM_PKG = "com.android.settings"
    }

    @Test
    fun `intercept enabled user app installs and intercepts`() {
        val decision = evaluate(TEST_PKG, isSystemApp = false, handleSystem = false, interceptEnabled = true)
        assertTrue(decision.shouldInstall)
        assertTrue(decision.shouldIntercept)
        assertTrue(decision.showNotify)
        assertTrue(decision.crashLogEnabled)
    }

    @Test
    fun `intercept disabled user app observes only`() {
        val decision = evaluate(TEST_PKG, isSystemApp = false, handleSystem = false, interceptEnabled = false)
        assertTrue(decision.shouldInstall)
        assertFalse(decision.shouldIntercept)
        assertFalse(decision.showNotify)
        assertTrue(decision.crashLogEnabled)
    }

    @Test
    fun `system app without handleSystem is not installed`() {
        val decision = evaluate(SYSTEM_PKG, isSystemApp = true, handleSystem = false, interceptEnabled = true)
        assertFalse(decision.shouldInstall)
        assertFalse(decision.shouldIntercept)
    }

    @Test
    fun `system app with handleSystem and intercept enabled`() {
        val decision = evaluate(SYSTEM_PKG, isSystemApp = true, handleSystem = true, interceptEnabled = true)
        assertTrue(decision.shouldInstall)
        assertTrue(decision.shouldIntercept)
        assertTrue(decision.showNotify)
    }

    @Test
    fun `ignored packages never install`() {
        val ignoredPackages = listOf(
            "android",
            "de.robv.android.xposed.installer",
            "org.meowcat.edxposed.manager",
            "org.lsposed.manager",
        )
        for (pkg in ignoredPackages) {
            val decision = evaluate(pkg, isSystemApp = false, handleSystem = true, interceptEnabled = true)
            assertFalse("Package $pkg should be ignored", decision.shouldInstall)
            assertFalse(decision.shouldIntercept)
        }
    }

    private fun evaluate(
        packageName: String,
        isSystemApp: Boolean,
        handleSystem: Boolean,
        interceptEnabled: Boolean,
    ): ScopeDecision = ScopePolicy.evaluate(
        packageName = packageName,
        isSystemApp = isSystemApp,
        handleSystem = handleSystem,
        interceptEnabled = interceptEnabled,
    )
}
