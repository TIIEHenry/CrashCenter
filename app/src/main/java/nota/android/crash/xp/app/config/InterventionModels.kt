package nota.android.crash.xp.app.config

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class InterventionRuleType {
    CATCH_ALL,
    ;

    fun toJson(): String = name

    companion object {
        fun fromJson(value: String): InterventionRuleType? =
            entries.firstOrNull { it.name == value }
    }
}

enum class InterventionStatus {
    PENDING,
    ENABLED,
}

data class InterventionRule(
    val id: String,
    val type: InterventionRuleType,
    val enabled: Boolean,
    val showNotify: Boolean? = null,
    val crashLogEnabled: Boolean? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put(KEY_ID, id)
        put(KEY_TYPE, type.toJson())
        put(KEY_ENABLED, enabled)
        if (showNotify != null) put(KEY_SHOW_NOTIFY, showNotify)
        if (crashLogEnabled != null) put(KEY_CRASH_LOG_ENABLED, crashLogEnabled)
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_TYPE = "type"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SHOW_NOTIFY = "showNotify"
        private const val KEY_CRASH_LOG_ENABLED = "crashLogEnabled"

        fun fromJson(json: JSONObject): InterventionRule? {
            val type = InterventionRuleType.fromJson(json.optString(KEY_TYPE)) ?: return null
            val id = json.optString(KEY_ID).takeIf { it.isNotEmpty() } ?: UUID.randomUUID().toString()
            return InterventionRule(
                id = id,
                type = type,
                enabled = json.optBoolean(KEY_ENABLED, false),
                showNotify = json.optNullableBoolean(KEY_SHOW_NOTIFY),
                crashLogEnabled = json.optNullableBoolean(KEY_CRASH_LOG_ENABLED),
            )
        }

        fun defaultCatchAll(enabled: Boolean = true): InterventionRule = InterventionRule(
            id = UUID.randomUUID().toString(),
            type = InterventionRuleType.CATCH_ALL,
            enabled = enabled,
        )
    }
}

data class AppInterventionProfile(
    val rules: List<InterventionRule>,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val hasEnabledRule: Boolean get() = rules.any { it.enabled }
    val enabledRuleCount: Int get() = rules.count { it.enabled }

    fun toJson(): JSONObject = JSONObject().apply {
        put(KEY_RULES, JSONArray().apply { rules.forEach { put(it.toJson()) } })
        put(KEY_UPDATED_AT, updatedAt)
    }

    companion object {
        private const val KEY_RULES = "rules"
        private const val KEY_UPDATED_AT = "updatedAt"

        val EMPTY = AppInterventionProfile(rules = emptyList())

        fun fromJson(json: JSONObject): AppInterventionProfile {
            val rulesArray = json.optJSONArray(KEY_RULES) ?: JSONArray()
            val rules = buildList {
                for (i in 0 until rulesArray.length()) {
                    val ruleJson = rulesArray.optJSONObject(i) ?: continue
                    InterventionRule.fromJson(ruleJson)?.let { add(it) }
                }
            }
            return AppInterventionProfile(
                rules = rules,
                updatedAt = json.optLong(KEY_UPDATED_AT, System.currentTimeMillis()),
            )
        }
    }
}

object InterventionRulesCodec {
    fun decode(json: String): Map<String, AppInterventionProfile> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            val root = JSONObject(json)
            buildMap {
                root.keys().forEach { packageName ->
                    val profileJson = root.optJSONObject(packageName) ?: return@forEach
                    put(packageName, AppInterventionProfile.fromJson(profileJson))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun encode(profiles: Map<String, AppInterventionProfile>): String {
        if (profiles.isEmpty()) return "{}"
        val root = JSONObject()
        profiles.forEach { (packageName, profile) ->
            root.put(packageName, profile.toJson())
        }
        return root.toString()
    }
}

private fun JSONObject.optNullableBoolean(key: String): Boolean? {
    if (!has(key) || isNull(key)) return null
    return getBoolean(key)
}
