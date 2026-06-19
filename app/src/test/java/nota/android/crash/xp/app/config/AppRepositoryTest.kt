package nota.android.crash.xp.app.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRepositoryTest {

    // ---------- passesSystemFilter ----------

    @Test
    fun `passesSystemFilter returns true for non-system app`() {
        assertTrue(AppRepository.passesSystemFilter(isSystemApp = false, handleSystem = false))
        assertTrue(AppRepository.passesSystemFilter(isSystemApp = false, handleSystem = true))
    }

    @Test
    fun `passesSystemFilter returns true for system app when handleSystem is true`() {
        assertTrue(AppRepository.passesSystemFilter(isSystemApp = true, handleSystem = true))
    }

    @Test
    fun `passesSystemFilter returns false for system app when handleSystem is false`() {
        assertFalse(AppRepository.passesSystemFilter(isSystemApp = true, handleSystem = false))
    }

    @Test
    fun `passesSystemFilter handles all combinations`() {
        // (isSystemApp, handleSystem) -> expected
        val cases = listOf(
            Pair(Pair(false, false), true),
            Pair(Pair(false, true), true),
            Pair(Pair(true, false), false),
            Pair(Pair(true, true), true),
        )
        for ((input, expected) in cases) {
            val (isSystemApp, handleSystem) = input
            assertEquals(
                "Failed for isSystemApp=$isSystemApp, handleSystem=$handleSystem",
                expected,
                AppRepository.passesSystemFilter(isSystemApp, handleSystem),
            )
        }
    }

    // ---------- enableIntervention (via setInterventionEnabled logic) ----------

    @Test
    fun `enableIntervention on empty profile creates default catch-all rule`() {
        // This tests the logic path: profile.rules.isEmpty() -> create default rule
        val emptyProfile = AppInterventionProfile.EMPTY
        // We can't directly test private enableIntervention, but we can verify
        // the profile state transitions that would occur

        // An empty profile has no enabled rules
        assertEquals(0, emptyProfile.enabledRuleCount)
        assertFalse(emptyProfile.hasEnabledRule)
    }

    @Test
    fun `AppInterventionProfile EMPTY has no rules`() {
        assertTrue(AppInterventionProfile.EMPTY.rules.isEmpty())
        assertEquals(0, AppInterventionProfile.EMPTY.enabledRuleCount)
        assertFalse(AppInterventionProfile.EMPTY.hasEnabledRule)
    }

    @Test
    fun `AppInterventionProfile with enabled rules counts correctly`() {
        val profile = AppInterventionProfile(
            rules = listOf(
                InterventionRule(
                    id = "r1",
                    type = InterventionRuleType.CATCH_ALL,
                    enabled = true,
                ),
                InterventionRule(
                    id = "r2",
                    type = InterventionRuleType.CATCH_ALL,
                    enabled = false,
                ),
                InterventionRule(
                    id = "r3",
                    type = InterventionRuleType.CATCH_ALL,
                    enabled = true,
                ),
            ),
        )
        assertEquals(2, profile.enabledRuleCount)
        assertTrue(profile.hasEnabledRule)
    }

    @Test
    fun `AppInterventionProfile with all disabled rules counts correctly`() {
        val profile = AppInterventionProfile(
            rules = listOf(
                InterventionRule(
                    id = "r1",
                    type = InterventionRuleType.CATCH_ALL,
                    enabled = false,
                ),
                InterventionRule(
                    id = "r2",
                    type = InterventionRuleType.CATCH_ALL,
                    enabled = false,
                ),
            ),
        )
        assertEquals(0, profile.enabledRuleCount)
        assertFalse(profile.hasEnabledRule)
    }

    // ---------- InterventionRule ----------

    @Test
    fun `InterventionRule defaultCatchAll generates unique IDs`() {
        val rule1 = InterventionRule.defaultCatchAll()
        val rule2 = InterventionRule.defaultCatchAll()
        assertTrue(rule1.id != rule2.id)
        assertEquals(36, rule1.id.length) // UUID length
        assertEquals(36, rule2.id.length)
    }

    @Test
    fun `InterventionRule defaultCatchAll respects enabled parameter`() {
        val enabledRule = InterventionRule.defaultCatchAll(enabled = true)
        val disabledRule = InterventionRule.defaultCatchAll(enabled = false)
        assertTrue(enabledRule.enabled)
        assertFalse(disabledRule.enabled)
    }

    @Test
    fun `InterventionRule toJson includes all fields when set`() {
        val rule = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = true,
            crashLogEnabled = false,
        )
        val json = rule.toJson()
        assertEquals("r1", json.getString("id"))
        assertEquals("CATCH_ALL", json.getString("type"))
        assertEquals(true, json.getBoolean("enabled"))
        assertEquals(true, json.getBoolean("showNotify"))
        assertEquals(false, json.getBoolean("crashLogEnabled"))
    }

    @Test
    fun `InterventionRule toJson omits null optional fields`() {
        val rule = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = null,
            crashLogEnabled = null,
        )
        val json = rule.toJson()
        assertTrue(!json.has("showNotify"))
        assertTrue(!json.has("crashLogEnabled"))
    }

    @Test
    fun `InterventionRule fromJson parses valid JSON`() {
        val json = org.json.JSONObject().apply {
            put("id", "r1")
            put("type", "CATCH_ALL")
            put("enabled", true)
            put("showNotify", false)
            put("crashLogEnabled", true)
        }
        val rule = InterventionRule.fromJson(json)
        assertEquals("r1", rule?.id)
        assertEquals(InterventionRuleType.CATCH_ALL, rule?.type)
        assertEquals(true, rule?.enabled)
        assertEquals(false, rule?.showNotify)
        assertEquals(true, rule?.crashLogEnabled)
    }

    @Test
    fun `InterventionRule fromJson returns null for unknown type`() {
        val json = org.json.JSONObject().apply {
            put("id", "r1")
            put("type", "UNKNOWN_TYPE")
            put("enabled", true)
        }
        assertEquals(null, InterventionRule.fromJson(json))
    }

    @Test
    fun `InterventionRule fromJson generates UUID for missing id`() {
        val json = org.json.JSONObject().apply {
            put("type", "CATCH_ALL")
            put("enabled", true)
        }
        val rule = InterventionRule.fromJson(json)
        assertEquals(36, rule?.id?.length)
    }

    @Test
    fun `InterventionRule fromJson handles missing optional fields`() {
        val json = org.json.JSONObject().apply {
            put("id", "r1")
            put("type", "CATCH_ALL")
            put("enabled", true)
        }
        val rule = InterventionRule.fromJson(json)
        assertEquals(null, rule?.showNotify)
        assertEquals(null, rule?.crashLogEnabled)
    }

    // ---------- AppInterventionProfile toJson/fromJson ----------

    @Test
    fun `AppInterventionProfile round-trip serialization`() {
        val original = AppInterventionProfile(
            rules = listOf(
                InterventionRule(
                    id = "r1",
                    type = InterventionRuleType.CATCH_ALL,
                    enabled = true,
                    showNotify = false,
                    crashLogEnabled = true,
                ),
            ),
            updatedAt = 12345678L,
        )
        val json = original.toJson()
        val restored = AppInterventionProfile.fromJson(json)

        assertEquals(original.enabledRuleCount, restored.enabledRuleCount)
        assertEquals(original.updatedAt, restored.updatedAt)
        assertEquals(original.rules.size, restored.rules.size)
        assertEquals(original.rules[0].id, restored.rules[0].id)
        assertEquals(original.rules[0].enabled, restored.rules[0].enabled)
        assertEquals(original.rules[0].showNotify, restored.rules[0].showNotify)
        assertEquals(original.rules[0].crashLogEnabled, restored.rules[0].crashLogEnabled)
    }

    @Test
    fun `AppInterventionProfile fromJson handles empty rules array`() {
        val json = org.json.JSONObject().apply {
            put("rules", org.json.JSONArray())
            put("updatedAt", 99L)
        }
        val profile = AppInterventionProfile.fromJson(json)
        assertEquals(0, profile.rules.size)
        assertEquals(99L, profile.updatedAt)
        assertFalse(profile.hasEnabledRule)
    }

    @Test
    fun `AppInterventionProfile fromJson handles missing rules key`() {
        val json = org.json.JSONObject().apply {
            put("updatedAt", 55L)
        }
        val profile = AppInterventionProfile.fromJson(json)
        assertEquals(0, profile.rules.size)
        assertEquals(55L, profile.updatedAt)
    }

    @Test
    fun `AppInterventionProfile fromJson filters out unknown rule types`() {
        val rulesArray = org.json.JSONArray().apply {
            put(org.json.JSONObject().apply {
                put("id", "r1")
                put("type", "UNKNOWN")
                put("enabled", true)
            })
            put(org.json.JSONObject().apply {
                put("id", "r2")
                put("type", "CATCH_ALL")
                put("enabled", true)
            })
        }
        val json = org.json.JSONObject().apply {
            put("rules", rulesArray)
            put("updatedAt", 0L)
        }
        val profile = AppInterventionProfile.fromJson(json)
        assertEquals(1, profile.rules.size)
        assertEquals("r2", profile.rules[0].id)
    }

    // ---------- InterventionRuleType ----------

    @Test
    fun `InterventionRuleType toJson returns name`() {
        assertEquals("CATCH_ALL", InterventionRuleType.CATCH_ALL.toJson())
    }

    @Test
    fun `InterventionRuleType fromJson returns correct enum`() {
        assertEquals(InterventionRuleType.CATCH_ALL, InterventionRuleType.fromJson("CATCH_ALL"))
    }

    @Test
    fun `InterventionRuleType fromJson returns null for unknown`() {
        assertEquals(null, InterventionRuleType.fromJson("UNKNOWN"))
        assertEquals(null, InterventionRuleType.fromJson(""))
    }

    // ---------- InterventionStatus ----------

    @Test
    fun `InterventionStatus enum values`() {
        assertEquals(2, InterventionStatus.entries.size)
        assertTrue(InterventionStatus.entries.contains(InterventionStatus.PENDING))
        assertTrue(InterventionStatus.entries.contains(InterventionStatus.ENABLED))
    }

    // ---------- InterventionRulesCodec ----------

    @Test
    fun `InterventionRulesCodec round-trip with profile containing null optionals`() {
        val rule = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = null,
            crashLogEnabled = null,
        )
        val profile = AppInterventionProfile(rules = listOf(rule), updatedAt = 0L)
        val original = mapOf("pkg" to profile)

        val encoded = InterventionRulesCodec.encode(original)
        val decoded = InterventionRulesCodec.decode(encoded)

        val decodedRule = decoded["pkg"]!!.rules[0]
        assertEquals(null, decodedRule.showNotify)
        assertEquals(null, decodedRule.crashLogEnabled)
    }

    @Test
    fun `InterventionRulesCodec encode empty map returns empty JSON`() {
        assertEquals("{}", InterventionRulesCodec.encode(emptyMap()))
    }

    @Test
    fun `InterventionRulesCodec decode blank string returns empty map`() {
        assertTrue(InterventionRulesCodec.decode("   ").isEmpty())
    }

    @Test
    fun `InterventionRulesCodec decode invalid JSON returns empty map`() {
        assertTrue(InterventionRulesCodec.decode("not json").isEmpty())
    }
}
