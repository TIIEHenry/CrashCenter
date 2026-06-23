package nota.android.crash.analysis

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nota.android.crash.common.data.CrashAnalysis

@Serializable
internal data class Rule(
    val id: String,
    val exceptionClassPatterns: List<String> = emptyList(),
    val stackTracePatterns: List<String> = emptyList(),
    val category: String,
    val rootCauseTags: List<String> = emptyList(),
    val suggestion: String,
    val devSuggestion: String,
)

@Serializable
internal data class RuleFile(
    val version: Int,
    val rules: List<Rule>,
)

class RuleEngine internal constructor(private val rules: List<Rule>) {

    /**
     * Attempt to match the crash to a rule.
     *
     * @param exceptionClass fully-qualified exception class name (e.g. "java.lang.NullPointerException")
     * @param stackTrace full stack trace text
     * @return [CrashAnalysis] if a rule matched, null otherwise
     */
    fun match(exceptionClass: String, stackTrace: String): CrashAnalysis? {
        if (rules.isEmpty()) return null
        val traceUpper = stackTrace.uppercase()
        for (rule in rules) {
            val matched = when {
                rule.exceptionClassPatterns.isNotEmpty() ->
                    matchesException(rule, exceptionClass) &&
                        (rule.stackTracePatterns.isEmpty() || matchesStackTrace(rule, traceUpper))
                else ->
                    rule.stackTracePatterns.isNotEmpty() && matchesStackTrace(rule, traceUpper)
            }
            if (!matched) continue
            return CrashAnalysis(
                category = rule.category,
                rootCauseTags = rule.rootCauseTags,
                suggestion = rule.suggestion,
                devSuggestion = rule.devSuggestion,
            )
        }
        return null
    }

    private fun matchesException(rule: Rule, exceptionClass: String): Boolean {
        return rule.exceptionClassPatterns.any { pattern ->
            exceptionClass == pattern || exceptionClass.startsWith("$pattern$")
        }
    }

    private fun matchesStackTrace(rule: Rule, traceUpper: String): Boolean {
        if (rule.stackTracePatterns.isEmpty()) return true
        return rule.stackTracePatterns.any { it.uppercase() in traceUpper }
    }

    companion object {
        private const val RULES_ASSET_PATH = "crash_analysis/rules_v1.json"

        fun fromAssets(context: Context): RuleEngine {
            val json = context.assets.open(RULES_ASSET_PATH).use { it.readBytes().toString(Charsets.UTF_8) }
            return fromJson(json)
        }

        internal fun fromJson(json: String): RuleEngine {
            val ruleFile = Json { ignoreUnknownKeys = true }.decodeFromString<RuleFile>(json)
            return RuleEngine(ruleFile.rules)
        }
    }
}
