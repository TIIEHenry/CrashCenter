package nota.android.crash.xp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopePolicyTest {

    private companion object {
        const val TEST_PKG = "com.example.app"
        const val SYSTEM_PKG = "com.android.settings"
        const val IGNORED_PKG = "android"
    }

    // ---------- Legacy mode (managedPackages == null) ----------

    @Test
    fun `legacy not disabled - install and intercept`() {
        val decision = evaluateLegacy(disabled = false)
        assertTrue(decision.shouldInstall)
        assertTrue(decision.shouldIntercept)
        assertTrue(decision.showNotify)
        assertTrue(decision.crashLogEnabled)
    }

    @Test
    fun `legacy disabled package - observe only`() {
        val decision = evaluateLegacy(disabled = true)
        assertTrue(decision.shouldInstall)
        assertFalse(decision.shouldIntercept)
        assertFalse(decision.showNotify)
        assertTrue(decision.crashLogEnabled)
    }

    @Test
    fun `legacy scopeMode on user app not disabled - install and intercept`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = true,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldInstall)
        assertTrue(decision.shouldIntercept)
    }

    @Test
    fun `legacy scopeMode on disabled list - observe only`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = true,
            handleSystem = false,
            packageListDisabled = true,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldInstall)
        assertFalse(decision.shouldIntercept)
    }

    @Test
    fun `legacy scopeMode on system without handleSystem - no install`() {
        val decision = ScopePolicy.evaluate(
            packageName = SYSTEM_PKG,
            isSystemApp = true,
            scopeMode = true,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertFalse(decision.shouldInstall)
    }

    @Test
    fun `legacy scopeMode on system with handleSystem - install and intercept`() {
        val decision = ScopePolicy.evaluate(
            packageName = SYSTEM_PKG,
            isSystemApp = true,
            scopeMode = true,
            handleSystem = true,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldInstall)
        assertTrue(decision.shouldIntercept)
        assertTrue(decision.showNotify)
    }

    // ---------- Managed mode ----------

    @Test
    fun `managed package not in list - observe only`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf("other.package"),
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldInstall)
        assertFalse(decision.shouldIntercept)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed in list but no enabled rules - observe only`() {
        val json = """{"$TEST_PKG":{"rules":[],"updatedAt":0}}"""
        val decision = evaluateManaged(json)
        assertTrue(decision.shouldInstall)
        assertFalse(decision.shouldIntercept)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed with enabled rules - install and intercept`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decision = evaluateManaged(json)
        assertTrue(decision.shouldInstall)
        assertTrue(decision.shouldIntercept)
        assertTrue(decision.showNotify)
        assertTrue(decision.crashLogEnabled)
    }

    @Test
    fun `managed system in scopeMode without handleSystem - no install`() {
        val json = """{"$SYSTEM_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = SYSTEM_PKG,
            isSystemApp = true,
            scopeMode = true,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(SYSTEM_PKG),
            interventionRulesJson = json,
        )
        assertFalse(decision.shouldInstall)
    }

    @Test
    fun `managed showNotify false when rule explicitly false`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true,"showNotify":false}],"updatedAt":0}}"""
        val decision = evaluateManaged(json)
        assertTrue(decision.shouldIntercept)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed crashLogEnabled false when rule explicitly false`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true,"crashLogEnabled":false}],"updatedAt":0}}"""
        val decision = evaluateManaged(json)
        assertTrue(decision.shouldIntercept)
        assertFalse(decision.crashLogEnabled)
    }

    @Test
    fun `managed empty managed set - observe only for any package`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = emptySet(),
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldInstall)
        assertFalse(decision.shouldIntercept)
    }

    @Test
    fun `managed disabled rules - observe only`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":false}],"updatedAt":0}}"""
        val decision = evaluateManaged(json)
        assertTrue(decision.shouldInstall)
        assertFalse(decision.shouldIntercept)
    }

    // ---------- Ignored packages ----------

    @Test
    fun `ignored packages never install`() {
        val ignoredPackages = listOf(
            "android",
            "de.robv.android.xposed.installer",
            "org.meowcat.edxposed.manager",
            "org.lsposed.manager",
        )
        for (pkg in ignoredPackages) {
            val decision = ScopePolicy.evaluate(
                packageName = pkg,
                isSystemApp = false,
                scopeMode = false,
                handleSystem = false,
                packageListDisabled = false,
                managedPackages = setOf(pkg),
                interventionRulesJson = """{"$pkg":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}""",
            )
            assertFalse("Package $pkg should be ignored", decision.shouldInstall)
            assertFalse(decision.shouldIntercept)
        }
    }

    // ---------- Edge cases ----------

    @Test
    fun `managed invalid JSON - observe only`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = "not json",
        )
        assertTrue(decision.shouldInstall)
        assertFalse(decision.shouldIntercept)
    }

    @Test
    fun `managed mixed enabled rules - intercept`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":false},{"id":"r2","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decision = evaluateManaged(json)
        assertTrue(decision.shouldIntercept)
    }

    private fun evaluateLegacy(disabled: Boolean): ScopeDecision =
        ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = disabled,
            managedPackages = null,
            interventionRulesJson = "{}",
        )

    private fun evaluateManaged(interventionRulesJson: String): ScopeDecision =
        ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = interventionRulesJson,
        )
}
