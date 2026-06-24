package nota.android.crash.xp.app.config

import android.view.MenuItem
import nota.android.crash.xp.app.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ConfigOptionsMenuHelperTest {

    private lateinit var viewModel: ConfigViewModel
    private lateinit var helper: ConfigOptionsMenuHelper

    @Before
    fun setUp() {
        viewModel = mock()
        helper = ConfigOptionsMenuHelper(
            viewModel = viewModel,
            showAddManagedAppSheet = {},
            showHelpDialog = {},
            showTestCrashDialog = {},
        )
    }

    @Test
    fun `prepareOptionsMenu is no-op`() {
        val menu = mock<android.view.Menu>()
        helper.prepareOptionsMenu(menu, listCount = 0)
    }

    @Test
    fun `handleOptionsItem help returns true`() {
        val item = mockMenuItem(R.id.item_help)
        assertTrue(helper.handleOptionsItem(item))
    }

    @Test
    fun `handleOptionsItem sort by name sets checked and sort mode`() {
        val item = mockMenuItem(R.id.item_sort_by_name)
        assertTrue(helper.handleOptionsItem(item))
        verify(item).isChecked = true
        verify(viewModel).setSortMode(SortMode.NAME_ASC)
    }

    @Test
    fun `handleOptionsItem returns false for unknown item`() {
        val item = mockMenuItem(Int.MAX_VALUE)
        assertFalse(helper.handleOptionsItem(item))
    }

    private fun mockMenuItem(id: Int): MenuItem = mock {
        on { itemId } doReturn id
    }
}
