package nota.android.crash.xp.app.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import android.util.Log
import java.util.UUID

enum class InterventionRuleType {
    CATCH_ALL,
    ;

    companion object {
        fun fromJson(value: String): InterventionRuleType? =
            entries.firstOrNull { it.name == value }
    }
}

enum class InterventionStatus {
    PENDING,
    ENABLED,
}

@Serializable
data class InterventionRule(
    val id: String = "",
    val type: InterventionRuleType,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val enabled: Boolean = false,
    val showNotify: Boolean? = null,
    val crashLogEnabled: Boolean? = null,
) {
    companion object {
        fun defaultCatchAll(enabled: Boolean = true): InterventionRule = InterventionRule(
            id = UUID.randomUUID().toString(),
            type = InterventionRuleType.CATCH_ALL,
            enabled = enabled,
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AppInterventionProfile(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val rules: List<InterventionRule> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val updatedAt: Long = System.currentTimeMillis(),
) {
    val hasEnabledRule: Boolean get() = rules.any { it.enabled }
    val enabledRuleCount: Int get() = rules.count { it.enabled }

    companion object {
        val EMPTY = AppInterventionProfile(rules = emptyList())
    }
}

object InterventionRulesCodec {
    private val json = Json {
        explicitNulls = false
        encodeDefaults = false
    }

    fun decode(jsonStr: String): Map<String, AppInterventionProfile> {
        if (jsonStr.isBlank() || jsonStr == "{}") return emptyMap()
        return try {
            val root = json.parseToJsonElement(jsonStr).jsonObject
            buildMap {
                for ((packageName, element) in root) {
                    if (element !is kotlinx.serialization.json.JsonObject) continue
                    val profile = decodeProfile(element)
                    put(packageName, profile)
                }
            }
        } catch (e: Exception) {
            try { Log.w("InterventionRulesCodec", "decode failed", e) } catch (_: Throwable) {}
            emptyMap()
        }
    }

    private fun decodeProfile(jsonObject: kotlinx.serialization.json.JsonObject): AppInterventionProfile {
        val rulesArray = jsonObject["rules"]?.jsonArray
        val rules = if (rulesArray != null) {
            buildList {
                for (ruleElement in rulesArray) {
                    try {
                        val rule = json.decodeFromJsonElement<InterventionRule>(ruleElement)
                        val finalRule = if (rule.id.isEmpty()) {
                            rule.copy(id = UUID.randomUUID().toString())
                        } else {
                            rule
                        }
                        add(finalRule)
                    } catch (e: Exception) {
                        try { Log.w("InterventionRulesCodec", "Skipping invalid rule", e) } catch (_: Throwable) {}
                    }
                }
            }
        } else {
            emptyList()
        }
        val updatedAt = jsonObject["updatedAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
        return AppInterventionProfile(
            rules = rules,
            updatedAt = updatedAt,
        )
    }

    fun encode(profiles: Map<String, AppInterventionProfile>): String {
        if (profiles.isEmpty()) return "{}"
        return json.encodeToString(
            serializer = MapSerializer(String.serializer(), AppInterventionProfile.serializer()),
            value = profiles,
        )
    }
}
