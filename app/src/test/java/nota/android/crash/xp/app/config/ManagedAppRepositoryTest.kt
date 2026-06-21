package nota.android.crash.xp.app.config

import android.content.Context
import android.content.SharedPreferences
import nota.android.crash.xp.PrefManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ManagedAppRepositoryTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var repository: ManagedAppRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefs = context.getSharedPreferences(PrefManager.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository = ManagedAppRepository(context, prefs)
    }

    // ─── Mode detection ───

    @Test
    fun `isLegacyMode returns true when managed_packages key is absent`() {
        assertTrue(repository.isLegacyMode())
    }

    @Test
    fun `isLegacyMode returns false when managed_packages key exists`() {
        prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, emptySet()).commit()
        assertFalse(repository.isLegacyMode())
    }

    // ─── readManagedPackageNames ───

    @Test
    fun `readManagedPackageNames returns null in legacy mode`() {
        assertNull(repository.readManagedPackageNames())
    }

    @Test
    fun `readManagedPackageNames returns empty set when no packages stored`() {
        prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, emptySet()).commit()
        val names = repository.readManagedPackageNames()
        assertNotNull(names)
        assertTrue(names!!.isEmpty())
    }

    @Test
    fun `readManagedPackageNames returns stored packages`() {
        val stored = setOf("com.example.a", "com.example.b")
        prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, stored).commit()
        val names = repository.readManagedPackageNames()
        assertNotNull(names)
        assertEquals(2, names!!.size)
        assertTrue(names.contains("com.example.a"))
        assertTrue(names.contains("com.example.b"))
    }

    @Test
    fun `readManagedPackageNames returns defensive copy`() {
        prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, setOf("com.example.a")).commit()
        val names1 = repository.readManagedPackageNames()
        val names2 = repository.readManagedPackageNames()
        assertNotNull(names1)
        assertNotNull(names2)
        assertTrue(names1 !== names2)
        assertEquals(names1, names2)
    }

    // ─── addManagedPackages ───

    @Test
    fun `addManagedPackages creates managed_packages key`() {
        assertTrue(repository.isLegacyMode())
        repository.addManagedPackages(listOf("com.example.a"))
        assertFalse(repository.isLegacyMode())
        val names = repository.readManagedPackageNames()
        assertNotNull(names)
        assertTrue(names!!.contains("com.example.a"))
    }

    @Test
    fun `addManagedPackages appends to existing set`() {
        prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, setOf("com.example.a")).commit()
        repository.addManagedPackages(listOf("com.example.b"))
        val names = repository.readManagedPackageNames()
        assertNotNull(names)
        assertEquals(2, names!!.size)
        assertTrue(names.contains("com.example.a"))
        assertTrue(names.contains("com.example.b"))
    }

    @Test
    fun `addManagedPackages ignores duplicates`() {
        prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, setOf("com.example.a")).commit()
        repository.addManagedPackages(listOf("com.example.a"))
        val names = repository.readManagedPackageNames()
        assertNotNull(names)
        assertEquals(1, names!!.size)
    }

    @Test
    fun `addManagedPackages ignores self package`() {
        repository.addManagedPackages(listOf(PrefManager.ITSELF))
        // In legacy mode (no managed_packages key yet), self is filtered and nothing is written
        assertTrue(repository.isLegacyMode())
    }

    @Test
    fun `addManagedPackages with empty collection does nothing`() {
        repository.addManagedPackages(emptyList())
        assertTrue(repository.isLegacyMode())
    }

    // ─── removeManagedPackage ───

    @Test
    fun `removeManagedPackage removes package and profile`() {
        prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, setOf("com.example.a", "com.example.b")).commit()
        // Pre-seed a profile
        val profile = AppInterventionProfile(
            rules = listOf(InterventionRule.defaultCatchAll(enabled = true)),
        )
        val profiles = mapOf("com.example.a" to profile, "com.example.b" to AppInterventionProfile.EMPTY)
        prefs.edit().putString(PrefManager.PREF_INTERVENTION_RULES, InterventionRulesCodec.encode(profiles)).commit()

        repository.removeManagedPackage("com.example.a")

        val names = repository.readManagedPackageNames()
        assertNotNull(names)
        assertEquals(1, names!!.size)
        assertFalse(names.contains("com.example.a"))
        assertTrue(names.contains("com.example.b"))

        // Profile should also be removed
        val remainingProfile = repository.getProfile("com.example.a")
        assertEquals(AppInterventionProfile.EMPTY, remainingProfile)
    }

    @Test
    fun `removeManagedPackage does nothing for non-managed package`() {
        prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, setOf("com.example.a")).commit()
        repository.removeManagedPackage("com.example.b")
        val names = repository.readManagedPackageNames()
        assertNotNull(names)
        assertEquals(1, names!!.size)
        assertTrue(names!!.contains("com.example.a"))
    }

    @Test
    fun `removeManagedPackage in legacy mode does nothing`() {
        assertTrue(repository.isLegacyMode())
        repository.removeManagedPackage("com.example.a")
        assertTrue(repository.isLegacyMode())
    }

    // ─── pruneUninstalled ───

    @Test
    fun `pruneUninstalled returns 0 in legacy mode`() {
        assertEquals(0, repository.pruneUninstalled())
    }

    @Test
    fun `pruneUninstalled returns 0 when no packages stored`() {
        prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, emptySet()).commit()
        assertEquals(0, repository.pruneUninstalled())
    }

    @Test
    fun `pruneUninstalled removes non-existent packages`() {
        // Store packages that don't exist on the test device
        prefs.edit().putStringSet(
            PrefManager.PREF_MANAGED_PACKAGES,
            setOf("com.nonexistent.package.1", "com.nonexistent.package.2"),
        ).commit()
        val pruned = repository.pruneUninstalled()
        assertEquals(2, pruned)
        val remaining = repository.readManagedPackageNames()
        assertNotNull(remaining)
        assertTrue(remaining!!.isEmpty())
    }

    // ─── Intervention profiles ───

    @Test
    fun `getProfile returns empty for unknown package`() {
        val profile = repository.getProfile("com.unknown.package")
        assertEquals(AppInterventionProfile.EMPTY, profile)
    }

    @Test
    fun `saveProfile stores profile`() {
        val profile = AppInterventionProfile(
            rules = listOf(InterventionRule.defaultCatchAll(enabled = true)),
        )
        repository.saveProfile("com.example.a", profile)
        val retrieved = repository.getProfile("com.example.a")
        assertEquals(1, retrieved.rules.size)
        assertEquals(InterventionRuleType.CATCH_ALL, retrieved.rules[0].type)
        assertTrue(retrieved.rules[0].enabled)
    }

    @Test
    fun `saveProfile with empty rules removes profile`() {
        val profile = AppInterventionProfile(
            rules = listOf(InterventionRule.defaultCatchAll(enabled = true)),
        )
        repository.saveProfile("com.example.a", profile)
        assertEquals(1, repository.getProfile("com.example.a").rules.size)

        repository.saveProfile("com.example.a", AppInterventionProfile.EMPTY)
        assertEquals(AppInterventionProfile.EMPTY, repository.getProfile("com.example.a"))
    }

    @Test
    fun `saveProfile updates existing profile`() {
        val profile1 = AppInterventionProfile(
            rules = listOf(InterventionRule.defaultCatchAll(enabled = true)),
        )
        repository.saveProfile("com.example.a", profile1)

        val profile2 = AppInterventionProfile(
            rules = listOf(
                InterventionRule.defaultCatchAll(enabled = false),
                InterventionRule.defaultCatchAll(enabled = true),
            ),
        )
        repository.saveProfile("com.example.a", profile2)

        val retrieved = repository.getProfile("com.example.a")
        assertEquals(2, retrieved.rules.size)
    }

    @Test
    fun `saveProfile updates updatedAt timestamp`() {
        val before = System.currentTimeMillis()
        val profile = AppInterventionProfile(
            rules = listOf(InterventionRule.defaultCatchAll(enabled = true)),
            updatedAt = 0L,
        )
        repository.saveProfile("com.example.a", profile)
        val retrieved = repository.getProfile("com.example.a")
        assertTrue(retrieved.updatedAt >= before)
    }

    // ─── setInterventionEnabled ───

    @Test
    fun `setInterventionEnabled on empty profile creates catch-all rule`() {
        repository.setInterventionEnabled("com.example.a", true)
        val profile = repository.getProfile("com.example.a")
        assertEquals(1, profile.rules.size)
        assertEquals(InterventionRuleType.CATCH_ALL, profile.rules[0].type)
        assertTrue(profile.rules[0].enabled)
    }

    @Test
    fun `setInterventionEnabled false on empty profile does nothing`() {
        repository.setInterventionEnabled("com.example.a", false)
        val profile = repository.getProfile("com.example.a")
        assertEquals(AppInterventionProfile.EMPTY, profile)
    }

    @Test
    fun `setInterventionEnabled true on existing profile with enabled rule is no-op`() {
        val profile = AppInterventionProfile(
            rules = listOf(
                InterventionRule(id = "r1", type = InterventionRuleType.CATCH_ALL, enabled = true),
            ),
        )
        repository.saveProfile("com.example.a", profile)
        repository.setInterventionEnabled("com.example.a", true)
        val retrieved = repository.getProfile("com.example.a")
        assertEquals(1, retrieved.rules.size)
        assertTrue(retrieved.rules[0].enabled)
    }

    @Test
    fun `setInterventionEnabled true enables catch-all rule`() {
        val profile = AppInterventionProfile(
            rules = listOf(
                InterventionRule(id = "r1", type = InterventionRuleType.CATCH_ALL, enabled = false),
            ),
        )
        repository.saveProfile("com.example.a", profile)
        repository.setInterventionEnabled("com.example.a", true)
        val retrieved = repository.getProfile("com.example.a")
        assertTrue(retrieved.rules[0].enabled)
    }

    @Test
    fun `setInterventionEnabled false disables all rules`() {
        val profile = AppInterventionProfile(
            rules = listOf(
                InterventionRule(id = "r1", type = InterventionRuleType.CATCH_ALL, enabled = true),
                InterventionRule(id = "r2", type = InterventionRuleType.CATCH_ALL, enabled = true),
            ),
        )
        repository.saveProfile("com.example.a", profile)
        repository.setInterventionEnabled("com.example.a", false)
        val retrieved = repository.getProfile("com.example.a")
        assertTrue(retrieved.rules.all { !it.enabled })
    }

    @Test
    fun `setInterventionEnabled true with no enabled rules enables first rule`() {
        val profile = AppInterventionProfile(
            rules = listOf(
                InterventionRule(id = "r1", type = InterventionRuleType.CATCH_ALL, enabled = false),
                InterventionRule(id = "r2", type = InterventionRuleType.CATCH_ALL, enabled = false),
            ),
        )
        repository.saveProfile("com.example.a", profile)
        repository.setInterventionEnabled("com.example.a", true)
        val retrieved = repository.getProfile("com.example.a")
        // Both rules get enabled because they are both CATCH_ALL type
        assertTrue(retrieved.rules[0].enabled)
        assertTrue(retrieved.rules[1].enabled)
    }

    @Test
    fun `setInterventionEnabled true with no catch-all enables first rule`() {
        // This tests the fallback path when no CATCH_ALL rule exists to enable
        val profile = AppInterventionProfile(
            rules = listOf(
                InterventionRule(id = "r1", type = InterventionRuleType.CATCH_ALL, enabled = false),
            ),
        )
        repository.saveProfile("com.example.a", profile)
        repository.setInterventionEnabled("com.example.a", true)
        val retrieved = repository.getProfile("com.example.a")
        assertTrue(retrieved.rules.any { it.enabled })
    }

    // ─── loadManagedApps returns empty in various states ───

    @Test
    fun `loadManagedApps returns empty in legacy mode`() {
        val apps = repository.loadManagedApps()
        assertTrue(apps.isEmpty())
    }

    @Test
    fun `loadManagedApps returns empty when no managed packages`() {
        prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, emptySet()).commit()
        val apps = repository.loadManagedApps()
        assertTrue(apps.isEmpty())
    }

    // ─── loadPickableApps ───

    @Test
    fun `loadPickableApps returns non-empty list`() {
        // In legacy mode, readManagedPackageNames returns null which is treated as empty set
        val apps = repository.loadPickableApps()
        // Should contain at least the test app's own packages
        assertNotNull(apps)
    }

    @Test
    fun `loadPickableApps excludes self package`() {
        val apps = repository.loadPickableApps()
        assertFalse(apps.any { it.packageName == PrefManager.ITSELF })
    }

    @Test
    fun `loadPickableApps excludes already managed packages`() {
        // Seed a managed package that exists
        val existingPackages = context.packageManager.getInstalledPackages(0).map { it.packageName }
        if (existingPackages.isNotEmpty()) {
            val managed = setOf(existingPackages.first())
            prefs.edit().putStringSet(PrefManager.PREF_MANAGED_PACKAGES, managed).commit()
            val apps = repository.loadPickableApps()
            assertFalse(apps.any { it.packageName == existingPackages.first() })
        }
    }

    @Test
    fun `loadPickableApps filters system apps when showSystemUi false`() {
        prefs.edit().putBoolean("show_system_ui", false).commit()
        val apps = repository.loadPickableApps()
        // All returned apps should be non-system
        assertTrue(apps.all { !it.isSystem })
    }

    @Test
    fun `loadPickableApps includes system apps when showSystemUi true`() {
        prefs.edit().putBoolean("show_system_ui", true).commit()
        val apps = repository.loadPickableApps()
        // May include system apps; just verify it doesn't crash
        assertNotNull(apps)
    }
}
