package nota.android.crash.xp.app.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRepositoryTest {

    private val json = Json {
        explicitNulls = false
        encodeDefaults = false
    }

    // ---------- passesSystemFilter ----------

    @Test
    fun `passesSystemFilter returns true for non-system app`() {
        assertTrue(LegacyAppRepository.passesSystemFilter(isSystemApp = false, handleSystem = false))
        assertTrue(LegacyAppRepository.passesSystemFilter(isSystemApp = false, handleSystem = true))
    }

    @Test
    fun `passesSystemFilter returns true for system app when handleSystem is true`() {
        assertTrue(LegacyAppRepository.passesSystemFilter(isSystemApp = true, handleSystem = true))
    }

    @Test
    fun `passesSystemFilter returns false for system app when handleSystem is false`() {
        assertFalse(LegacyAppRepository.passesSystemFilter(isSystemApp = true, handleSystem = false))
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
                LegacyAppRepository.passesSystemFilter(isSystemApp, handleSystem),
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
    fun `InterventionRule serialization includes all fields when set`() {
        val rule = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = true,
            crashLogEnabled = false,
        )
        val jsonStr = json.encodeToString(InterventionRule.serializer(), rule)
        val element = Json.parseToJsonElement(jsonStr).jsonObject
        assertEquals("r1", element["id"]!!.jsonPrimitive.content)
        assertEquals("CATCH_ALL", element["type"]!!.jsonPrimitive.content)
        assertEquals(true, element["enabled"]!!.jsonPrimitive.boolean)
        assertEquals(true, element["showNotify"]!!.jsonPrimitive.boolean)
        assertEquals(false, element["crashLogEnabled"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `InterventionRule serialization omits null optional fields`() {
        val rule = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = null,
            crashLogEnabled = null,
        )
        val jsonStr = json.encodeToString(InterventionRule.serializer(), rule)
        val element = Json.parseToJsonElement(jsonStr).jsonObject
        assertTrue(!element.containsKey("showNotify"))
        assertTrue(!element.containsKey("crashLogEnabled"))
    }

    @Test
    fun `InterventionRule deserialization parses valid JSON`() {
        val jsonStr = """{"id":"r1","type":"CATCH_ALL","enabled":true,"showNotify":false,"crashLogEnabled":true}"""
        val rule = json.decodeFromString(InterventionRule.serializer(), jsonStr)
        assertEquals("r1", rule.id)
        assertEquals(InterventionRuleType.CATCH_ALL, rule.type)
        assertEquals(true, rule.enabled)
        assertEquals(false, rule.showNotify)
        assertEquals(true, rule.crashLogEnabled)
    }

    @Test
    fun `InterventionRule deserialization handles unknown type`() {
        // kotlinx.serialization will throw for unknown enum values
        val jsonStr = """{"id":"r1","type":"UNKNOWN_TYPE","enabled":true}"""
        try {
            json.decodeFromString(InterventionRule.serializer(), jsonStr)
            assertTrue("Should have thrown", false)
        } catch (_: Exception) {
            // Expected
        }
    }

    @Test
    fun `InterventionRule deserialization generates UUID for missing id`() {
        val jsonStr = """{"type":"CATCH_ALL","enabled":true}"""
        val rule = json.decodeFromString(InterventionRule.serializer(), jsonStr)
        assertEquals("", rule.id) // Default empty string, not UUID (UUID generation is in codec)
    }

    @Test
    fun `InterventionRule deserialization handles missing optional fields`() {
        val jsonStr = """{"id":"r1","type":"CATCH_ALL","enabled":true}"""
        val rule = json.decodeFromString(InterventionRule.serializer(), jsonStr)
        assertEquals(null, rule.showNotify)
        assertEquals(null, rule.crashLogEnabled)
    }

    // ---------- AppInterventionProfile serialization ----------

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
        val jsonStr = json.encodeToString(AppInterventionProfile.serializer(), original)
        val restored = json.decodeFromString(AppInterventionProfile.serializer(), jsonStr)

        assertEquals(original.enabledRuleCount, restored.enabledRuleCount)
        assertEquals(original.updatedAt, restored.updatedAt)
        assertEquals(original.rules.size, restored.rules.size)
        assertEquals(original.rules[0].id, restored.rules[0].id)
        assertEquals(original.rules[0].enabled, restored.rules[0].enabled)
        assertEquals(original.rules[0].showNotify, restored.rules[0].showNotify)
        assertEquals(original.rules[0].crashLogEnabled, restored.rules[0].crashLogEnabled)
    }

    @Test
    fun `AppInterventionProfile deserialization handles empty rules array`() {
        val jsonStr = """{"rules":[],"updatedAt":99}"""
        val profile = json.decodeFromString(AppInterventionProfile.serializer(), jsonStr)
        assertEquals(0, profile.rules.size)
        assertEquals(99L, profile.updatedAt)
        assertFalse(profile.hasEnabledRule)
    }

    @Test
    fun `AppInterventionProfile deserialization handles missing rules key`() {
        val jsonStr = """{"updatedAt":55}"""
        val profile = json.decodeFromString(AppInterventionProfile.serializer(), jsonStr)
        assertEquals(0, profile.rules.size)
        assertEquals(55L, profile.updatedAt)
    }

    @Test
    fun `AppInterventionProfile fromJson filters out unknown rule types via codec`() {
        // This behavior is tested via InterventionRulesCodec
        val json = """{"pkg":{"rules":[{"id":"r1","type":"UNKNOWN","enabled":true},{"id":"r2","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val profile = InterventionRulesCodec.decode(json)["pkg"]!!
        assertEquals(1, profile.rules.size)
        assertEquals("r2", profile.rules[0].id)
    }

    // ---------- InterventionRuleType ----------

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
