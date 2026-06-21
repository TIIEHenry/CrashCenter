package nota.android.crash.xp.migration

import android.content.Context
import android.content.SharedPreferences
import nota.android.crash.xp.PrefManager
import nota.android.crash.xp.PrefMigrator
import nota.android.crash.xp.app.config.InterventionRulesCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ManagedModelMigratorTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        // Clear all prefs before each test
        prefs.edit().clear().commit()
    }

    // ---------- Already migrated flag set ----------

    @Test
    fun `migrateIfNeeded does nothing when already migrated flag is set`() {
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, true)
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = false)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false))
        assertFalse(prefs.contains(PrefManager.PREF_MANAGED_PACKAGES))
    }

    // ---------- Already has managed packages ----------

    @Test
    fun `migrateIfNeeded sets flag when managed packages already exist`() {
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putStringSet(PrefManager.PREF_MANAGED_PACKAGES, setOf("com.example"))
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = false)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false))
        assertEquals(setOf("com.example"), prefs.getStringSet(PrefManager.PREF_MANAGED_PACKAGES, null))
    }

    // ---------- Empty activation (no legacy data) ----------

    @Test
    fun `migrateIfNeeded activates empty state when no legacy data exists`() {
        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = false)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false))
        assertTrue(prefs.contains(PrefManager.PREF_MANAGED_PACKAGES))
        val managed = prefs.getStringSet(PrefManager.PREF_MANAGED_PACKAGES, null)
        assertTrue(managed?.isEmpty() ?: false)
        assertEquals("{}", prefs.getString(PrefManager.PREF_INTERVENTION_RULES, null))
    }

    // ---------- Legacy data present (importHadData = true) ----------

    @Test
    fun `migrateIfNeeded migrates from legacy when import had data`() {
        // Simulate legacy prefs imported: scope_mode=true, package_list has disabled apps
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putBoolean(PrefManager.PREF_SCOPE_MODE, true)
                putBoolean(PrefManager.PREF_HANDLE_SYSTEM, false)
                putStringSet(PrefManager.PREF_PACKAGE_LIST, setOf("com.disabled"))
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = true)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false))
        assertTrue(prefs.contains(PrefManager.PREF_MANAGED_PACKAGES))
        assertTrue(prefs.contains(PrefManager.PREF_INTERVENTION_RULES))
    }

    @Test
    fun `migrateIfNeeded migrates when package_list exists in current prefs`() {
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putStringSet(PrefManager.PREF_PACKAGE_LIST, setOf("com.app1", "com.app2"))
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = false)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false))
    }

    @Test
    fun `migrateIfNeeded migrates when scope_mode is true`() {
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putBoolean(PrefManager.PREF_SCOPE_MODE, true)
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = false)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false))
    }

    @Test
    fun `migrateIfNeeded migrates when handle_system is true`() {
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putBoolean(PrefManager.PREF_HANDLE_SYSTEM, true)
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = false)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false))
    }

    @Test
    fun `migrateIfNeeded migrates when show_system_ui is true`() {
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putBoolean(PrefManager.PREF_SHOW_SYSTEM_UI, true)
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = false)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false))
    }

    // ---------- Edge cases ----------

    @Test
    fun `migrateIfNeeded with empty package_list activates empty`() {
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putStringSet(PrefManager.PREF_PACKAGE_LIST, emptySet())
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = false)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false))
    }

    @Test
    fun `migrateIfNeeded writes valid intervention rules JSON`() {
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putBoolean(PrefManager.PREF_SCOPE_MODE, false)
                putStringSet(PrefManager.PREF_PACKAGE_LIST, emptySet())
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = true)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        val rulesJson = prefs.getString(PrefManager.PREF_INTERVENTION_RULES, null)
        assertTrue(rulesJson != null)
        assertTrue(rulesJson!!.isNotEmpty())

        // Verify it's valid JSON by parsing
        val decoded = InterventionRulesCodec.decode(rulesJson)
        // Should be a valid map (may be empty or have entries depending on installed packages)
        assertTrue(decoded.isNotEmpty() || rulesJson == "{}")
    }

    @Test
    fun `migrateIfNeeded leaves package_list untouched`() {
        val originalList = setOf("com.app1", "com.app2")
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putStringSet(PrefManager.PREF_PACKAGE_LIST, originalList)
                putBoolean(PrefManager.PREF_SCOPE_MODE, true)
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = true)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        // package_list should still exist (read-only, not deleted)
        assertEquals(originalList, prefs.getStringSet(PrefManager.PREF_PACKAGE_LIST, null))
    }

    @Test
    fun `migrateIfNeeded idempotent - second call does nothing`() {
        context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
            .edit().apply {
                putBoolean(PrefManager.PREF_SCOPE_MODE, true)
                putStringSet(PrefManager.PREF_PACKAGE_LIST, setOf("com.disabled"))
                apply()
            }

        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = true)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        val firstManaged = prefs.getStringSet(PrefManager.PREF_MANAGED_PACKAGES, null)
        val firstRules = prefs.getString(PrefManager.PREF_INTERVENTION_RULES, null)

        // Second call should be no-op due to flag
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        assertEquals(firstManaged, prefs.getStringSet(PrefManager.PREF_MANAGED_PACKAGES, null))
        assertEquals(firstRules, prefs.getString(PrefManager.PREF_INTERVENTION_RULES, null))
    }

    @Test
    fun `migrateIfNeeded with all false booleans and no package_list activates empty`() {
        // No legacy indicators at all
        val legacyState = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = false)
        ManagedModelMigrator.migrateIfNeeded(context, prefs, legacyState)

        val prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(PrefManager.PREF_MANAGED_MODEL_MIGRATED, false))
        val managed = prefs.getStringSet(PrefManager.PREF_MANAGED_PACKAGES, null)
        assertTrue(managed?.isEmpty() ?: false)
    }
}
