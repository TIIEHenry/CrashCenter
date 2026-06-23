package nota.android.crash.common.data

import kotlinx.serialization.Serializable

@Serializable
data class CrashAnalysis(
    val category: String,
    val rootCauseTags: List<String>,
    val suggestion: String,
    val devSuggestion: String,
)
