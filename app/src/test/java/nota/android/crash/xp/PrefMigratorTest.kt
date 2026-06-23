package nota.android.crash.xp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefMigratorTest {

    // ---------- LegacyPrefState data class ----------

    @Test
    fun `LegacyPrefState data class equality`() {
        val a = PrefMigrator.LegacyPrefState(hadPriorSession = true, importHadData = false)
        val b = PrefMigrator.LegacyPrefState(hadPriorSession = true, importHadData = false)
        val c = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = true)

        assertEquals(a, b)
        assertTrue(a != c)
    }

    @Test
    fun `LegacyPrefState data class copy works`() {
        val original = PrefMigrator.LegacyPrefState(hadPriorSession = false, importHadData = false)
        val copy = original.copy(importHadData = true)

        assertFalse(copy.hadPriorSession)
        assertTrue(copy.importHadData)
    }

    @Test
    fun `LegacyPrefState component functions work`() {
        val state = PrefMigrator.LegacyPrefState(hadPriorSession = true, importHadData = true)
        assertTrue(state.component1()) // hadPriorSession
        assertTrue(state.component2()) // importHadData
    }

    @Test
    fun `LegacyPrefState toString contains field values`() {
        val state = PrefMigrator.LegacyPrefState(hadPriorSession = true, importHadData = false)
        val str = state.toString()
        assertTrue(str.contains("hadPriorSession=true"))
        assertTrue(str.contains("importHadData=false"))
    }

    // ---------- Snapshot.hasData() (via reflection since Snapshot is private) ----------

    // Snapshot is private, but we can test its behavior indirectly through migrateIfNeeded.
    // The pure logic we can test here is the data class properties and the XML parsing.

    // ---------- PrefManager constants used by PrefMigrator ----------

    @Test
    fun `PrefManager constants are stable`() {
        // These constants are used by PrefMigrator; verify they haven't changed
        assertEquals("crash", PrefManager.PREF_NAME)
        assertEquals("scope_mode", PrefManager.PREF_SCOPE_MODE)
        assertEquals("handle_system", PrefManager.PREF_HANDLE_SYSTEM)
        assertEquals("show_system_ui", PrefManager.PREF_SHOW_SYSTEM_UI)
        assertEquals("package_list", PrefManager.PREF_PACKAGE_LIST)
        assertEquals("managed_packages", PrefManager.PREF_MANAGED_PACKAGES)
        assertEquals("intervention_rules", PrefManager.PREF_INTERVENTION_RULES)
        assertEquals("managed_model_migrated", PrefManager.PREF_MANAGED_MODEL_MIGRATED)
        assertEquals("observe_intercept_split_migrated", PrefManager.PREF_OBSERVE_INTERCEPT_SPLIT_MIGRATED)
    }

    @Test
    fun `LegacyPrefState handles all boolean combinations`() {
        val cases = listOf(
            Pair(true, true),
            Pair(true, false),
            Pair(false, true),
            Pair(false, false),
        )
        for ((hadPrior, importHad) in cases) {
            val state = PrefMigrator.LegacyPrefState(
                hadPriorSession = hadPrior,
                importHadData = importHad,
            )
            assertEquals(hadPrior, state.hadPriorSession)
            assertEquals(importHad, state.importHadData)
        }
    }
}
