package nota.android.crash.xp.app.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterventionRulesCodecTest {

    @Test
    fun `round-trip single profile with single rule`() {
        val rule = InterventionRule(
            id = "rule-1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
            showNotify = true,
            crashLogEnabled = false,
        )
        val profile = AppInterventionProfile(
            rules = listOf(rule),
            updatedAt = 12345678L,
        )
        val original = mapOf("com.example.app" to profile)

        val encoded = InterventionRulesCodec.encode(original)
        val decoded = InterventionRulesCodec.decode(encoded)

        assertEquals(1, decoded.size)
        val decodedProfile = decoded["com.example.app"]!!
        assertEquals(1, decodedProfile.rules.size)
        assertEquals("rule-1", decodedProfile.rules[0].id)
        assertEquals(InterventionRuleType.CATCH_ALL, decodedProfile.rules[0].type)
        assertEquals(true, decodedProfile.rules[0].enabled)
        assertEquals(true, decodedProfile.rules[0].showNotify)
        assertEquals(false, decodedProfile.rules[0].crashLogEnabled)
        assertEquals(12345678L, decodedProfile.updatedAt)
    }

    @Test
    fun `round-trip multiple profiles with multiple rules`() {
        val rule1 = InterventionRule(
            id = "r1",
            type = InterventionRuleType.CATCH_ALL,
            enabled = true,
        )
        val rule2 = InterventionRule(
            id = "r2",
            type = InterventionRuleType.CATCH_ALL,
            enabled = false,
            showNotify = false,
            crashLogEnabled = true,
        )
        val profileA = AppInterventionProfile(rules = listOf(rule1, rule2), updatedAt = 100L)
        val profileB = AppInterventionProfile(rules = listOf(rule1), updatedAt = 200L)

        val original = mapOf("pkg.a" to profileA, "pkg.b" to profileB)
        val encoded = InterventionRulesCodec.encode(original)
        val decoded = InterventionRulesCodec.decode(encoded)

        assertEquals(2, decoded.size)
        assertEquals(2, decoded["pkg.a"]!!.rules.size)
        assertEquals(1, decoded["pkg.b"]!!.rules.size)
        assertEquals(100L, decoded["pkg.a"]!!.updatedAt)
        assertEquals(200L, decoded["pkg.b"]!!.updatedAt)
    }

    @Test
    fun `encode empty map returns empty JSON object`() {
        val encoded = InterventionRulesCodec.encode(emptyMap())
        assertEquals("{}", encoded)
    }

    @Test
    fun `decode empty JSON object returns empty map`() {
        val decoded = InterventionRulesCodec.decode("{}")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun `decode blank string returns empty map`() {
        val decoded = InterventionRulesCodec.decode("   ")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun `decode invalid JSON returns empty map`() {
        val decoded = InterventionRulesCodec.decode("not json at all")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun `decode ignores unknown rule types`() {
        val json = """{"pkg":{"rules":[{"id":"r1","type":"UNKNOWN_TYPE","enabled":true}],"updatedAt":0}}"""
        val decoded = InterventionRulesCodec.decode(json)
        assertEquals(1, decoded.size)
        assertEquals(0, decoded["pkg"]!!.rules.size) // unknown type filtered out
    }

    @Test
    fun `decode handles missing optional fields`() {
        val json = """{"pkg":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true}],"updatedAt":42}}"""
        val decoded = InterventionRulesCodec.decode(json)
        val rule = decoded["pkg"]!!.rules[0]
        assertEquals(null, rule.showNotify)
        assertEquals(null, rule.crashLogEnabled)
    }

    @Test
    fun `decode handles null optional fields`() {
        val json = """{"pkg":{"rules":[{"id":"r1","type":"CATCH_ALL","enabled":true,"showNotify":null,"crashLogEnabled":null}],"updatedAt":0}}"""
        val decoded = InterventionRulesCodec.decode(json)
        val rule = decoded["pkg"]!!.rules[0]
        assertEquals(null, rule.showNotify)
        assertEquals(null, rule.crashLogEnabled)
    }

    @Test
    fun `decode handles empty rules array`() {
        val json = """{"pkg":{"rules":[],"updatedAt":99}}"""
        val decoded = InterventionRulesCodec.decode(json)
        assertEquals(0, decoded["pkg"]!!.rules.size)
        assertEquals(99L, decoded["pkg"]!!.updatedAt)
    }

    @Test
    fun `decode handles missing rules key`() {
        val json = """{"pkg":{"updatedAt":55}}"""
        val decoded = InterventionRulesCodec.decode(json)
        assertEquals(0, decoded["pkg"]!!.rules.size)
        assertEquals(55L, decoded["pkg"]!!.updatedAt)
    }

    @Test
    fun `decode generates UUID for missing rule id`() {
        val json = """{"pkg":{"rules":[{"type":"CATCH_ALL","enabled":true}],"updatedAt":0}}"""
        val decoded = InterventionRulesCodec.decode(json)
        val rule = decoded["pkg"]!!.rules[0]
        assertEquals(36, rule.id.length) // UUID string length
    }

    @Test
    fun `round-trip with null showNotify and crashLogEnabled`() {
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
    fun `encode produces valid JSON structure`() {
        val rule = InterventionRule.defaultCatchAll(enabled = true)
        val profile = AppInterventionProfile(rules = listOf(rule), updatedAt = 0L)
        val encoded = InterventionRulesCodec.encode(mapOf("com.test" to profile))

        // Should be parseable and contain expected keys
        assertTrue(encoded.contains("\"com.test\""))
        assertTrue(encoded.contains("\"rules\""))
        assertTrue(encoded.contains("\"CATCH_ALL\""))
        assertTrue(encoded.contains("\"updatedAt\""))
    }

    @Test
    fun `decode ignores non-object values in root`() {
        val json = """{"pkg":{"rules":[],"updatedAt":0},"bad":"not an object"}"""
        val decoded = InterventionRulesCodec.decode(json)
        assertEquals(1, decoded.size) // only "pkg" is kept
        assertTrue(decoded.containsKey("pkg"))
        assertTrue(!decoded.containsKey("bad"))
    }
}
