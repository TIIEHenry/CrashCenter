package nota.android.crash.common.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CrashEvent(
    val id: String = "",
    val timestampMs: Long = 0L,
    val packageName: String = "",
    val appLabel: String? = null,
    val processName: String? = null,
    val exceptionClass: String = "Unknown",
    val message: String? = null,
    val stackTrace: String = "",
    val source: String? = null,
    val backendWritten: List<String> = emptyList(),
    val ingestedFrom: String? = null,
) {
    fun withBackendWritten(backends: List<String>): CrashEvent =
        copy(backendWritten = backends.distinct())
    val shortExceptionClass: String
        get() = exceptionClass.substringAfterLast('.')

    fun toJsonLine(): String {
        return json.encodeToString(CrashEvent.serializer(), this)
    }

    companion object {
        private val json = Json {
            explicitNulls = false
            encodeDefaults = false
        }

        fun fromJson(line: String): CrashEvent? {
            return try {
                val event = json.decodeFromString<CrashEvent>(line)
                if (event.id.isEmpty()) return null
                event.copy(backendWritten = event.backendWritten.filter { it.isNotEmpty() })
            } catch (_: Exception) {
                null
            }
        }
    }
}
