package nota.android.crash.xp.app.config

import android.view.Menu
import android.view.MenuItem
import nota.android.crash.xp.app.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ConfigOptionsMenuHelperTest {

    private lateinit var viewModel: ConfigViewModel
    private lateinit var showAddManagedAppSheet: () -> Unit
    private lateinit var showHelpDialog: () -> Unit
    private lateinit var showTestCrashDialog: () -> Unit
    private lateinit var helper: ConfigOptionsMenuHelper

    @Before
    fun setUp() {
        viewModel = mock()
        showAddManagedAppSheet = mock()
        showHelpDialog = mock()
        showTestCrashDialog = mock()
        helper = ConfigOptionsMenuHelper(viewModel, showAddManagedAppSheet, showHelpDialog, showTestCrashDialog)
    }

    // ─── prepareOptionsMenu ───

    @Test
    fun `prepareOptionsMenu shows select all and cancel all in legacy mode`() {
        val selectAll = mockMenuItem()
        val cancelAll = mockMenuItem()
        val addManaged = mockMenuItem()
        val menu = mockMenu(
            R.id.item_select_all to selectAll,
            R.id.item_cancel_all to cancelAll,
            R.id.item_add_managed_app to addManaged,
        )

        helper.prepareOptionsMenu(menu, isLegacyMode = true)

        verify(selectAll).isVisible = true
        verify(cancelAll).isVisible = true
        verify(addManaged).isVisible = false
    }

    @Test
    fun `prepareOptionsMenu shows add managed app in managed mode`() {
        val selectAll = mockMenuItem()
        val cancelAll = mockMenuItem()
        val addManaged = mockMenuItem()
        val menu = mockMenu(
            R.id.item_select_all to selectAll,
            R.id.item_cancel_all to cancelAll,
            R.id.item_add_managed_app to addManaged,
        )

        helper.prepareOptionsMenu(menu, isLegacyMode = false)

        verify(selectAll).isVisible = false
        verify(cancelAll).isVisible = false
        verify(addManaged).isVisible = true
    }

    @Test
    fun `prepareOptionsMenu hides all toggle items when isLegacyMode is null`() {
        val selectAll = mockMenuItem()
        val cancelAll = mockMenuItem()
        val addManaged = mockMenuItem()
        val menu = mockMenu(
            R.id.item_select_all to selectAll,
            R.id.item_cancel_all to cancelAll,
            R.id.item_add_managed_app to addManaged,
        )

        helper.prepareOptionsMenu(menu, isLegacyMode = null)

        verify(selectAll).isVisible = false
        verify(cancelAll).isVisible = false
        verify(addManaged).isVisible = false
    }

    // ─── handleOptionsItem — known action items ───

    @Test
    fun `handleOptionsItem add managed app calls showAddManagedAppSheet`() {
        val item = mockMenuItem(R.id.item_add_managed_app)

        val result = helper.handleOptionsItem(item)

        assertTrue(result)
        verify(showAddManagedAppSheet).invoke()
    }

    @Test
    fun `handleOptionsItem select all calls viewModel selectAll enabled true`() {
        val item = mockMenuItem(R.id.item_select_all)

        val result = helper.handleOptionsItem(item)

        assertTrue(result)
        verify(viewModel).selectAll(enabled = true)
    }

    @Test
    fun `handleOptionsItem cancel all calls viewModel selectAll enabled false`() {
        val item = mockMenuItem(R.id.item_cancel_all)

        val result = helper.handleOptionsItem(item)

        assertTrue(result)
        verify(viewModel).selectAll(enabled = false)
    }

    @Test
    fun `handleOptionsItem help calls showHelpDialog`() {
        val item = mockMenuItem(R.id.item_help)

        val result = helper.handleOptionsItem(item)

        assertTrue(result)
        verify(showHelpDialog).invoke()
    }

    @Test
    fun `handleOptionsItem test crash calls showTestCrashDialog`() {
        val item = mockMenuItem(R.id.item_test_crash)

        val result = helper.handleOptionsItem(item)

        assertTrue(result)
        verify(showTestCrashDialog).invoke()
    }

    // ─── handleOptionsItem — unknown item ───

    @Test
    fun `handleOptionsItem returns false for unknown item`() {
        val item = mockMenuItem(Int.MAX_VALUE)

        val result = helper.handleOptionsItem(item)

        assertFalse(result)
        verify(viewModel, never()).setSortMode(any())
    }

    // ─── handleOptionsItem — sort mode items ───

    @Test
    fun `handleOptionsItem sort by name sets isChecked and calls setSortMode NAME_ASC`() {
        verifySortItem(R.id.item_sort_by_name, SortMode.NAME_ASC)
    }

    @Test
    fun `handleOptionsItem sort by name reverse sets isChecked and calls setSortMode NAME_DESC`() {
        verifySortItem(R.id.item_sort_by_name_reverse, SortMode.NAME_DESC)
    }

    @Test
    fun `handleOptionsItem sort by install time sets isChecked and calls setSortMode INSTALL_TIME_ASC`() {
        verifySortItem(R.id.item_sort_by_install_time, SortMode.INSTALL_TIME_ASC)
    }

    @Test
    fun `handleOptionsItem sort by install time reverse sets isChecked and calls setSortMode INSTALL_TIME_DESC`() {
        verifySortItem(R.id.item_sort_by_install_time_reverse, SortMode.INSTALL_TIME_DESC)
    }

    @Test
    fun `handleOptionsItem sort by update time sets isChecked and calls setSortMode UPDATE_TIME_ASC`() {
        verifySortItem(R.id.item_sort_by_update_time, SortMode.UPDATE_TIME_ASC)
    }

    @Test
    fun `handleOptionsItem sort by update time reverse sets isChecked and calls setSortMode UPDATE_TIME_DESC`() {
        verifySortItem(R.id.item_sort_by_update_time_reverse, SortMode.UPDATE_TIME_DESC)
    }

    // ─── helpers ───

    private fun verifySortItem(itemId: Int, expectedSortMode: SortMode) {
        val item = mockMenuItem(itemId)

        val result = helper.handleOptionsItem(item)

        assertTrue(result)
        verify(item).isChecked = true
        verify(viewModel).setSortMode(expectedSortMode)
    }

    private fun mockMenuItem(id: Int = 0): MenuItem = mock {
        on { itemId } doReturn id
    }

    private fun mockMenu(vararg items: Pair<Int, MenuItem>): Menu = mock {
        items.forEach { (id, item) ->
            on { findItem(id) } doReturn item
        }
    }
}
