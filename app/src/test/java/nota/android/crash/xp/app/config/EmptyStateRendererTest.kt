package nota.android.crash.xp.app.config

import nota.android.crash.xp.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmptyStateRendererTest {

    // -- decide empty --

    @Test
    fun `empty list and EMPTY_MANAGED_LIST shows add action`() {
        val state = ConfigUiState(
            isLoading = false,
            isLegacyMode = false,
            emptyMessage = ConfigViewModel.EMPTY_MANAGED_LIST,
        )

        val decision = decideEmpty(state, listCount = 0)

        assertTrue(decision.isEmpty)
        assertEquals(R.string.managed_empty_message, decision.messageResId)
        assertTrue(decision.showAddAction)
        assertEquals(R.drawable.ic_tab_config, decision.iconResId)
    }

    @Test
    fun `empty list and EMPTY_FILTER hides add action`() {
        val state = ConfigUiState(
            isLoading = false,
            isLegacyMode = false,
            emptyMessage = ConfigViewModel.EMPTY_FILTER,
        )

        val decision = decideEmpty(state, listCount = 0)

        assertTrue(decision.isEmpty)
        assertEquals(R.string.managed_filter_empty, decision.messageResId)
        assertFalse(decision.showAddAction)
        assertNull(decision.iconResId)
    }

    @Test
    fun `loading suppresses empty state`() {
        val state = ConfigUiState(
            isLoading = true,
            emptyMessage = ConfigViewModel.EMPTY_MANAGED_LIST,
        )

        val decision = decideEmpty(state, listCount = 0)

        assertFalse(decision.isEmpty)
    }

    @Test
    fun `non-empty list not empty`() {
        val state = ConfigUiState(isLoading = false)

        val decision = decideEmpty(state, listCount = 5)

        assertFalse(decision.isEmpty)
    }

    @Test
    fun `EMPTY_FILTER in legacy mode uses filter_empty resource`() {
        val state = ConfigUiState(
            isLoading = false,
            isLegacyMode = true,
            emptyMessage = ConfigViewModel.EMPTY_FILTER,
        )

        val decision = decideEmpty(state, listCount = 0)

        assertEquals(R.string.filter_empty, decision.messageResId)
    }
}
