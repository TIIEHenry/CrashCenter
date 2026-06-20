package nota.android.crash.xp.migration

import android.content.Context
import nota.android.crash.xp.PrefManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LegacyPrefImporterTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        // Clear prefs before each test
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `import writes booleans to current prefs`() {
        val snapshot = LegacyPrefSnapshotReader.Snapshot(
            booleans = mapOf(
                PrefManager.PREF_SCOPE_MODE to true,
                PrefManager.PREF_HANDLE_SYSTEM to false,
                PrefManager.PREF_SHOW_SYSTEM_UI to true,
            ),
            stringSets = emptyMap(),
        )

        val result = LegacyPrefImporter.import(context, snapshot)
        assertTrue(result)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_SCOPE_MODE, false))
        assertFalse(prefs.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, true))
        assertTrue(prefs.getBoolean(PrefManager.PREF_SHOW_SYSTEM_UI, false))
    }

    @Test
    fun `import writes string sets to current prefs`() {
        val snapshot = LegacyPrefSnapshotReader.Snapshot(
            booleans = emptyMap(),
            stringSets = mapOf(
                PrefManager.PREF_PACKAGE_LIST to setOf("com.a", "com.b", "com.c"),
            ),
        )

        val result = LegacyPrefImporter.import(context, snapshot)
        assertTrue(result)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null)
        assertEquals(setOf("com.a", "com.b", "com.c"), set)
    }

    @Test
    fun `import writes both booleans and string sets`() {
        val snapshot = LegacyPrefSnapshotReader.Snapshot(
            booleans = mapOf(PrefManager.PREF_SCOPE_MODE to true),
            stringSets = mapOf(PrefManager.PREF_PACKAGE_LIST to setOf("pkg1")),
        )

        val result = LegacyPrefImporter.import(context, snapshot)
        assertTrue(result)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_SCOPE_MODE, false))
        assertEquals(setOf("pkg1"), prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null))
    }

    @Test
    fun `import returns false when snapshot has no data`() {
        val snapshot = LegacyPrefSnapshotReader.Snapshot()
        val result = LegacyPrefImporter.import(context, snapshot)
        assertFalse(result)
    }

    @Test
    fun `import with empty booleans and empty string sets returns false`() {
        val snapshot = LegacyPrefSnapshotReader.Snapshot(
            booleans = emptyMap(),
            stringSets = emptyMap(),
        )
        val result = LegacyPrefImporter.import(context, snapshot)
        assertFalse(result)
    }

    @Test
    fun `import overwrites existing values`() {
        // Pre-populate with different values
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putBoolean(PrefManager.PREF_SCOPE_MODE, false)
                putStringSet(PrefManager.PREF_PACKAGE_LIST, setOf("old"))
                apply()
            }

        val snapshot = LegacyPrefSnapshotReader.Snapshot(
            booleans = mapOf(PrefManager.PREF_SCOPE_MODE to true),
            stringSets = mapOf(PrefManager.PREF_PACKAGE_LIST to setOf("new")),
        )

        LegacyPrefImporter.import(context, snapshot)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_SCOPE_MODE, false))
        assertEquals(setOf("new"), prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null))
    }

    @Test
    fun `import does not affect other existing keys`() {
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putBoolean("other_key", true)
                putString("other_string", "value")
                apply()
            }

        val snapshot = LegacyPrefSnapshotReader.Snapshot(
            booleans = mapOf(PrefManager.PREF_SCOPE_MODE to true),
            stringSets = emptyMap(),
        )

        LegacyPrefImporter.import(context, snapshot)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("other_key", false))
        assertEquals("value", prefs.getString("other_string", null))
    }

    @Test
    fun `import with empty string set writes empty set`() {
        val snapshot = LegacyPrefSnapshotReader.Snapshot(
            booleans = emptyMap(),
            stringSets = mapOf(PrefManager.PREF_PACKAGE_LIST to emptySet()),
        )

        val result = LegacyPrefImporter.import(context, snapshot)
        assertTrue(result)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null)
        assertTrue(set?.isEmpty() ?: false)
    }

    @Test
    fun `import with only booleans no string sets`() {
        val snapshot = LegacyPrefSnapshotReader.Snapshot(
            booleans = mapOf(PrefManager.PREF_HANDLE_SYSTEM to true),
            stringSets = emptyMap(),
        )

        val result = LegacyPrefImporter.import(context, snapshot)
        assertTrue(result)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_HANDLE_SYSTEM, false))
        assertFalse(prefs.contains(PrefManager.PREF_PACKAGE_LIST))
    }

    @Test
    fun `import with only string sets no booleans`() {
        val snapshot = LegacyPrefSnapshotReader.Snapshot(
            booleans = emptyMap(),
            stringSets = mapOf(PrefManager.PREF_PACKAGE_LIST to setOf("x")),
        )

        val result = LegacyPrefImporter.import(context, snapshot)
        assertTrue(result)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertEquals(setOf("x"), prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null))
        assertFalse(prefs.contains(PrefManager.PREF_SCOPE_MODE))
    }
}
