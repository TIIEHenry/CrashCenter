package nota.android.crash.xp.app.config

import nota.android.crash.xp.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmptyStateRendererTest {

    @Test
    fun `empty list and EMPTY_LIST uses managed empty message`() {
        val state = ConfigUiState(
            isLoading = false,
            emptyMessage = ConfigViewModel.EMPTY_LIST,
        )

        val (messageResId, showAction) = decideEmpty(state, listCount = 0)

        assertEquals(R.string.managed_empty_message, messageResId)
        assertTrue(showAction)
    }

    @Test
    fun `empty list and EMPTY_FILTER uses filter empty message`() {
        val state = ConfigUiState(
            isLoading = false,
            emptyMessage = ConfigViewModel.EMPTY_FILTER,
        )

        val (messageResId, showAction) = decideEmpty(state, listCount = 0)

        assertEquals(R.string.filter_empty, messageResId)
        assertFalse(showAction)
    }

    @Test
    fun `loading suppresses empty state`() {
        val state = ConfigUiState(
            isLoading = true,
            emptyMessage = ConfigViewModel.EMPTY_LIST,
        )

        val (_, showAction) = decideEmpty(state, listCount = 0)

        assertFalse(showAction)
    }

    private fun decideEmpty(state: ConfigUiState, listCount: Int): Pair<Int, Boolean> {
        val empty = !state.isLoading && listCount == 0
        if (!empty) return Pair(R.string.managed_empty_message, false)
        val showAction = state.emptyMessage == ConfigViewModel.EMPTY_LIST
        val messageResId = when (state.emptyMessage) {
            ConfigViewModel.EMPTY_LIST -> R.string.managed_empty_message
            else -> R.string.filter_empty
        }
        return Pair(messageResId, showAction)
    }
}
