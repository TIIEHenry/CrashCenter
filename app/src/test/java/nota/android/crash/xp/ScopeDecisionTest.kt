package nota.android.crash.xp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopeDecisionTest {

    @Test
    fun `default values are correct`() {
        val decision = ScopeDecision(
            shouldHook = true,
            showNotify = true,
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)
        assertTrue(decision.crashLogEnabled) // default = true
    }

    @Test
    fun `can create with all fields specified`() {
        val decision = ScopeDecision(
            shouldHook = false,
            showNotify = false,
            crashLogEnabled = false,
        )
        assertFalse(decision.shouldHook)
        assertFalse(decision.showNotify)
        assertFalse(decision.crashLogEnabled)
    }

    @Test
    fun `can create with explicit crashLogEnabled true`() {
        val decision = ScopeDecision(
            shouldHook = true,
            showNotify = true,
            crashLogEnabled = true,
        )
        assertTrue(decision.shouldHook)
        assertTrue(decision.showNotify)
        assertTrue(decision.crashLogEnabled)
    }

    @Test
    fun `data class equality works`() {
        val a = ScopeDecision(shouldHook = true, showNotify = false, crashLogEnabled = true)
        val b = ScopeDecision(shouldHook = true, showNotify = false, crashLogEnabled = true)
        val c = ScopeDecision(shouldHook = false, showNotify = false, crashLogEnabled = true)

        assertEquals(a, b)
        assertTrue(a != c)
    }

    @Test
    fun `data class copy works`() {
        val original = ScopeDecision(shouldHook = true, showNotify = true, crashLogEnabled = true)
        val copy = original.copy(showNotify = false)

        assertTrue(copy.shouldHook)
        assertFalse(copy.showNotify)
        assertTrue(copy.crashLogEnabled)
    }

    @Test
    fun `data class toString contains field values`() {
        val decision = ScopeDecision(shouldHook = true, showNotify = false, crashLogEnabled = true)
        val str = decision.toString()
        assertTrue(str.contains("shouldHook=true"))
        assertTrue(str.contains("showNotify=false"))
        assertTrue(str.contains("crashLogEnabled=true"))
    }

    @Test
    fun `data class component functions work`() {
        val decision = ScopeDecision(shouldHook = true, showNotify = false, crashLogEnabled = true)
        assertTrue(decision.component1()) // shouldHook
        assertFalse(decision.component2()) // showNotify
        assertTrue(decision.component3()) // crashLogEnabled
    }
}
