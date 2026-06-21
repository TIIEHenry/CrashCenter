package nota.android.crash.xp.app.config

import android.app.Activity
import android.content.SharedPreferences
import android.os.Build
import android.widget.TextView
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.app.ModuleActivation
import nota.android.crash.xp.app.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ConfigDialogHelperTest {

    private lateinit var activity: Activity
    private var callbackFired = false
    private lateinit var prefs: FakeSharedPreferences

    /** Only non-null when a test explicitly needs ModuleActivation to return true. */
    private var moduleMock: MockedStatic<ModuleActivation>? = null

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        callbackFired = false
        prefs = FakeSharedPreferences()
    }

    @After
    fun tearDown() {
        moduleMock?.close()
        moduleMock = null
    }

    private fun helper() = ConfigDialogHelper(
        context = activity,
        prefs = prefs,
        onPermissionSettingsOpened = { callbackFired = true },
    )

    private fun latestDialog(): android.app.Dialog {
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        assertNotNull("Expected a dialog to be shown", dialog)
        return dialog
    }

    private fun activateModule() {
        val mock = Mockito.mockStatic(ModuleActivation::class.java)
        mock.`when`<Boolean> { ModuleActivation.isModuleActive() }.thenReturn(true)
        moduleMock = mock
    }

    // ─── showXposedInactiveDialogIfNeeded ───

    @Test
    fun `returns false when module is active`() {
        activateModule()

        val result = helper().showXposedInactiveDialogIfNeeded(xposedDialogShown = false)

        assertFalse(result)
    }

    @Test
    fun `returns true and shows dialog when module inactive and not dismissed`() {
        // ModuleActivation.isModuleActive() returns false by default (no mock needed)
        val result = helper().showXposedInactiveDialogIfNeeded(xposedDialogShown = false)

        assertTrue(result)
        assertNotNull(latestDialog())
    }

    @Test
    fun `returns false when xposedDialogShown is already true`() {
        val result = helper().showXposedInactiveDialogIfNeeded(xposedDialogShown = true)

        assertFalse(result)
    }

    @Test
    fun `returns false when previously dismissed via prefs`() {
        prefs = FakeSharedPreferences(
            PrefManager.PREF_XPOSED_DIALOG_DISMISSED to true,
        )

        val result = helper().showXposedInactiveDialogIfNeeded(xposedDialogShown = false)

        assertFalse(result)
    }

    // ─── showHelpDialog ───

    @Test
    fun `showHelpDialog displays dialog with expected title`() {
        helper().showHelpDialog()

        val dialog = latestDialog()
        val title = (dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle))?.text
        assertEquals(activity.getString(R.string.using_warning_title), title?.toString())
    }

    // ─── showPermissionRationaleDialog ───

    @Test
    fun `permission rationale dialog shows expected title`() {
        helper().showPermissionRationaleDialog()

        val dialog = latestDialog()
        val title = (dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle))?.text
        assertEquals(activity.getString(R.string.permission_rationale_title), title?.toString())
    }

    // ─── Fake SharedPreferences ───

    private class FakeSharedPreferences(
        vararg initial: Pair<String, Any>,
    ) : SharedPreferences {

        private val store = mutableMapOf<String, Any>(*initial)

        override fun getAll(): Map<String, *> = store.toMap()

        override fun getString(key: String, defValue: String?): String? =
            store[key] as? String ?: defValue

        override fun getStringSet(key: String, defValue: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST")
            (store[key] as? MutableSet<String>) ?: defValue

        override fun getInt(key: String, defValue: Int): Int =
            (store[key] as? Int) ?: defValue

        override fun getLong(key: String, defValue: Long): Long =
            (store[key] as? Long) ?: defValue

        override fun getFloat(key: String, defValue: Float): Float =
            (store[key] as? Float) ?: defValue

        override fun getBoolean(key: String, defValue: Boolean): Boolean =
            (store[key] as? Boolean) ?: defValue

        override fun contains(key: String): Boolean = store.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(store)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        private class FakeEditor(
            private val store: MutableMap<String, Any>,
        ) : SharedPreferences.Editor {

            private val pending = mutableMapOf<String, Any?>()

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                pending[key] = value; return this
            }

            override fun putStringSet(
                key: String,
                values: MutableSet<String>?,
            ): SharedPreferences.Editor {
                pending[key] = values; return this
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                pending[key] = value; return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                pending[key] = value; return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                pending[key] = value; return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                pending[key] = value; return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                pending[key] = null; return this
            }

            override fun clear(): SharedPreferences.Editor {
                pending.clear(); store.clear(); return this
            }

            override fun commit(): Boolean {
                pending.forEach { (k, v) ->
                    if (v == null) store.remove(k) else store[k] = v
                }
                pending.clear()
                return true
            }

            override fun apply() {
                commit()
            }
        }
    }
}
