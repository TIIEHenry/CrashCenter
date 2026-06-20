package nota.android.crash.xp.app.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InterventionModelsTest {

    private val json = Json {
        explicitNulls = false
        encodeDefaults = false
    }

    // ---------- InterventionRule serialization ----------

    @Test
    fun `InterventionRule serialization with all fields set`() {
        val rule = InterventionRule(
            id = "rule-1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = true,
            crashLogEnabled = false,
        )
        val jsonStr = json.encodeToString(InterventionRule.serializer(), rule)
        val element = Json.parseToJsonElement(jsonStr).jsonObject

        assertEquals("rule-1", element["id"]!!.jsonPrimitive.content)
        assertEquals("CATCH_ALL", element["type"]!!.jsonPrimitive.content)
        assertEquals(true, element["enabled"]!!.jsonPrimitive.boolean)
        assertEquals(true, element["showNotify"]!!.jsonPrimitive.boolean)
        assertEquals(false, element["crashLogEnabled"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `InterventionRule serialization omits null showNotify`() {
        val rule = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = null,
            crashLogEnabled = true,
        )
        val jsonStr = json.encodeToString(InterventionRule.serializer(), rule)
        val element = Json.parseToJsonElement(jsonStr).jsonObject
        assertTrue(!element.containsKey("showNotify"))
        assertEquals(true, element["crashLogEnabled"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `InterventionRule serialization omits null crashLogEnabled`() {
        val rule = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = false,
            crashLogEnabled = null,
        )
        val jsonStr = json.encodeToString(InterventionRule.serializer(), rule)
        val element = Json.parseToJsonElement(jsonStr).jsonObject
        assertEquals(false, element["showNotify"]!!.jsonPrimitive.boolean)
        assertTrue(!element.containsKey("crashLogEnabled"))
    }

    @Test
    fun `InterventionRule serialization omits both null optionals`() {
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
        assertEquals(3, element.size) // id, type, enabled
    }

    @Test
    fun `InterventionRule serialization with disabled state`() {
        val rule = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = false,
            showNotify = true,
            crashLogEnabled = true,
        )
        val jsonStr = json.encodeToString(InterventionRule.serializer(), rule)
        val element = Json.parseToJsonElement(jsonStr).jsonObject
        assertEquals(false, element["enabled"]!!.jsonPrimitive.boolean)
        assertEquals(true, element["showNotify"]!!.jsonPrimitive.boolean)
        assertEquals(true, element["crashLogEnabled"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun `InterventionRule round-trip serialization`() {
        val original = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = false,
            crashLogEnabled = true,
        )
        val jsonStr = json.encodeToString(InterventionRule.serializer(), original)
        val restored = json.decodeFromString(InterventionRule.serializer(), jsonStr)

        assertEquals(original.id, restored.id)
        assertEquals(original.type, restored.type)
        assertEquals(original.enabled, restored.enabled)
        assertEquals(original.showNotify, restored.showNotify)
        assertEquals(original.crashLogEnabled, restored.crashLogEnabled)
    }

    @Test
    fun `InterventionRule round-trip with null optionals`() {
        val original = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = null,
            crashLogEnabled = null,
        )
        val jsonStr = json.encodeToString(InterventionRule.serializer(), original)
        val restored = json.decodeFromString(InterventionRule.serializer(), jsonStr)

        assertEquals(original.id, restored.id)
        assertEquals(original.type, restored.type)
        assertEquals(original.enabled, restored.enabled)
        assertNull(restored.showNotify)
        assertNull(restored.crashLogEnabled)
    }

    // ---------- AppInterventionProfile serialization ----------

    @Test
    fun `AppInterventionProfile serialization with empty rules`() {
        val profile = AppInterventionProfile(rules = emptyList(), updatedAt = 123L)
        val jsonStr = json.encodeToString(AppInterventionProfile.serializer(), profile)
        val element = Json.parseToJsonElement(jsonStr).jsonObject

        assertEquals(123L, element["updatedAt"]!!.jsonPrimitive.long)
        val rulesArray = element["rules"]!!.jsonArray
        assertEquals(0, rulesArray.size)
    }

    @Test
    fun `AppInterventionProfile serialization with multiple rules`() {
        val rule1 = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = true,
            crashLogEnabled = false,
        )
        val rule2 = InterventionRule(
            id = "r2",
            type = InterventionRuleType.CATCH_ALL,
            enabled = false,
            showNotify = null,
            crashLogEnabled = null,
        )
        val profile = AppInterventionProfile(rules = listOf(rule1, rule2), updatedAt = 456L)
        val jsonStr = json.encodeToString(AppInterventionProfile.serializer(), profile)
        val element = Json.parseToJsonElement(jsonStr).jsonObject

        assertEquals(456L, element["updatedAt"]!!.jsonPrimitive.long)
        val rulesArray = element["rules"]!!.jsonArray
        assertEquals(2, rulesArray.size)
        assertEquals("r1", rulesArray[0].jsonObject["id"]!!.jsonPrimitive.content)
        assertEquals("r2", rulesArray[1].jsonObject["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `AppInterventionProfile round-trip with empty rules`() {
        val original = AppInterventionProfile(rules = emptyList(), updatedAt = 999L)
        val jsonStr = json.encodeToString(AppInterventionProfile.serializer(), original)
        val restored = json.decodeFromString(AppInterventionProfile.serializer(), jsonStr)

        assertEquals(0, restored.rules.size)
        assertEquals(999L, restored.updatedAt)
        assertFalse(restored.hasEnabledRule)
        assertEquals(0, restored.enabledRuleCount)
    }

    @Test
    fun `AppInterventionProfile round-trip with rules`() {
        val rule = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = false,
            crashLogEnabled = true,
        )
        val original = AppInterventionProfile(rules = listOf(rule), updatedAt = 777L)
        val jsonStr = json.encodeToString(AppInterventionProfile.serializer(), original)
        val restored = json.decodeFromString(AppInterventionProfile.serializer(), jsonStr)

        assertEquals(1, restored.rules.size)
        assertEquals("r1", restored.rules[0].id)
        assertEquals(true, restored.rules[0].enabled)
        assertEquals(false, restored.rules[0].showNotify)
        assertEquals(true, restored.rules[0].crashLogEnabled)
        assertEquals(777L, restored.updatedAt)
    }

    @Test
    fun `AppInterventionProfile serialization preserves rule order`() {
        val rules = (1..5).map { i ->
            InterventionRule(
                id = "r$i",
                type = InterventionRuleType.CATCH_ALL,
                enabled = i % 2 == 1,
            )
        }
        val profile = AppInterventionProfile(rules = rules, updatedAt = 0L)
        val jsonStr = json.encodeToString(AppInterventionProfile.serializer(), profile)
        val element = Json.parseToJsonElement(jsonStr).jsonObject
        val rulesArray = element["rules"]!!.jsonArray

        assertEquals(5, rulesArray.size)
        for (i in 1..5) {
            assertEquals("r$i", rulesArray[i - 1].jsonObject["id"]!!.jsonPrimitive.content)
        }
    }

    // ---------- AppInterventionProfile.EMPTY ----------

    @Test
    fun `EMPTY profile has no rules and no enabled rules`() {
        assertEquals(0, AppInterventionProfile.EMPTY.rules.size)
        assertEquals(0, AppInterventionProfile.EMPTY.enabledRuleCount)
        assertFalse(AppInterventionProfile.EMPTY.hasEnabledRule)
    }

    @Test
    fun `EMPTY profile serialization produces valid JSON`() {
        val jsonStr = json.encodeToString(AppInterventionProfile.serializer(), AppInterventionProfile.EMPTY)
        val element = Json.parseToJsonElement(jsonStr).jsonObject
        assertTrue(element.containsKey("rules"))
        assertTrue(element.containsKey("updatedAt"))
        assertEquals(0, element["rules"]!!.jsonArray.size)
    }

    // ---------- InterventionRuleType ----------

    @Test
    fun `InterventionRuleType fromJson returns correct enum`() {
        assertEquals(InterventionRuleType.CATCH_ALL, InterventionRuleType.fromJson("CATCH_ALL"))
    }

    @Test
    fun `InterventionRuleType fromJson returns null for unknown`() {
        assertNull(InterventionRuleType.fromJson("UNKNOWN"))
        assertNull(InterventionRuleType.fromJson(""))
        assertNull(InterventionRuleType.fromJson("catch_all")) // case sensitive
    }

    // ---------- InterventionStatus ----------

    @Test
    fun `InterventionStatus enum values`() {
        assertEquals(2, InterventionStatus.entries.size)
        assertTrue(InterventionStatus.entries.contains(InterventionStatus.PENDING))
        assertTrue(InterventionStatus.entries.contains(InterventionStatus.ENABLED))
    }

    @Test
    fun `InterventionStatus values are distinct`() {
        assertTrue(InterventionStatus.PENDING != InterventionStatus.ENABLED)
    }

    // ---------- InterventionRule defaultCatchAll ----------

    @Test
    fun `defaultCatchAll generates unique UUIDs`() {
        val rule1 = InterventionRule.defaultCatchAll()
        val rule2 = InterventionRule.defaultCatchAll()
        assertTrue(rule1.id != rule2.id)
        assertEquals(36, rule1.id.length)
        assertEquals(36, rule2.id.length)
    }

    @Test
    fun `defaultCatchAll respects enabled parameter`() {
        val enabled = InterventionRule.defaultCatchAll(enabled = true)
        val disabled = InterventionRule.defaultCatchAll(enabled = false)
        assertTrue(enabled.enabled)
        assertFalse(disabled.enabled)
        assertNull(enabled.showNotify)
        assertNull(enabled.crashLogEnabled)
        assertEquals(InterventionRuleType.CATCH_ALL, enabled.type)
    }

    // ---------- InterventionRule deserialization edge cases ----------

    @Test
    fun `decode preserves empty id in direct deserialization`() {
        val jsonStr = """{"id":"","type":"CATCH_ALL","enabled":true}"""
        val rule = json.decodeFromString(InterventionRule.serializer(), jsonStr)
        assertEquals("", rule.id) // Default empty string preserved
    }

    @Test
    fun `codec decode generates UUID when id is empty string`() {
        val jsonStr = """{"pkg":{"rules":[{"id":"","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decoded = InterventionRulesCodec.decode(jsonStr)
        assertEquals(36, decoded["pkg"]!!.rules[0].id.length) // UUID generated by codec
    }

    @Test
    fun `decode handles showNotify true and crashLogEnabled false`() {
        val jsonStr = """{"id":"r1","type":"CATCH_ALL","enabled":true,"showNotify":true,"crashLogEnabled":false}"""
        val rule = json.decodeFromString(InterventionRule.serializer(), jsonStr)
        assertEquals(true, rule.showNotify)
        assertEquals(false, rule.crashLogEnabled)
    }

    @Test
    fun `decode handles null values`() {
        val jsonStr = """{"id":"r1","type":"CATCH_ALL","enabled":true,"showNotify":null,"crashLogEnabled":null}"""
        val rule = json.decodeFromString(InterventionRule.serializer(), jsonStr)
        assertNull(rule.showNotify)
        assertNull(rule.crashLogEnabled)
    }

    // ---------- AppInterventionProfile deserialization edge cases ----------

    @Test
    fun `AppInterventionProfile decode with missing rules key`() {
        val jsonStr = """{"updatedAt":42}"""
        val profile = json.decodeFromString(AppInterventionProfile.serializer(), jsonStr)
        assertEquals(0, profile.rules.size)
        assertEquals(42L, profile.updatedAt)
    }

    @Test
    fun `AppInterventionProfile decode filters invalid rules via codec`() {
        // This is tested via InterventionRulesCodec.decode which handles invalid rules
        val json = """{"pkg":{"rules":[{"id":"r1","type":"INVALID_TYPE","enabled":true},{"id":"r2","type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decoded = InterventionRulesCodec.decode(json)
        assertEquals(1, decoded["pkg"]!!.rules.size)
        assertEquals("r2", decoded["pkg"]!!.rules[0].id)
    }

    @Test
    fun `AppInterventionProfile hasEnabledRule with mixed rules`() {
        val profile = AppInterventionProfile(
            rules = listOf(
                InterventionRule(id = "r1", type = InterventionRuleType.CATCH_ALL, enabled = true),
                InterventionRule(id = "r2", type = InterventionRuleType.CATCH_ALL, enabled = false),
                InterventionRule(id = "r3", type = InterventionRuleType.CATCH_ALL, enabled = true),
            ),
        )
        assertTrue(profile.hasEnabledRule)
        assertEquals(2, profile.enabledRuleCount)
    }

    @Test
    fun `AppInterventionProfile hasEnabledRule with all disabled`() {
        val profile = AppInterventionProfile(
            rules = listOf(
                InterventionRule(id = "r1", type = InterventionRuleType.CATCH_ALL, enabled = false),
                InterventionRule(id = "r2", type = InterventionRuleType.CATCH_ALL, enabled = false),
            ),
        )
        assertFalse(profile.hasEnabledRule)
        assertEquals(0, profile.enabledRuleCount)
    }

    @Test
    fun `AppInterventionProfile hasEnabledRule with no rules`() {
        val profile = AppInterventionProfile(rules = emptyList())
        assertFalse(profile.hasEnabledRule)
        assertEquals(0, profile.enabledRuleCount)
    }

    // ---------- InterventionRulesCodec edge cases ----------

    @Test
    fun `InterventionRulesCodec decode empty string returns empty map`() {
        assertTrue(InterventionRulesCodec.decode("").isEmpty())
    }

    @Test
    fun `InterventionRulesCodec decode whitespace returns empty map`() {
        assertTrue(InterventionRulesCodec.decode("   ").isEmpty())
        assertTrue(InterventionRulesCodec.decode("\n\t").isEmpty())
    }

    @Test
    fun `InterventionRulesCodec decode nested invalid JSON returns empty map`() {
        // Valid outer JSON but invalid inner structure
        val json = """{"pkg": "not an object"}"""
        val decoded = InterventionRulesCodec.decode(json)
        assertEquals(0, decoded.size) // non-object values are skipped
    }

    @Test
    fun `InterventionRulesCodec encode single profile`() {
        val rule = InterventionRule.defaultCatchAll(enabled = true)
        val profile = AppInterventionProfile(rules = listOf(rule), updatedAt = 0L)
        val encoded = InterventionRulesCodec.encode(mapOf("com.test" to profile))

        assertTrue(encoded.contains("com.test"))
        assertTrue(encoded.contains("rules"))
        assertTrue(encoded.contains("CATCH_ALL"))
    }

    @Test
    fun `InterventionRulesCodec round-trip with multiple packages`() {
        val rule1 = InterventionRule(id = "r1", type = InterventionRuleType.CATCH_ALL, enabled = true)
        val rule2 = InterventionRule(id = "r2", type = InterventionRuleType.CATCH_ALL, enabled = false)
        val profiles = mapOf(
            "pkg.a" to AppInterventionProfile(rules = listOf(rule1), updatedAt = 100L),
            "pkg.b" to AppInterventionProfile(rules = listOf(rule2), updatedAt = 200L),
        )

        val encoded = InterventionRulesCodec.encode(profiles)
        val decoded = InterventionRulesCodec.decode(encoded)

        assertEquals(2, decoded.size)
        assertEquals(1, decoded["pkg.a"]!!.rules.size)
        assertEquals(1, decoded["pkg.b"]!!.rules.size)
        assertEquals(true, decoded["pkg.a"]!!.rules[0].enabled)
        assertEquals(false, decoded["pkg.b"]!!.rules[0].enabled)
    }
}
