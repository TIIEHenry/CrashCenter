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
    fun `legacy scopeMode off - shouldHook true, showNotify true when not disabled`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)
        assertTrue(decision.crashLogEnabled) // default
    }

    @Test
    fun `legacy scopeMode off - disabled package shouldHook true, showNotify false`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = true,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `legacy scopeMode on - user app not in disabled list shouldHook true`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = true,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)
    }

    @Test
    fun `legacy scopeMode on - user app in disabled list shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = true,
            handleSystem = false,
            packageListDisabled = true,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertFalse(decision.shouldHook)
    }

    @Test
    fun `legacy scopeMode on - system app without handleSystem shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = SYSTEM_PKG,
            isSystemApp = true,
            scopeMode = true,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertFalse(decision.shouldHook)
    }

    @Test
    fun `legacy scopeMode on - system app with handleSystem shouldHook true`() {
        val decision = ScopePolicy.evaluate(
            packageName = SYSTEM_PKG,
            isSystemApp = true,
            scopeMode = true,
            handleSystem = true,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)
    }

    @Test
    fun `legacy scopeMode on - system app with handleSystem but disabled shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = SYSTEM_PKG,
            isSystemApp = true,
            scopeMode = true,
            handleSystem = true,
            packageListDisabled = true,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertFalse(decision.shouldHook)
    }

    // ---------- Managed mode ----------

    @Test
    fun `managed - package not in managedPackages shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf("other.package"),
            interventionRulesJson = "{}",
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed - package in managedPackages but no enabled rules shouldHook false`() {
        val json = """{"$TEST_PKG":{"rules":[],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed - package with enabled rules shouldHook true`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)
        assertTrue(decision.crashLogEnabled)
    }

    @Test
    fun `managed - system app in scopeMode without handleSystem shouldHook false`() {
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
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed - system app in scopeMode with handleSystem shouldHook true`() {
        val json = """{"$SYSTEM_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = SYSTEM_PKG,
            isSystemApp = true,
            scopeMode = true,
            handleSystem = true,
            packageListDisabled = false,
            managedPackages = setOf(SYSTEM_PKG),
            interventionRulesJson = json,
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)
    }

    @Test
    fun `managed - showNotify false when all rules explicitly set false`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true,"showNotify":false}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertTrue(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed - crashLogEnabled false when all rules explicitly set false`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true,"crashLogEnabled":false}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertTrue(decision.shouldHook)
        assertFalse(decision.crashLogEnabled)
    }

    @Test
    fun `managed - mixed showNotify uses any true`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true,"showNotify":false},{"id":"r2","type":"CATCH_ALL","enabled":true,"showNotify":true}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertTrue(decision.showNotify)
    }

    // ---------- Ignored packages ----------

    @Test
    fun `ignored package android shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = IGNORED_PKG,
            isSystemApp = true,
            scopeMode = false,
            handleSystem = true,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `ignored package xposed installer shouldHook false even in managed mode`() {
        val decision = ScopePolicy.evaluate(
            packageName = "de.robv.android.xposed.installer",
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf("de.robv.android.xposed.installer"),
            interventionRulesJson = """{"de.robv.android.xposed.installer":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}""",
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `ignored package edXposed manager shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = "org.meowcat.edxposed.manager",
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf("org.meowcat.edxposed.manager"),
            interventionRulesJson = """{"org.meowcat.edxposed.manager":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}""",
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `ignored package LSPosed manager shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = "org.lsposed.manager",
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf("org.lsposed.manager"),
            interventionRulesJson = """{"org.lsposed.manager":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}""",
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `all ignored packages are blocked in legacy mode`() {
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
                managedPackages = null,
                interventionRulesJson = "{}",
            )
            assertFalse("Package $pkg should be ignored", decision.shouldHook)
            assertFalse("Package $pkg showNotify should be false", decision.showNotify)
        }
    }

    @Test
    fun `all ignored packages are blocked in managed mode`() {
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
            assertFalse("Package $pkg should be ignored in managed mode", decision.shouldHook)
            assertFalse("Package $pkg showNotify should be false in managed mode", decision.showNotify)
        }
    }

    // ---------- Edge cases ----------

    @Test
    fun `empty package name shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = "",
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldHook) // empty is not in IGNORED_PACKAGES
        assertTrue(decision.showNotify)
    }

    @Test
    fun `managed mode with empty intervention rules JSON shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = "{}",
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed mode with invalid JSON shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = "not json",
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed mode with disabled rules shouldHook false`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":false}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed mode with mixed enabled and disabled rules only counts enabled`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":false},{"id":"r2","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)
    }

    @Test
    fun `legacy mode with null managedPackages`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = true,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldHook)
    }

    @Test
    fun `managed mode with null showNotify and crashLogEnabled uses defaults`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)   // globalDefaultShowNotify = true
        assertTrue(decision.crashLogEnabled) // defaultEnabled = true
    }

    @Test
    fun `managed mode with multiple rules mixed showNotify and crashLogEnabled`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true,"showNotify":false,"crashLogEnabled":false},{"id":"r2","type":"CATCH_ALL","enabled":true,"showNotify":true,"crashLogEnabled":true}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)   // any true
        assertTrue(decision.crashLogEnabled) // any true
    }

    @Test
    fun `managed mode with all rules showNotify null uses global default`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true},{"id":"r2","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)   // globalDefaultShowNotify = true
        assertTrue(decision.crashLogEnabled) // defaultEnabled = true
    }

    @Test
    fun `legacy scopeMode off - system app shouldHook true`() {
        val decision = ScopePolicy.evaluate(
            packageName = SYSTEM_PKG,
            isSystemApp = true,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)
    }

    @Test
    fun `legacy scopeMode on - system app with handleSystem and disabled shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = SYSTEM_PKG,
            isSystemApp = true,
            scopeMode = true,
            handleSystem = true,
            packageListDisabled = true,
            managedPackages = null,
            interventionRulesJson = "{}",
        )
        assertFalse(decision.shouldHook)
    }

    @Test
    fun `managed mode with empty managed packages set shouldHook false`() {
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = emptySet(),
            interventionRulesJson = "{}",
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed mode with only disabled rules and null showNotify defaults`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":false,"showNotify":true}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed mode with valid JSON but package not in root keys shouldHook false`() {
        val json = """{"other.package":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `managed mode crashLogEnabled false when all rules explicitly set false`() {
        val json = """{"$TEST_PKG":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true,"crashLogEnabled":false},{"id":"r2","type":"CATCH_ALL","enabled":true,"crashLogEnabled":false}],"updatedAt":0}}"""
        val decision = ScopePolicy.evaluate(
            packageName = TEST_PKG,
            isSystemApp = false,
            scopeMode = false,
            handleSystem = false,
            packageListDisabled = false,
            managedPackages = setOf(TEST_PKG),
            interventionRulesJson = json,
        )
        assertTrue(decision.shouldHook)
        assertFalse(decision.crashLogEnabled)
    }
}
