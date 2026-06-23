package nota.android.crash.xp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopeDecisionTest {

    @Test
    fun `default crashLogEnabled is true`() {
        val decision = ScopeDecision(
            shouldInstall = true,
            shouldIntercept = true,
            showNotify = true,
        )
        assertTrue(decision.shouldInstall)
        assertTrue(decision.shouldIntercept)
        assertTrue(decision.showNotify)
        assertTrue(decision.crashLogEnabled)
    }

    @Test
    fun `observe only decision`() {
        val decision = ScopeDecision(
            shouldInstall = true,
            shouldIntercept = false,
            showNotify = false,
            crashLogEnabled = true,
        )
        assertTrue(decision.shouldInstall)
        assertFalse(decision.shouldIntercept)
        assertFalse(decision.showNotify)
    }

    @Test
    fun `data class equality works`() {
        val a = ScopeDecision(shouldInstall = true, shouldIntercept = false, showNotify = false)
        val b = ScopeDecision(shouldInstall = true, shouldIntercept = false, showNotify = false)
        val c = ScopeDecision(shouldInstall = false, shouldIntercept = false, showNotify = false)
        assertEquals(a, b)
        assertTrue(a != c)
    }

    @Test
    fun `data class copy works`() {
        val original = ScopeDecision(shouldInstall = true, shouldIntercept = true, showNotify = true)
        val copy = original.copy(shouldIntercept = false)
        assertTrue(copy.shouldInstall)
        assertFalse(copy.shouldIntercept)
    }
}
